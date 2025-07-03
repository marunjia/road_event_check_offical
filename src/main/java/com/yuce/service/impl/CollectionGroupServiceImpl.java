package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.*;
import com.yuce.mapper.CollectionGroupMapper;
import com.yuce.service.CollectionGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
     * @desc 新增告警组记录
     * @param collectionGroupRecord
     * @return
     */
    public boolean insertGroup(CollectionGroupRecord collectionGroupRecord) {
        return this.save(collectionGroupRecord);
    }

    /**
     * @desc 根据collectionId查询告警组
     * @param collectionId
     * @return
     */
    public List<CollectionGroupRecord> queryByCollectionId(String collectionId) {
        QueryWrapper<CollectionGroupRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collection_id", collectionId);
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }

    /**
     * @desc 根据groupId查询告警组
     * @param groupId
     * @return
     */
    public List<CollectionGroupRecord> queryByGroupId(String groupId) {
        QueryWrapper<CollectionGroupRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId);
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }


    /**
     * @desc 根据collectionId和groupId查询告警组
     * @param groupId
     * @return
     */
    public List<CollectionGroupRecord> queryByCollectionIdAndGroupId(String collectionId,String groupId) {
        QueryWrapper<CollectionGroupRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collection_id", collectionId);
        queryWrapper.eq("group_id", groupId);
        queryWrapper.orderByDesc("create_time");
        return this.list(queryWrapper);
    }












}
