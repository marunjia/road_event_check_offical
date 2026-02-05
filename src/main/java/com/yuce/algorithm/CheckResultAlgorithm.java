package com.yuce.algorithm;

import com.yuce.config.VideoProperties;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmProcessMapper;
import com.yuce.mapper.CheckAlarmResultMapper;
import com.yuce.service.impl.CheckAlarmProcessServiceImpl;
import com.yuce.service.impl.CheckAlarmResultServiceImpl;
import com.yuce.util.FlagTagUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @ClassName CheckResultAlgorithm
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/11/28 17:08
 * @Version 1.0
 */

@Component
@Slf4j
public class CheckResultAlgorithm {

    int frameCount = 0;

    @Autowired
    private VideoProperties videoProperties;

    @PostConstruct
    public void init() {
        frameCount = videoProperties.getFrameCount();
    }

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private CheckAlarmResultMapper checkAlarmResultMapper;

    @Autowired
    private CheckAlarmProcessMapper checkAlarmProcessMapper;

    /**
     * @desc 预检结果处理_通用算法
     * @param record
     * @param checkFlag
     * @param reason
     */
    public void checkResultDealByGen(OriginalAlarmRecord record, int checkFlag, String reason) {
        long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        if (!isAlreadyDeal(tblId, alarmId, imagePath, videoPath)) {
            checkAlarmResultServiceImpl.insert(record, checkFlag, reason, FlagTagUtil.CHECK_ALGO_SOURCE_GENERAL, "no_item");
            log.info("告警记录完成算法初检 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
        }
    }

    /**
     * @desc 预检结果处理_自研算法
     * @param record
     */
    public void checkResultDealByAlgo(OriginalAlarmRecord record) {
        // 提取核心字段
        long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String eventType = record.getEventType();

        if (isAlreadyDeal(tblId, alarmId, imagePath, videoPath)) {
            return;
        }

        if(eventType.equals("停驶")){
            checkResultByIou(tblId, alarmId, imagePath, videoPath,0.2,1);
        }else if(eventType.equals("行人")){
            checkResultByImgNum(record, "person");
        }else if(eventType.equals("抛洒物")){
            checkResultByImgNum(record, "abandon");
        }else{
            log.info("告警记录类型不在算法检测范围：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | eventType:{}", tblId, alarmId, imagePath, videoPath, eventType);
        }
    }

    /**
     * @desc 告警记录是否已经过算法初检
     * @param tblId
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public boolean isAlreadyDeal(long tblId, String alarmId, String imagePath, String videoPath){
        if (checkAlarmResultServiceImpl.isExistByTblId(tblId)) {
            log.info("告警记录已完成算法初检,不再处理 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            return true;
        }else{
            return false;
        }
    }

    /**
     * @desc 根据IOU确定算法核检结果
     * @param tblId
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @param iouConfig
     * @param rightCheckNumConfig
     */
    public void checkResultByIou(long tblId, String alarmId, String imagePath, String videoPath, double iouConfig, int rightCheckNumConfig) {

        //抽帧图片逐个判定检验结果
        int rightCheckNum = 0;

        for (int i = 0; i < frameCount; i++) {
            String imageId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + alarmId + "_" + i + "_" + tblId;
            CheckAlarmProcess checkAlarmProcess = checkAlarmProcessMapper.getIouTop1ByKeyAndPic(alarmId, imagePath, videoPath, imageId);
            double iou = 0;
            if(checkAlarmProcess != null) {
                iou = checkAlarmProcess.getIou();
            }

            if (iou >= iouConfig) {
                log.info("正检判定：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | imageId:{} | iou:{} | rightCheckNum:{}", tblId, alarmId, imagePath, videoPath, imageId, iou, rightCheckNum);
                rightCheckNum++;
            }else{
                log.info("正检判定：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | imageId:{} | iou:{} | rightCheckNum:{}", tblId, alarmId, imagePath, videoPath, imageId, iou, rightCheckNum);
            }
        }

        int checkFlag = 0;
        String checkReason = "";

        if (rightCheckNum >= rightCheckNumConfig) {
            checkFlag = 1;
        } else {
            checkFlag = 2;
            checkReason = "自研算法正检图片数量未达阈值";
        }

        String checkName = null;
        CheckAlarmProcess checkAlarmProcess = checkAlarmProcessMapper.getIouTop1ByKey(alarmId,imagePath,videoPath);
        if(checkAlarmProcess != null){
            checkName = checkAlarmProcess.getName();
        }

        CheckAlarmResult checkAlarmResult = new CheckAlarmResult();
        checkAlarmResult.setTblId(tblId);
        checkAlarmResult.setAlarmId(alarmId);
        checkAlarmResult.setCheckFlag(checkFlag);
        checkAlarmResult.setImagePath(imagePath);
        checkAlarmResult.setVideoPath(videoPath);
        checkAlarmResult.setCheckName(checkName);
        checkAlarmResult.setCheckReason(checkReason);
        checkAlarmResult.setCheckSource(FlagTagUtil.CHECK_ALGO_SOURCE_DEVELOP);
        checkAlarmResult.setCheckTime(LocalDateTime.now());

        CheckAlarmResult existing = checkAlarmResultServiceImpl.getResultByTblId(tblId);
        if (existing != null) {
            // 更新已有记录
            log.info("检测结果已存在，更新检测结果：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            checkAlarmResult.setId(existing.getId());
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResultMapper.updateById(checkAlarmResult);
        } else {
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResult.setCreateTime(LocalDateTime.now());
            checkAlarmResultMapper.insert(checkAlarmResult);
            log.info("算法初检完成，插入检测结果：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
        }
    }

    /**
     * @param record
     * @param type
     * @desc 根据图片数量确定算法核检结果
     */
    public void checkResultByImgNum(OriginalAlarmRecord record, String type) {

        long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        CheckAlarmResult existing = checkAlarmResultMapper.getResultByTblId(record.getTblId());
        if(existing != null){
            log.info("告警记录已经检测，不再处理：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
        }

        //查询算法检验结果列表
        List<CheckAlarmProcess> list = checkAlarmProcessMapper.getListByKeyAndType(alarmId, imagePath, videoPath, type);
        int checkFlag = 0;
        String checkReason = "";

        String checkName = null;
        if (list.size() > 0) {
            checkFlag = 1;
            checkName = list.get(0).getName();//匹配度最高的物体名称
        } else {
            checkFlag = 2;
            checkReason = "自研算法未检测到符合阈值的匹配目标";
        }

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
            checkAlarmResult.setTblId(tblId);
            checkAlarmResult.setAlarmId(alarmId);
            checkAlarmResult.setImagePath(imagePath);
            checkAlarmResult.setVideoPath(videoPath);
            checkAlarmResult.setCheckFlag(checkFlag);
            checkAlarmResult.setCheckName(checkName);
            checkAlarmResult.setCheckSource(FlagTagUtil.CHECK_ALGO_SOURCE_DEVELOP);
            checkAlarmResult.setCheckReason(checkReason);
            checkAlarmResult.setCheckTime(LocalDateTime.now());
            checkAlarmResult.setCreateTime(LocalDateTime.now());
            checkAlarmResult.setUpdateTime(LocalDateTime.now());
            checkAlarmResultMapper.insert(checkAlarmResult);
        }
    }
}