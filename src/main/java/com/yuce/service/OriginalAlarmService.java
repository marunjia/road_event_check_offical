package com.yuce.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.entity.QueryResultCheckRecord;

import java.time.LocalDateTime;

/**
 * @ClassName OriginalEventAlarmRecordService
 * @Description 原始告警事件业务操作接口
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:01
 * @Version 1.0
 */
public interface OriginalAlarmService extends IService<OriginalAlarmRecord> {

    //新增或更新告警记录
    boolean saveOrUpdateRecord(OriginalAlarmRecord record);

    //分页查询告警记录
    IPage<QueryResultCheckRecord> selectWithOriginaleField(String alarmId, String startDate, String endDate, String deviceName ,String roadId, String direction, String eventType, Integer dealFlag, Integer checkFlag, Integer disposalAdvice, String adviceReason, String deviceId, int pageNo, int pageSize);
}