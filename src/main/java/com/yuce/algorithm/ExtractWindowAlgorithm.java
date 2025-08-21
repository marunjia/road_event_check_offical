package com.yuce.algorithm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.impl.ExtractWindowServiceImpl;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 提框算法服务：负责调用提框接口、结果解析及记录持久化
 */
@Service
@Slf4j
public class ExtractWindowAlgorithm{

    // ------------------------------ 常量定义 ------------------------------
    private static final String EXTRACT_POINT_URL = "http://12.1.97.206:7870/extract";
    private static final int HTTP_CONNECT_TIMEOUT = 10; // 连接超时（秒）
    private static final int HTTP_READ_TIMEOUT = 15;    // 读取超时（秒）
    private static final String DEFAULT_BBOX_COLOR = "red";
    private static final String DEFAULT_BBOX_TYPE = "normal";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final int DEFAULT_COORD = -999; // 默认坐标（表示无数据）


    // ------------------------------ 成员变量 ------------------------------
    private OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ExtractWindowServiceImpl extractWindowService;


    // ------------------------------ 初始化 ------------------------------
    @PostConstruct
    public void init() {
        // 初始化OkHttpClient（单例+连接池，提升性能）
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS)) // 复用连接
                .retryOnConnectionFailure(true) // 连接失败自动重试
                .build();
        log.info("提框服务初始化完成 | 接口地址:{} | 连接超时:{}s | 读取超时:{}s",
                EXTRACT_POINT_URL, HTTP_CONNECT_TIMEOUT, HTTP_READ_TIMEOUT);
    }


    // ------------------------------ 核心业务方法 ------------------------------
    public void extractWindow(OriginalAlarmRecord record) throws IOException {
        // 提取核心字段（确保日志可追踪）
        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        log.info("开始提框处理 | alarmId:{} | imagePath:{} | videoPath:{} | tblId:{}",
                alarmId, imagePath, videoPath, tblId);

        // 入参校验（提前阻断无效请求）
        validateInput(record, alarmId, imagePath, videoPath);

        // 重复提框校验（避免重复调用）
        if (extractWindowService.existsByKey(alarmId, imagePath, videoPath)) {
            log.info("提框记录已存在，跳过处理 | alarmId:{} | imagePath:{} | videoPath:{} | tblId:{}",
                    alarmId, imagePath, videoPath, tblId);
            return;
        }

        // 构建请求参数
        Map<String, Object> requestData = buildRequestData(record);
        String requestBodyStr = objectMapper.writeValueAsString(requestData);
        log.debug("提框请求参数 | alarmId:{} | 请求体:{}", alarmId, requestBodyStr);

        // 调用提框接口（自动关闭响应资源）
        String responseBodyStr;
        try (Response response = callExtractApi(requestBodyStr, alarmId, imagePath, videoPath)) {
            if (!response.isSuccessful()) {
                throw new IOException(String.format(
                        "提框接口返回失败 | alarmId:%s | imagePath:%s | videoPath:%s | 状态码:%d",
                        alarmId, imagePath, videoPath, response.code()));
            }
            ResponseBody body = response.body();
            responseBodyStr = (body != null) ? body.string() : null;
        }

        // 响应体校验
        if (responseBodyStr == null || responseBodyStr.isEmpty()) {
            throw new IOException(String.format(
                    "提框接口返回空响应 | alarmId:%s | imagePath:%s | videoPath:%s",
                    alarmId, imagePath, videoPath));
        }
        log.info("提框接口调用成功 | alarmId:{} | imagePath:{} | videoPath:{} | 响应体:{}",
                alarmId, imagePath, videoPath, responseBodyStr);

        // 解析响应并持久化
        ExtractWindowRecord windowRecord = parseResponse(responseBodyStr, record, alarmId, imagePath, videoPath);
        extractWindowService.insertWindow(windowRecord);
        log.info("提框处理完成 | alarmId:{} | imagePath:{} | videoPath:{} | 提框状态:{}",
                alarmId, imagePath, videoPath, windowRecord.getStatus());
    }


    // ------------------------------ 私有工具方法 ------------------------------
    /**
     * 校验入参合法性
     */
    private void validateInput(OriginalAlarmRecord record, String alarmId, String imagePath, String videoPath) {
        if (record == null) {
            throw new IllegalArgumentException("原始告警记录不能为空");
        }
        if (isEmpty(alarmId)) {
            throw new IllegalArgumentException(String.format(
                    "alarmId不能为空 | imagePath:%s | videoPath:%s", imagePath, videoPath));
        }
        if (isEmpty(imagePath)) {
            throw new IllegalArgumentException(String.format(
                    "imagePath不能为空 | alarmId:%s | videoPath:%s", alarmId, videoPath));
        }
        if (isEmpty(videoPath)) {
            throw new IllegalArgumentException(String.format(
                    "videoPath不能为空 | alarmId:%s | imagePath:%s", alarmId, imagePath));
        }
        if (isEmpty(record.getDeviceId())) {
            throw new IllegalArgumentException(String.format(
                    "deviceId不能为空 | alarmId:%s | imagePath:%s | videoPath:%s",
                    alarmId, imagePath, videoPath));
        }
    }

    /**
     * 调用提框接口
     */
    private Response callExtractApi(String requestBodyStr, String alarmId, String imagePath, String videoPath) throws IOException {
        try {
            Request request = new Request.Builder()
                    .url(EXTRACT_POINT_URL)
                    .post(RequestBody.create(requestBodyStr, JSON_MEDIA_TYPE))
                    .addHeader("Connection", "keep-alive")
                    .build();
            log.debug("发起提框请求 | alarmId:{} | 接口地址:{}", alarmId, EXTRACT_POINT_URL);
            return okHttpClient.newCall(request).execute();
        } catch (IOException e) {
            log.error("提框接口调用异常 | alarmId:{} | imagePath:{} | videoPath:{}",
                    alarmId, imagePath, videoPath, e);
            throw e;
        }
    }

    /**
     * 构建请求参数
     */
    /**
     * 构建请求参数
     */
    private Map<String, Object> buildRequestData(OriginalAlarmRecord record) {
        Map<String, Object> root = new HashMap<>(2);
        root.put("alarm_type", Collections.singletonList("overlay_image_extract_bbox"));

        // 构建messages结构
        List<List<Map<String, Object>>> messages = new ArrayList<>(1);
        List<Map<String, Object>> messageGroup = new ArrayList<>(1);
        Map<String, Object> message = new HashMap<>(2);
        message.put("role", "user");

        // 构建content列表
        List<Map<String, Object>> content = new ArrayList<>(7);
        content.add(buildContentItem("device_id", "device_id", record.getDeviceId()));
        content.add(buildContentItem("image_id", "image_id", generateUniqueId(record.getDeviceId(), "image")));
        content.add(buildContentItem("video_id", "video_id", generateUniqueId(record.getDeviceId(), "video")));
        content.add(buildContentItem("bbox_color", "bbox_color", getExtractColor(record)));
        content.add(buildContentItem("bbox_type", "bbox_type", getExtractWindowType(record)));

        // 修复：Java 8 用 HashMap 替代 Map.of()
        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", record.getImagePath());
        content.add(buildContentItem("image_url", "image_url", imageUrlMap));

        Map<String, Object> videoUrlMap = new HashMap<>();
        videoUrlMap.put("url", record.getVideoPath());
        content.add(buildContentItem("video_url", "video_url", videoUrlMap));

        message.put("content", content);
        messageGroup.add(message);
        messages.add(messageGroup);
        root.put("messages", messages);

        return root;
    }

    /**
     * 生成唯一ID（设备ID+类型+时间戳）
     */
    private String generateUniqueId(String deviceId, String type) {
        return deviceId + "_" + type + "_" + System.currentTimeMillis();
    }

    /**
     * 构建content单项
     */
    private Map<String, Object> buildContentItem(String type, String key, Object value) {
        Map<String, Object> item = new HashMap<>(2);
        item.put("type", type);
        item.put(key, value);
        return item;
    }

    /**
     * 获取提框颜色
     */
    public String getExtractColor(OriginalAlarmRecord record) {
        String company = record.getCompany();
        String imagePath = record.getImagePath();

        // 高瞳且图片路径不含{时返回blue，其他场景返回red
        if ("高瞳".equals(company) && imagePath != null && !imagePath.contains("{")) {
            return "blue";
        }
        return DEFAULT_BBOX_COLOR;
    }

    /**
     * 获取提框类型
     */
    public String getExtractWindowType(OriginalAlarmRecord record) {
        String company = record.getCompany();
        if (company == null) {
            return DEFAULT_BBOX_TYPE;
        }

        switch (company) {
            case "高瞳":
                return "gaotong";
            case "之江智能":
                return "抛洒物".equals(record.getEventType()) ? "zhijiang_paosawu" : "zhijiang";
            default:
                return DEFAULT_BBOX_TYPE;
        }
    }

    /**
     * 解析接口响应为提框记录
     */
    public ExtractWindowRecord parseResponse(String responseBodyStr, OriginalAlarmRecord alarmRecord,
                                              String alarmId, String imagePath, String videoPath) {
        ExtractWindowRecord record = new ExtractWindowRecord();
        // 基础字段赋值
        record.setAlarmId(alarmId);
        record.setImagePath(imagePath);
        record.setVideoPath(videoPath);
        record.setReceivedTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setModifyTime(LocalDateTime.now());

        try {
            JSONArray responseArray = JSONArray.parseArray(responseBodyStr);
            if (responseArray.isEmpty()) {
                log.warn("响应数组为空，使用默认坐标 | alarmId:{} | imagePath:{} | videoPath:{}",
                        alarmId, imagePath, videoPath);
                setDefaultCoords(record);
                return record;
            }

            JSONObject resultObj = responseArray.getJSONObject(0);
            record.setImageId(resultObj.getString("image_id"));
            record.setStatus(resultObj.getIntValue("status"));

            // 解析坐标数据
            String data = resultObj.getString("data");
            if (isEmpty(data)) {
                log.warn("响应data为空，使用默认坐标 | alarmId:{} | imagePath:{} | videoPath:{}",
                        alarmId, imagePath, videoPath);
                setDefaultCoords(record);
                return record;
            }

            JSONArray dataArray = JSONArray.parseArray(data);
            if (dataArray.isEmpty()) {
                log.warn("data数组为空，使用默认坐标 | alarmId:{} | imagePath:{} | videoPath:{}",
                        alarmId, imagePath, videoPath);
                setDefaultCoords(record);
                return record;
            }

            JSONObject dataObj = dataArray.getJSONObject(0);
            JSONArray pointsArray = JSONArray.parseArray(dataObj.getString("points"));
            if (pointsArray.size() < 2) {
                log.warn("points数组元素不足，使用默认坐标 | alarmId:{} | imagePath:{} | videoPath:{} | 实际点数:{}",
                        alarmId, imagePath, videoPath, pointsArray.size());
                setDefaultCoords(record);
                return record;
            }

            // 正常解析坐标
            JSONObject point1 = pointsArray.getJSONObject(0);
            JSONObject point2 = pointsArray.getJSONObject(1);
            record.setPoint1X(point1.getIntValue("x"));
            record.setPoint1Y(point1.getIntValue("y"));
            record.setPoint2X(point2.getIntValue("x"));
            record.setPoint2Y(point2.getIntValue("y"));

        } catch (Exception e) {
            log.error("响应解析异常，使用默认坐标 | alarmId:{} | imagePath:{} | videoPath:{}",
                    alarmId, imagePath, videoPath, e);
            setDefaultCoords(record);
        }

        return record;
    }

    /**
     * 设置默认坐标（无数据时）
     */
    public void setDefaultCoords(ExtractWindowRecord record) {
        record.setPoint1X(DEFAULT_COORD);
        record.setPoint1Y(DEFAULT_COORD);
        record.setPoint2X(DEFAULT_COORD);
        record.setPoint2Y(DEFAULT_COORD);
    }

    /**
     * 字符串空值判断（工具方法）
     */
    public boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}