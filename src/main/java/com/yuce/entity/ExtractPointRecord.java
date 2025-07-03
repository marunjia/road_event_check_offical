package com.yuce.entity;

/**
 * @ClassName ExtractPointRecord
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/17 17:45
 * @Version 1.0
 */
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("extract_point_record")
public class ExtractPointRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("alarm_id")
    private String alarmId;

    @TableField("image_path")
    private String imagePath;

    @TableField("video_path")
    private String videoPath;

    @TableField("image_id")
    private String imageId;

    @TableField("status")
    private Integer status;

    @TableField("point1_x")
    private Integer point1X;

    @TableField("point1_y")
    private Integer point1Y;

    @TableField("point2_x")
    private Integer point2X;

    @TableField("point2_y")
    private Integer point2Y;

    @TableField("received_time")
    private LocalDateTime receivedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;
}