package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.OriginalAlarmRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * @ClassName OriginalEventAlarmRecordMapper
 * @Description 原始事件告警记录表数据库操作mapper
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:01
 * @Version 1.0
 */

@Mapper
public interface OriginalAlarmMapper extends BaseMapper<OriginalAlarmRecord> {
    // 可以根据需要定义自定义的查询方法
}