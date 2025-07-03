package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抠图检测框记录表实体类
 */
@Data
@TableName("extract_image_record")
public class ExtractImageRecord {

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

    @TableField("image_url")//抠图目标图片对应的url
    private String imageUrl;

    @TableField("cropped_image_url")//抠图结果图片对应的url
    private String croppedImageUrl;

    @TableField("point1_x")
    private Integer point1X;

    @TableField("point1_y")
    private Integer point1Y;

    @TableField("point2_x")
    private Integer point2X;

    @TableField("point2_y")
    private Integer point2Y;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;
}