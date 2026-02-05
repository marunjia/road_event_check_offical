package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.AlarmTimeRange;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.entity.OriginalAlarmRecordBak;
import com.yuce.entity.QueryResultCheckRecord;
import com.yuce.mapper.OriginalAlarmBakMapper;
import com.yuce.mapper.OriginalAlarmMapper;
import com.yuce.service.OriginalAlarmBakService;
import com.yuce.service.OriginalAlarmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * @ClassName OriginalAlarmServiceImpl
 * @Description 原始告警事件业务操作实现类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:01
 * @Version 1.0
 */
@Service
@Slf4j
public class OriginalAlarmBakServiceImpl extends ServiceImpl<OriginalAlarmBakMapper, OriginalAlarmRecordBak> implements OriginalAlarmBakService {

}