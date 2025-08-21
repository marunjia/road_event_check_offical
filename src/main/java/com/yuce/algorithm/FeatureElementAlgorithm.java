package com.yuce.algorithm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yuce.entity.*;
import com.yuce.mapper.CheckAlarmProcessMapper;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.mapper.ExtractWindowMapper;
import com.yuce.mapper.FeatureElementMapper;
import com.yuce.mapper.OriginalAlarmMapper;
import com.yuce.mapper.RoadCheckRecordMapper;
import com.yuce.util.IouUtil;
import com.yuce.util.RoadIntersectionUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 特征要素算法服务：处理告警记录的特征提取、处置建议生成
 * 核心功能：模型算法特征处理、通用算法特征处理、重复告警判定、处置建议生成
 * 适配 JDK 1.8
 */
@Service
@Slf4j
public class FeatureElementAlgorithm {

    // ------------------------------ 常量定义（内部静态类封装，便于维护） ------------------------------
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ElementConstant {
        /** 人员类型清单 */
        public static final List<String> PERSON_LIST = new ArrayList<String>() {{
            add("person");
            add("traffic_police");
            add("medical_person");
            add("builder");
        }};

        /** 施救力量清单 */
        public static final List<String> RESCUE_LIST = new ArrayList<String>() {{
            add("anti_collision_vehicle");
            add("maintenance_construction_vehicle");
            add("police_car");
            add("ambulance");
            add("fire_fighting_truck");
            add("traffic_police");
            add("medical_person");
            add("builder");
        }};

        /** 忽略的车辆类型（无需处理） */
        public static final List<String> IGNORE_VEHICLE_LIST = new ArrayList<String>() {{
            add("anti_collision_vehicle");
            add("maintenance_construction_vehicle");
            add("police_car");
            add("ambulance");
            add("fire_fighting_truck");
        }};

        /** 忽略的人员类型（无需处理） */
        public static final List<String> IGNORE_PERSON_LIST = new ArrayList<String>() {{
            add("medical_person");
            add("builder");
            add("traffic_police");
        }};

        /** 忽略的抛洒物类型（无需处理） */
        public static final List<String> IGNORE_PSW_LIST = new ArrayList<String>() {{
            add("paper");
            add("plastic bags");
            add("plastic");
            add("cardboard");
            add("warning triangle");
        }};

        /** 处置建议：无法判断 */
        public static final int ADVICE_UNDETERMINED = 0;
        /** 处置建议：疑似误报 */
        public static final int ADVICE_FALSE_ALARM = 1;
        /** 处置建议：尽快确认 */
        public static final int ADVICE_CONFIRM = 2;
        /** 处置建议：无需处理（含重复告警） */
        public static final int ADVICE_NO_NEED = 3;

        /** 初检结果：无法判断 */
        public static final int CHECK_FLAG_UNDETERMINED = 0;
        /** 初检结果：疑似误报 */
        public static final int CHECK_FLAG_FALSE_ALARM = 1;
        /** 初检结果：正检（需进一步判定） */
        public static final int CHECK_FLAG_POSITIVE = 2;

        /** 拥堵判定阈值：满足条件的图片数量 */
        public static final int CONGESTION_IMG_THRESHOLD = 2;
        /** 路面占比阈值：判定拥堵的面积占比 */
        public static final double ROAD_AREA_THRESHOLD = 0.7;
        /** 车道占用阈值：判定占道的百分比 */
        public static final double LANE_OCCUPY_THRESHOLD = 0.1;
    }


    // ------------------------------ 依赖注入 ------------------------------
    @Autowired
    private CheckAlarmProcessMapper checkAlarmProcessMapper;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private ExtractWindowMapper extractWindowMapper;

    @Autowired
    private OriginalAlarmMapper originalAlarmMapper;

    @Autowired
    private FeatureElementMapper featureElementMapper;

    @Autowired
    private RoadCheckRecordMapper roadCheckRecordMapper;


