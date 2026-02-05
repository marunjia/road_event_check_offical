package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alarm_collection_v2")
public class AlarmCollection {

    @TableId(type = IdType.AUTO)
    private Integer id; // 自增主键ID

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

    @TableField("related_tbl_id_list")
    private String relatedTblIdList; // 关联告警tbl_id列表

    @TableField("related_alarm_id_list")
    private String relatedAlarmIdList; // 关联告警alarm_id列表

    @TableField("earliest_alarm_time")
    private LocalDateTime earliestAlarmTime; // 关联最早告警时间

    @TableField("latest_alarm_time")
    private LocalDateTime latestAlarmTime; // 关联最新告警时间

    @TableField("related_alarm_num")
    private Integer relatedAlarmNum; // 关联告警数量

    @TableField("collection_status")
    private Integer collectionStatus; // 告警集状态：1-使用中，2-已关闭

    @TableField("person_check_flag")
    private Integer personCheckFlag; // 0未核查、1是、2否

    @TableField("right_check_num")
    private Integer rightCheckNum; // 正检告警记录数量

    @TableField("person_check_reason")
    private String personCheckReason; // 人工核查关联是否正确

    @TableField("related_source_type")
    private Integer relatedSourceType; //关联告警记录来源类型：1：正常告警集、2：摩托车类告警集

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 告警集创建时间

    @TableField(value = "modify_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime; // 告警集更新时间
}