package com.yuce.algorithm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuce.entity.*;
import com.yuce.service.impl.ExtractWindowServiceImpl;
import com.yuce.service.impl.FrameImageServiceImpl;
import com.yuce.service.impl.RoadCheckRecordServiceImpl;
import com.yuce.util.RoadIntersectionUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * @ClassName RoadAlgorithm
 * @Description 停驶算法服务
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/15 11:02
 * @Version 1.0
 */

@Component
@Slf4j
public class RoadAlgorithm {

    @Autowired
    private FrameImageServiceImpl frameImageServiceImpl;

    @Autowired
    private RoadCheckRecordServiceImpl roadCheckRecordServiceImpl;

    @Autowired
    private ExtractWindowServiceImpl extractWindowServiceImpl;

    //定义接口请求访问地址
    private static final String ROAD_URL = "http://12.1.97.206:9993/v1/road_segmentation";

    //定义客户端对象
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 调用算法接口
     * @param record
     * @return boolean
     */
    public void roadCheckDeal(OriginalAlarmRecord record) throws Exception {
        /**
         * 获取告警记录基础字段信息
         */
        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String eventType = record.getEventType();
        log.info("调用路面检测服务(use roadCheck service):alarmId->{},imagePath->{},videoPath->{}", alarmId, imagePath, videoPath);

        FrameImageInfo frameImageInfo = frameImageServiceImpl.getFrameByKeyAndNo(alarmId, imagePath, videoPath, 1);

        /**
         * 检查路面算法是否已检测
         */
        if(roadCheckRecordServiceImpl.getRecordByKeyAndType(alarmId, imagePath, videoPath,"road").size() > 0){
            log.info("路面算法已检测：tblId->{}, alarmId->{}, imagePath->{}, videoPath->{}", tblId, alarmId, imagePath, videoPath);
            return;
        }

        String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(buildRequestData(record,frameImageInfo));
        } catch (IOException e) {
            throw new IOException(String.format("路面算法请求体序列化失败,alarmId->%s,imagePath->%s,videoPath->%s", alarmId, imagePath, videoPath));
        }

        // 构造请求
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        Request request = new Request.Builder().url(ROAD_URL).post(body).build();

