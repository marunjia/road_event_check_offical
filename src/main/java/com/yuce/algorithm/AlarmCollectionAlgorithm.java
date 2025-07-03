package com.yuce.algorithm;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.config.AlarmCollectionProperties;
import com.yuce.entity.AlarmCollection;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.*;
import com.yuce.service.AlarmCollectionService;
import com.yuce.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * @ClassName AlarmCollectionService
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/15 16:53
 * @Version 1.0
 */

@Service
@Slf4j
public class AlarmCollectionAlgorithm extends ServiceImpl<AlarmCollectionMapper, AlarmCollection> implements AlarmCollectionService {
    private int rightIntervarlMinute = 0;
    private int falseIntervarlMinute = 0;
    private int uncertainIntervarlMinute = 0;

    @Autowired
    private AlarmCollectionServiceImpl alarmCollectionServiceImpl;

    @Autowired
    private AlarmCollectionProperties alarmCollectionProperties;

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private FeatureElementServiceImpl featureElementServiceImpl;

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    //启动调用一次
    @PostConstruct
    public void init() {
        //初始化告警集间隔时长
        rightIntervarlMinute = alarmCollectionProperties.getRightIntervalMinute();
        falseIntervarlMinute = alarmCollectionProperties.getFalseIntervalMinute();
        uncertainIntervarlMinute = alarmCollectionProperties.getUncertainIntervalMinute();
    }

    /**
     * @desc 告警集逻辑处理
     * @param record
     */
    public void collectionDeal(OriginalAlarmRecord record) {

        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videpPath = record.getVideoPath();
        String roadId = record.getRoadId();
        String deviceId = record.getDeviceId(); //获取设备id
        LocalDateTime alarmTime = record.getAlarmTime();
        String deviceName = getDeviceName(record.getContent());
        String eventType = record.getEventType();
        int milestone = record.getMilestone();

        int checkFlag = checkAlarmResultMapper.getResultByKey(alarmId, imagePath, videpPath).getCheckFlag();//查询算法检测结果
        AlarmCollection alarmCollection = alarmCollectionServiceImpl.getCollectionByKey(deviceId, checkFlag);//根据deviceId、checkFlag查询告警集

        /**
         * 告警集&&推定事件类型&&处置建议
         */
        if (alarmCollection != null) {
            long minutes = Math.abs(Duration.between(alarmCollection.getLatestAlarmTime(), alarmTime).toMinutes()); //根据当前记录告警时间与告警集最新时间差值
            /**
             * 1、时间差值 > 间隔时长配置：
             *      关闭旧告警集，开启新告警集；
             * 2、时间差值 <= 间隔时长配置：
             *      更新告警集属性；
             *
             */
            if (minutes > getIntervalDuration(checkFlag)) {
                alarmCollection.setCollectionStatus(2); //超过配置时长，修改告警集状态为关闭
                alarmCollectionServiceImpl.updateById(alarmCollection);
                insertAlarmCollection(roadId, deviceId, deviceName, eventType, checkFlag, tblId, alarmTime, milestone);//开启新告警集并插入数据
            } else {
                List<String> list = new ArrayList<>(Arrays.asList(alarmCollection.getRelatedIdList().split(",")));//获取当前告警集关联事件列表
                if(!list.contains(tblId)){
                    list.add(String.valueOf(tblId));
                    if(alarmCollection.getCollectionType() == 1){
                        alarmCollection.setEventType(typeDefineDeal(list));
                    }
                    alarmCollection.setDisposalAdvice(getCollectionAdvice(alarmCollection));
                    alarmCollection.setRelatedIdList(String.join(",", list));
                    alarmCollection.setLatestAlarmTime(alarmTime);
                    alarmCollection.setRelatedAlarmNum(list.size());
                    alarmCollection.setModifyTime(LocalDateTime.now());
                    this.saveOrUpdate(alarmCollection);
                }
            }
        } else {
            insertAlarmCollection(roadId, deviceId, deviceName, eventType, checkFlag, tblId, alarmTime, milestone);
        }
    }

