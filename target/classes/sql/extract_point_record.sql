CREATE TABLE `extract_point_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alarm_id` varchar(255) NOT NULL COMMENT '告警id',
    `image_path` varchar(255) NOT NULL COMMENT '图片路径',
    `video_path` varchar(255) NOT NULL COMMENT '视频路径',
    `image_id` varchar(255) NOT NULL COMMENT '图片ID',
    `status` int(10) DEFAULT '0' COMMENT '状态',
    `point1_x` int(11) NOT NULL COMMENT '左上角X坐标',
    `point1_y` int(11) NOT NULL COMMENT '左上角Y坐标',
    `point2_x` int(11) NOT NULL COMMENT '右下角X坐标',
    `point2_y` int(11) NOT NULL COMMENT '右下角Y坐标',
    `received_time` datetime DEFAULT NULL COMMENT '算法接收时间',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_id` (`alarm_id`)
) ENGINE=InnoDB AUTO_INCREMENT=70730 DEFAULT CHARSET=utf8mb4 COMMENT='图片检测框记录表'