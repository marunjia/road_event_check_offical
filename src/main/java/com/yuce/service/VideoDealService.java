package com.yuce.service;

import com.yuce.entity.OriginalAlarmRecord;
import org.bytedeco.javacv.Frame;

/**
 * @ClassName VideoDealService
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/27 10:36
 * @Version 1.0
 */
public interface VideoDealService {

    public boolean processVideo(OriginalAlarmRecord originalAlarmRecord);
}