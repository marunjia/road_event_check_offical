package com.yuce.util;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.List;
import java.util.ArrayList;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * @ClassName RoadIntersectionUtil
 * @Description 路面与提框交叉像素计算工具类（交集占比以矩形像素为基准）
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/14 14:33
 * @Version 1.2
 */

@Component
public class RoadIntersectionUtil {

    /**
     * 计算路面多边形与提框矩形交叉像素占比（以矩形像素为基准）
     *
     * @param roadPointsJson 路面多边形坐标 JSON 字符串
     * @param rectangle 矩形坐标
     * @return 交叉像素占比（0~1）
     */
    public static double calculateIntersectionRatioByRectanglePixels(String roadPointsJson, Rectangle rectangle) {
        List<Point> roadPoints = parsePointsFromJson(roadPointsJson);
        if (roadPoints.isEmpty() || rectangle.width <= 0 || rectangle.height <= 0) {
            return 0.0;
        }

        // 构建路面多边形
        Polygon roadPolygon = new Polygon();
        for (Point p : roadPoints) {
            roadPolygon.addPoint(p.x, p.y);
        }

        // 构建 Area
        Area roadArea = new Area(roadPolygon);
        Area rectArea = new Area(rectangle);

        // 求交集
        rectArea.intersect(roadArea);

        // 计算交集像素数
        int intersectPixelCount = countPixels(rectArea);

        // 矩形总像素数
        int rectPixelCount = rectangle.width * rectangle.height;
        if (rectPixelCount == 0) {
            return 0.0;
        }

        return (double)intersectPixelCount/rectPixelCount;
    }

    /**
     * 解析 JSON 字符串为 Point 列表
     *
     * @param json JSON 字符串
     * @return Point 列表
     */
    private static List<Point> parsePointsFromJson(String json) {
        List<Point> points = new ArrayList<>();
        try {
            JSONArray array = JSON.parseArray(json);
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                int x = obj.getIntValue("x");
                int y = obj.getIntValue("y");
                points.add(new Point(x, y));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return points;
    }

    /**
     * 统计 Area 内的像素数量
     *
     * @param area 要统计的 Area
     * @return 像素数
     */
    private static int countPixels(Area area) {
        // 获取交集区域的边界框，确定遍历范围
        Rectangle bounds = area.getBounds();
        int count = 0;
        // 遍历边界框内的每个像素点
        for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
            for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
                // 判断像素点 (x+0.5, y+0.5) 是否在交集中（用中心点判断更准确）
                if (area.contains(x + 0.5, y + 0.5)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 使用鞋带公式计算多边形面积
     * @param pointsJson 字符串形式的 points，如:
     *                   "[{\"x\":1649,\"y\":228},{\"x\":1545,\"y\":285},{\"x\":1154,\"y\":597}]"
     */
    public static double calculatePolygonArea(String pointsJson){
        double roadArea = 0;
        if(pointsJson == null || pointsJson.isEmpty()){
            return roadArea;
        }else{
            try{
                ObjectMapper mapper = new ObjectMapper();
                JsonNode arrayNode = mapper.readTree(pointsJson);
                if (!arrayNode.isArray() || arrayNode.size() < 3) {
                    return 0.0; // 少于3个点无法构成多边形
                }

                List<Point> points = new ArrayList<>();
                for (JsonNode node : arrayNode) {
                    int x = node.get("x").asInt();
                    int y = node.get("y").asInt();
                    points.add(new Point(x, y));
                }

                double area = 0.0;
                int n = points.size();
                for (int i = 0; i < n; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get((i + 1) % n); // 闭合多边形
                    area += (p1.x * p2.y) - (p2.x * p1.y);
                }
                roadArea =  Math.abs(area) / 2.0;
            }catch(Exception e){
                e.printStackTrace();
            }finally {
                return roadArea;
            }
        }
    }
}