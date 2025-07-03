CREATE TABLE `extract_point_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `alarm_id` VARCHAR(255) NOT NULL COMMENT '告警id',
    `image_path` VARCHAR(255) NOT NULL COMMENT '图片路径',
    `video_path` VARCHAR(255) NOT NULL COMMENT '视频路径',
    `image_id` VARCHAR(255) NOT NULL COMMENT '图片ID',
    `status` TINYINT DEFAULT 0 COMMENT '状态',
    `point1_x` INT NOT NULL COMMENT '左上角X坐标',
    `point1_y` INT NOT NULL COMMENT '左上角Y坐标',
    `point2_x` INT NOT NULL COMMENT '右下角X坐标',
    `point2_y` INT NOT NULL COMMENT '右下角Y坐标',
    `received_time` DATETIME COMMENT '算法接收时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `modify_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    INDEX `idx_alarm_id` (`alarm_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片检测框记录表';