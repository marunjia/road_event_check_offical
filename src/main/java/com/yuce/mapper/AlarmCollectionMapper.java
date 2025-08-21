package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.AlarmCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * AlarmCollection 数据访问层：操作 alarm_collection_v2 表，处理告警集相关查询与更新
 */
@Mapper
public interface AlarmCollectionMapper extends BaseMapper<AlarmCollection> {

    /**
     * 查询指定设备、指定类型的活跃告警集（collection_status=1）
     * 按创建时间倒序，取最新一条
     * @param deviceId 设备ID
     * @param collectionType 告警集类型
     * @return 符合条件的告警集，无则返回null
     */
    @Select("SELECT * FROM alarm_collection_v2 " +
            "WHERE device_id = #{deviceId} " +
            "  AND collection_type = #{collectionType} " +
            "  AND collection_status = 1 " +
            "ORDER BY create_time DESC " +
            "LIMIT 1")
    AlarmCollection getActiveCollectionByDeviceAndType(
            @Param("deviceId") String deviceId,
            @Param("collectionType") Integer collectionType); // 用Integer兼容null，避免基本类型默认值问题

    /**
     * 根据告警ID查询所属告警集（检查alarmId是否在related_alarm_id_list中）
     * @param alarmId 告警唯一标识
     * @return 所属告警集，无则返回null
     */
    @Select("SELECT * FROM alarm_collection_v2 " +
            "WHERE FIND_IN_SET(#{alarmId}, related_alarm_id_list)") // 补充逻辑删除过滤，避免查询已删除数据
    AlarmCollection getCollectionByAlarmId(@Param("alarmId") String alarmId);

    /**
     * 更新告警集记录关联标签
     * @param collectionId 告警集唯一标识
     * @param personCheckFlag 行人检测标记（1-已检测，0-未检测等）
     * @return 影响行数（1-成功，0-无匹配数据）
     */
    @Update("UPDATE alarm_collection_v2 " +
            "SET person_check_flag = #{personCheckFlag}, " +
            "    modify_time = NOW() " + // 补充更新时间，便于数据追踪
            "WHERE collection_id = #{collectionId}") // 过滤已删除数据，避免误更新
    int updatePersonCheckFlag(
            @Param("collectionId") String collectionId,
            @Param("personCheckFlag") Integer personCheckFlag);

    /**
     * 更新告警集记录关联错误原因
     * @param collectionId 告警集唯一标识
     * @param personCheckReason
     * @return 影响行数（1-成功，0-无匹配数据）
     */
    @Update("UPDATE alarm_collection_v2 " +
            "SET person_check_reason = #{personCheckReason}, " +
            "    modify_time = NOW() " + // 补充更新时间，便于数据追踪
            "WHERE collection_id = #{collectionId} ")
    int updatePersonCheckReason(
            @Param("collectionId") String collectionId,
            @Param("personCheckReason") String personCheckReason);
}