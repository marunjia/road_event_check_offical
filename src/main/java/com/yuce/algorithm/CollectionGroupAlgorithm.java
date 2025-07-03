package com.yuce.algorithm;

import com.yuce.entity.*;
import com.yuce.service.impl.*;
import com.yuce.util.IouUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * @ClassName CollectionAlgorithm
 * @Description 告警组处理逻辑
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/29 15:07
 * @Version 1.0
 */

@Component
@Slf4j
public class CollectionGroupAlgorithm {

    @Autowired
    private AlarmCollectionServiceImpl alarmCollectionServiceImpl;

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    private CollectionGroupServiceImpl collectionGroupServiceImpl;

    @Autowired
    private ExtractWindowServiceImpl extractWindowServiceImpl;

    @Autowired
    private ExtractImageServiceImpl extractImageServiceImpl;

    /**
     * @desc 告警组处理逻辑
     * @param record
     * @return
     */
    public void groupDeal(OriginalAlarmRecord record){
        /**
         * 获取告警记录原始告警信息
         */
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String eventType = record.getEventType();
        LocalDateTime alarmTime = record.getAlarmTime();

        //判断告警记录是否已分组
        if(collectionGroupServiceImpl.getGroupByKey(alarmId, imagePath, videoPath) != null) {
            log.info("告警记录已分组无需再次处理，告警记录：alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{}", alarmId, imagePath, videoPath, eventType, alarmTime);
            return;
        }

        /**
         * 获取上条告警记录
         *  1、先获取当前告警记录tblId
         *  2、根据tblId查询当前告警记录归属告警集
         *  3、查询告警集关联告警记录的tblId
         *  4、根据tblId查询原始告警记录中告警时间小于当前记录告警时间的第一条记录
         */
        long tblId = originalAlarmServiceImpl.getRecordByKey(alarmId,imagePath,videoPath).getTblId();
        AlarmCollection alarmCollection = alarmCollectionServiceImpl.getCollectionByTblId(tblId);
        if(alarmCollection == null) {
            log.info("告警记录无对应告警集请检查，告警记录：tblId->{},alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{}", tblId, alarmId, imagePath, videoPath, eventType, alarmTime);
            return;
        }
        String collectionId = alarmCollection.getCollectionId();
        log.info("告警记录进行group操作:tblId->{},alarmId->{},alarmTime->{},eventType->{},imagePath->{},videoPath->{};告警集信息：collectionId->{}, relatedIdList->{}", tblId, alarmId, alarmTime, eventType, imagePath, videoPath,alarmCollection.getCollectionId(), alarmCollection.getRelatedIdList());
        //根据归属告警集查询上一条告警记录
        List<String> tblIdList = Arrays.asList(alarmCollection.getRelatedIdList().split(","));//获取当前record对应告警集关联的所有告警记录id；
        OriginalAlarmRecord leadRecord = originalAlarmServiceImpl.getRecordByTblIdListAndTime(tblIdList,alarmTime);

        /**
         * 1、获取当前告警记录提框信息
         * 2、获取当前告警记录检测物品信息
         * 3、获取当前告警记录抠图信息
         * 4、获取上条告警记录提框信息
         * 5、获取上条告警记录抠图信息
         * 6、获取上条告警记录检测物品信息
         */

        Integer point1X = null;
        Integer point1Y = null;
        Integer point2X = null;
        Integer point2Y = null;
        String checkName = null;
        String croppedImageUrl = null;

        ExtractWindowRecord extractWindowRecord = extractWindowServiceImpl.getExtractWindow(alarmId,imagePath,videoPath);
        log.info("告警记录：tblId->{},alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{};提框结果是否为空:{}", tblId, alarmId, imagePath, videoPath, eventType, alarmTime, null != extractWindowRecord);
        CheckAlarmProcess checkAlarmProcess = checkAlarmProcessServiceImpl.getIouTop1ByKey(alarmId,imagePath,videoPath);
        log.info("告警记录：tblId->{},alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{};检测结果是否为空:{}", tblId, alarmId, imagePath, videoPath, eventType, alarmTime, null != checkAlarmProcess);
        ExtractImageRecord extractImageRecord = extractImageServiceImpl.getImageByKey(alarmId,imagePath,videoPath);
        log.info("告警记录：tblId->{},alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{};抠图结果是否为空:{}", tblId, alarmId, imagePath, videoPath, eventType, alarmTime, null != extractImageRecord);


        /**
         * 原始告警记录属性赋值
         */
        if(extractWindowRecord != null){
            point1X = extractWindowRecord.getPoint1X();
            point1Y = extractWindowRecord.getPoint1Y();
            point2X = extractWindowRecord.getPoint1X();
            point2Y = extractWindowRecord.getPoint1Y();
        }

        if(checkAlarmProcess != null){
            checkName = checkAlarmProcess.getName();
        }

        if(extractImageRecord != null){
            croppedImageUrl = extractImageRecord.getCroppedImageUrl();
        }

        String groupId = alarmCollection.getCollectionId() + "_" + System.currentTimeMillis();

        if(leadRecord == null) {
            log.info("告警记录为告警集第1条记录，无上条记录信息，告警记录：tblId->{},alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{}", tblId, alarmId, imagePath, videoPath, eventType, alarmTime);
            insertRecord(collectionId, groupId, croppedImageUrl,eventType,alarmId,imagePath,videoPath,eventType,alarmTime,croppedImageUrl,checkName,point1X,point1Y,point2X,point2Y,0,0 );
        }else{
            Long leadTblId = leadRecord.getTblId();
            String leadAlarmId = leadRecord.getId();
            String leadImagePath = leadRecord.getImagePath();
            String leadVideoPath = leadRecord.getVideoPath();
            LocalDateTime leadAlarmTime = leadRecord.getAlarmTime();
            log.info("告警记录：tblId->{},alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{};上条告警记录:tblId->{},alarmId->{},imagePath->{},videoPath->{},alarmTime->{}", tblId, alarmId, imagePath, videoPath, eventType, alarmTime, leadTblId, leadAlarmId, leadImagePath, leadVideoPath, leadAlarmTime);

            ExtractWindowRecord leadExtractWindowRecord = extractWindowServiceImpl.getExtractWindow(leadAlarmId,leadImagePath,leadVideoPath);
            log.info("告警记录：tblId->{},alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{};上条告警记录提框结果是否为空:{}", tblId, alarmId, imagePath, videoPath, eventType, alarmTime, null == leadExtractWindowRecord);

            CheckAlarmProcess leadCheckAlarmProcess = checkAlarmProcessServiceImpl.getIouTop1ByKey(leadAlarmId,leadImagePath,leadVideoPath);
            log.info("告警记录：tblId->{},alarmId->{},imagePath->{},videoPath->{},eventType->{},alarmTime->{};上条告警记录检测结果是否为空:{}", tblId, alarmId, imagePath, videoPath, eventType, alarmTime, null == leadCheckAlarmProcess);

            if(extractWindowRecord != null && checkAlarmProcess != null && extractImageRecord != null && leadExtractWindowRecord != null && leadCheckAlarmProcess != null) {
                Integer leadPoint1X = leadExtractWindowRecord.getPoint1X();
                Integer leadPoint1Y = leadExtractWindowRecord.getPoint1Y();
                Integer leadPoint2X = leadExtractWindowRecord.getPoint2X();
                Integer leadPoint2Y = leadExtractWindowRecord.getPoint2Y();
                String leadCheckName = leadCheckAlarmProcess.getName();
                long minutes = Duration.between(alarmTime, leadAlarmTime).toMinutes();
                double iou = 0;
                if(point1X != null && point1Y != null && point2X != null && point2Y != null && leadPoint1X != null && leadPoint1Y != null && leadPoint2X != null && leadPoint2Y != null) {
                    iou = IouUtil.calculateIoU(point1X,point1Y,point2X,point2Y,leadPoint1X,leadPoint1Y,leadPoint2X,leadPoint2Y);
                }
                //满足同组条件
                log.info("告警组比对记录tblId->({},{}), eventType->{}, iou->{}, elementName->({},{})",tblId,leadTblId,eventType,iou,checkName,leadCheckName);
                if((eventType.equals("停驶") && checkName.equals(leadCheckName) && iou>=0.7)
                        || (eventType.equals("行人") && checkName.equals(leadCheckName))
                        || (eventType.equals("抛洒物") && minutes <= 30 && checkName.equals(leadCheckName))){
                    //加入告警组
                   groupId = collectionGroupServiceImpl.getGroupByKey(leadAlarmId, leadImagePath, leadVideoPath).getGroupId();
                }
                insertRecord(collectionId, groupId, croppedImageUrl,eventType,alarmId,imagePath,videoPath,eventType,alarmTime,croppedImageUrl,checkName,point1X,point1Y,point2X,point2Y,0,0 );
            }else{
                log.info("数据存在空值，新建告警组：tblId->({},{})", tblId, leadImagePath);
                insertRecord(collectionId, groupId, croppedImageUrl,eventType,alarmId,imagePath,videoPath,eventType,alarmTime,croppedImageUrl,checkName,point1X,point1Y,point2X,point2Y,0,0 );
            }
        }
    }

