package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.entity.FeatureElementRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @ClassName FeatureElementMapper
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/6 13:44
 * @Version 1.0
 */

@Mapper
public interface FeatureElementMapper extends BaseMapper<FeatureElementRecord>{

    @Select("SELECT * FROM feature_element_record WHERE alarm_id = #{alarmId} AND image_path = #{imagePath} AND video_path = #{videoPath} LIMIT 1")
    FeatureElementRecord getFeatureByKey(@Param("alarmId") String alarmId,
                                       @Param("imagePath") String imagePath,
                                       @Param("videoPath") String videoPath);

}