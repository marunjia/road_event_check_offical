package com.yuce.task;

import com.alibaba.fastjson.JSONObject;
import com.yuce.config.VideoProperties;
import com.yuce.entity.*;
import com.yuce.service.impl.*;
import com.yuce.util.KafkaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.io.File;

/**
 * @ClassName EventFetchTask
 * @Description 事件捕获任务
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:01
 * @Version 1.0
 */
@Slf4j
@Component
public class EventFetchTask {

    @Autowired
    private OriginalAlarmServiceImpl originalAlarmServiceImpl;

    @Autowired
    private PswAlgorithmServiceImpl pswAlgorithmServiceImpl;

    @Autowired
    private GeneralAlgorithmServiceImpl generalAlgorithmServiceImpl;

    @Autowired
    private PersonAlgorithmServiceImpl personAlgorithmServiceImpl;

    @Autowired
    private VehicleAlgorithmServiceImpl vehicleAlgorithmServiceImpl;

    @Autowired
    private VideoDealServiceImpl videoDealServiceImpl;

    @Autowired
    private ExtractWindowServiceImpl extractWindowServiceImpl;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    @Autowired
    private FeatureElementServiceImpl featureElementServiceImpl;

    @Autowired
    private CollectionServiceImpl collectionServiceImpl;

    @Autowired
    private VideoProperties videoProperties;

    @Autowired
    private KafkaUtil kafkaUtil;

    //输出抽帧图片存放文件目录
    private String outputDir;

    //启动调用一次
    @PostConstruct
    public void init() {
        outputDir = videoProperties.getOutputDir();
        log.info("项目启动中...");
        dirInit(outputDir);
        log.info("项目初始化完成");
        new Thread(this::coreDeal).start(); // 后台线程持续消费
    }

    /**
     * @desc 核心处理逻辑
     */
    public void coreDeal() {

        KafkaConsumer consumer = kafkaUtil.getConsumer();//获取kafka消费者

        while(true){
            ConsumerRecords<String, String> records = consumer.poll(5000);//循环拉取处理数据
            for (ConsumerRecord<String, String> record : records) {
                String kafkaRecord = record.value().replaceAll("^\"|\"$", "").replace("\\", "");//kafka数据json结构化
                OriginalAlarmRecord alarmRecord = JSONObject.parseObject(kafkaRecord, OriginalAlarmRecord.class);//转换为原始告警记录对象
                String eventType = alarmRecord.getEventType();
                String alarmId = alarmRecord.getId();
                String roadId = alarmRecord.getRoadId();
                /**
                 * 事件类型筛选：
                 *  --仅获取33141和33112道路数据
                 *  --仅获取停驶、行人、抛洒物类型
                 *  --其他类型丢弃
                 */
                if (("33141".equals(roadId) || "33112".equals(roadId)) && ("停驶".equals(eventType) || "行人".equals(eventType) || "抛洒物".equals(eventType))){
                    log.info("分区:{},偏移量:{}，告警记录id:{},告警记录类型:{},告警时间:{}",record.partition(),record.offset(),alarmId,eventType,alarmRecord.getAlarmTime());
                    if(originalAlarmServiceImpl.getRecordByAlarmIdAndTag(alarmId)){
                        originalAlarmServiceImpl.saveOrUpdate(alarmRecord);//告警信息已经接受且算法处理完成
                    }else{
                        //校验视频格式以及抽帧处理
                        originalAlarmServiceImpl.save(alarmRecord);//存储数据
                        if(videoDealServiceImpl.processVideo(alarmRecord) && extractWindowServiceImpl.extractDeal(alarmRecord)) {
                            if ("抛洒物".equals(eventType)) {
                                if(!pswAlgorithmServiceImpl.pswDeal(alarmRecord)){
                                    log.error("抛洒物算法处理异常:alarmId{}",alarmId);
                                    generalAlgorithmServiceImpl.checkDeal(alarmId);
                                }
                            } else if("行人".equals(eventType)) {
                                if(!personAlgorithmServiceImpl.personDeal(alarmRecord)){
                                    log.error("行人算法处理异常:alarmId{}",alarmId);
                                    generalAlgorithmServiceImpl.checkDeal(alarmId);
                                }else{
                                    checkAlarmResultServiceImpl.checkResultByImgNum(alarmId, "person");//更新算法结果:只要有一张图片核检成功即认为为正检；
                                    featureElementServiceImpl.featureElementDeal(alarmId);//更新关联要素
                                    collectionServiceImpl.collectionDeal(alarmId);//更新告警集
                                }
                            } else if("停驶".equals(eventType)) {
                                if(!vehicleAlgorithmServiceImpl.vehicleDeal(alarmRecord)){
                                    log.error("停驶算法处理异常:alarmId{}",alarmId);
                                    generalAlgorithmServiceImpl.checkDeal(alarmId);
                                }else{
                                    checkAlarmResultServiceImpl.checkResultByIou(alarmId, 0.2, 1);//更新算法结果:只要有2张图片核检成功即认为为正检；
                                    featureElementServiceImpl.featureElementDeal(alarmId);//更新关联要素
                                    collectionServiceImpl.collectionDeal(alarmId);//更新告警集
                                }
                            } else{
                                generalAlgorithmServiceImpl.checkDeal(alarmId);
                            }
                        }else {
                            log.error("视频格式或抽框服务异常:alarmId{}",alarmId);
                            generalAlgorithmServiceImpl.checkDeal(alarmId);
                        }
                    }
                    alarmRecord.setConsumeTag(1);
                    originalAlarmServiceImpl.saveOrUpdate(alarmRecord);//算法处理完成变更状态
                }
                kafkaUtil.updateOffset(record);//更新kafka偏移量
            }
        }
    }

    /**
     * @desc 初始化抽帧图片存储目录
     * @param dirPath
     */
    public void dirInit(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("创建输出目录失败: " + outputDir);
            }
        }
    }
}