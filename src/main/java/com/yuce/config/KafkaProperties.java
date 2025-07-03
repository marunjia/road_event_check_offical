package com.yuce.config;

/**
 * @ClassName KafkaProperties
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/21 17:55
 * @Version 1.0
 */
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    private List<String> bootstrapServers;
    private Consumer consumer;
    private Topic topic;

    @Data
    public static class Consumer {
        private String groupId;
        private boolean enableAutoCommit;
        private String autoOffsetReset;
        private int maxPollRecords;
        private String keyDeserializer;
        private String valueDeserializer;
    }

    @Data
    public static class Topic {
        private String videoAlarmDetail;
    }
}