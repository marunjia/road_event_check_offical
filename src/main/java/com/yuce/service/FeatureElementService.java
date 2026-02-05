package com.yuce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yuce.entity.FeatureElementRecord;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @ClassName FeatureElementService
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/6 13:45
 * @Version 1.0
 */
public interface FeatureElementService extends IService<FeatureElementRecord> {

    /**
     * @desc 根据tblId查询特征要素
     * @param tblId
     * @return
     */
    FeatureElementRecord getFeatureByTblId(long tblId);

    /**
     * @desc 根据tblId特征要素是否存在
     * @param tblId
     * @return
     */
    boolean isExistByTblId(long tblId);

    /**
     * @desc 根据特征要素id更新告警集关联状态
     * @param id
     * @param collectionMatchStatus
     * @return
     */
    int updateCollectionMatchStatus(Integer id, Integer collectionMatchStatus);

    /**
     * @desc 根据特征要素id更新告警集人工核查标签
     * @param id
     * @param personCheckFlag
     * @return
     */
    int updatePersonCheckFlag(Integer id, Integer personCheckFlag);

    /**
     * @desc 根据特征要素id更新告警记录是否正确匹配标签
     * @param id
     * @param matchCheckFlag
     * @return
     */
    int updateMatchCheckFlag(Integer id, Integer matchCheckFlag);

    /**
     * @desc 根据特征要素id更新告警记录匹配错误原因
     * @param id
     * @param matchCheckReason
     * @return
     */
    int updateMatchCheckReason(Integer id, String matchCheckReason);

    /**
     * @desc 根据告警记录时间区间查询特征要素列表
     * @param previousMinute
     * @param currentMinute
     * @return
     */
    List<FeatureElementRecord> getListByTimeRange(LocalDateTime previousMinute, LocalDateTime currentMinute);

}