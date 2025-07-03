package com.yuce.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * @desc 根据告警id、图片路径、视频路径组成的联合主键判断记录是否存在
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    @Select("SELECT * FROM kafka_original_alarm_record WHERE alarm_id = #{alarmId} and image_path = #{imagePath} and video_path = #{videoPath} ")
    boolean existsByKey(@Param("alarmId")String alarmId, @Param("imagePath")String imagePath, @Param("videoPath")String videoPath);

    /**
     * @desc 根据告警id、图片路径、视频路径组成的联合主键判断记录是否存在
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    @Select("SELECT * FROM kafka_original_alarm_record WHERE alarm_id = #{alarmId} and image_path = #{imagePath} and video_path = #{videoPath} ")
    OriginalAlarmRecord getRecordByKey(@Param("alarmId")String alarmId, @Param("imagePath")String imagePath, @Param("videoPath")String videoPath);

    /**
     * @desc 根据关联alarmIdList查询所有告警记录明细
     * @param relatedIdList
     * @return
     */
    @Select({
            "<script>",
            "SELECT * FROM kafka_original_alarm_record",
            "WHERE alarm_id IN",
            "<foreach item='alarm_id' index='index' collection='relatedIdList' open='(' separator=',' close=')'>",
            "#{alarm_id}",
            "</foreach>",
            "ORDER BY alarm_time DESC",
            "</script>"
    })
    List<OriginalAlarmRecord> getListByIdList(@Param("relatedIdList") List<String> relatedIdList);

    /**
     * @desc 查询告警集关联告警记录中被打标为事件的记录
     * @param relatedIdList
     * @return
     */
    @Select({
            "<script>",
            "SELECT * FROM kafka_original_alarm_record",
            "WHERE alarm_id IN ",
            "<foreach collection='relatedIdList' item='alarm_id' open='(' separator=',' close=')'>",
            "#{alarm_id}",
            "</foreach>",
            "AND deal_flag = '1' ",
            "ORDER BY alarm_time DESC",
            "</script>"
    })
    List<OriginalAlarmRecord> getEventByIdList(@Param("relatedIdList") List<String> relatedIdList);


    /**
     * @desc 根据关联alarmIdList查询所有告警记录明细
     * @param relatedList
     * @return
     */
    @Select({
            "<script>",
            "SELECT * FROM kafka_original_alarm_record",
            "WHERE tbl_id IN",
            "<foreach item='id' collection='relatedList' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND deal_flag = '1'",
            "ORDER BY alarm_time DESC",
            "</script>"
    })
    List<OriginalAlarmRecord> getListByDealFlag(@Param("relatedList") List<String> relatedList);

    /**
     * @desc 查询未被标记为事件的记录
     * @param relatedList
     * @return
     */
    @Select({
            "<script>",
            "SELECT * FROM kafka_original_alarm_record",
            "WHERE tbl_id IN",
            "<foreach item='id' collection='relatedList' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND deal_flag != '1'",
            "ORDER BY alarm_time DESC",
            "</script>"
    })
    List<OriginalAlarmRecord> getUnConfirmList(@Param("relatedList") List<String> relatedList);

    /**
     * @desc 根据时间范围查询未被标记为事件的记录
     * @param relatedList
     * @param alarmTime
     * @return
     */
    @Select({
            "<script>",
            "SELECT * FROM kafka_original_alarm_record",
            "WHERE tbl_id IN",
            "<foreach item='id' collection='relatedList' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND deal_flag != '1'",
            "AND alarm_time &gt; #{alarmTime}",
            "ORDER BY alarm_time DESC",
            "</script>"
    })
    List<OriginalAlarmRecord> getUnConfirmListWithTime(
            @Param("relatedList") List<String> relatedList,
            @Param("alarmTime") LocalDateTime alarmTime
    );

    /**
     * @desc 查询最新一条被确认为事件的记录
     * @param relatedList
     * @return
     */
    @Select({
            "<script>",
            "SELECT * FROM kafka_original_alarm_record",
            "WHERE tbl_id IN",
            "<foreach item='id' collection='relatedList' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "AND deal_flag = '1'",
            "ORDER BY alarm_time DESC",
            "LIMIT 1",
            "</script>"
    })
    OriginalAlarmRecord getLatestConfirm(@Param("relatedList") List<String> relatedList);

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
            "    f.disposal_advice AS advice_flag, \n" +
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
}