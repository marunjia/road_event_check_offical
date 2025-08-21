package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.FailReasonConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 失败原因配置表 Mapper 接口
 */
@Mapper
public interface FailReasonConfigMapper extends BaseMapper<FailReasonConfig> {
    // 继承 BaseMapper 后，已包含常见 CRUD 方法（无需手动编写 SQL）
    // 如需自定义查询，可在此添加方法并在 XML 中实现
}