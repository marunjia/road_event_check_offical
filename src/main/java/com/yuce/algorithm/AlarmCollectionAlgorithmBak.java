package com.yuce.algorithm;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.common.GxFeatureDataPush;
import com.yuce.entity.*;
import com.yuce.mapper.AlarmCollectionMapper;
import com.yuce.service.AlarmCollectionService;
import com.yuce.service.impl.*;
import com.yuce.util.FlagTagUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警集算法服务：负责告警集的创建、更新、类型推定及处置建议生成
 * 修复点：
 * 1. 修复handleExistingDeviceCollection方法的IndexOutOfBoundsException异常
 * 2. 增加全链路防御性判空逻辑
 * 3. 完善异常兜底和日志打印
 */
@Component
@Slf4j
public class AlarmCollectionAlgorithmBak extends ServiceImpl<AlarmCollectionMapper, AlarmCollection> implements AlarmCollectionService {

    // ------------------------------ 依赖注入 ------------------------------
    @Autowired
    private AlarmCollectionServiceImpl alarmCollectionServiceImpl;

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private FeatureElementServiceImpl featureElementServiceImpl;

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private CollectionDurationConfigServiceImpl durationConfigServiceImpl;

    @Autowired
    private GxFeatureDataPush gxFeatureDataPush;


    // ------------------------------ 核心业务方法 ------------------------------
    /**
     * 告警集处理主逻辑：
     * 1、tblId是否已存在
     *      已存在：告警记录已合并告警集，终止后续处理
     *      不存在：继续后续处理逻辑
     * 2、alarmId是否已存在对应告警集
     *      已存在：将当前告警记录合并到对应告警集
     *      不存在：继续后续处理逻辑
     * 3、deviceId点位是否存在告警集
     *      已存在：继续后续处理
     *      不存在：新建告警集
     * 4、判断告警类型：
     *      行人告警：判断时间间隔是否小于10分钟，如果小于10分钟则合并进告警集，如果大于10分钟则新创建告警集；
     *      抛洒物告警：判断时间间隔是否小于10分钟，如果小于10分钟则合并进告警集，如果大于10分钟则新创建告警集；
     *      停驶告警：如果上条告警记录告警类型非停驶，判断时间间隔是否小于10分钟，如果小于10分钟，则直接合并进告警集；
     *              如果上条记录为停驶，则判断时间间隔是否小于30分钟且(桩米号一致或桩米号间隔不超过15米）则直接合并进告警集；
     */
    public void collectionDeal(OriginalAlarmRecord record) {
        // 防御性判空
        if (record == null) {
            log.error("告警记录为null，终止告警集处理");
            return;
        }

        // 提取核心字段（增加非空默认值）
        Long tblId = record.getTblId() == null ? -1L : record.getTblId();
        String alarmId = record.getId() == null ? "" : record.getId();
        String imagePath = record.getImagePath() == null ? "" : record.getImagePath();
        String videoPath = record.getVideoPath() == null ? "" : record.getVideoPath();
        String roadId = record.getRoadId() == null ? "" : record.getRoadId();
        String deviceId = record.getDeviceId() == null ? "" : record.getDeviceId();
        LocalDateTime alarmTime = record.getAlarmTime() == null ? LocalDateTime.now() : record.getAlarmTime();
        String eventType = record.getEventType() == null ? "" : record.getEventType();
        int milestone = record.getMilestone() == 0 ? 0 : record.getMilestone();

        log.info("告警记录进行告警集逻辑处理 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | eventType:{} | alarmTime:{}",
                tblId, alarmId, imagePath, videoPath, eventType, alarmTime);

        //获取检测结果标签（增加非空处理）
        int checkFlag = FlagTagUtil.CHECK_RESULT_UNKNOWN;
        try {
            CheckAlarmResult checkResult = checkAlarmResultServiceImpl.getResultByTblId(tblId);
            if (checkResult != null) {
                checkFlag = checkResult.getCheckFlag();
            }
        } catch (Exception e) {
            log.error("获取检测结果标签失败 | tblId:{} | 异常原因:{}", tblId, e.getMessage(), e);
        }

        // 解析设备名称
        String deviceName = parseDeviceName(record.getContent());

        if ("非法车辆".equals(eventType)) {
            deviceId = "无固定点位";
            deviceName = "无固定点位名称";
        }

        //根据alarmId查询是否已经归属告警集，如果归属则将当前记录直接列入对应告警集
        try {
            AlarmCollection existingByAlarmId = alarmCollectionServiceImpl.getCollectionByAlarmId(alarmId);
            if (existingByAlarmId != null) {
                // 已归属：更新告警集（追加当前告警）
                int rightCheckNum = existingByAlarmId.getRightCheckNum() == null ? 0 : existingByAlarmId.getRightCheckNum();
                if (checkFlag == FlagTagUtil.CHECK_RESULT_RIGHT) {
                    rightCheckNum = rightCheckNum + 1;
                }
                //更新告警集
                updateCollectionWithNewAlarm(existingByAlarmId, tblId, alarmId, deviceId, deviceName, eventType, rightCheckNum);
                log.info("当前告警记录alarmId已存在,更新告警集 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | collectionId:{}",
                        tblId, alarmId, imagePath, videoPath, existingByAlarmId.getId());

                //更新处置建议
                appendAdvice(record, alarmCollectionServiceImpl.getCollectionByTblId(tblId).getId());

                pushToGx(record, alarmId);
                return;
            }
        } catch (Exception e) {
            log.error("根据alarmId查询告警集失败 | alarmId:{} | 异常原因:{}", alarmId, e.getMessage(), e);
        }

        log.info("当前告警记录alarmId未归属,继续后续处理 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}",
                tblId, alarmId, imagePath, videoPath);

        //alarmId暂无归属告警集：判定非法车辆
        if ("非法车辆".equals(eventType)) {
            log.info("当前告警记录为非法车辆告警且alarmId无归属，创建新告警集：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}",
                    tblId, alarmId, imagePath, videoPath);
            createNewCollection(roadId, deviceId, deviceName, eventType, tblId, alarmId, alarmTime, milestone, checkFlag);
        } else {
            // 检查点位是否有最新活跃告警集
            try {
                AlarmCollection latestByDevice = alarmCollectionServiceImpl.getLatestByDeviceId(deviceId);
                if (latestByDevice != null) {
                    log.info("当前告警记录deviceId:{}存在活跃告警集，进行业务逻辑判定 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}",
                            deviceId, tblId, alarmId, imagePath, videoPath);
                    handleExistingDeviceCollection(latestByDevice, record, checkFlag);
                } else {
                    // 无点位告警集：创建新告警集
                    log.info("当前告警记录deviceId:{}不存在活跃告警集，创建新告警集 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}",
                            deviceId, tblId, alarmId, imagePath, videoPath);
                    createNewCollection(roadId, deviceId, deviceName, eventType, tblId, alarmId, alarmTime, milestone, checkFlag);
                }
            } catch (Exception e) {
                log.error("查询点位最新告警集失败 | deviceId:{} | 异常原因:{}", deviceId, e.getMessage(), e);
                // 兜底：创建新集
                createNewCollection(roadId, deviceId, deviceName, eventType, tblId, alarmId, alarmTime, milestone, checkFlag);
            }
        }

        //更新处置建议
        try {
            appendAdvice(record, alarmCollectionServiceImpl.getCollectionByTblId(tblId).getId());
        } catch (Exception e) {
            log.error("更新处置建议失败 | tblId:{} | 异常原因:{}", tblId, e.getMessage(), e);
        }

        // 推送数据到GX
        pushToGx(record, alarmId);
    }

