package com.yuce.entity;

import java.time.LocalDateTime;

/**
 * @ClassName AlarmTimeRange
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/9/18 12:01
 * @Version 1.0
 */
public class AlarmTimeRange {
    private LocalDateTime minAlarmTime;
    private LocalDateTime maxAlarmTime;

    // getter & setter
    public LocalDateTime getMinAlarmTime() {
        return minAlarmTime;
    }

    public void setMinAlarmTime(LocalDateTime minAlarmTime) {
        this.minAlarmTime = minAlarmTime;
    }

    public LocalDateTime getMaxAlarmTime() {
        return maxAlarmTime;
    }

    public void setMaxAlarmTime(LocalDateTime maxAlarmTime) {
        this.maxAlarmTime = maxAlarmTime;
    }
}