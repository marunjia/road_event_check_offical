package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * @ClassName FeatureElement
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/6 11:59
 * @Version 1.0
 */

@Data
@TableName("feature_element_record")
public class FeatureElementRecord {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID

    private Long tblId;

    private String alarmId; // 告警ID，关联外部告警表

    private String imagePath; // 告警ID，关联外部告警表

    private String videoPath; // 告警ID，关联外部告警表

    private String abnormalLocation; // 异常位置，例如：第1车道、应急车道、硬路肩、道路外

    private String abnormalType; // 异常类型，例如：停驶、行人、抛洒物等

    private String involvedVehicleInfo; // 涉事车辆类型及数量，JSON结构，例如：{"小轿车":2,"货车":1}

    private String involvedPersonInfo; // 涉事人员类型及数量，JSON结构，例如：{"行人":1,"施工人":2}

    private String weatherCondition; // 天气状况：晴、雨、雪

    private String laneOccupyInfo; // 占道情况，包含交集车道数量及通行状态，JSON结构

    private String rescueForce; // 施救力量，JSON结构，例如：{"工程车":1,"施工人":2}

    private Integer congestionStatus; // 0:未发生拥堵，1:发生拥堵

    private String dangerElement; // 危险要素，例如：冒烟、起火、无

    private int disposalAdvice; // 处置建议，例如：尽快确认、无需处理、疑似误报、无法判断

    private String adviceReason; // 处置建议依据

    private String alarmElement; // 告警物体

    private String alarmElementRange; // 告警物附近物体

    private Integer collectionMatchStatus; //告警集关联状态：1关联正确，2关联错误

    private Integer personCheckFlag; // 人工检验标签:1未核查，2确认事件，3无需处理，4误报

    private Integer matchCheckFlag; // 关联是否正确：0未核查、1是、2否

    private String matchCheckReason; // 关联错误原因

    private Integer matchCollectionId; // 关联告警集id

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;
}