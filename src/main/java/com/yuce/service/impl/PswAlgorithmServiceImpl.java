package com.yuce.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.FrameImageInfo;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.AlgorithmService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
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
public class PswAlgorithmServiceImpl implements AlgorithmService {

    @Autowired
    FrameImageServiceImpl frameImageServiceImpl;

    @Autowired
    CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    GeneralAlgorithmServiceImpl generalAlgorithmServiceImpl;

    //定义接口请求访问地址
    private static final String PSW_URL = "http://12.1.97.206:9991/isps/batch_completions";

    //定义客户端对象
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 调用算法接口
     * @param originalAlarmRecord
     * @return boolean
     */
    public boolean pswDeal(OriginalAlarmRecord originalAlarmRecord) {
        log.info("抛洒物算法检验:{}", originalAlarmRecord.getId());
        Map<String, Object> requestData = buildRequestData(originalAlarmRecord); // 封装请求体
        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(requestData);
        } catch (IOException e) {
            return false;
        }

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(PSW_URL).post(body).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("算法请求失败（HTTP错误）: status={}", response.code());
                return false;
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
        // 构建根对象
        Map<String, Object> root = new HashMap<>();
        root.put("conf", 0.8);
        root.put("alarm_type", Collections.singletonList("abandon"));

        // 构建单个 message group（包含多张图片）
        List<Map<String, Object>> imageGroup = new ArrayList<>();

        List<FrameImageInfo> list = frameImageServiceImpl.getListByAlarmId(originalAlarmRecord.getId());

        // 遍历图片，构建每一张的 message 内容
        Set<String> dedupSet = new HashSet<>(); // 用于去重 image_id（可选）
        for (FrameImageInfo imageInfo : list) {
            String imageId = originalAlarmRecord.getId() + "_" + imageInfo.getImageSortNo();
            if (!dedupSet.add(imageId)) {
                continue; // 已添加过，跳过重复
            }

            Map<String, Object> message = new HashMap<>();
            List<Map<String, Object>> content = new ArrayList<>();

            // device_id
            Map<String, Object> deviceIdMap = new HashMap<>();
            deviceIdMap.put("type", "device_id");
            deviceIdMap.put("device_id", originalAlarmRecord.getDeviceId());
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