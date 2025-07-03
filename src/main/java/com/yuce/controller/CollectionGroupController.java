package com.yuce.controller;

import com.yuce.common.ApiResponse;
import com.yuce.service.impl.CollectionGroupServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
@RequestMapping("/collection_group")
public class CollectionGroupController {

    @Autowired
    private CollectionGroupServiceImpl collectionGroupServiceImpl;

    /**
     * @desc 根据collectionId查询告警组记录
     * @param collectionId
     * @return
     */
    @GetMapping("/queryByCollectionId")
    public ApiResponse queryByCollectionId(@RequestParam(required = true) String collectionId) {
        if (collectionId == null || collectionId.isEmpty()) {
            return ApiResponse.fail(400, "参数collectionId不能为空");
        }
        return ApiResponse.success(collectionGroupServiceImpl.queryByCollectionId(collectionId));
    }

    /**
     * @desc 根据groupId查询告警组记录
     * @param groupId
     * @return
     */
    @GetMapping("/queryByGroupId")
    public ApiResponse queryByGroupId(@RequestParam(required = true) String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return ApiResponse.fail(400, "参数collectionId不能为空");
        }
        return ApiResponse.success(collectionGroupServiceImpl.queryByGroupId(groupId));
    }

    /**
     * @desc 根据groupId查询告警组记录
     * @param groupId
     * @return
     */
    @GetMapping("/queryByCollectionIdAndGroupId")
    public ApiResponse queryByCollectionIdAndGroupId(@RequestParam(required = true) String collectionId,@RequestParam(required = true) String groupId) {
        if (groupId == null || groupId.isEmpty() || collectionId == null || collectionId.isEmpty()) {
            return ApiResponse.fail(400, "参数collectionId不能为空");
        }
        return ApiResponse.success(collectionGroupServiceImpl.queryByCollectionIdAndGroupId(collectionId,groupId));
    }

}