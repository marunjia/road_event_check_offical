package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.*;
import com.yuce.mapper.CollectionGroupMapper;
import com.yuce.service.CollectionGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @ClassName CollectionGroupServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/29 13:47
 * @Version 1.0
 */

@Service
@Slf4j
public class CollectionGroupServiceImpl extends ServiceImpl<CollectionGroupMapper, CollectionGroupRecord> implements CollectionGroupService {

    @Autowired
    private CollectionGroupMapper collectionGroupMapper;

    /**
     * @desc 根据key值查询告警记录归属告警组
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public CollectionGroupRecord getGroupByKey(String alarmId, String imagePath, String videoPath) {
        QueryWrapper<CollectionGroupRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);
        return this.getOne(queryWrapper);
    }

    /**
     * @desc 根据collectionId和groupId查询告警组列表
     * @param groupId
     * @return
     */
    public List<Map<String, Object>> queryByCollectionIdAndGroupId(Integer collectionId,String groupId) {
        if(!StringUtils.hasText(groupId)){
            return collectionGroupMapper.queryByCollectionId(collectionId);
        }else{
            return collectionGroupMapper.queryByCollectionIdAndGroupId(collectionId, groupId);
        }
    }

    /**
     * @desc 根据collectionId和groupId查询告警组列表
     * @param groupId
     * @return
     */
    public CollectionGroupRecord queryTop1ByCollectionIdAndGroupId(Integer collectionId,String groupId) {
        QueryWrapper<CollectionGroupRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collection_id", collectionId);
        queryWrapper.eq("group_id", groupId);
        queryWrapper.orderByDesc("alarm_time");
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    /**
     * @desc 根据告警集id查询告警组分布情况
     * @param collectionId
     * @return
     */
    public List<Map<String, Object>> getIndexByCollectionId(Integer collectionId) {
        // 参数校验：避免无效查询
        List<Map<String, Object>> statMapList = collectionGroupMapper.getIndexByCollectionId(collectionId);
        return statMapList;
    }
}
