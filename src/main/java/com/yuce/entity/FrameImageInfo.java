package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alarm_frame_image_info")
public class FrameImageInfo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id; // 主键ID，自增

    @TableField("alarm_id")
    private String alarmId; // 关联告警事件ID（外键）

    @TableField("image_path")
    private String imagePath; // 图片链接

    @TableField("video_path")
    private String videoPath; // 视频链接

    @TableField("frame_num")
    private Integer frameNum; // 抽帧帧数

    @TableField("image_sort_no")
    private Integer imageSortNo; // 图片排序编号

    @TableField("image_url")
    private String imageUrl; // 图片URL

    @TableField("create_time")
    private LocalDateTime createTime; // 记录创建时间

    @TableField("update_time")
    private LocalDateTime updateTime; // 记录更新时间
}