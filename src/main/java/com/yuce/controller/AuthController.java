package com.yuce.controller;

import com.alibaba.fastjson.JSONObject;
import com.yuce.common.ApiResponse;
import com.yuce.entity.SysUser;
import com.yuce.service.impl.SysUserServiceImpl;
import com.yuce.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @ClassName Auth
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/27 09:58
 * @Version 1.0
 */

@RestController
public class AuthController {

    @Autowired
    private SysUserServiceImpl sysUserServiceImpl;

    @Autowired
    private JwtUtil jwtUtil;

    // 用户登录
    @PostMapping("/login")
    public ApiResponse login(@RequestBody SysUser user) {
        SysUser existUser = sysUserServiceImpl.login(user.getUsername(), user.getPassword());
        if (existUser != null) {
            String token = jwtUtil.generateToken(existUser.getId(), existUser.getUsername());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username",user.getUsername());
            jsonObject.put("password",user.getPassword());
            jsonObject.put("token", "Bearer ".concat(token));
            return ApiResponse.success(jsonObject);
        } else {
            return ApiResponse.fail(401, "用户名或密码错误");
        }
    }


    // 登出接口
    @RequestMapping("/logout")
    @ResponseBody
    public ApiResponse logout(HttpSession session) {
        session.invalidate(); // 使 Session 失效
        return ApiResponse.success("登出成功");
    }

    // 用户注册
    @PostMapping("/register")
    public ApiResponse register(@RequestBody SysUser user) {
        // 1. 校验用户名是否为空
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return ApiResponse.fail(400, "用户名不能为空");
        }

        // 2. 校验密码是否为空
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return ApiResponse.fail(400, "密码不能为空");
        }

        // 3. 调用服务层注册
        boolean result = sysUserServiceImpl.register(user);

        // 4. 返回结果
        if (result) {
            // 可选：注册成功后直接返回 token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", user.getUsername());
            jsonObject.put("token", "Bearer ".concat(token));
            return ApiResponse.success(jsonObject);
        } else {
            return ApiResponse.fail(401, "用户名已存在");
        }
    }
}