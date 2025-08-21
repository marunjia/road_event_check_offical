package com.yuce.algorithm;

import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmResultMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 通用算法处理服务：负责通用场景下的告警检验、特征提取、告警聚合等全流程处理
 * 核心功能：默认标签检验、自定义标签检验，统一触发后续业务链路
 */
@Slf4j
@Component
public class GeneralAlgorithm {

    // 常量定义（通用算法来源标识，避免硬编码）
    private static final String CHECK_SOURCE = "通用算法";
    // 默认初检标签（无法判断）
    private static final int DEFAULT_CHECK_FLAG = 0;

    // 依赖注入（按业务顺序排序，提高可读性）
    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private FeatureElementAlgorithm featureElementAlgorithm;

    @Autowired
    private AlarmCollectionAlgorithm alarmCollectionAlgorithm;

    @Autowired
    private CollectionGroupAlgorithm collectionGroupAlgorithm;


    /**
     * 调用通用算法（默认初检标签：0-无法判断）
     * @param record 原始告警记录
     * @param reason 处理原因（建议非空，便于追溯）
     */
    public void checkDeal(OriginalAlarmRecord record, String reason) {
        // 复用自定义标签方法，默认标签传0
        checkDeal(record, reason, DEFAULT_CHECK_FLAG);
    }

    /**
     * 调用通用算法（自定义初检标签）
     * @param record 原始告警记录
     * @param reason 处理原因（建议非空，便于追溯）
     * @param checkFlag 自定义初检标签（0-无法判断，1-疑似误报，2-正检等）
     */
    public void checkDeal(OriginalAlarmRecord record, String reason, int checkFlag) {
        // 1. 提取核心字段并校验非空（避免空指针）
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        if (!validateCoreFields(alarmId, imagePath, videoPath)) {
            return;
        }

        // 2. 日志标准化输出（包含核心参数，便于排查）
        log.info("通用算法检验开始 | 类型:{} | alarmId:{} | imagePath:{} | videoPath:{} | reason:{} | checkFlag:{}",
                (checkFlag == DEFAULT_CHECK_FLAG ? "默认标签" : "自定义标签"),
                alarmId, imagePath, videoPath, reason, checkFlag);

        // 3. 检查是否已处理（避免重复入库和后续流程）
        if (isCheckResultExists(alarmId, imagePath, videoPath)) {
            log.info("通用算法已处理，跳过 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
            return;
        }

        try {
            // 4. 统一执行处理流程（封装为私有方法，减少重复代码）
            processAlarm(record, alarmId, imagePath, videoPath, reason, checkFlag);
            log.info("通用算法检验完成 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
        } catch (Exception e) {
            // 5. 异常捕获与日志记录（避免流程中断，便于问题定位）
            log.error("通用算法处理异常 | alarmId:{} | imagePath:{} | videoPath:{}，异常详情:", alarmId, imagePath, videoPath, e);
            // 可根据业务需求添加重试机制或告警通知
        }
    }


    /**
     * 核心处理流程：封装初检结果入库、抠图、特征提取、告警聚合等步骤
     */
    private void processAlarm(OriginalAlarmRecord record, String alarmId, String imagePath, String videoPath,
                              String reason, int checkFlag) {
        // 4.1 保存初检结果
        CheckAlarmResult checkResult = buildCheckAlarmResult(alarmId, imagePath, videoPath, checkFlag);
        checkAlarmResultMapper.insert(checkResult);

        // 4.2 处理抠图逻辑：通用算法不抠图
        //extractImageAlgorithm.extractImage(record);

        // 4.3 处理特征要素（调用通用特征处理方法）
        featureElementAlgorithm.featureElementDealByGen(record, checkFlag, reason);

        // 4.4 告警集逻辑处理
        alarmCollectionAlgorithm.collectionDeal(record);

        // 4.5 告警组逻辑处理
        collectionGroupAlgorithm.groupDeal(record);
    }

    /**
     * 校验核心字段非空（alarmId、imagePath、videoPath为流程必需）
     */
    private boolean validateCoreFields(String alarmId, String imagePath, String videoPath) {
        if (!StringUtils.hasText(alarmId)) {
            log.error("通用算法处理失败：告警ID为空");
            return false;
        }
        if (!StringUtils.hasText(imagePath)) {
            log.error("通用算法处理失败：图片路径为空 | alarmId:{}", alarmId);
            return false;
        }
        if (!StringUtils.hasText(videoPath)) {
            log.error("通用算法处理失败：视频路径为空 | alarmId:{} | imagePath:{}", alarmId, imagePath);
            return false;
        }
        return true;
    }

    /**
     * 检查初检结果是否已存在（避免重复处理）
     */
    private boolean isCheckResultExists(String alarmId, String imagePath, String videoPath) {
        try {
            return checkAlarmResultMapper.getResultByKey(alarmId, imagePath, videoPath) != null;
        } catch (Exception e) {
            log.error("查询初检结果异常，默认按未处理继续 | alarmId:{}", alarmId, e);
            return false; // 查库异常时继续处理，避免因查询失败阻塞流程
        }
    }

    /**
     * 构建初检结果对象（封装对象创建逻辑，避免重复代码）
     */
    private CheckAlarmResult buildCheckAlarmResult(String alarmId, String imagePath, String videoPath, int checkFlag) {
        CheckAlarmResult result = new CheckAlarmResult();
        result.setAlarmId(alarmId);
        result.setImagePath(imagePath);
        result.setVideoPath(videoPath);
        result.setCheckFlag(checkFlag);
        result.setCheckSource(CHECK_SOURCE);
        result.setCheckTime(LocalDateTime.now());
        return result;
    }
}