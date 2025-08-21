package com.yuce.algorithm;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.common.GxFeatureDataPush;
import com.yuce.entity.*;
import com.yuce.mapper.AlarmCollectionMapper;
import com.yuce.service.AlarmCollectionService;
import com.yuce.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 告警集算法服务：负责告警集的创建、更新、类型推定及处置建议生成
 */
@Service
@Slf4j
public class AlarmCollectionAlgorithm extends ServiceImpl<AlarmCollectionMapper, AlarmCollection> implements AlarmCollectionService {

    // ------------------------------ 常量定义（统一维护，避免硬编码） ------------------------------
    /** 告警集状态：开启 */
    private static final int COLLECTION_STATUS_OPEN = 1;
    /** 告警集状态：关闭 */
    private static final int COLLECTION_STATUS_CLOSED = 2;
    /** 告警集来源类型：正常告警 */
    private static final int SOURCE_TYPE_NORMAL = 1;
    /** 告警集来源类型：非法闯入 */
    private static final int SOURCE_TYPE_ILLEGAL_ENTRY = 2;
    /** 处置建议：疑似误报 */
    private static final int ADVICE_SUSPECTED_ERROR = 1;
    /** 处置建议：尽快确认 */
    private static final int ADVICE_NEED_CONFIRM = 2;
    /** 处置建议：无需处理 */
    private static final int ADVICE_NO_NEED = 3;
    /** 处置建议：无法判断 */
    private static final int ADVICE_UNKNOWN = 0;


    // ------------------------------ 依赖注入 ------------------------------
    @Autowired
    private AlarmCollectionServiceImpl alarmCollectionService;

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmService;

    @Autowired
    private FeatureElementServiceImpl featureElementService;

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessService;

    @Autowired
    private CollectionDurationConfigServiceImpl durationConfigService;

    @Autowired
    private GxFeatureDataPush gxFeatureDataPush;


    // ------------------------------ 核心业务方法 ------------------------------
    /**
     * 告警集处理主逻辑：
     * 1. 检查告警是否已归属告警集 → 已归属则更新
     * 2. 未归属则检查点位是否有活跃告警集 → 有则判断时间间隔，无则创建新集
     */
    public void collectionDeal(OriginalAlarmRecord record) {
        // 提取核心字段
        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String roadId = record.getRoadId();
        String deviceId = record.getDeviceId();
        LocalDateTime alarmTime = record.getAlarmTime();
        String eventType = record.getEventType();
        int milestone = record.getMilestone();

        // 核心字段空值校验（提前阻断非法数据）
        validateCoreFields(tblId, alarmId, imagePath, videoPath, deviceId, alarmTime, eventType);

        // 解析设备名称
        String deviceName = parseDeviceName(record.getContent());
        log.info("开始告警集处理 | alarmId:{} | deviceId:{} | eventType:{} | videoPath:{}", alarmId, deviceId, eventType, videoPath);

        // 检查告警是否已归属告警集
        AlarmCollection existingByAlarmId = alarmCollectionService.getCollectionByAlarmId(alarmId);
        if (existingByAlarmId != null) {
            // 已归属：更新告警集（追加当前告警）
            updateCollectionWithNewAlarm(existingByAlarmId, tblId, alarmId);
            log.info("告警已归属告警集，更新完成 | alarmId:{} | imagePath:{} | videoPath:{} | collectionId:{}", alarmId, imagePath, videoPath, existingByAlarmId.getId());
            pushToGx(record, alarmId);
            return;
        }

        // 未归属：检查点位是否有最新活跃告警集
        AlarmCollection latestByDevice = alarmCollectionService.getLatestByDeviceId(deviceId);
        if (latestByDevice != null) {
            // 有点位告警集：判断时间间隔是否超过配置
            handleExistingDeviceCollection(latestByDevice, record, tblId, alarmId, videoPath, alarmTime, eventType);
        } else {
            // 无点位告警集：创建新告警集
            createNewCollection(roadId, deviceId, deviceName, eventType, tblId, alarmId, alarmTime, milestone);
            log.info("点位无活跃告警集，新集创建完成 | alarmId:{} | deviceId:{}", alarmId, deviceId);
        }
        // 推送数据到GX
        pushToGx(record, alarmId);
    }


