package com.yuce.algorithm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.*;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 行人算法服务：调用行人检测接口、解析响应、存储检测结果，触发后续业务流程
 * 核心流程：接口调用 → 响应解析 → IOU计算 → 结果存储
 * 适配 JDK 1.8
 */
@Component
@Slf4j
public class PersonAlgorithm {

    // ------------------------------ 常量定义（统一维护，避免硬编码） ------------------------------
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Constant {
        /** 行人检测接口地址 */
        public static final String PERSON_DETECT_URL = "http://12.1.97.206:7860/detect";
        /** 告警类型（接口要求固定值） */
        public static final String ALARM_TYPE = "overlay_image_extract_bbox";
        /** OKHttpClient连接超时（秒） */
        public static final int HTTP_CONNECT_TIMEOUT = 15;
        /** OKHttpClient读取超时（秒） */
        public static final int HTTP_READ_TIMEOUT = 30;
        /** JSON请求媒体类型 */
        public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
        /** ImageId生成格式（日期_告警ID_图片序号_表ID） */
        public static final DateTimeFormatter IMAGE_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
        /** 默认IOU值（无检测结果时） */
        public static final double DEFAULT_IOU = 0.0;
        /** 响应日志长度限制（避免大响应体打印冗余） */
        public static final int RESPONSE_LOG_MAX_LEN = 2000;
    }


    // ------------------------------ 依赖注入（按业务相关性排序） ------------------------------
    @Autowired
    private FrameImageServiceImpl frameImageService;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    private ExtractWindowServiceImpl extractWindowServiceImpl;

    @Autowired
    private ExtractWindowAlgorithm extractWindowAlgorithm;


