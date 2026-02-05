package com.yuce.algorithm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.FrameImageInfo;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.mapper.FrameImageMapper;
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
 * 抛洒物算法服务：负责调用抛洒物检测接口、处理响应并返回检测状态
 * 核心流程：参数校验 → 重复检测判断 → 请求构建 → 接口调用 → 结果处理
 */
@Slf4j
@Component
public class PswAlgorithm {

    // ------------------------------ 常量定义（统一维护，避免硬编码） ------------------------------
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Constant {
        /** 抛洒物检测接口地址 */
        public static final String PSW_DETECT_URL = "http://12.1.97.206:9991/isps/batch_completions";
        /** 算法置信度阈值 */
        public static final double CONF_THRESHOLD = 0.8;
        /** 告警类型（接口要求固定值） */
        public static final String ALARM_TYPE = "abandon";
        /** HTTP连接超时时间（秒） */
        public static final int HTTP_CONNECT_TIMEOUT = 15;
        /** HTTP读取超时时间（秒） */
        public static final int HTTP_READ_TIMEOUT = 30;
        /** JSON请求媒体类型 */
        public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
        /** 图片ID生成的日期格式 */
        public static final DateTimeFormatter IMAGE_ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
        /** 接口成功响应码 */
        public static final int SUCCESS_CODE = 200;
        /** 响应日志最大长度（避免大响应体打印冗余） */
        public static final int RESPONSE_LOG_MAX_LENGTH = 2000;
    }


    // ------------------------------ 依赖注入（按业务相关性排序） ------------------------------
    @Autowired
    private FrameImageMapper frameImageMapper;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;


