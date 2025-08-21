package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警集分组记录表实体类
 */
@Data
@TableName("collection_group_record")
public class CollectionGroupRecord {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 归属告警集id（非空）
     */
    private String collectionId;

    /**
     * 分组ID（非空）
     */
    private String groupId;

    /**
     * 分组图片
     */
    private String groupImageUrl;

    /**
     * 分组物品类型
     */
    private String groupItemType;

    /**
     * 告警记录唯一标识
     */
    private long tblId;

    /**
     * 告警ID
     */
    private String alarmId;

    /**
     * 图片路径
     */
    private String imagePath;

    /**
     * 视频路径
     */
    private String videoPath;

    /**
     * 告警类型
     */
    private String eventType;

    /**
     * 告警时间
     */
    private LocalDateTime alarmTime;

    /**
     * 记录创建时间（自动填充）
     */
    @TableField(value = "create_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 记录修改时间（自动填充）
     */
    @TableField(value = "modify_time", fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;
}