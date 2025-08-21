package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.CollectionDurationConfig;
import com.yuce.mapper.CollectionDurationConfigMapper;
import com.yuce.service.CollectionDurationConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName CollectionDurationConfigServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/14 18:01
 * @Version 1.0
 */

@Service
public class CollectionDurationConfigServiceImpl extends ServiceImpl<CollectionDurationConfigMapper, CollectionDurationConfig> implements CollectionDurationConfigService {

    @Autowired
    private CollectionDurationConfigMapper collectionDurationConfigMapper;

    /**
     * @desc 获取所有告警配置
     * @return
     */
    public List<CollectionDurationConfig> getAll() {
        return this.list();
    }

    /**
     * @desc 根据告警集配置获取告警集详情
     * @return
     */
    public CollectionDurationConfig getConfig() {
        QueryWrapper<CollectionDurationConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("modify_time");
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper,false);
    }

    /**
     * @desc 根据告警集配置获取告警集详情
     * @return
     */
    public int updateConfigById(CollectionDurationConfig config) {
        QueryWrapper<CollectionDurationConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("id", config.getId());
        return collectionDurationConfigMapper.update(config, wrapper);
    }
}