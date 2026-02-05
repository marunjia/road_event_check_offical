package com.yuce.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 多边形坐标缩放工具类
 * 功能：以坐标集合的最小外接矩形中心点为基准，全方位均匀缩放（扩大/缩小），适配任意不规则多边形
 * 兼容：整数坐标、小数坐标，凸多边形、凹多边形
 */
public class PolygonCoordinateScaleUtil {
    // 静态Jackson实例（复用，提升序列化/反序列化效率）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 默认缩放比例：扩大10%（如需缩小，传入0.9即缩小10%）
     */
    public static final double DEFAULT_SCALE = 1.1;

    /**
     * 核心方法：缩放多边形坐标（默认扩大10%）
     * @param points 输入坐标JSON字符串（格式：[{"x":516,"y":95},...]，支持整数/小数）
     * @return 缩放后的坐标JSON字符串（与输入格式一致）
     * @throws Exception 输入参数异常、JSON解析异常、坐标格式异常
     */
    public static String scalePoints(String points) throws Exception {
        return scalePoints(points, DEFAULT_SCALE, false);
    }

    /**
     * 重载方法：自定义缩放比例，支持过滤异常顶点
     * @param points 输入坐标JSON字符串（格式：[{"x":516,"y":95},...]）
     * @param scale 缩放比例（>1 扩大，<1 缩小，如1.2=扩大20%，0.8=缩小20%）
     * @param filterAbnormalPoints 是否过滤异常顶点（true=过滤远离整体区域的孤立点）
     * @return 缩放后的坐标JSON字符串
     * @throws Exception 输入参数异常、JSON解析异常、坐标格式异常
     */
    public static String scalePoints(String points, double scale, boolean filterAbnormalPoints) throws Exception {
        // 1. 校验输入参数
        validateInput(points, scale);

        // 2. 解析JSON为坐标列表（支持Integer/Double类型的x/y）
        List<Map<String, Number>> originalCoords = OBJECT_MAPPER.readValue(
                points, new TypeReference<List<Map<String, Number>>>() {});

        // 3. 校验坐标格式（必须包含x和y字段）
        validateCoordinateFormat(originalCoords);

        // 4. 可选：过滤异常顶点（避免孤立点导致边界框异常）
        List<Map<String, Number>> targetCoords = filterAbnormalPoints ?
                filterAbnormalCoordinates(originalCoords) : originalCoords;

        // 5. 计算最小外接矩形（边界框）和中心点
        BoundingBox boundingBox = calculateBoundingBox(targetCoords);
        double centerX = boundingBox.getCenterX();
        double centerY = boundingBox.getCenterY();

        // 6. 遍历所有顶点，基于中心点缩放
        List<Map<String, Number>> scaledCoords = scaleEachCoordinate(targetCoords, centerX, centerY, scale);

        // 7. 序列化回JSON字符串（保持与输入一致的格式，不额外格式化）
        return OBJECT_MAPPER.writeValueAsString(scaledCoords);
    }

    /**
     * 输入参数校验
     * @param points 坐标JSON字符串
     * @param scale 缩放比例
     * @throws IllegalArgumentException 参数异常
     */
    private static void validateInput(String points, double scale) {
        if (points == null || points.trim().isEmpty()) {
            throw new IllegalArgumentException("坐标字符串不能为空");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("缩放比例必须大于0（当前值：" + scale + "）");
        }
    }

    /**
     * 校验坐标格式（每个坐标必须包含x和y字段）
     * @param coords 坐标列表
     * @throws IllegalArgumentException 坐标格式异常
     */
    private static void validateCoordinateFormat(List<Map<String, Number>> coords) {
        for (int i = 0; i < coords.size(); i++) {
            Map<String, Number> coord = coords.get(i);
            if (!coord.containsKey("x") || !coord.containsKey("y")) {
                throw new IllegalArgumentException("第" + (i + 1) + "个坐标格式异常，缺少x或y字段：" + coord);
            }
        }
    }

    /**
     * 过滤异常顶点（基于标准差：过滤超出2倍标准差的点）
     * @param coords 原始坐标列表
     * @return 过滤后的坐标列表
     */
    private static List<Map<String, Number>> filterAbnormalCoordinates(List<Map<String, Number>> coords) {
        if (coords.size() <= 3) {
            // 少于等于3个点（三角形），不过滤（过滤可能导致多边形失效）
            return coords;
        }

        // 计算x坐标的平均值和标准差
        double avgX = coords.stream().mapToDouble(c -> c.get("x").doubleValue()).average().orElse(0);
        double stdX = Math.sqrt(coords.stream()
                .mapToDouble(c -> Math.pow(c.get("x").doubleValue() - avgX, 2))
                .average().orElse(0));

        // 计算y坐标的平均值和标准差
        double avgY = coords.stream().mapToDouble(c -> c.get("y").doubleValue()).average().orElse(0);
        double stdY = Math.sqrt(coords.stream()
                .mapToDouble(c -> Math.pow(c.get("y").doubleValue() - avgY, 2))
                .average().orElse(0));

        // 过滤超出2倍标准差的点（认为是异常孤立点）
        List<Map<String, Number>> filtered = coords.stream()
                .filter(coord -> {
                    double x = coord.get("x").doubleValue();
                    double y = coord.get("y").doubleValue();
                    return Math.abs(x - avgX) <= 2 * stdX && Math.abs(y - avgY) <= 2 * stdY;
                })
                .collect(Collectors.toList());

        // 若过滤后点数过少，返回原始列表（避免破坏多边形结构）
        return filtered.size() >= 3 ? filtered : coords;
    }

