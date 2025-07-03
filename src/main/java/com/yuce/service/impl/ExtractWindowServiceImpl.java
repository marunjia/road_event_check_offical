package com.yuce.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.*;
import com.yuce.mapper.ExtractPointMapper;
import com.yuce.service.ExtractPointService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
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
public class ExtractWindowServiceImpl extends ServiceImpl<ExtractPointMapper, ExtractPointRecord> implements ExtractPointService {

    @Autowired
    FrameImageServiceImpl frameImageServiceImpl;

    @Autowired
    CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    GeneralAlgorithmServiceImpl generalAlgorithmServiceImpl;

    @Autowired
    ExtractPointMapper extractPointMapper;

    //定义接口请求访问地址
    private static final String PSW_URL = "http://12.1.97.206:7870/extract";

    //定义客户端对象
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 调用算法接口
     * @param originalAlarmRecord
     * @return boolean
     */
    public boolean extractDeal(OriginalAlarmRecord originalAlarmRecord) {

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

            // 解析为 JSON 数组
            JSONObject resutlJsonObject = JSONArray.parseArray(responseBody).getJSONObject(0);
            String imageId = resutlJsonObject.getString("image_id");
            int status = resutlJsonObject.getIntValue("status");
            String data = resutlJsonObject.getString("data");

            ExtractPointRecord extractPointRecord = new ExtractPointRecord();
            extractPointRecord.setAlarmId(originalAlarmRecord.getId());
            extractPointRecord.setImagePath(originalAlarmRecord.getImagePath());
            extractPointRecord.setVideoPath(originalAlarmRecord.getVideoPath());
            extractPointRecord.setImageId(imageId);
            extractPointRecord.setStatus(status);
            extractPointRecord.setReceivedTime(receivedTime);
            extractPointRecord.setCreateTime(LocalDateTime.now());
            extractPointRecord.setModifyTime(LocalDateTime.now());

            JSONArray jsonArray = JSONArray.parseArray(data);
            if(jsonArray.size() == 0){
                extractPointRecord.setPoint1X(-999);
                extractPointRecord.setPoint1Y(-999);
                extractPointRecord.setPoint2X(-999);
                extractPointRecord.setPoint2Y(-999);
            }else{
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                JSONArray pointsJsonArray = JSONArray.parseArray(jsonObject.getString("points"));
                JSONObject point1 = pointsJsonArray.getJSONObject(0);
                JSONObject point2 = pointsJsonArray.getJSONObject(1);
                extractPointRecord.setPoint1X(point1.getIntValue("x"));
                extractPointRecord.setPoint1Y(point1.getIntValue("y"));
                extractPointRecord.setPoint2X(point2.getIntValue("x"));
                extractPointRecord.setPoint2Y(point2.getIntValue("y"));
            }
            extractPointMapper.insert(extractPointRecord);
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

        root.put("alarm_type", Collections.singletonList("overlay_image_extract_bbox"));// alarm_type

        List<List<Map<String, Object>>> messages = new ArrayList<>();// 外层 messages

        List<Map<String, Object>> messageGroup = new ArrayList<>();// 中间层 messageGroup（只有一个分组）

        Map<String, Object> message = new HashMap<>();// 构造 message
        message.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<>();

        // device_id
        Map<String, Object> deviceIdMap = new HashMap<>();
        deviceIdMap.put("type", "device_id");
        deviceIdMap.put("device_id", originalAlarmRecord.getDeviceId());
        content.add(deviceIdMap);

        Map<String, Object> imageIdMap = new HashMap<>();
        imageIdMap.put("type", "image_id");
        imageIdMap.put("image_id", originalAlarmRecord.getDeviceId() + "_image_" + System.currentTimeMillis());
        content.add(imageIdMap);

        Map<String, Object> videoIdMap = new HashMap<>();
        videoIdMap.put("type", "video_id");
        videoIdMap.put("videoId", originalAlarmRecord.getDeviceId() + "_video_" + System.currentTimeMillis());
        content.add(videoIdMap);

        Map<String, Object> colorMap = new HashMap<>();
        colorMap.put("type", "bbox_color");
        colorMap.put("bbox_color", getExtractColor(originalAlarmRecord));
        content.add(colorMap);

        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("type", "image_url");
        Map<String, Object> imageUrlValueMap = new HashMap<>();
        imageUrlValueMap.put("url", originalAlarmRecord.getImagePath());
        imageUrlMap.put("image_url", imageUrlValueMap);
        content.add(imageUrlMap);

        Map<String, Object> videoUrlMap = new HashMap<>();
        videoUrlMap.put("type", "image_url");
        Map<String, Object> videoUrlValueMap = new HashMap<>();
        videoUrlValueMap.put("url", originalAlarmRecord.getVideoPath());
        videoUrlMap.put("video_url", videoUrlValueMap);
        content.add(videoUrlMap);

        message.put("content", content);
        messageGroup.add(message);
        messages.add(messageGroup);
        root.put("messages", messages);
        return root;
    }

    /**
     * @desc 根据告警id查询原始告警图框坐标
     * @param alarmId
     * @return
     */
    public ExtractPointRecord getExtractPointByAlarmId(String alarmId) {
        //根据告警记录id查询原始告警抽图对应的框坐标
        QueryWrapper<ExtractPointRecord> extractWrapper = new QueryWrapper<>();
        extractWrapper.eq("alarm_id", alarmId);
        extractWrapper.orderByDesc("create_time");
        extractWrapper.last("limit 1");
        return extractPointMapper.selectOne(extractWrapper);
    }


    /**
     * @desc 获取抽框坐标入参颜色字段
     *          company == null ,返回red;
     *          company == "闪马|彗精髓", 返回red
     *          company == “高通”, 返回blue
     *          其他场景返回red
     *
     * @param originalAlarmRecord
     * @return
     */
    public String getExtractColor(OriginalAlarmRecord originalAlarmRecord) {
        String color = null;
        String company = originalAlarmRecord.getCompany();
        String imageId = originalAlarmRecord.getDeviceId();
        if (company == null || company.isEmpty()) {
            color = "red";
        }else if (company.equals("闪马") || company.equals("彗景")) {
            color = "red";
        }else if (company.equals("高通")) {
            if (imageId.contains("{")) {
                color = "red";
            }else {
                color = "blue";
            }
        }else {
            color = "red";
        }
        return color;
    }
}