package com.yuce.task;

import com.yuce.service.impl.FeatureElementServiceImpl;
import com.yuce.service.impl.OriginalAlarmServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class ScheduleDeleteTask {

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

//    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点整执行
//    public void deleteHistoryData() {
//
//        @Autowired
//        originalAlarmServiceImpl
//        //获取当前日期
//        LocalDateTime now = LocalDateTime.now();
//
//        //删除原始数据表历史数据
//
//
//
//
//
//        // 查询上一分钟新增数据
//        List<FeatureElementRecord> list = featureElementServiceImpl.getListByTimeRange(previousMinute, currentMinute);
//        if (list.isEmpty()) {
//            log.info("请求时间区间暂无新增告警记录:[{}~{}]", previousMinute, currentMinute);
//            return;
//        }
//
//        // 遍历数据，使用线程池并发调用外部接口
//        for (FeatureElementRecord featureElementRecord : list) {
//            executorService.submit(() -> {
//                try {
//                    String alarmId = featureElementRecord.getAlarmId();
//                    String imagePath = featureElementRecord.getImagePath();
//                    String videoPath = featureElementRecord.getVideoPath();
//                    OriginalAlarmRecord originalAlarmRecord = originalAlarmServiceImpl.getRecordByKey(alarmId, imagePath, videoPath);
//
//                    // 获取帧图片 URL
//                    FrameImageInfo frameImageInfo = frameImageServiceImpl.getFrameByKeyAndNo(alarmId, imagePath, videoPath, 2);
//                    if (frameImageInfo == null || frameImageInfo.getImageUrl() == null) {
//                        log.info("告警记录未查询到抽帧图片: alarmId={}, imagePath={}, videoPath={}", alarmId, imagePath, videoPath);
//                        return;
//                    }
//
//                    String imageId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + originalAlarmRecord.getId() + "_" + 1 + "_" + originalAlarmRecord.getTblId();
//
//                    // 构造请求体
//                    Map<String, Object> requestBody = buildRequestData(frameImageInfo.getImageUrl(), originalAlarmRecord.getDeviceId(),imageId);
//
//                    // 调用接口获取结果
//                    String responseStr = restTemplate.postForObject(url, requestBody, String.class);
//                    if (responseStr == null) {
//                        log.warn("接口返回空: alarmId={}", alarmId);
//                        return;
//                    }
//                    Map<String, String> map = parseResponse(responseStr, featureElementRecord);// 解析返回结果
//                    String fire = map.get("fire");
//                    String weather = map.get("weather");
//                    featureElementRecord.setDangerElement(fire);
//                    featureElementRecord.setWeatherCondition(weather);
//                    featureElementServiceImpl.updateById(featureElementRecord);
//                } catch (Exception e) {
//                    log.error("处理告警记录异常: alarmId={}, error={}", featureElementRecord.getAlarmId(), e.getMessage(), e);
//                }
//            });
//        }
//    }
}