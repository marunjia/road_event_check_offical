package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("kafka_original_alarm_record")
public class OriginalAlarmRecord implements Serializable {

    @TableId(value = "tbl_id", type = IdType.AUTO)
    private Long tblId; // 自增主键

    @TableField("alarm_id")
    private String id; // 原始数据中的业务ID

    @TableField("image_path")
    private String imagePath;

    @TableField("video_path")
    private String videoPath;

    @TableField("event_id")
    private String eventId;

    @TableField("name_inp")
    private String nameInp;

    @TableField("direction")
    private String direction;

    @TableField("direction_type")
    private String directionType;

    @TableField("alarm_time")
    private LocalDateTime alarmTime;

    @TableField("milestone")
    private Integer milestone;

    @TableField("end_milestone")
    private Integer endMilestone;

    @TableField("road_id")
    private String roadId;

    @TableField("organization_id")
    private String organizationId;

    @TableField("alert_level")
    private String alertLevel;

    @TableField("latitude")
    private Double latitude;

    @TableField("longitude")
    private Double longitude;

    @TableField("event_type")
    private String eventType;

    @TableField("event_type_id")
    private String eventTypeId;

    @TableField("content")
    private String content;

    @TableField("content_custom")
    private String contentCustom;

    @TableField("source")
    private String source;

    @TableField("source_id")
    private String sourceId;

    @TableField("company")
    private String company;

    @TableField("company_id")
    private String companyId;

    @TableField("direction_des")
    private String directionDes;

    @TableField("source_event_id")
    private String sourceEventId;

    @TableField("jam_speed")
    private String jamSpeed;

    @TableField("long_time")
    private String longTime;

    @TableField("jam_dist")
    private String jamDist;

    @TableField("weather")
    private String weather;

    @TableField("deal_flag")
    private String dealFlag;

    @TableField("deal_time")
    private Date dealTime;

    @TableField("read_flag")
    private String readFlag;

    @TableField("read_time")
    private Date readTime;

    @TableField("voice_url")
    private String voiceUrl;

    @TableField("user_id")
    private String userId;

    @TableField("suspend_time")
    private Date suspendTime;

    @TableField("server_ip")
    private String serverIp;

    @TableField("client_ip")
    private String clientIp;

    @TableField("popup_flag")
    private String popupFlag;

    @TableField("voice_flag")
    private String voiceFlag;

    @TableField("phone")
    private String phone;

    @TableField("frame_flag")
    private String frameFlag;

    @TableField("destroy_time")
    private Date destroyTime;

    @TableField("alarm_place")
    private String alarmPlace;

    @TableField("lane_index")
    private Integer laneIndex;

    @TableField("device_id")
    private String deviceId;

    @TableField("countermeasures")
    private String countermeasures;

    @TableField("supplement")
    private String supplement;

    @TableField("rel_alarm_id")
    private String relAlarmId;

    @TableField("uuid")
    private String uuid;

    @TableField("situational_list")
    private String situationalList;

    @TableField("redundance")
    private String redundance;

    @TableField("ramp_id")
    private String rampId;

    @TableField("toll_id")
    private String tollId;

    @TableField("create_time")
    private String createTime;

    @TableField("create_time_sys")
    private String createTimeSys;

    @TableField("modify_time_sys")
    private String modifyTimeSys;

    @TableField("consume_tag")
    private Integer consumeTag;

    @TableField(value = "db_create_time", fill = FieldFill.INSERT)
    private LocalDateTime dbCreateTime;

    @TableField(value = "db_update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime dbUpdateTime;
}