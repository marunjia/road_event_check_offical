package com.yuce.algorithm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.FrameImageInfo;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmProcessMapper;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.mapper.FrameImageMapper;
import com.yuce.service.impl.CheckAlarmResultServiceImpl;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @ClassName VideoDealService
 * @Description 抛洒物算法服务
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/15 11:02
 * @Version 1.0
 */

@Slf4j
@Component
public class PswAlgorithm {

    @Autowired
    private FrameImageMapper frameImageMapper;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private CheckAlarmProcessMapper checkAlarmProcessMapper;

    @Autowired
    private GeneralAlgorithm generalAlgorithm;

    //定义接口请求访问地址
    private static final String PSW_URL = "http://12.1.97.206:9991/isps/batch_completions";

    //定义客户端对象
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 调用算法接口
     * @param record
     * @return boolean
     */
    public boolean pswDeal(OriginalAlarmRecord record) {

        Long id = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        CheckAlarmResult checkAlarmResult = checkAlarmResultMapper.getResultByKey(alarmId,imagePath,videoPath);
        if(checkAlarmResult != null) {
            log.info("抛洒物算法已检过：id->{}, alarmId->{}, imagePath->{}, videoPath->{}", id, alarmId, imagePath, videoPath);
            return true;
        }

        Map<String, Object> requestData = buildRequestData(record);
        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(requestData);
        } catch (IOException e) {
            log.error("JSON 序列化失败", e);
            return false;
        }

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(PSW_URL).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            log.info("调用抛洒物算法检验:alarmId -> {}, 请求体内容 -> {}，返回码 -> {}, 返回体 -> {}, ", alarmId,jsonBody,response.code(),response.body());
            if (response != null && response.code() == 200) {
                return true;
            } else {
                return false;
            }
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
        Map<String, Object> root = new HashMap<>();// 构建根对象
        root.put("conf", 0.8);
        root.put("alarm_type", Collections.singletonList("abandon"));

        List<Map<String, Object>> imageGroup = new ArrayList<>();// 构建单个 message group（包含多张图片）

        List<FrameImageInfo> list = frameImageMapper.getFrameListByKey(record.getId(), record.getImagePath(), record.getVideoPath() );//查询抽帧图片

        // 遍历图片，构建每一张的 message 内容
        Set<String> dedupSet = new HashSet<>(); // 用于去重 image_id（可选）
        for (FrameImageInfo imageInfo : list) {
            String imageId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + record.getId() + "_" + imageInfo.getImageSortNo() + "_" + record.getTblId();
            if (!dedupSet.add(imageId)) {
                continue; // 已添加过，跳过重复
            }

            Map<String, Object> message = new HashMap<>();
            List<Map<String, Object>> content = new ArrayList<>();

            // device_id
            Map<String, Object> deviceIdMap = new HashMap<>();
            deviceIdMap.put("type", "device_id");
            deviceIdMap.put("device_id", record.getDeviceId());
            content.add(deviceIdMap);

            // image_id
            Map<String, Object> imageIdMap = new HashMap<>();
            imageIdMap.put("type", "image_id");
            imageIdMap.put("image_id", imageId);
            content.add(imageIdMap);

            // image_url
            Map<String, Object> imageUrlMap = new HashMap<>();
            imageUrlMap.put("type", "image_url");
            Map<String, Object> imageUrlContent = new HashMap<>();
            imageUrlContent.put("url", imageInfo.getImageUrl());
            imageUrlMap.put("image_url", imageUrlContent);
            content.add(imageUrlMap);

            message.put("content", content);
            imageGroup.add(message);
        }

        // 构建 messages（外层包一层）
        List<List<Map<String, Object>>> messages = new ArrayList<>();
        messages.add(imageGroup);

        root.put("messages", messages);
        return root;
    }
}