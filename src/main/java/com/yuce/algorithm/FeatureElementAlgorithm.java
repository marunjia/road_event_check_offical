package com.yuce.algorithm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.entity.FeatureElementRecord;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.*;
import com.yuce.util.IouUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @ClassName FeatureElementServiceImpl
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/6 13:45
 * @Version 1.0
 */

@Service
@Slf4j
public class FeatureElementAlgorithm{

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

    public static final List<String> personList = Arrays.asList("person","traffic_police","medical_person","builder");//人员类型清单
    public static final List<String> rescueList = Arrays.asList("anti_collision_vehicle","maintenance_construction_vehicle","police_car","ambulance","fire_fighting_truck","traffic_police","medical_person","builder");//施救力量清单
    public static final List<String> ignoreVehicleList = Arrays.asList("anti_collision_vehicle", "maintenance_construction_vehicle", "police_car", "sedan", "ambulance", "fire_fighting_truck");
    public static final List<String> ignorePersonList = Arrays.asList("medical_person", "builder", "traffic_police");
    public static final List<String> ignorePswList = Arrays.asList("paper", "plastic bags", "plastic", "cardboard", "warning triangle");

    /**
     * @desc 根据alarmId的更新特征要素信息
     * @param record
     */
    public void featureElementDealByAlgo(OriginalAlarmRecord record){

        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        if(featureElementMapper.getFeatureByKey(alarmId,imagePath,videoPath) != null){
            log.info("告警记录特征要素已判定:alarmId->{}, imagePath->{}, videoPath->{}", alarmId, imagePath, videoPath);
            return;
        }

        CheckAlarmProcess checkAlarmProcess = checkAlarmProcessMapper.getIouTop1ByKey(alarmId,imagePath,videoPath);//查询告警记录检测结果iou top1对象
        log.info("告警记录信息：alarmId->{},imagePath->{},videoPath->{};检测过程信息:{}", alarmId, imagePath, videoPath, checkAlarmProcess.toString());
        List<CheckAlarmProcess> list = checkAlarmProcessMapper.getListByKey(alarmId, imagePath, videoPath);//查询告警记录所有检测结果
        log.info("告警记录信息：alarmId->{},imagePath->{},videoPath->{};所有检测过程信息:{}", alarmId, imagePath, videoPath, list.toString());
        int checkFlag = checkAlarmResultMapper.getResultByKey(alarmId,imagePath,videoPath).getCheckFlag();//查询告警记录检测结果
        log.info("告警记录信息：alarmId->{},imagePath->{},videoPath->{};检测结果:{}", alarmId, imagePath, videoPath, checkFlag);

        List<Map<String, Integer>> resultList = checkAlarmProcessMapper.getElementGroupByKey(alarmId, imagePath, videoPath, checkAlarmProcess.getImageId(), checkAlarmProcess.getId().intValue());//查询周边告警物体数量

        //封装特征要素对象
        FeatureElementRecord featureElementRecord = new FeatureElementRecord();
        featureElementRecord.setAlarmId(alarmId);//告警id
        featureElementRecord.setImagePath(imagePath);
        featureElementRecord.setVideoPath(videoPath);
        featureElementRecord.setAbnormalLocation(record.getAlarmPlace());//告警地点
        String eventType = record.getEventType();
        featureElementRecord.setInvolvedPersonInfo(record.getWeather());//天气信息
        featureElementRecord.setAbnormalType(eventType);//事件类型
        featureElementRecord.setLaneOccupyInfo(null);//占道情况
        featureElementRecord.setCongestionStatus(null);//拥堵情况
        featureElementRecord.setDangerElement(null);//危险要素

        //判断检查结果
        if(checkAlarmProcess != null && list.size() > 0){
            log.info("特征要素判定逻辑：alarmId->{},imagePath->{},videoPath->{},enentType->{}", alarmId, imagePath, videoPath, eventType);
            featureElementRecord.setAlarmElement(checkAlarmProcess.getName()); //告警物体
            featureElementRecord.setAlarmElementRange(JSON.toJSONString(resultList)); //告警物周边物体

            Set<String> personSet = new HashSet<>();//人员类型集合
            Set<String> rescueSet = new HashSet<>();//救援力量(工程救援车辆、施工人员)

            for(int i = 0; i < list.size(); i++){
                String name = list.get(i).getName();

                if(personList.contains(name)){
                    personSet.add(name);
                }
                if(rescueList.contains(name)){
                    rescueSet.add(name);
                }
            }
            log.info("人员类型集合：alarmId->{},imagePath->{},videoPath->{},eventType->{},personSet", alarmId, imagePath, videoPath, eventType,personSet);
            log.info("救援类型集合：alarmId->{},imagePath->{},videoPath->{},eventType->{},rescueSet", alarmId, imagePath, videoPath, eventType,rescueSet);

            featureElementRecord.setRescueForce(String.join(",",rescueSet));//救援力量

            //处理涉事车辆、涉事人员
            if(eventType.equals("停驶")){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("vehicleType", checkAlarmProcess.getName());
                jsonObject.put("vehicleNum", 1);
                featureElementRecord.setInvolvedVehicleInfo(jsonObject.toJSONString());//涉事车辆
                featureElementRecord.setInvolvedPersonInfo(null);//无涉事人员
            }else if(eventType.equals("行人")){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("personType", checkAlarmProcess.getName());
                jsonObject.put("personNum", 1);
                featureElementRecord.setInvolvedVehicleInfo(null);//无涉事车辆
                featureElementRecord.setInvolvedPersonInfo(jsonObject.toJSONString());//涉事人员
            }else if(eventType.equals("抛洒物")){
                featureElementRecord.setInvolvedVehicleInfo(null);//涉事车辆
                featureElementRecord.setInvolvedPersonInfo(String.join(",", personSet));//涉事人员
            }else{
                log.info("告警id:{},事件类型:{},尚未对接",alarmId, eventType);
            }
            Map<String,Object> map = getAdviceInfo(record,checkFlag,eventType,checkAlarmProcess.getName());
            featureElementRecord.setDisposalAdvice((int)map.get("disposalAdvice"));//建议类型
            featureElementRecord.setAdviceReason((String)map.get("adviceReason"));//建议依据
        }else{
            featureElementRecord.setAlarmElement(null); //告警物体
            featureElementRecord.setAlarmElementRange(null); //告警物周边物体
            featureElementRecord.setInvolvedVehicleInfo(null);
            featureElementRecord.setInvolvedPersonInfo(null);
            featureElementRecord.setRescueForce(null);
            Map<String,Object> map = getAdviceInfo(record,checkFlag,eventType,checkAlarmProcess.getName());
            featureElementRecord.setDisposalAdvice((int)map.get("disposalAdvice"));//建议类型
            featureElementRecord.setAdviceReason((String)map.get("adviceReason"));//建议依据
        }
        log.info("特征要素逻辑处理完成：featureElementRecord->{}", featureElementRecord.toString());
        featureElementMapper.insert(featureElementRecord);
    }

