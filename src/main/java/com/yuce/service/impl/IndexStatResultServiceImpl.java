package com.yuce.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.IndexStatResult;
import com.yuce.mapper.IndexStatResultMapper;
import com.yuce.service.IndexStatResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

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
    public List<IndexStatResult> validAlarmCheckRate() {
        log.info("查询有效告警检出率");
        List<IndexStatResult> statList = indexStatResultMapper.validAlarmCheckRate();
        log.info("查询有效告警检出率 | 数据条数:{}", statList.size());
        return statList;
    }

    /**
     * 查询有效告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public List<IndexStatResult> validAlarmCheckRightRate() {
        log.info("查询有效告警检出准确率");
        List<IndexStatResult> statList = indexStatResultMapper.validAlarmCheckRightRate();
        log.info("查询有效告警检出准确率 | 数据条数:{}", statList.size());
        return statList;
    }

    /**
     * 查询误检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public List<IndexStatResult> errorReportCheckRate() {
        log.info("查询误检检出率");
        List<IndexStatResult> statList = indexStatResultMapper.errorReportCheckRate();
        log.info("查询误检检出率 | 数据条数:{}", statList.size());
        return statList;
    }

    /**
     * 查询误检检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public List<IndexStatResult> errorReportCheckRightRate() {
        log.info("查询误检检出准确率");
        List<IndexStatResult> statList = indexStatResultMapper.errorReportCheckRightRate();
        log.info("查询误检检出准确率 | 数据条数:{}", statList.size());
        return statList;
    }

    /**
     * 查询正检告警检出率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public List<IndexStatResult> rightReportCheckRate() {
        log.info("查询正检检出率");
        List<IndexStatResult> statList = indexStatResultMapper.rightReportCheckRate();
        log.info("查询正检检出率 | 数据条数:{}", statList.size());
        return statList;
    }

    /**
     * 查询正检告警检出准确率
     * @return 统计数据列表（包含每日的分子、分母、计算结果）
     */
    @Override
    public List<IndexStatResult> rightReportCheckRightRate() {
        log.info("查询正检检出准确率");
        List<IndexStatResult> statList = indexStatResultMapper.rightReportCheckRightRate();
        log.info("查询正检检出准确率 | 数据条数:{}", statList.size());
        return statList;
    }
}