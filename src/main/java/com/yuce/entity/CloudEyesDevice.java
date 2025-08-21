package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 云眼点位表
 */
@Data
@TableName("cloud_eyes_devices")
public class CloudEyesDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "device_id", type = IdType.INPUT)
    private String deviceId;   // 点位id, 重复自动生成

    private String parentId;   // 点位父id

    private String deviceName; // 点位名称

    private Integer deviceType; // 设备类型 0-根节点 10-子节点 20-点位 30-服务区外场摄像头

    private String devicePath; // 点位路径：区域id+点位路径

    private String deviceUrl;  // 点位 rtsp 地址 或者 SIP 值

    private String sourceIp;   // 设备信息上报IP

    private String sourceFrom; // 数据来源：1：致捷  2：接口接入 3：手动添加 4：云眼

    private String driverId;   // 点位接入驱动id

    private String deviceCorp; // 设备厂商(名)

    private String algoRunServerId; // 算法运行服务器id

    private Integer algoRunStatus;  // 算法真实运行状态 0-关闭 1-开启 2-异常

    private String algoRunStatusDesc; // 算法真实运行状态描述

    private String regionId;   // 区域id

    private Double longitude;  // 经度

    private Double latitude;   // 纬度

    private String deviceInfo; // 点位详细信息

    private Integer roadType;  // 点位所在道路类型 1-高速 2-城市道路 3-服务区

    private Integer port;      // 端口

    private String userName;   // 用户名

    private String password;   // 密码

    private Integer deviceMode; // 业务上的设备模式 0-卡口抓拍 1-视频监控

    private Integer dataObtainMode; // 数据获取模式 0-自动获取 1-主动抓取 2-停用

    private String model;      // 设备型号

    private String upDirectionName;   // 上行方向名称

    private String downDirectionName; // 下行方向名称

    private String mainRoadDirection; // 主要道路方向

    private String kilometerStake;    // 千米桩

    private String hectometerStake;   // 百米桩

    private Integer direction; // 方向 0-上行,1-下行,2-上下行

    private String rawDeviceCode; // 原始点位编号

    private String rawDeviceName; // 原始点位名称

    private String rawDevicePath; // 原始点位路径

    private Integer rawDeviceStatus; // 原始点位状态 0-已断开 1-在线 2-离线

    private String referName;  // 关联名称-zhijie服务获取名称

    private String roadUid;    // 路网：设备道路UID

    private String roadName;   // 路网：设备道路名称

    private String segmentUid; // 路网：路段UID

    private String informationPoint; // 信息点

    private String informationPointName; // 信息点名称

    private Double congestionIndexUp;   // 拥堵指数上行

    private Double congestionIndexDown; // 拥堵指数下行

    private Integer deleteTag; // 删除标志位 0-未删 1-已删

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime; // 创建时间

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime; // 更新时间

    private String openUserId; // 开启点位的用户id

    private Integer distance;  // 设备之间距离
}