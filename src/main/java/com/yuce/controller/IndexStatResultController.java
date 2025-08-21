package com.yuce.controller;

import com.yuce.common.ApiResponse;
import com.yuce.entity.IndexStatResult;
import com.yuce.service.impl.IndexStatResultServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 算法校验统计控制器
 */
@RestController
@RequestMapping("/indexs")
@Slf4j
public class IndexStatResultController {

    @Autowired
    private IndexStatResultServiceImpl indexStatResultServiceImpl;

    /**
     * 查询有效告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @GetMapping("/valid-alarm-check-rate")
    public ApiResponse validAlarmCheckRate() {
        log.info("查询有效告警检出率");
        List<IndexStatResult> result = indexStatResultServiceImpl.validAlarmCheckRate();
        return ApiResponse.success(result);
    }

    /**
     * 查询有效告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @GetMapping("/valid-alarm-check-right-rate")
    public ApiResponse validAlarmCheckRightRate() {
        log.info("查询有效告警检出准确率");
        List<IndexStatResult> result = indexStatResultServiceImpl.validAlarmCheckRightRate();
        return ApiResponse.success(result);
    }

    /**
     * 查询误检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @GetMapping("/error-report-check-rate")
    public ApiResponse errorReportCheckRate() {
        log.info("查询误检告警检出率");
        List<IndexStatResult> result = indexStatResultServiceImpl.errorReportCheckRate();
        return ApiResponse.success(result);
    }

    /**
     * 查询误检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @GetMapping("/error-report-check-right-rate")
    public ApiResponse errorReportCheckRightRate() {
        log.info("查询误检告警检出准确率");
        List<IndexStatResult> result = indexStatResultServiceImpl.errorReportCheckRightRate();
        return ApiResponse.success(result);
    }

    /**
     * 查询正检检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @GetMapping("/right-report-check-rate")
    public ApiResponse rightReportCheckRate() {
        log.info("查询正检检出率");
        List<IndexStatResult> result = indexStatResultServiceImpl.rightReportCheckRate();
        return ApiResponse.success(result);
    }

    /**
     * 查询正检检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @GetMapping("/right-report-check-right-rate")
    public ApiResponse rightReportCheckRightRate() {
        log.info("查询正检检出准确率");
        List<IndexStatResult> result = indexStatResultServiceImpl.rightReportCheckRightRate();
        return ApiResponse.success(result);
    }
}