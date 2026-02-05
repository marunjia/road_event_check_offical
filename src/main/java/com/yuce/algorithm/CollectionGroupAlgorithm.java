package com.yuce.algorithm;

import com.yuce.entity.AlarmCollection;
import com.yuce.entity.CheckAlarmResult;
import com.yuce.entity.CollectionGroupRecord;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.impl.AlarmCollectionServiceImpl;
import com.yuce.service.impl.CheckAlarmResultServiceImpl;
import com.yuce.service.impl.CollectionGroupServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * @ClassName CollectionGroupAlgorithm
 * @Description 告警组处理逻辑：负责告警记录分组、同组判定及分组记录持久化
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/29 15:07
 * @Version 1.0
 */
@Component
@Slf4j
public class CollectionGroupAlgorithm {

    @Autowired
    private CollectionGroupServiceImpl collectionGroupServiceImpl;

    @Autowired
    private AlarmCollectionServiceImpl alarmCollectionServiceImpl;

    @Autowired
    private CheckAlarmResultServiceImpl checkAlarmResultServiceImpl;

    public void groupDeal(OriginalAlarmRecord record) {

        // 获取基础字段信息（用 Long 接收，避免 null 转 long 空指针）
        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String eventType = record.getEventType();
        LocalDateTime alarmTime = record.getAlarmTime();

        try {
            // 1. 查询告警记录归属告警集（处理 null 场景）
            AlarmCollection alarmCollection = alarmCollectionServiceImpl.getCollectionByTblId(tblId);
            if (alarmCollection == null || alarmCollection.getId() == null) {
                log.error("告警组分组失败：未查询到归属告警集: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                return;
            }
            Integer collectionId = alarmCollection.getId();

            // 2. 判断告警记录是否已经分组（避免重复分组）
            CollectionGroupRecord existGroup = collectionGroupServiceImpl.getGroupByKey(alarmId, imagePath, videoPath);
            if (existGroup != null) {
                log.info("告警记录已分组，不再处理: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                return;
            }

            // 3. 查询检测结果（处理 null 场景）
            CheckAlarmResult checkAlarmResult = checkAlarmResultServiceImpl.getResultByTblId(tblId);
            String checkName = "";
            if (checkAlarmResult == null || checkAlarmResult.getCheckName() == null) {
                log.warn("告警组分组异常：未查询到检测结果为null: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                checkName="noitem";
            }else{
                checkName = checkAlarmResult.getCheckName();
            }

            // 4. 生成分组 ID（规则：告警集ID_检测名称）
            String groupId = collectionId + "_" + checkName;
            log.debug("告警记录生成分组 ID: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | collectionId:{} | checkName:{} | groupId:{}", tblId, alarmId, imagePath, videoPath, collectionId, checkName, groupId);

            // 5. 查询组内最新一条告警记录
            CollectionGroupRecord latestGroup = collectionGroupServiceImpl.queryTop1ByCollectionIdAndGroupId(collectionId, groupId);
            CollectionGroupRecord groupRecord = new CollectionGroupRecord();

            // 6. 确定分组图片（最新时间的图片作为分组图片）
            if (latestGroup == null) {
                // 分组尚未创建，用当前告警图片
                groupRecord.setGroupImageUrl(imagePath);
                log.debug("分组首次创建，使用当前告警图片: tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | groupId:{}", tblId, alarmId, imagePath, videoPath, groupId);
            } else {
                LocalDateTime latestGroupTime = latestGroup.getAlarmTime();
                if (latestGroupTime == null) {
                    // 历史分组时间为空，用当前告警图片
                    groupRecord.setGroupImageUrl(imagePath);
                    log.warn("分组历史记录告警时间为空，使用当前告警图片:tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | groupId:{}, latestGroupId:{}", tblId,  alarmId, imagePath, videoPath, groupId, latestGroup.getId());
                } else if (alarmTime.isAfter(latestGroupTime)) {
                    // 当前告警时间更新，替换分组图片
                    groupRecord.setGroupImageUrl(imagePath);
                    log.debug("当前告警时间更新，替换分组图片 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | groupId:{}, oldImage:{}, newImage:{}", tblId, alarmId, imagePath, videoPath, groupId, latestGroup.getImagePath(), imagePath);
                } else {
                    // 沿用历史最新图片
                    groupRecord.setGroupImageUrl(latestGroup.getImagePath());
                }
            }

            // 7. 填充分组记录字段（自动填充字段无需手动赋值）
            groupRecord.setCollectionId(collectionId);
            groupRecord.setGroupId(groupId);
            groupRecord.setGroupItemType(checkName);
            groupRecord.setTblId(tblId);
            groupRecord.setAlarmId(alarmId);
            groupRecord.setImagePath(imagePath);
            groupRecord.setVideoPath(videoPath);
            groupRecord.setEventType(eventType);
            groupRecord.setAlarmTime(alarmTime);

            // 8. 保存分组记录
            boolean saveSuccess = collectionGroupServiceImpl.save(groupRecord);
            if (saveSuccess) {
                log.info("告警记录分组成功 | tblId:{} | alarmId:{}, imagePath:{} | videoPath:{} | groupId:{}, collectionId:{}", tblId, alarmId, imagePath, videoPath, groupId, collectionId);
            } else {
                log.error("告警记录分组失败：保存分组记录失败 | tblId:{} | alarmId:{}, imagePath:{} | videoPath:{} | groupId:{}, collectionId:{}", tblId, alarmId, imagePath, videoPath, groupId, collectionId);
            }
        } catch (Exception e) {
            log.error("告警记录分组异常 | tblId:{} | alarmId:{}, imagePath:{}, videoPath:{}, 异常信息:{}", tblId, alarmId, imagePath, videoPath, e);
        }
    }
}