    // ------------------------------ 成员变量（单例初始化，减少资源消耗） ------------------------------
    /** OKHttpClient单例（复用连接池，提升性能） */
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(Constant.HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constant.HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS)) // 连接池复用
            .retryOnConnectionFailure(true) // 临时网络故障自动重试
            .build();

    /** ObjectMapper单例（避免重复创建，提升序列化性能） */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 行人算法处理：调用检测接口 → 解析结果 → 存储检测过程 → 返回处理状态
     */
    public boolean personDeal(OriginalAlarmRecord record) {
        // 1. 提取核心字段并校验（避免空指针，提前阻断无效请求）
        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        // 2. 重复处理校验（避免重复调用接口和入库）
        if (isAlreadyChecked(tblId)) {
            return true;
        }

        // 3. 构建接口请求参数
        String requestBody;
        try {
            Map<String, Object> requestData = buildRequestData(record);
            requestBody = objectMapper.writeValueAsString(requestData);
        } catch (IOException e) {
            log.error("行人算法检查异常: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常信息:{}", tblId, alarmId, imagePath, imagePath, e.getMessage());
            return false;
        }

        // 4. 调用行人检测接口并处理响应
        try {
            // 4.1 发送接口请求
            String responseBody = callPersonDetectApi(requestBody, tblId);
            if (StringUtils.isEmpty(responseBody)) {
                log.error("行人算法检测结果返回为null: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, imagePath);
                return false;
            }

            // 4.2 查询提框坐标（后续计算IOU必需）
            ExtractWindowRecord extractRecord = getExtractWindowRecord(alarmId, imagePath, videoPath);
            if (extractRecord == null) {
                log.error("告警记录提框失败：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                return false;
            }

            // 4.3 解析响应并构建检测过程列表
            List<CheckAlarmProcess> processList = parseApiResponse(tblId, responseBody, alarmId, imagePath, videoPath, extractRecord);
            if (CollectionUtils.isEmpty(processList)) {
                log.error("告警记录行人检测未获得有效返回体：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                processList = buildEmptyProcessList(tblId, alarmId, imagePath, videoPath);
            }

            // 4.4 批量存储检测结果
            boolean saveResult = checkAlarmProcessServiceImpl.saveBatch(processList);
            if (saveResult) {
                log.error("告警记录行人检测成功且入库成功：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            } else {
                log.error("告警记录行人检测成功但入库失败：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            }
            return saveResult;
        } catch (Exception e) {
            log.error("行人算法检查异常: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常信息:{}", tblId, alarmId, imagePath, imagePath, e.getMessage());
            return false;
        }
    }

    /**
     * 检查告警是否已处理（通过初检结果判断）
     */
    private boolean isAlreadyChecked(long tblId) {
        CheckAlarmResult checkAlarmResult = checkAlarmResultMapper.getResultByTblId(tblId);
        if(checkAlarmResult == null){
            return false;
        } else{
            return true;
        }
    }

    /**
     * 构建行人检测接口请求参数
     */
    private Map<String, Object> buildRequestData(OriginalAlarmRecord record) {
        Map<String, Object> root = new HashMap<>(2);
        // 1. 设置告警类型（固定值）
        root.put("alarm_type", Collections.singletonList(Constant.ALARM_TYPE));

        // 2. 查询帧图片列表（接口需要每张图片的信息）
        List<FrameImageInfo> frameList = frameImageService.getFrameListByKey(
                record.getId(), record.getImagePath(), record.getVideoPath());
        if (CollectionUtils.isEmpty(frameList)) {
            log.warn("未获取到抽帧图片, tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", record.getTblId(), record.getId(), record.getImagePath(), record.getVideoPath());
            root.put("messages", new ArrayList<>());
            return root;
        }

        // 3. 构建messages结构（接口要求格式）
        List<List<Map<String, Object>>> messages = new ArrayList<>(1);
        List<Map<String, Object>> messageGroup = new ArrayList<>(frameList.size());

        for (FrameImageInfo frame : frameList) {
            // 3.1 构建单张图片的content参数
            List<Map<String, Object>> content = new ArrayList<>(4);
            // 设备ID
            content.add(buildContentItem("device_id", "device_id", record.getDeviceId()));
            // 图片ID（自定义唯一格式：日期_告警ID_图片序号_表ID）
            String imageId = buildUniqueImageId(record, frame.getImageSortNo());
            content.add(buildContentItem("image_id", "image_id", imageId));
            // 提框颜色（复用ExtractWindowAlgorithm的逻辑，保持一致性）
            content.add(buildContentItem("bbox_color", "bbox_color", extractWindowAlgorithm.getExtractColor(record)));
            // 图片URL（接口要求{url: "..."}格式）
            content.add(buildContentItem("image_url", "image_url", buildUrlParam(frame.getImageUrl())));

            // 3.2 构建单条message
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
     * 构建content单项（统一格式，减少重复代码）
     */
    private Map<String, Object> buildContentItem(String type, String key, Object value) {
        Map<String, Object> item = new HashMap<>(2);
        item.put("type", type);
        item.put(key, value);
        return item;
    }

    /**
     * 构建图片URL参数（适配接口{url: "..."}格式）
     */
    private Map<String, Object> buildUrlParam(String imageUrl) {
        Map<String, Object> urlParam = new HashMap<>(1);
        urlParam.put("url", imageUrl);
        return urlParam;
    }

    /**
     * 生成唯一ImageId（格式：日期_告警ID_图片序号_表ID）
     */
    private String buildUniqueImageId(OriginalAlarmRecord record, Integer imageSortNo) {
        String dateStr = LocalDateTime.now().format(Constant.IMAGE_ID_DATE_FORMAT);
        // 图片序号为空时用0填充，避免NullPointerException
        int sortNo = (imageSortNo == null || imageSortNo < 0) ? 0 : imageSortNo;
        return String.format("%s_%s_%d_%d",
                dateStr, record.getId(), sortNo, record.getTblId());
    }

    /**
     * 调用行人检测接口
     */
    private String callPersonDetectApi(String requestBody, long tblId) throws IOException {
        // 构建HTTP请求
        Request request = new Request.Builder()
                .url(Constant.PERSON_DETECT_URL)
                .post(RequestBody.create(requestBody, Constant.JSON_MEDIA_TYPE))
                .addHeader("Connection", "keep-alive")
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response == null || !response.isSuccessful()) {
                int statusCode = (response != null) ? response.code() : -1;
                throw new IOException(String.format("行人检测接口返回接口编码异常:%d, tblId:%d", statusCode, tblId));
            }

            // 读取响应体（统一处理null情况）
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException(String.format("行人检测接口返回数据为null, tblId:%d", tblId));
            }

            String responseStr = responseBody.string();
            // 响应体过长时截断日志（避免冗余）
            String logResponse = responseStr.length() > Constant.RESPONSE_LOG_MAX_LEN
                    ? responseStr.substring(0, Constant.RESPONSE_LOG_MAX_LEN) + "..."
                    : responseStr;
            return responseStr;
        }
    }

    /**
     * 查询提框记录（用于计算IOU）
     */
    private ExtractWindowRecord getExtractWindowRecord(String alarmId, String imagePath, String videoPath) {
        try {
            return extractWindowServiceImpl.getExtractWindow(alarmId, imagePath, videoPath);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析接口响应，构建CheckAlarmProcess列表
     */
    private List<CheckAlarmProcess> parseApiResponse(long tblId, String responseBody, String alarmId, String imagePath, String videoPath, ExtractWindowRecord extractRecord) {
        List<CheckAlarmProcess> processList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int baseX1 = extractRecord.getPoint1X();
        int baseY1 = extractRecord.getPoint1Y();
        int baseX2 = extractRecord.getPoint2X();
        int baseY2 = extractRecord.getPoint2Y();

        try {
            JSONArray resultArray = JSONArray.parseArray(responseBody);
            for (int i = 0; i < resultArray.size(); i++) {
                JSONObject resultObj = resultArray.getJSONObject(i);
                String imageId = resultObj.getString("image_id");
                int status = resultObj.getIntValue("status");
                JSONArray dataArray = resultObj.getJSONArray("data");

                if (dataArray != null && dataArray.size() > 0) {
                    // 有检测结果时，逐个构建CheckAlarmProcess
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONObject dataObj = dataArray.getJSONObject(j);
                        processList.add(buildCheckProcess(tblId, alarmId, imagePath, videoPath, imageId, status, dataObj, baseX1, baseY1, baseX2, baseY2, now));
                    }
                } else {
                    // 无检测结果时，构建空的CheckAlarmProcess（标记状态）
                    processList.add(buildEmptyCheckProcess(tblId, alarmId, imagePath, videoPath, imageId, status, now));
                }
            }
        } catch (Exception e) {
            log.error("解析行人算法响应异常 | alarmId:{}", alarmId, e);
        }
        return processList;
    }

    /**
     * 构建有检测结果的CheckAlarmProcess
     */
    private CheckAlarmProcess buildCheckProcess(long tblId, String alarmId, String imagePath, String videoPath,
                                                String imageId, int status, JSONObject dataObj,
                                                int baseX1, int baseY1, int baseX2, int baseY2, LocalDateTime now) {
        CheckAlarmProcess process = new CheckAlarmProcess();
        // 基础关联字段
        process.setTblId(tblId);
        process.setAlarmId(alarmId);
        process.setImageId(imageId);
        process.setImagePath(imagePath);
        process.setVideoPath(videoPath);
        // 时间字段
        process.setReceivedTime(now);
        process.setCompletedTime(now);
        // 检测结果字段
        process.setStatus(status);
        process.setType(dataObj.getString("type"));
        process.setName(dataObj.getString("name"));
        process.setScore(dataObj.getBigDecimal("score"));

        // 解析坐标点（处理可能的格式异常）
        JSONArray pointsJsonArray = dataObj.getJSONArray("points");
        if (pointsJsonArray != null && pointsJsonArray.size() >= 2) {
            JSONObject point1 = pointsJsonArray.getJSONObject(0);
            JSONObject point2 = pointsJsonArray.getJSONObject(1);
            int checkX1 = point1.getIntValue("x");
            int checkY1 = point1.getIntValue("y");
            int checkX2 = point2.getIntValue("x");
            int checkY2 = point2.getIntValue("y");

            process.setPoint1X(checkX1);
            process.setPoint1Y(checkY1);
            process.setPoint2X(checkX2);
            process.setPoint2Y(checkY2);

            // 计算IOU（与提框坐标对比）
            double iou = IouUtil.calculateIoU(
                    baseX1, baseY1, baseX2, baseY2,
                    checkX1, checkY1, checkX2, checkY2);
            process.setIou(iou);
        } else {
            // 坐标格式异常时，设置默认值
            process.setPoint1X(null);
            process.setPoint1Y(null);
            process.setPoint2X(null);
            process.setPoint2Y(null);
            process.setIou(Constant.DEFAULT_IOU);
            log.warn("coordinate format exception,tblId is:{}", tblId);
        }
        return process;
    }

    /**
     * 构建无检测结果的CheckAlarmProcess（空数据）
     */
    private CheckAlarmProcess buildEmptyCheckProcess(long tblId, String alarmId, String imagePath, String videoPath,
                                                     String imageId, int status, LocalDateTime now) {
        CheckAlarmProcess process = new CheckAlarmProcess();
        process.setTblId(tblId);
        process.setAlarmId(alarmId);
        process.setImageId(imageId);
        process.setImagePath(imagePath);
        process.setVideoPath(videoPath);
        process.setReceivedTime(now);
        process.setCompletedTime(now);
        process.setStatus(status);
        // 无检测结果时，关键字段置空
        process.setType(null);
        process.setName(null);
        process.setScore(null);
        process.setPoint1X(null);
        process.setPoint1Y(null);
        process.setPoint2X(null);
        process.setPoint2Y(null);
        process.setIou(Constant.DEFAULT_IOU);
        return process;
    }

    /**
     * 构建空检测结果列表（当接口无返回数据时使用）
     */
    private List<CheckAlarmProcess> buildEmptyProcessList(long tblId, String alarmId, String imagePath, String videoPath) {
        List<CheckAlarmProcess> emptyList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // 创建一条空记录标记无结果状态
        CheckAlarmProcess emptyProcess = new CheckAlarmProcess();
        emptyProcess.setTblId(tblId);
        emptyProcess.setAlarmId(alarmId);
        emptyProcess.setImageId("EMPTY_" + System.currentTimeMillis()); // 临时ID
        emptyProcess.setImagePath(imagePath);
        emptyProcess.setVideoPath(videoPath);
        emptyProcess.setReceivedTime(now);
        emptyProcess.setCompletedTime(now);
        emptyProcess.setStatus(-1); // 特殊状态码标记无结果
        emptyProcess.setType(null);
        emptyProcess.setName(null);
        emptyProcess.setScore(null);
        emptyProcess.setPoint1X(null);
        emptyProcess.setPoint1Y(null);
        emptyProcess.setPoint2X(null);
        emptyProcess.setPoint2Y(null);
        emptyProcess.setIou(Constant.DEFAULT_IOU);
        emptyList.add(emptyProcess);
        return emptyList;
    }
}