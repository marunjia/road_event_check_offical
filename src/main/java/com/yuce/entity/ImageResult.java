package com.yuce.entity;

import lombok.Data;
import java.util.List;

@Data
public class ImageResult {
    public String imageId;
    public String receivedTime;
    public int status;
    public List<DataItem> data;

    @Data
    public static class DataItem {
        public String completedTime;
        public String type;
        public String name;
        public String score;
        public List<Point> points;
    }

    @Data
    public static class Point {
        public int x;
        public int y;
    }
}