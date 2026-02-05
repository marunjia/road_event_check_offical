package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.IndexStatResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 算法校验统计Mapper（兼容JDK 1.8）
 * 所有语句均为：存在即更新，不存在即插入模式
 */
@Mapper
public interface IndexStatResultMapper extends BaseMapper<IndexStatResult> {

    /**
     * 有效告警检出率
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT ",
            "    #{statDate} AS stat_date,  '有效告警检出率' AS index_name, ",
            "    1 AS index_type, ",
            "    COUNT(IF(t2.person_check_flag = 2 AND t3.check_flag = 1, t2.alarm_id, NULL)) AS numerator, ",
            "    COUNT(IF(t2.person_check_flag = 2, t2.alarm_id, NULL)) AS denominator, ",
            "    CASE WHEN COUNT(IF(t2.person_check_flag = 2, t2.alarm_id, NULL)) = 0 THEN 0 ",
            "        ELSE ROUND(COUNT(IF(t2.person_check_flag = 2 AND t3.check_flag = 1, t2.alarm_id, NULL)) * 1.0 / COUNT(IF(t2.person_check_flag = 2, t2.alarm_id, NULL)), 4) ",
            "    END AS result, ",
            "    NOW() AS create_time, ",
            "    NOW() AS modify_time ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "LEFT JOIN algorithm_check_result t3 ON t1.tbl_id = t3.tbl_id ",
            "WHERE t1.alarm_time >= #{statDate} ",
            "  AND t1.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY)",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertValidAlarmCheckRate(@Param("statDate") String statDate);

    /**
     * 有效告警校验正确率
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT ",
            "    #{statDate} AS stat_date, ",
            "    '有效告警校验正确率' AS index_name, ",
            "    2 AS index_type, ",
            "    COUNT(IF(t2.person_check_flag = 2 AND t3.check_flag = 1, t2.alarm_id, NULL)) AS numerator, ",
            "    COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 1, t2.alarm_id, NULL)) AS denominator, ",
            "    CASE ",
            "        WHEN COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 1, t2.alarm_id, NULL)) = 0 THEN 0 ",
            "        ELSE ROUND(COUNT(IF(t2.person_check_flag = 2 AND t3.check_flag = 1, t2.alarm_id, NULL)) * 1.0 / COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 1, t2.alarm_id, NULL)), 4) ",
            "    END AS result, ",
            "    NOW() AS create_time, ",
            "    NOW() AS modify_time ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "LEFT JOIN algorithm_check_result t3 ON t1.tbl_id = t3.tbl_id ",
            "WHERE t1.alarm_time >= #{statDate} ",
            "  AND t1.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY) ",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertValidAlarmCheckRightRate(@Param("statDate") String statDate);

    /**
     * 误检告警检出率
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT ",
            "    #{statDate} AS stat_date, ",
            "    '误检告警检出率' AS index_name, ",
            "    3 AS index_type, ",
            "    COUNT(IF(t2.person_check_flag = 4 AND t3.check_flag = 2, t2.alarm_id, NULL)) AS numerator, ",
            "    COUNT(IF(t2.person_check_flag = 4, t2.alarm_id, NULL)) AS denominator, ",
            "    CASE WHEN COUNT(IF(t2.person_check_flag = 4, t2.alarm_id, NULL)) = 0 ",
            "        THEN 0 ",
            "        ELSE ROUND(COUNT(IF(t2.person_check_flag = 4 AND t3.check_flag = 2, t2.alarm_id, NULL)) * 1.0 ",
            "                   / COUNT(IF(t2.person_check_flag = 4, t2.alarm_id, NULL)), 4) END AS result, ",
            "    NOW() AS create_time, ",
            "    NOW() AS modify_time ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "LEFT JOIN algorithm_check_result t3 ON t1.tbl_id = t3.tbl_id ",
            "WHERE t1.alarm_time >= #{statDate} ",
            "  AND t1.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY) ",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertErrorReportCheckRate(@Param("statDate") String statDate);

    /**
     * 误检告警检出正确率
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT ",
            "    #{statDate} AS stat_date, ",
            "    '误检告警检出正确率' AS index_name, ",
            "    4 AS index_type, ",
            "    COUNT(IF(t2.person_check_flag = 4 AND t3.check_flag = 2, t2.alarm_id, NULL)) AS numerator, ",
            "    COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 2, t2.alarm_id, NULL)) AS denominator, ",
            "    CASE WHEN COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 2, t2.alarm_id, NULL)) = 0 ",
            "        THEN 0 ",
            "        ELSE ROUND(COUNT(IF(t2.person_check_flag = 4 AND t3.check_flag = 2, t2.alarm_id, NULL)) * 1.0 ",
            "                   / COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 2, t2.alarm_id, NULL)), 4) END AS result, ",
            "    NOW() AS create_time, ",
            "    NOW() AS modify_time ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "LEFT JOIN algorithm_check_result t3 ON t1.tbl_id = t3.tbl_id ",
            "WHERE t1.alarm_time >= #{statDate} ",
            "  AND t1.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY) ",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertErrorReportCheckRightRate(@Param("statDate") String statDate);

    /**
     * 正确告警检出率
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT ",
            "    #{statDate} AS stat_date, ",
            "    '正检告警检出率' AS index_name, ",
            "    5 AS index_type, ",
            "    COUNT(IF(t2.person_check_flag = 2 AND t3.check_flag = 1, t2.alarm_id, NULL)) AS numerator, ",
            "    COUNT(IF(t2.person_check_flag = 2, t2.alarm_id, NULL)) AS denominator, ",
            "    CASE WHEN COUNT(IF(t2.person_check_flag = 2, t2.alarm_id, NULL)) = 0 ",
            "        THEN 0 ",
            "        ELSE ROUND(COUNT(IF(t2.person_check_flag = 2 AND t3.check_flag = 1, t2.alarm_id, NULL)) * 1.0 ",
            "                   / COUNT(IF(t2.person_check_flag = 2, t2.alarm_id, NULL)), 4) END AS result, ",
            "    NOW() AS create_time, ",
            "    NOW() AS modify_time ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "LEFT JOIN algorithm_check_result t3 ON t1.tbl_id = t3.tbl_id ",
            "WHERE t1.alarm_time >= #{statDate} ",
            "  AND t1.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY) ",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertRightReportCheckRate(@Param("statDate") String statDate);

    /**
     * 正检告警检出正确率
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT ",
            "    #{statDate} AS stat_date, ",
            "    '正检告警检出正确率' AS index_name, ",
            "    6 AS index_type, ",
            "    COUNT(IF(t2.person_check_flag = 2 AND t3.check_flag = 1, t2.alarm_id, NULL)) AS numerator, ",
            "    COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 1, t2.alarm_id, NULL)) AS denominator, ",
            "    CASE WHEN COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 1, t2.alarm_id, NULL)) = 0 THEN 0 ",
            "        ELSE ROUND(COUNT(IF(t2.person_check_flag = 2 AND t3.check_flag = 1, t2.alarm_id, NULL)) * 1.0 / COUNT(IF(t2.person_check_flag != 1 AND t3.check_flag = 1, t2.alarm_id, NULL)), 4) END AS result, ",
            "    NOW() AS create_time, ",
            "    NOW() AS modify_time ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "LEFT JOIN algorithm_check_result t3 ON t1.tbl_id = t3.tbl_id   ",
            "WHERE t1.alarm_time >= #{statDate} ",
            "  AND t1.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY) ",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertRightReportCheckRightRate(@Param("statDate") String statDate);

    /**
     * 告警压缩率
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT ",
            "    #{statDate} AS stat_date, ",
            "    '告警压缩率' AS index_name, ",
            "    7 AS index_type, ",
            "    COUNT(DISTINCT(t2.match_collection_id)) AS numerator, ",
            "    COUNT(t1.tbl_id) AS denominator, ",
            "    CASE ",
            "        WHEN COUNT(t1.tbl_id) = 0 THEN 0 ",
            "        ELSE ROUND((COUNT(t1.tbl_id) - COUNT(DISTINCT(t2.match_collection_id))) * 1.0 / COUNT(t1.tbl_id), 4) ",
            "    END AS result, ",
            "    NOW() AS create_time, ",
            "    NOW() AS modify_time ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN feature_element_record t2 ON t1.tbl_id = t2.tbl_id ",
            "WHERE t1.alarm_time >= #{statDate} ",
            "  AND t1.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY) ",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertAlarmCompressionRate(@Param("statDate") String statDate);

    /**
     * 交通事件检测转化率
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT ",
            "    #{statDate} AS stat_date, ",
            "    '交通事件检测转化率' AS index_name, ",
            "    8 AS index_type, ",
            "    COUNT(IF(a.match_right_num = b.related_alarm_num and a.confirm_num > 0, a.match_collection_id, null)) AS numerator, ",
            "    COUNT(IF(a.confirm_num > 0, a.match_collection_id, null)) AS denominator, ",
            "    CASE WHEN COUNT(IF(a.confirm_num > 0, a.match_collection_id, null)) = 0 THEN 0 ",
            "        ELSE ROUND(COUNT(IF(a.match_right_num = b.related_alarm_num and a.confirm_num > 0, a.match_collection_id, null))/COUNT(IF(a.confirm_num > 0, a.match_collection_id, null)), 4) ",
            "    END AS result, ",
            "    NOW() AS create_time, ",
            "    NOW() AS modify_time ",
            "FROM (",
            "    SELECT ",
            "        t1.match_collection_id, ",
            "        COUNT(IF(t1.match_check_flag = 1, t1.tbl_id, null)) AS match_right_num, ",
            "        COUNT(IF(t1.disposal_advice = 2, t1.tbl_id, null)) AS confirm_num ",
            "    FROM feature_element_record t1 ",
            "    LEFT JOIN kafka_original_alarm_record t2 ON t1.tbl_id = t2.tbl_id ",
            "    WHERE t2.alarm_time >= #{statDate} ",
            "      AND t2.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY) ",
            "      AND t1.match_check_flag != 0 ",
            "    GROUP BY t1.match_collection_id ",
            ") a ",
            "JOIN alarm_collection_v2 b ON a.match_collection_id = b.id ",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertTrafficEventConversionRate(@Param("statDate") String statDate);

    /**
     * 事件关联跟踪准确率（动态日期参数）
     */
    @Insert({
            "INSERT INTO index_stat_result (stat_date, index_name, index_type, numerator, denominator, result, create_time, modify_time)",
            "SELECT #{statDate} AS stat_date, '事件关联跟踪准确率', 9, ",
            "COUNT(IF(t1.match_check_flag = 1 AND t1.person_check_flag = 2, t1.id, NULL)) AS numerator, ",
            "COUNT(IF(t1.match_check_flag != 0 AND t1.person_check_flag = 2, t1.id, NULL)) AS denominator, ",
            "CASE WHEN COUNT(IF(t1.match_check_flag !=0 AND t1.person_check_flag = 2, t1.id, NULL)) = 0 THEN 0 ELSE ROUND(COUNT(IF(t1.match_check_flag = 1 AND t1.person_check_flag = 2, t1.id, NULL)) * 1.0 / COUNT(IF(t1.match_check_flag !=0 AND t1.person_check_flag = 2, t1.id, NULL)), 4) END, ",
            "NOW(), NOW() ",
            "FROM feature_element_record t1 ",
            "LEFT JOIN kafka_original_alarm_record t2 ON t1.tbl_id = t2.tbl_id ",
            "WHERE t1.match_check_flag != 0 ",
            "  AND t2.alarm_time >= #{statDate} ",
            "  AND t2.alarm_time < DATE_ADD(#{statDate}, INTERVAL 1 DAY) ",
            "ON DUPLICATE KEY UPDATE ",
            "    numerator = VALUES(numerator), ",
            "    denominator = VALUES(denominator), ",
            "    result = VALUES(result), ",
            "    modify_time = NOW()"
    })
    int insertEventTrackingAccuracy(@Param("statDate") String statDate);

