CREATE TABLE `algorithm_check_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
  `alarm_id` varchar(128) NOT NULL COMMENT '关联告警事件ID',
  `image_path` varchar(255) DEFAULT NULL COMMENT '图片路径',
  `video_path` varchar(255) DEFAULT NULL COMMENT '视频路径',
  `check_flag` tinyint(4) DEFAULT NULL COMMENT '检测标记（0-无法判断，1-正检，2-误检）',
  `check_time` datetime DEFAULT NULL COMMENT '核检时间',
  `check_source` varchar(64) DEFAULT NULL COMMENT '核检来源：general、psw等',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_alarm_image_video` (`alarm_id`,`image_path`,`video_path`),
  KEY `idx_alarm_id` (`alarm_id`),
  KEY `idx_check_flag` (`check_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=70774 DEFAULT CHARSET=utf8mb4 COMMENT='算法核检结果表'