package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.AlarmCollection;
import com.yuce.mapper.AlarmCollectionMapper;
import com.yuce.service.AlarmCollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private AlarmCollectionMapper alarmCollectionMapper;

    /**
     * @desc 根拒设备id查看其对应最新告警集
     * @param deviceId
     * @return
     */
    public AlarmCollection getLatestByDeviceId(String deviceId) {
        QueryWrapper<AlarmCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("device_id", deviceId);
        queryWrapper.orderByDesc("latest_alarm_time");
        queryWrapper.last("limit 1");
        return this.alarmCollectionMapper.selectOne(queryWrapper, false);
    }

    /**
     * @desc 根据tblId查询归属告警集
     * @param tblId
     * @return
     */
    public AlarmCollection getCollectionByTblId(long tblId) {
        QueryWrapper<AlarmCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("FIND_IN_SET({0}, related_tbl_id_list)", tblId);
        return this.getOne(queryWrapper);
    }

    /**
     * @desc 根据alarmId查询归属告警集
     * @param alarmId
     * @return
     */
    public AlarmCollection getCollectionByAlarmId(String alarmId) {
        QueryWrapper<AlarmCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("FIND_IN_SET({0}, related_alarm_id_list)", alarmId);
        return this.getOne(queryWrapper);
    }

    /**
     * @desc 根拒告警集id查询告警集详情
     * @param collectionId
     * @return
     */
    public AlarmCollection getCollectionByCollectionId(String collectionId) {
        QueryWrapper<AlarmCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("collection_id", collectionId);
        return this.getOne(queryWrapper,false);
    }

    /**
     * @desc 根拒告警集id更新人工检验标签
     * @param collectionId
     * @return
     */
    public int updatePersonCheckFlag(String collectionId, Integer personCheckFlag) {
       return alarmCollectionMapper.updatePersonCheckFlag(collectionId,personCheckFlag);
    }

    /**
     * @desc 根拒告警集id更新人工检验标签
     * @param collectionId
     * @return
     */
    public int updatePersonCheckReason(String collectionId, String personCheckReason) {
        return alarmCollectionMapper.updatePersonCheckReason(collectionId,personCheckReason);
    }


}