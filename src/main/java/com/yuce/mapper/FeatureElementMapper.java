package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.FeatureElementRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * @desc 根据联合唯一主键查询特征要素
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    @Select("SELECT * FROM feature_element_record WHERE alarm_id = #{alarmId} AND image_path = #{imagePath} AND video_path = #{videoPath} LIMIT 1")
    FeatureElementRecord getFeatureByKey(@Param("alarmId") String alarmId,
                                       @Param("imagePath") String imagePath,
                                       @Param("videoPath") String videoPath);

    /**
     * @desc 根据特征要素id更新告警集关联状态
     * @param id
     * @param collectionMatchStatus
     * @return
     */
    @Update("update feature_element_record set collection_match_status = #{collectionMatchStatus} where id = #{id}")
    int updateCollectionMatchStatus(@Param("id") Integer id, @Param("collectionMatchStatus") Integer collectionMatchStatus);

    /**
     * @desc 根据特征要素id更新告警集人工核查标签
     * @param id
     * @param personCheckFlag
     * @return
     */
    @Update("update feature_element_record set person_check_flag = #{personCheckFlag} where id = #{id}")
    int updatePersonCheckFlag(@Param("id") Integer id, @Param("personCheckFlag") Integer personCheckFlag);

    /**
     * @desc 根据特征要素id更新告警记录是否正确匹配标签
     * @param id
     * @param matchCheckFlag
     * @return
     */
    @Update("update feature_element_record set match_check_flag = #{matchCheckFlag} where id = #{id}")
    int updateMatchCheckFlag(@Param("id") Integer id, @Param("matchCheckFlag") Integer matchCheckFlag);

    /**
     * @desc 根据特征要素id更新告警记录匹配错误原因
     * @param id
     * @param matchCheckReason
     * @return
     */
    @Update("update feature_element_record set match_check_reason = #{matchCheckReason} where id = #{id}")
    int updateMatchCheckReason(@Param("id") Integer id, @Param("matchCheckReason") String matchCheckReason);

    /**
     * @desc 根据告警记录时间区间查询特征要素列表
     * @param previousMinute
     * @param currentMinute
     * @return
     */
    @Select("SELECT *\n" +
            "FROM feature_element_record\n" +
            "WHERE create_time >= #{previousMinute}\n" +
            "AND create_time < #{currentMinute}")
    List<FeatureElementRecord> getListByTimeRange(@Param("previousMinute") LocalDateTime previousMinute,
                                                  @Param("currentMinute") LocalDateTime currentMinute);

}