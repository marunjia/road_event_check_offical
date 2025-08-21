package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.FrameImageInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @ClassName AlarmFrameImageMapper
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/22 14:30
 * @Version 1.0
 */

@Mapper
public interface FrameImageMapper extends BaseMapper<FrameImageInfo> {

    /**
     * @desc 根据alarmId、imagePath、videoPath、imageSortNo查询记录是否存在
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @param imageSortNo
     * @return
     */
    @Select("SELECT * FROM alarm_frame_image_info " +
            "WHERE alarm_id = #{alarmId} " +
            "AND image_path = #{imagePath} " +
            "AND video_path = #{videoPath} " +
            "AND image_sort_no = #{imageSortNo} " +
            "LIMIT 1")
    FrameImageInfo getFrameByKey(@Param("alarmId") String alarmId,
                               @Param("imagePath") String imagePath,
                               @Param("videoPath") String videoPath,
                               @Param("imageSortNo") int imageSortNo);


    /**
     * @desc 根据alarmId、imagePath、videoPath查询抽帧图片列表
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    @Select("SELECT * FROM alarm_frame_image_info " +
            "WHERE alarm_id = #{alarmId} " +
            "AND image_path = #{imagePath} " +
            "AND video_path = #{videoPath} " +
            "ORDER BY create_time DESC")
    List<FrameImageInfo> getFrameListByKey(@Param("alarmId") String alarmId,
                                           @Param("imagePath") String imagePath,
                                           @Param("videoPath") String videoPath);

    /**
     * @desc 根据 alarmId、imagePath、videoPath 删除抽帧图片记录
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return 删除的记录数
     */
    @Delete("DELETE FROM alarm_frame_image_info " +
            "WHERE alarm_id = #{alarmId} " +
            "AND image_path = #{imagePath} " +
            "AND video_path = #{videoPath}")
    int deleteByKey(@Param("alarmId") String alarmId,
                    @Param("imagePath") String imagePath,
                    @Param("videoPath") String videoPath);

    /**
     * @desc 根据alarmId、imagePath、videoPath、image_sort_no查询抽帧图片
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    @Select("SELECT * FROM alarm_frame_image_info " +
            "WHERE alarm_id = #{alarmId} " +
            "AND image_path = #{imagePath} " +
            "AND video_path = #{videoPath} " +
            "AND image_sort_no = #{imageSortNo} ")
    FrameImageInfo getFrameByKeyAndNo(@Param("alarmId") String alarmId,
                                      @Param("imagePath") String imagePath,
                                      @Param("videoPath") String videoPath,
                                      @Param("imageSortNo") int imageSortNo);
}