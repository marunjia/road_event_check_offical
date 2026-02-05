package com.yuce.util;

import com.yuce.handler.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class KafkaUtil {

    private final ConcurrentHashMap<String, KafkaConsumer<String, String>> consumers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ExecutorService> consumerExecutors = new ConcurrentHashMap<>();
    private final AtomicInteger consumerCounter = new AtomicInteger(0);

    private static final long COMMIT_INTERVAL_MS = 1000;
    private volatile String groupId = "concurrent-version-video-alarm-169";
    private volatile String topic = "DATA_COLLECT_VIDEO_ALARM_DETAIL";
    private volatile String bootstrapServers = "12.1.150.178:9082,12.1.150.179:9082,12.1.150.180:9082,12.1.150.91:9082,12.1.150.92:9082";
    private volatile Integer maxPollRecords = 60;

    /**
     * @desc 启动所有consumers
     * @param consumerCount
     * @param messageHandler
     */
    public void startConsumers(int consumerCount, MessageHandler messageHandler) {
        for (int i = 0; i < consumerCount; i++) {
            String clientId = "consumer-" + consumerCounter.incrementAndGet();
            startConsumer(clientId, messageHandler);
        }
    }

    /**
     * @desc 启动单个consumer实例
     * @param clientId
     * @param messageHandler
     */
    private void startConsumer(String clientId, MessageHandler messageHandler) {
        Properties props = buildConsumerProperties(clientId);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        String consumerKey = groupId + "-" + clientId;
        consumers.put(consumerKey, consumer);
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "KafkaConsumerThread-" + clientId));
        consumerExecutors.put(consumerKey, executor);
        executor.submit(() -> runConsumer(consumer, consumerKey, messageHandler));
    }

    /**
     * @desc 执行consumer消费逻辑
     * @param consumer
     * @param consumerKey
     * @param handler
     */
    private void runConsumer(KafkaConsumer<String, String> consumer, String consumerKey, MessageHandler handler) {
        try {
            consumer.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                    consumer.commitSync(); // 提交当前偏移量
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                    log.info("Assigned partitions: {}", partitions);
                }
            });

            waitForAssignment(consumer);
            consumeMessages(consumer, consumerKey, handler);
        } catch (Exception e) {
            log.error("消费者 {} 运行异常", consumerKey, e);
        }
    }

    /**
     * @desc 分配分区
     * @param consumer
     * @return
     */
    private Set<TopicPartition> waitForAssignment(KafkaConsumer<String, String> consumer) {
        Set<TopicPartition> partitions = new HashSet<>();
        int retries = 0;
        while (partitions.isEmpty() && retries++ < 20) {
            consumer.poll(1000);
            partitions = consumer.assignment();
        }
        return partitions;
    }

    /**
     * @desc 消费处理消息逻辑
     * @param consumer
     * @param consumerKey
     * @param handler
     */
    private void consumeMessages(KafkaConsumer<String, String> consumer, String consumerKey, MessageHandler handler) {
        long lastCommitTime = System.currentTimeMillis();

        while (!Thread.currentThread().isInterrupted()) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(1000);
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        handler.handleMessage(record);
                    } catch (Exception e) {
                        log.error("消费失败：topic={}, partition={}, offset={}", record.topic(), record.partition(), record.offset(), e);
                    }
                }

                if (!records.isEmpty() && System.currentTimeMillis() - lastCommitTime > COMMIT_INTERVAL_MS) {
                    consumer.commitSync();
                    lastCommitTime = System.currentTimeMillis();
                    log.debug("手动提交偏移量");
                }
            } catch (Exception e) {
                log.error("消费拉取异常", e);
            }
        }
    }

    /**
     * @desc consumer属性配置z
     * @param clientId
     * @return
     */
    private Properties buildConsumerProperties(String clientId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // 手动提交
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);     // 30s，避免心跳超时
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);  // 10s，建议为 session 的 1/3
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 600000);  // 5min，防止处理太久被踢出组
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 40000);     // 保证比 session.timeout.ms 更大
        return props;
    }
}