    /**
     * @desc 根据时间区间按照告警类型、初检结果、处置建议统计告警记录条数
     * @param startTime
     * @param endTime
     * @return
     */
    @Select({
            "SELECT ",
            "    1 as data_level, ",
            "    t1.event_type, ",
            "    NULL AS check_flag, ",
            "    NULL AS disposal_advice, ",
            "    COUNT(t1.alarm_id) AS alarm_count ",
            "FROM kafka_original_alarm_record t1 ",
            "WHERE t1.alarm_time >= #{startTime} ",
            "  AND t1.alarm_time <= #{endTime} ",
            "GROUP BY t1.event_type ",
            "UNION ALL ",
            "SELECT ",
            "    2 as data_level, ",
            "    t1.event_type, ",
            "    t2.check_flag, ",
            "    NULL AS disposal_advice, ",
            "    COUNT(t1.alarm_id) AS alarm_count ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN algorithm_check_result t2 ON t1.tbl_id = t2.tbl_id ",
            "WHERE t1.alarm_time >= #{startTime} ",
            "  AND t1.alarm_time <= #{endTime} ",
            "GROUP BY t1.event_type, t2.check_flag ",
            "UNION ALL ",
            "SELECT ",
            "    3 as data_level, ",
            "    t1.event_type, ",
            "    t2.check_flag, ",
            "    t3.disposal_advice, ",
            "    COUNT(t1.alarm_id) AS alarm_count ",
            "FROM kafka_original_alarm_record t1 ",
            "LEFT JOIN algorithm_check_result t2 ON t1.tbl_id = t2.tbl_id ",
            "LEFT JOIN feature_element_record t3 ON t1.tbl_id = t3.tbl_id ",
            "WHERE t1.alarm_time >= #{startTime} ",
            "  AND t1.alarm_time <= #{endTime} ",
            "GROUP BY t1.event_type, t2.check_flag, t3.disposal_advice"
    })
    List<Map<String, Object>> getFunnelAnalysis(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

}