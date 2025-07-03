DROP TABLE IF EXISTS `extract_image_record`;
CREATE TABLE `extract_image_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alarm_id` VARCHAR(64) DEFAULT NULL COMMENT '告警ID',
    `image_path` VARCHAR(255) DEFAULT NULL COMMENT '图片路径',
    `video_path` VARCHAR(255) DEFAULT NULL COMMENT '视频路径',
    `image_id` VARCHAR(64) DEFAULT NULL COMMENT '图片ID',
    `image_url` VARCHAR(255) DEFAULT NULL COMMENT '抠图目标图片对应的URL',
    `cropped_image_url` VARCHAR(255) DEFAULT NULL COMMENT '抠图结果图片对应的URL',
    `point1_x` INT DEFAULT NULL COMMENT '左上角X坐标',
    `point1_y` INT DEFAULT NULL COMMENT '左上角Y坐标',
    `point2_x` INT DEFAULT NULL COMMENT '右下角X坐标',
    `point2_y` INT DEFAULT NULL COMMENT '右下角Y坐标',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`),
    KEY `idx_alarm_image_video` (`alarm_id`, `image_path`, `video_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抠图检测框记录表';