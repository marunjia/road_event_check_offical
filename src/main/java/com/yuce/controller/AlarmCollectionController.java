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
import java.time.format.DateTimeFormatter;
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
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private AlarmCollectionServiceImpl alarmCollectionServiceImpl;

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

            List<String> relatedIds = alarmCollection.getRelatedTblIdList() != null
                    ? Arrays.asList(alarmCollection.getRelatedTblIdList().split(","))
                    : Collections.emptyList();

            OriginalAlarmRecord earliestRecord = (relatedIds.size() > 0)
                    ? originalAlarmServiceImpl.getById(relatedIds.get(0))
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
            jsonObject.put("collectionStatus", alarmCollection.getCollectionStatus());

            if (relatedIds.size() > 0) {
                jsonObject.put("alarmList", originalAlarmServiceImpl.getListByTblIdList(relatedIds));
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

            String[] relatedIds = alarmCollection.getRelatedTblIdList() != null
                    ? alarmCollection.getRelatedTblIdList().split(",")
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
            jsonObject.put("collectionStatus", alarmCollection.getCollectionStatus());

            if (relatedIds.length > 0) {
                QueryWrapper<OriginalAlarmRecord> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("tbl_id", Arrays.asList(relatedIds));
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
            @RequestParam(required = false) String collectionId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String roadId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Integer disposalAdvice,
            @RequestParam(required = false) Integer checkFlag,
            @RequestParam(required = false) Integer relatedAlarmNum,
            @RequestParam(required = false) Integer collectionType
    ) {
        Page<AlarmCollection> page = new Page<>(pageNo, pageSize);
        QueryWrapper<AlarmCollection> query = new QueryWrapper<>();

        if (StringUtils.hasText(collectionId)) {
            query.eq("collection_id", collectionId);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if (StringUtils.hasText(startTime)) {
            LocalDate date = LocalDate.parse(startTime, formatter);
            query.ge("earliest_alarm_time", date.atStartOfDay());
        }

        if (StringUtils.hasText(endTime)) {
            LocalDate date = LocalDate.parse(endTime, formatter);
            query.le("latest_alarm_time", date.atTime(23, 59, 59));
        }

        if (StringUtils.hasText(deviceId)) {
            query.eq("device_id", deviceId);
        }

        if (StringUtils.hasText(deviceName)) {
            query.like("device_name", deviceName); // 模糊匹配 content 字段
        }

        if (StringUtils.hasText(roadId)) {
            query.eq("road_id", roadId); // 模糊匹配 content 字段
        }

        if (StringUtils.hasText(eventType)) {
            query.eq("event_type", eventType);
        }

        if (disposalAdvice != null) {
            query.eq("disposal_advice", disposalAdvice);
        }

        if (checkFlag != null) {
            query.eq("check_flag", checkFlag);
        }

        if (relatedAlarmNum != null) {
            query.ge("related_alarm_num", relatedAlarmNum);
        }

        if (collectionType != null) {
            query.eq("collection_type", collectionType);
        }

        query.orderByDesc("latest_alarm_time");

        IPage<AlarmCollection> result = alarmCollectionServiceImpl.page(page, query);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}/relatedEvents")
    public ApiResponse getRelatedEvents(@PathVariable Integer id) {
        AlarmCollection alarmCollection = alarmCollectionServiceImpl.getById(id);
        String[] idList = alarmCollection.getRelatedTblIdList().split(",");

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
     * @param tblId
     * @return
     */
    @GetMapping("/{alarmId}")
    public ApiResponse getConnectionByTblId(@PathVariable long tblId) {
        return ApiResponse.success(alarmCollectionServiceImpl.getCollectionByTblId(tblId));
    }

    /**
     * @desc 根据collectionId查询告警集信息
     * @param collectionId
     * @return
     */
    @GetMapping("/query/byCollectionId")
    public ApiResponse getConnectionByTblId(@RequestParam String collectionId) {
        return ApiResponse.success(alarmCollectionServiceImpl.getCollectionByCollectionId(collectionId));
    }

    @PutMapping("/update/personCheckFlag/byCollectionId")
    public ApiResponse updatePersonCheckFlag(@RequestParam String collectionId,
                                             @RequestParam Integer personCheckFlag) {
        if (collectionId == null || personCheckFlag == null) {
            return ApiResponse.fail(400,"参数collectionId、personCheckFlag不能为空");
        }

        int affectRows = alarmCollectionServiceImpl.updatePersonCheckFlag(collectionId, personCheckFlag);

        if (affectRows > 0) {
            return ApiResponse.success("更新成功");
        } else {
            return ApiResponse.fail(401,"更新失败，未找到对应记录");
        }
    }

    @PutMapping("/update/personCheckReason/byCollectionId")
    public ApiResponse updatePersonCheckFlag(@RequestParam String collectionId,
                                             @RequestParam String personCheckReason) {
        if (collectionId == null || personCheckReason == null) {
            return ApiResponse.fail(400,"参数collectionId、personCheckFlag不能为空");
        }

        int affectRows = alarmCollectionServiceImpl.updatePersonCheckReason(collectionId, personCheckReason);

        if (affectRows > 0) {
            return ApiResponse.success("更新成功");
        } else {
            return ApiResponse.fail(401,"更新失败，未找到对应记录");
        }
    }
}