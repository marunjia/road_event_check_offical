package com.yuce.util;

import com.yuce.entity.OriginalAlarmRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 视频元数据校验工具类：验证视频格式、编码、分辨率、时长等是否符合系统要求
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 禁止实例化工具类
public class VideoCheckUtil {

    // ------------------------------ 常量定义（集中管理配置，便于调整） ------------------------------
    /** 支持的视频编码格式（小写） */
    private static final List<String> SUPPORTED_VIDEO_CODECS = Arrays.asList("h264", "hevc");

    /** 支持的视频容器格式（小写，包含关键字即可匹配） */
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("mp4", "matroska", "webm", "mkv", "avi");

    /** 最小宽度（1280px） */
    private static final int MIN_WIDTH = 1280;
    /** 最小高度（720px） */
    private static final int MIN_HEIGHT = 720;
    /** 最大宽度（4096px） */
    private static final int MAX_WIDTH = 4096;
    /** 最大高度（3072px） */
    private static final int MAX_HEIGHT = 3072;

    /** 停驶事件最小视频时长（秒） */
    private static final double STOPPED_MIN_DURATION = 9.0;
    /** 抛洒物/行人事件最小视频时长（秒） */
    private static final double OBJECT_PEDESTRIAN_MIN_DURATION = 5.0;


    // ------------------------------ 核心校验方法 ------------------------------
    /**
     * 验证视频元数据是否符合系统要求（格式、编码、分辨率、时长等）
     * @param grabber FFmpeg帧抓取器（已初始化）
     * @param record 原始告警记录（包含事件类型等上下文）
     * @param alarmId 告警ID（用于日志追踪）
     * @return 校验通过返回true，否则false
     * @throws FFmpegFrameGrabber.Exception 抓取器操作异常
     */
    public static boolean checkVideoMetaData(FFmpegFrameGrabber grabber, OriginalAlarmRecord record, String alarmId)
            throws FFmpegFrameGrabber.Exception {
        // 1. 提取视频元数据（统一处理空值）
        String format = Optional.ofNullable(grabber.getFormat()).orElse("").toLowerCase();
        String codec = Optional.ofNullable(grabber.getVideoCodecName()).orElse("").toLowerCase();
        int width = grabber.getImageWidth();
        int height = grabber.getImageHeight();
        double durationSec = grabber.getLengthInTime() / 1_000_000.0; // 转换为秒
        int totalFrames = grabber.getLengthInFrames();
        String eventType = Optional.ofNullable(record.getEventType()).orElse("未知事件");

        // 2. 打印元数据日志（便于问题排查）
        log.info("[视频元数据] 告警ID:{} | 格式:{} | 编码:{} | 分辨率:{}x{} | 时长:{}秒 | 总帧数:{} | 事件类型:{}",
                alarmId, format, codec, width, height, durationSec, totalFrames, eventType);

        // 3. 执行各项校验（短路逻辑：一项失败则整体失败）
        return checkVideoValid(grabber, record, alarmId)
                && isDurationSupported(eventType, durationSec, alarmId, record)
                && isFormatSupported(format, alarmId, record)
                && isCodecSupported(codec, alarmId, record)
                && isResolutionSupported(width, height, alarmId, record)
                && isFrameCountValid(totalFrames, alarmId);
    }


    // ------------------------------ 私有校验方法（单一职责，日志包含关键信息） ------------------------------
    /**
     * 检查视频是否有有效图像（非空视频）
     */
    private static boolean checkVideoValid(FFmpegFrameGrabber grabber, OriginalAlarmRecord record, String alarmId)
            throws FFmpegFrameGrabber.Exception {
        boolean hasValidImage = grabber.grabImage() != null;
        if (!hasValidImage) {
            log.warn("[视频校验失败] 视频为空 | 告警ID:{} | 记录ID:{} | 视频路径:{}",
                    alarmId, record.getTblId(), record.getVideoPath());
        }
        return hasValidImage;
    }

    /**
     * 检查视频格式是否支持（匹配支持的容器格式）
     */
    private static boolean isFormatSupported(String format, String alarmId, OriginalAlarmRecord record) {
        boolean supported = SUPPORTED_FORMATS.stream().anyMatch(format::contains);
        if (!supported) {
            log.warn("[视频校验失败] 格式不支持 | 告警ID:{} | 记录ID:{} | 视频路径:{} | 当前格式:{} | 支持格式:{}",
                    alarmId, record.getTblId(), record.getVideoPath(), format, SUPPORTED_FORMATS);
        }
        return supported;
    }

    /**
     * 检查视频编码是否支持（匹配支持的编码格式）
     */
    private static boolean isCodecSupported(String codec, String alarmId, OriginalAlarmRecord record) {
        boolean supported = SUPPORTED_VIDEO_CODECS.contains(codec);
        if (!supported) {
            log.warn("[视频校验失败] 编码不支持 | 告警ID:{} | 记录ID:{} | 视频路径:{} | 当前编码:{} | 支持编码:{}",
                    alarmId, record.getTblId(), record.getVideoPath(), codec, SUPPORTED_VIDEO_CODECS);
        }
        return supported;
    }

    /**
     * 检查视频分辨率是否在支持范围内（宽度和高度均需符合）
     */
    private static boolean isResolutionSupported(int width, int height, String alarmId, OriginalAlarmRecord record) {
        boolean widthValid = width >= MIN_WIDTH && width <= MAX_WIDTH;
        boolean heightValid = height >= MIN_HEIGHT && height <= MAX_HEIGHT;
        boolean supported = widthValid && heightValid;

        if (!supported) {
            log.warn("[视频校验失败] 分辨率不支持 | 告警ID:{} | 记录ID:{} | 视频路径:{} | 当前分辨率:{}x{} | 支持范围:{}x{}-{}x{}",
                    alarmId, record.getTblId(), record.getVideoPath(), width, height,
                    MIN_WIDTH, MIN_HEIGHT, MAX_WIDTH, MAX_HEIGHT);
        }
        return supported;
    }

    /**
     * 检查视频时长是否符合事件类型要求（不同事件类型有不同时长阈值）
     */
    private static boolean isDurationSupported(String eventType, double durationSec, String alarmId, OriginalAlarmRecord record) {
        // 根据事件类型判断最小时长
        double minRequired;
        switch (eventType) {
            case "停驶":
                minRequired = STOPPED_MIN_DURATION;
                break;
            case "抛洒物":
            case "行人":
                minRequired = OBJECT_PEDESTRIAN_MIN_DURATION;
                break;
            default:
                minRequired = 1.0;
        }

        boolean supported = durationSec >= minRequired;
        if (!supported) {
            log.warn("[视频校验失败] 时长不足 | 告警ID:{} | 记录ID:{} | 视频路径:{} | 事件类型:{} | 当前时长:{}秒 | 最小要求:{}秒",
                    alarmId, record.getTblId(), record.getVideoPath(), eventType, durationSec, minRequired);
        }
        return supported;
    }

    /**
     * 检查视频帧数是否合法（必须大于0）
     */
    private static boolean isFrameCountValid(int totalFrames, String alarmId) {
        boolean valid = totalFrames > 0;
        if (!valid) {
            log.warn("[视频校验失败] 帧数非法 | 告警ID:{} | 总帧数:{}（必须>0）", alarmId, totalFrames);
        }
        return valid;
    }
}