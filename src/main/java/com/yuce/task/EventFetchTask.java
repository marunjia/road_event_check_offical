package com.yuce.task;

import com.alibaba.fastjson.JSONObject;
import com.yuce.algorithm.*;
import com.yuce.common.GxDealTagDataPush;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.handler.MessageHandler;
import com.yuce.service.impl.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 多消费者并行处理事件捕获任务
 * 支持按配置数量启动消费者，每个消费者独立处理消息并管理偏移量
 */
@Slf4j
@Component
public class EventFetchTask implements MessageHandler {

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private RoadCheckRecordServiceImpl roadCheckRecordServiceImpl;

    @Autowired
    private CloudEyesDeviceServiceImpl cloudEyesDeviceServiceImpl;

    @Autowired
    private GeneralAlgorithm generalAlgorithm; //通用算法服务

    @Autowired
    private ExtractFrameAlgorithm extractFrameAlgorithm;//抽帧算法服务

    @Autowired
    private ExtractWindowAlgorithm extractWindowAlgorithm;//提框算法服务

    @Autowired
    private PswAlgorithm pswAlgorithm;//抛洒物算法服务

    @Autowired
    private PersonAlgorithm personAlgorithm;//行人算法服务

    @Autowired
    private VehicleAlgorithm vehicleAlgorithm;//停驶算法服务(目前停驶算法与行人算法基本保持一致，为了扩展性，两者独立)

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private FeatureElementAlgorithm featureElementAlgorithm;//特征要素填充服务

    @Autowired
    private AlarmCollectionAlgorithm alarmCollectionAlgorithm;//告警集填充服务

    @Autowired
    private CollectionGroupAlgorithm collectionGroupAlgorithm;//告警组填充服务

    @Autowired
    private RoadAlgorithm roadAlgorithm;//路面检测算法服务

    @Autowired
    private GxDealTagDataPush gxDealTagDataPush;

    private static final List<String> ROAD_LIST = Arrays.asList("33141", "33112");//接受道路编码列表
    private static final List<String> EVENT_TYPE_LIST = Arrays.asList("停驶", "行人", "抛洒物");//接受事件类型列表

    /**
     * 业务核心处理方法：
     *  1、解析kafka topic原始数据，获取告警记录字段信息
     *  2、筛选目标道路
     *  3、筛选目标时间范围数据
     *  3、剔除图片、视频字段异常数据
     *  4、存储符合条件的原始告警数据
     *  5、根据视频进行抽帧逻辑处理(路面识别、算法识别均需要用到抽帧逻辑)
     *  6、根据图片进行提框逻辑处理(后续业务会用到提框逻辑)
     *  7、调用路面算法检测服务，剔除路面外物体
     *  8、调用算法服务进行业务逻辑处理
     *  9、调用特征要素服务补充字段信息
     *  10、调用告警集服务补充告警集信息
     *  11、调用告警组服务补充告警组信息
     */
    public void handleMessage(ConsumerRecord<String, String> record) {

        //kafka原始数据格式化处理
        OriginalAlarmRecord alarmRecord = JSONObject.parseObject(record.value().replaceAll("^\"|\"$|\\\\", ""), OriginalAlarmRecord.class);//

        //获取基础告警记录字段信息
        String eventType = Optional.ofNullable(alarmRecord.getEventType()).orElse("未知类型");
        String alarmId = Optional.ofNullable(alarmRecord.getId()).orElse("未知ID");
        String roadId = Optional.ofNullable(alarmRecord.getRoadId()).orElse("未知道路ID");
        String imagePath = Optional.ofNullable(alarmRecord.getImagePath()).orElse("无图片地址");
        String videoPath = Optional.ofNullable(alarmRecord.getVideoPath()).orElse("无视频地址");
        String companyName = Optional.ofNullable(alarmRecord.getCompany()).orElse("未知公司");
        log.info("接收原始告警记录 | 分区:{} | 偏移量:{} | 道路ID:{} | 告警类型:{} | 告警ID:{} | 公司:{} | 图片路径:{} | 视频路径:{}", record.partition(), record.offset(), roadId, eventType, alarmId, companyName, imagePath, videoPath);

        //筛选目标道路:仅抽取G33141、G33112
        if (!ROAD_LIST.contains(roadId)) {
            return;
        }

        //告警记录时间范围筛选
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 4, 0, 0, 0);
        if (alarmRecord.getAlarmTime().isBefore(startTime)) {//判断告警时间是否在起始时间之后
            return;
        }

        //脏数据处理：提出视频路径、图片路径为空记录
        if (!(StringUtils.hasText(imagePath)) && !(StringUtils.hasText(videoPath))) {
            return;
        }

