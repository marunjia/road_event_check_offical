package com.yuce.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @ClassName VideoProperties
 * @Description 视频属性信息配置类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:18
 * @Version 1.0
 */


@Data
@Component
@ConfigurationProperties(prefix = "video")
public class VideoProperties {
    private int frameCount; //抽帧数量
    private int frameInterval; //帧数间隔
    private String imageBaseDir; //图片文件存储路径
    private String videoBaseDir; //视频文件存储路径
}