    /**
     * @desc 新增告警集
     * @param roadId
     * @param deviceId
     * @param deviceName
     * @param eventType
     * @param checkFlag
     * @param tblId
     * @param alarmTime
     */
    public void insertAlarmCollection(String roadId, String deviceId, String deviceName, String eventType, int checkFlag, Long tblId, LocalDateTime alarmTime, int milestone) {
        AlarmCollection alarmCollection = new AlarmCollection();
        alarmCollection.setCollectionId(UUID.randomUUID().toString());
        alarmCollection.setRoadId(roadId);
        alarmCollection.setDeviceId(deviceId);
        alarmCollection.setDeviceName(deviceName);
        alarmCollection.setMilestone(milestone);
        if(checkFlag == 1){
            alarmCollection.setEventType(getTypeReflect(eventType));
        }
        alarmCollection.setCollectionType(checkFlag);
        List<String> list = new ArrayList<>();
        list.add(String.valueOf(tblId));
        alarmCollection.setRelatedIdList(String.join(",", list));
        alarmCollection.setDisposalAdvice(getCollectionAdvice(alarmCollection));
        alarmCollection.setEarliestAlarmTime(alarmTime);
        alarmCollection.setLatestAlarmTime(alarmTime);
        alarmCollection.setRelatedAlarmNum(list.size());
        alarmCollection.setCollectionStatus(1);
        alarmCollection.setCreateTime(LocalDateTime.now());
        alarmCollection.setModifyTime(LocalDateTime.now());
        this.saveOrUpdate(alarmCollection);
    }

    /**
     * @desc 根据路径描述获取高速路段信息
     * @param content
     * @return
     */
    public String getDeviceName(String content) {
        if (content == null) return "";
        String[] parts = content.split(" ");
        return parts.length >= 3 ? parts[0] + " " + parts[1] + " " + parts[2] : content;
    }

    /**
     * @desc 根据原始告警类型转换类型描述
     * @param eventType
     * @return
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
     * @desc 根据检查结果确定告警集间隔时长配置
     * @param checkFlag
     * @return
     */
    public int getIntervalDuration(int checkFlag) {
        if(checkFlag == 0){
            return uncertainIntervarlMinute;
        }else if(checkFlag == 1){
            return rightIntervarlMinute;
        }else if(checkFlag == 2){
            return falseIntervarlMinute;
        }else{
            return uncertainIntervarlMinute;
        }
    }

