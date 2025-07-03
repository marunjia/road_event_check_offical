package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.FeatureElementRecord;
import com.yuce.mapper.FeatureElementMapper;
import com.yuce.service.FeatureElementService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName FeatureElementServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/31 11:25
 * @Version 1.0
 */

@Service
public class FeatureElementServiceImpl extends ServiceImpl<FeatureElementMapper, FeatureElementRecord> implements FeatureElementService {

    /**
     * @desc 判断告警记录特征要素是否已经处理
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public boolean existsRecord(String alarmId, String imagePath, String videoPath){
        QueryWrapper<FeatureElementRecord> queryWrapper = new QueryWrapper();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);
        return this.list(queryWrapper).size() > 0;
    }

    /**
     * @desc 判断告警记录特征要素是否已经处理
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public FeatureElementRecord getFeatureByKey(String alarmId, String imagePath, String videoPath){
        QueryWrapper<FeatureElementRecord> queryWrapper = new QueryWrapper();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    /**
     * @desc 查询所有特征要素
     * @return
     */
    public List<FeatureElementRecord> queryAll(){
        return this.list();
    }
}