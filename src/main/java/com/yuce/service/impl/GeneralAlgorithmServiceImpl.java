package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.service.GeneralAlgorithmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
@Service
public class GeneralAlgorithmServiceImpl implements GeneralAlgorithmService {

    private final int checkFlag = 0;//无法判断

    @Autowired
    private CollectionServiceImpl collectionServiceImpl;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private FeatureElementServiceImpl featureElementServiceImpl;

    public void checkDeal(String alarmId){

        /**
         * 不存在则更新
         * 存在则丢弃
         */
        if (!checkAlarmResultServiceImpl.existsJudgeByAlarmId(alarmId)) {
            log.info("通用算法检验：{}",alarmId);
            CheckAlarmResult checkAlarmResult = new CheckAlarmResult();
            checkAlarmResult.setAlarmId(alarmId);
            checkAlarmResult.setCheckFlag(checkFlag);
            checkAlarmResult.setCheckTime(LocalDateTime.now());
            checkAlarmResult.setCreateTime(LocalDateTime.now());
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResultServiceImpl.save(checkAlarmResult);

            //处理特征要素
            featureElementServiceImpl.featureElementDeal(alarmId);

            // 处理告警集逻辑
            collectionServiceImpl.collectionDeal(alarmId);
        }else{
            log.info("通用算法已检验，忽略记录：{}",alarmId);
        }
    }
}