package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.ExtractImageRecord;
import com.yuce.mapper.ExtractImageMapper;
import com.yuce.service.ExtractImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @ClassName ExtractImageServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/29 15:31
 * @Version 1.0
 */

@Service
@Slf4j
public class ExtractImageServiceImpl extends ServiceImpl<ExtractImageMapper, ExtractImageRecord> implements ExtractImageService {

    /**
     * @desc 保存抠图对象
     * @param extractImageRecord
     */
    public void insertImage(ExtractImageRecord extractImageRecord) {
        this.save(extractImageRecord);
    }

    /**
     * @desc 根据key值查询抠图对象
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public ExtractImageRecord getImageByKey(String alarmId, String imagePath, String videoPath) {
        QueryWrapper<ExtractImageRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("alarm_id", alarmId);
        queryWrapper.eq("image_path", imagePath);
        queryWrapper.eq("video_path", videoPath);
        return this.getOne(queryWrapper);
    }
}