        //原始告警记录存储&&更新
        OriginalAlarmRecord recordExists = originalAlarmServiceImpl.getRecordByKey(alarmId, imagePath, videoPath);
        if (recordExists != null) {
            alarmRecord.setId(recordExists.getId());
            originalAlarmServiceImpl.updateByKey(alarmRecord);
            log.info("告警记录已存在，执行更新 | 道路ID:{} | 告警ID:{} | 告警类型:{} | 图片路径:{} | 视频路径:{}", roadId, alarmId, eventType, imagePath, videoPath);
            return;
        } else {
            originalAlarmServiceImpl.insert(alarmRecord);
            log.info("新增告警记录(add a new alarm record):roadId->{},alarmId->{},eventType->{},imagePath->{},videoPath->{}", roadId, alarmId, eventType, imagePath, videoPath);
        }
        gxDealTagDataPush.pushToGx(alarmRecord);
        alarmRecord = originalAlarmServiceImpl.getRecordByKey(alarmId, imagePath, videoPath);//查询tblId,原始告警记录不携带tblId

        //视频抽帧服务
        try {
            extractFrameAlgorithm.extractFrame(alarmRecord);
            log.info("视频抽帧完成 | 道路ID:{} | 告警ID:{} | 图片路径:{} | 视频路径:{}", roadId, alarmId, imagePath ,videoPath);
        } catch (Exception e) {
            //generalAlgorithm.checkDeal(alarmRecord, "视频抽帧异常");
            log.error("视频抽帧异常 | 道路ID:{} | 告警ID:{} | 图片路径:{} | 视频路径:{} | 异常详情:", roadId, alarmId, imagePath ,videoPath, e);
            return;
        }

        /**
         * 图片提框服务
         */
        try {
            extractWindowAlgorithm.extractWindow(alarmRecord);
        } catch (Exception e) {
            //generalAlgorithm.checkDeal(alarmRecord, "图片提框异常"); // 调用通用算法处理
            log.error("图片提框异常 | 道路ID:{} | 告警ID:{} | 图片路径:{} | 视频路径:{} | 异常详情:", roadId, alarmId, imagePath ,videoPath, e);
            return;
        }

