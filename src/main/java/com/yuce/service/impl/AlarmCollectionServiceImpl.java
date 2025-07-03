package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.AlarmCollection;
import com.yuce.mapper.AlarmCollectionMapper;
import com.yuce.service.AlarmCollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @ClassName AlarmCollectionServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/25 15:24
 * @Version 1.0
 */

@Service
@Slf4j
public class AlarmCollectionServiceImpl extends ServiceImpl<AlarmCollectionMapper, AlarmCollection> implements AlarmCollectionService {

    /**
     * @desc 根拒设备id、告警集类型查看告警集是否存在
     * @param deviceId
     * @param collectionType
     * @return
     */
    public boolean existsCollection(String deviceId, int collectionType) {
        QueryWrapper<AlarmCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("device_id", deviceId);
        queryWrapper.eq("collection_type", collectionType);
        return this.count(queryWrapper) > 0;
    }

    /**
     * @desc 根拒设备id查看告警集是否存在
     * @param deviceId
     * @param collectionType
     * @return
     */
    public AlarmCollection getCollectionByKey(String deviceId, int collectionType) {
        QueryWrapper<AlarmCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("device_id", deviceId);
        queryWrapper.eq("collection_type", collectionType);
        return this.getOne(queryWrapper,false);
    }

    /**
     * @desc 根据tblId查询归属告警集
     * @param tblId
     * @return
     */
    public AlarmCollection getCollectionByTblId(long tblId) {
        QueryWrapper<AlarmCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("FIND_IN_SET({0}, related_id_list)", tblId);
        return this.getOne(queryWrapper);
    }
}