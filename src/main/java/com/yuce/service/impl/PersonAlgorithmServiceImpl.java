package com.yuce.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.*;
import com.yuce.service.AlgorithmService;
import com.yuce.util.IouUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @ClassName VideoDealService
 * @Description 抛洒物算法服务
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/15 11:02
 * @Version 1.0
 */

@Service
@Slf4j
public class PersonAlgorithmServiceImpl implements AlgorithmService {

    @Autowired
    FrameImageServiceImpl frameImageServiceImpl;

    @Autowired
    CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    GeneralAlgorithmServiceImpl generalAlgorithmServiceImpl;

    @Autowired
    ExtractWindowServiceImpl extractWindowServiceImpl;

    //定义接口请求访问地址
    private static final String PSW_URL = "http://12.1.97.206:7860/detect";

    //定义客户端对象
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 调用算法接口
     * @param originalAlarmRecord
     * @return boolean
     */
    public boolean personDeal(OriginalAlarmRecord originalAlarmRecord) {
        String alarmId = originalAlarmRecord.getId();
        log.info("行人算法检验:{}", alarmId);

        // 构造请求体
        Map<String, Object> requestData = buildRequestData(originalAlarmRecord);
        LocalDateTime receivedTime = LocalDateTime.now();
        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(requestData);
        } catch (IOException e) {
            log.error("请求体序列化失败", e);
            return false;
        }

        // 构造请求
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(PSW_URL).post(body).build();

