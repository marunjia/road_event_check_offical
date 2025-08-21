package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 路面检测记录实体类
 */
@Data
@TableName("road_check_record")
public class RoadCheckRecord {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 告警记录ID
     */
    @TableField("alarm_id")
    private String alarmId;

    /**
     * 图片路径
     */
    @TableField("image_path")
    private String imagePath;

    /**
     * 视频路径
     */
    @TableField("video_path")
    private String videoPath;

    /**
     * 提取图片URL
     */
    @TableField("extract_image_url")
    private String extractImageUrl;

    /**
     * 图片ID
     */
    @TableField("image_id")
    private String imageId;

    /**
     * 检测状态（0：未处理，1：已处理等）
     */
    @TableField("status")
    private Integer status;

    /**
     * 检测类型（如 road、lane 等）
     */
    @TableField("type")
    private String type;

    /**
     * 检测对象名称
     */
    @TableField("name")
    private String name;

    /**
     * 检测区域的坐标点数组（JSON 格式）
     */
    @TableField("points")
    private String points;

    /**
     * 重叠像素数占比
     */
    @TableField("percent")
    private double percent;

    /**
     * 路面检测结果标签（1、路面内；2、路面外）
     */
    @TableField("road_check_flag")
    private Integer roadCheckFlag;

    /**
     * 提框坐标
     */
    @TableField("extract_point1_x")
    private Integer extractPoint1X;

    /**
     * 提框坐标
     */
    @TableField("extract_point1_y")
    private Integer extractPoint1Y;

    /**
     * 提框坐标
     */
    @TableField("extract_point2_x")
    private Integer extractPoint2X;

    /**
     * 提框坐标
     */
    @TableField("extract_point2_y")
    private Integer extractPoint2Y;


    /**
     * 记录创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 记录更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}