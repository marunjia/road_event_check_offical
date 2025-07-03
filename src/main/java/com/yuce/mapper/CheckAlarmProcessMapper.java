package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.CheckAlarmProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CheckAlarmProcessMapper extends BaseMapper<CheckAlarmProcess> {

    @Select("SELECT COUNT(DISTINCT image_id) FROM algorithm_check_process WHERE alarm_id = #{alarmId}")
    int countDistinctImageId(@Param("alarmId") String alarmId);
}