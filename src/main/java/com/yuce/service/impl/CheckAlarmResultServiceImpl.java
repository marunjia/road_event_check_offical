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

    int frameCount = 0;

    @Autowired
    private VideoProperties videoProperties;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private CheckAlarmProcessMapper checkAlarmProcessMapper;


    @PostConstruct
    public void init() {
        frameCount = videoProperties.getFrameCount();
    }

    /**
     * @param alarmId
     * @return
     * @desc 根据唯一参数组数查询检测结果
     */
    public CheckAlarmResult getResultByKey(String alarmId, String imagePath, String videoPath) {
        QueryWrapper<CheckAlarmResult> query = new QueryWrapper<>();
        query.eq("alarm_id", alarmId);
        query.eq("image_path", imagePath);
        query.eq("video_path", videoPath);
        return checkAlarmResultMapper.selectOne(query);
    }

    /**
     * @param alarmIdList
     * @return
     * @desc 根据alarmIdList查询检验结果
     */
    public List<CheckAlarmResult> getByAlarmIdList(List<String> alarmIdList) {
        QueryWrapper<CheckAlarmResult> query = new QueryWrapper<>();
        query.in("alarm_id", alarmIdList);
        query.orderByDesc("check_time");
        return checkAlarmResultMapper.selectList(query);
    }

    /**
     * @param record
     * @param iouConfig
     * @param rightCheckNumConfig
     * @desc 根据IOU确定算法核检结果
     */
    public void checkResultByIou(OriginalAlarmRecord record, double iouConfig, int rightCheckNumConfig) {

        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        //抽帧图片逐个判定检验结果
        int rightCheckNum = 0;
        for (int i = 0; i < frameCount; i++) {
            String imageId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + record.getId() + "_" + i + "_" + record.getTblId();
            CheckAlarmProcess checkAlarmProcess = checkAlarmProcessMapper.getIouTop1ByKeyAndPic(alarmId, imagePath, videoPath, imageId);
            double iou = 0;
            if(checkAlarmProcess != null) {
                iou = checkAlarmProcess.getIou();
            }

            if (iou >= iouConfig) {
                log.info("正检判定：alarmId->{}, imagePath->{}, videoPath->{}, imageId->{}, iou->{}, rightCheckNum->{}", alarmId, imagePath, videoPath, imageId, iou, rightCheckNum);
                rightCheckNum++;
            }else{
                log.info("误检判定：alarmId->{}, imagePath->{}, videoPath->{}, imageId->{}, iou->{}, rightCheckNum->{}", alarmId, imagePath, videoPath, imageId, iou, rightCheckNum);
            }
        }

        int checkFlag = 0;
        if (rightCheckNum >= rightCheckNumConfig) {
            checkFlag = 1;
        } else {
            checkFlag = 2;
        }

        String checkName = null;
        CheckAlarmProcess checkAlarmProcess = checkAlarmProcessMapper.getIouTop1ByKey(alarmId,imagePath,videoPath);
        if(checkAlarmProcess != null){
            checkName = checkAlarmProcess.getName();
        }

        CheckAlarmResult checkAlarmResult = new CheckAlarmResult();
        checkAlarmResult.setAlarmId(alarmId);
        checkAlarmResult.setCheckFlag(checkFlag);
        checkAlarmResult.setImagePath(imagePath);
        checkAlarmResult.setVideoPath(videoPath);
        checkAlarmResult.setCheckName(checkName);
        checkAlarmResult.setCheckTime(LocalDateTime.now());

        CheckAlarmResult existing = getResultByKey(alarmId, imagePath, videoPath);
        if (existing != null) {
            // 更新已有记录
            log.info("检测结果已存在，更新检测结果：alarmId->{}, imagePath->{}, videoPath->{}", alarmId, imagePath, videoPath);
            checkAlarmResult.setId(existing.getId());
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResultMapper.updateById(checkAlarmResult);
        } else {
            log.info("未进行检测判定，插入检测结果：alarmId->{}, imagePath->{}, videoPath->{}", alarmId, imagePath, videoPath);
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResult.setCreateTime(LocalDateTime.now());
            checkAlarmResultMapper.insert(checkAlarmResult);
        }
        log.info("iou检测完成：记录id->{}, alarmId->{}, checkFlag->{}", tblId, alarmId, checkFlag);
    }

    /**
     * @param record
     * @param type
     * @desc 根据图片数量确定算法核检结果
     */
    public void checkResultByImgNum(OriginalAlarmRecord record, String type) {

        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        //查询算法检验结果列表
        List<CheckAlarmProcess> list = checkAlarmProcessMapper.getListByKeyAndType(alarmId, imagePath, videoPath, type);
        int checkFlag = 0;

        String checkName = null;
        if (list.size() > 0) {
            checkFlag = 1;
            checkName = list.get(0).getName();//匹配度最高的物体名称
        } else {
            checkFlag = 2;
        }

        CheckAlarmResult existing = checkAlarmResultMapper.getResultByKey(alarmId, imagePath, videoPath);
        if (existing != null) {
            // 更新已有记录
            existing.setCheckFlag(checkFlag);
            existing.setCheckName(checkName);
            existing.setUpdateTime(LocalDateTime.now());
            existing.setCheckTime(LocalDateTime.now());
            checkAlarmResultMapper.updateById(existing);
        } else {
            // 新增记录
            CheckAlarmResult checkAlarmResult = new CheckAlarmResult();
            checkAlarmResult.setAlarmId(alarmId);
            checkAlarmResult.setImagePath(imagePath);
            checkAlarmResult.setVideoPath(videoPath);
            checkAlarmResult.setCheckFlag(checkFlag);
            checkAlarmResult.setCheckName(checkName);
            checkAlarmResult.setCheckTime(LocalDateTime.now());
            checkAlarmResult.setCreateTime(LocalDateTime.now());
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResultMapper.insert(checkAlarmResult);
        }
    }

    /**
     * @param id
     * @desc 获取最近50条误检数据的设备id
     */
    public List<String> getRecentFalseCheckList(Long id) {
        return checkAlarmResultMapper.getRecentFalseCheckList(id);
    }
}