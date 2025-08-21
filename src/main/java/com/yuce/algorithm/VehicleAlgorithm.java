package com.yuce.algorithm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.entity.FrameImageInfo;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.service.impl.CheckAlarmProcessServiceImpl;
import com.yuce.service.impl.ExtractWindowServiceImpl;
import com.yuce.service.impl.FrameImageServiceImpl;
import com.yuce.util.IouUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 停驶算法服务：调用车辆检测接口、解析检测结果、计算IOU并存储处理记录
 * 核心流程：参数校验 → 重复检测判断 → 请求构建 → 接口调用 → 结果解析与入库
 */
@Component
@Slf4j
public class VehicleAlgorithm {

    // ------------------------------ 常量定义（统一维护，避免硬编码） ------------------------------
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Constant {
        /** 停驶算法接口地址 */
        public static final String VEHICLE_DETECT_URL = "http://12.1.97.206:7860/detect";
        /** 告警类型（接口要求固定值） */
        public static final String ALARM_TYPE = "overlay_image_extract_bbox";
        /** 默认边界框颜色（兜底值） */
        public static final String DEFAULT_BBOX_COLOR = "red";
        /** HTTP连接超时（秒） */
        public static final int HTTP_CONNECT_TIMEOUT = 20;
        /** HTTP读取超时（秒） */
        public static final int HTTP_READ_TIMEOUT = 30;
        /** JSON请求媒体类型 */
        public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
        /** ImageId生成日期格式 */
        public static final DateTimeFormatter IMAGE_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
        /** 响应日志最大长度（避免大响应体冗余） */
        public static final int RESPONSE_LOG_MAX_LEN = 2000;
        /** 无检测结果时的IOU默认值 */
        public static final double DEFAULT_IOU = 0.0;
    }


    // ------------------------------ 依赖注入（按业务相关性排序） ------------------------------
    @Autowired
    private FrameImageServiceImpl frameImageService;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessService;

    @Autowired
    private ExtractWindowAlgorithm extractWindowAlgorithm;

    @Autowired
    private ExtractWindowServiceImpl extractWindowService;


