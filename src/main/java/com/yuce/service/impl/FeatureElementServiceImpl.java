package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.FeatureElementRecord;
import com.yuce.mapper.FeatureElementMapper;
import com.yuce.service.FeatureElementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private FeatureElementMapper featureElementMapper;

    /**
     * @desc 根据联合唯一主键查询特征要素
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

    /**
     * @desc 根据特征要素id更新告警集关联状态
     * @param id
     * @param collectionMatchStatus
     * @return
     */
    public int updateCollectionMatchStatus(Integer id, Integer collectionMatchStatus){
        return featureElementMapper.updateCollectionMatchStatus(id, collectionMatchStatus);
    }

    /**
     * @desc 根据特征要素id更新告警集人工核查标签
     * @param id
     * @param personCheckFlag
     * @return
     */
    public int updatePersonCheckFlag(Integer id, Integer personCheckFlag){
        return featureElementMapper.updatePersonCheckFlag(id, personCheckFlag);
    }

    /**
     * @desc 根据特征要素id更新告警记录是否正确匹配标签
     * @param id
     * @param matchCheckFlag
     * @return
     */
    public int updateMatchCheckFlag(Integer id, Integer matchCheckFlag){
        return featureElementMapper.updateMatchCheckFlag(id, matchCheckFlag);
    }

    /**
     * @desc 根据特征要素id更新告警记录匹配错误原因
     * @param id
     * @param matchCheckReason
     * @return
     */
    public int updateMatchCheckReason(Integer id, String matchCheckReason){
        return featureElementMapper.updateMatchCheckReason(id, matchCheckReason);
    }

    /**
     * @desc 根据告警记录时间区间查询特征要素列表
     * @param previousMinute
     * @param currentMinute
     * @return
     */
    public List<FeatureElementRecord> getListByTimeRange(LocalDateTime previousMinute, LocalDateTime currentMinute){
        return featureElementMapper.getListByTimeRange(previousMinute, currentMinute);
    }
}