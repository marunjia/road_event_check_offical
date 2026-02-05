package com.yuce.task;

import com.yuce.util.KafkaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @ClassName MainLineTask
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/3 15:06
 * @Version 1.0
 */

@Component
public class MainLineTask implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private KafkaUtil kafkaUtil;

    @Autowired
    private EventFetchTask eventFetchTask;

    @Autowired
    private MysqlFetchTask mysqlFetchTask;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        //mysqlFetchTask.totalProcess();
        kafkaUtil.startConsumers(5, eventFetchTask);
    }
}