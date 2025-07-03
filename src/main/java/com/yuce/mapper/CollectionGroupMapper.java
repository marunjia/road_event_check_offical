package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.AlarmCollection;
import com.yuce.entity.CollectionGroupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface CollectionGroupMapper extends BaseMapper<CollectionGroupRecord> {

}