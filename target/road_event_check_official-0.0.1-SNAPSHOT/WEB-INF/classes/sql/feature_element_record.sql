CREATE TABLE `feature_element_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `alarm_id` VARCHAR(64) COMMENT '告警ID，关联外部告警表',
  `abnormal_location` VARCHAR(64) COMMENT '异常位置，例如：第1车道、应急车道、硬路肩、道路外',
  `abnormal_type` VARCHAR(64) COMMENT '异常类型，例如：停驶、行人、抛洒物等',
  `involved_vehicle_info` TEXT COMMENT '涉事车辆类型及数量，JSON结构表示，例如：{"小轿车":2,"货车":1}',
  `involved_person_info` TEXT COMMENT '涉事人员类型及数量，JSON结构表示，例如：{"行人":1,"施工人":2}',
  `weather_condition` VARCHAR(16) COMMENT '天气状况：晴、雨、雪',
  `lane_occupy_info` TEXT COMMENT '占道情况，包含交集车道数量及通行状态，JSON结构，例如：{"交集车道数":2,"通行情况":["缓行","阻塞"]}',
  `rescue_force` TEXT COMMENT '施救力量，画面中出现的工程救援车辆和施工人员，JSON结构表示',
  `congestion_status` VARCHAR(32) COMMENT '拥堵情况，例如：无拥堵、轻度拥堵、严重拥堵',
  `danger_element` VARCHAR(64) COMMENT '危险要素，例如：冒烟、起火、无',
  `disposal_advice` INT COMMENT '处置建议：0-无法判断、1-疑似误报、2-尽快确认、3-无需处理',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `modify_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特征要素记录表';