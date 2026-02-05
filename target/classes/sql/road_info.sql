CREATE TABLE `road_info` (
    `id` bigint(20) NOT NULL COMMENT '主键ID',
    `alias_id` bigint(20) DEFAULT NULL COMMENT '别名ID',
    `milestone` bigint(20) DEFAULT NULL COMMENT '起始桩号',
    `end_milestone` bigint(20) DEFAULT NULL COMMENT '终点桩号',
    `name_inp` varchar(255) DEFAULT NULL COMMENT '路段名称（含编号）',
    `short_name` varchar(100) DEFAULT NULL COMMENT '路段简称',
    `new_gb_code` varchar(50) DEFAULT NULL COMMENT '国标编码',
    `new_gb_name` varchar(255) DEFAULT NULL COMMENT '国标名称',
    `road_level` int(11) DEFAULT NULL COMMENT '道路等级',
    `in_group` bigint(20) DEFAULT NULL COMMENT '所属分组',
    `plate` bigint(20) DEFAULT NULL COMMENT '所属区域/牌照代码',
    `user_id` varchar(64) DEFAULT NULL COMMENT '创建人用户ID',
    `des` text COMMENT '备注描述',
    `sort` int(11) DEFAULT '0' COMMENT '排序字段',
    `del_flag` tinyint(4) DEFAULT '0' COMMENT '删除标志（0-未删除，1-已删除）',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='道路信息表'

#迭代目标
# 1、增加桩米号字段信息
# 2、增加
