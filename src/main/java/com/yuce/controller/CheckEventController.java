package com.yuce.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuce.common.ApiResponse;
import com.yuce.config.VideoProperties;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.QueryResultCheckRecord;
import com.yuce.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private CollectionServiceImpl collectionServiceImpl;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultService;

    @Autowired
    private FeatureElementServiceImpl featureElementServiceImpl;

    @Autowired
    private GeneralAlgorithmServiceImpl generalAlgorithmServiceImpl;

    @Autowired
    private VideoProperties videoProperties;


    // ⭐️从配置文件中获取输出目录和期望帧数
    private String outputDir = null;
    private int frameCount = 1;
    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @PostConstruct
    public void init() {
        outputDir = videoProperties.getOutputDir();
        frameCount = videoProperties.getFrameCount();
    }
    /**
     * 分页查询复核事件，支持多条件过滤
     */
    @GetMapping
    public ApiResponse<IPage<QueryResultCheckRecord>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) Integer checkFlag,
            @RequestParam(required = false) String roadName,
            @RequestParam(required = false) String directiondes
    ) {
        IPage<QueryResultCheckRecord> result = checkAlarmResultService.selectWithOriginaleField(
                pageNo, pageSize, startDate, endDate, eventType, content, checkFlag, roadName, directiondes
        );
        return ApiResponse.success(result);
    }


    //将查询结果数据写入数据表
    @PostMapping("/insert/byAlgorithm")
    public void insert(@RequestBody String responseText) {
        String alarmId = null;
        try {
            JSONArray jsonArray = JSONArray.parseArray(responseText);
            JSONObject rootJsonObject = JSONObject.parseObject(jsonArray.get(0).toString());

            String imageId = rootJsonObject.getString("image_id"); // 告警记录id+"_"+图片编号
            alarmId = imageId.split("_")[0];
            LocalDateTime receivedTime = LocalDateTime.parse(rootJsonObject.getString("received_time"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            int status = rootJsonObject.getIntValue("status");

            JSONArray dataJsonArray = JSONArray.parseArray(rootJsonObject.getString("data"));
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
                    checkAlarmProcess.setReceivedTime(receivedTime);
                    checkAlarmProcess.setStatus(status);
                    checkAlarmProcess.setCompletedTime(completedTime);
                    checkAlarmProcess.setType(type);
                    checkAlarmProcess.setName(name);
                    checkAlarmProcess.setScore(score);
                    checkAlarmProcess.setPoint1X(point1.getIntValue("x"));
                    checkAlarmProcess.setPoint1Y(point1.getIntValue("y"));
                    checkAlarmProcess.setPoint2X(point2.getIntValue("x"));
                    checkAlarmProcess.setPoint2Y(point2.getIntValue("y"));
                    checkAlarmProcess.setCreateTime(LocalDateTime.now());
                    checkAlarmProcess.setModifyTime(LocalDateTime.now());
                    checkAlarmProcessServiceImpl.save(checkAlarmProcess);
                }
            } else {
                // data 为空也需要保存记录
                CheckAlarmProcess checkAlarmProcess = new CheckAlarmProcess();
                checkAlarmProcess.setAlarmId(alarmId);
                checkAlarmProcess.setImageId(imageId);
                checkAlarmProcess.setReceivedTime(receivedTime);
                checkAlarmProcess.setStatus(status);
                checkAlarmProcess.setCompletedTime(LocalDateTime.now());
                checkAlarmProcess.setType(null);
                checkAlarmProcess.setName(null);
                checkAlarmProcess.setScore(null);
                checkAlarmProcess.setPoint1X(null);
                checkAlarmProcess.setPoint1Y(null);
                checkAlarmProcess.setPoint2X(null);
                checkAlarmProcess.setPoint2Y(null);
                checkAlarmProcess.setCreateTime(LocalDateTime.now());
                checkAlarmProcess.setModifyTime(LocalDateTime.now());
                checkAlarmProcessServiceImpl.save(checkAlarmProcess);
            }

            //查询算法处理的图片数量
            int count = checkAlarmProcessServiceImpl.countDistinctImageId(alarmId);//查询算法处理的图片数量

            if (count == frameCount) {
                log.info("算法返回结果：{}",alarmId);
                checkAlarmResultServiceImpl.checkResultByIou(alarmId, 0.2, 1);//更新算法结果:只要有一张图片核检成功即认为为正检；
                featureElementServiceImpl.featureElementDeal(alarmId);//处理特征要素
                collectionServiceImpl.collectionDeal(alarmId);//处理告警集
            }
        } catch (Exception e) {
            log.error("算法处理过程异常：{}", alarmId);
            generalAlgorithmServiceImpl.checkDeal(alarmId);
        }
    }
}