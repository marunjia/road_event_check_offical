package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.RoadCheckRecord;
import com.yuce.mapper.RoadCheckRecordMapper;
import com.yuce.service.RoadCheckRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName RoadCheckRecordServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/14 10:47
 * @Version 1.0
 */

@Service
@Slf4j
public class RoadCheckRecordServiceImpl extends ServiceImpl<RoadCheckRecordMapper, RoadCheckRecord> implements RoadCheckRecordService {

    @Autowired
    private RoadCheckRecordMapper roadCheckRecordMapper;

    /**
     * @desc 批量新增路面检测结果
     * @param list
     */
    public void insertRoadCheckRecord(List<RoadCheckRecord> list) {
        this.saveBatch(list);
    }

    /**
     * @desc 根据key、检测类型、检测结果查询路面检测匹配结果
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @param type
     * @param checkFlag
     * @return
     */
    public List<RoadCheckRecord> getRecordByKeyAndTypeAndFlag(String alarmId, String imagePath, String videoPath, String type, Integer checkFlag){
        return roadCheckRecordMapper.getRecordByKeyAndTypeAndFlag(alarmId,imagePath,videoPath,type, checkFlag);
    }

    /**
     * @desc 根据key和检测类型查询路面检测结果
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @param type
     * @return
     */
    public List<RoadCheckRecord> getRecordByKeyAndType(String alarmId, String imagePath, String videoPath, String type){
        QueryWrapper<RoadCheckRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);
        queryWrapper.eq("type", type);
        return roadCheckRecordMapper.selectList(queryWrapper);
    }



}