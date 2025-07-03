CREATE TABLE IF NOT EXISTS `algorithm_check_process` (
   `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
   `alarm_id` VARCHAR(255) COMMENT '关联告警记录id',
   `image_id` VARCHAR(64) NOT NULL COMMENT '图像 ID',
   `received_time` DATETIME NOT NULL COMMENT '接收时间',
   `status` TINYINT NOT NULL DEFAULT 0 COMMENT '图像状态',
   `completed_time` DATETIME NOT NULL COMMENT '检测完成时间',
   `type` VARCHAR(32) NOT NULL COMMENT '检测类型',
   `name` VARCHAR(32) NOT NULL COMMENT '目标名称',
   `score` DECIMAL(6,4) NOT NULL COMMENT '置信度分数',
   `point1_x` INT NOT NULL COMMENT '第一个点的 x 坐标',
   `point1_y` INT NOT NULL COMMENT '第一个点的 y 坐标',
   `point2_x` INT NOT NULL COMMENT '第二个点的 x 坐标',
   `point2_y` INT NOT NULL COMMENT '第二个点的 y 坐标',
   `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
   `modify_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图像检测数据简化表';