package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.AlarmCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AlarmCollection 数据访问层：操作 alarm_collection_v2 表，处理告警集相关查询与更新
 */
@Mapper
public interface AlarmCollectionMapper extends BaseMapper<AlarmCollection> {

    /**
     * 更新告警集记录关联标签
     * @param id 告警集唯一标识
     * @param personCheckFlag 行人检测标记（1-已检测，0-未检测等）
     * @return 影响行数（1-成功，0-无匹配数据）
     */
    @Update("UPDATE alarm_collection_v2 " +
            "SET person_check_flag = #{personCheckFlag}, " +
            "    modify_time = NOW() " + // 补充更新时间，便于数据追踪
            "WHERE id = #{id}") // 过滤已删除数据，避免误更新
    int updatePersonCheckFlag(
            @Param("id") Integer id,
            @Param("personCheckFlag") Integer personCheckFlag);

    /**
     * 更新告警集记录关联错误原因
     * @param id 告警集唯一标识
     * @param personCheckReason
     * @return 影响行数（1-成功，0-无匹配数据）
     */
    @Update("UPDATE alarm_collection_v2 " +
            "SET person_check_reason = #{personCheckReason}, " +
            "    modify_time = NOW() " + // 补充更新时间，便于数据追踪
            "WHERE collection_id = #{id} ")
    int updatePersonCheckReason(
            @Param("id") Integer id,
            @Param("personCheckReason") String personCheckReason);

    /**
     * @desc 统计指定时间区间范围内关联错误的原因分布情况
     * @param startTime
     * @param endTime
     * @return
     */
    @Select({
            "SELECT ",
            "    t3.person_check_reason AS reasonType, ",
            "    COUNT(t2.tbl_id) AS statCount ",
            "FROM kafka_original_alarm_record t1 ",
            "JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "JOIN alarm_collection_v2 t3 ON t2.match_collection_id = t3.id ",
            "WHERE t1.alarm_time >= #{startTime} ",
            "  AND t1.alarm_time <= #{endTime} ",
            "  AND t3.person_check_reason != '' ",
            "GROUP BY t3.person_check_reason"
    })
    List<Map<String, Object>> getIndexByReasonType(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * @desc 统计指定时间区间范围内关联错误的总记录条数
     * @param startTime
     * @param endTime
     * @return
     */
    @Select({
            "SELECT ",
            "    COUNT(t2.tbl_id) AS statCount ",
            "FROM kafka_original_alarm_record t1 ",
            "JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "JOIN alarm_collection_v2 t3 ON t2.match_collection_id = t3.id ",
            "WHERE t1.alarm_time >= #{startTime} ",
            "  AND t1.alarm_time <= #{endTime} ",
            "  AND t3.person_check_reason != ''"
    })
    List<Map<String, Object>> getIndexMatchErrorCount(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}