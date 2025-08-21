package com.yuce.config;

import com.yuce.handler.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @ClassName WebMvcConfig
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/30 23:46
 * @Version 1.0
 */

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                        "/login",       // 登录接口放行
                        "/alarmCollections/gxVersion", //放行高信接口
                        "/alarmCollections/gxVersion/**", //放行高信接口
                        "/checkEvents/insert/byAlgorithm", //放行抛洒物接口
                        "/register",    // 可选：放行 /login 下所有子路径
                        "/error",       // Spring 默认错误路径建议也放行
                        "/favicon.ico"  // 防止浏览器请求图标被拦截
                );
    }
}