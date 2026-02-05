package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.RoadCheckRecord;
import com.yuce.mapper.RoadCheckRecordMapper;
import com.yuce.service.RoadCheckRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName RoadCheckRecordServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/14 10:47
 * @Version 1.0
 */

@Service
@Slf4j
public class RoadCheckRecordServiceImpl extends ServiceImpl<RoadCheckRecordMapper, RoadCheckRecord> implements RoadCheckRecordService {

    @Autowired
    private RoadCheckRecordMapper roadCheckRecordMapper;

    /**
     * @desc 批量新增路面检测结果
     * @param list
     */
    public void insertRoadCheckRecord(List<RoadCheckRecord> list) {
        this.saveBatch(list);
    }

    /**
     * @desc 根据tblId、检测类型、检测结果查询路面检测匹配结果
     * @param tblId
     * @param type
     * @param checkFlag
     * @return
     */
    public List<RoadCheckRecord> getRecordByTblIdAndTypeAndFlag(long tblId, String type, Integer checkFlag){
        return roadCheckRecordMapper.getRecordByTblIdAndTypeAndFlag(tblId, type, checkFlag);
    }

    /**
     * @desc 根据tblId和检测类型查询路面检测结果
     * @param tblId
     * @param type
     * @return
     */
    public List<RoadCheckRecord> getRecordByTblIdAndType(long tblId, String type){
        QueryWrapper<RoadCheckRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tbl_id", tblId);
        queryWrapper.eq("type", type);
        return roadCheckRecordMapper.selectList(queryWrapper);
    }



}