package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("algorithm_check_result")
public class CheckAlarmResult {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("alarm_id")
    private String alarmId;

    @TableField("image_path")
    private String imagePath;

    @TableField("video_path")
    private String videoPath;

    @TableField("check_flag")
    private Integer checkFlag; // 0-无法判断，1-正检，2-误检

    @TableField("check_name")
    private String checkName; //检测物品名称

    @TableField("check_source")
    private String checkSource; //检测结果来源

    @TableField("check_time")
    private LocalDateTime checkTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}