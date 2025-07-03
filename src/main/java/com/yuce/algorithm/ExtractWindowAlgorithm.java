package com.yuce.algorithm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.*;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.mapper.ExtractWindowMapper;
import com.yuce.service.ExtractWindowService;
import com.yuce.service.impl.CheckAlarmProcessServiceImpl;
import com.yuce.service.impl.CheckAlarmResultServiceImpl;
import com.yuce.service.impl.ExtractWindowServiceImpl;
import com.yuce.service.impl.FrameImageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @ClassName ExtractWindowServiceImpl
 * @Description 提框算法
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/15 11:02
 * @Version 1.0
 */

@Service
@Slf4j
public class ExtractWindowAlgorithm extends ServiceImpl<ExtractWindowMapper, ExtractWindowRecord> implements ExtractWindowService {

    @Autowired
    private FrameImageServiceImpl frameImageServiceImpl;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    private ExtractWindowServiceImpl extractWindowServiceImpl;

    @Autowired
    private GeneralAlgorithm generalAlgorithm;

    //定义接口请求访问地址
    private static final String EXTRACT_POINT_URL = "http://12.1.97.206:7870/extract";

    //定义客户端对象
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 调用提框
     * @param record
     * @return boolean
     */
    @Override
    public boolean extractWindow(OriginalAlarmRecord record) {

        Long id = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        if(extractWindowServiceImpl.existsByKey(alarmId,imagePath,videoPath)) {
            log.info("告警记录已提框：id->{}, alarmId->{}, imagePath->{}, videoPath->{}", id, alarmId, imagePath, videoPath);
            return true;
        }

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(buildRequestData(record));
        } catch (IOException e) {
            log.error("请求体序列化失败", e);
            return false;
        }

        // 构造请求
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(EXTRACT_POINT_URL).post(body).build();

        // 执行同步请求
        try (Response response = client.newCall(request).execute()) {
            if (response == null || !response.isSuccessful()) {
                log.error("提框服务接口返回非成功状态: code={}, alarm_id->{}, image_path->{}, video_path->{}", response != null ? response.code() : "null",alarmId, imagePath, videoPath);
                return false;
            }

            // 关键：只读取一次响应体，并确保日志中不重复调用
            String responseBody = response.body() != null ? response.body().string() : null;
            log.info("调用提框服务:alarmId -> {}, 请求体内容 -> {}, 返回体 -> {}", alarmId, jsonBody, responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                return false;
            }

            // 解析为 JSON 数组
            JSONObject resutlJsonObject = JSONArray.parseArray(responseBody).getJSONObject(0);
            String imageId = resutlJsonObject.getString("image_id");
            int status = resutlJsonObject.getIntValue("status");
            String data = resutlJsonObject.getString("data");

            ExtractWindowRecord extractPointRecord = new ExtractWindowRecord();
            extractPointRecord.setAlarmId(record.getId());
            extractPointRecord.setImagePath(record.getImagePath());
            extractPointRecord.setVideoPath(record.getVideoPath());
            extractPointRecord.setImageId(imageId);
            extractPointRecord.setStatus(status);
            extractPointRecord.setReceivedTime(LocalDateTime.now());
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
            extractWindowServiceImpl.insertWindow(extractPointRecord);
            return true;
        } catch (IOException e) {
            log.error("算法请求异常", e);
            return false;
        }
    }

    /**
     * 构造请求体 Map 数据
     * @param record
     * @return 请求体 Map
     */
    public Map<String, Object> buildRequestData(OriginalAlarmRecord record) {
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
        deviceIdMap.put("device_id", record.getDeviceId());
        content.add(deviceIdMap);

        Map<String, Object> imageIdMap = new HashMap<>();
        imageIdMap.put("type", "image_id");
        imageIdMap.put("image_id", record.getDeviceId() + "_image_" + System.currentTimeMillis());
        content.add(imageIdMap);

        Map<String, Object> videoIdMap = new HashMap<>();
        videoIdMap.put("type", "video_id");
        videoIdMap.put("video_id", record.getDeviceId() + "_video_" + System.currentTimeMillis());
        content.add(videoIdMap);

        Map<String, Object> colorMap = new HashMap<>();
        colorMap.put("type", "bbox_color");
        colorMap.put("bbox_color", getExtractColor(record));
        content.add(colorMap);

//        Map<String, Object> typeMap = new HashMap<>();
//        colorMap.put("type", "bbox_type");
//        colorMap.put("bbox_type", getExtractWindowType(record));
//        content.add(typeMap);

        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("type", "image_url");
        Map<String, Object> imageUrlValueMap = new HashMap<>();
        imageUrlValueMap.put("url", record.getImagePath());
        imageUrlMap.put("image_url", imageUrlValueMap);
        content.add(imageUrlMap);

        Map<String, Object> videoUrlMap = new HashMap<>();
        videoUrlMap.put("type", "video_url");
        Map<String, Object> videoUrlValueMap = new HashMap<>();
        videoUrlValueMap.put("url", record.getVideoPath());
        videoUrlMap.put("video_url", videoUrlValueMap);
        content.add(videoUrlMap);

        message.put("content", content);
        messageGroup.add(message);
        messages.add(messageGroup);
        root.put("messages", messages);
        return root;
    }

    /**
     * @desc 获取抽框坐标入参颜色字段
     *          company == null ,返回red;
     *          company == "闪马|彗精髓", 返回red
     *          company == “高通”, 返回blue
     *          其他场景返回red
     *
     * @param record
     * @return
     */
    public String getExtractColor(OriginalAlarmRecord record) {
        String color = null;
        String company = record.getCompany();
        String imageId = record.getImagePath();
        if (company == null || company.isEmpty()) {
            color = "red";
        }else if (company.equals("闪马") || company.equals("彗景")) {
            color = "red";
        }else if (company.equals("高瞳")) {
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

    /**
     * @desc 获取抽框坐标框类型字段
     *       company == "之江智能"
     *           eventType == "抛洒物"，返回zhijiang_paosawu
     *           eventType != "抛洒物"，返回zhijiang
     *       company == “高瞳”, 返回gaotong
     *       其他场景返回normal
     * @param record
     * @return
     */
    public static String getExtractWindowType(OriginalAlarmRecord record) {
        String bboxType = "normal";
        String company = record.getCompany();
        String eventType = record.getEventType();

        if(company.equals("高瞳")) {
            bboxType = "gaotong";
        }

        if(company.equals("之江智能")){
            if(eventType.equals("抛洒物")){
                bboxType = "zhijiang_paosawu";
            }else{
                bboxType = "zhijiang";
            }
        }
        return bboxType;
    }
}