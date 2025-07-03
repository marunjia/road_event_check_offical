package com.yuce.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.QueryResultCheckRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * @ClassName CheckAlarmRecordMapper
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/22 16:12
 * @Version 1.0
 */

@Mapper
public interface CheckAlarmResultMapper extends BaseMapper<CheckAlarmResult> {

    @Select("SELECT o.*, " +
            "a.check_flag," +
            "f.disposal_advice as advice_flag, " +
            "r.short_name " +
            "FROM kafka_original_alarm_record o " +
            "LEFT JOIN algorithm_check_result a ON o.id = a.alarm_id " +
            "LEFT JOIN feature_element_record f ON o.id = f.alarm_id " +
            "LEFT JOIN road_info r ON o.road_id = r.short_name " +
            "${ew.customSqlSegment}")
    IPage<QueryResultCheckRecord> selectWithJoin(Page<?> page, @Param(Constants.WRAPPER) QueryWrapper<QueryResultCheckRecord> wrapper);
}