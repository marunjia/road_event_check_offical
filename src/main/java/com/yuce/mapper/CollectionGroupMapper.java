package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.CollectionGroupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface CollectionGroupMapper extends BaseMapper<CollectionGroupRecord> {

    /**
     * 根据告警集ID和分组ID查询列表（适配索引 idx_collection_group）
     * @param collectionId 归属告警集id
     * @param groupId 分组ID
     * @return 分组记录列表
     */
    @Select(
            "SELECT " +
                    "t1.alarm_id AS alarmId, " +
                    "t1.image_path AS imagePath, " +
                    "t1.video_path AS videoPath, " +
                    "t2.id AS id, " +
                    "t2.match_check_flag AS matchCheckFlag, " +
                    "t2.match_check_reason AS matchCheckReason, " +
                    "t3.check_flag AS checkFlag, " +
                    "t4.alarm_time AS alarmTime, " +
                    "t4.event_type AS eventType " +
                    "FROM ( " +
                    "SELECT alarm_id, image_path, video_path, collection_id, group_id " +
                    "FROM collection_group_record " +
                    "WHERE collection_id = #{collectionId} " +
                    "AND group_id = #{groupId} " +
                    ") t1 " +
                    "LEFT JOIN feature_element_record t2 " +
                    "ON t1.alarm_id = t2.alarm_id " +
                    "AND t1.image_path = t2.image_path " +
                    "AND t1.video_path = t2.video_path " +
                    "LEFT JOIN algorithm_check_result t3 " +
                    "ON t1.alarm_id = t3.alarm_id " +
                    "AND t1.image_path = t3.image_path " +
                    "AND t1.video_path = t3.video_path " +
                    "LEFT JOIN kafka_original_alarm_record t4 " +
                    "ON t1.alarm_id = t4.alarm_id " +
                    "AND t1.image_path = t4.image_path " +
                    "AND t1.video_path = t4.video_path " +
                    "ORDER BY t4.alarm_time desc"
    )
    List<Map<String, Object>> queryByCollectionIdAndGroupId(
            @Param("collectionId") Integer collectionId,
            @Param("groupId") String groupId);

    /**
     * 根据告警集ID和分组ID查询列表（适配索引 idx_collection_group）
     * @param collectionId 归属告警集id
     * @return 分组记录列表
     */
    @Select(
            "SELECT " +
                    "t1.alarm_id AS alarmId, " +
                    "t1.image_path AS imagePath, " +
                    "t1.video_path AS videoPath, " +
                    "t2.id AS id, " +
                    "t2.match_check_flag AS matchCheckFlag, " +
                    "t2.match_check_reason AS matchCheckReason, " +
                    "t3.check_flag AS checkFlag, " +
                    "t4.alarm_time AS alarmTime, " +
                    "t4.event_type AS eventType " +
                    "FROM ( " +
                    "SELECT alarm_id, image_path, video_path, collection_id, group_id " +
                    "FROM collection_group_record " +
                    "WHERE collection_id = #{collectionId} " +
                    ") t1 " +
                    "LEFT JOIN feature_element_record t2 " +
                    "ON t1.alarm_id = t2.alarm_id " +
                    "AND t1.image_path = t2.image_path " +
                    "AND t1.video_path = t2.video_path " +
                    "LEFT JOIN algorithm_check_result t3 " +
                    "ON t1.alarm_id = t3.alarm_id " +
                    "AND t1.image_path = t3.image_path " +
                    "AND t1.video_path = t3.video_path " +
                    "LEFT JOIN kafka_original_alarm_record t4 " +
                    "ON t1.alarm_id = t4.alarm_id " +
                    "AND t1.image_path = t4.image_path " +
                    "AND t1.video_path = t4.video_path " +
                    "ORDER BY t4.alarm_time desc"
    )
    List<Map<String, Object>> queryByCollectionId(
            @Param("collectionId") Integer collectionId);

    @Select("SELECT " +
            "group_id as groupId, " +
            "max(group_image_url) as groupImageUrl, " +
            "count(tbl_id) as alarmNum " +
            "FROM collection_group_record " +
            "WHERE collection_id = #{collectionId} " +
            "GROUP BY group_id " +
            "UNION ALL " +
            "SELECT " +
            "'total' as groupId, " +
            "'' as groupImageUrl, " +
            "count(tbl_id) as alarmNum " +
            "FROM collection_group_record " +
            "WHERE collection_id = #{collectionId}")
    List<Map<String, Object>> getIndexByCollectionId(@Param("collectionId") Integer collectionId);
}