    /**
     * 计算坐标集合的最小外接矩形（边界框）
     * @param coords 坐标列表
     * @return 边界框对象（包含minX、maxX、minY、maxY、中心点）
     */
    private static BoundingBox calculateBoundingBox(List<Map<String, Number>> coords) {
        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Map<String, Number> coord : coords) {
            double x = coord.get("x").doubleValue();
            double y = coord.get("y").doubleValue();

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        return new BoundingBox(minX, maxX, minY, maxY);
    }

    /**
     * 遍历每个坐标，基于中心点进行缩放
     * @param coords 坐标列表
     * @param centerX 中心点X坐标
     * @param centerY 中心点Y坐标
     * @param scale 缩放比例
     * @return 缩放后的坐标列表
     */
    private static List<Map<String, Number>> scaleEachCoordinate(List<Map<String, Number>> coords,
                                                                 double centerX, double centerY, double scale) {
        for (Map<String, Number> coord : coords) {
            double originalX = coord.get("x").doubleValue();
            double originalY = coord.get("y").doubleValue();

            // 核心逻辑：中心点 +（原坐标 - 中心点）× 缩放比例
            double scaledX = centerX + (originalX - centerX) * scale;
            double scaledY = centerY + (originalY - centerY) * scale;

            // 保留原始数据类型（整数→整数，小数→小数），避免精度丢失
            if (coord.get("x") instanceof Integer) {
                coord.put("x", Math.round(scaledX)); // 整数坐标四舍五入
            } else {
                coord.put("x", scaledX); // 小数坐标保留原始精度
            }

            if (coord.get("y") instanceof Integer) {
                coord.put("y", Math.round(scaledY));
            } else {
                coord.put("y", scaledY);
            }
        }
        return coords;
    }

    /**
     * 内部静态类：存储最小外接矩形（边界框）信息
     */
    private static class BoundingBox {
        private final double minX;
        private final double maxX;
        private final double minY;
        private final double maxY;

        public BoundingBox(double minX, double maxX, double minY, double maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }

        // 计算中心点X坐标
        public double getCenterX() {
            return (minX + maxX) / 2.0;
        }

        // 计算中心点Y坐标
        public double getCenterY() {
            return (minY + maxY) / 2.0;
        }
    }

    // ------------------------------ 测试方法（验证功能）------------------------------
    public static void main(String[] args) {
        try {
            // 测试输入：不规则多边形坐标（包含凹形结构）
            String testPoints = "[{\"x\": 516, \"y\": 95}, {\"x\": 700, \"y\": 161}, {\"x\": 777, \"y\": 174}, " +
                    "{\"x\": 840, \"y\": 214}, {\"x\": 887, \"y\": 216}, {\"x\": 1049, \"y\": 269}, " +
                    "{\"x\": 1113, \"y\": 305}, {\"x\": 1236, \"y\": 324}, {\"x\": 1493, \"y\": 400}, " +
                    "{\"x\": 1521, \"y\": 397}, {\"x\": 1633, \"y\": 452}, {\"x\": 1654, \"y\": 454}, " +
                    "{\"x\": 1635, \"y\": 442}, {\"x\": 1692, \"y\": 448}, {\"x\": 1744, \"y\": 484}, " +
                    "{\"x\": 1853, \"y\": 511}, {\"x\": 1919, \"y\": 543}, {\"x\": 1919, \"y\": 424}, " +
                    "{\"x\": 1838, \"y\": 416}, {\"x\": 1855, \"y\": 435}, {\"x\": 1854, \"y\": 459}, " +
                    "{\"x\": 1812, \"y\": 477}, {\"x\": 1767, \"y\": 443}, {\"x\": 1750, \"y\": 443}, " +
                    "{\"x\": 1744, \"y\": 428}, {\"x\": 1723, \"y\": 426}, {\"x\": 1731, \"y\": 416}, " +
                    "{\"x\": 1701, \"y\": 414}, {\"x\": 1726, \"y\": 404}, {\"x\": 1750, \"y\": 425}, " +
                    "{\"x\": 1789, \"y\": 427}, {\"x\": 1771, \"y\": 407}, {\"x\": 1797, \"y\": 414}, " +
                    "{\"x\": 1793, \"y\": 388}, {\"x\": 1767, \"y\": 378}, {\"x\": 1716, \"y\": 384}, " +
                    "{\"x\": 1703, \"y\": 350}, {\"x\": 1553, \"y\": 312}, {\"x\": 1476, \"y\": 317}, " +
                    "{\"x\": 1266, \"y\": 253}, {\"x\": 1174, \"y\": 243}, {\"x\": 1027, \"y\": 198}, " +
                    "{\"x\": 917, \"y\": 186}, {\"x\": 829, \"y\": 156}, {\"x\": 773, \"y\": 156}]";

            // 1. 使用默认配置：扩大10%，不过滤异常点
            String scaledPointsDefault = PolygonCoordinateScaleUtil.scalePoints(testPoints);
            System.out.println("=== 默认配置（扩大10%）===");
            System.out.println(scaledPointsDefault);

            // 2. 自定义配置：扩大20%，过滤异常点
            String scaledPointsCustom = PolygonCoordinateScaleUtil.scalePoints(testPoints, 1.2, true);
            System.out.println("\n=== 自定义配置（扩大20%，过滤异常点）===");
            System.out.println(scaledPointsCustom);

        } catch (Exception e) {
            System.err.println("测试失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
}