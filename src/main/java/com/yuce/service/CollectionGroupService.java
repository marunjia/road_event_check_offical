package com.yuce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuce.entity.CollectionGroupRecord;

import java.util.List;
import java.util.Map;

public interface CollectionGroupService extends IService<CollectionGroupRecord> {

    /**
     * @desc 根据告警集id统计告警组分布情况
     * @param collectionId
     * @return
     */
    List<Map<String, Object>> getIndexByCollectionId(String collectionId);
}