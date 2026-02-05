package com.yuce.task;

import com.yuce.service.impl.IndexStatResultServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Component
@Slf4j
public class ScheduleIndexTask {

    @Autowired
    private IndexStatResultServiceImpl indexStatResultServiceImpl;

    @Scheduled(cron = "0 0 4 * * ?") // 每天凌晨4点整执行
    public void indexStat() {

        //获取昨日日期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -1); // 日期减1天
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String yesterdayStr = sdf.format(calendar.getTime());

        //有效告警检出率
        indexStatResultServiceImpl.insertValidAlarmCheckRate(yesterdayStr);

        //有效告警检出正确率
        indexStatResultServiceImpl.insertValidAlarmCheckRightRate(yesterdayStr);

        //误检告警检出率
        indexStatResultServiceImpl.insertErrorReportCheckRate(yesterdayStr);

        //误检告警检出正确率
        indexStatResultServiceImpl.insertErrorReportCheckRightRate(yesterdayStr);

        //正检告警检出率
        indexStatResultServiceImpl.insertRightReportCheckRate(yesterdayStr);

        //正检告警检出正确率
        indexStatResultServiceImpl.insertRightReportCheckRightRate(yesterdayStr);

        //告警压缩率
        indexStatResultServiceImpl.insertAlarmCompressionRate(yesterdayStr);

        //交通事件检测转化率
        indexStatResultServiceImpl.insertTrafficEventConversionRate(yesterdayStr);

        //事件关联跟踪准确率
        indexStatResultServiceImpl.insertEventTrackingAccuracy(yesterdayStr);
    }
}