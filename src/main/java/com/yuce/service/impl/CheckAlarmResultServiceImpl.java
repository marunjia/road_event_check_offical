package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.config.VideoProperties;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmProcessMapper;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.service.CheckAlarmResultService;
import com.yuce.util.FlagTagUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @ClassName AlgorithmCheckServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/22 16:07
 * @Version 1.0
 */

@Slf4j
@Service
public class CheckAlarmResultServiceImpl extends ServiceImpl<CheckAlarmResultMapper, CheckAlarmResult> implements CheckAlarmResultService {



    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private CheckAlarmProcessMapper checkAlarmProcessMapper;

    /**
     * @param tblId
     * @return
     * @desc 根据tblId查询告警记录
     */
    public CheckAlarmResult getResultByTblId(long tblId) {
        QueryWrapper<CheckAlarmResult> query = new QueryWrapper<>();
        query.eq("tbl_id", tblId);
        return checkAlarmResultMapper.selectOne(query);
    }

    /**
     * @desc 检测初检结果是否存在：false代表不存在，true代表存在
     * @param tblId
     * @return
     */
    public boolean isExistByTblId(long tblId) {
        QueryWrapper<CheckAlarmResult> query = new QueryWrapper<>();
        query.eq("tbl_id", tblId);
        CheckAlarmResult checkAlarmResult = checkAlarmResultMapper.selectOne(query);
        return checkAlarmResult != null ? true : false;
    }

    /**
     * @param id
     * @desc 获取最近50条误检数据的设备id
     */
    public List<String> getRecentFalseCheckList(Long id) {
        return checkAlarmResultMapper.getRecentFalseCheckList(id);
    }

    /**
     * @desc 新增检测结果
     * @param record
     * @param checkFlag
     * @param checkReason
     * @param sourceType
     * @param name
     */
    public void insert(OriginalAlarmRecord record, int checkFlag, String checkReason, String sourceType, String name) {
        CheckAlarmResult result = new CheckAlarmResult();
        result.setTblId(record.getTblId());
        result.setAlarmId(record.getId());
        result.setImagePath(record.getImagePath());
        result.setVideoPath(record.getVideoPath());
        result.setCheckFlag(checkFlag);
        result.setCheckName(name);
        result.setCheckSource(sourceType);
        result.setCheckReason(checkReason);
        result.setCheckTime(LocalDateTime.now());
        checkAlarmResultMapper.insert(result);
    }
}