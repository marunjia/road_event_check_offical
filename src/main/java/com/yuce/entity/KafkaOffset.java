package com.yuce.entity;

/**
 * @ClassName KafkaOffset
 * @Description Kafka消费位移记录表实体类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/21 18:00
 * @Version 1.0
 */

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kafka_offsets")
public class KafkaOffset {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("consumer_group")
    private String consumerGroup;

    @TableField("topic")
    private String topic;

    @TableField("partition_id")
    private Integer partitionId;

    @TableField("offset_value")
    private Long offsetValue;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "modify_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;
}