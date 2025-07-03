package com.yuce.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.entity.QueryResultCheckRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


/**
 * @ClassName CheckAlarmRecordMapper
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/22 16:12
 * @Version 1.0
 */

@Mapper
public interface CheckAlarmResultMapper extends BaseMapper<CheckAlarmResult> {

    @Select("SELECT * FROM algorithm_check_result WHERE alarm_id = #{alarmId} AND image_path = #{imagePath} AND video_path = #{videoPath} LIMIT 1")
    CheckAlarmResult getResultByKey(@Param("alarmId") String alarmId,
                                       @Param("imagePath") String imagePath,
                                       @Param("videoPath") String videoPath);
}