        // 执行同步请求
        try (Response response = client.newCall(request).execute()) {
            if (response == null || !response.isSuccessful()) {
                log.error("路面检测算法接口返回非成功状态: code={}, alarm_id->{}", response != null ? response.code() : "null",alarmId);
                return;
            }

            // 关键：只读取一次响应体，并确保日志中不重复调用
            String responseBody = response.body() != null ? response.body().string() : null;
            log.info("路面检测算法服务:alarmId->{}, imagePath->{}, videoPath->{}, 请求体内容 -> {}, 返回体 -> {}", alarmId, imagePath, videoPath, jsonBody, responseBody);

            if (responseBody == null || responseBody.isEmpty()) {
                return;
            }

            //获取该告警id对应的抽框坐标
            ExtractWindowRecord extractWindowRecord = extractWindowServiceImpl.getExtractWindow(alarmId, imagePath, videoPath);
            int baseX1 = extractWindowRecord.getPoint1X();
            int baseY1 = extractWindowRecord.getPoint1Y();
            int baseX2 = extractWindowRecord.getPoint2X();
            int baseY2 = extractWindowRecord.getPoint2Y();

            // 解析为 JSON 数组
            JSONArray resultArray = JSONArray.parseArray(responseBody);
            List<RoadCheckRecord> roadCheckRecordList = new ArrayList<>();

            for (int i = 0; i < resultArray.size(); i++) {
                JSONObject result = resultArray.getJSONObject(i);

                String imageId = result.getString("image_id");
                int status = result.getIntValue("status");
                JSONArray dataArray = result.getJSONArray("data");

                if (dataArray.size() > 0) {
                    for (int j = 0; j < dataArray.size(); j++) {
                        JSONObject subJsonObject = dataArray.getJSONObject(j);
                        String type = subJsonObject.getString("type");
                        String name = subJsonObject.getString("name");
                        String points = subJsonObject.getString("points");

                        RoadCheckRecord roadCheckRecord = new RoadCheckRecord();
                        roadCheckRecord.setAlarmId(alarmId);
                        roadCheckRecord.setImagePath(imagePath);
                        roadCheckRecord.setVideoPath(videoPath);
                        roadCheckRecord.setImageId(imageId);
                        roadCheckRecord.setType(type);
                        roadCheckRecord.setName(name);
                        roadCheckRecord.setStatus(status);
                        roadCheckRecord.setPoints(points);
                        roadCheckRecord.setExtractImageUrl(frameImageInfo.getImageUrl());
                        roadCheckRecord.setCreateTime(LocalDateTime.now());
                        roadCheckRecord.setUpdateTime(LocalDateTime.now());

                        //路面与提框坐标交叉百分比判定
                        double percent = 0;

                        if(eventType.equals("停驶") || eventType.equals("行人")){
                            int midY = (baseY1 + baseY2)/2;
                            roadCheckRecord.setExtractPoint1X(baseX1);
                            roadCheckRecord.setExtractPoint1Y(midY);
                            roadCheckRecord.setExtractPoint2X(baseX2);
                            roadCheckRecord.setExtractPoint2Y(baseY2);
                            log.info("路面算法检测坐标：tblId->{}, alarmId->{}, imagePath->{}, videoPath->{},({},{},{},{})", tblId, alarmId, imagePath, videoPath, baseX1, midY, baseX2, baseY2);
                            percent = RoadIntersectionUtil.calculateIntersectionRatioByRectanglePixels(points ,getRectangle(baseX1, midY, baseX2, baseY2));
                        }

                        if(eventType.equals("抛洒物")){
                            roadCheckRecord.setExtractPoint1X(baseX1);
                            roadCheckRecord.setExtractPoint1Y(baseY1);
                            roadCheckRecord.setExtractPoint2X(baseX2);
                            roadCheckRecord.setExtractPoint2Y(baseY2);
                            percent = RoadIntersectionUtil.calculateIntersectionRatioByRectanglePixels(points ,getRectangle(baseX1, baseY1, baseX2, baseY2));
                        }
                        roadCheckRecord.setPercent(percent);

                        //路面内外标签判定
                        if(percent > 0){
                            roadCheckRecord.setRoadCheckFlag(1);
                        }else{
                            roadCheckRecord.setRoadCheckFlag(2);
                        }
                        roadCheckRecordList.add(roadCheckRecord);
                    }
                } else {
                    RoadCheckRecord roadCheckRecord = new RoadCheckRecord();
                    roadCheckRecord.setAlarmId(alarmId);
                    roadCheckRecord.setImagePath(imagePath);
                    roadCheckRecord.setVideoPath(videoPath);
                    roadCheckRecord.setImageId(imageId);
                    roadCheckRecord.setType(null);
                    roadCheckRecord.setName(null);
                    roadCheckRecord.setExtractPoint1X(null);
                    roadCheckRecord.setExtractPoint1X(null);
                    roadCheckRecord.setExtractPoint1X(null);
                    roadCheckRecord.setExtractPoint1X(null);
                    roadCheckRecord.setRoadCheckFlag(null);
                    roadCheckRecord.setRoadCheckFlag(null);
                    roadCheckRecord.setCreateTime(LocalDateTime.now());
                    roadCheckRecord.setUpdateTime(LocalDateTime.now());
                    roadCheckRecordList.add(roadCheckRecord);
                }
            }
            roadCheckRecordServiceImpl.saveBatch(roadCheckRecordList);
            log.info("告警记录完成路面检测：alarmId->{},imagePath->{},videoPath->", alarmId, imagePath, videoPath);
        } catch (IOException e) {
            throw new IOException(String.format("路面算法请求异常,alarmId->%s,imagePath->%s,videoPath->%s", alarmId, imagePath, videoPath));
        }
    }

    /**
     * @desc 构造请求体 Map 数据
     * @param record
     * @return
     */
    public Map<String, Object> buildRequestData(OriginalAlarmRecord record, FrameImageInfo frameImageInfo) {
        Map<String, Object> root = new HashMap<>();

        root.put("alarm_type", Collections.singletonList("road_segmentation"));

        List<List<Map<String, Object>>> messages = new ArrayList<>();

        // 每个分组
        List<Map<String, Object>> messageGroup = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        List<Map<String, Object>> content = new ArrayList<>();

        // device_id
        Map<String, Object> deviceIdMap = new HashMap<>();
        deviceIdMap.put("type", "device_id");
        deviceIdMap.put("device_id", record.getDeviceId());
        content.add(deviceIdMap);

        // image_id
        String imageId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + record.getId() + "_" + frameImageInfo.getImageSortNo() + "_" + record.getTblId();
        Map<String, Object> imageIdMap = new HashMap<>();
        imageIdMap.put("type", "image_id");
        imageIdMap.put("image_id", imageId);
        content.add(imageIdMap);

        // image_url
        Map<String, Object> imageUrlMap = new HashMap<>();
        imageUrlMap.put("type", "image_url");

        Map<String, Object> imageUrlValue = new HashMap<>();
        imageUrlValue.put("url", frameImageInfo.getImageUrl());
        imageUrlMap.put("image_url", imageUrlValue);
        content.add(imageUrlMap);

        // 把 content 放进 message
        message.put("content", content);
        messageGroup.add(message);
        messages.add(messageGroup);

        // 放入 root
        root.put("messages", messages);
        return root;
    }

    /**
     * @desc 根据坐标框生成矩形框
     * @param baseX1
     * @param baseY1
     * @param baseX2
     * @param baseY2
     * @return
     */
    public Rectangle getRectangle(int baseX1, int baseY1, int baseX2, int baseY2) {
        return new Rectangle(
                Math.min(baseX1, baseX2),
                Math.min(baseY1, baseY2),
                Math.abs(baseX2 - baseX1),
                Math.abs(baseY2 - baseY1)
        );
    }
}