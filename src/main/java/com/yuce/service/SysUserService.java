package com.yuce.service;

import com.yuce.entity.SysUser;

public interface SysUserService {
    boolean register(SysUser user);

    SysUser login(String username, String password);
}