    // ------------------------------ 成员变量（单例复用，减少资源消耗） ------------------------------
    /**
     * OkHttpClient单例：配置连接池和超时，避免频繁创建销毁资源
     */
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(Constant.HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constant.HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS)) // 连接池复用
            .retryOnConnectionFailure(true) // 临时网络故障自动重试
            .build();

    /**
     * ObjectMapper单例：避免重复创建，提升JSON序列化性能
     */
    private final ObjectMapper objectMapper = new ObjectMapper();


    // ------------------------------ 核心业务方法 ------------------------------
    /**
     * 抛洒物算法处理主入口：调用检测接口并返回处理结果
     */
    public boolean pswDeal(OriginalAlarmRecord record) {
        // 1. 提取核心字段并校验（提前阻断无效请求，避免后续流程出错）
        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        // 2. 检查是否已处理（避免重复调用接口和数据冗余）
        if (isAlreadyProcessed(tblId)) {
            log.info("抛洒物算法已处理，跳过 | tblId:{} | alarmId:{} | imagePath:{}",
                    tblId, alarmId, imagePath);
            return true;
        }

        // 3. 构建请求参数并序列化
        String requestBodyJson;
        try {
            Map<String, Object> requestData = buildRequestData(record);
            requestBodyJson = objectMapper.writeValueAsString(requestData);
            log.debug("抛洒物算法请求参数 | alarmId:{} | imagePath:{} | videoPath:{} | 请求体:{}", alarmId, imagePath, videoPath, requestBodyJson);
        } catch (IOException e) {
            log.error("抛洒物算法请求体序列化失败 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
            return false;
        }

        // 4. 调用接口并处理响应
        try {
            return executeRequest(requestBodyJson, alarmId, tblId);
        } catch (Exception e) {
            log.error("抛洒物算法处理异常 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
            return false;
        }
    }

    /**
     * 检查告警是否已处理（通过查询检测结果判断）
     */
    private boolean isAlreadyProcessed(long tblId) {
        try {
            CheckAlarmResult result = checkAlarmResultMapper.getResultByTblId(tblId);
            return result != null;
        } catch (Exception e) {
            log.error("查询抛洒物算法处理状态异常,默认按未处理继续 |tblId:{} | 异常详情:{}", tblId, e);
            return false; // 查库异常时允许继续处理，避免阻塞流程
        }
    }

    /**
     * 构建接口请求参数
     */
    private Map<String, Object> buildRequestData(OriginalAlarmRecord record) {
        Map<String, Object> root = new HashMap<>(3);
        // 1. 固定参数设置
        root.put("conf", Constant.CONF_THRESHOLD);
        root.put("alarm_type", Collections.singletonList(Constant.ALARM_TYPE));

        // 2. 查询帧图片列表（接口需要的核心数据）
        List<FrameImageInfo> frameList = frameImageMapper.getFrameListByKey(
                record.getId(), record.getImagePath(), record.getVideoPath());
        if (CollectionUtils.isEmpty(frameList)) {
            log.error("未查询到帧图片列表,请求将包含空消息 | alarmId:{} | imagePath:{} | videoPath:{}", record.getId(), record.getImagePath(), record.getVideoPath());
            root.put("messages", new ArrayList<>());
            return root;
        }

        // 3. 构建messages结构（接口要求的嵌套格式）
        List<List<Map<String, Object>>> messages = new ArrayList<>(1);
        List<Map<String, Object>> imageGroup = new ArrayList<>(frameList.size());
        Set<String> imageIdDedupSet = new HashSet<>(); // 图片ID去重，避免重复请求

        for (FrameImageInfo frame : frameList) {
            // 3.1 生成唯一图片ID（格式：日期_告警ID_图片序号_表ID）
            String imageId = generateUniqueImageId(record, frame.getImageSortNo());
            if (!imageIdDedupSet.add(imageId)) {
                log.debug("图片ID重复，跳过处理 | alarmId:{} | imagePath:{} | videoPath:{} |imageId:{}", record.getId(), record.getImagePath(), record.getVideoPath(), imageId);
                continue;
            }

            // 3.2 构建单张图片的请求内容（复用工具方法，减少重复代码）
            List<Map<String, Object>> content = new ArrayList<>(3);
            content.add(buildContentItem("device_id", "device_id", record.getDeviceId()));
            content.add(buildContentItem("image_id", "image_id", imageId));
            content.add(buildContentItem("image_url", "image_url", buildImageUrlParam(frame.getImageUrl())));

            // 3.3 封装单条图片消息
            Map<String, Object> message = new HashMap<>(1);
            message.put("content", content);
            imageGroup.add(message);
        }

        messages.add(imageGroup);
        root.put("messages", messages);
        return root;
    }

    /**
     * 生成唯一图片ID（确保不重复，便于追踪）
     */
    private String generateUniqueImageId(OriginalAlarmRecord record, Integer imageSortNo) {
        String dateStr = LocalDateTime.now().format(Constant.IMAGE_ID_DATE_FORMAT);
        // 处理图片序号为空的情况，避免NullPointerException
        int sortNo = (imageSortNo == null || imageSortNo < 0) ? 0 : imageSortNo;
        return String.format("%s_%s_%d_%d",
                dateStr, record.getId(), sortNo, record.getTblId());
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
     * 构建图片URL参数（适配接口要求的{url: "..."}格式）
     */
    private Map<String, Object> buildImageUrlParam(String imageUrl) {
        Map<String, Object> urlParam = new HashMap<>(1);
        // 图片URL为空时用空字符串填充，避免序列化null导致的接口异常
        urlParam.put("url", StringUtils.hasText(imageUrl) ? imageUrl : "");
        return urlParam;
    }

    /**
     * 执行HTTP请求并处理响应
     */
    private boolean executeRequest(String requestBodyJson, String alarmId, Long tblId) throws IOException {
        // 1. 构建HTTP请求
        Request request = new Request.Builder()
                .url(Constant.PSW_DETECT_URL)
                .post(RequestBody.create(requestBodyJson, Constant.JSON_MEDIA_TYPE))
                .addHeader("Connection", "keep-alive")
                .build();

        log.info("发起抛洒物算法接口请求 | tblId:{} | alarmId:{} | 接口地址:{}",
                tblId, alarmId, Constant.PSW_DETECT_URL);

        // 2. 执行请求（try-with-resources自动关闭响应资源，避免内存泄漏）
        try (Response response = okHttpClient.newCall(request).execute()) {
            // 3. 提取响应信息
            int responseCode = (response != null) ? response.code() : -1;
            String responseBody = (response != null && response.body() != null) ? response.body().string() : "";

            // 4. 处理响应日志（截断过长内容，避免日志冗余）
            String logResponseBody = responseBody.length() > Constant.RESPONSE_LOG_MAX_LENGTH
                    ? responseBody.substring(0, Constant.RESPONSE_LOG_MAX_LENGTH) + "..."
                    : responseBody;
            log.info("抛洒物算法接口响应 | tblId:{} | alarmId:{} | 状态码:{} | 响应体:{}",
                    tblId, alarmId, responseCode, logResponseBody);

            // 5. 判断接口是否调用成功（仅状态码200视为成功）
            return responseCode == Constant.SUCCESS_CODE;
        }
    }
}