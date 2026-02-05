package com.yuce.controller;

import com.yuce.common.ApiResponse;
import com.yuce.entity.IndexStatResult;
import com.yuce.service.impl.FeatureElementServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @ClassName FeatureElementController
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/31 11:24
 * @Version 1.0
 */

@Slf4j
@RestController
@RequestMapping("/feature-elements")
public class FeatureElementController {

    @Autowired
    private FeatureElementServiceImpl featureElementServiceImpl;

    /**
     * @desc 根据collectionId查询告警组记录
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    @GetMapping("/primary-key")
    public ApiResponse queryByKey(@RequestParam(required = true) String alarmId,@RequestParam(required = true) String imagePath,@RequestParam(required = true) String videoPath ) {
        if (alarmId == null || alarmId.isEmpty() || imagePath == null || imagePath.isEmpty() || videoPath == null || videoPath.isEmpty()) {
            return ApiResponse.fail(400, "参数alarmId、imagePath、videoPath不能为空");
        }
        return ApiResponse.success(featureElementServiceImpl.getFeatureByKey(alarmId, imagePath, videoPath));
    }

    /**
     * @desc 查询所有特征要素
     * @return
     */
    @GetMapping("/all")
    public ApiResponse queryAll() {
        return ApiResponse.success(featureElementServiceImpl.queryAll());
    }

    /**
     * @desc 根据特征要素主键id更新告警集匹配关系
     * @param id
     * @param collectionMatchStatus
     * @return
     */
    @PutMapping("/match-status")
    public ApiResponse updateCollectionMatchStatus(@RequestParam Integer id,
                                                   @RequestParam Integer collectionMatchStatus) {
        if (id == null || collectionMatchStatus == null) {
            return ApiResponse.fail(400,"参数id、collectionMatchStatus不能为空");
        }

        int affectRows = featureElementServiceImpl.updateCollectionMatchStatus(id, collectionMatchStatus);

        if (affectRows > 0) {
            return ApiResponse.success("更新成功");
        } else {
            return ApiResponse.fail(401,"更新失败，未找到对应记录");
        }
    }

    /**
     * @desc 根据特征要素id更新人工检验标签
     * @param id
     * @param personCheckFlag
     * @return
     */
    @PutMapping("/person-check-flag")
    public ApiResponse updatePersonCheckFlag(@RequestParam Integer id, @RequestParam Integer personCheckFlag) {
        if (id == null || personCheckFlag == null) {
            return ApiResponse.fail(400,"参数id、personCheckFlag不能为空");
        }

        int affectRows = featureElementServiceImpl.updatePersonCheckFlag(id, personCheckFlag);

        if (affectRows > 0) {
            return ApiResponse.success("更新成功");
        } else {
            return ApiResponse.fail(401,"更新失败，未找到对应记录");
        }
    }

    /**
     * @desc 根据特征要素id更新关联是否准确字段
     * @param id
     * @param matchCheckFlag
     * @return
     */
    @PutMapping("/match-check-flag")
    public ApiResponse updateMatchCheckFlag(@RequestParam Integer id, @RequestParam Integer matchCheckFlag) {
        if (id == null || matchCheckFlag == null) {
            return ApiResponse.fail(400,"参数id、matchCheckFlag不能为空");
        }

        int affectRows = featureElementServiceImpl.updateMatchCheckFlag(id, matchCheckFlag);

        if (affectRows > 0) {
            return ApiResponse.success("更新成功");
        } else {
            return ApiResponse.fail(401,"更新失败，未找到对应记录");
        }
    }

    /**
     * @desc 根据特征要素id更新关联错误原因字段
     * @param id
     * @param matchCheckReason
     * @return
     */
    @PutMapping("/match-check-reason")
    public ApiResponse updateMatchCheckReason(@RequestParam Integer id, @RequestParam String matchCheckReason) {
        if (id == null || matchCheckReason == null) {
            return ApiResponse.fail(400,"参数id、matchCheckReason不能为空");
        }

        int affectRows = featureElementServiceImpl.updateMatchCheckReason(id, matchCheckReason);

        if (affectRows > 0) {
            return ApiResponse.success("更新成功");
        } else {
            return ApiResponse.fail(401,"更新失败，未找到对应记录");
        }
    }

    /**
     * @desc 统计指定时间区间范围内关联错误的原因分布情况
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/index/reason/byReasonType")
    public ApiResponse getIndexByReasonType(
            @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        if (Objects.isNull(startTime) || Objects.isNull(endTime)) {
            return ApiResponse.fail(400,"开始时间/结束时间不能为空");
        }
        if (startTime.isAfter(endTime)) {
            return ApiResponse.fail(400,"开始时间不能晚于结束时间");
        }

        return ApiResponse.success(featureElementServiceImpl.getIndexByReasonType(startTime, endTime));
    }

    /**
     * @desc  统计指定时间区间范围内关联错误的总记录条数
     * @param startTime
     * @param endTime
     * @return
     */
    @GetMapping("/index/reason/total")
    public ApiResponse getIndexMatchErrorCount(
            @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        if (Objects.isNull(startTime) || Objects.isNull(endTime)) {
            return ApiResponse.fail(400,"开始时间/结束时间不能为空");
        }
        if (startTime.isAfter(endTime)) {
            return ApiResponse.fail(400,"开始时间不能晚于结束时间");
        }

        return ApiResponse.success(featureElementServiceImpl.getIndexMatchErrorCount(startTime, endTime));
    }


}