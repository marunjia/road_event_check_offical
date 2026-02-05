package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.IndexStatResult;
import com.yuce.mapper.IndexStatResultMapper;
import com.yuce.service.IndexStatResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @ClassName IndexServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/11/5 14:05
 * @Version 1.0
 */

@Slf4j
@Service
public class IndexStatResultServiceImpl extends ServiceImpl<IndexStatResultMapper, IndexStatResult> implements IndexStatResultService{

    @Autowired
    private IndexStatResultMapper indexStatResultMapper;

    /**
     * 查询有效告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertValidAlarmCheckRate(String statDate) {
        int rows =  indexStatResultMapper.insertValidAlarmCheckRate(statDate);
        return rows > 0;
    }

    /**
     * 查询有效告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertValidAlarmCheckRightRate(String statDate) {
        int rows =   indexStatResultMapper.insertValidAlarmCheckRightRate(statDate);
        return rows > 0;
    }

    /**
     * 查询误检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertErrorReportCheckRate(String statDate) {
        int rows = indexStatResultMapper.insertErrorReportCheckRate(statDate);
        return rows > 0;
    }

    /**
     * 查询误检检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertErrorReportCheckRightRate(String statDate) {
        int rows =  indexStatResultMapper.insertErrorReportCheckRightRate(statDate);
        return rows > 0;
    }

    /**
     * 查询正检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertRightReportCheckRate(String statDate) {
        int rows =  indexStatResultMapper.insertRightReportCheckRate(statDate);
        return rows > 0;
    }

    /**
     * 查询正检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertRightReportCheckRightRate(String statDate) {
        int rows =  indexStatResultMapper.insertRightReportCheckRightRate(statDate);
        return rows > 0;
    }

    /**
     * 查询告警压缩率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertAlarmCompressionRate(String statDate) {
        int rows =  indexStatResultMapper.insertAlarmCompressionRate(statDate);
        return rows > 0;
    }

    /**
     * 插入交通事件检测转化率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertTrafficEventConversionRate(String statDate) {
        int rows =  indexStatResultMapper.insertTrafficEventConversionRate(statDate);
        return rows > 0;
    }

    /**
     * 插入事件关联跟踪准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public boolean insertEventTrackingAccuracy(String statDate) {
        int rows =  indexStatResultMapper.insertEventTrackingAccuracy(statDate);
        return rows > 0;
    }

    /**
     * @desc 根据指标名称查询数据
     * @param indexType
     * @return
     */
    public List<IndexStatResult> getIndexByIndexType(Integer indexType) {
        QueryWrapper<IndexStatResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("index_type", indexType);
        queryWrapper.orderByAsc("modify_time");
        return this.list(queryWrapper);
    }

    /**
     * @desc 根据时间区间按照告警类型、初检结果、处置建议统计告警记录条数
     * @param startTime
     * @param endTime
     * @return
     */
    public List<Map<String, Object>> getFunnelAnalysis(LocalDateTime startTime, LocalDateTime endTime) {
        return indexStatResultMapper.getFunnelAnalysis(startTime, endTime);
    }
}