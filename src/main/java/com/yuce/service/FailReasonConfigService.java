package com.yuce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuce.entity.FailReasonConfig;
import java.util.List;

/**
 * 失败原因配置表 Service 接口
 */
public interface FailReasonConfigService extends IService<FailReasonConfig> {

    /**
     * 根据来源查询原因列表
     * @param source 原因来源
     * @return 符合条件的原因列表
     */
    List<FailReasonConfig> listBySource(Integer source);

    /**
     * 新增失败原因配置
     * @param failReasonConfig 配置信息
     * @return 是否新增成功
     */
    boolean addConfig(FailReasonConfig failReasonConfig);

    /**
     * 更新失败原因配置（根据ID）
     * @param failReasonConfig 配置信息（需包含ID）
     * @return 是否更新成功
     */
    boolean updateConfig(FailReasonConfig failReasonConfig);
}