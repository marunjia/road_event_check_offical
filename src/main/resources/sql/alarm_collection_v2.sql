CREATE TABLE IF NOT EXISTS `alarm_collection_v2` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
    `collection_id` varchar(36) NOT NULL COMMENT '告警集ID，UUID格式',
    `road_id` varchar(255) NOT NULL COMMENT '告警集对应道路编码',
    `device_id` varchar(255) NOT NULL COMMENT '告警集点位设备ID',
    `device_name` varchar(255) DEFAULT NULL COMMENT '告警集点位名称',
    `milestone` INT COMMENT '桩米号',
    `event_type` varchar(255) DEFAULT NULL COMMENT '告警集事件类型',
    `disposal_advice` int(11) DEFAULT NULL COMMENT '处置建议：0-无法判断、1-疑似误报、2-尽快确认、3-无需处理',
    `collection_type` int(11) DEFAULT NULL COMMENT '告警集类型：0-无法判断告警集，1-正检告警集，2-误检告警集',
    `related_id_list` varchar(255) DEFAULT NULL COMMENT '关联告警id列表',
    `earliest_alarm_time` datetime DEFAULT NULL COMMENT '关联最新告警事件时间',
    `latest_alarm_time` datetime DEFAULT NULL COMMENT '关联最新告警事件时间',
    `related_alarm_num` int(10) NOT NULL COMMENT '关联告警数量',
    `collection_status` int(11) DEFAULT NULL COMMENT '告警集状态：1-使用中，2-已关闭',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '告警集创建时间',
    `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '告警集更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_collection_id` (`collection_id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COMMENT='核检告警集';

