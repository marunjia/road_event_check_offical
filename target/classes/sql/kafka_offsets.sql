CREATE TABLE IF NOT EXISTS kafka_offsets (
   id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
   consumer_group  VARCHAR(255) NOT NULL COMMENT '消费者组ID',
   topic           VARCHAR(255) NOT NULL COMMENT 'Kafka主题名称',
   partition_id    INT NOT NULL COMMENT '分区号',
   offset_value    BIGINT NOT NULL COMMENT '已消费到的偏移量',
   create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   modify_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
   UNIQUE KEY uk_consumer_topic_partition (consumer_group, topic, partition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Kafka消费偏移量表';