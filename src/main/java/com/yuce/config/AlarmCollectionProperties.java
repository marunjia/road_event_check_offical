package com.yuce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName AlarmCollectionProperties
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/15 17:46
 * @Version 1.0
 */

@Data
@Component
@ConfigurationProperties(prefix = "alarm-collection")
public class AlarmCollectionProperties {
    private int rightIntervalMinute; //正检集间隔分钟
    private int falseIntervalMinute; //误检集间隔分钟
    private int uncertainIntervalMinute; //无法判定集间隔分钟
}




