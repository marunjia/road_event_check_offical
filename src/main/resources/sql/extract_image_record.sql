CREATE TABLE `extract_image_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `tbl_id` bigint(20) NOT NULL COMMENT '原始告警记录表主键',
    `alarm_id` varchar(64) DEFAULT NULL COMMENT '告警ID',
    `image_path` varchar(255) DEFAULT NULL COMMENT '图片路径',
    `video_path` varchar(255) DEFAULT NULL COMMENT '视频路径',
    `image_id` varchar(64) DEFAULT NULL COMMENT '图片ID',
    `image_url` varchar(255) DEFAULT NULL COMMENT '抠图目标图片对应的URL',
    `cropped_image_url` varchar(255) DEFAULT NULL COMMENT '抠图结果图片对应的URL',
    `point1_x` int(11) DEFAULT NULL COMMENT '左上角X坐标',
    `point1_y` int(11) DEFAULT NULL COMMENT '左上角Y坐标',
    `point2_x` int(11) DEFAULT NULL COMMENT '右下角X坐标',
    `point2_y` int(11) DEFAULT NULL COMMENT '右下角Y坐标',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_tbl_id` (`tbl_id`),
    KEY `idx_alarm_image_video` (`alarm_id`,`image_path`,`video_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抠图检测框记录表'
