package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.OriginalAlarmMapper;
import com.yuce.service.OriginalAlarmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName OriginEventAlarmRecordServiceImpl
 * @Description 原始告警事件业务操作实现类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:01
 * @Version 1.0
 */
@Service
@Slf4j
public class OriginalAlarmServiceImpl extends ServiceImpl<OriginalAlarmMapper, OriginalAlarmRecord> implements OriginalAlarmService {

    @Autowired
    private OriginalAlarmMapper originalAlarmMapper;

    @Override
    public void saveIfNotExists(OriginalAlarmRecord record) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", record.getId());

        OriginalAlarmRecord existing = this.getOne(queryWrapper);
        log.info(existing.toString());
        if (existing == null) {
            this.save(record);
            log.info("事件ID {} 保存成功", record.getEventId());
        } else if (!existing.getDealFlag().equals(record.getDealFlag())) {
            existing.setDealFlag(record.getDealFlag());
            this.updateById(existing);
            log.info("事件ID {} 状态不一致，已更新为新状态 {}", record.getEventId(), record.getDealFlag());
        } else {
            log.info("事件ID {} 已存在且状态一致，跳过保存", record.getEventId());
        }
    }

    /**
     * @desc 根据关联告警记录id查询原始记录
     * @param relatedList
     * @return
     */
    public List<OriginalAlarmRecord> getListByIdList(List<String> relatedList) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", relatedList);
        queryWrapper.orderByDesc("alarm_time");
        return originalAlarmMapper.selectList(queryWrapper);
    }

    /**
     * @desc 查询标记为事件的记录
     * @param relatedList
     * @param dealFlag
     * @return
     */
    public List<OriginalAlarmRecord> getListByDealFlag(List<String> relatedList, int dealFlag) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", relatedList);
        queryWrapper.eq("deal_flag", "1");
        queryWrapper.orderByDesc("alarm_time");
        return originalAlarmMapper.selectList(queryWrapper);
    }

    /**
     * @desc 查询未被标记为事件的记录
     * @param relatedList
     * @return
     */
    public List<OriginalAlarmRecord> getUnConfirmList(List<String> relatedList) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", relatedList);
        queryWrapper.ne("deal_flag", "1");
        queryWrapper.orderByDesc("alarm_time");
        return originalAlarmMapper.selectList(queryWrapper);
    }

    /**
     * @desc 根据时间范围查询未被标记为事件的记录
     * @param relatedList
     * @return
     */
    public List<OriginalAlarmRecord> getUnConfirmList(List<String> relatedList, LocalDateTime alarmTime) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", relatedList);
        queryWrapper.ne("deal_flag", "1");
        queryWrapper.gt("alarm_time", alarmTime);
        queryWrapper.orderByDesc("alarm_time");
        return originalAlarmMapper.selectList(queryWrapper);
    }

    /**
     * @desc 查询指定列表中最新一条被确认为事件的记录
     * @param relatedList
     * @return
     */
    public OriginalAlarmRecord getLatestUnconfirm(List<String> relatedList) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", relatedList);
        queryWrapper.eq("deal_flag", "1");
        queryWrapper.orderByDesc("alarm_time");
        queryWrapper.last("limit 1");
        return originalAlarmMapper.selectOne(queryWrapper);
    }

    /**
     * @desc 根据告警时间范围查询告警记录
     * @param relatedList
     * @param alarmTime
     * @return
     */
    public List<OriginalAlarmRecord> getListByAlarmTimeRange(List<String> relatedList, LocalDateTime alarmTime) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", relatedList);
        queryWrapper.gt("alarm_time", alarmTime);
        queryWrapper.orderByDesc("alarm_time");
        return originalAlarmMapper.selectList(queryWrapper);
    }

    /**
     * @desc 根据告警记录id查询告警记录是否存在
     * @param alarmId
     * @return
     */
    public boolean getRecordByAlarmIdAndTag(String alarmId) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", alarmId);
        queryWrapper.eq("consume_tag", 1);
        return originalAlarmMapper.exists(queryWrapper);
    }
}