    /**
     * @desc 根据告警集关联告警记录id列表推定告警集事件类型
     * @param ids
     * @return
     */
    public String typeDefineDeal(List<String> ids) {
        if (ids.isEmpty()) return null;

        List<OriginalAlarmRecord> originalList = originalAlarmServiceImpl.getListByTblIdList(ids);
        if (originalList.isEmpty()) {
            log.error("原始告警记录丢失，告警集id列表:{}", ids);
            return null;
        }

        List<String> stopDriveList = new ArrayList<>();//停驶列表
        List<String> pswDriveList = new ArrayList<>();//抛洒物列表
        List<String> personList = new ArrayList<>();//行人列表

        List<String> vehicleNameList = new ArrayList<>();//车辆子类型列表
        List<String> personNameList = new ArrayList<>();//行人子类型列表
        List<String> pswNameList = new ArrayList<>();//行人子类型列表

        List<Long> stopDriveTimes = new ArrayList<>();//停驶时间列表

        for (OriginalAlarmRecord record : originalList) {
            CheckAlarmProcess process = checkAlarmProcessServiceImpl.getIouTop1ByKey(record.getId(), record.getImagePath(), record.getVideoPath());//每个事件返回一个检测结果
            String checkName = process.getName();

            String eventType = record.getEventType();
            if ("停驶".equals(eventType)) {
                stopDriveList.add(eventType);
                stopDriveTimes.add(record.getAlarmTime().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond());
                vehicleNameList.add(checkName);
            } else if ("抛洒物".equals(eventType)) {
                pswDriveList.add(eventType);
                pswNameList.add(checkName);
            } else if ("行人".equals(eventType)) {
                personList.add(eventType);
                personNameList.add(checkName);
            }
        }

        // 定义优先级：交通事故 > 车辆故障 > 行人闯入 > 路面异常 > 车辆停驶 > 道路施工

        // --- 判断是否是交通事故 ---
        if (stopDriveList.size() >= 2) {
            stopDriveTimes.sort(Long::compare);
            for (int i = 0; i < stopDriveTimes.size() - 1; i++) {
                long diff = stopDriveTimes.get(i + 1) - stopDriveTimes.get(i);
                if (diff <= 2 * 60) { // 小于等于2分钟
                    long nonEngineerCount = vehicleNameList.stream().filter(v -> !v.contains("maintenance_construction_vehicle") && !v.contains("police_car") && !v.contains("anti_collision_vehicle") && !v.contains("fire_fighting_truck") && !v.contains("ambulance")).count();
                    if (nonEngineerCount >= 2) {
                        return "交通事故";
                    }
                }
            }
        }

        // --- 判断是否是车辆故障 ---
        if (!stopDriveList.isEmpty() && !personList.isEmpty()) {
            return "车辆故障"; // 情形一
        }
        if (!stopDriveList.isEmpty()) {
            long engineer = vehicleNameList.stream().filter(v -> v.contains("maintenance_construction_vehicle") || v.contains("police_car") || v.contains("anti_collision_vehicle") || v.contains("fire_fighting_truck") || v.contains("ambulance")).count();
            long nonEngineer = vehicleNameList.stream().filter(v -> !v.contains("maintenance_construction_vehicle")).count();
            if (engineer > 0 && nonEngineer > 0) {
                return "车辆故障"; // 情形二
            }
        }
        if (!stopDriveList.isEmpty() && !personList.isEmpty() && !pswDriveList.isEmpty()) {
            return "车辆故障"; // 情形三
        }

        // --- 判断是否是路面异常 ---
        if (originalList.stream().allMatch(r -> "抛洒物".equals(r.getEventType()))) {
            return "路面异常";
        }

        // --- 判断是否是行人闯入 ---
        if (originalList.stream().allMatch(r -> "行人".equals(r.getEventType())) &&
                personNameList.stream().allMatch(p -> p.contains("person"))) {
            return "行人闯入";
        }

        // --- 判断是否是车辆停驶 ---
        if (originalList.stream().allMatch(r -> "停驶".equals(r.getEventType()))) {
            if (vehicleNameList.stream().allMatch(v -> v.contains("sedan") || v.contains("truck"))) {
                return "车辆停驶";
            }
        }

        // --- 判断是否是道路施工 ---
        if (!stopDriveList.isEmpty() && vehicleNameList.stream().allMatch(v -> v.contains("maintenance_construction_vehicle") || v.contains("police_car") || v.contains("anti_collision_vehicle") || v.contains("fire_fighting_truck") || v.contains("ambulance"))) {
            return "道路施工";
        }
        if (!personList.isEmpty() && personNameList.stream().allMatch(p -> p.contains("builder"))) {
            return "道路施工";
        }
        return null;
    }

