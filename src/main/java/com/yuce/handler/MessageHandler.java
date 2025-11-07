package com.yuce.handler;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface MessageHandler {
    void handleMessage(ConsumerRecord<String, String> record) throws Exception;
}