    /**
     * @desc 通用算法处理结果
     * @param record
     */
    public void featureElementDealByGen(OriginalAlarmRecord record, String reason){
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        if(featureElementMapper.getFeatureByKey(alarmId,imagePath,videoPath) != null){
            log.info("告警记录特征要素已判定:alarmId->{}, imagePath->{}, videoPath->{}", alarmId, imagePath, videoPath);
            return;
        }

        //封装特征要素对象
        FeatureElementRecord featureElementRecord = new FeatureElementRecord();
        featureElementRecord.setAlarmId(alarmId);//告警id
        featureElementRecord.setImagePath(imagePath);
        featureElementRecord.setVideoPath(videoPath);
        featureElementRecord.setAbnormalLocation(record.getAlarmPlace());//告警地点
        String eventType = record.getEventType();
        featureElementRecord.setInvolvedPersonInfo(record.getWeather());//天气信息
        featureElementRecord.setAbnormalType(eventType);//事件类型
        featureElementRecord.setLaneOccupyInfo(null);//占道情况
        featureElementRecord.setCongestionStatus(null);//拥堵情况
        featureElementRecord.setDangerElement(null);//危险要素
        featureElementRecord.setInvolvedVehicleInfo(null);
        featureElementRecord.setInvolvedPersonInfo(null);
        featureElementRecord.setRescueForce(null);
        featureElementRecord.setDisposalAdvice(0);//经过通用算法检测的处置建议为0
        featureElementRecord.setAdviceReason(reason);//判断无法处理原因
        featureElementRecord.setAlarmElement(null);
        featureElementRecord.setAlarmElementRange(null);
        featureElementMapper.insert(featureElementRecord);
    }

