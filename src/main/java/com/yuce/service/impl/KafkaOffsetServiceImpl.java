package com.yuce.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.KafkaOffset;
import com.yuce.mapper.KafkaOffsetMapper;
import com.yuce.service.KafkaOffsetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @ClassName KafkaOffsetServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/21 18:24
 * @Version 1.0
 */

@Slf4j
@Service
public class KafkaOffsetServiceImpl extends ServiceImpl<KafkaOffsetMapper, KafkaOffset> implements KafkaOffsetService {
}