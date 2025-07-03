package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.AlarmCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface AlarmCollectionMapper extends BaseMapper<AlarmCollection> {

    @Select("SELECT * FROM alarm_collection " +
            "WHERE device_id = #{deviceId} " +
            "AND collection_type = #{collectionType} " +
            "AND collection_status = 1 " +
            "ORDER BY create_time DESC " +
            "LIMIT 1")
    AlarmCollection existsCollection(@Param("deviceId") String deviceId,
                                     @Param("collectionType") int collectionType);

    @Select("SELECT * FROM alarm_collection WHERE FIND_IN_SET(#{alarmId}, related_id_list)")
    AlarmCollection getCollectionByAlarmId(@Param("alarmId") String alarmId);
}