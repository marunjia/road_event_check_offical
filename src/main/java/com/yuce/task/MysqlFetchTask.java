package com.yuce.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuce.algorithm.*;
import com.yuce.common.GxDealTagDataPush;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.entity.OriginalAlarmRecordBak;
import com.yuce.mapper.OriginalAlarmBakMapper;
import com.yuce.service.impl.*;
import com.yuce.util.FlagTagUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 多消费者并行处理事件捕获任务
 * 支持按配置数量启动消费者，每个消费者独立处理消息并管理偏移量
 */
@Slf4j
@Component
public class MysqlFetchTask{

    // 分批读取大小（可调整，3万条数据建议1000-2000条/批，平衡性能和内存）
    private static final int PAGE_SIZE = 1000;

    @Autowired
    private OriginalAlarmBakMapper originalAlarmBakMapper;

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private RoadCheckRecordServiceImpl roadCheckRecordServiceImpl;

    @Autowired
    private CloudEyesDeviceServiceImpl cloudEyesDeviceServiceImpl;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

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
    private CheckResultAlgorithm checkResultAlgorithm;

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

    private static final List<String> EVENT_TYPE_LIST = Arrays.asList("停驶", "行人", "抛洒物");//接受事件类型列表
    private static final LocalDateTime startTime = LocalDateTime.of(2025, 11, 21, 0, 0, 0);
    private static final LocalDateTime endTime = LocalDateTime.of(2025, 11, 22, 23, 59, 59);


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