    /**
     * 解析设备名称（从content字段提取）
     */
    private String parseDeviceName(String content) {
        if (content == null) {
            log.warn("告警记录content为空，设备名称设为默认 | content:null");
            return "未知设备";
        }
        String[] parts = content.split("\\+");
        return parts.length > 0 ? parts[0].trim() : "未知设备";
    }

    /**
     * 已归属告警集：追加新告警并更新
     */
    private void updateCollectionWithNewAlarm(AlarmCollection collection, Long newTblId, String newAlarmId,
                                              String deviceId, String deviceName, String eventType, int rightCheckNum) {
        try {
            // 防御性判空
            if (collection == null) {
                log.error("告警集对象为null，终止更新 | newTblId:{} | newAlarmId:{}", newTblId, newAlarmId);
                return;
            }

            // 解析已有关联列表（处理空字符串）
            List<String> tblIdList = parseRelatedList(collection.getRelatedTblIdList());
            List<String> alarmIdList = parseRelatedList(collection.getRelatedAlarmIdList());

            // 追加新告警ID（去重）
            if (newTblId != null && !tblIdList.contains(String.valueOf(newTblId))) {
                tblIdList.add(String.valueOf(newTblId));
            }
            if (StringUtils.hasText(newAlarmId) && !alarmIdList.contains(newAlarmId)) {
                alarmIdList.add(newAlarmId);
            }

            // 获取更新后的时间范围（增加非空判断）
            AlarmTimeRange timeRange = originalAlarmServiceImpl.getTimeRangeByTblIdList(tblIdList);
            if (timeRange == null) {
                log.error("获取告警集时间范围失败，使用默认值 | tblIdList:{}", tblIdList);
                collection.setEarliestAlarmTime(null);
                collection.setLatestAlarmTime(null);
            }else{
                collection.setEarliestAlarmTime(timeRange.getMinAlarmTime());
                collection.setLatestAlarmTime(timeRange.getMaxAlarmTime());
            }

            // 更新告警集字段
            collection.setDeviceId(deviceId);
            collection.setDeviceName(deviceName);
            collection.setRightCheckNum(rightCheckNum);
            if ("非法车辆".equals(eventType)) {
                collection.setEventType("摩托车闯禁");
            } else {
                collection.setEventType(defineCollectionEventType(collection.getId(), tblIdList));
            }
            collection.setRelatedTblIdList(String.join(",", tblIdList));
            collection.setRelatedAlarmIdList(String.join(",", alarmIdList));
            collection.setDisposalAdvice(calculateCollectionAdvice(collection));
            collection.setRelatedAlarmNum(tblIdList.size());
            if ("非法车辆".equals(eventType)) {
                collection.setRelatedSourceType(FlagTagUtil.SOURCE_TYPE_ILLEGAL_ENTRY);
            } else {
                collection.setRelatedSourceType(FlagTagUtil.SOURCE_TYPE_NORMAL);
            }
            collection.setModifyTime(LocalDateTime.now());
            this.updateById(collection);
            log.info("告警集执行更新：tblId:{} | alarmId:{} | collectionId:{}", newTblId, newAlarmId, collection.getId());
        } catch (Exception e) {
            log.error("告警集更新异常：tblId:{} | alarmId:{} | collectionId:{} | exception info:{}",
                    newTblId, newAlarmId, collection != null ? collection.getId() : "null", e.getMessage());
            throw new RuntimeException("告警集更新失败", e);
        }
    }

