package com.yuce.algorithm;

import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.service.impl.CheckAlarmResultServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * @ClassName GeneralAlgorithmServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/28 14:47
 * @Version 1.0
 */

@Slf4j
@Component
public class GeneralAlgorithm {

    private final int checkFlag = 0;//无法判断

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private FeatureElementAlgorithm featureElementAlgorithm;

    @Autowired
    private AlarmCollectionAlgorithm alarmCollectionAlgorithm;

    @Autowired
    private CollectionGroupAlgorithm collectionGroupAlgorithm;

    public void checkDeal(OriginalAlarmRecord record, String reason){

        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        log.info("调用通用算法检验：alarmId->{}, imagePath->{}, videoPath->{}, reason->{}",alarmId, imagePath, videoPath, reason);

        if (checkAlarmResultMapper.getResultByKey(alarmId,imagePath,videoPath) == null) {
            CheckAlarmResult checkAlarmResult = new CheckAlarmResult();
            checkAlarmResult.setAlarmId(alarmId);
            checkAlarmResult.setImagePath(imagePath);
            checkAlarmResult.setVideoPath(videoPath);
            checkAlarmResult.setCheckFlag(checkFlag);
            checkAlarmResult.setCheckTime(LocalDateTime.now());
            checkAlarmResultMapper.insert(checkAlarmResult);

            featureElementAlgorithm.featureElementDealByGen(record, reason);//处理特征要素
            alarmCollectionAlgorithm.collectionDeal(record);// 告警集逻辑处理
            collectionGroupAlgorithm.groupDeal(record);//告警组逻辑处理

        }else{
            log.info("算法已检验：alarmId->{}, imagePath->{}, videoPath->{}, reason->{}",alarmId, imagePath, videoPath, reason);
        }
    }
}