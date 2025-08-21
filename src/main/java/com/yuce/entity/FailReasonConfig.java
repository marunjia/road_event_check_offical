package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 失败原因配置表实体类
 */
@Data
@TableName("fail_reason_config")
public class FailReasonConfig {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原因描述
     */
    private String reason;

    /**
     * 原因来源（可自定义枚举值，如1：告警记录；2：告警集等）
     */
    private Integer source;

    /**
     * 创建时间（自动填充）
     */
    @TableField(value = "create_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改时间（自动填充）
     */
    @TableField(value = "modify_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;
}