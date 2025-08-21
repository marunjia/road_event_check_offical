package com.yuce.controller;

import com.yuce.common.ApiResponse;
import com.yuce.service.impl.CollectionGroupServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @ClassName CollectionGroupController
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/31 11:09
 * @Version 1.0
 */

@Slf4j
@RestController
@RequestMapping("/collection-groups")
public class CollectionGroupController {

    @Autowired
    private CollectionGroupServiceImpl collectionGroupServiceImpl;

    /**
     * @desc 根据collectionId和groupId查询告警组对应告警记录
     * @param groupId
     * @param collectionId
     * @return
     */
    @GetMapping("/list/by-colleciton-group")
    public ApiResponse queryByCollectionIdAndGroupId(@RequestParam(required = true) String collectionId,@RequestParam(required = true) String groupId) {
        if (groupId == null || groupId.isEmpty() || collectionId == null || collectionId.isEmpty()) {
            return ApiResponse.fail(400, "参数collectionId、groupId不能为空");
        }
        return ApiResponse.success(collectionGroupServiceImpl.queryByCollectionIdAndGroupId(collectionId,groupId));
    }

    /**
     * @desc 根据告警集id统计告警组分布情况
     * @param collectionId
     * @return
     */
    @GetMapping("/group-index-stat")
    public ApiResponse getIndexByCollectionId(@RequestParam String collectionId) {
        List<Map<String, Object>> statMapList = collectionGroupServiceImpl.getIndexByCollectionId(collectionId);
        return ApiResponse.success(statMapList);
    }
}