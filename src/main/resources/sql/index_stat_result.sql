CREATE TABLE `index_stat_result` (
 `id` INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
 `stat_date` DATE NOT NULL COMMENT '统计日期',
 `index_name` VARCHAR(255) NOT NULL COMMENT '指标名称',
 `index_type` int(10) NOT NULL COMMENT '指标标签',
 `numerator` BIGINT DEFAULT 0 COMMENT '分子（符合条件的告警数）',
 `denominator` BIGINT DEFAULT 0 COMMENT '分母（总告警数）',
 `result` DECIMAL(10,4) DEFAULT NULL COMMENT '计算结果（numerator/denominator）',
 `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `modify_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
 UNIQUE KEY `uk_stat_date_index_type` (`stat_date`, `index_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='指标统计结果表';