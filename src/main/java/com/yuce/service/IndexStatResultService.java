package com.yuce.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yuce.entity.IndexStatResult;
import java.util.List;

public interface IndexStatResultService extends IService<IndexStatResult> {

    /**
     * 查询有效告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    List<IndexStatResult> validAlarmCheckRate();

    /**
     * 查询有效告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    List<IndexStatResult> validAlarmCheckRightRate();

    /**
     * 查询误检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    List<IndexStatResult> errorReportCheckRate();

    /**
     * 查询误检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    List<IndexStatResult> errorReportCheckRightRate();

    /**
     * 查询正检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    List<IndexStatResult> rightReportCheckRate();

    /**
     * 查询正检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    List<IndexStatResult> rightReportCheckRightRate();
}