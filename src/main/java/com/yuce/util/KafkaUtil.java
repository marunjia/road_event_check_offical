package com.yuce.util;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yuce.config.KafkaProperties;
import com.yuce.entity.KafkaOffset;
import com.yuce.service.impl.KafkaOffsetServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Slf4j
public class KafkaUtil {

    @Autowired
    private KafkaProperties kafkaProperties;

    @Autowired
    KafkaOffsetServiceImpl kafkaOffsetServiceImpl;

    /**
     * @desc 获取kafka消费者对象
     * @return
     */
    public KafkaConsumer getConsumer(){
        log.info("-------------------kafka消费者初始化-------------------");
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", kafkaProperties.getBootstrapServers()));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getConsumer().getGroupId());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // 禁用自动提交
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaProperties.getConsumer().getAutoOffsetReset());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaProperties.getConsumer().getKeyDeserializer());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaProperties.getConsumer().getValueDeserializer());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaProperties.getConsumer().getMaxPollRecords());

        String topic = kafkaProperties.getTopic().getVideoAlarmDetail();//获取topic名称

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

        // 订阅主题
        consumer.subscribe(Collections.singletonList(topic));

        // 初次 poll() 必须触发分区分配（Kafka 0.11）
        while (consumer.assignment().isEmpty()) {
            log.info(consumer.assignment().toString());//打印kafka topic信息
            consumer.poll(1000); // 必须多次poll触发分区rebalance
        }

        Set<TopicPartition> partitions = consumer.assignment();

        // 设置每个分区的消费起点
        for (TopicPartition partition : partitions) {
            QueryWrapper<KafkaOffset> wrapper = new QueryWrapper<>();
            wrapper.eq("consumer_group", kafkaProperties.getConsumer().getGroupId());
            wrapper.eq("topic", topic);
            wrapper.eq("partition_id", partition.partition());

            KafkaOffset offsetRecord = kafkaOffsetServiceImpl.getOne(wrapper, false);
            if (offsetRecord != null && offsetRecord.getOffsetValue() != null) {
                consumer.seek(partition, offsetRecord.getOffsetValue());
                log.info("设置偏移量：topic={}, partition={}, offset={}", topic, partition.partition(), offsetRecord.getOffsetValue());
            } else {
                log.info("未找到历史偏移量，使用默认策略 auto.offset.reset 处理：topic={}, partition={}", topic, partition.partition());
            }
        }
        return consumer;
    }

    /**
     * @desc 更新kafka消费者偏移量
     * @param consumerRecord
     */
    public void updateOffset(ConsumerRecord consumerRecord){
        KafkaOffset kafkaOffset = new KafkaOffset();
        //所有流程走完以后，保存偏移量
        KafkaOffset exist = kafkaOffsetServiceImpl.getOne(
                new QueryWrapper<KafkaOffset>()
                        .eq("consumer_group", kafkaProperties.getConsumer().getGroupId())
                        .eq("topic", consumerRecord.topic())
                        .eq("partition_id", consumerRecord.partition())
        );

        if (exist != null) {
            kafkaOffset.setId(exist.getId()); // 设置主键
        }
        kafkaOffset.setConsumerGroup(kafkaProperties.getConsumer().getGroupId());
        kafkaOffset.setTopic(consumerRecord.topic());
        kafkaOffset.setPartitionId(consumerRecord.partition());
        kafkaOffset.setOffsetValue(consumerRecord.offset() + 1L);
        kafkaOffsetServiceImpl.saveOrUpdate(kafkaOffset);
    }

}