    public void totalProcess(){

        log.info("开始读取数据表：kafka_original_alarm_record_bak_20260111");

        int currentPage = 1; // 当前页码（从1开始）
        IPage<OriginalAlarmRecordBak> page = null; // 显式初始化，避免未赋值警告
        int totalPages = 0; // 总页数（单独存储，避免无数据时NPE）

        do {
            // 构建分页条件：第currentPage页，每页PAGE_SIZE条
            Page<OriginalAlarmRecordBak> pagination = new Page<>(currentPage, PAGE_SIZE);
            // 分页查询（MyBatis-Plus自动生成count和limit语句）
            page = originalAlarmBakMapper.selectPage(pagination, null);

            // 第一次查询时获取总页数（避免重复获取）
            if (currentPage == 1) {
                totalPages = (int) page.getPages();
                log.info("数据表总条数：{}，总页数：{}，每页条数：{}",
                        page.getTotal(), totalPages, PAGE_SIZE);
            }

            // 获取当前页数据（MyBatis-Plus的Page.getRecords()永远不为null，无需判空null）
            if (!page.getRecords().isEmpty()) {
                log.info("正在处理第{}页数据，当前页条数：{}，累计处理条数：{}",
                        currentPage, page.getRecords().size(),
                        Math.min(currentPage * PAGE_SIZE, page.getTotal())); // 修复累计条数计算（避免最后一页超总条数）

                // 遍历当前页数据，执行后续处理（核心业务逻辑）
                for (OriginalAlarmRecordBak record : page.getRecords()) {
                    OriginalAlarmRecord original = new OriginalAlarmRecord();
                    BeanUtils.copyProperties(record, original);
                    log.info(original.toString());
                    handleMessage(original);
                }
            } else {
                log.info("第{}页无数据，跳过", currentPage);
            }

            currentPage++; // 页码自增，查询下一页
        } while (currentPage <= page.getPages()); // 直到页码超过总页数
    }
    public void handleMessage(OriginalAlarmRecord alarmRecord ) {

        //获取基础告警记录字段信息
        long tblId = alarmRecord.getTblId();
        String eventType = alarmRecord.getEventType();
        String alarmId = alarmRecord.getId();
        String roadId = alarmRecord.getRoadId();
        String imagePath = alarmRecord.getImagePath();
        String videoPath = alarmRecord.getVideoPath();
        String companyName = alarmRecord.getCompany();

        /**
         * @desc 数据时间范围校验
         * @remark
         */
//        if (alarmRecord.getAlarmTime().isBefore(startTime) || alarmRecord.getAlarmTime().isAfter(endTime)) {//判断告警时间是否在起始时间之后
//            return;
//        }

        /**
         * @desc 原始告警记录存储&&更新
         * @remark 记录已存在返回true，已存在记录仅更新，不做后续处理
         */
        if(originalAlarmServiceImpl.saveOrUpdateRecord(alarmRecord)){
            return;
        };

        /**
         * @desc 视频路径、图片路径为空告警记录直接剔除
         */
        if(!StringUtils.hasText(imagePath) || !StringUtils.hasText(videoPath) || !roadId.equals("33112")){
            log.info("图品/视频路径为空，调用通用算法检测 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            return;
        }

        if(alarmRecord.getCompany().equals("高德")){
            log.info("告警记录为高德数据，忽略处理 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
            return;
        }

        /**
         * @desc 非停驶、行人、抛洒物告警默认正检
         */
        if(!EVENT_TYPE_LIST.contains(eventType)){
            generalAlgorithm.checkDeal(alarmRecord,"", FlagTagUtil.CHECK_RESULT_RIGHT);
            return;
        }

        /**
         * @desc 视频连通性&&元数据校验
         * @remark 校验成功进行后续算法处理；校验失败调用通用算法处理
         */
        if (!extractFrameAlgorithm.checkVideoConnectivityWithRetry(alarmRecord)) {
            generalAlgorithm.checkDeal(alarmRecord,"视频资源访问异常，初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
            return;
        }

        /**
         * @desc 视频抽帧服务
         * @remark 校验成功进行后续算法处理；校验失败调用通用算法处理
         */
        if(!extractFrameAlgorithm.extractFrame(alarmRecord)){
            generalAlgorithm.checkDeal(alarmRecord,"视频抽帧异常，初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
            return;
        }

        /**
         * @desc 图片提框异常
         * @remark 校验成功进行后续算法处理；校验失败调用通用算法处理
         */
        if(!extractWindowAlgorithm.extractWindow(alarmRecord)){
            generalAlgorithm.checkDeal(alarmRecord,"图片提框异常，初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
            return;
        }

        /**
         * @desc 之江智能夜间检测逻辑：
         * 夜间时段（17:00~次日06:00）的之江智能记录，默认标记为"正检"（
         */
        int alarmHour = alarmRecord.getAlarmTime().getHour();
        boolean isZhijiang = "之江智能".equals(companyName);
        boolean isNightTime = alarmHour >= 17 || alarmHour < 6;
        if (isZhijiang && isNightTime) {
            generalAlgorithm.checkDeal(alarmRecord, "之江智能夜间检测，初检为正检", FlagTagUtil.CHECK_RESULT_RIGHT);
            return;
        }

        /**
         * 场景1：误检
         *      物体在路面外（路面内无记录，路面外有记录）→ 标记误检，调用通用算法处理
         * 场景2：正检
         *      检测类型非停驶、行人、抛洒物，通用算法处理
         *      检测类型为停驶、行人、抛洒物，调用后续算法处理
         */
        try{
            // 执行路面检测核心逻辑
            roadAlgorithm.roadCheckDeal(alarmRecord);
            // 查询路面检测结果（内/外记录数）
            int onRoadCount = roadCheckRecordServiceImpl.getRecordByTblIdAndTypeAndFlag(tblId,"road", 1).size();
            int outRoadCount = roadCheckRecordServiceImpl.getRecordByTblIdAndTypeAndFlag(tblId,"road", 2).size();
            log.info("路面检测结果统计：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 路面内:{}条 | 路面外:{}条 ", tblId, alarmId, imagePath, videoPath, onRoadCount, outRoadCount);

            if (onRoadCount == 0 && outRoadCount > 0) {
                generalAlgorithm.checkDeal(alarmRecord, "非路面物体,初检为误检", FlagTagUtil.CHECK_RESULT_ERROR);
                log.info("路面检测：物体在路面外，标记为误检：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                return;
            }

            if (!EVENT_TYPE_LIST.contains(eventType)) {
                log.info("路面检测：物体在路面内，但检测类型非停驶、行人、抛洒物，默认正检 | alarmId:{} | eventType:{} | imagePath:{} | videoPath:{} | roadId:{}", alarmId, eventType, imagePath, videoPath, roadId);
                generalAlgorithm.checkDeal(alarmRecord, "", FlagTagUtil.CHECK_RESULT_RIGHT);
                return;
            }else{
                log.info("路面检测：物体在路面内且事件类型需后续处理 | alarmId:{} | eventType:{} | imagePath:{} | videoPath:{}", alarmId, eventType, imagePath, videoPath);
            }
        } catch (Exception e) {
            log.error("路面检测发生异常，默认继续后续处理：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:{}", tblId, alarmId, imagePath, videoPath, e);
        }

        /**
         * 抛洒物算法检验：调用抛洒物专属算法，失败时兜底通用算法
         */
        if ("抛洒物".equals(eventType)) {
            try {
                boolean isPswSuccess = pswAlgorithm.pswDeal(alarmRecord);
                if (!isPswSuccess) {
                    // 3. 处理失败：日志明确原因，调用通用算法兜底
                    generalAlgorithm.checkDeal(alarmRecord, "抛洒物算法处理失败,初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
                    log.info("抛洒物算法处理失败,兜底调用通用算法：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                }
            } catch (Exception e) {
                generalAlgorithm.checkDeal(alarmRecord, "抛洒物算法执行异常，初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
                log.error("抛洒物算法处理异常,兜底调用通用算法：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:{}",tblId, alarmId, imagePath, videoPath, e);
            }
            return;
        }

        /**
         * 行人算法检验：调用行人专属算法，成功后执行系列后续处理，失败时兜底通用算法
         */
        if ("行人".equals(eventType)) {
            try {
                boolean isPersonDealSuccess = personAlgorithm.personDeal(alarmRecord);
                if (!isPersonDealSuccess) {
                    // 算法处理失败：日志说明原因，调用通用算法兜底
                    log.info("行人算法处理失败,兜底调用通用算法:tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                    generalAlgorithm.checkDeal(alarmRecord, "行人算法处理失败,初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
                } else {
                    checkResultAlgorithm.checkResultDealByAlgo(alarmRecord); // 1. 获取算法检测结果
                    log.debug("行人后续流程：图片数量校验完成： tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);

                    featureElementAlgorithm.featureElementDealByAlgo(alarmRecord); // 3. 特征要素判定
                    log.debug("行人后续流程：特征要素判定完成: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);

                    alarmCollectionAlgorithm.collectionDeal(alarmRecord); // 4. 告警集判定
                    log.debug("行人后续流程：告警集判定完成:tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);

                    collectionGroupAlgorithm.groupDeal(alarmRecord);// 5. 告警组判定
                    log.info("行人后续流程：全部完成: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                }
            } catch (Exception e) {
                log.error("行人算法处理异常 | 兜底调用通用算法 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
                generalAlgorithm.checkDeal(alarmRecord, "行人算法执行异常,初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
            }
            return;
        }

        /**
         * 停驶算法检验：调用停驶专属算法，成功后执行系列后续处理，失败时兜底通用算法
         */
        if ("停驶".equals(eventType)) {
            try {
                // 执行停驶核心算法，用变量名清晰表达执行结果
                boolean isVehicleDealSuccess = vehicleAlgorithm.vehicleDeal(alarmRecord);

                if (!isVehicleDealSuccess) {
                    log.info("停驶算法处理失败 | 兜底调用通用算法, tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                    generalAlgorithm.checkDeal(alarmRecord, "停驶算法处理失败,初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
                } else {
                    log.info("停驶算法处理成功 | 开始执行后续业务流程 | tblId:{} | alarmId:{}", alarmId);

                    checkResultAlgorithm.checkResultDealByAlgo(alarmRecord);// 1. 获取算法检测结果
                    log.debug("停驶后续流程：IOU校验完成（阈值0.2): tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);

                    featureElementAlgorithm.featureElementDealByAlgo(alarmRecord); // 3. 特征要素判定
                    log.debug("停驶后续流程：特征要素判定完成: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);

                    alarmCollectionAlgorithm.collectionDeal(alarmRecord);// 4. 告警集判定
                    log.debug("停驶后续流程：告警集判定完成: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);

                    collectionGroupAlgorithm.groupDeal(alarmRecord); // 5. 告警组判定
                    log.info("停驶后续流程：全部处理完成: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                }
            } catch (Exception e) {
                log.error("停驶算法处理异常 | 兜底调用通用算法 | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", alarmId, imagePath, videoPath, e);
                generalAlgorithm.checkDeal(alarmRecord, "停驶算法执行异常,初检为无法判断", FlagTagUtil.CHECK_RESULT_UNKNOWN);
            }
            return;
        }

        /**
         * 误检点位推送逻辑：
         * 1. 查询当前告警的检测结果
         * 2. 若为误检（checkFlag=2），获取最近50条误检记录的设备ID并推送刷新
         */
        // 1. 查询当前告警的检测结果（明确变量含义，避免歧义）
        CheckAlarmResult currentCheckResult = checkAlarmResultServiceImpl.getResultByTblId(tblId);
        log.info("开始误检点位推送处理 | 查询当前告警检测结果 | alarmId:{} | imagePath:{} | videoPath:{} | 检测结果是否存在:{}", alarmId, imagePath, videoPath, currentCheckResult != null);

        // 2. 处理检测结果不存在的场景
        if (currentCheckResult == null) {
            log.error("误检点位推送失败 | 未查询到当前告警的正误检标签 | roadId:{} | alarmId:{} | eventType:{} | imagePath:{} | videoPath:{}", roadId, alarmId, eventType, imagePath, videoPath);
            return; // 无检测结果，终止后续逻辑
        }

        // 3. 仅处理误检场景（checkFlag=2）
        if (currentCheckResult.getCheckFlag() != 2) {
            log.debug("当前告警非误检，无需推送点位 | checkFlag:{} | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", currentCheckResult.getCheckFlag(), tblId, alarmId, imagePath, videoPath);
            return;
        }

        try {
            // 4. 获取最近50条误检记录的设备ID列表
            List<String> recentFalseDeviceIds = checkAlarmResultServiceImpl.getRecentFalseCheckList(currentCheckResult.getId());
            log.info("查询到最近50条误检记录的设备ID | 数量:{} | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 设备ID列表:{}", recentFalseDeviceIds.size(), tblId, alarmId, imagePath, videoPath, recentFalseDeviceIds);

            // 5. 处理空列表场景（避免无效调用）
            if (recentFalseDeviceIds.isEmpty()) {
                log.warn("误检点位推送：最近50条误检记录的设备ID列表为空 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                return;
            }

            // 6. 推送设备ID列表刷新
            cloudEyesDeviceServiceImpl.refreshDevices(recentFalseDeviceIds);
            log.info("误检点位推送成功 | 设备ID数量:{} tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", recentFalseDeviceIds.size(), tblId, alarmId, imagePath, videoPath);
        } catch (Exception e) {
            log.error("误检点位推送异常 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", tblId, alarmId, imagePath, videoPath, e);
        }
    }
}