    /**
     * @desc 重复告警场景处理建议
     * @param record
     * @param name
     * @return
     */
    public Map<String,Object> getAdviceFlagRepeat(OriginalAlarmRecord record, String name){
        Map<String,Object> map = new HashMap<>();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String eventType = record.getEventType();
        String deviceId = record.getDeviceId();
        LocalDateTime alarmTime = record.getAlarmTime();

        ExtractWindowRecord extractWindowRecord = extractWindowMapper.getWindowByKey(alarmId,imagePath,videoPath);//查询当前告警记录坐标框
        OriginalAlarmRecord leadOriginalAlarmRecord = originalAlarmMapper.getLastByDeviceAndType(deviceId,eventType,alarmTime);//查询同点位上一条异常事件

        if(leadOriginalAlarmRecord == null){//无上条告警记录
            log.info("点位->{},告警类型->{},告警记录信息：alarmId->{},imagePath->{},videoPath->{};上条告警记录信息为空", deviceId, eventType, alarmId, imagePath, videoPath);
            return null;//尽快确认
        }

        String leadAlarmId = leadOriginalAlarmRecord.getId();
        String leadImagePath = leadOriginalAlarmRecord.getImagePath();
        String leadVideoPath = leadOriginalAlarmRecord.getVideoPath();
        log.info("点位->{},告警类型->{},告警记录信息：alarmId->{},imagePath->{},videoPath->{};上条信息alarmId->{},imagePath->{},videoPath->{}", deviceId, eventType, alarmId, imagePath, videoPath, leadAlarmId,leadImagePath, leadVideoPath);

        CheckAlarmProcess leadCheckAlarmProcess = checkAlarmProcessMapper.getIouTop1ByKey(leadAlarmId, leadImagePath, leadVideoPath);
        if(leadCheckAlarmProcess == null){//无IOU检测结果
            log.info("点位->{},告警类型->{},告警记录信息：alarmId->{},imagePath->{},videoPath->{};上条告警记录算法检测过程为空", deviceId, eventType, alarmId, imagePath, videoPath);
            return null;//尽快确认
        }

        ExtractWindowRecord leadExtractWindowRecord = extractWindowMapper.getWindowByKey(leadAlarmId,leadImagePath,leadVideoPath);
        if(leadExtractWindowRecord == null){//无坐标框
            log.info("点位->{},告警类型->{},告警记录信息：alarmId->{},imagePath->{},videoPath->{};上条告警记录提框结果为空", deviceId, eventType, alarmId, imagePath, videoPath);
            return null;//尽快确认
        }

        int point1X = extractWindowRecord.getPoint1X();
        int point1Y = extractWindowRecord.getPoint1Y();
        int point2X = extractWindowRecord.getPoint2X();
        int point2Y = extractWindowRecord.getPoint2Y();

        int leadPoint1X = leadExtractWindowRecord.getPoint1X();
        int leadPoint1Y = leadExtractWindowRecord.getPoint1Y();
        int leadPoint2X = leadExtractWindowRecord.getPoint2X();
        int leadPoint2Y = leadExtractWindowRecord.getPoint2Y();

        double iou = IouUtil.calculateIoU(point1X, point1Y, point2X, point2Y, leadPoint1X, leadPoint1Y, leadPoint2X, leadPoint2Y);
        long minutes = Duration.between(record.getAlarmTime(), leadOriginalAlarmRecord.getAlarmTime()).toMinutes();
        log.info("点位->{},告警类型->{},告警记录信息：alarmId->{},imagePath->{},videoPath->{};上条信息alarmId->{},imagePath->{},videoPath->{}; 两天记录坐标框IOU->{}, 间隔时长->{}", deviceId, eventType, alarmId, imagePath, videoPath, leadAlarmId,leadImagePath, leadVideoPath, iou, minutes);

        if((eventType.equals("停驶") && name.equals(leadCheckAlarmProcess.getName()) && iou >= 0.5 && minutes <= 10)
            || (eventType.equals("行人") && name.equals(leadCheckAlarmProcess.getName()) && minutes <= 15)
            || (eventType.equals("抛洒物") && name.equals(leadCheckAlarmProcess.getName()) && minutes <= 60)
            || (eventType.equals("抛洒物") && name.equals(leadCheckAlarmProcess.getName()) && minutes > 60 && minutes <= 1440 && iou >= 0.7)
            || (eventType.equals("抛洒物") && minutes > 1440)
        ){
            map.put("disposalAdvice",3);
            map.put("adviceReason","重复告警");
            return map;
        }
        return null;
    }

