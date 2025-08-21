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
import java.util.Map;

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
     * @desc 根据联合主键查询算法处理的图片数量
     * @param alarmId
     * @return
     */
    public int countDistinctImageId(String alarmId, String imagePath, String videoPath) {
        return checkAlarmProcessMapper.countDistinctImageId(alarmId, imagePath, videoPath);
    }

    /**
     * @desc 根据告警id、图片路径、视频路径、图片id查询非top1的物体分类情况
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public List<Map<String,Integer>> getElementGroupByKey(String alarmId, String imagePath, String videoPath, String imageId, int id) {
        return checkAlarmProcessMapper.getElementGroupByKey(alarmId, imagePath, videoPath, imageId, id);
    }

    /**
     * @desc 根据告警id、图片路径、视频路径查询物体检测结果中iou排名top1的记录
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public CheckAlarmProcess getIouTop1ByKey(String alarmId, String imagePath, String videoPath) {
        QueryWrapper<CheckAlarmProcess> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);
        queryWrapper.orderByDesc("iou");
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }
}