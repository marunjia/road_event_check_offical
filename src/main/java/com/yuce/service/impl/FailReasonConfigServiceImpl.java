package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.FailReasonConfig;
import com.yuce.mapper.FailReasonConfigMapper;
import com.yuce.service.FailReasonConfigService;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 失败原因配置表 Service 实现类
 */
@Service
public class FailReasonConfigServiceImpl extends ServiceImpl<FailReasonConfigMapper, FailReasonConfig> implements FailReasonConfigService {

    /**
     * 根据来源查询原因列表
     */
    @Override
    public List<FailReasonConfig> listBySource(Integer source) {
        LambdaQueryWrapper<FailReasonConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(source != null, FailReasonConfig::getSource, source)
                .orderByDesc(FailReasonConfig::getCreateTime); // 按创建时间倒序
        return baseMapper.selectList(queryWrapper);
    }

    /**
     * 新增失败原因配置
     */
    @Override
    public boolean addConfig(FailReasonConfig failReasonConfig) {
        // 可添加参数校验（如原因描述非空）
        if (failReasonConfig.getReason() == null || failReasonConfig.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("原因描述不能为空");
        }
        return save(failReasonConfig);
    }

    /**
     * 更新失败原因配置
     */
    @Override
    public boolean updateConfig(FailReasonConfig failReasonConfig) {
        // 校验ID非空
        if (failReasonConfig.getId() == null) {
            throw new IllegalArgumentException("更新失败：ID不能为空");
        }
        // 校验记录是否存在
        FailReasonConfig exist = getById(failReasonConfig.getId());
        if (exist == null) {
            throw new IllegalArgumentException("更新失败：不存在ID为" + failReasonConfig.getId() + "的记录");
        }
        return updateById(failReasonConfig);
    }

    @Override
    public boolean deleteConfigById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID不能为空");
        }
        return this.removeById(id);
    }
}