    /**
     * @desc 插入分组记录
     * @param collectionId
     * @param groupId
     * @param groupImageUrl
     * @param groupEventType
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @param eventType
     * @param alarmTime
     * @param extractImageUrl
     * @param alarmElement
     * @param point1x
     * @param point1y
     * @param point2x
     * @param point2y
     * @param iou
     * @param minutes
     */
    public void insertRecord(
            String collectionId,
            String groupId,
            String groupImageUrl,
            String groupEventType,
            String alarmId,
            String imagePath,
            String videoPath,
            String eventType,
            LocalDateTime alarmTime,
            String extractImageUrl,
            String alarmElement,
            Integer point1x,
            Integer point1y,
            Integer point2x,
            Integer point2y,
            double iou,
            double minutes
    ) {
        CollectionGroupRecord record = new CollectionGroupRecord();
        record.setCollectionId(collectionId);
        record.setGroupId(groupId);
        record.setGroupImageUrl(groupImageUrl);
        record.setGroupEventType(groupEventType);

        record.setAlarmId(alarmId);
        record.setImagePath(imagePath);
        record.setVideoPath(videoPath);
        record.setEventType(eventType);
        record.setAlarmTime(alarmTime);
        record.setExtractImageUrl(extractImageUrl);
        record.setAlarmElement(alarmElement);
        record.setPoint1X(point1x);
        record.setPoint1Y(point1y);
        record.setPoint2X(point2x);
        record.setPoint2Y(point2y);
        record.setLeadCompareIou(iou);
        record.setLeadCompareMinute(minutes);
        collectionGroupServiceImpl.insertGroup(record);
    }
}