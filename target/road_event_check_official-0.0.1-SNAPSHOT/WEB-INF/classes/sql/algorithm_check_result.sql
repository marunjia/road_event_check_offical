CREATE TABLE IF NOT EXISTS `algorithm_check_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID，自增',
  `alarm_id` varchar(64) NOT NULL COMMENT '关联告警事件ID',
  `check_flag` tinyint(4) DEFAULT NULL COMMENT '检测标记（0-无法判断，1-正检，2-误检）',
  `check_time` datetime DEFAULT NULL COMMENT '核检时间',
  `check_source` datetime DEFAULT NULL COMMENT '核检来源：general、psw等',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='算法核检结果表'

-- 为 alarm_id 添加单列索引（若不存在）
CREATE INDEX idx_alarm_id ON algorithm_check_result(alarm_id);

-- 为 check_flag 添加单列索引（若不存在）
CREATE INDEX idx_check_flag ON algorithm_check_result(check_flag);

-- 添加联合索引 alarm_id + check_flag（组合查询更高效）
CREATE INDEX idx_alarm_check ON algorithm_check_result(alarm_id, check_flag);