    // ------------------------------ 告警集操作方法 ------------------------------
    /**
     * 校验核心业务字段非空
     */
    private void validateCoreFields(Long tblId, String alarmId, String imagePath, String videoPath,
                                    String deviceId, LocalDateTime alarmTime, String eventType) {
        Assert.notNull(tblId, "告警记录tblId不能为空");
        Assert.hasText(alarmId, "告警ID(alarmId)不能为空或空白");
        Assert.hasText(imagePath, "图片路径(imagePath)不能为空或空白");
        Assert.hasText(videoPath, "视频路径(videoPath)不能为空或空白");
        Assert.hasText(deviceId, "设备ID(deviceId)不能为空或空白");
        Assert.notNull(alarmTime, "告警时间(alarmTime)不能为空");
        Assert.hasText(eventType, "告警类型(eventType)不能为空或空白");
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
    private void updateCollectionWithNewAlarm(AlarmCollection collection, Long newTblId, String newAlarmId) {
        try {
            // 解析已有关联列表（处理空字符串）
            List<String> tblIdList = parseRelatedList(collection.getRelatedTblIdList());
            List<String> alarmIdList = parseRelatedList(collection.getRelatedAlarmIdList());

            // 追加新告警ID（去重）
            if (!tblIdList.contains(String.valueOf(newTblId))) {
                tblIdList.add(String.valueOf(newTblId));
            }
            if (!alarmIdList.contains(newAlarmId)) {
                alarmIdList.add(newAlarmId);
            }

            // 获取更新后的时间范围
            AlarmTimeRange timeRange = originalAlarmService.getTimeRangeByTblIdList(tblIdList);
            Assert.notNull(timeRange, "获取告警集时间范围失败，tblIdList:" + tblIdList);

            // 更新告警集字段
            collection.setRelatedTblIdList(String.join(",", tblIdList));
            collection.setRelatedAlarmIdList(String.join(",", alarmIdList));
            collection.setDisposalAdvice(calculateCollectionAdvice(collection));
            collection.setEarliestAlarmTime(timeRange.getMinAlarmTime());
            collection.setLatestAlarmTime(timeRange.getMaxAlarmTime());
            collection.setRelatedAlarmNum(tblIdList.size());
            collection.setRelatedSourceType(SOURCE_TYPE_NORMAL);
            collection.setModifyTime(LocalDateTime.now());

            this.updateById(collection);
        } catch (Exception e) {
            log.error("更新告警集失败 | collectionId:{} | newAlarmId:{} | 异常原因:",
                    collection.getId(), newAlarmId, e);
            throw new RuntimeException("更新告警集失败", e);
        }
    }

    /**
     * 处理点位已有告警集：判断时间间隔，决定更新或创建新集
     */
    private void handleExistingDeviceCollection(AlarmCollection existingCollection, OriginalAlarmRecord record,
                                                Long tblId, String alarmId, String videoPath, LocalDateTime alarmTime, String eventType) {
        try {
            // 获取配置的时间间隔（校验配置）
            CollectionDurationConfig config = durationConfigService.getConfig();
            Assert.notNull(config, "未获取到告警集时间间隔配置");
            long configMinutes = config.getDurationMinutes();
            Assert.isTrue(configMinutes > 0, "告警集时间间隔配置必须大于0");

            // 计算时间差
            long timeDiffMinutes = Math.abs(Duration.between(existingCollection.getLatestAlarmTime(), alarmTime).toMinutes());
            log.debug("点位告警集时间差 | collectionId:{} | 配置间隔:{}分钟 | 实际间隔:{}分钟",
                    existingCollection.getId(), configMinutes, timeDiffMinutes);

            if (timeDiffMinutes > configMinutes) {
                // 超过间隔：关闭旧集，创建新集
                closeOldCollection(existingCollection);
                createNewCollection(record.getRoadId(), record.getDeviceId(),
                        parseDeviceName(record.getContent()), eventType, tblId, alarmId, alarmTime, record.getMilestone());
                log.info("告警集超时，创建新集 | oldCollectionId:{} | newAlarmId:{} | 间隔:{}分钟",
                        existingCollection.getId(), alarmId, timeDiffMinutes);
            } else {
                // 未超间隔：更新旧集
                updateExistingDeviceCollection(existingCollection, tblId, alarmId, eventType);
                log.info("告警集未超时，更新完成 | collectionId:{} | alarmId:{} | 间隔:{}分钟",
                        existingCollection.getId(), alarmId, timeDiffMinutes);
            }
        } catch (Exception e) {
            log.error("处理点位告警集失败 | collectionId:{} | alarmId:{} | 异常原因:",
                    existingCollection.getId(), alarmId, e);
            throw new RuntimeException("处理点位告警集失败", e);
        }
    }

    /**
     * 关闭旧告警集
     */
    private void closeOldCollection(AlarmCollection collection) {
        collection.setCollectionStatus(COLLECTION_STATUS_CLOSED);
        collection.setModifyTime(LocalDateTime.now());
        alarmCollectionService.updateById(collection);
        log.debug("旧告警集已关闭 | collectionId:{}", collection.getId());
    }

    /**
     * 创建新告警集
     */
    public void insertAlarmCollection(String roadId, String deviceId, String deviceName, String eventType,
                                      Long tblId, String alarmId, Integer sourceType, LocalDateTime alarmTime, int milestone) {
        try {
            // 校验入参
            Assert.hasText(roadId, "道路ID(roadId)不能为空");
            Assert.hasText(deviceId, "设备ID(deviceId)不能为空");
            Assert.hasText(eventType, "事件类型(eventType)不能为空");
            Assert.notNull(sourceType, "来源类型(sourceType)不能为空");

            // 构建新告警集
            AlarmCollection newCollection = new AlarmCollection();
            newCollection.setCollectionId(UUID.randomUUID().toString());
            newCollection.setRoadId(roadId);
            newCollection.setDeviceId(deviceId);
            newCollection.setDeviceName(StringUtils.hasText(deviceName) ? deviceName : "未知设备");
            newCollection.setMilestone(milestone);
            newCollection.setRelatedTblIdList(String.valueOf(tblId));
            newCollection.setRelatedAlarmIdList(alarmId);
            newCollection.setEventType(eventType);
            newCollection.setDisposalAdvice(ADVICE_NEED_CONFIRM);
            newCollection.setEarliestAlarmTime(alarmTime);
            newCollection.setLatestAlarmTime(alarmTime);
            newCollection.setRelatedAlarmNum(1);
            newCollection.setCollectionStatus(COLLECTION_STATUS_OPEN);
            newCollection.setRelatedSourceType(sourceType);
            newCollection.setCreateTime(LocalDateTime.now());
            newCollection.setModifyTime(LocalDateTime.now());

            this.saveOrUpdate(newCollection);
            log.debug("新告警集创建成功 | collectionId:{} | alarmId:{}",
                    newCollection.getCollectionId(), alarmId);
        } catch (Exception e) {
            log.error("创建新告警集失败 | alarmId:{} | 异常原因:", alarmId, e);
            throw new RuntimeException("创建新告警集失败", e);
        }
    }

    /**
     * 调用insertAlarmCollection的简化方法
     */
    private void createNewCollection(String roadId, String deviceId, String deviceName, String eventType,
                                     Long tblId, String alarmId, LocalDateTime alarmTime, int milestone) {
        int sourceType = getRelatedSourceType(eventType);
        insertAlarmCollection(roadId, deviceId, deviceName, getTypeReflect(eventType),
                tblId, alarmId, sourceType, alarmTime, milestone);
    }

    /**
     * 更新点位已有告警集（追加新告警）
     */
    private void updateExistingDeviceCollection(AlarmCollection collection, Long newTblId,
                                                String newAlarmId, String eventType) {
        List<String> tblIdList = parseRelatedList(collection.getRelatedTblIdList());
        List<String> alarmIdList = parseRelatedList(collection.getRelatedAlarmIdList());

        // 追加新告警（去重）
        if (!tblIdList.contains(String.valueOf(newTblId))) {
            tblIdList.add(String.valueOf(newTblId));
        }
        if (!alarmIdList.contains(newAlarmId)) {
            alarmIdList.add(newAlarmId);
        }

        // 推定告警集类型
        String definedEventType = defineCollectionEventType(collection.getCollectionId(), tblIdList);
        AlarmTimeRange timeRange = originalAlarmService.getTimeRangeByTblIdList(tblIdList);
        Assert.notNull(timeRange, "获取时间范围失败，tblIdList:" + tblIdList);

        // 更新字段
        collection.setRelatedTblIdList(String.join(",", tblIdList));
        collection.setRelatedAlarmIdList(String.join(",", alarmIdList));
        collection.setEventType(StringUtils.hasText(definedEventType) ? definedEventType : eventType);
        collection.setDisposalAdvice(calculateCollectionAdvice(collection));
        collection.setEarliestAlarmTime(timeRange.getMinAlarmTime());
        collection.setLatestAlarmTime(timeRange.getMaxAlarmTime());
        collection.setRelatedAlarmNum(tblIdList.size());
        collection.setModifyTime(LocalDateTime.now());

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
    public String defineCollectionEventType(String collectionId, List<String> tblIdList) {
        if (tblIdList.isEmpty()) {
            log.error("推定类型失败：关联tblId列表为空 | collectionId:{}", collectionId);
            return "";
        }

        // 获取告警集中所有正检的告警记录
        List<OriginalAlarmRecord> positiveRecords = originalAlarmService.getListByTblIdList(tblIdList, 1);
        if (positiveRecords.isEmpty()) {
            log.error("推定类型失败：无正检记录 | collectionId:{} | tblIdList:{}", collectionId, tblIdList);
            return "";
        }

        // 分类存储告警记录
        List<String> stopDriveList = new ArrayList<>();
        List<String> pswList = new ArrayList<>();
        List<String> personList = new ArrayList<>();
        List<String> vehicleNameList = new ArrayList<>();
        List<String> personNameList = new ArrayList<>();
        List<String> pswNameList = new ArrayList<>();

        for (OriginalAlarmRecord record : positiveRecords) {
            CheckAlarmProcess process = checkAlarmProcessService.getIouTop1ByKey(
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

        return "";
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
        List<String> tblIds = parseRelatedList(collection.getRelatedTblIdList());
        List<OriginalAlarmRecord> allRecords = originalAlarmService.getListByTblIdList(tblIds);
        List<OriginalAlarmRecord> confirmedEvents = originalAlarmService.getEventByIdList(tblIds);
        List<OriginalAlarmRecord> unconfirmedEvents = originalAlarmService.getNoEventByIdList(tblIds);

        // 统计各类型处置建议
        Map<Integer, Integer> adviceCount = countAdviceFlags(allRecords, confirmedEvents, unconfirmedEvents);

        // 根据统计结果返回建议
        if (confirmedEvents.isEmpty()) {
            return getAdviceForUnconfirmed(adviceCount, unconfirmedEvents.size());
        } else {
            return getAdviceForConfirmed(collection, tblIds, adviceCount, unconfirmedEvents.size());
        }
    }

    /**
     * 统计处置建议数量
     */
    private Map<Integer, Integer> countAdviceFlags(List<OriginalAlarmRecord> allRecords,
                                                   List<OriginalAlarmRecord> confirmed,
                                                   List<OriginalAlarmRecord> unconfirmed) {
        Map<Integer, Integer> countMap = new HashMap<>();
        countMap.put(ADVICE_UNKNOWN, 0);
        countMap.put(ADVICE_SUSPECTED_ERROR, 0);
        countMap.put(ADVICE_NEED_CONFIRM, 0);
        countMap.put(ADVICE_NO_NEED, 0);

        // 统计未确认事件的建议
        for (OriginalAlarmRecord record : unconfirmed) {
            try {
                FeatureElementRecord feature = featureElementService.getFeatureByKey(
                        record.getId(), record.getImagePath(), record.getVideoPath());
                int advice = feature.getDisposalAdvice();
                countMap.put(advice, countMap.get(advice) + 1);
            } catch (Exception e) {
                log.error("统计处置建议失败 | alarmId:{} | 异常原因:", record.getId(), e);
                countMap.put(ADVICE_UNKNOWN, countMap.get(ADVICE_UNKNOWN) + 1); // 异常默认无法判断
            }
        }
        return countMap;
    }

    /**
     * 未确认事件的处置建议
     */
    private int getAdviceForUnconfirmed(Map<Integer, Integer> adviceCount, int totalUnconfirmed) {
        if (adviceCount.get(ADVICE_SUSPECTED_ERROR) == totalUnconfirmed) {
            return ADVICE_SUSPECTED_ERROR;
        } else if (adviceCount.get(ADVICE_UNKNOWN) == totalUnconfirmed) {
            return ADVICE_NEED_CONFIRM;
        } else if (adviceCount.get(ADVICE_NO_NEED) == totalUnconfirmed) {
            return ADVICE_NO_NEED;
        } else if (adviceCount.get(ADVICE_NEED_CONFIRM) == totalUnconfirmed) {
            return ADVICE_NEED_CONFIRM;
        } else if (adviceCount.get(ADVICE_UNKNOWN) == 1 || adviceCount.get(ADVICE_NEED_CONFIRM) == 1) {
            return ADVICE_NEED_CONFIRM;
        } else if (adviceCount.get(ADVICE_SUSPECTED_ERROR) == 1
                && adviceCount.get(ADVICE_NO_NEED) == totalUnconfirmed - 1) {
            return ADVICE_NO_NEED;
        }
        return ADVICE_NEED_CONFIRM;
    }

    /**
     * 已确认事件的处置建议
     */
    private int getAdviceForConfirmed(AlarmCollection collection, List<String> tblIds,
                                      Map<Integer, Integer> adviceCount, int totalUnconfirmed) {
        OriginalAlarmRecord latestConfirmed = originalAlarmService.getLatestConfirm(tblIds);
        if (latestConfirmed == null) {
            log.warn("已确认事件为空，但confirmedEvents非空 | collectionId:{}", collection.getId());
            return ADVICE_NEED_CONFIRM;
        }

        // 最新记录为已确认事件 → 无需处理
        if (collection.getLatestAlarmTime().equals(latestConfirmed.getAlarmTime())) {
            return ADVICE_NO_NEED;
        }

        // 处理最新确认事件之后的未确认记录
        if (adviceCount.get(ADVICE_SUSPECTED_ERROR) == totalUnconfirmed) {
            return ADVICE_NO_NEED;
        } else if (adviceCount.get(ADVICE_UNKNOWN) == totalUnconfirmed) {
            return ADVICE_NEED_CONFIRM;
        } else if (adviceCount.get(ADVICE_NO_NEED) == totalUnconfirmed) {
            return ADVICE_NO_NEED;
        } else if (adviceCount.get(ADVICE_NEED_CONFIRM) == totalUnconfirmed) {
            return ADVICE_NEED_CONFIRM;
        } else if (adviceCount.get(ADVICE_UNKNOWN) == 1 || adviceCount.get(ADVICE_NEED_CONFIRM) == 1) {
            return ADVICE_NEED_CONFIRM;
        } else if (adviceCount.get(ADVICE_SUSPECTED_ERROR) == 1
                && adviceCount.get(ADVICE_NO_NEED) == totalUnconfirmed - 1) {
            return ADVICE_NO_NEED;
        }
        return ADVICE_NEED_CONFIRM;
    }

    /**
     * 转换告警类型描述
     */
    public String getTypeReflect(String eventType) {
        if (eventType == null || eventType.isEmpty()) return "数据异常";
        switch (eventType) {
            case "抛洒物": return "路面异常";
            case "停驶": return "车辆停驶";
            case "行人": return "行人闯入";
            default: return "不在判定范围";
        }
    }

    /**
     * 获取告警集来源类型
     */
    public Integer getRelatedSourceType(String eventType){
        return "非法闯入".equals(eventType) ? SOURCE_TYPE_ILLEGAL_ENTRY : SOURCE_TYPE_NORMAL;
    }

    /**
     * 获取最小时间（预留方法，未使用）
     */
    private LocalDateTime getMinTime(List<OriginalAlarmRecord> list, String type) {
        return list.stream()
                .filter(r -> type.equals(r.getEventType()) && r.getAlarmTime() != null)
                .map(OriginalAlarmRecord::getAlarmTime)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }
}