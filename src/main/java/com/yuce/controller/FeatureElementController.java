package com.yuce.controller;

import com.yuce.common.ApiResponse;
import com.yuce.service.impl.FeatureElementServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("/featureElement")
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
    @GetMapping("/queryByKey")
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
    @GetMapping("/queryAll")
    public ApiResponse queryAll() {
        return ApiResponse.success(featureElementServiceImpl.queryAll());
    }
}