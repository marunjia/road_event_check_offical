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
 * @ClassName AlarmCollectionService
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/15 16:53
 * @Version 1.0
 */

@Service
@Slf4j
public class AlarmCollectionServiceImpl extends ServiceImpl<AlarmCollectionMapper, AlarmCollection> implements AlarmCollectionService {

    @Autowired
    private AlarmCollectionMapper alarmCollectionMapper;

    /**
     * @desc 根拒设备id查看告警集是否存在
     * @param deviceId
     * @param collectionType
     * @return
     */
    public AlarmCollection existsCollection(String deviceId, int collectionType) {
        return alarmCollectionMapper.selectLastByDeviceIdAndType(deviceId, collectionType);
    }

    /**
     * @desc 根据告警id查询归属告警集
     * @param alarmId
     * @return
     */
    public AlarmCollection getCollectionByAlarmId(String alarmId) {
        QueryWrapper<AlarmCollection> queryWrapper = new QueryWrapper<>();
        queryWrapper.apply("FIND_IN_SET({0}, related_id_list)", alarmId);
        return alarmCollectionMapper.selectOne(queryWrapper);
    }
}