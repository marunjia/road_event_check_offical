package com.yuce.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuce.entity.AlarmTimeRange;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.OriginalAlarmRecord;
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
public interface OriginalAlarmMapper extends BaseMapper<OriginalAlarmRecord> {

    /**
     * @desc 根据设备id、事件类型查询告警最新一条告警记录
     * @param deviceId
     * @param eventType
     * @return
     */
    @Select("SELECT * FROM kafka_original_alarm_record " +
            "WHERE device_id = #{deviceId} " +
            "AND event_type = #{eventType} " +
            "AND alarm_time < #{alarmTime} " +
            "ORDER BY alarm_time DESC LIMIT 1")
    OriginalAlarmRecord getLastByDeviceAndType(@Param("deviceId") String deviceId,
                                               @Param("eventType") String eventType,
                                               @Param("alarmTime") LocalDateTime alarmTime);

    @Select("SELECT \n" +
            "    o.*, \n" +
            "    a.check_flag, \n" +
            "    f.id AS feature_id, \n" +
            "    f.disposal_advice AS advice_flag, \n" +
            "    f.person_check_flag AS person_check_flag, \n" +
            "    f.advice_reason AS advice_reason, \n" +
            "    r.short_name \n" +
            "FROM kafka_original_alarm_record o\n" +
            "LEFT JOIN algorithm_check_result a \n" +
            "    ON o.alarm_id = a.alarm_id \n" +
            "   AND o.image_path = a.image_path \n" +
            "   AND o.video_path = a.video_path\n" +
            "LEFT JOIN feature_element_record f \n" +
            "    ON o.alarm_id = f.alarm_id \n" +
            "   AND o.image_path = f.image_path \n" +
            "   AND o.video_path = f.video_path\n" +
            "LEFT JOIN road_info r \n" +
            "    ON o.road_id = r.id " +
            "${ew.customSqlSegment}")
    IPage<QueryResultCheckRecord> selectWithJoin(Page<?> page, @Param(Constants.WRAPPER) QueryWrapper<QueryResultCheckRecord> wrapper);


    @Select({
            "<script>",
            "SELECT MIN(alarm_time) AS minAlarmTime,",
            "       MAX(alarm_time) AS maxAlarmTime",
            "FROM kafka_original_alarm_record",
            "WHERE tbl_id IN",
            "<foreach collection='tblIdList' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    AlarmTimeRange getTimeRangeByTblIdList(@Param("tblIdList") List<String> tblIdList);

    @Select({
            "<script>",
            "SELECT t1.*",
            "FROM kafka_original_alarm_record t1",
            "JOIN algorithm_check_result t2",
            "ON t1.alarm_id = t2.alarm_id",
            "AND t1.image_path = t2.image_path",
            "AND t1.video_path = t2.video_path",
            "AND t2.check_flag = #{checkFlag}",
            "WHERE t1.tbl_id IN",
            "<foreach collection='tblIdList' item='id' open='(' separator=',' close=')'>",
            "    #{id}",
            "</foreach>",
            "</script>"
    })
    List<OriginalAlarmRecord> getListByTblIdList(
            @Param("tblIdList") List<String> tblIdList,
            @Param("checkFlag") Integer checkFlag
    );
}