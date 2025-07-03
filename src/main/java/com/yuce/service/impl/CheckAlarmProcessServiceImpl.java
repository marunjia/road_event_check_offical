package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.mapper.CheckAlarmProcessMapper;
import com.yuce.service.CheckAlarmProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * @ClassName AlgorithmCheckServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/22 16:07
 * @Version 1.0
 */

@Slf4j
@Service
public class CheckAlarmProcessServiceImpl extends ServiceImpl<CheckAlarmProcessMapper, CheckAlarmProcess> implements CheckAlarmProcessService {

    @Autowired
    private CheckAlarmProcessMapper checkAlarmProcessMapper;

    /**
     * @desc 根据告警id查询算法处理的图片数量
     * @param alarmId
     * @return
     */
    public int countDistinctImageId(String alarmId) {
        return checkAlarmProcessMapper.countDistinctImageId(alarmId);
    }

    /**
     * @desc 根据告警id查询算法处理返回明细
     * @param alarmId
     * @return
     */
    public List<CheckAlarmProcess> getListByAlarmId(String alarmId) {
        QueryWrapper<CheckAlarmProcess> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.orderByAsc("create_time");
        return checkAlarmProcessMapper.selectList(queryWrapper);
    }

    /**
     * @desc 根据告警id查询iou top1检验结果
     * @param alarmId
     * @return
     */
    public CheckAlarmProcess getIouTop1ByAlarmId(String alarmId) {
        QueryWrapper<CheckAlarmProcess> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.orderByDesc("iou");
        queryWrapper.last("limit 1");
        return checkAlarmProcessMapper.selectOne(queryWrapper);
    }

    /**
     * @desc 根据告警id、图片id查询单张图片结果iou top1检验结果
     * @param alarmId
     * @return
     */
    public CheckAlarmProcess getIouTop1ByAlarmIdAndImgId(String alarmId, String imageId) {
        QueryWrapper<CheckAlarmProcess> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_id", imageId);
        queryWrapper.orderByDesc("iou");
        queryWrapper.last("limit 1");
        return checkAlarmProcessMapper.selectOne(queryWrapper);
    }

    /**
     * @desc 根据告警id、检出类型查询算法检验结果
     * @param alarmId
     * @return
     */
    public List<CheckAlarmProcess> getListByAlarmIdAndType(String alarmId, String type) {
        QueryWrapper<CheckAlarmProcess> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("type", type);
        return checkAlarmProcessMapper.selectList(queryWrapper);
    }

    /**
     * @desc 根据告警id、检出名称查询算法检验结果
     * @param alarmId
     * @return
     */
    public List<CheckAlarmProcess> getListByAlarmIdAndName(String alarmId, List<String> nameList) {
        QueryWrapper<CheckAlarmProcess> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.in("name", nameList);
        return checkAlarmProcessMapper.selectList(queryWrapper);
    }
}