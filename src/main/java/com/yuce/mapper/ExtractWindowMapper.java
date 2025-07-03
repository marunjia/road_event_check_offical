package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.ExtractWindowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ExtractWindowMapper extends BaseMapper<ExtractWindowRecord> {

    @Select("SELECT * FROM extract_point_record WHERE alarm_id = #{alarmId} AND image_path = #{imagePath} AND video_path = #{videoPath} LIMIT 1")
    ExtractWindowRecord getWindowByKey(@Param("alarmId") String alarmId,
                                       @Param("imagePath") String imagePath,
                                       @Param("videoPath") String videoPath);
}