    /**
     * 处理点位已有告警集：判断时间间隔，决定更新或创建新集
     * 核心修复：解决IndexOutOfBoundsException异常，增加全链路防御性判空
     */
    private void handleExistingDeviceCollection(AlarmCollection existingCollection, OriginalAlarmRecord record, int checkFlag) {
        // 外层异常捕获，兜底处理所有未预判的异常
        try {
            // 1. 防御性判空
            if (existingCollection == null || record == null) {
                log.error("告警集/告警记录为null，创建新集 | existingCollection:{} | record:{}",
                        existingCollection, record);
                if (record != null) {
                    createNewCollection(record.getRoadId(), record.getDeviceId(), parseDeviceName(record.getContent()),
                            record.getEventType(), record.getTblId(), record.getId(), record.getAlarmTime(),
                            record.getMilestone(), checkFlag);
                }
                return;
            }

            // 2. 获取告警集属性信息
            int collectionId = existingCollection.getId();
            String deviceId = existingCollection.getDeviceId() == null ? "" : existingCollection.getDeviceId();

            // 3. 获取当前告警记录字段属性信息（增加非空默认值）
            long currentTblId = record.getTblId() == null ? -1L : record.getTblId();
            String currentAlarmId = record.getId() == null ? "" : record.getId();
            String currentImagePath = record.getImagePath() == null ? "" : record.getImagePath();
            String currentVideoPath = record.getVideoPath() == null ? "" : record.getVideoPath();
            String currentEventType = record.getEventType() == null ? "" : record.getEventType();
            LocalDateTime currentAlarmTime = record.getAlarmTime() == null ? LocalDateTime.now() : record.getAlarmTime();
            int currentMileStone = record.getMilestone() == 0 ? 0 : record.getMilestone();

            // 4. 获取告警集最新记录字段属性信息
            List<String> tblIdList = parseRelatedList(existingCollection.getRelatedTblIdList());
            // 防御性判空：tblIdList为空直接创建新集
            if (tblIdList.isEmpty()) {
                log.warn("告警集关联的tblIdList为空，无法获取对比记录，创建新集 | collectionId:{}", collectionId);
                createNewCollection(record.getRoadId(), deviceId, parseDeviceName(record.getContent()), currentEventType,
                        currentTblId, currentAlarmId, currentAlarmTime, currentMileStone, checkFlag);
                return;
            }

            // 获取小于当前告警记录时间的最新一条记录，由于数据乱序问题，时间可能为空
            List<OriginalAlarmRecord> timeAgoList = originalAlarmServiceImpl.getListByTblIdListAndTimeAgo(tblIdList, currentAlarmTime);
            OriginalAlarmRecord targetRecord = null;

            if (timeAgoList != null && !timeAgoList.isEmpty()) {
                targetRecord = timeAgoList.get(0);
            } else {
                log.info("当前告警集没有小于alarmTime的告警记录，向后查找 | collectionId:{} | currentAlarmTime:{}",
                        collectionId, currentAlarmTime);
                List<OriginalAlarmRecord> timeAfterList = originalAlarmServiceImpl.getListByTblIdListAndTimeAfter(tblIdList, currentAlarmTime);
                // 核心修复：校验timeAfterList是否为空
                if (timeAfterList == null || timeAfterList.isEmpty()) {
                    log.error("告警集无任何可对比的告警记录 | collectionId:{} | tblIdList:{}", collectionId, tblIdList);
                    // 兜底方案：直接创建新集，避免越界
                    createNewCollection(record.getRoadId(), deviceId, parseDeviceName(record.getContent()), currentEventType,
                            currentTblId, currentAlarmId, currentAlarmTime, currentMileStone, checkFlag);
                    return;
                }
                targetRecord = timeAfterList.get(0);
            }

            // 防御性判空：targetRecord为空时直接创建新集
            if (targetRecord == null) {
                log.error("对比的告警记录为null | collectionId:{} | tblIdList:{}", collectionId, tblIdList);
                createNewCollection(record.getRoadId(), deviceId, parseDeviceName(record.getContent()), currentEventType,
                        currentTblId, currentAlarmId, currentAlarmTime, currentMileStone, checkFlag);
                return;
            }

            // 5. 提取对比记录的字段（增加非空校验）
            long latestTblId = targetRecord.getTblId() == null ? -1 : targetRecord.getTblId();
            String latestAlarmId = targetRecord.getId() == null ? "" : targetRecord.getId();
            String latestImagePath = targetRecord.getImagePath() == null ? "" : targetRecord.getImagePath();
            String latestVideoPath = targetRecord.getVideoPath() == null ? "" : targetRecord.getVideoPath();
            String latestEventType = targetRecord.getEventType() == null ? "" : targetRecord.getEventType();
            LocalDateTime latestAlarmTime = targetRecord.getAlarmTime();
            int latestMileStone = targetRecord.getMilestone() == 0 ? 0 : targetRecord.getMilestone();

            // 6. 防御性处理：latestAlarmTime为空时直接创建新集
            if (latestAlarmTime == null) {
                log.error("对比记录的告警时间为空 | collectionId:{} | latestAlarmId:{}", collectionId, latestAlarmId);
                createNewCollection(record.getRoadId(), deviceId, parseDeviceName(record.getContent()), currentEventType,
                        currentTblId, currentAlarmId, currentAlarmTime, currentMileStone, checkFlag);
                return;
            }

            // 7. 获取相邻告警时间差值
            long timeDiffMinutes = Math.abs(Duration.between(latestAlarmTime, currentAlarmTime).toMinutes());

            // 8. 获取发生点位差值
            int mileStoneDiff = Math.abs(currentMileStone - latestMileStone);

            log.info("当前告警记录信息：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | eventType:{} | alarmTime:{} |mileStone:{}" +
                            "集内对比记录信息：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | eventType:{} | alarmTime:{} |mileStone:{}" +
                            "告警集id:{} | deviceId:{} | timeDiffMinutes:{} | mileStoneDiff:{}",
                    currentTblId, currentAlarmId, currentImagePath, currentVideoPath, currentEventType, currentAlarmTime, currentMileStone,
                    latestTblId, latestAlarmId, latestImagePath, latestVideoPath, latestEventType, latestAlarmTime, latestMileStone,
                    collectionId, deviceId,
                    timeDiffMinutes, mileStoneDiff
            );

            // 9. 定义核心动作（消除重复代码）
            // 动作1：关闭旧集+创建新集
            Runnable createNewCollectionAction = () -> {
                closeOldCollection(existingCollection);
                createNewCollection(record.getRoadId(), deviceId, parseDeviceName(record.getContent()), currentEventType,
                        currentTblId, currentAlarmId, currentAlarmTime, currentMileStone, checkFlag);
                log.info("告警集超时，创建新集 | oldCollectionId:{} | newAlarmId:{} | 间隔:{}分钟",
                        existingCollection.getId(), currentAlarmId, timeDiffMinutes);
            };
            // 动作2：更新旧集
            Runnable updateCollectionAction = () -> {
                updateExistingDeviceCollection(existingCollection, currentTblId, currentAlarmId, currentEventType, checkFlag);
                log.info("告警集未超时，更新完成 | collectionId:{} | alarmId:{} | 间隔:{}分钟",
                        existingCollection.getId(), currentAlarmId, timeDiffMinutes);
            };

            // 10. 简化条件判断（解耦嵌套，提升可读性）
            if (!"停驶".equals(currentEventType)) {
                //当前非停驶记录，仅判断与上条记录时间差值是否超过10分钟
                if (timeDiffMinutes > 10) {
                    createNewCollectionAction.run();
                } else {
                    updateCollectionAction.run();
                }
            } else {
                //当前记录为停驶，则判断上条记录类型
                if ("停驶".equals(latestEventType)) {
                    //时间间隔大于30分钟创建新集
                    if (timeDiffMinutes > 30) {
                        if (mileStoneDiff == 0) {
                            //点位不变，更新旧集
                            updateCollectionAction.run();
                        } else {
                            createNewCollectionAction.run();
                        }
                    } else {
                        //时间间隔小于30分钟
                        if (mileStoneDiff > 20) {
                            //点位间隔超过20m，创建新告警集
                            createNewCollectionAction.run();
                        } else {
                            //点位间隔小于20m，更新旧集
                            updateCollectionAction.run();
                        }
                    }
                } else {
                    if (timeDiffMinutes > 10) {
                        createNewCollectionAction.run();
                    } else {
                        updateCollectionAction.run();
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理点位告警集异常 | collectionId:{} | alarmId:{} | 异常原因:{}",
                    existingCollection.getId(), record.getId(), e.getMessage(), e);
            // 兜底：创建新集，避免流程中断
            createNewCollection(record.getRoadId(), existingCollection.getDeviceId(),
                    parseDeviceName(record.getContent()), record.getEventType(),
                    record.getTblId(), record.getId(), record.getAlarmTime(),
                    record.getMilestone(), checkFlag);
        }
    }

    /**
     * 关闭旧告警集
     */
    private void closeOldCollection(AlarmCollection collection) {
        if (collection == null) {
            log.warn("关闭旧告警集失败：告警集对象为null");
            return;
        }
        collection.setCollectionStatus(FlagTagUtil.COLLECTION_STATUS_CLOSED);
        collection.setModifyTime(LocalDateTime.now());
        alarmCollectionServiceImpl.updateById(collection);
        log.debug("旧告警集已关闭 | collectionId:{}", collection.getId());
    }

    /**
     * 创建新告警集（底层方法）
     */
    public void insertAlarmCollection(String roadId, String deviceId, String deviceName, String eventType,
                                      Long tblId, String alarmId, Integer sourceType, LocalDateTime alarmTime, int milestone, int checkFlag) {
        try {
            // 构建新告警集
            AlarmCollection newCollection = new AlarmCollection();
            newCollection.setRoadId(roadId == null ? "" : roadId);
            newCollection.setDeviceId(deviceId == null ? "" : deviceId);
            newCollection.setDeviceName(StringUtils.hasText(deviceName) ? deviceName : "未知设备");
            newCollection.setMilestone(milestone);
            newCollection.setRelatedTblIdList(tblId == null ? "" : String.valueOf(tblId));
            newCollection.setRelatedAlarmIdList(alarmId == null ? "" : alarmId);
            newCollection.setEventType(eventType == null ? "" : eventType);
            newCollection.setDisposalAdvice(FlagTagUtil.ADVICE_CONFIRM);
            newCollection.setEarliestAlarmTime(alarmTime == null ? LocalDateTime.now() : alarmTime);
            newCollection.setLatestAlarmTime(alarmTime == null ? LocalDateTime.now() : alarmTime);
            newCollection.setRelatedAlarmNum(1);
            newCollection.setCollectionStatus(FlagTagUtil.COLLECTION_STATUS_OPEN);
            newCollection.setRelatedSourceType(sourceType == null ? FlagTagUtil.SOURCE_TYPE_NORMAL : sourceType);
            newCollection.setCreateTime(LocalDateTime.now());
            newCollection.setModifyTime(LocalDateTime.now());

            if (checkFlag == FlagTagUtil.CHECK_RESULT_RIGHT) {
                newCollection.setRightCheckNum(1);
            } else {
                newCollection.setRightCheckNum(0);
            }

            this.saveOrUpdate(newCollection);
            log.debug("新告警集创建成功 | collectionId:{} | alarmId:{}", newCollection.getId(), alarmId);
        } catch (Exception e) {
            log.error("创建新告警集失败 | alarmId:{} | 异常原因:", alarmId, e);
            throw new RuntimeException("创建新告警集失败", e);
        }
    }

    /**
     * 调用insertAlarmCollection的简化方法
     */
    private void createNewCollection(String roadId, String deviceId, String deviceName, String eventType,
                                     Long tblId, String alarmId, LocalDateTime alarmTime, int milestone, int checkFlag) {
        int sourceType = getRelatedSourceType(eventType);
        insertAlarmCollection(roadId, deviceId, deviceName, getTypeReflect(eventType),
                tblId, alarmId, sourceType, alarmTime, milestone, checkFlag);
    }

    /**
     * 更新点位已有告警集（追加新告警）
     */
    private void updateExistingDeviceCollection(AlarmCollection collection, Long newTblId,
                                                String newAlarmId, String eventType, int checkFlag) {
        // 防御性判空
        if (collection == null) {
            log.error("更新点位告警集失败：告警集对象为null | newTblId:{} | newAlarmId:{}", newTblId, newAlarmId);
            return;
        }

        List<String> tblIdList = parseRelatedList(collection.getRelatedTblIdList());
        List<String> alarmIdList = parseRelatedList(collection.getRelatedAlarmIdList());

        // 追加新告警（去重）
        if (newTblId != null && !tblIdList.contains(String.valueOf(newTblId))) {
            tblIdList.add(String.valueOf(newTblId));
        }
        if (StringUtils.hasText(newAlarmId) && !alarmIdList.contains(newAlarmId)) {
            alarmIdList.add(newAlarmId);
        }

        // 推定告警集类型
        String definedEventType = defineCollectionEventType(collection.getId(), tblIdList);
        AlarmTimeRange timeRange = originalAlarmServiceImpl.getTimeRangeByTblIdList(tblIdList);
        if (timeRange == null) {
            log.error("获取时间范围失败，使用默认值 | tblIdList:{}", tblIdList);
            collection.setEarliestAlarmTime(null);
            collection.setLatestAlarmTime(null);
        }else{
            collection.setEarliestAlarmTime(timeRange.getMinAlarmTime());
            collection.setLatestAlarmTime(timeRange.getMaxAlarmTime());
        }

        // 更新字段
        collection.setRelatedTblIdList(String.join(",", tblIdList));
        collection.setRelatedAlarmIdList(String.join(",", alarmIdList));
        collection.setEventType(definedEventType);
        collection.setDisposalAdvice(calculateCollectionAdvice(collection));
        collection.setEarliestAlarmTime(timeRange.getMinAlarmTime());
        collection.setLatestAlarmTime(timeRange.getMaxAlarmTime());
        collection.setRelatedAlarmNum(tblIdList.size());
        collection.setModifyTime(LocalDateTime.now());

        if (checkFlag == FlagTagUtil.CHECK_RESULT_RIGHT) {
            Integer rightCheckNum = collection.getRightCheckNum();
            collection.setRightCheckNum(rightCheckNum == null ? 1 : rightCheckNum + 1);
        }
        this.updateById(collection);
    }

    /**
     * 解析关联ID列表（处理空字符串）
     */
    private List<String> parseRelatedList(String relatedStr) {
        if (relatedStr == null || relatedStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(relatedStr.split(","))
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 推送数据到GX（封装异常处理）
     */
    private void pushToGx(OriginalAlarmRecord record, String alarmId) {
        try {
            if (record == null) {
                log.warn("GX数据推送失败：告警记录为null | alarmId:{}", alarmId);
                return;
            }
            gxFeatureDataPush.pushToGx(record);
            log.debug("GX数据推送成功 | alarmId:{}", alarmId);
        } catch (Exception e) {
            log.error("GX数据推送失败 | alarmId:{} | 异常原因:", alarmId, e);
            // 推送失败不中断主流程
        }
    }


    // ------------------------------ 告警集类型与处置建议 ------------------------------
    /**
     * 推定告警集事件类型（基于关联正检告警）
     */
    public String defineCollectionEventType(Integer collectionId, List<String> tblIdList) {
        if (tblIdList.isEmpty()) {
            log.error("推定类型失败：关联tblId列表为空 | collectionId:{}", collectionId);
            return "不在判定范围";
        }

        // 获取告警集中所有正检的告警记录
        List<OriginalAlarmRecord> positiveRecords = originalAlarmServiceImpl.getListByTblIdList(tblIdList, 1);
        if (positiveRecords.isEmpty()) {
            log.error("推定类型失败：无正检记录 | collectionId:{} | tblIdList:{}", collectionId, tblIdList);
            return "不在判定范围";
        }

        // 分类存储告警记录
        List<String> stopDriveList = new ArrayList<>();
        List<String> pswList = new ArrayList<>();
        List<String> personList = new ArrayList<>();
        List<String> vehicleNameList = new ArrayList<>();
        List<String> personNameList = new ArrayList<>();
        List<String> pswNameList = new ArrayList<>();

        for (OriginalAlarmRecord record : positiveRecords) {
            CheckAlarmProcess process = checkAlarmProcessServiceImpl.getIouTop1ByKey(
                    record.getId(), record.getImagePath(), record.getVideoPath());

            if (process == null || process.getName() == null) {
                log.warn("检测结果为空或名称缺失 | alarmId:{} | imagePath:{}",
                        record.getId(), record.getImagePath());
                continue;
            }

            String eventType = record.getEventType();
            String checkName = process.getName();
            switch (eventType) {
                case "停驶":
                    stopDriveList.add(eventType);
                    vehicleNameList.add(checkName);
                    break;
                case "抛洒物":
                    pswList.add(eventType);
                    pswNameList.add(checkName);
                    break;
                case "行人":
                    personList.add(eventType);
                    personNameList.add(checkName);
                    break;
                default:
                    log.debug("忽略非目标类型 | eventType:{} | alarmId:{}", eventType, record.getId());
            }
        }

        // 按优先级推定类型（道路施工 > 交通事故 > 车辆故障 > 行人闯入 > 路面异常 > 车辆停驶）
        String finalType = checkRoadConstruction(stopDriveList, vehicleNameList, personNameList);
        if (StringUtils.hasText(finalType)) return finalType;

        finalType = checkTrafficAccident(stopDriveList, vehicleNameList, personNameList);
        if (StringUtils.hasText(finalType)) return finalType;

        finalType = checkVehicleFailure(stopDriveList, vehicleNameList, pswNameList);
        if (StringUtils.hasText(finalType)) return finalType;

        finalType = checkPersonEntry(personList, positiveRecords.size(), personNameList);
        if (StringUtils.hasText(finalType)) return finalType;

        finalType = checkPsw(pswList, positiveRecords.size());
        if (StringUtils.hasText(finalType)) return finalType;

        finalType = checkVehicleStop(vehicleNameList, positiveRecords.size());
        if (StringUtils.hasText(finalType)) return finalType;

        return "不在判定范围";
    }

    /**
     * 判断是否为道路施工
     */
    private String checkRoadConstruction(List<String> stopDriveList, List<String> vehicleNames, List<String> personNames) {
        if (stopDriveList.isEmpty()) return null;

        long constructionVehicles = vehicleNames.stream()
                .filter(name -> name.contains("maintenance_construction_vehicle")
                        || name.contains("anti_collision_vehicle"))
                .count();

        long constructionWorkers = personNames.stream()
                .filter(name -> name.contains("builder"))
                .count();

        if (constructionVehicles == vehicleNames.size() || constructionWorkers == personNames.size()) {
            return "道路施工";
        }
        return null;
    }

    /**
     * 判断是否为交通事故
     */
    private String checkTrafficAccident(List<String> stopDriveList, List<String> vehicleNames, List<String> personNames) {
        if (stopDriveList.isEmpty()) return null;

        long rescueVehicles = vehicleNames.stream()
                .filter(name -> name.contains("police_car")
                        || name.contains("ambulance")
                        || name.contains("fire_fighting_truck"))
                .count();

        long rescueWorkers = personNames.stream()
                .filter(name -> name.contains("traffic_police")
                        || name.contains("medical_person"))
                .count();

        if (rescueVehicles > 0 || rescueWorkers > 0) {
            return "交通事故";
        }
        return null;
    }

    /**
     * 判断是否为车辆故障
     */
    private String checkVehicleFailure(List<String> stopDriveList, List<String> vehicleNames, List<String> pswNames) {
        if (stopDriveList.isEmpty()) return null;

        long nonRescueVehicles = vehicleNames.stream()
                .filter(name -> !(name.contains("maintenance_construction_vehicle")
                        || name.contains("fire_fighting_truck")
                        || name.contains("ambulance")
                        || name.contains("police_car")))
                .count();

        long faultPsw = pswNames.stream()
                .filter(name -> name.contains("tripod")
                        || name.contains("spills")
                        || name.contains("tyre"))
                .count();

        if (nonRescueVehicles > 0 && faultPsw > 0) {
            return "车辆故障";
        }
        return null;
    }

    /**
     * 判断是否为行人闯入
     */
    private String checkPersonEntry(List<String> personList, int totalPositive, List<String> personNames) {
        if (personList.size() != totalPositive) return null;

        long normalPersons = personNames.stream()
                .filter(name -> name.contains("person"))
                .count();

        if (normalPersons == totalPositive) {
            return "行人闯入";
        }
        return null;
    }

    /**
     * 判断是否为路面异常（抛洒物）
     */
    private String checkPsw(List<String> pswList, int totalPositive) {
        return pswList.size() == totalPositive ? "路面异常" : null;
    }

    /**
     * 判断是否为车辆停驶
     */
    private String checkVehicleStop(List<String> vehicleNames, int totalPositive) {
        if (vehicleNames.isEmpty()) return null;

        long nonRescueVehicles = vehicleNames.stream()
                .filter(name -> !(name.contains("maintenance_construction_vehicle")
                        || name.contains("fire_fighting_truck")
                        || name.contains("ambulance")
                        || name.contains("police_car")))
                .count();

        return nonRescueVehicles == totalPositive ? "车辆停驶" : null;
    }

    /**
     * 计算告警集处置建议
     */
    public int calculateCollectionAdvice(AlarmCollection collection) {
        if (collection == null) {
            log.error("计算处置建议失败：告警集对象为null");
            return FlagTagUtil.ADVICE_CONFIRM;
        }

        List<String> tblIds = parseRelatedList(collection.getRelatedTblIdList());
        List<OriginalAlarmRecord> allRecords = originalAlarmServiceImpl.getListByTblIdList(tblIds);
        List<OriginalAlarmRecord> confirmedEvents = originalAlarmServiceImpl.getEventByIdList(tblIds);
        List<OriginalAlarmRecord> unconfirmedEvents = originalAlarmServiceImpl.getNoEventByIdList(tblIds);

        // 统计各类型处置建议
        Map<Integer, Integer> adviceCount = countAdviceFlags(allRecords, confirmedEvents, unconfirmedEvents);

        // 根据统计结果返回建议
        if (confirmedEvents == null || confirmedEvents.isEmpty()) {
            return getAdviceForUnconfirmed(adviceCount, unconfirmedEvents == null ? 0 : unconfirmedEvents.size());
        } else {
            return getAdviceForConfirmed(collection, tblIds, adviceCount, unconfirmedEvents == null ? 0 : unconfirmedEvents.size());
        }
    }

    /**
     * 统计处置建议数量（修复空指针 + 完善判空逻辑）
     */
    private Map<Integer, Integer> countAdviceFlags(List<OriginalAlarmRecord> allRecords,
                                                   List<OriginalAlarmRecord> confirmed,
                                                   List<OriginalAlarmRecord> unconfirmed) {
        // 初始化计数Map，避免空值
        Map<Integer, Integer> countMap = new HashMap<>();
        countMap.put(FlagTagUtil.ADVICE_UNDETERMINED, 0);
        countMap.put(FlagTagUtil.ADVICE_FALSE_ALARM, 0);
        countMap.put(FlagTagUtil.ADVICE_CONFIRM, 0);
        countMap.put(FlagTagUtil.ADVICE_NO_NEED, 0);

        // 防御性判空：避免unconfirmed列表为null导致遍历异常
        if (unconfirmed == null || unconfirmed.isEmpty()) {
            log.warn("未确认事件列表为空，无需统计处置建议");
            return countMap;
        }

        // 统计未确认事件的建议
        for (OriginalAlarmRecord record : unconfirmed) {
            // 1. 判空record本身
            if (record == null) {
                log.warn("未确认事件记录为null，跳过统计");
                countMap.put(FlagTagUtil.ADVICE_UNDETERMINED, countMap.get(FlagTagUtil.ADVICE_UNDETERMINED) + 1);
                continue;
            }
            // 2. 判空tblId（核心查询条件）
            Long tblId = record.getTblId();
            if (tblId == null) {
                log.warn("未确认事件tblId为null | alarmId:{}", record.getId());
                countMap.put(FlagTagUtil.ADVICE_UNDETERMINED, countMap.get(FlagTagUtil.ADVICE_UNDETERMINED) + 1);
                continue;
            }

            try {
                // 3. 查询特征要素记录并判空
                FeatureElementRecord feature = featureElementServiceImpl.getFeatureByTblId(tblId);
                if (feature == null) {
                    log.warn("未查询到特征要素记录 | tblId:{} | alarmId:{}", tblId, record.getId());
                    countMap.put(FlagTagUtil.ADVICE_UNDETERMINED, countMap.get(FlagTagUtil.ADVICE_UNDETERMINED) + 1);
                    continue;
                }
                // 4. 安全获取处置建议
                int advice = feature.getDisposalAdvice();
                // 5. 校验建议值是否在合法范围内
                if (!countMap.containsKey(advice)) {
                    log.warn("处置建议值非法 | advice:{} | tblId:{} | alarmId:{}", advice, tblId, record.getId());
                    countMap.put(FlagTagUtil.ADVICE_UNDETERMINED, countMap.get(FlagTagUtil.ADVICE_UNDETERMINED) + 1);
                } else {
                    countMap.put(advice, countMap.get(advice) + 1);
                }
            } catch (Exception e) {
                log.error("统计处置建议失败 | alarmId:{} | tblId:{} | 异常原因:{}",
                        record.getId(), tblId, e.getMessage(), e);
                countMap.put(FlagTagUtil.ADVICE_UNDETERMINED, countMap.get(FlagTagUtil.ADVICE_UNDETERMINED) + 1);
            }
        }
        return countMap;
    }

    /**
     * 未确认事件的处置建议
     */
    private int getAdviceForUnconfirmed(Map<Integer, Integer> adviceCount, int totalUnconfirmed) {
        if (adviceCount == null) {
            log.warn("处置建议统计Map为null，返回默认值ADVICE_CONFIRM");
            return FlagTagUtil.ADVICE_CONFIRM;
        }

        if (adviceCount.get(FlagTagUtil.ADVICE_FALSE_ALARM) == totalUnconfirmed) {
            return FlagTagUtil.ADVICE_FALSE_ALARM;

        } else if (adviceCount.get(FlagTagUtil.ADVICE_UNDETERMINED) == totalUnconfirmed) {
            return FlagTagUtil.ADVICE_CONFIRM;

        } else if (adviceCount.get(FlagTagUtil.ADVICE_NO_NEED) == totalUnconfirmed) {
            return FlagTagUtil.ADVICE_NO_NEED;

        } else if (adviceCount.get(FlagTagUtil.ADVICE_CONFIRM) == totalUnconfirmed) {
            return FlagTagUtil.ADVICE_CONFIRM;
        } else if (adviceCount.get(FlagTagUtil.ADVICE_UNDETERMINED) == 1 || adviceCount.get(FlagTagUtil.ADVICE_CONFIRM) == 1) {
            return FlagTagUtil.ADVICE_CONFIRM;
        } else if (adviceCount.get(FlagTagUtil.ADVICE_FALSE_ALARM) == 1
                && adviceCount.get(FlagTagUtil.ADVICE_NO_NEED) == totalUnconfirmed - 1) {
            return FlagTagUtil.ADVICE_NO_NEED;
        }
        return FlagTagUtil.ADVICE_CONFIRM;
    }

    /**
     * 已确认事件的处置建议
     */
    private int getAdviceForConfirmed(AlarmCollection collection, List<String> tblIds,
                                      Map<Integer, Integer> adviceCount, int totalUnconfirmed) {
        if (collection == null || tblIds == null || tblIds.isEmpty()) {
            log.warn("已确认事件处置建议计算参数异常，返回默认值ADVICE_CONFIRM");
            return FlagTagUtil.ADVICE_CONFIRM;
        }

        OriginalAlarmRecord latestConfirmed = originalAlarmServiceImpl.getLatestConfirm(tblIds);
        if (latestConfirmed == null) {
            log.warn("已确认事件为空，但confirmedEvents非空 | collectionId:{}", collection.getId());
            return FlagTagUtil.ADVICE_CONFIRM;
        }

        // 最新记录为已确认事件 → 无需处理
        if (collection.getLatestAlarmTime() != null && collection.getLatestAlarmTime().equals(latestConfirmed.getAlarmTime())) {
            return FlagTagUtil.ADVICE_NO_NEED;
        }

        // 处理最新确认事件之后的未确认记录
        if (adviceCount == null) {
            return FlagTagUtil.ADVICE_CONFIRM;
        }

        if (adviceCount.get(FlagTagUtil.ADVICE_FALSE_ALARM) == totalUnconfirmed) {
            return FlagTagUtil.ADVICE_NO_NEED;
        } else if (adviceCount.get(FlagTagUtil.ADVICE_UNDETERMINED) == totalUnconfirmed) {
            return FlagTagUtil.ADVICE_CONFIRM;
        } else if (adviceCount.get(FlagTagUtil.ADVICE_NO_NEED) == totalUnconfirmed) {
            return FlagTagUtil.ADVICE_NO_NEED;
        } else if (adviceCount.get(FlagTagUtil.ADVICE_CONFIRM) == totalUnconfirmed) {
            return FlagTagUtil.ADVICE_CONFIRM;
        } else if (adviceCount.get(FlagTagUtil.ADVICE_UNDETERMINED) == 1 || adviceCount.get(FlagTagUtil.ADVICE_CONFIRM) == 1) {
            return FlagTagUtil.ADVICE_CONFIRM;
        } else if (adviceCount.get(FlagTagUtil.ADVICE_FALSE_ALARM) == 1
                && adviceCount.get(FlagTagUtil.ADVICE_NO_NEED) == totalUnconfirmed - 1) {
            return FlagTagUtil.ADVICE_NO_NEED;
        }
        return FlagTagUtil.ADVICE_CONFIRM;
    }

    /**
     * 转换告警类型描述
     */
    public String getTypeReflect(String eventType) {
        if (eventType == null || eventType.isEmpty()) return "数据异常";
        switch (eventType) {
            case "抛洒物":
                return "路面异常";
            case "停驶":
                return "车辆停驶";
            case "行人":
                return "行人闯入";
            case "非法车辆":
                return "摩托车闯禁";
            default:
                return "不在判定范围";
        }
    }

    /**
     * 获取告警集来源类型
     */
    public Integer getRelatedSourceType(String eventType) {
        return "非法车辆".equals(eventType) ? FlagTagUtil.SOURCE_TYPE_ILLEGAL_ENTRY : FlagTagUtil.SOURCE_TYPE_NORMAL;
    }

    /**
     * 追加补充特征要素处置建议信息
     */
    public void appendAdvice(OriginalAlarmRecord record, int collectionId) {
        if (record == null) {
            log.error("追加处置建议失败：告警记录为null");
            return;
        }

        Long tblId = record.getTblId();
        if (tblId == null) {
            log.error("追加处置建议失败：tblId为null | alarmId:{}", record.getId());
            return;
        }

        FeatureElementRecord featureElementRecord = featureElementServiceImpl.getFeatureByTblId(tblId);
        if (featureElementRecord == null) {
            log.error("追加处置建议失败：特征要素记录为null | tblId:{}", tblId);
            return;
        }

        if (featureElementRecord.getDisposalAdvice() == FlagTagUtil.ADVICE_TMP_WAIT) {
            int disposalAdvice = FlagTagUtil.ADVICE_UNDETERMINED;
            String adviceReason = "";

            AlarmCollection alarmCollection = alarmCollectionServiceImpl.getCollectionByTblId(tblId);
            log.info("正检告警集判断:{}", alarmCollection == null ? "null" : alarmCollection.toString());

            int rightCheckNum = 0; // 默认值
            if (alarmCollection != null) {
                Integer rcn = alarmCollection.getRightCheckNum();
                rightCheckNum = rcn == null ? 0 : rcn;
                log.info("告警集初检为正检告警记录条数: {}条, 当前查询告警记录 {}", rightCheckNum, tblId);
            }
            if (rightCheckNum == 1) {
                disposalAdvice = FlagTagUtil.ADVICE_CONFIRM;
                adviceReason = "需人工确认"; // 补充默认原因，避免空值
            } else {
                disposalAdvice = FlagTagUtil.ADVICE_REPEAT;
                adviceReason = "重复告警";
            }

            featureElementRecord.setMatchCollectionId(collectionId);
            featureElementRecord.setDisposalAdvice(disposalAdvice);
            featureElementRecord.setAdviceReason(adviceReason);
            featureElementServiceImpl.updateById(featureElementRecord);
            log.info("更新特征要素处置建议信息：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | advice:{} | reason:{}",
                    tblId, record.getId(), record.getImagePath(), record.getVideoPath(), disposalAdvice, adviceReason);
        } else {
            featureElementRecord.setMatchCollectionId(collectionId);
            featureElementServiceImpl.updateById(featureElementRecord);
        }
    }
}