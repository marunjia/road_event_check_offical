package com.yuce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.entity.OriginalAlarmRecord;

public interface ExtractWindowService extends IService<ExtractWindowRecord> {

    /**
     * @desc 查询告警记录图片对应坐标框
     * @param alarmId
     * @param imagePath
     * @param videoPath
     * @return
     */
    ExtractWindowRecord getExtractWindow(String alarmId, String imagePath, String videoPath);

    /**
     * @desc 查询告警记录是否已经提框
     * @param alarmId
     * @param imagePath
     * * @param videoPath
     * @return
     */
    boolean existsByKey(String alarmId, String imagePath, String videoPath);

    /**
     * @desc 查询告警记录是否已经提框
     * @param extractWindowRecord
     * @return
     */
    boolean insertWindow(ExtractWindowRecord extractWindowRecord);
}
