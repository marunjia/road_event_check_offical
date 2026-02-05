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
        long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String dealFlag = record.getDealFlag();

        try {
            //查询原始告警记录并进行推送
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id",alarmId);
            jsonObject.put("imagePath",imagePath);
            jsonObject.put("videoPath",videoPath);
            jsonObject.put("confirmStatus",0);//默认为0
            jsonObject.put("dealFlag",dealFlag);

            // 发送 POST 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonObject.toJSONString(), headers);
            String response = restTemplate.postForObject(API_URL, entity, String.class);
            log.info("原始告警记录推送成功，tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
        } catch (Exception e) {
            log.error("原始告警记录推送失败，tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 失败原因:{}", tblId, alarmId, imagePath, videoPath, e);
        }
    }
}