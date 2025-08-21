package com.yuce.controller;

import com.yuce.common.ApiResponse;
import com.yuce.entity.CollectionDurationConfig;
import com.yuce.service.impl.CollectionDurationConfigServiceImpl;
import com.yuce.validation.UpdateGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * @ClassName CollectionDurationConfigController
 * @Description 告警集配置时长，固定单条记录修改配置时长字段，不需要增删操作，仅需查询、修改操作
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/14 18:02
 * @Version 1.0
 */

@Slf4j
@RestController
@RequestMapping("/collection-duration-configs") // 使用复数形式和横杠分隔，添加API版本
public class CollectionDurationConfigController {

    @Autowired
    private CollectionDurationConfigServiceImpl collectionDurationConfigServiceImpl;

    /**
     * 修改告警集时长配置
     */
    @PutMapping
    public ApiResponse update(@Validated(UpdateGroup.class) @RequestBody CollectionDurationConfig collectionDurationConfig) {
        int affectRows = collectionDurationConfigServiceImpl.updateConfigById(collectionDurationConfig);
        if(affectRows > 0){
            return ApiResponse.success("更新数据成功");
        }else{
            return ApiResponse.fail(500,"更新数据失败");
        }
    }

    /**
     * 查询所有告警集时长配置
     */
    @GetMapping
    public ApiResponse list() {
        List<CollectionDurationConfig> list = collectionDurationConfigServiceImpl.list();
        return ApiResponse.success(list);
    }
}
