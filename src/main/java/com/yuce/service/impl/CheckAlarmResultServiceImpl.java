package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.config.VideoProperties;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.QueryResultCheckRecord;
import com.yuce.mapper.CheckAlarmProcessMapper;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.service.CheckAlarmResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private VideoProperties videoProperties;

    int frameCount = 0;
    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @PostConstruct
    public void init() {
        frameCount = videoProperties.getFrameCount();
    }

    @Override
    public IPage<QueryResultCheckRecord> selectWithOriginaleField(int pageNo, int pageSize,
                                                                  LocalDate startDate, LocalDate endDate,
                                                                  String eventType, String content,
                                                                  Integer checkFlag, String roadName, String directiondes) {
        Page<QueryResultCheckRecord> page = new Page<>(pageNo, pageSize);

        QueryWrapper<QueryResultCheckRecord> query = new QueryWrapper<>();

        if (startDate != null) {
            query.ge("alarm_time", startDate.atStartOfDay());
        }
        if (endDate != null) {
            query.le("alarm_time", endDate.atTime(23, 59, 59));
        }
        if (StringUtils.hasText(eventType)) {
            query.eq("event_type", eventType);
        }
        if (StringUtils.hasText(content)) {
            query.like("content", content);
        }
        if (checkFlag != null) {
            // check_flag 是来自关联表的字段，MyBatis-Plus无法自动处理，需要用 Wrapper 传递这个条件
            query.eq("a.check_flag", checkFlag);
        }
        if (StringUtils.hasText(roadName)) {
            query.like("r.short_name", roadName);
        }
        if (StringUtils.hasText(directiondes)) {
            query.like("o.directiondes", directiondes);
        }

        query.orderByDesc("o.alarm_time");

        // 调用 Mapper 方法执行分页查询
        IPage<QueryResultCheckRecord> result = checkAlarmResultMapper.selectWithJoin(page, query);

        return result;
    }

    /**
     * @desc 根据alarmId查询告警记录是否存在
     * @param alarmId
     * @return
     */
    public boolean existsJudgeByAlarmId(String alarmId) {
        QueryWrapper<CheckAlarmResult> query = new QueryWrapper<>();
        query.eq("alarm_id", alarmId);
        return checkAlarmResultMapper.exists(query);
    }

    /**
     * @desc 根据alarmId查询检验结果
     * @param alarmId
     * @return
     */
    public CheckAlarmResult getByAlarmId(String alarmId) {
        QueryWrapper<CheckAlarmResult> query = new QueryWrapper<>();
        query.eq("alarm_id", alarmId);
        return checkAlarmResultMapper.selectOne(query);
    }

    /**
     * @desc 根据alarmIdList查询检验结果
     * @param alarmIdList
     * @return
     */
    public List<CheckAlarmResult> getByAlarmIdList(List<String> alarmIdList) {
        QueryWrapper<CheckAlarmResult> query = new QueryWrapper<>();
        query.in("alarm_id", alarmIdList);
        query.orderByDesc("check_time");
        return checkAlarmResultMapper.selectList(query);
    }

    /**
     * @desc 根据IOU确定算法核检结果
     * @param alarmId
     */
    public void checkResultByIou(String alarmId, double iouConfig, int rightCheckNumConfig) {

        //查询算法检验结果列表
        QueryWrapper<CheckAlarmProcess> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("alarm_id", alarmId);

        //抽帧图片逐个判定检验结果
        int rightCheckNum = 0;
        for (int i = 0; i < frameCount; i++) {
            String imageId = alarmId + "_" + i;
            checkWrapper.eq("image_id", imageId);
            double iou = checkAlarmProcessServiceImpl.getIouTop1ByAlarmIdAndImgId(alarmId, imageId).getIou();
            if (iou >= iouConfig) {
                rightCheckNum++;
            }
        }

        int checkFlag = 0;
        if(rightCheckNum >= rightCheckNumConfig) {
            checkFlag = 1;
        }else {
            checkFlag = 2;
        }

        CheckAlarmResult existing = getByAlarmId(alarmId);
        if (existing != null) {
            // 更新已有记录
            existing.setCheckFlag(checkFlag);
            existing.setUpdateTime(LocalDateTime.now());
            existing.setCheckTime(LocalDateTime.now());
            checkAlarmResultMapper.updateById(existing);
        } else {
            // 新增记录
            CheckAlarmResult checkAlarmResult = new CheckAlarmResult();
            checkAlarmResult.setAlarmId(alarmId);
            checkAlarmResult.setCheckFlag(checkFlag);
            checkAlarmResult.setCheckTime(LocalDateTime.now());
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResult.setCreateTime(LocalDateTime.now());
            checkAlarmResultMapper.insert(checkAlarmResult);
        }
    }

    /**
     * @desc 根据图片数量确定算法核检结果
     * @param alarmId
     */
    public void checkResultByImgNum(String alarmId, String type) {

        //查询算法检验结果列表
        List<CheckAlarmProcess> list = checkAlarmProcessServiceImpl.getListByAlarmIdAndType(alarmId,type);
        int checkFlag = 0;
        if(list.size()>0) {
            checkFlag = 1;
        }else {
            checkFlag = 2;
        }

        CheckAlarmResult existing = getByAlarmId(alarmId);
        if (existing != null) {
            // 更新已有记录
            existing.setCheckFlag(checkFlag);
            existing.setUpdateTime(LocalDateTime.now());
            existing.setCheckTime(LocalDateTime.now());
            checkAlarmResultMapper.updateById(existing);
        } else {
            // 新增记录
            CheckAlarmResult checkAlarmResult = new CheckAlarmResult();
            checkAlarmResult.setAlarmId(alarmId);
            checkAlarmResult.setCheckFlag(checkFlag);
            checkAlarmResult.setCheckTime(LocalDateTime.now());
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResult.setCreateTime(LocalDateTime.now());
            checkAlarmResultMapper.insert(checkAlarmResult);
        }
    }
}