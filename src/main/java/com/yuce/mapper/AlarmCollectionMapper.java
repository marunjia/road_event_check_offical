package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.AlarmCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface AlarmCollectionMapper extends BaseMapper<AlarmCollection> {
    // 可以根据需要定义自定义的查询方法
    @Select("SELECT * FROM alarm_collection_v2 WHERE device_id = #{deviceId} AND collection_type = #{collectionType} AND collection_status = 1")
    AlarmCollection selectLastByDeviceIdAndType(@Param("deviceId") String deviceId,
                                                @Param("collectionType") int collectionType);
}