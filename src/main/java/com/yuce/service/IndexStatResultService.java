package com.yuce.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yuce.entity.IndexStatResult;

public interface IndexStatResultService extends IService<IndexStatResult> {

    /**
     * @desc 插入有效告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    boolean insertValidAlarmCheckRate(String statDate);

    /**
     * @desc 插入有效告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    boolean insertValidAlarmCheckRightRate(String statDate);

    /**
     * @desc 插入误检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    boolean insertErrorReportCheckRate(String statDate);

    /**
     * @desc 插入误检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    boolean insertErrorReportCheckRightRate(String statDate);

    /**
     * @desc 插入正检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    boolean insertRightReportCheckRate(String statDate);

    /**
     * @desc 插入正检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    boolean insertRightReportCheckRightRate(String statDate);

    /**
     * @desc 插入告警压缩率
     * @return
     */
    boolean insertAlarmCompressionRate(String statDate);

    /**
     * @desc 交通事件检测转化率
     * @return
     */
    boolean insertTrafficEventConversionRate(String statDate);

    /**
     * @desc 事件关联跟踪准确率
     * @return
     */
    boolean insertEventTrackingAccuracy(String statDate);
}