package com.yuce.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuce.algorithm.*;
import com.yuce.common.ApiResponse;
import com.yuce.config.VideoProperties;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.entity.QueryResultCheckRecord;
import com.yuce.service.impl.*;
import com.yuce.util.IouUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CheckEventController
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/16 14:44
 * @Version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/checkEvents")
public class CheckEventController {

    @Autowired
    private CheckAlarmProcessServiceImpl checkAlarmProcessServiceImpl;

    @Autowired
    private AlarmCollectionAlgorithm alarmCollectionAlgorithm;

    @Autowired
    private FeatureElementAlgorithm featureElementAlgorithm;

    @Autowired
    private GeneralAlgorithm generalAlgorithm;

    @Autowired
    private ExtractWindowServiceImpl extractWindowServiceImpl;

    @Autowired
    private VideoProperties videoProperties;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private CollectionGroupAlgorithm collectionGroupAlgorithm;

    private int frameCount = 1;

    @PostConstruct
    public void init() {
        frameCount = videoProperties.getFrameCount();
    }
    /**
     * 分页查询复核事件，支持多条件过滤
     */
    @GetMapping
    public ApiResponse<IPage<QueryResultCheckRecord>> list(
            @RequestParam(required = false) String alarmId,
            @RequestParam(required = false) String startDate,//yyyy-MM-dd格式
            @RequestParam(required = false) String endDate,//yyyy-MM-dd格式
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String roadId,
            @RequestParam(required = false) String directionDes,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Integer dealFlag,
            @RequestParam(required = false) Integer checkFlag,
            @RequestParam(required = false) Integer disposalAdvice,
            @RequestParam(required = false) String adviceReason,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        IPage<QueryResultCheckRecord> result = originalAlarmServiceImpl.selectWithOriginaleField(alarmId, startDate, endDate, deviceName, roadId, directionDes, eventType, dealFlag, checkFlag, disposalAdvice, adviceReason , pageNo, pageSize);
        return ApiResponse.success(result);
    }