        /**
         * 路面检测逻辑：
         * 1. 若物体在路面外 → 标记为误检，调用通用算法处理
         * 2. 若物体在路面内 → 仅保留"抛洒物/停驶/行人"类型继续处理，其他类型默认正检
         */
        try {
            // 执行路面检测核心逻辑
            roadAlgorithm.roadCheckDeal(alarmRecord);

            // 查询路面检测结果（内/外记录数）
            int onRoadCount = roadCheckRecordServiceImpl
                    .getRecordByKeyAndTypeAndFlag(alarmId, imagePath, videoPath, "road", 1)
                    .size();
            int outRoadCount = roadCheckRecordServiceImpl
                    .getRecordByKeyAndTypeAndFlag(alarmId, imagePath, videoPath, "road", 2)
                    .size();

            log.info("路面检测结果统计 | alarmId:{} | 路面内:{}条 | 路面外:{}条 | imagePath:{} | videoPath:{}",
                    alarmId, onRoadCount, outRoadCount, imagePath, videoPath);

            // 场景1：物体在路面外（路面内无记录，路面外有记录）→ 标记误检，调用通用算法处理
            if (onRoadCount == 0 && outRoadCount > 0) {
                generalAlgorithm.checkDeal(alarmRecord, "非路面物体", 2);
                log.info("路面检测：物体在路面外，标记为误检 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
                return; // 处理完成，终止后续逻辑
            }

            // 场景2：物体在路面内 → 判断是否为需特殊处理的事件类型
            // 非"抛洒物/停驶/行人"类型 → 默认正检
            if (!EVENT_TYPE_LIST.contains(eventType)) {
                log.info("路面检测：物体在路面内，但告警类型无需后续处理，默认正检 | alarmId:{} | eventType:{} | imagePath:{} | videoPath:{} | roadId:{}", alarmId, eventType, imagePath, videoPath, roadId);
                generalAlgorithm.checkDeal(alarmRecord, "", 1);
                return; // 处理完成，终止后续逻辑
            }

            // 若为"抛洒物/停驶/行人"类型 → 继续后续处理（不返回，执行外层逻辑）
            log.debug("路面检测：物体在路面内且事件类型需后续处理 | alarmId:{} | eventType:{} | imagePath:{} | videoPath:{}", alarmId, eventType, imagePath, videoPath);
        } catch (Exception e) {
            // 异常处理：路面检测失败时默认"检测成功"，避免流程中断
            log.error("路面检测发生异常，默认继续后续处理 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
        }

        /**
         * 之江智能夜间自动正检逻辑：
         * 夜间时段（19:00~次日07:00）的之江智能记录，默认标记为"正检"（无需人工复核）
         */
        // 提取告警时间的小时数（避免重复调用方法）
        int alarmHour = alarmRecord.getAlarmTime().getHour();
        // 定义夜间时段判断条件（常量化，便于理解和修改）
        boolean isZhijiang = "之江智能".equals(companyName);
        boolean isNightTime = alarmHour >= 19 || alarmHour < 7;

        if (isZhijiang && isNightTime) {
            log.info("之江智能夜间自动正检 | 符合夜间时段(19:00-07:00) | roadId:{} | alarmId:{} | eventType:{} | imagePath:{} | videoPath:{}",
                    roadId, alarmId, eventType, imagePath, videoPath);
            // 调用正检方法（参数含义通过变量名/注释明确）
            // 第二个参数为空字符串：表示无需依据信息；第三个参数1：表示正检状态
            generalAlgorithm.checkDeal(alarmRecord, "", 1);
            return;
        }

        /**
         * 抛洒物算法检验：调用抛洒物专属算法，失败时兜底通用算法
         */
        if ("抛洒物".equals(eventType)) {
            // 1. 日志标准化：统一格式，关键字段必现，补充操作说明
            log.info("开始抛洒物算法处理 | 调用抛洒物服务 | roadId:{} | alarmId:{} | imagePath:{} | videoPath:{}",
                    roadId, alarmId, imagePath, videoPath);

            try {
                // 2. 执行抛洒物算法：用变量接收结果，提升可读性
                boolean isPswSuccess = pswAlgorithm.pswDeal(alarmRecord);

                if (!isPswSuccess) {
                    // 3. 处理失败：日志明确原因，调用通用算法兜底
                    log.info("抛洒物算法处理失败 | 兜底调用通用算法 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
                    generalAlgorithm.checkDeal(alarmRecord, "抛洒物算法处理失败");
                } else {
                    // 4. 处理成功：日志明确结果，无后续操作则清晰说明
                    log.info("抛洒物算法处理成功 | 无后续额外操作 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
                }
            } catch (Exception e) {
                // 5. 异常防护：避免算法执行抛异常导致流程中断，同时记录完整堆栈
                log.error("抛洒物算法执行异常 | 兜底调用通用算法 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
                generalAlgorithm.checkDeal(alarmRecord, "抛洒物算法执行异常");
            }
            // 6. 明确终止后续逻辑：避免进入其他事件类型的处理流程
            return;
        }

        /**
         * 行人算法检验：调用行人专属算法，成功后执行系列后续处理，失败时兜底通用算法
         */
        if ("行人".equals(eventType)) {
            log.info("开始行人算法处理 | 调用行人服务 | roadId:{} | alarmId:{} | imagePath:{} | videoPath:{}", roadId, alarmId, imagePath, videoPath);

            try {
                boolean isPersonDealSuccess = personAlgorithm.personDeal(alarmRecord);
                if (!isPersonDealSuccess) {
                    // 算法处理失败：日志说明原因，调用通用算法兜底
                    log.info("行人算法处理失败 | 兜底调用通用算法 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
                    generalAlgorithm.checkDeal(alarmRecord, "行人算法处理失败");
                } else {
                    log.info("行人算法处理成功 | 开始执行后续业务流程 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
                    checkAlarmResultServiceImpl.checkResultByImgNum(alarmRecord, "person"); // 1. 获取算法检测结果
                    log.debug("行人后续流程：图片数量校验完成 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);

                    featureElementAlgorithm.featureElementDealByAlgo(alarmRecord); // 3. 特征要素判定
                    log.debug("行人后续流程：特征要素判定完成 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);

                    alarmCollectionAlgorithm.collectionDeal(alarmRecord); // 4. 告警集判定
                    log.debug("行人后续流程：告警集判定完成 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);

                    collectionGroupAlgorithm.groupDeal(alarmRecord);// 5. 告警组判定
                    log.info("行人后续流程：全部完成 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
                }
            } catch (Exception e) {
                log.error("行人算法处理异常 | 兜底调用通用算法 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
                generalAlgorithm.checkDeal(alarmRecord, "行人算法执行异常");
            }

            // 明确终止后续逻辑，避免进入其他事件类型处理流程
            return;
        }

        /**
         * 停驶算法检验：调用停驶专属算法，成功后执行系列后续处理，失败时兜底通用算法
         */
        if ("停驶".equals(eventType)) {
            // 标准化开始日志：明确流程起点，包含所有关键业务字段
            log.info("开始停驶算法处理 | 调用停驶服务 | roadId:{} | alarmId:{} | eventType:{} | imagePath:{} | videoPath:{}", roadId, alarmId, eventType, imagePath, videoPath);

            try {
                // 执行停驶核心算法，用变量名清晰表达执行结果
                boolean isVehicleDealSuccess = vehicleAlgorithm.vehicleDeal(alarmRecord);

                if (!isVehicleDealSuccess) {
                    log.info("停驶算法处理失败 | 兜底调用通用算法 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
                    generalAlgorithm.checkDeal(alarmRecord, "停驶算法处理失败");
                } else {
                    log.info("停驶算法处理成功 | 开始执行后续业务流程 | alarmId:{}", alarmId);

                    checkAlarmResultServiceImpl.checkResultByIou(alarmRecord, 0.2, 1);// 1. 获取算法检测结果
                    log.debug("停驶后续流程：IOU校验完成（阈值0.2） | alarmId:{}", alarmId);

                    featureElementAlgorithm.featureElementDealByAlgo(alarmRecord); // 3. 特征要素判定
                    log.debug("停驶后续流程：特征要素判定完成 | alarmId:{}", alarmId);

                    alarmCollectionAlgorithm.collectionDeal(alarmRecord);// 4. 告警集判定
                    log.debug("停驶后续流程：告警集判定完成 | alarmId:{}", alarmId);

                    collectionGroupAlgorithm.groupDeal(alarmRecord); // 5. 告警组判定
                    log.info("停驶后续流程：全部处理完成 | alarmId:{} | videoPath:{}", alarmId, videoPath);
                }
            } catch (Exception e) {
                // 异常防护：捕获算法执行及后续流程中的所有异常，确保流程不中断
                log.error("停驶算法处理异常 | 兜底调用通用算法 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
                generalAlgorithm.checkDeal(alarmRecord, "停驶算法执行异常");
            }
            return;
        }

        /**
         * 误检点位推送逻辑：
         * 1. 查询当前告警的检测结果
         * 2. 若为误检（checkFlag=2），获取最近50条误检记录的设备ID并推送刷新
         */
        // 1. 查询当前告警的检测结果（明确变量含义，避免歧义）
        CheckAlarmResult currentCheckResult = checkAlarmResultServiceImpl.getResultByKey(alarmId, imagePath, videoPath);
        log.info("开始误检点位推送处理 | 查询当前告警检测结果 | alarmId:{} | imagePath:{} | videoPath:{} | 检测结果是否存在:{}", alarmId, imagePath, videoPath, currentCheckResult != null);

        // 2. 处理检测结果不存在的场景
        if (currentCheckResult == null) {
            log.error("误检点位推送失败 | 未查询到当前告警的正误检标签 | roadId:{} | alarmId:{} | eventType:{} | imagePath:{} | videoPath:{}", roadId, alarmId, eventType, imagePath, videoPath);
            return; // 无检测结果，终止后续逻辑
        }

        // 3. 仅处理误检场景（checkFlag=2）
        if (currentCheckResult.getCheckFlag() != 2) {
            log.debug("当前告警非误检，无需推送点位 | checkFlag:{} | alarmId:{} | imagePath:{} | videoPath:{}", currentCheckResult.getCheckFlag(), alarmId, imagePath, videoPath);
            return;
        }

        try {
            // 4. 获取最近50条误检记录的设备ID列表
            List<String> recentFalseDeviceIds = checkAlarmResultServiceImpl.getRecentFalseCheckList(currentCheckResult.getId());
            log.info("查询到最近50条误检记录的设备ID | 数量:{} | alarmId:{} | 设备ID列表:{}", recentFalseDeviceIds.size(), alarmId, recentFalseDeviceIds);

            // 5. 处理空列表场景（避免无效调用）
            if (recentFalseDeviceIds.isEmpty()) {
                log.warn("误检点位推送：最近50条误检记录的设备ID列表为空 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
                return;
            }

            // 6. 推送设备ID列表刷新
            cloudEyesDeviceServiceImpl.refreshDevices(recentFalseDeviceIds);
            log.info("误检点位推送成功 | 设备ID数量:{} | alarmId:{} | imagePath:{} | videoPath:{}", recentFalseDeviceIds.size(), alarmId, imagePath, videoPath);
        } catch (Exception e) {
            log.error("误检点位推送异常 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
        }
    }
    /**
     * @desc 辅助方法：格式化null或空值为显式标记
     * @param value
     * @return
     */
    private static String formatNull(String value) {
        return StringUtils.hasText(value) ? value : "[空值或空白]";
    }
}