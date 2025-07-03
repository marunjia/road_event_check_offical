package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.ExtractImageRecord;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.mapper.ExtractImageMapper;
import com.yuce.mapper.ExtractWindowMapper;
import com.yuce.service.ExtractImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @ClassName ExtractWindowServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/29 20:05
 * @Version 1.0
 */

@Service
@Slf4j
public class ExtractWindowServiceImpl extends ServiceImpl<ExtractWindowMapper, ExtractWindowRecord> implements ExtractImageService {

    /**
     * @desc 查询告警记录图片对应坐标框
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public ExtractWindowRecord getExtractWindow(String alarmId, String imagePath, String videoPath) {
        QueryWrapper<ExtractWindowRecord> extractWrapper = new QueryWrapper<>();
        extractWrapper.eq("alarm_id", alarmId);
        extractWrapper.eq("image_path", imagePath);
        extractWrapper.eq("video_path", videoPath);
        return this.getOne(extractWrapper);
    }

    /**
     * @desc 查询告警记录是否已经提框
     * @param alarmId
     * @param imagePath
     * * @param videoPath
     * @return
     */
    public boolean existsByKey(String alarmId, String imagePath, String videoPath) {
        //根据告警记录id查询原始告警抽图对应的框坐标
        QueryWrapper<ExtractWindowRecord> extractWrapper = new QueryWrapper<>();
        extractWrapper.eq("alarm_id", alarmId);
        extractWrapper.eq("image_path", imagePath);
        extractWrapper.eq("video_path", videoPath);
        return this.getOne(extractWrapper) != null;
    }

    /**
     * @desc 查询告警记录是否已经提框
     * @param extractWindowRecord
     * @return
     */
    public boolean insertWindow(ExtractWindowRecord extractWindowRecord) {
        return this.save(extractWindowRecord);
    }
}