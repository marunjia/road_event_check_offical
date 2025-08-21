package com.yuce.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}