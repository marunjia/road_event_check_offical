CREATE TABLE IF NOT EXISTS `road_info` (
    `id` BIGINT PRIMARY KEY COMMENT '主键ID',
    `alias_id` BIGINT COMMENT '别名ID',
    `milestone` BIGINT COMMENT '起始桩号',
    `end_milestone` BIGINT COMMENT '终点桩号',
    `name_inp` VARCHAR(255) COMMENT '路段名称（含编号）',
    `short_name` VARCHAR(100) COMMENT '路段简称',
    `new_gb_code` VARCHAR(50) COMMENT '国标编码',
    `new_gb_name` VARCHAR(255) COMMENT '国标名称',
    `road_level` INT COMMENT '道路等级',
    `in_group` BIGINT COMMENT '所属分组',
    `plate` BIGINT COMMENT '所属区域/牌照代码',
    `user_id` VARCHAR(64) COMMENT '创建人用户ID',
    `des` TEXT COMMENT '备注描述',
    `sort` INT DEFAULT 0 COMMENT '排序字段',
    `del_flag` TINYINT DEFAULT 0 COMMENT '删除标志（0-未删除，1-已删除）',
    `create_time` DATETIME COMMENT '创建时间',
    `update_time` DATETIME COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='道路信息表';