package com.yuce.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuce.entity.AlarmTimeRange;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.entity.OriginalAlarmRecordBak;
import com.yuce.entity.QueryResultCheckRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName OriginalEventAlarmRecordMapper
 * @Description 原始事件告警记录表数据库操作mapper
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:01
 * @Version 1.0
 */

@Mapper
public interface OriginalAlarmBakMapper extends BaseMapper<OriginalAlarmRecordBak> {

}