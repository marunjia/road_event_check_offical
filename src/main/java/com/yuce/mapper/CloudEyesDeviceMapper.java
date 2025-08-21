package com.yuce.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.CloudEyesDevice;
import org.apache.ibatis.annotations.Mapper;
/**
 * @ClassName CloudEyesDeviceMapper
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/20 15:55
 * @Version 1.0
 */


@Mapper
@DS("secondary")   // 指定使用 secondary 数据源
public interface CloudEyesDeviceMapper extends BaseMapper<CloudEyesDevice> {

}