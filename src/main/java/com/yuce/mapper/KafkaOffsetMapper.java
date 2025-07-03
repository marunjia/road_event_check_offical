package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.KafkaOffset;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KafkaOffsetMapper extends BaseMapper<KafkaOffset> {
}
