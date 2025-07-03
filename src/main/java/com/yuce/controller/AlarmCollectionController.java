package com.yuce.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuce.common.ApiResponse;
import com.yuce.entity.AlarmCollection;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.impl.AlarmCollectionServiceImpl;
import com.yuce.service.impl.CheckAlarmResultServiceImpl;
import com.yuce.service.impl.OriginalAlarmServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @ClassName AlarmCollectionController
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/16 14:43
 * @Version 1.0
 */

@Slf4j
@RestController
@RequestMapping("/alarmCollections")
public class AlarmCollectionController {

    @Autowired
    private AlarmCollectionServiceImpl alarmCollectionServiceImpl;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    /**
     * @desc 高信专用接口_分页
     * @param roadId
     * @param milestoneStart
     * @param milestoneEnd
     * @param startTime
     * @param endTime
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/gxVersion")
    public ApiResponse list(
            @RequestParam(required = false) String roadId,
            @RequestParam(required = false) Integer milestoneStart,
            @RequestParam(required = false) Integer milestoneEnd,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        QueryWrapper<AlarmCollection> query = new QueryWrapper<>();

        if (roadId != null && !roadId.isEmpty()) {
            query.like("road_id", roadId);
        }

        if (milestoneStart != null) {
            query.ge("milestone", milestoneStart);
        }

        if (milestoneEnd != null) {
            query.le("milestone", milestoneEnd);
        }

        if (startTime != null) {
            query.ge("latest_alarm_time", startTime);
        }

        if (endTime != null) {
            query.le("latest_alarm_time", endTime);
        }

        query.orderByDesc("create_time");

        // 分页查询
        Page<AlarmCollection> page = new Page<>(pageNum, pageSize);
        IPage<AlarmCollection> resultPage = alarmCollectionServiceImpl.page(page, query);

        List<AlarmCollection> resultList = resultPage.getRecords();
        JSONArray jsonArray = new JSONArray();

        for (AlarmCollection alarmCollection : resultList) {
            JSONObject jsonObject = new JSONObject();

            String[] relatedIds = alarmCollection.getRelatedIdList() != null
                    ? alarmCollection.getRelatedIdList().split(",")
                    : new String[0];

            OriginalAlarmRecord earliestRecord = (relatedIds.length > 0)
                    ? originalAlarmServiceImpl.getById(relatedIds[0])
                    : null;

            jsonObject.put("id", alarmCollection.getCollectionId());
            jsonObject.put("inferEventTypeId", null);
            jsonObject.put("inferEventTypeName", alarmCollection.getEventType());
            jsonObject.put("suggestion", alarmCollection.getDisposalAdvice());
            jsonObject.put("preCheckTag", null);
            jsonObject.put("confirmStatus", null);
            jsonObject.put("eventId", earliestRecord != null ? earliestRecord.getEventId() : null);
            jsonObject.put("createTime", alarmCollection.getCreateTime());
            jsonObject.put("updateTime", alarmCollection.getModifyTime());
            jsonObject.put("alarmNum", alarmCollection.getRelatedAlarmNum());

            if (relatedIds.length > 0) {
                QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("id", Arrays.asList(relatedIds));
                jsonObject.put("alarmList", originalAlarmServiceImpl.list(queryWrapper));
            } else {
                jsonObject.put("alarmList", new JSONArray());
            }

            jsonObject.put("roadId", alarmCollection.getRoadId());
            jsonObject.put("alarmTime", earliestRecord != null ? earliestRecord.getAlarmTime() : null);

            if (earliestRecord != null && earliestRecord.getContent() != null) {
                String[] parts = earliestRecord.getContent().split(" ");
                jsonObject.put("alarmPosition", parts.length >= 3 ? parts[0] + " " + parts[1] + " " + parts[2] : earliestRecord.getContent());
                jsonObject.put("direction", earliestRecord.getDirection());
                jsonObject.put("milestoneMeter", earliestRecord.getMilestone());
                jsonObject.put("directiondes", earliestRecord.getDirectionDes());
                jsonObject.put("imagePath", earliestRecord.getImagePath());
                jsonObject.put("videoPath", earliestRecord.getVideoPath());
            } else {
                jsonObject.put("alarmPosition", null);
                jsonObject.put("direction", null);
                jsonObject.put("milestoneMeter", null);
                jsonObject.put("directiondes", null);
                jsonObject.put("imagePath", null);
                jsonObject.put("videoPath", null);
            }

            jsonArray.add(jsonObject);
        }

        JSONObject result = new JSONObject();
        result.put("total", resultPage.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("list", jsonArray);
        return ApiResponse.success(result);
    }

    /**
     * @desc 高信专用接口_不分页
     * @param roadId
     * @param milestoneStart
     * @param milestoneEnd
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/gxVersion/noPage")
    public ApiResponse list(
            @RequestParam(required = false) String roadId,
            @RequestParam(required = false) Integer milestoneStart,
            @RequestParam(required = false) Integer milestoneEnd,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        QueryWrapper<AlarmCollection> query = new QueryWrapper<>();

        if (roadId != null && !roadId.isEmpty()) {
            query.like("road_id", roadId);
        }

        if (milestoneStart != null) {
            query.ge("milestone", milestoneStart);
        }

        if (milestoneEnd != null) {
            query.le("milestone", milestoneEnd);
        }

        if (startTime != null) {
            query.ge("latest_alarm_time", startTime);
        }

        if (endTime != null) {
            query.le("latest_alarm_time", endTime);
        }

        query.orderByDesc("create_time");

        // 不分页查询
        List<AlarmCollection> resultList = alarmCollectionServiceImpl.list(query);
        JSONArray jsonArray = new JSONArray();

        for (AlarmCollection alarmCollection : resultList) {
            JSONObject jsonObject = new JSONObject();

            String[] relatedIds = alarmCollection.getRelatedIdList() != null
                    ? alarmCollection.getRelatedIdList().split(",")
                    : new String[0];

            OriginalAlarmRecord earliestRecord = (relatedIds.length > 0)
                    ? originalAlarmServiceImpl.getById(relatedIds[0])
                    : null;

            jsonObject.put("id", alarmCollection.getCollectionId());
            jsonObject.put("inferEventTypeId", null);
            jsonObject.put("inferEventTypeName", alarmCollection.getEventType());
            jsonObject.put("suggestion", alarmCollection.getDisposalAdvice());
            jsonObject.put("preCheckTag", null);
            jsonObject.put("confirmStatus", null);
            jsonObject.put("eventId", earliestRecord != null ? earliestRecord.getEventId() : null);
            jsonObject.put("createTime", alarmCollection.getCreateTime());
            jsonObject.put("updateTime", alarmCollection.getModifyTime());
            jsonObject.put("alarmNum", alarmCollection.getRelatedAlarmNum());

            if (relatedIds.length > 0) {
                QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("id", Arrays.asList(relatedIds));
                jsonObject.put("alarmList", originalAlarmServiceImpl.list(queryWrapper));
            } else {
                jsonObject.put("alarmList", new JSONArray());
            }

            jsonObject.put("roadId", alarmCollection.getRoadId());
            jsonObject.put("alarmTime", earliestRecord != null ? earliestRecord.getAlarmTime() : null);

            if (earliestRecord != null && earliestRecord.getContent() != null) {
                String[] parts = earliestRecord.getContent().split(" ");
                jsonObject.put("alarmPosition", parts.length >= 3 ? parts[0] + " " + parts[1] + " " + parts[2] : earliestRecord.getContent());
                jsonObject.put("direction", earliestRecord.getDirection());
                jsonObject.put("milestoneMeter", earliestRecord.getMilestone());
                jsonObject.put("directiondes", earliestRecord.getDirectionDes());
                jsonObject.put("imagePath", earliestRecord.getImagePath());
                jsonObject.put("videoPath", earliestRecord.getVideoPath());
            } else {
                jsonObject.put("alarmPosition", null);
                jsonObject.put("direction", null);
                jsonObject.put("milestoneMeter", null);
                jsonObject.put("directiondes", null);
                jsonObject.put("imagePath", null);
                jsonObject.put("videoPath", null);
            }

            jsonArray.add(jsonObject);
        }

        JSONObject result = new JSONObject();
        result.put("total", jsonArray.size());
        result.put("list", jsonArray);
        return ApiResponse.success(result);
    }

    /**
     * 分页查询复核事件，支持多条件过滤
     */
    @GetMapping
    public ApiResponse<IPage<AlarmCollection>> list(

            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) Integer disposalAdvice
    ) {
        Page<AlarmCollection> page = new Page<>(pageNo, pageSize);
        QueryWrapper<AlarmCollection> query = new QueryWrapper<>();

        if (startDate != null) {
            query.ge("create_time", startDate.atStartOfDay()); // >= 00:00:00
        }
        if (endDate != null) {
            query.le("create_time", endDate.atTime(23, 59, 59)); // <= 23:59:59
        }

        if (StringUtils.hasText(eventType)) {
            query.eq("event_type", eventType);
        }

        if (StringUtils.hasText(deviceName)) {
            query.like("device_name", deviceName); // 模糊匹配 content 字段
        }

        if (disposalAdvice != null) {
            query.eq("disposal_advice", disposalAdvice);
        }

        query.orderByDesc("create_time");

        IPage<AlarmCollection> result = alarmCollectionServiceImpl.page(page, query);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/relatedEvents")
    public ApiResponse getRelatedEvents(@PathVariable Integer id) {
        AlarmCollection alarmCollection = alarmCollectionServiceImpl.getById(id);
        String[] idList = alarmCollection.getRelatedIdList().split(",");

        JSONArray resultJsonArray = new JSONArray();
        for (int i = idList.length - 1; i >= 0; i--) {
            QueryWrapper<CheckAlarmResult> wrapperResult = new QueryWrapper<>();
            wrapperResult.eq("alarmId", idList[i]);//获取原始告警记录id
            CheckAlarmResult checkAlarmResult = checkAlarmResultServiceImpl.getOne(wrapperResult, false);//获取检测结果

            QueryWrapper<OriginalAlarmRecord> wrapperOriginal = new QueryWrapper<>();
            wrapperResult.eq("alarmId", idList[i]);//获取原始告警记录id
            OriginalAlarmRecord originalAlarmRecord = originalAlarmServiceImpl.getOne(wrapperOriginal, false);//获取原始记录

            if (checkAlarmResult != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", checkAlarmResult.getAlarmId());
                jsonObject.put("alarmTime", originalAlarmRecord.getAlarmTime());
                jsonObject.put("eventType", originalAlarmRecord.getEventType());
                jsonObject.put("content", originalAlarmRecord.getContent());
                jsonObject.put("checkFlag", checkAlarmResult.getCheckFlag());
                jsonObject.put("dealFlag", originalAlarmRecord.getDealFlag());
                jsonObject.put("videoPath", originalAlarmRecord.getVideoPath());
                jsonObject.put("imagePath", originalAlarmRecord.getImagePath());
                resultJsonArray.add(jsonObject);
            }
        }
        return ApiResponse.success(resultJsonArray);
    }

    /**
     * @desc 根据alarmId查询归属告警集
     * @param alarmId
     * @return
     */
    @GetMapping("/{alarmId}")
    public ApiResponse getConnectionByAlarmId(@PathVariable String alarmId) {
        return ApiResponse.success(alarmCollectionServiceImpl.getCollectionByAlarmId(alarmId));
    }
}