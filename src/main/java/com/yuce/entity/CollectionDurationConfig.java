package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.yuce.validation.AddGroup;
import com.yuce.validation.UpdateGroup;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * @ClassName CollectionDurationConfig
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/14 18:00
 * @Version 1.0
 */

@Data
@TableName("alarm_collection_duration_config")
public class CollectionDurationConfig {

    @TableId(type = IdType.AUTO)
    @NotNull(message = "配置id不能为空",groups = {UpdateGroup.class})
    private Integer id; // 主键ID

    @TableField("duration_minutes")
    @NotNull(message = "告警集时长不能为空",groups = {UpdateGroup.class})
    private Integer durationMinutes; // 告警集时长（分钟）

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @TableField(value = "modify_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime; // 更新时间
}