package com.yuce.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 经纬度距离计算工具类（基于WGS84坐标系，地球半径6371km）
 * 兼容Java 8及以上版本
 */
public class LatLngDistanceUtil{
    // 地球平均半径（单位：千米），WGS84坐标系标准值
    private static final double EARTH_RADIUS = 6371.0088;

    /**
     * 将度分秒格式的经纬度转为十进制度（如 120°30'15" → 120.504167°）
     * @param degree 度
     * @param minute 分
     * @param second 秒
     * @return 十进制度
     */
    public static double dms2dd(int degree, int minute, double second) {
        return degree + minute / 60.0 + second / 3600.0;
    }

    /**
     * 将十进制度转为弧度（三角函数计算需要）
     * @param degree 十进制度
     * @return 弧度值
     */
    private static double degree2Radian(double degree) {
        return Math.toRadians(degree);
    }

    /**
     * 计算两组经纬度的距离（默认返回千米，保留4位小数）
     * @param lat1 第一点纬度（十进制度）
     * @param lng1 第一点经度（十进制度）
     * @param lat2 第二点纬度（十进制度）
     * @param lng2 第二点经度（十进制度）
     * @return 两点间距离（千米）
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // 空值/异常值校验
        if (lat1 < -90 || lat1 > 90 || lat2 < -90 || lat2 > 90 ||
                lng1 < -180 || lng1 > 180 || lng2 < -180 || lng2 > 180) {
            throw new IllegalArgumentException("经纬度超出合法范围（纬度：-90~90，经度：-180~180）");
        }

        // 转为弧度
        double radLat1 = degree2Radian(lat1);
        double radLng1 = degree2Radian(lng1);
        double radLat2 = degree2Radian(lat2);
        double radLng2 = degree2Radian(lng2);

        // Haversine公式核心计算
        double deltaLat = radLat2 - radLat1;
        double deltaLng = radLng2 - radLng1;
        double sinHalfDeltaLat = Math.sin(deltaLat / 2);
        double sinHalfDeltaLng = Math.sin(deltaLng / 2);

        double a = sinHalfDeltaLat * sinHalfDeltaLat +
                Math.cos(radLat1) * Math.cos(radLat2) *
                        sinHalfDeltaLng * sinHalfDeltaLng;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 计算距离（千米），并保留4位小数避免精度冗余
        BigDecimal distance = new BigDecimal(EARTH_RADIUS * c)
                .setScale(4, RoundingMode.HALF_UP);
        return distance.doubleValue();
    }

    /**
     * 计算距离（支持切换单位）
     * @param lat1 纬度1
     * @param lng1 经度1
     * @param lat2 纬度2
     * @param lng2 经度2
     * @param unit 单位（km：千米，m：米，mile：英里）
     * @return 对应单位的距离
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2, String unit) {
        double kmDistance = calculateDistance(lat1, lng1, lat2, lng2);
        // 替换Java 14+的switch表达式为Java 8支持的switch语句
        String lowerUnit = unit.toLowerCase();
        switch (lowerUnit) {
            case "m":
                return kmDistance * 1000; // 转为米
            case "mile":
                return kmDistance * 0.621371; // 转为英里
            case "km":
                return kmDistance; // 默认千米
            default:
                throw new IllegalArgumentException("不支持的单位：" + unit + "，仅支持km/m/mile");
        }
    }
}