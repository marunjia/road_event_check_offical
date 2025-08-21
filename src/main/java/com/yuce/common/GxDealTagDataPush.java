package com.yuce.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yuce.entity.AlarmCollection;
import com.yuce.entity.FeatureElementRecord;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.impl.AlarmCollectionServiceImpl;
import com.yuce.service.impl.FeatureElementServiceImpl;
import com.yuce.service.impl.OriginalAlarmServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class GxDealTagDataPush {

    private static final String API_URL = "http://12.1.150.27:6080/nest/alarm/updateConfirmStatus";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 推送告警集数据到高信接口
     */
    public void pushToGx(OriginalAlarmRecord record) {
        try {
            //查询原始告警记录并进行推送
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id",record.getId());
            jsonObject.put("imagePath",record.getImagePath());
            jsonObject.put("videoPath",record.getVideoPath());
            jsonObject.put("confirmStatus",0);//默认为0
            jsonObject.put("dealFlag",record.getDealFlag());

            // 发送 POST 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonObject.toJSONString(), headers);
            String response = restTemplate.postForObject(API_URL, entity, String.class);
            log.info("处理标识数据推送成功，响应结果: {}", response);
        } catch (Exception e) {
            log.error("推送广信接口失败", e);
        }
    }
}