    private LocalDateTime getMinTime(List<OriginalAlarmRecord> list, String type) {
        return list.stream()
                .filter(r -> type.equals(r.getEventType()) && r.getAlarmTime() != null)
                .map(OriginalAlarmRecord::getAlarmTime)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * @desc 获取告警集处置建议
     * @param alarmCollection
     * @return
     */
    public int getCollectionAdvice(AlarmCollection alarmCollection) {
        List<String> ids = new ArrayList<>(Arrays.asList(alarmCollection.getRelatedIdList().split(",")));
        List<OriginalAlarmRecord> originals = originalAlarmServiceImpl.getListByTblIdList(ids);//获取告警集关联的记录
        List<OriginalAlarmRecord> confirmed = originalAlarmServiceImpl.getEventByIdList(ids);//获取被确定为事件的记录
        List<OriginalAlarmRecord> unConfirmed = originalAlarmServiceImpl.getNoEventByIdList(ids);//获取未被确定为事件的记录

        List<Integer> errorList = new ArrayList<>();
        List<Integer> unknownList = new ArrayList<>();
        List<Integer> ignoreList = new ArrayList<>();
        List<Integer> confirmList = new ArrayList<>();

        /**
         * 处置建议：0-无法判断、1-疑似误报、2-尽快确认、3-无需处理',
         * 告警集是否有大于等于1条被确认为事件的告警记录：
         *  --是
         *      全为疑似误报，则无需处理
         *      全为无法判断，则尽快确认
         *      全为无需处理，则无需处理
         *      全为尽快确认，则尽快确认
         *      只有1条无法判断，则尽快确认
         *      只有1条尽快确认，则尽快确认
         *      只有1条疑似误报，则无需处理
         *  --否
         *      全为疑似误报，则疑似误报
         *      全为无法判断，则尽快确认
         *      全为无需处理，则无需处理
         *      全为尽快确认，则尽快确认
         *      只有1条无法判断，则尽快确认
         *      只有1条尽快确认，则尽快确认
         *      只有1条疑似误报，则无需处理
         */

        if(confirmed.size() == 0){
            //遍历所有告警记录的处置建议
            for(OriginalAlarmRecord record : originals){
                int adviceFlag = featureElementServiceImpl.getFeatureByKey(record.getId(), record.getImagePath(), record.getVideoPath()).getDisposalAdvice();
                switch (adviceFlag) {
                    case 0: unknownList.add(adviceFlag); break; //无法判断
                    case 1: errorList.add(adviceFlag); break; //疑似误报
                    case 2: confirmList.add(adviceFlag); break; //尽快确认
                    case 3: ignoreList.add(adviceFlag); break; //无需处理
                    default: log.error("adviceFlag判定异常: alarmId->{}, imagePath->{}, videoPath->{}", record.getId(), record.getImagePath(), record.getVideoPath());
                }
            }
            if(errorList.size() == unConfirmed.size()) return 1; //全为疑似误报
            if(unknownList.size() == unConfirmed.size()) return 2; //全为无法判断
            if(ignoreList.size() == unConfirmed.size()) return 3; //全为无需处理
            if(confirmList.size() == unConfirmed.size()) return 2; //全为尽快确认
            if(unknownList.size() == 1 || confirmList.size() == 1) return 2;
            if(errorList.size() == 1 || ignoreList.size() == unConfirmed.size() - 1) return 3;
        }else{
            //获取最新一条被确认为事件的告警记录
            OriginalAlarmRecord originalAlarmRecord = originalAlarmServiceImpl.getLatestConfirm(ids);//最新一条被确认为事件的记录
            if(alarmCollection.getLatestAlarmTime() == originalAlarmRecord.getAlarmTime()){
                return 3;
            }else{
                List<OriginalAlarmRecord> waitDealList = originalAlarmServiceImpl.getUnConfirmListByTime(ids,originalAlarmRecord.getAlarmTime());//获取最新一条被确认为事件告警之后且未被确认为事件的告警记录
                //判断告警记录里面是否有
                for(OriginalAlarmRecord record : waitDealList){
                    String alarmId = record.getId();
                    int adviceFlag = featureElementServiceImpl.getFeatureByKey(alarmId, record.getImagePath(), record.getVideoPath()).getDisposalAdvice();
                    switch (adviceFlag) {
                        case 0: unknownList.add(adviceFlag); break;
                        case 1: errorList.add(adviceFlag); break;
                        case 2: confirmList.add(adviceFlag); break;
                        case 3: ignoreList.add(adviceFlag); break;
                        default: log.error("adviceFlag 异常: {}", alarmId);
                    }
                }
                if(errorList.size() == unConfirmed.size()) return 3;
                if(unknownList.size() == unConfirmed.size()) return 2;
                if(ignoreList.size() == unConfirmed.size()) return 3;
                if(confirmList.size() == unConfirmed.size()) return 2;
                if(unknownList.size() == 1 || confirmList.size() == 1) return 2;
                if(errorList.size() == 1 || ignoreList.size() == unConfirmed.size() - 1) return 3;
            }
        }
        return 0;
    }
}