    // ------------------------------ 成员变量（单例复用，减少资源消耗） ------------------------------
    /** OkHttpClient单例：配置连接池和超时，提升性能 */
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(Constant.HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constant.HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS)) // 连接池复用
            .retryOnConnectionFailure(true) // 临时网络故障自动重试
            .build();

    /** ObjectMapper单例：避免重复创建，提升序列化性能 */
    private final ObjectMapper objectMapper = new ObjectMapper();


    // ------------------------------ 核心业务方法 ------------------------------
    /**
     * 停驶算法处理主入口：调用检测接口并返回处理结果
     */
    public boolean vehicleDeal(OriginalAlarmRecord record) {
        // 1. 提取核心字段并校验（提前阻断无效请求）
        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        if (!validateCoreFields(alarmId, imagePath, videoPath, tblId)) {
            return false;
        }

        // 2. 日志前缀：所有日志统一包含关键标识
        String logPrefix = String.format("alarmId:%s | imagePath:%s | videoPath:%s | tblId:%d", alarmId, imagePath, videoPath, tblId);
        log.info("开始停驶算法处理 | {}", logPrefix);

        // 3. 重复检测校验（避免重复调用接口）
        if (isAlreadyProcessed(alarmId, imagePath, videoPath)) {
            log.info("停驶算法已处理，跳过 | {}", logPrefix);
            return true;
        }

        // 4. 构建请求参数并序列化
        String requestBodyJson;
        try {
            Map<String, Object> requestData = buildRequestData(record, logPrefix);
            requestBodyJson = objectMapper.writeValueAsString(requestData);
            log.debug("停驶算法请求参数 | {} | 请求体:{}", logPrefix, requestBodyJson);
        } catch (IOException e) {
            log.error("停驶算法请求体序列化失败 | {} | 异常:{}", logPrefix, e.getMessage());
            return false;
        }

        // 5. 调用接口并处理响应
        try {
            return executeDetectRequest(requestBodyJson, record, logPrefix);
        } catch (Exception e) {
            log.error("停驶算法处理异常 | {} | 异常:{}", logPrefix, e.getMessage());
            return false;
        }
    }


    // ------------------------------ 私有工具方法（单一职责，日志全链路覆盖） ------------------------------
    /**
     * 校验核心字段非空（避免空指针异常）
     */
    private boolean validateCoreFields(String alarmId, String imagePath, String videoPath, Long tblId) {
        if (!StringUtils.hasText(alarmId)) {
            log.error("停驶算法处理失败：alarmId为空");
            return false;
        }
        if (!StringUtils.hasText(imagePath)) {
            log.error("停驶算法处理失败 | alarmId:{} | imagePath为空", alarmId);
            return false;
        }
        if (!StringUtils.hasText(videoPath)) {
            log.error("停驶算法处理失败 | alarmId:{} | imagePath:{} | videoPath为空", alarmId, imagePath);
            return false;
        }
        if (tblId == null || tblId <= 0) {
            log.error("停驶算法处理失败 | alarmId:{} | imagePath:{} | tblId非法（需>0）", alarmId, imagePath);
            return false;
        }
        return true;
    }

    /**
     * 检查是否已处理（通过查询检测结果判断）
     */
    private boolean isAlreadyProcessed(String alarmId, String imagePath, String videoPath) {
        try {
            return checkAlarmResultMapper.getResultByKey(alarmId, imagePath, videoPath) != null;
        } catch (Exception e) {
            log.error("查询停驶算法处理状态异常 | alarmId:{} | imagePath:{} | videoPath:{} | 异常:{}",
                    alarmId, imagePath, videoPath, e.getMessage());
            return false; // 查库异常时允许继续处理，避免阻塞流程
        }
    }

    /**
     * 构建停驶算法接口请求参数
     */
    private Map<String, Object> buildRequestData(OriginalAlarmRecord record, String logPrefix) {
        Map<String, Object> root = new HashMap<>(2);
        // 1. 固定告警类型
        root.put("alarm_type", Collections.singletonList(Constant.ALARM_TYPE));

        // 2. 查询帧图片列表（接口需要的核心数据）
        List<FrameImageInfo> frameList = frameImageService.getFrameListByKey(
                record.getId(), record.getImagePath(), record.getVideoPath());
        if (CollectionUtils.isEmpty(frameList)) {
            log.warn("未查询到帧图片列表，请求将包含空消息 | {}", logPrefix);
            root.put("messages", new ArrayList<>());
            return root;
        }

        // 3. 构建messages结构（接口要求的嵌套格式）
        List<List<Map<String, Object>>> messages = new ArrayList<>(1);
        List<Map<String, Object>> messageGroup = new ArrayList<>(frameList.size());

        for (FrameImageInfo frame : frameList) {
            // 3.1 构建单张图片的content参数
            List<Map<String, Object>> content = new ArrayList<>(4);
            // 设备ID
            content.add(buildContentItem("device_id", "device_id", record.getDeviceId()));
            // 唯一图片ID（格式：日期_告警ID_帧序号_表ID）
            String imageId = generateUniqueImageId(record, frame.getImageSortNo());
            content.add(buildContentItem("image_id", "image_id", imageId));
            // 边界框颜色（获取失败时用默认值）
            String bboxColor = getBboxColor(record);
            content.add(buildContentItem("bbox_color", "bbox_color", bboxColor));
            // 图片URL（适配接口{url: "..."}格式）
            content.add(buildContentItem("image_url", "image_url", buildImageUrlParam(frame.getImageUrl())));

            // 3.2 封装单条图片message
            Map<String, Object> message = new HashMap<>(2);
            message.put("role", "user");
            message.put("content", content);
            messageGroup.add(message);
        }

        messages.add(messageGroup);
        root.put("messages", messages);
        return root;
    }

    /**
     * 生成唯一图片ID（确保不重复，便于追踪）
     */
    private String generateUniqueImageId(OriginalAlarmRecord record, Integer imageSortNo) {
        String dateStr = LocalDateTime.now().format(Constant.IMAGE_ID_DATE_FORMAT);
        int sortNo = (imageSortNo == null || imageSortNo < 0) ? 0 : imageSortNo;
        return String.format("%s_%s_%d_%d",
                dateStr, record.getId(), sortNo, record.getTblId());
    }

    /**
     * 获取边界框颜色（失败时返回默认值）
     */
    private String getBboxColor(OriginalAlarmRecord record) {
        try {
            String color = extractWindowAlgorithm.getExtractColor(record);
            return StringUtils.hasText(color) ? color : Constant.DEFAULT_BBOX_COLOR;
        } catch (Exception e) {
            log.warn("获取边界框颜色失败，使用默认值:{} | alarmId:{}",
                    Constant.DEFAULT_BBOX_COLOR, record.getId());
            return Constant.DEFAULT_BBOX_COLOR;
        }
    }

    /**
     * 构建content单项（统一格式，减少重复代码）
     */
    private Map<String, Object> buildContentItem(String type, String key, Object value) {
        Map<String, Object> item = new HashMap<>(2);
        item.put("type", type);
        item.put(key, value);
        return item;
    }

    /**
     * 构建图片URL参数（适配接口格式）
     */
    private Map<String, Object> buildImageUrlParam(String imageUrl) {
        Map<String, Object> urlParam = new HashMap<>(1);
        urlParam.put("url", StringUtils.hasText(imageUrl) ? imageUrl : "");
        return urlParam;
    }

    /**
     * 执行检测接口请求并处理响应
     */
    private boolean executeDetectRequest(String requestBodyJson, OriginalAlarmRecord record, String logPrefix) throws IOException {
        // 1. 构建HTTP请求
        Request request = new Request.Builder()
                .url(Constant.VEHICLE_DETECT_URL)
                .post(RequestBody.create(requestBodyJson, Constant.JSON_MEDIA_TYPE))
                .addHeader("Connection", "keep-alive")
                .build();

        // 2. 执行请求（try-with-resources自动关闭资源）
        try (Response response = okHttpClient.newCall(request).execute()) {
            // 3. 校验响应状态
            if (response == null || !response.isSuccessful()) {
                int statusCode = (response != null) ? response.code() : -1;
                log.error("停驶算法接口返回非成功状态 | {} | 状态码:{}", logPrefix, statusCode);
                return false;
            }

            // 4. 读取响应体并处理日志
            String responseBody = (response.body() != null) ? response.body().string() : "";
            String logResponseBody = responseBody.length() > Constant.RESPONSE_LOG_MAX_LEN
                    ? responseBody.substring(0, Constant.RESPONSE_LOG_MAX_LEN) + "..."
                    : responseBody;
            log.info("停驶算法接口响应 | {} | 响应体:{}", logPrefix, logResponseBody);

            // 5. 解析响应并生成处理记录
            if (StringUtils.isEmpty(responseBody)) {
                log.warn("停驶算法接口返回空响应 | {}", logPrefix);
                return false;
            }
            return parseAndSaveResponse(responseBody, record, logPrefix);
        }
    }

    /**
     * 解析接口响应并保存处理记录
     */
    private boolean parseAndSaveResponse(String responseBody, OriginalAlarmRecord record, String logPrefix) {
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        LocalDateTime now = LocalDateTime.now();

        // 1. 获取提框坐标（计算IOU必需）
        ExtractWindowRecord extractRecord = extractWindowService.getExtractWindow(alarmId, imagePath, videoPath);
        if (extractRecord == null) {
            log.error("解析响应失败 | {} | 未查询到提框记录", logPrefix);
            return false;
        }
        int baseX1 = extractRecord.getPoint1X();
        int baseY1 = extractRecord.getPoint1Y();
        int baseX2 = extractRecord.getPoint2X();
        int baseY2 = extractRecord.getPoint2Y();
        log.debug("停驶算法提框坐标 | {} | 坐标:({},{},{},{})", logPrefix, baseX1, baseY1, baseX2, baseY2);

        // 2. 解析JSON响应并生成记录列表
        List<CheckAlarmProcess> processList = new ArrayList<>();
        try {
            JSONArray resultArray = JSONArray.parseArray(responseBody);
            for (int i = 0; i < resultArray.size(); i++) {
                JSONObject resultObj = resultArray.getJSONObject(i);
                String imageId = resultObj.getString("image_id");
                int status = resultObj.getIntValue("status");
                JSONArray dataArray = resultObj.getJSONArray("data");

                if (dataArray != null && dataArray.size() > 0) {
                    // 2.1 有检测结果：生成正常记录
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONObject dataObj = dataArray.getJSONObject(j);
                        processList.add(buildNormalProcessRecord(
                                dataObj, alarmId, imagePath, videoPath, imageId, status,
                                baseX1, baseY1, baseX2, baseY2, now, logPrefix));
                    }
                } else {
                    // 2.2 无检测结果：生成空记录
                    processList.add(buildEmptyProcessRecord(
                            alarmId, imagePath, videoPath, imageId, status, now));
                    log.warn("停驶算法无检测数据 | {} | imageId:{}", logPrefix, imageId);
                }
            }
        } catch (Exception e) {
            log.error("解析停驶算法响应异常 | {} | 异常:{}", logPrefix, e.getMessage());
            return false;
        }

        // 3. 批量保存记录
        if (CollectionUtils.isEmpty(processList)) {
            log.warn("停驶算法无有效处理记录，无需保存 | {}", logPrefix);
            return false;
        }
        return checkAlarmProcessService.saveBatch(processList);
    }

    /**
     * 构建有检测结果的CheckAlarmProcess
     */
    private CheckAlarmProcess buildNormalProcessRecord(JSONObject dataObj, String alarmId, String imagePath, String videoPath,
                                                       String imageId, int status, int baseX1, int baseY1, int baseX2, int baseY2,
                                                       LocalDateTime now, String logPrefix) {
        CheckAlarmProcess process = new CheckAlarmProcess();
        // 基础关联字段
        process.setAlarmId(alarmId);
        process.setImageId(imageId);
        process.setImagePath(imagePath);
        process.setVideoPath(videoPath);
        process.setReceivedTime(now);
        process.setCompletedTime(now);
        process.setStatus(status);

        // 检测结果字段
        process.setType(dataObj.getString("type"));
        process.setName(dataObj.getString("name"));
        process.setScore(dataObj.getBigDecimal("score"));

        // 解析坐标并计算IOU
        try {
            JSONArray pointsArray = JSONArray.parseArray(dataObj.getString("points"));
            if (pointsArray.size() < 2) {
                log.warn("检测坐标点数不足（需2个点） | {} | imageId:{}", logPrefix, imageId);
                setDefaultBbox(process);
                process.setIou(Constant.DEFAULT_IOU);
                return process;
            }
            // 提取检测框坐标
            JSONObject point1 = pointsArray.getJSONObject(0);
            JSONObject point2 = pointsArray.getJSONObject(1);
            int checkX1 = point1.getIntValue("x");
            int checkY1 = point1.getIntValue("y");
            int checkX2 = point2.getIntValue("x");
            int checkY2 = point2.getIntValue("y");
            // 设置坐标
            process.setPoint1X(checkX1);
            process.setPoint1Y(checkY1);
            process.setPoint2X(checkX2);
            process.setPoint2Y(checkY2);
            // 计算IOU（交并比）
            double iou = IouUtil.calculateIoU(baseX1, baseY1, baseX2, baseY2, checkX1, checkY1, checkX2, checkY2);
            process.setIou(iou);
        } catch (Exception e) {
            log.error("解析检测坐标或计算IOU异常 | {} | imageId:{} | 异常:{}",
                    logPrefix, imageId, e.getMessage());
            setDefaultBbox(process);
            process.setIou(Constant.DEFAULT_IOU);
        }

        return process;
    }

    /**
     * 构建无检测结果的CheckAlarmProcess（空数据）
     */
    private CheckAlarmProcess buildEmptyProcessRecord(String alarmId, String imagePath, String videoPath,
                                                      String imageId, int status, LocalDateTime now) {
        CheckAlarmProcess process = new CheckAlarmProcess();
        process.setAlarmId(alarmId);
        process.setImageId(imageId);
        process.setImagePath(imagePath);
        process.setVideoPath(videoPath);
        process.setReceivedTime(now);
        process.setCompletedTime(now);
        process.setStatus(status);
        // 无结果时关键字段置空
        process.setType(null);
        process.setName(null);
        process.setScore(null);
        setDefaultBbox(process);
        process.setIou(Constant.DEFAULT_IOU);
        return process;
    }

    /**
     * 设置边界框默认值（空或异常时）
     */
    private void setDefaultBbox(CheckAlarmProcess process) {
        process.setPoint1X(null);
        process.setPoint1Y(null);
        process.setPoint2X(null);
        process.setPoint2Y(null);
    }
}