package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.IndexStatResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 算法校验统计Mapper
 */
@Mapper
public interface IndexStatResultMapper extends BaseMapper<IndexStatResult> {

    /**
     * 按日期查询有效告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Select("SELECT " +
            "    date(t3.alarm_time) as stat_date, " +
            "    count(t2.alarm_id) as numerator, " +
            "    count(t1.alarm_id) as denominator, " +
            "    CASE WHEN count(t1.alarm_id) = 0 THEN 0 " +
            "         ELSE round(count(t2.alarm_id) * 1.0 / count(t1.alarm_id), 4) " +
            "    END as result " +  // 新增防除零和四舍五入处理
            "FROM (" +
            "    SELECT * FROM feature_element_record WHERE person_check_flag = 2" +
            ") t1 " +
            "LEFT JOIN (" +
            "    SELECT * FROM algorithm_check_result WHERE check_flag = 1" +
            ") t2 " +
            "ON t1.alarm_id = t2.alarm_id " +
            "AND t1.image_path = t2.image_path " +
            "AND t1.video_path = t2.video_path " +
            "JOIN kafka_original_alarm_record t3 " +
            "ON t1.alarm_id = t3.alarm_id " +
            "AND t1.image_path = t3.image_path " +
            "AND t1.video_path = t3.video_path " +
            "GROUP BY date(t3.alarm_time) " +
            "ORDER BY stat_date")  // 按日期倒序，便于查看最新数据
    List<IndexStatResult> validAlarmCheckRate();

    /**
     * 按日期查询有效告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Select("SELECT " +
            "    date(t3.alarm_time) as stat_date, " +
            "    count(t2.alarm_id) as numerator, " +
            "    count(t1.alarm_id) as denominator, " +
            "    CASE WHEN count(t1.alarm_id) = 0 THEN 0 " +  // 防除零处理
            "         ELSE round(count(t2.alarm_id) * 1.0 / count(t1.alarm_id), 4) " +  // 保留4位小数
            "    END as result " +
            "FROM (" +
            "    SELECT * FROM algorithm_check_result WHERE check_flag = 1" +  // t1表：算法检查结果，check_flag=1
            ") t1 " +
            "LEFT JOIN (" +
            "    SELECT * FROM feature_element_record WHERE person_check_flag = 2" +  // t2表：特征元素记录，person_check_flag=2
            ") t2 " +
            "ON t1.alarm_id = t2.alarm_id " +
            "AND t1.image_path = t2.image_path " +
            "AND t1.video_path = t2.video_path " +
            "JOIN kafka_original_alarm_record t3 " +  // 关联原始告警表取时间
            "ON t1.alarm_id = t3.alarm_id " +
            "AND t1.image_path = t3.image_path " +
            "AND t1.video_path = t3.video_path " +
            "GROUP BY date(t3.alarm_time) " +
            "ORDER BY stat_date")  // 按日期倒序
    List<IndexStatResult> validAlarmCheckRightRate();

    /**
     * 按日期查询误检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Select("SELECT " +
            "    date(t3.alarm_time) AS stat_date, " +
            "    count(t2.alarm_id) AS numerator, " +  // t2表（algorithm_check_result）记录数
            "    count(t1.alarm_id) AS denominator, " +  // t1表（feature_element_record）记录数
            "    CASE WHEN count(t1.alarm_id) = 0 THEN 0 " +  // 避免除零错误
            "         ELSE ROUND(count(t2.alarm_id) * 1.0 / count(t1.alarm_id), 4) " +  // 保留4位小数
            "    END AS result " +
            "FROM (" +
            "    SELECT * FROM feature_element_record WHERE person_check_flag = 4" +  // t1条件
            ") t1 " +
            "JOIN (" +  // 内连接t2表
            "    SELECT * FROM algorithm_check_result WHERE check_flag = 2" +  // t2条件（修正表名拼写）
            ") t2 " +
            "ON 1 = 1 " +  // 无条件全量关联
            "JOIN kafka_original_alarm_record t3 " +  // 关联原始告警表获取时间
            "ON t1.alarm_id = t3.alarm_id " +
            "   AND t1.image_path = t3.image_path " +
            "   AND t1.video_path = t3.video_path " +
            "GROUP BY date(t3.alarm_time) " +  // 按日期分组
            "ORDER BY stat_date")  // 按日期倒序排列
    List<IndexStatResult> errorReportCheckRate();

    /**
     * 按日期查询误检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Select("SELECT " +
            "    date(t3.alarm_time) AS stat_date, " +
            "    count(t2.alarm_id) AS numerator, " +  // t2中匹配的记录数（左连接：无匹配则为0）
            "    count(t1.alarm_id) AS denominator, " +  // t1的总记录数（person_check_flag=4）
            "    CASE WHEN count(t1.alarm_id) = 0 THEN 0 " +  // 防除零错误
            "         ELSE ROUND(count(t2.alarm_id) * 1.0 / count(t1.alarm_id), 4) " +  // 保留4位小数
            "    END AS result " +
            "FROM (" +
            "    SELECT * FROM feature_element_record WHERE person_check_flag = 4" +  // t1条件
            ") t1 " +
            "LEFT JOIN (" +  // 左连接：保留t1所有记录，匹配t2中符合条件的记录
            "    SELECT * FROM algorithm_check_result WHERE check_flag = 2" +  // t2条件
            ") t2 " +
            "ON t1.alarm_id = t2.alarm_id " +  // 按alarm_id关联
            "   AND t1.image_path = t2.image_path " +  // 按image_path关联
            "   AND t1.video_path = t2.video_path " +  // 按video_path关联
            "JOIN kafka_original_alarm_record t3 " +  // 内连接原始告警表（过滤t1中无对应告警的记录）
            "ON t1.alarm_id = t3.alarm_id " +
            "   AND t1.image_path = t3.image_path " +
            "   AND t1.video_path = t3.video_path " +
            "GROUP BY date(t3.alarm_time) " +  // 按日期分组
            "ORDER BY stat_date")  // 按日期倒序
    List<IndexStatResult> errorReportCheckRightRate();

    /**
     * 按日期查询正检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Select("SELECT " +
            "    date(t3.alarm_time) AS stat_date, " +
            "    count(t2.alarm_id) AS numerator, " +  // t2记录数（因无条件关联，值为t2总数×t1总数）
            "    count(t1.alarm_id) AS denominator, " +  // t1总记录数（person_check_flag=2）
            "    CASE WHEN count(t1.alarm_id) = 0 THEN 0 " +  // 避免除零错误
            "         ELSE ROUND(count(t2.alarm_id) * 1.0 / count(t1.alarm_id), 4) " +  // 保留4位小数
            "    END AS result " +
            "FROM (" +
            "    SELECT * FROM feature_element_record WHERE person_check_flag = 2" +  // t1筛选条件
            ") t1 " +
            "JOIN (" +  // 内连接t2，无条件关联
            "    SELECT * FROM algorithm_check_result WHERE check_flag = 1" +  // t2筛选条件
            ") t2 " +
            "ON 1 = 1 " +  // 无条件关联（全量匹配，产生笛卡尔积）
            "JOIN kafka_original_alarm_record t3 " +  // 关联原始告警表获取时间
            "ON t1.alarm_id = t3.alarm_id " +
            "   AND t1.image_path = t3.image_path " +
            "   AND t1.video_path = t3.video_path " +
            "GROUP BY date(t3.alarm_time) " +  // 按日期分组统计
            "ORDER BY stat_date")  // 按日期倒序排列
    List<IndexStatResult> rightReportCheckRate();

    /**
     * 按日期查询正检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Select("SELECT " +
            "    date(t3.alarm_time) AS stat_date, " +
            "    count(t2.alarm_id) AS numerator, " +  // t2表记录数（无条件关联后被放大）
            "    count(t1.alarm_id) AS denominator, " +  // t1表总记录数（person_check_flag=2）
            "    CASE WHEN count(t1.alarm_id) = 0 THEN 0 " +  // 防除零错误
            "         ELSE ROUND(count(t2.alarm_id) * 1.0 / count(t1.alarm_id), 4) " +  // 保留4位小数
            "    END AS result " +
            "FROM (" +
            "    SELECT * FROM feature_element_record WHERE person_check_flag = 2" +  // t1条件
            ") t1 " +
            "JOIN (" +  // 内连接t2表（无条件关联）
            "    SELECT * FROM algorithm_check_result WHERE check_flag = 1" +  // t2条件
            ") t2 " +
            "ON 1 = 1 " +  // 无条件全量关联（笛卡尔积）
            "JOIN kafka_original_alarm_record t3 " +  // 关联原始告警表获取时间
            "ON t1.alarm_id = t3.alarm_id " +
            "   AND t1.image_path = t3.image_path " +
            "   AND t1.video_path = t3.video_path " +
            "GROUP BY date(t3.alarm_time) " +  // 按日期分组
            "ORDER BY stat_date")  // 按日期倒序
    List<IndexStatResult> rightReportCheckRightRate();
}