        // 执行同步请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("算法请求失败（HTTP错误）: status={}", response.code());
                return false;
            }

            // 读取响应体字符串
            String responseBody = response.body() != null ? response.body().string() : null;
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("算法响应为空");
                return false;
            }

            //获取该告警id对应的抽框坐标
            ExtractPointRecord extractPointRecord = extractWindowServiceImpl.getExtractPointByAlarmId(alarmId);
            int baseX1 = extractPointRecord.getPoint1X();
            int baseY1 = extractPointRecord.getPoint1Y();
            int baseX2 = extractPointRecord.getPoint2X();
            int baseY2 = extractPointRecord.getPoint2Y();

            // 解析为 JSON 数组
            JSONArray resultArray = JSONArray.parseArray(responseBody);
            for (int i = 0; i < resultArray.size(); i++) {
                JSONObject result = resultArray.getJSONObject(i);

                String imageId = result.getString("image_id");
                int status = result.getIntValue("status");
                String errorMessage = result.getString("error_message");

                JSONArray dataArray = result.getJSONArray("data");
                if (dataArray.size() > 0) {
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONObject subJsonObject = dataArray.getJSONObject(j);
                        String type = subJsonObject.getString("type");
                        String name = subJsonObject.getString("name");
                        BigDecimal score = subJsonObject.getBigDecimal("score");

                        JSONArray pointsJsonArray = JSONArray.parseArray(subJsonObject.getString("points"));
                        JSONObject point1 = pointsJsonArray.getJSONObject(0);
                        JSONObject point2 = pointsJsonArray.getJSONObject(1);

                        CheckAlarmProcess checkAlarmProcess = new CheckAlarmProcess();
                        checkAlarmProcess.setAlarmId(alarmId);
                        checkAlarmProcess.setImageId(imageId);
                        checkAlarmProcess.setReceivedTime(receivedTime);
                        checkAlarmProcess.setStatus(status);
                        checkAlarmProcess.setCompletedTime(LocalDateTime.now());
                        checkAlarmProcess.setType(type);
                        checkAlarmProcess.setName(name);
                        checkAlarmProcess.setScore(score);
                        int checkX1 = point1.getIntValue("x");
                        int checkY1 = point1.getIntValue("y");
                        int checkX2 = point2.getIntValue("x");
                        int checkY2 = point2.getIntValue("y");
                        checkAlarmProcess.setPoint1X(checkX1);
                        checkAlarmProcess.setPoint1Y(checkY1);
                        checkAlarmProcess.setPoint2X(checkX2);
                        checkAlarmProcess.setPoint2Y(checkY2);
                        checkAlarmProcess.setIou(IouUtil.calculateIoU(baseX1,baseY1,baseX2,baseY2,checkX1,checkY1,checkX2,checkY2));
                        checkAlarmProcess.setCreateTime(LocalDateTime.now());
                        checkAlarmProcess.setModifyTime(LocalDateTime.now());
                        checkAlarmProcessServiceImpl.save(checkAlarmProcess);
                    }
                } else {
                    // data 为空也需要保存记录
                    CheckAlarmProcess checkAlarmProcess = new CheckAlarmProcess();
                    checkAlarmProcess.setAlarmId(alarmId);
                    checkAlarmProcess.setImageId(imageId);
                    checkAlarmProcess.setReceivedTime(receivedTime);
                    checkAlarmProcess.setStatus(status);
                    checkAlarmProcess.setCompletedTime(LocalDateTime.now());
                    checkAlarmProcess.setType(null);
                    checkAlarmProcess.setName(null);
                    checkAlarmProcess.setScore(null);
                    checkAlarmProcess.setPoint1X(null);
                    checkAlarmProcess.setPoint1Y(null);
                    checkAlarmProcess.setPoint2X(null);
                    checkAlarmProcess.setPoint2Y(null);
                    checkAlarmProcess.setIou(0.0);
                    checkAlarmProcess.setCreateTime(LocalDateTime.now());
                    checkAlarmProcess.setModifyTime(LocalDateTime.now());
                    checkAlarmProcessServiceImpl.save(checkAlarmProcess);
                }
            }
            return true;
        } catch (IOException e) {
            log.error("算法请求异常", e);
            return false;
        }
    }

    /**
     * 构造请求体 Map 数据
     * @param originalAlarmRecord
     * @return 请求体 Map
     */
    public Map<String, Object> buildRequestData(OriginalAlarmRecord originalAlarmRecord) {
        Map<String, Object> root = new HashMap<>();

        // 添加alarm_type字段
        root.put("alarm_type", Collections.singletonList("overlay_image_extract_bbox"));

        List<FrameImageInfo> list = frameImageServiceImpl.getListByAlarmId(originalAlarmRecord.getId());

        // 外层messages列表
        List<List<Map<String, Object>>> messages = new ArrayList<>();
        // 中间层messageGroup列表（在你的示例中只有一个元素，但根据数据结构可能需要多个）
        List<Map<String, Object>> messageGroup = new ArrayList<>();

        // 为每张图片创建一个单独的message
        for (FrameImageInfo imageInfo : list) {
            List<Map<String, Object>> content = new ArrayList<>();

            // 添加device_id
            Map<String, Object> deviceIdMap = new HashMap<>();
            deviceIdMap.put("type", "device_id");
            deviceIdMap.put("device_id", originalAlarmRecord.getDeviceId());
            content.add(deviceIdMap);

            // 添加image_id（使用图片ID和排序号生成）
            String imageId = originalAlarmRecord.getId() + "_" + imageInfo.getImageSortNo();
            Map<String, Object> imageIdMap = new HashMap<>();
            imageIdMap.put("type", "image_id");
            imageIdMap.put("image_id", imageId);
            content.add(imageIdMap);

            // 添加bbox_color（固定为red）
            Map<String, Object> bboxColorMap = new HashMap<>();
            bboxColorMap.put("type", "bbox_color");
            bboxColorMap.put("bbox_color", extractWindowServiceImpl.getExtractColor(originalAlarmRecord));
            content.add(bboxColorMap);

            // 添加image_url
            Map<String, Object> imageUrlMap = new HashMap<>();
            imageUrlMap.put("type", "image_url");
            Map<String, Object> imageUrlContent = new HashMap<>();
            imageUrlContent.put("url", imageInfo.getImageUrl());
            imageUrlMap.put("image_url", imageUrlContent);
            content.add(imageUrlMap);

            // 创建message并添加到messageGroup
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", content);
            messageGroup.add(message);
        }

        // 将messageGroup添加到messages
        messages.add(messageGroup);

        // 设置messages字段
        root.put("messages", messages);

        return root;
    }
}