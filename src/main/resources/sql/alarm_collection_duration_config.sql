CREATE TABLE `alarm_collection_duration_config` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `duration_minutes` int(11) NOT NULL COMMENT '告警集时长（分钟）',
    `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COMMENT='告警集时长配置表'