    //将查询结果数据写入数据表
    @PostMapping("/insert/byAlgorithm")
    public void insert(@RequestBody String responseText) {
        log.info("抛洒物算法请求参数: param->{}",responseText);

        Long tblId = null;
        try {
            JSONArray jsonArray = JSONArray.parseArray(responseText);
            JSONObject rootJsonObject = JSONObject.parseObject(jsonArray.get(0).toString());

            //imageId格式:日期_alarmId_sortNo_tblId;
            String imageId = rootJsonObject.getString("image_id");

            tblId = Long.valueOf(imageId.split("_")[3]);//告警记录唯一id
            OriginalAlarmRecord record = originalAlarmServiceImpl.getById(tblId);
            String alarmId = record.getId();
            String imagePath = record.getImagePath();
            String videoPath = record.getVideoPath();

            LocalDateTime receivedTime = LocalDateTime.parse(rootJsonObject.getString("received_time"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            int status = rootJsonObject.getIntValue("status");

            JSONArray dataJsonArray = JSONArray.parseArray(rootJsonObject.getString("data"));

            ExtractWindowRecord extractPointRecord = extractWindowServiceImpl.getExtractWindow(alarmId,imagePath,videoPath);

            int extractPoint1x = extractPointRecord.getPoint1X();
            int extractPoint1y = extractPointRecord.getPoint1Y();
            int extractPoint2x = extractPointRecord.getPoint2X();
            int extractPoint2y = extractPointRecord.getPoint2Y();

            List<CheckAlarmProcess> checkProcessList = new ArrayList<>();
            if (dataJsonArray.size() > 0) {
                for (int i = 0; i < dataJsonArray.size(); i++) {
                    JSONObject subJsonObject = dataJsonArray.getJSONObject(i);

                    LocalDateTime completedTime = LocalDateTime.parse(rootJsonObject.getString("completed_time"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String type = subJsonObject.getString("type");
                    String name = subJsonObject.getString("name");
                    BigDecimal score = subJsonObject.getBigDecimal("score");

                    JSONArray pointsJsonArray = JSONArray.parseArray(subJsonObject.getString("points"));
                    JSONObject point1 = pointsJsonArray.getJSONObject(0);
                    JSONObject point2 = pointsJsonArray.getJSONObject(1);

                    CheckAlarmProcess checkAlarmProcess = new CheckAlarmProcess();
                    checkAlarmProcess.setAlarmId(alarmId);
                    checkAlarmProcess.setImageId(imageId);
                    checkAlarmProcess.setImagePath(imagePath);
                    checkAlarmProcess.setVideoPath(videoPath);
                    checkAlarmProcess.setReceivedTime(receivedTime);
                    checkAlarmProcess.setStatus(status);
                    checkAlarmProcess.setCompletedTime(completedTime);
                    checkAlarmProcess.setType(type);
                    checkAlarmProcess.setName(name);
                    checkAlarmProcess.setScore(score);

                    int checkPoint1x = point1.getIntValue("x");
                    int checkPoint1y = point1.getIntValue("y");
                    int checkPoint2x = point2.getIntValue("x");
                    int checkPoint2y = point2.getIntValue("y");
                    checkAlarmProcess.setIou(IouUtil.calculateIoU(checkPoint1x, checkPoint1y,checkPoint2x,checkPoint2y,extractPoint1x,extractPoint1y,extractPoint2x,extractPoint2y));

                    checkAlarmProcess.setPoint1X(point1.getIntValue("x"));
                    checkAlarmProcess.setPoint1Y(point1.getIntValue("y"));
                    checkAlarmProcess.setPoint2X(point2.getIntValue("x"));
                    checkAlarmProcess.setPoint2Y(point2.getIntValue("y"));

                    checkAlarmProcess.setCreateTime(LocalDateTime.now());
                    checkAlarmProcess.setModifyTime(LocalDateTime.now());
                    checkProcessList.add(checkAlarmProcess);
                }
            } else {
                // data 为空也需要保存记录
                CheckAlarmProcess checkAlarmProcess = new CheckAlarmProcess();
                checkAlarmProcess.setAlarmId(alarmId);
                checkAlarmProcess.setImageId(imageId);
                checkAlarmProcess.setImagePath(imagePath);
                checkAlarmProcess.setVideoPath(videoPath);
                checkAlarmProcess.setReceivedTime(receivedTime);
                checkAlarmProcess.setStatus(status);
                checkAlarmProcess.setCompletedTime(LocalDateTime.now());
                checkAlarmProcess.setType(null);
                checkAlarmProcess.setName(null);
                checkAlarmProcess.setScore(null);
                checkAlarmProcess.setIou(0.0000);
                checkAlarmProcess.setPoint1X(null);
                checkAlarmProcess.setPoint1Y(null);
                checkAlarmProcess.setPoint2X(null);
                checkAlarmProcess.setPoint2Y(null);
                checkAlarmProcess.setCreateTime(LocalDateTime.now());
                checkAlarmProcess.setModifyTime(LocalDateTime.now());
                checkProcessList.add(checkAlarmProcess);
            }
            checkAlarmProcessServiceImpl.saveBatch(checkProcessList);

            //查询算法处理的图片数量
            int count = checkAlarmProcessServiceImpl.countDistinctImageId(alarmId, imagePath, videoPath);//根据联合主键查询已检测的图片数量
            log.info("抛洒物回调：alarmId->{}, imagePath->{}, videoPath->{}, checkImageNum->{}", alarmId, imagePath, videoPath, count);
            if (count == frameCount) {
                log.info("抛洒物算法处理完成：alarmId->{}, imagePath->{}, videoPath->{}", alarmId, imagePath, videoPath);
                checkAlarmResultServiceImpl.checkResultByIou(record, 0.2, 1);//更新算法结果:只要有一张图片核检成功即认为为正检；
                featureElementAlgorithm.featureElementDealByAlgo(record);//处理特征要素
                alarmCollectionAlgorithm.collectionDeal(record);//处理告警集
                collectionGroupAlgorithm.groupDeal(record);//告警组判定
            }
        } catch (Exception e) {
            log.error("算法处理过程异常：{}", tblId);
            generalAlgorithm.checkDeal(originalAlarmServiceImpl.getById(tblId), "算法处理失败");
        }
    }
}