    // ------------------------------ 核心业务方法 ------------------------------
    /**
     * 特征要素处理（模型算法逻辑）：基于检测结果提取告警特征，生成处置建议
     */
    public void featureElementDealByAlgo(OriginalAlarmRecord record) {
        // 1. 提取核心字段，校验非空
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        if (!validateCoreFields(alarmId, imagePath, videoPath)) {
            return;
        }

        // 2. 重复处理校验（避免重复入库）
        if (isFeatureProcessed(alarmId, imagePath, videoPath)) {
            log.info("告警记录特征要素已处理，跳过 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
            return;
        }

        // 3. 初始化特征要素记录（基础字段赋值）
        FeatureElementRecord featureRecord = initFeatureRecord(record);

        try {
            // 4. 查询依赖数据（初检结果、TOP1检测物体、所有检测物体）
            CheckAlarmResult checkResult = checkAlarmResultMapper.getResultByKey(alarmId, imagePath, videoPath);
            int checkFlag = (checkResult != null) ? checkResult.getCheckFlag() : ElementConstant.CHECK_FLAG_UNDETERMINED;

            CheckAlarmProcess top1Process = checkAlarmProcessMapper.getIouTop1ByKey(alarmId, imagePath, videoPath);
            List<CheckAlarmProcess> allProcessList = checkAlarmProcessMapper.getListByKey(alarmId, imagePath, videoPath);

            // 5. 有检测结果时，提取特征；无结果时用默认值
            if (top1Process != null && !CollectionUtils.isEmpty(allProcessList)) {
                // 5.1 提取基础特征（告警物体、周边物体、救援力量）
                extractBaseFeatures(featureRecord, alarmId, imagePath, videoPath, top1Process, allProcessList);

                // 5.2 提取场景特征（拥堵状态、车道占用）
                extractSceneFeatures(featureRecord, alarmId, imagePath, videoPath, allProcessList);

                // 5.3 提取涉事对象特征（车辆/人员）
                extractInvolvedFeatures(featureRecord, record.getEventType(), top1Process, allProcessList);

                // 5.4 生成处置建议
                Map<String, Object> adviceMap = getAdviceInfo(record, checkFlag, record.getEventType(), top1Process.getName());
                featureRecord.setDisposalAdvice((Integer) adviceMap.get("disposalAdvice"));
                featureRecord.setAdviceReason((String) adviceMap.get("adviceReason"));

            } else {
                // 无检测结果时，设置默认值
                setDefaultFeatureValues(featureRecord);
                log.warn("告警记录无检测结果，使用默认特征值 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
            }

            // 6. 入库特征记录
            featureElementMapper.insert(featureRecord);
            log.info("告警记录特征要素处理完成，已入库 | alarmId:{} | imagePath:{} | videoPath:{} | featureRecord:{}",  alarmId, imagePath, videoPath, JSON.toJSONString(featureRecord));
        } catch (Exception e) {
            log.error("特征要素处理异常 | alarmId:{} | imagePath:{} | videoPath:{}",
                    alarmId, imagePath, videoPath, e);
            // 异常时仍入库（避免后续重复处理失败），标记异常原因
            featureRecord.setAdviceReason("特征处理异常：" + e.getMessage());
            featureElementMapper.insert(featureRecord);
        }
    }

    /**
     * 特征要素处理（通用算法逻辑）：基于人工/通用规则提取特征，生成处置建议
     */
    public void featureElementDealByGen(OriginalAlarmRecord record, int checkFlag, String reason) {
        // 1. 提取核心字段，校验非空
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        if (!validateCoreFields(alarmId, imagePath, videoPath)) {
            return;
        }

        // 2. 重复处理校验
        if (isFeatureProcessed(alarmId, imagePath, videoPath)) {
            log.info("告警记录特征要素已处理，跳过 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
            return;
        }

        // 3. 初始化特征记录（基础字段赋值）
        FeatureElementRecord featureRecord = initFeatureRecord(record);

        try {
            // 4. 通用逻辑：仅设置基础字段和处置建议，无检测相关特征
            featureRecord.setInvolvedPersonInfo(record.getWeather()); // 注意：原逻辑将天气存入人员信息字段，建议确认是否为笔误
            featureRecord.setLaneOccupyInfo(null);
            featureRecord.setCongestionStatus(null);
            featureRecord.setDangerElement(null);
            featureRecord.setInvolvedVehicleInfo(null);
            featureRecord.setInvolvedPersonInfo(null);
            featureRecord.setRescueForce(null);
            featureRecord.setAlarmElement(null);
            featureRecord.setAlarmElementRange(null);

            // 5. 生成处置建议
            Map<String, Object> adviceMap = getAdviceInfo(record, checkFlag, record.getEventType(), "");
            featureRecord.setDisposalAdvice((Integer) adviceMap.get("disposalAdvice"));
            featureRecord.setAdviceReason(reason); // 外部传入的原因

            // 6. 入库
            featureElementMapper.insert(featureRecord);
            log.info("告警记录通用特征处理完成，已入库 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);

        } catch (Exception e) {
            log.error("通用特征要素处理异常 | alarmId:{} | imagePath:{} | videoPath:{}，异常详情：", alarmId, imagePath, videoPath, e);
            featureRecord.setAdviceReason("通用处理异常：" + e.getMessage());
            featureElementMapper.insert(featureRecord);
        }
    }


    // ------------------------------ 私有工具方法 ------------------------------
    /**
     * 校验核心字段非空（alarmId、imagePath、videoPath）
     */
    private boolean validateCoreFields(String alarmId, String imagePath, String videoPath) {
        if (StringUtils.isEmpty(alarmId)) {
            log.error("告警ID为空，终止特征处理");
            return false;
        }
        if (StringUtils.isEmpty(imagePath)) {
            log.error("图片路径为空，终止特征处理 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
            return false;
        }
        if (StringUtils.isEmpty(videoPath)) {
            log.error("视频路径为空，终止特征处理 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
            return false;
        }
        return true;
    }

    /**
     * 检查特征要素是否已处理（避免重复入库）
     */
    private boolean isFeatureProcessed(String alarmId, String imagePath, String videoPath) {
        try {
            return featureElementMapper.getFeatureByKey(alarmId, imagePath, videoPath) != null;
        } catch (Exception e) {
            log.error("查询特征处理状态异常，默认判定为未处理 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath, e);
            return false;
        }
    }

    /**
     * 初始化特征要素记录（基础字段赋值）
     */
    private FeatureElementRecord initFeatureRecord(OriginalAlarmRecord record) {
        FeatureElementRecord featureRecord = new FeatureElementRecord();
        featureRecord.setAlarmId(record.getId());
        featureRecord.setImagePath(record.getImagePath());
        featureRecord.setVideoPath(record.getVideoPath());
        featureRecord.setAbnormalLocation(record.getAlarmPlace());
        featureRecord.setAbnormalType(record.getEventType());
        featureRecord.setWeatherCondition(record.getWeather());
        featureRecord.setDangerElement(null);
        featureRecord.setCollectionMatchStatus(0); // 默认未匹配
        return featureRecord;
    }

    /**
     * 提取基础特征：告警物体、周边物体、救援力量
     */
    private void extractBaseFeatures(FeatureElementRecord featureRecord, String alarmId, String imagePath, String videoPath,
                                     CheckAlarmProcess top1Process, List<CheckAlarmProcess> allProcessList) {
        // 1. 告警物体（TOP1检测物体）
        String top1Name = top1Process.getName();
        featureRecord.setAlarmElement(top1Name);

        // 2. 周边物体（按imageId分组的物体数量）
        List<Map<String, Integer>> elementRangeList = checkAlarmProcessMapper.getElementGroupByKey(
                alarmId, imagePath, videoPath, top1Process.getImageId(), top1Process.getId().intValue()
        );
        featureRecord.setAlarmElementRange(JSON.toJSONString(elementRangeList));

        // 3. 救援力量（循环提取）
        Set<String> rescueSet = new HashSet<>();
        for (CheckAlarmProcess process : allProcessList) {
            String name = process.getName();
            if (ElementConstant.RESCUE_LIST.contains(name)) {
                rescueSet.add(name);
            }
        }
        featureRecord.setRescueForce(StringUtils.collectionToCommaDelimitedString(rescueSet));
        log.debug("告警记录提取救援力量 | alarmId:{} | rescueSet:{}", alarmId, rescueSet);
    }

    /**
     * 提取场景特征：拥堵状态、车道占用
     */
    private void extractSceneFeatures(FeatureElementRecord featureRecord, String alarmId, String imagePath, String videoPath,
                                      List<CheckAlarmProcess> allProcessList) {
        // 1. 拥堵状态判定
        // 1.1 按imageId统计车辆数量
        Map<String, Integer> vehicleCountMap = new HashMap<>();
        for (CheckAlarmProcess process : allProcessList) {
            if ("vehicle".equals(process.getType())) {
                String imageId = process.getImageId();
                vehicleCountMap.put(imageId, vehicleCountMap.getOrDefault(imageId, 0) + 1);
            }
        }
        // 1.2 统计满足“车辆数>10”的图片数量
        int over10VehicleImgCount = 0;
        for (Integer count : vehicleCountMap.values()) {
            if (count > 10) {
                over10VehicleImgCount++;
            }
        }

        // 1.3 计算路面总面积
        double totalRoadArea = calculateTotalRoadArea(alarmId, imagePath, videoPath);

        // 1.4 按imageId统计车辆像素面积
        Map<String, Integer> vehicleAreaMap = calculateVehicleAreaByImageId(allProcessList);
        // 统计满足“面积占比>70%”的图片数量
        int over70AreaImgCount = 0;
        if (totalRoadArea > 0) {
            for (Integer area : vehicleAreaMap.values()) {
                if (area > totalRoadArea * ElementConstant.ROAD_AREA_THRESHOLD) {
                    over70AreaImgCount++;
                }
            }
        }

        // 1.5 判定拥堵
        int congestionStatus = (over10VehicleImgCount >= ElementConstant.CONGESTION_IMG_THRESHOLD
                && over70AreaImgCount >= ElementConstant.CONGESTION_IMG_THRESHOLD) ? 1 : 0;
        featureRecord.setCongestionStatus(congestionStatus);
        log.debug("告警记录拥堵判定 | alarmId:{} | 车辆超限图片数:{} | 面积超限图片数:{} | 拥堵状态:{}",
                alarmId, over10VehicleImgCount, over70AreaImgCount, congestionStatus);

        // 2. 车道占用信息
        int occupiedLaneCount = calculateOccupiedLaneCount(alarmId, imagePath, videoPath);
        featureRecord.setLaneOccupyInfo(String.format("占据%d条车道", occupiedLaneCount));
        log.debug("告警记录车道占用 | alarmId:{} | 占据车道数:{}", alarmId, occupiedLaneCount);
    }

    /**
     * 提取涉事对象特征：涉事车辆/涉事人员（按事件类型区分）
     */
    private void extractInvolvedFeatures(FeatureElementRecord featureRecord, String eventType,
                                         CheckAlarmProcess top1Process, List<CheckAlarmProcess> allProcessList) {
        String top1Name = top1Process.getName();
        switch (eventType) {
            case "停驶":
                // 涉事车辆：TOP1车辆类型，数量1
                JSONObject vehicleJson = new JSONObject();
                vehicleJson.put("vehicleType", top1Name);
                vehicleJson.put("vehicleNum", 1);
                featureRecord.setInvolvedVehicleInfo(vehicleJson.toJSONString());
                featureRecord.setInvolvedPersonInfo(null);
                break;

            case "行人":
                // 涉事人员：TOP1人员类型，数量1
                JSONObject personJson = new JSONObject();
                personJson.put("personType", top1Name);
                personJson.put("personNum", 1);
                featureRecord.setInvolvedVehicleInfo(null);
                featureRecord.setInvolvedPersonInfo(personJson.toJSONString());
                break;

            case "抛洒物":
                // 涉事人员：所有检测到的人员类型
                Set<String> personSet = new HashSet<>();
                for (CheckAlarmProcess process : allProcessList) {
                    String name = process.getName();
                    if (ElementConstant.PERSON_LIST.contains(name)) {
                        personSet.add(name);
                    }
                }
                featureRecord.setInvolvedVehicleInfo(null);
                featureRecord.setInvolvedPersonInfo(StringUtils.collectionToCommaDelimitedString(personSet));
                break;

            default:
                log.info("未处理的事件类型，涉事对象置空 | alarmId:{} | eventType:{}",
                        featureRecord.getAlarmId(), eventType);
                featureRecord.setInvolvedVehicleInfo(null);
                featureRecord.setInvolvedPersonInfo(null);
        }
    }

    /**
     * 计算路面总面积（从RoadCheckRecord中累加）
     */
    private double calculateTotalRoadArea(String alarmId, String imagePath, String videoPath) {
        List<RoadCheckRecord> roadCheckList = roadCheckRecordMapper.getRecordByKeyAndType(
                alarmId, imagePath, videoPath, "road");
        if (CollectionUtils.isEmpty(roadCheckList)) {
            log.debug("未查询到路面检测记录，总面积为0 | alarmId:{}", alarmId);
            return 0.0;
        }

        double totalArea = 0.0;
        for (RoadCheckRecord roadRecord : roadCheckList) {
            totalArea += RoadIntersectionUtil.calculatePolygonArea(roadRecord.getPoints());
        }
        return totalArea;
    }

    /**
     * 按imageId统计车辆像素面积
     */
    private Map<String, Integer> calculateVehicleAreaByImageId(List<CheckAlarmProcess> processList) {
        Map<String, Integer> areaMap = new HashMap<>();
        for (CheckAlarmProcess process : processList) {
            if (!"vehicle".equals(process.getType())) {
                continue;
            }
            // 计算单个车辆的像素面积
            int width = Math.abs(process.getPoint2X() - process.getPoint1X());
            int height = Math.abs(process.getPoint2Y() - process.getPoint1Y());
            int area = width * height;

            // 按imageId累加
            String imageId = process.getImageId();
            areaMap.put(imageId, areaMap.getOrDefault(imageId, 0) + area);
        }
        return areaMap;
    }

    /**
     * 计算被占用的车道数量（占比≥10%的车道）
     */
    private int calculateOccupiedLaneCount(String alarmId, String imagePath, String videoPath) {
        List<RoadCheckRecord> laneList = roadCheckRecordMapper.getRecordByKeyAndType(
                alarmId, imagePath, videoPath, "lane");
        if (CollectionUtils.isEmpty(laneList)) {
            return 0;
        }

        int count = 0;
        for (RoadCheckRecord lane : laneList) {
            if (lane.getPercent() >= ElementConstant.LANE_OCCUPY_THRESHOLD) {
                count++;
            }
        }
        return count;
    }

    /**
     * 设置默认特征值（无检测结果时）
     */
    private void setDefaultFeatureValues(FeatureElementRecord featureRecord) {
        featureRecord.setAlarmElement(null);
        featureRecord.setAlarmElementRange(null);
        featureRecord.setInvolvedVehicleInfo(null);
        featureRecord.setInvolvedPersonInfo(null);
        featureRecord.setRescueForce(null);
        featureRecord.setDisposalAdvice(ElementConstant.ADVICE_UNDETERMINED);
        featureRecord.setAdviceReason("算法未检测到目标");
        featureRecord.setCongestionStatus(null);
        featureRecord.setLaneOccupyInfo(null);
    }

    /**
     * 重复告警场景处理建议
     */
    public Map<String, Object> getAdviceFlagRepeat(OriginalAlarmRecord record, String name) {
        Map<String, Object> map = new HashMap<>();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String eventType = record.getEventType();
        String deviceId = record.getDeviceId();
        LocalDateTime alarmTime = record.getAlarmTime();

        // 查询当前告警记录坐标框
        ExtractWindowRecord extractWindowRecord = extractWindowMapper.getWindowByKey(alarmId, imagePath, videoPath);
        if (extractWindowRecord == null) {
            log.debug("当前告警无提框记录，无法判定重复 | alarmId:{}", alarmId);
            return null;
        }

        // 查询同点位上一条异常事件
        OriginalAlarmRecord leadOriginalAlarmRecord = originalAlarmMapper.getLastByDeviceAndType(
                deviceId, eventType, alarmTime);
        if (leadOriginalAlarmRecord == null) {
            log.debug("无上条告警记录，无法判定重复 | alarmId:{}", alarmId);
            return null;
        }

        String leadAlarmId = leadOriginalAlarmRecord.getId();
        String leadImagePath = leadOriginalAlarmRecord.getImagePath();
        String leadVideoPath = leadOriginalAlarmRecord.getVideoPath();

        // 查询上条告警的TOP1检测结果
        CheckAlarmProcess leadCheckAlarmProcess = checkAlarmProcessMapper.getIouTop1ByKey(
                leadAlarmId, leadImagePath, leadVideoPath);
        if (leadCheckAlarmProcess == null) {
            log.debug("上条告警无检测结果，无法判定重复 | alarmId:{} | 上条alarmId:{}", alarmId, leadAlarmId);
            return null;
        }

        // 查询上条告警的坐标框
        ExtractWindowRecord leadExtractWindowRecord = extractWindowMapper.getWindowByKey(
                leadAlarmId, leadImagePath, leadVideoPath);
        if (leadExtractWindowRecord == null) {
            log.debug("上条告警无提框记录，无法判定重复 | alarmId:{} | 上条alarmId:{}", alarmId, leadAlarmId);
            return null;
        }

        // 计算IOU和时间间隔
        double iou = IouUtil.calculateIoU(
                extractWindowRecord.getPoint1X(), extractWindowRecord.getPoint1Y(),
                extractWindowRecord.getPoint2X(), extractWindowRecord.getPoint2Y(),
                leadExtractWindowRecord.getPoint1X(), leadExtractWindowRecord.getPoint1Y(),
                leadExtractWindowRecord.getPoint2X(), leadExtractWindowRecord.getPoint2Y()
        );
        long minutes = Duration.between(alarmTime, leadOriginalAlarmRecord.getAlarmTime()).toMinutes();
        log.debug("重复告警判定 | alarmId:{} | 上条alarmId:{} | IOU:{} | 间隔分钟:{}",
                alarmId, leadAlarmId, iou, minutes);

        // 重复告警判定规则
        boolean isRepeat = false;
        if (eventType.equals("停驶")) {
            isRepeat = name.equals(leadCheckAlarmProcess.getName()) && iou >= 0.5 && minutes <= 10;
        } else if (eventType.equals("行人")) {
            isRepeat = name.equals(leadCheckAlarmProcess.getName()) && minutes <= 15;
        } else if (eventType.equals("抛洒物")) {
            isRepeat = (name.equals(leadCheckAlarmProcess.getName()) && minutes <= 60)
                    || (name.equals(leadCheckAlarmProcess.getName()) && minutes > 60 && minutes <= 1440 && iou >= 0.7)
                    || (minutes > 1440);
        }

        if (isRepeat) {
            map.put("disposalAdvice", ElementConstant.ADVICE_NO_NEED);
            map.put("adviceReason", "重复告警");
            return map;
        }
        return null;
    }

    /**
     * 获取通用算法处理建议
     */
    public Map<String, Object> getAdviceInfo(OriginalAlarmRecord record, int checkFlag, String eventType, String name) {
        int disposalAdvice = ElementConstant.ADVICE_UNDETERMINED;
        String adviceReason = "";
        Map<String, Object> map = new HashMap<>();

        // 处置建议规则
        if (checkFlag == ElementConstant.CHECK_FLAG_UNDETERMINED) {
            disposalAdvice = ElementConstant.ADVICE_UNDETERMINED;
            adviceReason = "异常情况，请联系开发查看";
        } else if (checkFlag == ElementConstant.CHECK_FLAG_FALSE_ALARM) {
            disposalAdvice = ElementConstant.ADVICE_FALSE_ALARM;
            if (eventType.equals("停驶")) {
                adviceReason = "物体类型非车辆";
            } else if (eventType.equals("行人")) {
                adviceReason = "物体类型非行人";
            } else if (eventType.equals("抛洒物")) {
                adviceReason = "物体类型非抛洒物";
            } else {
                adviceReason = "物体类型异常";
            }
        } else if (checkFlag == ElementConstant.CHECK_FLAG_POSITIVE) {
            // 无需处理类型检测
            if (ElementConstant.IGNORE_VEHICLE_LIST.contains(name)) {
                disposalAdvice = ElementConstant.ADVICE_NO_NEED;
                if (name.equals("ambulance") || name.equals("fire_fighting_truck")) {
                    adviceReason = "应急救援";
                } else if (name.equals("police_car")) {
                    adviceReason = "交通管理";
                } else if (name.equals("anti_collision_vehicle") || name.equals("maintenance_construction_vehicle")) {
                    adviceReason = "道路作业(短期/持续性作业)";
                }
            } else if (ElementConstant.IGNORE_PERSON_LIST.contains(name)) {
                disposalAdvice = ElementConstant.ADVICE_NO_NEED;
                if (name.equals("medical_person")) {
                    adviceReason = "应急救援";
                } else if (name.equals("traffic_police")) {
                    adviceReason = "交通管理";
                } else if (name.equals("builder")) {
                    adviceReason = "道路作业(短期/持续性作业)";
                }
            } else if (ElementConstant.IGNORE_PSW_LIST.contains(name)) {
                disposalAdvice = ElementConstant.ADVICE_NO_NEED;
                adviceReason = "不影响通行的小抛洒物";
            } else {
                // 重复告警检测
                Map<String, Object> repeatMap = getAdviceFlagRepeat(record, name);
                if (repeatMap != null) {
                    return repeatMap;
                } else {
                    disposalAdvice = ElementConstant.ADVICE_CONFIRM;
                    adviceReason = "";
                }
            }
        } else {
            disposalAdvice = ElementConstant.ADVICE_CONFIRM;
            adviceReason = "";
        }

        map.put("disposalAdvice", disposalAdvice);
        map.put("adviceReason", adviceReason);
        log.info("处置建议生成 | alarmId:{} | advice:{} | reason:{}", record.getId(), disposalAdvice, adviceReason);
        return map;
    }
}