package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alarm_collection_v2")
public class AlarmCollection {

    @TableId(type = IdType.AUTO)
    private Integer id; // 自增主键ID

    @TableField("collection_id")
    private String collectionId; // 告警集ID，UUID格式

    @TableField("road_id")
    private String roadId; // 告警集对应道路编码

    @TableField("device_id")
    private String deviceId; // 告警集点位设备ID

    @TableField("device_name")
    private String deviceName; // 告警集点位名称

    @TableField("milestone")
    private Integer milestone; // 设备对应桩米号

    @TableField("event_type")
    private String eventType; // 告警集事件类型

    @TableField("disposal_advice")
    private Integer disposalAdvice; // 处置建议：0-无法判断、1-疑似误报、2-尽快确认

    @TableField("collection_type")
    private Integer collectionType; // 告警集类型：0-无法判断，1-正检，2-误检

    @TableField("related_id_list")
    private String relatedIdList; // 关联告警id列表

    @TableField("earliest_alarm_time")
    private LocalDateTime earliestAlarmTime; // 关联最早告警时间

    @TableField("latest_alarm_time")
    private LocalDateTime latestAlarmTime; // 关联最新告警时间

    @TableField("related_alarm_num")
    private Integer relatedAlarmNum; // 关联告警数量

    @TableField("collection_status")
    private Integer collectionStatus; // 告警集状态：1-使用中，2-已关闭

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 告警集创建时间

    @TableField(value = "modify_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime; // 告警集更新时间
}