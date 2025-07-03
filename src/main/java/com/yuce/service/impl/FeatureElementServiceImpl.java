package com.yuce.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.FeatureElementRecord;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.FeatureElementMapper;
import com.yuce.service.FeatureElementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
public class FeatureElementServiceImpl extends ServiceImpl<FeatureElementMapper, FeatureElementRecord> implements FeatureElementService {

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private FeatureElementMapper featureElementMapper;

    /**
     * @desc 根据alarmI的更新特征要素信息
     * @param alarmId
     */
    public void featureElementDeal(String alarmId){

        OriginalAlarmRecord originalAlarmRecord = originalAlarmServiceImpl.getById(alarmId);//查询原始告警记录

        CheckAlarmProcess checkAlarmProcess = checkAlarmProcessServiceImpl.getIouTop1ByAlarmId(alarmId);//查询iou top1内容

        List<CheckAlarmProcess> list = checkAlarmProcessServiceImpl.getListByAlarmId(alarmId);//查询所有算法识别所有结果

        int checkFlag = checkAlarmResultServiceImpl.getByAlarmId(alarmId).getCheckFlag();//查询算法检测结果

        //封装特征要素对象
        FeatureElementRecord featureElementRecord = new FeatureElementRecord();
        featureElementRecord.setAlarmId(alarmId);//告警id
        featureElementRecord.setAbnormalLocation(originalAlarmRecord.getAlarmPlace());//告警地点
        String eventType = originalAlarmRecord.getEventType();
        featureElementRecord.setInvolvedPersonInfo(originalAlarmRecord.getWeather());//天气信息
        featureElementRecord.setAbnormalType(eventType);//事件类型
        featureElementRecord.setLaneOccupyInfo(null);//占道情况
        featureElementRecord.setCongestionStatus(null);//拥堵情况
        featureElementRecord.setDangerElement(null);//危险要素

        //判断检查结果
        if(checkAlarmProcess != null && list.size() > 0){
            Set<String> personSet = new HashSet<>();//人员类型集合
            Set<String> rescueSet = new HashSet<>();//救援力量(工程救援车辆、施工人员)
            List<String> personList = Arrays.asList("person","traffic_police","medical_person","builder");//人员类型清单
            List<String> rescueList = Arrays.asList("anti_collision_vehicle","maintenance_construction_vehicle","police_car","ambulance","fire_fighting_truck","traffic_police","medical_person","builder");//施救力量清单

            for(int i = 0; i < list.size(); i++){
                String name = list.get(i).getName();

                if(personList.contains(name)){
                    personSet.add(name);
                }
                if(rescueList.contains(name)){
                    rescueSet.add(name);
                }
            }

            //处理涉事车辆
            if(eventType.equals("停驶")){
                //涉事车辆
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("vehicleType", checkAlarmProcess.getName());
                jsonObject.put("vehicleNum", 1);
                featureElementRecord.setInvolvedVehicleInfo(jsonObject.toJSONString());

                //涉事人员
                featureElementRecord.setInvolvedPersonInfo(null);//涉事人员信息
            }else if(eventType.equals("行人")){
                //涉事车辆
                featureElementRecord.setInvolvedVehicleInfo(null);

                //涉事人员
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("personType", checkAlarmProcess.getName());
                jsonObject.put("personNum", 1);
                featureElementRecord.setInvolvedPersonInfo(null);
            }else if(eventType.equals("抛洒物")){
                //涉事车辆
                featureElementRecord.setInvolvedVehicleInfo(null);

                //涉事人员
                featureElementRecord.setInvolvedPersonInfo(String.join(",", personSet));
            }else{
                log.info("告警id:{},事件类型:{},尚未对接",alarmId, eventType);
            }
            featureElementRecord.setRescueForce(String.join(",",rescueSet));//救援力量
            featureElementRecord.setDisposalAdvice(getAdviceFlag(checkFlag,eventType,checkAlarmProcess.getName()));//建议类型
        }else{
            featureElementRecord.setInvolvedVehicleInfo(null);
            featureElementRecord.setInvolvedPersonInfo(null);
            featureElementRecord.setRescueForce(null);
            featureElementRecord.setDisposalAdvice(getAdviceFlag(checkFlag,eventType,null));//建议类型
        }
        featureElementRecord.setCreateTime(LocalDateTime.now());//创建时间
        featureElementRecord.setModifyTime(LocalDateTime.now());//修改时间
        featureElementMapper.insert(featureElementRecord);
    }

    /**
     * @desc 根据告警id查询
     * @param alarmId
     * @return
     */
    public FeatureElementRecord getByAlarmId(String alarmId){
        QueryWrapper<FeatureElementRecord> queryWrapper = new QueryWrapper();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.orderByDesc("create_time");
        return featureElementMapper.selectOne(queryWrapper);
    }

    /**
     * @desc 根据检查结果、事件类型、框检内容判断建议类型
     * @param checkFlag
     * @param eventType
     * @param name
     * @return
     */
    public int getAdviceFlag(int checkFlag,String eventType,String name){
        int disposalAdvice = 0;
        if (checkFlag == 0) {
            //0-无法判断、1-疑似误报、2-尽快确认、3-无需处理
            disposalAdvice = 0; //无法判断
        }else if(checkFlag == 1){
            if(eventType.equals("停驶")){
                if(Arrays.asList("anti_collision_vehicle","maintenance_construction_vehicle","police_car","sedan","ambulance","fire_fighting_truck").contains(name)){
                    disposalAdvice = 3; //无需处理
                }else{
                    disposalAdvice = 2; //尽快确认
                }
            }else if(eventType.equals("行人")){
                if(Arrays.asList("medical_person","builder","traffic_police").contains(name)){
                    disposalAdvice = 3; //无需处理
                }else{
                    disposalAdvice = 2;
                }
            }else if(eventType.equals("抛洒物")){
                if(Arrays.asList("paper","plastic bags","plastic","cardboard","warning triangle").contains(name)) {
                    disposalAdvice = 3; //无需处理
                } else {
                    disposalAdvice = 2; //尽快确认
                }
            }
        }else if(checkFlag ==2 ){
            disposalAdvice = 1; //疑似误报
        }else{
            disposalAdvice = 2;
        }
        return disposalAdvice;
    }
}