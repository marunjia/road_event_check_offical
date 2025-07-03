package com.yuce.entity;

import lombok.Data;
import java.util.Date;

/**
 * @ClassName QueryResultCheckRecord
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/4 16:04
 * @Version 1.0
 */

@Data
public class QueryResultCheckRecord {
    private String id; // 主键ID

    private String eventId;

    private String nameInp;

    private String direction;

    private String directionType;

    private Date alarmTime;

    private Integer milestone;

    private Integer endMilestone;

    private String roadId;

    private String organizationId;

    private String alertLevel;

    private Double latitude;

    private Double longitude;

    private String eventType;

    private String eventTypeId;

    private String content;

    private String contentCustom;

    private String imagePath;

    private String videoPath;

    private String source;

    private String sourceId;

    private String company;

    private String companyId;

    private String directionDes;

    private String sourceEventId;

    private String jamSpeed;

    private String longTime;

    private String jamDist;

    private String weather;

    private String dealFlag;

    private Date dealTime;

    private String readFlag;

    private Date readTime;

    private String voiceUrl;

    private String userId;

    private Date suspendTime;

    private String serverIp;

    private String clientIp;

    private String popupFlag;

    private String voiceFlag;

    private String phone;

    private String frameFlag;

    private Date destroyTime;

    private String alarmPlace;

    private Integer laneIndex;

    private String deviceId;

    private String countermeasures;

    private String supplement;

    private String relAlarmId;

    private String uuid;

    private String situationalList;

    private String redundance;

    private String rampId;

    private String tollId;

    private Date createTime;

    private Date createTimeSys;

    private Date modifyTimeSys;

    private Date dbCreateTime;

    private Date dbUpdateTime;

    private int checkFlag;

    private int adviceFlag;
}