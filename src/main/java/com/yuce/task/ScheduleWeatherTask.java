package com.yuce.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScheduleWeatherTask {

//    @Autowired
//    private FeatureElementServiceImpl featureElementServiceImpl;
//
//    @Autowired
//    private FrameImageServiceImpl frameImageServiceImpl;
//
//    @Autowired
//    private OriginalAlarmServiceImpl originalAlarmServiceImpl;
//
//    private final String url = "http://12.1.97.206:8000/mllm_analyze";
//
//    private ExecutorService executorService;
//
//    private RestTemplate restTemplate;
//
//    @PostConstruct
//    public void init() {
//        executorService = Executors.newFixedThreadPool(3); // 3个线程
//        restTemplate = new RestTemplate();
//    }
//
//    @Scheduled(cron = "0 * * * * ?")//在每分钟的第0s执行
//    public void featureUpdateTask() {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime currentMinute = now.withSecond(0).withNano(0);
//        LocalDateTime previousMinute = currentMinute.minusMinutes(1);
//
//        // 查询上一分钟新增数据
//        List<FeatureElementRecord> list = featureElementServiceImpl.getListByTimeRange(previousMinute, currentMinute);
//        if (list.isEmpty()) {
//            log.info("请求时间区间暂无新增告警记录:[{}~{}]", previousMinute, currentMinute);
//            return;
//        }
//
//        // 遍历数据，使用线程池并发调用外部接口
//        for (FeatureElementRecord featureElementRecord : list) {
//            executorService.submit(() -> {
//                try {
//                    String alarmId = featureElementRecord.getAlarmId();
//                    String imagePath = featureElementRecord.getImagePath();
//                    String videoPath = featureElementRecord.getVideoPath();
//                    OriginalAlarmRecord originalAlarmRecord = originalAlarmServiceImpl.getRecordByKey(alarmId, imagePath, videoPath);
//
//                    // 获取帧图片 URL
//                    FrameImageInfo frameImageInfo = frameImageServiceImpl.getFrameByKeyAndNo(alarmId, imagePath, videoPath, 2);
//                    if (frameImageInfo == null || frameImageInfo.getImageUrl() == null) {
//                        log.info("告警记录未查询到抽帧图片: alarmId={}, imagePath={}, videoPath={}", alarmId, imagePath, videoPath);
//                        return;
//                    }
//
//                    String imageId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + originalAlarmRecord.getId() + "_" + 1 + "_" + originalAlarmRecord.getTblId();
//
//                    // 构造请求体
//                    Map<String, Object> requestBody = buildRequestData(frameImageInfo.getImageUrl(), originalAlarmRecord.getDeviceId(),imageId);
//
//                    // 调用接口获取结果
//                    String responseStr = restTemplate.postForObject(url, requestBody, String.class);
//                    if (responseStr == null) {
//                        log.warn("接口返回空: alarmId={}", alarmId);
//                        return;
//                    }
//                    Map<String, String> map = parseResponse(responseStr, featureElementRecord);// 解析返回结果
//                    String fire = map.get("fire");
//                    String weather = map.get("weather");
//                    featureElementRecord.setDangerElement(fire);
//                    featureElementRecord.setWeatherCondition(weather);
//                    featureElementServiceImpl.updateById(featureElementRecord);
//                } catch (Exception e) {
//                    log.error("处理告警记录异常: alarmId={}, error={}", featureElementRecord.getAlarmId(), e.getMessage(), e);
//                }
//            });
//        }
//    }
//
//    // 构造请求体
//    public Map<String, Object> buildRequestData(String imageUrl, String deviceId, String imageId) {
//        Map<String, Object> requestBody = new HashMap<>();
//        requestBody.put("alarm_type", Arrays.asList("fire", "weather"));
//
//        // 构造 device_id
//        Map<String, Object> deviceIdMap = new HashMap<>();
//        deviceIdMap.put("type", "device_id");
//        deviceIdMap.put("device_id", deviceId);
//
//        // 构造 image_id
//        Map<String, Object> imageIdMap = new HashMap<>();
//        imageIdMap.put("type", "image_id");
//        imageIdMap.put("image_id", imageId);
//
//        // 构造 image_url
//        Map<String, Object> imageUrlMap = new HashMap<>();
//        imageUrlMap.put("type", "image_url");
//        imageUrlMap.put("image_url", Collections.singletonMap("url", imageUrl));
//
//        // content 数组
//        List<Map<String, Object>> contentList = Arrays.asList(deviceIdMap, imageIdMap, imageUrlMap);
//
//        // message
//        Map<String, Object> message = new HashMap<>();
//        message.put("role", "user");
//        message.put("content", contentList);
//
//        // messages 是一个二维数组
//        requestBody.put("messages", Arrays.asList(Arrays.asList(message)));
//
//        return requestBody;
//    }
//
//    // 解析返回结果
//    @SuppressWarnings("unchecked")
//    // 解析返回结果（只取 data 数组的第一个对象）
//    public Map<String,String> parseResponse(String responseStr, FeatureElementRecord record) {
//        Map<String,String> map = new HashMap<>();
//        try {
//            // 转换成 JSONArray
//            JSONArray responseArray = JSONArray.parseArray(responseStr);
//            if (responseArray.isEmpty()) {
//                log.warn("接口返回结果为空数组: alarmId={}, response={}", record.getAlarmId(), responseStr);
//                map.put("fire",null);
//                map.put("weather",null);
//                return null;
//            }
//
//            JSONObject firstObj = responseArray.getJSONObject(0);// 取第一个对象
//
//            String imageId = firstObj.getString("image_id");
//
//            // 获取 data 数组
//            JSONArray dataArray = firstObj.getJSONArray("data");
//            if(dataArray.isEmpty()){
//                log.warn("返回结果中 data 为空: alarmId={}, response={}", record.getAlarmId(), responseStr);
//                map.put("fire",null);
//                map.put("weather",null);
//            }else{
//                for (int i = 0; i < dataArray.size(); i++) {
//                    JSONObject dataObj = dataArray.getJSONObject(i);
//                    String type = dataObj.getString("type");
//                    String name = getChineseDesc(dataObj.getString("name"));
//                    String score = dataObj.getString("score");
//                    map.put(type,name);
//                    log.info("alarmId={}, imageId={}, type={}, name={}, score={}", record.getAlarmId(), imageId, type, name, score);
//                }
//            }
//        } catch (Exception e) {
//            log.error("解析接口返回结果失败: alarmId={}, response={}, error={}", record.getAlarmId(), responseStr, e.getMessage(), e);
//        }
//        return map;
//    }
//
//    public String getChineseDesc(String englishDesc) {
//        if (englishDesc == null) {
//            return "未知";
//        }
//        switch (englishDesc.toLowerCase()) { // 忽略大小写
//            case "yes":
//                return "存在火灾";
//            case "no":
//                return "不存在火灾";
//            case "sunny":
//                return "晴天";
//            case "rainy":
//                return "雨天";
//            case "foggy":
//                return "雾天";
//            case "snowy":
//                return "雪天";
//            case "unknown":
//                return "未知";
//            default:
//                return "未知描述";
//        }
//    }
}