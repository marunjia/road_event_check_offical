package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("algorithm_check_process")
public class CheckAlarmProcess implements Serializable {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联告警记录id */
    @TableField("alarm_id")
    private String alarmId;

    /**抽帧图片编号*/
    @TableField("image_id")
    private String imageId;

    /**原始图片地址*/
    @TableField("image_path")
    private String imagePath;

    /** 视频地址*/
    @TableField("video_path")
    private String videoPath;

    /** 接收时间 */
    @TableField("received_time")
    private LocalDateTime receivedTime;

    /** 图像状态 */
    @TableField("status")
    private Integer status;

    /** 检测完成时间 */
    @TableField("completed_time")
    private LocalDateTime completedTime;

    /** 检测类型 */
    @TableField("type")
    private String type;

    /** 目标名称 */
    @TableField("name")
    private String name;

    /** 置信度分数 */
    @TableField("score")
    private BigDecimal score;

    /** 第一个点的 x 坐标 */
    @TableField("point1_x")
    private Integer point1X;

    /** 第一个点的 y 坐标 */
    @TableField("point1_y")
    private Integer point1Y;

    /** 第二个点的 x 坐标 */
    @TableField("point2_x")
    private Integer point2X;

    /** 第二个点的 y 坐标 */
    @TableField("point2_y")
    private Integer point2Y;

    /** 第二个点的 y 坐标 */
    @TableField("iou")
    private double iou;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "modify_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;
}