    /**
     * @desc 无需处理类型告警处理建议
     * @param checkFlag
     * @param eventType
     * @param name
     * @return
     */
    public Map<String,Object> getAdviceInfo(OriginalAlarmRecord record,int checkFlag, String eventType, String name) {
        int disposalAdvice = 0;
        String adviceReason = "";
        Map<String,Object> map = new HashMap<>();

        /**
         * @desc 根据物体类型判定
         */
        if (checkFlag == 0) {
            //0-无法判断、1-疑似误报、2-尽快确认、3-无需处理
            disposalAdvice = 0; //理论上经过算法检验的没有无法判断
            adviceReason = "异常情况，请联系开发查看";

        } else if (checkFlag == 1) {
            //无需处理类型检测
            if(ignoreVehicleList.contains(name)){
                disposalAdvice = 3;
                adviceReason = "道路作业(短期/持续性作业)";
            }else if(ignorePersonList.contains(name)){
                disposalAdvice = 3;
                adviceReason = "道路作业(短期/持续性作业)";
            }else if(ignorePswList.contains(name)) {
                disposalAdvice = 3;
                adviceReason = "不影响通行的小抛洒物";
            }else{
                //重复告警检测
                if(getAdviceFlagRepeat(record,name)!=null){
                    return getAdviceFlagRepeat(record,name);
                }else{
                    disposalAdvice = 2;
                    adviceReason = "无需依据";
                }
            }
        } else if (checkFlag == 2) {
            disposalAdvice = 1; //疑似误报
            if (eventType.equals("停驶")) {
                adviceReason = "物体类型非车辆";
            } else if (eventType.equals("行人")) {
                adviceReason = "物体类型非行人";
            } else if (eventType.equals("抛洒物")) {
                adviceReason = "物体类型非抛洒物";
            } else {
                adviceReason = "物体类型异常";
            }
        } else {
            disposalAdvice = 2;
            adviceReason = "无需依据";
        }
        map.put("disposalAdvice", disposalAdvice);
        map.put("adviceReason", adviceReason);
        log.info("alarmId->{}, imagePath->{}, videoPath->{}, disposalAdvice->{}, adviceReason->{}", record.getId(), record.getImagePath(), record.getVideoPath(), disposalAdvice, adviceReason);
        return map;
    }
}