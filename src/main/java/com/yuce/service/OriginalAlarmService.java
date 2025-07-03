package com.yuce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuce.entity.OriginalAlarmRecord;

/**
 * @ClassName OriginalEventAlarmRecordService
 * @Description 原始告警事件业务操作接口
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:01
 * @Version 1.0
 */
public interface OriginalAlarmService extends IService<OriginalAlarmRecord> {
    // 可以定义一些业务逻辑的方法，例如自定义的保存方法
    void saveIfNotExists(OriginalAlarmRecord record);
}