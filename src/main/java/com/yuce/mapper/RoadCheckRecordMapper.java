package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.RoadCheckRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @ClassName RoadCheckRecordMapper
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/14 10:46
 * @Version 1.0
 */

@Mapper
public interface RoadCheckRecordMapper extends BaseMapper<RoadCheckRecord>{

    /**
     * @desc 根据alarmId、imagePath、vodeoPath、type查询路面内检测结果
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @param type
     * @return
     */
    @Select("SELECT * FROM road_check_record WHERE alarm_id = #{alarmId} and image_path = #{imagePath} and video_path = #{videoPath} and type = #{type} and road_check_flag = #{checkFlag} and image_id != 'id_for_notice'")
    List<RoadCheckRecord> getRecordByKeyAndTypeAndFlag(@Param("alarmId")String alarmId, @Param("imagePath")String imagePath, @Param("videoPath")String videoPath, @Param("type")String type, @Param("checkFlag")Integer checkFlag);

    @Select("SELECT * FROM road_check_record WHERE alarm_id = #{alarmId} and image_path = #{imagePath} and video_path = #{videoPath} and type = #{type} and image_id != 'id_for_notice'")
    List<RoadCheckRecord> getRecordByKeyAndType(@Param("alarmId")String alarmId, @Param("imagePath")String imagePath, @Param("videoPath")String videoPath, @Param("type")String type);


}