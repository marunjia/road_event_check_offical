package com.yuce.algorithm;

import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.service.impl.CheckAlarmResultServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 通用算法处理服务：负责通用场景下的告警检验、特征提取、告警聚合等全流程处理
 * 核心功能：默认标签检验、自定义标签检验，统一触发后续业务链路
 */
@Slf4j
@Component
public class GeneralAlgorithm {

    @Autowired
    private CheckResultAlgorithm checkResultAlgorithm;

    @Autowired
    private FeatureElementAlgorithm featureElementAlgorithm;

    @Autowired
    private AlarmCollectionAlgorithm alarmCollectionAlgorithm;

    @Autowired
    private CollectionGroupAlgorithm collectionGroupAlgorithm;

    // 依赖注入（按业务顺序排序，提高可读性）
    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;


    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;


    /**
     * 调用通用算法（自定义初检标签）
     * @param record 原始告警记录
     * @param reason 调用通用算法原因
     * @param checkFlag 自定义初检标签（0-无法判断，1-疑似误报，2-正检等）
     */
    public void checkDeal(OriginalAlarmRecord record, String reason, int checkFlag) {

        long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        // 算法初检模块
        checkResultAlgorithm.checkResultDealByGen(record,checkFlag,reason);

        // 处理特征要素（调用通用特征处理方法）
        featureElementAlgorithm.featureElementDealByGen(record, checkFlag);

        // 告警集逻辑处理
        alarmCollectionAlgorithm.collectionDeal(record);

        // 告警组逻辑处理
        collectionGroupAlgorithm.groupDeal(record);
        log.info("通用算法检测完成 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
    }
}