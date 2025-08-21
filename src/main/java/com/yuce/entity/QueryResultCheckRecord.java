package com.yuce.entity;

import lombok.Data;
import java.time.LocalDateTime;

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
    private Long tblId; // 主键ID，自增
    private String alarmId; // 告警id
    private String imagePath; // 图片路径
    private String videoPath; // 视频路径
    private String eventId; // 事件ID
    private String nameInp; // 名称输入
    private String direction; // 方向
    private String directionType; // 方向类型
    private LocalDateTime alarmTime; // 报警时间
    private Integer milestone; // 里程碑
    private Integer endMilestone; // 结束里程碑
    private String roadId; // 道路ID
    private String organizationId; // 组织ID
    private String alertLevel; // 报警级别
    private Double latitude; // 纬度
    private Double longitude; // 经度
    private String eventType; // 事件类型
    private String eventTypeId; // 事件类型ID
    private String content; // 内容
    private String contentCustom; // 自定义内容
    private String source; // 来源
    private String sourceId; // 来源ID
    private String company; // 公司
    private String companyId; // 公司ID
    private String directionDes; // 方向描述
    private String sourceEventId; // 源事件ID
    private String jamSpeed; // 拥堵速度
    private String longTime; // 持续时间
    private String jamDist; // 拥堵距离
    private String weather; // 天气
    private String dealFlag; // 处理标志
    private LocalDateTime dealTime; // 处理时间
    private String readFlag; // 阅读标志
    private LocalDateTime readTime; // 阅读时间
    private String voiceUrl; // 语音URL
    private String userId; // 用户ID
    private LocalDateTime suspendTime; // 暂停时间
    private String serverIp; // 服务器IP
    private String clientIp; // 客户端IP
    private String popupFlag; // 弹窗标志
    private String voiceFlag; // 语音标志
    private String phone; // 电话
    private String frameFlag; // 框架标志
    private LocalDateTime destroyTime; // 销毁时间
    private String alarmPlace; // 报警地点
    private Integer laneIndex; // 车道索引
    private String deviceId; // 设备ID
    private String countermeasures; // 对策
    private String supplement; // 补充信息
    private String relAlarmId; // 关联报警ID
    private String uuid; // 唯一标识
    private String situationalList; // 场景列表
    private String redundance; // 冗余信息
    private String rampId; // 匝道ID
    private String tollId; // 收费站ID
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime createTimeSys; // 系统创建时间
    private LocalDateTime modifyTimeSys; // 系统修改时间
    private Integer consumeTag; // 消费标记
    private LocalDateTime dbCreateTime; // 数据库创建时间
    private LocalDateTime dbUpdateTime;
    private int checkFlag; //检查表示
    private int featureId; //特征要素主键
    private int adviceFlag; //建议标识
    private int personCheckFlag; //人工打标标签
    private String adviceReason; //建议依据
    private String short_name; //路段名称简写
}