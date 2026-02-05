package com.yuce.algorithm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yuce.entity.*;
import com.yuce.mapper.CheckAlarmProcessMapper;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.mapper.FeatureElementMapper;
import com.yuce.mapper.RoadCheckRecordMapper;
import com.yuce.service.impl.FeatureElementServiceImpl;
import com.yuce.util.FlagTagUtil;
import com.yuce.util.RoadIntersectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
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

    // ------------------------------ 依赖注入 ------------------------------
    @Autowired
    private CheckAlarmProcessMapper checkAlarmProcessMapper;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private FeatureElementMapper featureElementMapper;

    @Autowired
    private RoadCheckRecordMapper roadCheckRecordMapper;

    @Autowired
    private FeatureElementServiceImpl featureElementServiceImpl;


    // ------------------------------ 核心业务方法 ------------------------------
    /**
     * 特征要素处理（模型算法逻辑）：基于检测结果提取告警特征，生成处置建议
     */
    public boolean featureElementDealByAlgo(OriginalAlarmRecord record) {
        // 提取核心字段，校验非空
        long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        // 重复处理校验（避免重复入库）
        if (featureElementServiceImpl.isExistByTblId(tblId)) {
            log.info("告警记录完成特征要素,不再处理 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            return true;
        }

        // 填充特征要素属性值
        CheckAlarmResult checkResult = checkAlarmResultMapper.getResultByTblId(tblId);
        if (checkResult == null) {
            log.info("自研算法_缺失算法初检结果，无法进行后续处理 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            return false;
        }
        int checkFlag = checkResult.getCheckFlag();

        // 创建特征要素基础对象
        FeatureElementRecord featureRecord = initFeatureRecord(record);

        // 设置目标物体名称属性
        CheckAlarmProcess top1Process = checkAlarmProcessMapper.getIouTop1ByTblId(tblId);
        if (top1Process == null) {
            log.info("自研算法_缺失算法初检过程，物体检测相关属性默认为null：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            featureRecord.setAlarmElement(""); //设置告警物体名称
        }else{
            featureRecord.setAlarmElement(top1Process.getName());
        }

        // 设置告警物体周边物体、救援力量、拥堵状态、车道占用、涉事车辆、涉事人员属性
        List<CheckAlarmProcess> allProcessList = checkAlarmProcessMapper.getListByTblId(tblId);
        if (CollectionUtils.isEmpty(allProcessList)) {
            log.info("自研算法_缺失检测物体列表，物体检测相关属性默认为null：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            featureRecord.setAlarmElementRange("");
            featureRecord.setRescueForce("");
            featureRecord.setCongestionStatus(0);
            featureRecord.setLaneOccupyInfo("");
        }else {
            // 周边物体（按imageId分组的物体数量）
            List<Map<String, Integer>> elementRangeList = checkAlarmProcessMapper.getElementGroupByKey(
                    alarmId, imagePath, videoPath, top1Process.getImageId(), top1Process.getId().intValue()
            );
            featureRecord.setAlarmElementRange(JSON.toJSONString(elementRangeList));

            // 救援力量（循环提取）
            Set<String> rescueSet = new HashSet<>();
            for (CheckAlarmProcess process : allProcessList) {
                String name = process.getName();
                if (FlagTagUtil.ElementConstant.RESCUE_LIST.contains(name)) {
                    rescueSet.add(name);
                }
            }
            featureRecord.setRescueForce(StringUtils.collectionToCommaDelimitedString(rescueSet));

            // 提取场景特征（拥堵状态、车道占用）
            extractSceneFeatures(featureRecord, alarmId, imagePath, videoPath, allProcessList);

            // 提取涉事对象特征（车辆/人员）
            extractInvolvedFeatures(featureRecord, record.getEventType(), top1Process, allProcessList);
        }

        // 生成处置建议
        Map<String, Object> adviceMap = getAdviceInfo(record, checkFlag, top1Process.getName());
        featureRecord.setDisposalAdvice((Integer) adviceMap.get("disposalAdvice"));
        featureRecord.setAdviceReason((String) adviceMap.get("adviceReason"));

        log.info("告警记录完成特征要素： tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);

        featureElementMapper.insert(featureRecord);
        return true;
    }

    /**
     * 特征要素处理（通用算法逻辑）：基于人工/通用规则提取特征，生成处置建议
     */
    public void featureElementDealByGen(OriginalAlarmRecord record, int checkFlag) {
        // 1. 提取核心字段，校验非空
        long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        // 2. 重复处理校验
        if (featureElementServiceImpl.isExistByTblId(tblId)) {
            log.info("告警记录已完成特征要素,不再处理 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
        }else{
            // 3. 初始化基础属性字段
            FeatureElementRecord featureRecord = initFeatureRecord(record);

            // 4. 填充算法检测字段
            featureRecord.setWeatherCondition(record.getWeather());
            featureRecord.setLaneOccupyInfo(null);
            featureRecord.setCongestionStatus(null);
            featureRecord.setDangerElement(null);
            featureRecord.setInvolvedVehicleInfo(null);
            featureRecord.setInvolvedPersonInfo(null);
            featureRecord.setRescueForce(null);
            featureRecord.setAlarmElement(null);
            featureRecord.setAlarmElementRange(null);

            // 5. 生成处置建议
            Map<String, Object> adviceMap = getAdviceInfo(record, checkFlag, "");
            featureRecord.setDisposalAdvice((Integer)(adviceMap.get("disposalAdvice")));
            featureRecord.setAdviceReason(String.valueOf(adviceMap.get("adviceReason")));

            // 6. 入库
            featureElementServiceImpl.save(featureRecord);
            log.info("告警记录完成特征要素 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
        }
    }

    /**
     * 初始化特征要素记录（基础字段赋值）
     */
    private FeatureElementRecord initFeatureRecord(OriginalAlarmRecord record) {
        FeatureElementRecord featureRecord = new FeatureElementRecord();
        featureRecord.setTblId(record.getTblId());
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
                if (area > totalRoadArea * FlagTagUtil.ROAD_AREA_THRESHOLD) {
                    over70AreaImgCount++;
                }
            }
        }

        // 1.5 判定拥堵
        int congestionStatus = (over10VehicleImgCount >= FlagTagUtil.CONGESTION_IMG_THRESHOLD
                && over70AreaImgCount >= FlagTagUtil.CONGESTION_IMG_THRESHOLD) ? 1 : 0;
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
                    if (FlagTagUtil.ElementConstant.PERSON_LIST.contains(name)) {
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
        List<RoadCheckRecord> roadCheckList = roadCheckRecordMapper.getRecordByKeyAndType(alarmId, imagePath, videoPath, "road");
        if (CollectionUtils.isEmpty(roadCheckList)) {
            log.debug("未查询到路面检测记录，总面积为0 | alarmId:{}", alarmId);
            return 0.0;
        }

        double totalArea = 0.0;
        for (RoadCheckRecord roadRecord : roadCheckList) {
            totalArea += RoadIntersectionUtil.calculatePolygonArea(roadRecord.getScaledUpPoints());
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
            if (lane.getPercent() >= FlagTagUtil.LANE_OCCUPY_THRESHOLD) {
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
        featureRecord.setDisposalAdvice(FlagTagUtil.ADVICE_UNDETERMINED);
        featureRecord.setAdviceReason("算法未检测到目标");
        featureRecord.setCongestionStatus(null);
        featureRecord.setLaneOccupyInfo(null);
    }

    /**
     * 获取处理建议
     *  初检结果为0：
     *      处置建议为无法判断
     *  初检结果为1：
     *      判断物体是否无需处理类型
     *          --是：处置建议为无需处理
     *          --否：暂不赋值，待告警集完成后再进行判定
     *  初检结果为2：
     *      处置建议为疑似误报
     */
    public Map<String, Object> getAdviceInfo(OriginalAlarmRecord record, int checkFlag, String name) {
        int disposalAdvice = FlagTagUtil.ADVICE_UNDETERMINED;
        String adviceReason = "";
        Map<String, Object> map = new HashMap<>();

        String eventType = record.getEventType();

        if(checkFlag == 0){
            disposalAdvice = FlagTagUtil.ADVICE_UNDETERMINED;
            adviceReason = "算法检测前置数据异常";
        }else if (checkFlag == 2) {
            disposalAdvice = FlagTagUtil.ADVICE_FALSE_ALARM;
            if (eventType.equals("停驶")) {
                adviceReason = "物体类型非车辆";
            } else if (eventType.equals("行人")) {
                adviceReason = "物体类型非行人";
            } else if (eventType.equals("抛洒物")) {
                adviceReason = "物体类型非抛洒物";
            } else {
                adviceReason = "物体类型异常";
            }
        }else if(checkFlag == 1){
            //无需处理对象
            if (FlagTagUtil.ElementConstant.IGNORE_VEHICLE_LIST.contains(name)) {
                disposalAdvice = FlagTagUtil.ADVICE_NO_NEED;
                if (name.equals("ambulance") || name.equals("fire_fighting_truck")) {
                    adviceReason = "应急救援";
                } else if (name.equals("police_car")) {
                    adviceReason = "交通管理";
                } else if (name.equals("anti_collision_vehicle") || name.equals("maintenance_construction_vehicle")) {
                    adviceReason = "道路作业(短期/持续性作业)";
                }
            } else if (FlagTagUtil.ElementConstant.IGNORE_PERSON_LIST.contains(name)) {
                disposalAdvice = FlagTagUtil.ADVICE_NO_NEED;
                if (name.equals("medical_person")) {
                    adviceReason = "应急救援";
                } else if (name.equals("traffic_police")) {
                    adviceReason = "交通管理";
                } else if (name.equals("builder")) {
                    adviceReason = "道路作业(短期/持续性作业)";
                }
            } else if (FlagTagUtil.ElementConstant.IGNORE_PSW_LIST.contains(name)) {
                disposalAdvice = FlagTagUtil.ADVICE_NO_NEED;
                adviceReason = "不影响通行的小抛洒物";
            } else{
                disposalAdvice = FlagTagUtil.ADVICE_TMP_WAIT;
                adviceReason = "需等待告警集处理";
            }
        }
        map.put("disposalAdvice", disposalAdvice);
        map.put("adviceReason", adviceReason);
        log.info("处置建议生成 | alarmId:{} | advice:{} | reason:{}", record.getId(), disposalAdvice, adviceReason);
        return map;
    }
}