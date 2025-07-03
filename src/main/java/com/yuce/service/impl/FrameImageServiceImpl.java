package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.FrameImageInfo;
import com.yuce.mapper.FrameImageMapper;
import com.yuce.service.FrameImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
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

    /**
     * @desc 根据alarmId、imagePath、videoPath查询抽帧图片列表
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public List<FrameImageInfo> getFrameListByKey(String alarmId, String imagePath, String videoPath){
        return frameImageMapper.getFrameListByKey(alarmId, imagePath, videoPath);
    }

    /**
     * @desc 插入新纪录
     * @param frameImageInfo
     */
    public void insert(FrameImageInfo frameImageInfo) {
        frameImageInfo.setCreateTime(LocalDateTime.now());
        frameImageInfo.setUpdateTime(LocalDateTime.now());
        frameImageMapper.insert(frameImageInfo);
    }

    /**
     * @desc 根据联合唯一主键更新记录
     * @param frameImageInfo
     */
    public void updateByKey(FrameImageInfo frameImageInfo) {
        QueryWrapper<FrameImageInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("alarm_id", frameImageInfo.getAlarmId())
                .eq("image_path", frameImageInfo.getImagePath())
                .eq("video_path", frameImageInfo.getVideoPath())
                .eq("image_sort_no", frameImageInfo.getImageSortNo());

        FrameImageInfo existing = frameImageMapper.selectOne(wrapper);
        if (existing != null) {
            frameImageInfo.setId(existing.getId());
            frameImageInfo.setUpdateTime(LocalDateTime.now());
            frameImageMapper.updateById(frameImageInfo);
        } else {
            throw new RuntimeException("记录不存在，无法更新");
        }
    }

    /**
     * @desc 根据key值判断记录是否存在
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public boolean existsByKey(String alarmId, String imagePath, String videoPath, int imageSortNo) {
        return frameImageMapper.getFrameByKey(alarmId, imagePath, videoPath, imageSortNo) == null?false:true;
    }

    /**
     * @desc 根据key值删除记录
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    public void deleteByKey(String alarmId, String imagePath, String videoPath){
        frameImageMapper.deleteByKey(alarmId, imagePath, videoPath);
    }

}