CREATE TABLE `alarm_frame_image_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
    `alarm_id` varchar(255) NOT NULL COMMENT '关联告警事件ID',
    `image_path` varchar(255) DEFAULT NULL COMMENT '图片路径',
    `video_path` varchar(255) DEFAULT NULL COMMENT '视频路径',
    `frame_num` int(11) DEFAULT NULL COMMENT '抽帧帧数',
    `image_sort_no` int(11) NOT NULL COMMENT '图片排序编号',
    `image_url` varchar(255) NOT NULL COMMENT '图片URL',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_image_video` (`alarm_id`,`image_path`,`video_path`)
) ENGINE=InnoDB AUTO_INCREMENT=110407 DEFAULT CHARSET=utf8mb4 COMMENT='告警抽帧图片信息表'