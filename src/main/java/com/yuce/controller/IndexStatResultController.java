package com.yuce.controller;

import com.yuce.common.ApiResponse;
import com.yuce.entity.IndexStatResult;
import com.yuce.service.impl.IndexStatResultServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

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
    @GetMapping("/calculate")
    public ApiResponse calculate() {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.format(formatter);
        //有效告警检出率
        indexStatResultServiceImpl.insertValidAlarmCheckRate(formattedDate);

        //有效告警检出正确率
        indexStatResultServiceImpl.insertValidAlarmCheckRightRate(formattedDate);

        //误检告警检出率
        indexStatResultServiceImpl.insertErrorReportCheckRate(formattedDate);

        //误检告警检出正确率
        indexStatResultServiceImpl.insertErrorReportCheckRightRate(formattedDate);

        //正检告警检出率
        indexStatResultServiceImpl.insertRightReportCheckRate(formattedDate);

        //正检告警检出正确率
        indexStatResultServiceImpl.insertRightReportCheckRightRate(formattedDate);

        //告警压缩率
        indexStatResultServiceImpl.insertAlarmCompressionRate(formattedDate);

        //交通事件检测转化率
        indexStatResultServiceImpl.insertTrafficEventConversionRate(formattedDate);

        //事件关联跟踪准确率
        indexStatResultServiceImpl.insertEventTrackingAccuracy(formattedDate);
        return ApiResponse.success("数据查询成功");
    }


    @GetMapping("/byIndexType")
    public ApiResponse getIndexByName(@RequestParam("indexType") Integer indexType) {
        // 调用 Service 获取指标统计结果
        List<IndexStatResult> result = indexStatResultServiceImpl.getIndexByIndexType(indexType);
        // 返回成功响应
        return ApiResponse.success(result);
    }

    /**
     * @desc 根据时间区间按照告警类型、初检结果、处置建议统计告警记录条数
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/funnel/byDimension")
    public ApiResponse getFunnelAnalysis(
            @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        // 调用 Service 获取指标统计结果

        if (Objects.isNull(startTime) || Objects.isNull(endTime)) {
            return ApiResponse.fail(400,"开始时间/结束时间不能为空");
        }
        if (startTime.isAfter(endTime)) {
            return ApiResponse.fail(400,"开始时间不能晚于结束时间");
        }
        return ApiResponse.success(indexStatResultServiceImpl.getFunnelAnalysis(startTime, endTime));
    }
}