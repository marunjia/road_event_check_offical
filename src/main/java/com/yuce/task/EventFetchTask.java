package com.yuce.task;

import com.alibaba.fastjson.JSONObject;
import com.yuce.algorithm.*;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.handler.MessageHandler;
import com.yuce.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;

/**
 * 多消费者并行处理事件捕获任务
 * 支持按配置数量启动消费者，每个消费者独立处理消息并管理偏移量
 */
@Slf4j
@Component
public class EventFetchTask implements MessageHandler {

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private PswAlgorithm pswAlgorithm;

    @Autowired
    private GeneralAlgorithm generalAlgorithm;

    @Autowired
    private PersonAlgorithm personAlgorithm;

    @Autowired
    private VehicleAlgorithm vehicleAlgorithm;

    @Autowired
    private ExtractFrameAlgorithm extractFrameAlgorithm;

    @Autowired
    private ExtractWindowAlgorithm extractWindowAlgorithm;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private FeatureElementAlgorithm featureElementAlgorithm;

    @Autowired
    private AlarmCollectionAlgorithm alarmCollectionAlgorithm;

    @Autowired
    private ExtractImageAlgorithm extractImageAlgorithm;

    @Autowired
    private CollectionGroupAlgorithm collectionGroupAlgorithm;



    // 业务核心处理方法，只负责业务逻辑
    public void handleMessage(ConsumerRecord<String, String> record) {
        //topic value字段包含特殊字符，非标准json，需要进行格式化处理再映射为OriginalAlarmRecord
        OriginalAlarmRecord alarmRecord = JSONObject.parseObject((record.value().replaceAll("^\"|\"$", "").replace("\\", "")),OriginalAlarmRecord.class);

        String eventType = alarmRecord.getEventType();//告警类型
        String alarmId = alarmRecord.getId();//告警id
        String roadId = alarmRecord.getRoadId();//告警所属道路id
        String imagePath = alarmRecord.getImagePath();//告警记录对应图片地址
        String videoPath = alarmRecord.getVideoPath();//告警记录对应视频地址
        log.info("接受原始告警记录:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{},offset->{}",roadId, alarmId, eventType, imagePath, videoPath,record.offset());

        //筛选目标道路：G33141、G33112
        if(!("33141".equals(roadId) || "33112".equals(roadId))){
            log.info("路段范围校验失败:roadId->{},alarmId->{},imagePath->{},videoPath->{}",roadId, alarmId, imagePath, videoPath);
            return;
        }

        //筛选告警类型：停驶、行人、抛洒物
        if(!("停驶".equals(eventType) || "行人".equals(eventType) || "抛洒物".equals(eventType))){
            log.info("告警类型校验失败:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}",roadId, alarmId, eventType, imagePath, videoPath);
            return;
        }

//        String alarmDate = alarmRecord.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        if (alarmDate.compareTo("20250725") < 0 || alarmDate.compareTo("20250725") > 0) {
//            log.info("告警时间监控失败:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
//            return;
//        }

        //筛选异常记录
        if(null == imagePath || imagePath.equals("") || null == videoPath || videoPath.equals("") || null == eventType || eventType.equals("")){
            log.info("字段筛选失败:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}",roadId, alarmId, eventType, imagePath, videoPath);
            return;
        }

        //告警记录存储逻辑：已接受过数据不再处理，仅做原始记录更新
        if (originalAlarmServiceImpl.existsByKey(alarmId, imagePath, videoPath)) {
            log.info("告警已检测:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
            originalAlarmServiceImpl.updateByKey(alarmRecord);
            return;
        }else{
            originalAlarmServiceImpl.insert(alarmRecord);
            log.info("新增告警记录:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
        }

        //视频抽帧服务
        if(!extractFrameAlgorithm.extractImage(alarmRecord)) {
            log.info("告警记录抽帧失败:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
            generalAlgorithm.checkDeal(alarmRecord,"视频抽帧失败");//调用通用算法处理
            return;
        }

        //图片提框服务
        if(!extractWindowAlgorithm.extractWindow(alarmRecord)) {
            log.info("告警记录提框失败:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
            generalAlgorithm.checkDeal(alarmRecord, "图片提框失败");//调用通用算法处理
            return;
        }

        if("抛洒物".equals(eventType)) {
            if (!pswAlgorithm.pswDeal(alarmRecord)) {
                log.info("抛洒物处理失败，调用通用算法处理:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
                generalAlgorithm.checkDeal(alarmRecord, "抛洒物算法处理失败");
            } else {
                log.info("抛洒物处理成功:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
            }
            return;
        }

        if ("行人".equals(eventType)) {
            if (!personAlgorithm.personDeal(alarmRecord)) {
                log.info("行人处理失败，调用通用算法处理:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
                generalAlgorithm.checkDeal(alarmRecord, "行人算法处理失败");
            } else {
                log.info("行人处理成功:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
                checkAlarmResultServiceImpl.checkResultByImgNum(alarmRecord, "person");
                extractImageAlgorithm.extractImage(alarmRecord);//抠图
                featureElementAlgorithm.featureElementDealByAlgo(alarmRecord);//特征要素判定
                alarmCollectionAlgorithm.collectionDeal(alarmRecord);//告警集判定
                collectionGroupAlgorithm.groupDeal(alarmRecord);//告警组判定
            }
            return;
        }

        if ("停驶".equals(eventType)) {
            if (!vehicleAlgorithm.vehicleDeal(alarmRecord)) {
                log.info("停驶处理失败，调用通用算法处理:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
                generalAlgorithm.checkDeal(alarmRecord,"停驶算法处理失败");
            } else {
                log.info("停驶处理成功:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
                checkAlarmResultServiceImpl.checkResultByIou(alarmRecord, 0.2, 1);
                extractImageAlgorithm.extractImage(alarmRecord);//抠图
                featureElementAlgorithm.featureElementDealByAlgo(alarmRecord);//特征要素判定
                alarmCollectionAlgorithm.collectionDeal(alarmRecord);//告警集判定
                collectionGroupAlgorithm.groupDeal(alarmRecord);//告警组判定
            }
            return;
        }
        log.info("other exception，请检查告警记录:roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
    }
}