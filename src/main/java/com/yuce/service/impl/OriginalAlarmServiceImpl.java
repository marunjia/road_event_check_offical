package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.entity.QueryResultCheckRecord;
import com.yuce.mapper.OriginalAlarmMapper;
import com.yuce.service.OriginalAlarmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @ClassName OriginalAlarmServiceImpl
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

    /**
     * @param record
     * @return
     * @desc 根据告警id、图片路径、视频路径组成的联合主键判断记录是否存在
     */
    public boolean existsByKey(OriginalAlarmRecord record) {
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);
        return this.count(queryWrapper) > 0;
    }

    /**
     * @param tblId
     * @return
     * @desc 根据告警id、图片路径、视频路径组成的联合主键判断记录是否存在
     */
    public OriginalAlarmRecord getRecordByTblId(long tblId) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tbl_id", tblId);
        return this.getOne(queryWrapper);
    }

    /**
     * @desc 根据告警id、图片路径、视频路径组成的联合主键查询记录
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public OriginalAlarmRecord getRecordByKey(String alarmId, String imagePath, String videoPath) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);
        return this.getOne(queryWrapper);
    }

    /**
     * @param record
     * @return
     * @desc 插入或更新记录
     */
    public void saveIfNotExists(OriginalAlarmRecord record) {

        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);

        OriginalAlarmRecord originalAlarmRecord = this.getOne(queryWrapper);
        if (originalAlarmRecord != null) {
            record.setTblId(originalAlarmRecord.getTblId());
            this.updateByKey(record);
            log.info("记录更新成功：alarmId->{}, imagePath->{}, videoPath->{}", alarmId, imagePath, videoPath);
        } else {
            this.save(record);
            log.info("记录保存成功：alarmId->{}, imagePath->{}, videoPath->{}", alarmId, imagePath, videoPath);
        }
    }

    /**
     * @param relatedAlarmIdList
     * @return
     * @desc 根据alarmIdList查询所有告警记录明细
     */
    public List<OriginalAlarmRecord> getListByAlarmIdList(List<String> relatedAlarmIdList) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("alarm_id", relatedAlarmIdList);
        queryWrapper.orderByDesc("alarm_time");
        return this.list(queryWrapper);
    }

    /**
     * @param relatedTblIdList
     * @return
     * @desc 根据关联tblIdList查询所有告警记录明细
     */
    public List<OriginalAlarmRecord> getListByTblIdList(List<String> relatedTblIdList) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("tbl_id", relatedTblIdList);
        queryWrapper.orderByDesc("alarm_time");
        return this.list(queryWrapper);
    }

    /**
     * @param relatedTblIdList
     * @return
     * @desc 在指定告警集列表中查询小于alarmTime的第一条记录
     */
    public OriginalAlarmRecord getRecordByTblIdListAndTime(List<String> relatedTblIdList, LocalDateTime alarmTime) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("tbl_id", relatedTblIdList);
        queryWrapper.lt("alarm_time", alarmTime);
        queryWrapper.orderByDesc("alarm_time");
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }


    /**
     * @param relatedIdList
     * @return
     * @desc 查询告警集关联告警记录中被打标为事件的记录
     */
    public List<OriginalAlarmRecord> getEventByIdList(List<String> relatedIdList) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deal_flag", 1);//1被确定为事件
        queryWrapper.in("alarm_id", relatedIdList);
        queryWrapper.orderByDesc("alarm_time");
        return this.list(queryWrapper);
    }

    /**
     * @param relatedIdList
     * @return
     * @desc 查询告警集关联告警记录中未被打标为事件的记录
     */
    public List<OriginalAlarmRecord> getNoEventByIdList(List<String> relatedIdList) {
        QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("deal_flag", 1);//1被确定为事件
        queryWrapper.in("alarm_id", relatedIdList);
        queryWrapper.orderByDesc("alarm_time");
        return this.list(queryWrapper);
    }

    /**
     * @param relatedIdList
     * @return
     * @desc 根据告警集关联记录查询集合中最新一条被确认为事件的记录
     */
    public OriginalAlarmRecord getLatestConfirm(List<String> relatedIdList) {
        QueryWrapper<OriginalAlarmRecord> wrapper = new QueryWrapper<>();
        wrapper.in("alarm_id", relatedIdList)
                .eq("deal_flag", 1)
                .orderByDesc("alarm_time")
                .last("limit 1");
        return this.getOne(wrapper);
    }

    /**
     * @param relatedIdList
     * @return
     * @desc 根据告警集关联记录查询集合中最新一条被确认为事件的记录之后的告警
     */
    public List<OriginalAlarmRecord> getUnConfirmListByTime(List<String> relatedIdList, LocalDateTime alarmTime) {
        QueryWrapper<OriginalAlarmRecord> wrapper = new QueryWrapper<>();
        wrapper.in("alarm_id", relatedIdList)
                .eq("deal_flag", 1)
                .ge("alarm_time", alarmTime)
                .orderByDesc("alarm_time");
        return this.list(wrapper);
    }


    /**
     * @param record
     * @desc 插入新纪录
     */
    public void insert(OriginalAlarmRecord record) {
        record.setDbCreateTime(LocalDateTime.now());
        record.setDbUpdateTime(LocalDateTime.now());
        this.save(record);
    }

    /**
     * @param record
     * @desc 根据联合唯一主键更新记录
     */
    public void updateByKey(OriginalAlarmRecord record) {
        QueryWrapper<OriginalAlarmRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("alarm_id", record.getId())
                .eq("image_path", record.getImagePath())
                .eq("video_path", record.getVideoPath());
        this.update(record, wrapper);
    }

    /**
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     * @desc 根据key值判断记录是否存在
     */
    public boolean existsByKey(String alarmId, String imagePath, String videoPath) {
        QueryWrapper<OriginalAlarmRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("alarm_id", alarmId)
                .eq("image_path", imagePath)
                .eq("video_path", videoPath);
        return this.count(wrapper) > 0;
    }

    /**
     * @param deviceId
     * @param eventType
     * @return
     * @desc 根据设备id、事件类型查询原始告警记录
     */
    public OriginalAlarmRecord getLastByDeviceAndType(String deviceId, String eventType) {
        QueryWrapper<OriginalAlarmRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("device_id", deviceId);
        wrapper.eq("event_type", eventType);
        wrapper.orderByDesc("alarm_time");
        wrapper.last("limit 1");
        return this.getOne(wrapper);
    }

    /**
     * @param deviceId
     * @param eventType
     * @return
     * @desc 根据多维条件查询告警记录处置情况
     */
    public OriginalAlarmRecord getAllByDimession(String deviceId, String eventType) {
        QueryWrapper<OriginalAlarmRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("device_id", deviceId);
        wrapper.eq("event_type", eventType);
        wrapper.orderByDesc("alarm_time");
        wrapper.last("limit 1");
        return this.getOne(wrapper);
    }

    @Override
    public IPage<QueryResultCheckRecord> selectWithOriginaleField(String alarmId, String startDate, String endDate, String deviceName, String roadId, String directionDes, String eventType, Integer dealFlag, Integer checkFlag, Integer disposalAdvice, String adviceReason, int pageNo, int pageSize) {
        Page<QueryResultCheckRecord> page = new Page<>(pageNo, pageSize);

        QueryWrapper<QueryResultCheckRecord> query = new QueryWrapper<>();

        if (alarmId != null) {
            query.eq("o.alarm_id", alarmId);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.hasText(startDate)) {
            LocalDate date = LocalDate.parse(startDate, formatter);
            query.ge("o.alarm_time", date.atStartOfDay());
        }

        if (StringUtils.hasText(endDate)) {
            LocalDate date = LocalDate.parse(endDate, formatter);
            query.le("o.alarm_time", date.atTime(23, 59, 59));
        }

        if (StringUtils.hasText(deviceName)) {
            query.like("o.content", deviceName);
        }

        if (roadId != null) {
            query.eq("o.road_id", roadId);
        }

        if (directionDes != null) {
            query.like("o.direction_des", directionDes);
        }

        if (StringUtils.hasText(eventType)) {
            query.eq("o.event_type", eventType);
        }

        if (dealFlag != null) {
            query.eq("o.deal_flag", dealFlag);
        }

        if (checkFlag != null) {
            query.eq("a.check_flag", checkFlag);
        }

        if (disposalAdvice != null) {
            query.eq("f.disposal_advice", disposalAdvice);
        }

        if (StringUtils.hasText(adviceReason)) {
            query.like("f.advice_reason", adviceReason);
        }

        query.orderByDesc("o.alarm_time");

        // 调用 Mapper 方法执行分页查询
        IPage<QueryResultCheckRecord> result = originalAlarmMapper.selectWithJoin(page, query);

        return result;
    }
}