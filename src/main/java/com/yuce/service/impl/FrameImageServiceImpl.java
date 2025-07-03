package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.FrameImageInfo;
import com.yuce.mapper.FrameImageMapper;
import com.yuce.service.FrameImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @ClassName AlarmFrameImageServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/22 14:32
 * @Version 1.0
 */
@Service
public class FrameImageServiceImpl extends ServiceImpl<FrameImageMapper, FrameImageInfo> implements FrameImageService {

    @Autowired
    private FrameImageMapper frameImageMapper;

    public List<FrameImageInfo> getListByAlarmId(String alarmId){
        QueryWrapper<FrameImageInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.orderByDesc("create_time");
        return frameImageMapper.selectList(queryWrapper);
    }

}