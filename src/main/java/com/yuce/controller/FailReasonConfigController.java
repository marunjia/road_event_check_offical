package com.yuce.controller;

import com.yuce.entity.FailReasonConfig;
import com.yuce.service.FailReasonConfigService;
import com.yuce.common.ApiResponse; // 假设存在通用响应类
import com.yuce.service.impl.FailReasonConfigServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 失败原因配置表 控制器
 */
@RestController
@RequestMapping("/fail-reason-config")
public class FailReasonConfigController {

    @Autowired
    private FailReasonConfigServiceImpl failReasonConfigServiceImpl;

    /**
     * 查询所有配置
     */
    @GetMapping("/list")
    public ApiResponse<List<FailReasonConfig>> getAllConfigs() {
        List<FailReasonConfig> list = failReasonConfigServiceImpl.list();
        return ApiResponse.success(list);
    }

    /**
     * 根据来源查询配置
     */
    @GetMapping("/list-by-source")
    public ApiResponse<List<FailReasonConfig>> getConfigsBySource(
            @RequestParam(required = false) Integer source) {
        List<FailReasonConfig> list = failReasonConfigServiceImpl.listBySource(source);
        return ApiResponse.success(list);
    }

    /**
     * 根据ID查询配置
     */
    @GetMapping("/{id}")
    public ApiResponse<FailReasonConfig> getConfigById(@PathVariable Long id) {
        FailReasonConfig config = failReasonConfigServiceImpl.getById(id);
        if (config == null) {
            return ApiResponse.fail(404, "未找到ID为" + id + "的配置");
        }
        return ApiResponse.success(config);
    }

    /**
     * 新增配置
     */
    @PostMapping
    public ApiResponse<Boolean> addConfig(@RequestBody FailReasonConfig config) {
        try {
            boolean success = failReasonConfigServiceImpl.addConfig(config);
            return ApiResponse.success(success);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    /**
     * 更新配置
     */
    @PutMapping
    public ApiResponse<Boolean> updateConfig(@RequestBody FailReasonConfig config) {
        try {
            boolean success = failReasonConfigServiceImpl.updateConfig(config);
            return ApiResponse.success(success);
        } catch (IllegalArgumentException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    /**
     * 删除配置（根据ID）
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteConfig(@PathVariable Long id) {
        try {
            boolean success = failReasonConfigServiceImpl.deleteConfigById(id);
            if (!success) {
                return ApiResponse.fail(404, "未找到ID为 " + id + " 的配置");
            }
            return ApiResponse.success(true);
        } catch (Exception e) {
            return ApiResponse.fail(500, "删除失败：" + e.getMessage());
        }
    }
}