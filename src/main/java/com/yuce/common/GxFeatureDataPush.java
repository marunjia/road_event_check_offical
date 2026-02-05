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
public class GxFeatureDataPush {

    @Autowired
    private AlarmCollectionServiceImpl alarmCollectionServiceImpl;

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private FeatureElementServiceImpl featureElementServiceImpl;

    private static final String API_URL = "http://12.1.150.27:6080/nest/preCheck/receive";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 推送告警集数据到高信接口
     */
    public void pushToGx(OriginalAlarmRecord record) {
        try {
            // 查询告警集
            AlarmCollection alarmCollection = alarmCollectionServiceImpl.getCollectionByTblId(record.getTblId());
            if (alarmCollection == null) {
                log.warn("未找到alarmId->{},image_path->{},video_path->{}对应的告警集", record.getTblId());
                return;
            }

            // 获取最早记录
            List<String> relatedIdList = Arrays.asList(alarmCollection.getRelatedTblIdList().split(","));
            long earliestTblId = Integer.valueOf(relatedIdList.get(0));
            OriginalAlarmRecord earliestRecord = originalAlarmServiceImpl.getRecordByTblId(earliestTblId);

            // 主告警集 JSON
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", alarmCollection.getId());
            jsonObject.put("eventId", earliestRecord.getEventId());
            jsonObject.put("inferEventTypeName", alarmCollection.getEventType());
            jsonObject.put("suggestion", alarmCollection.getDisposalAdvice());
            jsonObject.put("preCheckTag", null);
            jsonObject.put("confirmStatus", null);
            jsonObject.put("alarmNum", alarmCollection.getRelatedAlarmNum());
            jsonObject.put("alarmTime", alarmCollection.getEarliestAlarmTime());

            String[] parts = earliestRecord.getContent().split(" ");
            jsonObject.put("alarmPosition", parts.length >= 3 ? parts[0] + " " + parts[1] + " " + parts[2] : earliestRecord.getContent());
            jsonObject.put("roadId", alarmCollection.getRoadId());
            jsonObject.put("direction", earliestRecord.getDirection());
            jsonObject.put("directionDes", earliestRecord.getDirectionDes());
            jsonObject.put("milestoneMeter", earliestRecord.getMilestone());
            jsonObject.put("imagePath", earliestRecord.getImagePath());
            jsonObject.put("videoPath", earliestRecord.getVideoPath());
            jsonObject.put("preCheckCreateTime", alarmCollection.getCreateTime());
            jsonObject.put("preCheckUpdateTime", alarmCollection.getModifyTime());

            // 子告警记录数组
            JSONArray subAlarmArray = new JSONArray();
            List<OriginalAlarmRecord> list = originalAlarmServiceImpl.getListByTblIdList(relatedIdList);
            for (OriginalAlarmRecord relatedRecord : list) {
                JSONObject subJsonObject = new JSONObject();
                subJsonObject.put("id", relatedRecord.getId());
                subJsonObject.put("eventId", relatedRecord.getEventId());
                subJsonObject.put("preCheckId", null);
                subJsonObject.put("milestone", relatedRecord.getMilestone());
                subJsonObject.put("milestoneMeter", relatedRecord.getEndMilestone());
                subJsonObject.put("imagePath", relatedRecord.getImagePath());
                subJsonObject.put("videoPath", relatedRecord.getVideoPath());
                subJsonObject.put("content", relatedRecord.getContent());
                subJsonObject.put("alarmTime", relatedRecord.getAlarmTime());
                subJsonObject.put("eventType", relatedRecord.getEventType());
                subJsonObject.put("dealFlag", relatedRecord.getDealFlag());
                subJsonObject.put("longitude", relatedRecord.getLongitude());
                subJsonObject.put("latitude", relatedRecord.getLatitude());
                subJsonObject.put("direction", relatedRecord.getDirection());
                subJsonObject.put("directionDes", relatedRecord.getDirectionDes());
                subJsonObject.put("laneIndex", relatedRecord.getLaneIndex());
                subJsonObject.put("source", relatedRecord.getSource());
                subJsonObject.put("deviceId", relatedRecord.getDeviceId());
                subJsonObject.put("nameInp", relatedRecord.getNameInp());
                subJsonObject.put("companyId", relatedRecord.getCompanyId());
                subJsonObject.put("company", relatedRecord.getCompany());
                subJsonObject.put("modifyTime", relatedRecord.getModifyTimeSys());

                FeatureElementRecord featureElementRecord = featureElementServiceImpl.getFeatureByTblId(relatedRecord.getTblId());
                if (featureElementRecord != null) {
                    subJsonObject.put("adviceReason", featureElementRecord.getAdviceReason());
                    subJsonObject.put("disposalAdvice", featureElementRecord.getDisposalAdvice());
                }
                subAlarmArray.add(subJsonObject);
            }

            jsonObject.put("alarmList", subAlarmArray);
            jsonArray.add(jsonObject);
            // 发送 POST 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            //log.info("请求结果:{}",jsonArray.toJSONString());
            HttpEntity<String> entity = new HttpEntity<>(jsonArray.toJSONString(), headers);
            String response = restTemplate.postForObject(API_URL, entity, String.class);
            log.info("推送成功，响应结果: {}", response);
        } catch (Exception e) {
            log.error("推送广信接口失败", e);
        }
    }
}