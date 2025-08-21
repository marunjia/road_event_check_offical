package com.yuce.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.CloudEyesDevice;
import com.yuce.mapper.CloudEyesDeviceMapper;
import com.yuce.service.CloudEyesDeviceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName CloudEyesDeviceServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/20 16:03
 * @Version 1.0
 */

@Slf4j
@Service
@DS("secondary")
public class CloudEyesDeviceServiceImpl extends ServiceImpl<CloudEyesDeviceMapper, CloudEyesDevice> implements CloudEyesDeviceService {

    @Autowired
    private CloudEyesDeviceMapper cloudEyesDeviceMapper;

    @Transactional
    public void refreshDevices(List<String> list) {
        this.remove(null);   // ✅ remove(null) 表示无条件删除全部
        insertDefault();//插入默认设备记录
        List<CloudEyesDevice> deviceList = list.stream()
                .map(id -> {
                    CloudEyesDevice d = new CloudEyesDevice();
                    d.setDeviceId(id);
                    d.setParentId(null);
                    d.setDeviceName("监控点位");
                    d.setDeviceType(-1);
                    d.setDevicePath("/default-ed773c46e141/32010000002001005113");
                    d.setDeviceUrl("");
                    d.setSourceIp("");
                    d.setSourceFrom("3");
                    d.setDriverId("CloudEyesAccessDriver");
                    d.setDeviceCorp("监控点位");
                    d.setAlgoRunServerId("");
                    d.setAlgoRunStatus(0);
                    d.setAlgoRunStatusDesc("");
                    d.setRegionId("default-ed773c46e141");
                    d.setLongitude(0.0);
                    d.setLatitude(0.0);
                    d.setDeviceInfo(null);
                    d.setRoadType(null);
                    d.setPort(null);
                    d.setUserName(null);
                    d.setPassword(null);
                    d.setDeviceMode(1);
                    d.setDataObtainMode(null);
                    d.setModel(null);
                    d.setUpDirectionName(null);
                    d.setDownDirectionName(null);
                    d.setMainRoadDirection(null);
                    d.setKilometerStake(null);
                    d.setHectometerStake(null);
                    d.setDirection(null);
                    d.setRawDeviceCode("32010000002001005113");
                    d.setRawDeviceName("监控点位");
                    d.setRawDevicePath("/32010000002001005113");
                    d.setRawDeviceStatus(0);
                    d.setReferName("/32010000002001005113");
                    d.setRoadUid(null);
                    d.setRoadName(null);
                    d.setSegmentUid(null);
                    d.setInformationPoint("");
                    d.setInformationPointName(null);
                    d.setCongestionIndexUp(0.0);
                    d.setCongestionIndexDown(0.0);
                    d.setDeleteTag(0);
                    d.setCreateTime(LocalDateTime.now());
                    d.setUpdateTime(LocalDateTime.now());
                    return d;
                })
                .collect(Collectors.toList());  // ✅ JDK8 用这个
        this.saveBatch(deviceList);
    }


    /**
     * @desc 插入默认告警记录
     */
    public void insertDefault() {
        CloudEyesDevice device = new CloudEyesDevice();
        device.setDeviceId("32010000002001005113");
        device.setParentId(null);
        device.setDeviceName("监控点位");
        device.setDeviceType(-1);
        device.setDevicePath("/default-ed773c46e141/32010000002001005113");
        device.setDeviceUrl("");
        device.setSourceIp("");
        device.setSourceFrom("3");
        device.setDriverId("CloudEyesAccessDriver");
        device.setDeviceCorp("监控点位");
        device.setAlgoRunServerId("");
        device.setAlgoRunStatus(0);
        device.setAlgoRunStatusDesc("");
        device.setRegionId("default-ed773c46e141");
        device.setLongitude(0.0);
        device.setLatitude(0.0);
        device.setDeviceInfo(null);
        device.setRoadType(null);
        device.setPort(null);
        device.setUserName(null);
        device.setPassword(null);
        device.setDeviceMode(1);
        device.setDataObtainMode(null);
        device.setModel(null);
        device.setUpDirectionName(null);
        device.setDownDirectionName(null);
        device.setMainRoadDirection(null);
        device.setKilometerStake(null);
        device.setHectometerStake(null);
        device.setDirection(null);
        device.setRawDeviceCode("32010000002001005113");
        device.setRawDeviceName("监控点位");
        device.setRawDevicePath("/32010000002001005113");
        device.setRawDeviceStatus(0);
        device.setReferName("/32010000002001005113");
        device.setRoadUid(null);
        device.setRoadName(null);
        device.setSegmentUid("");
        device.setInformationPoint(null);
        device.setInformationPointName(null);
        device.setCongestionIndexUp(0.0);
        device.setCongestionIndexDown(0.0);
        device.setDeleteTag(0);
        device.setCreateTime(LocalDateTime.parse("2022-07-22T08:49:04"));
        device.setUpdateTime(LocalDateTime.parse("2022-07-22T15:30:37"));

        // 调用 Mapper 插入
        cloudEyesDeviceMapper.insert(device);
    }
}