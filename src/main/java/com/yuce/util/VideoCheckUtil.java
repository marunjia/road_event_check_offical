package com.yuce.util;

import com.yuce.entity.OriginalAlarmRecord;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @ClassName VideoCheckUtil
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/25 15:56
 * @Version 1.0
 */

@Slf4j
@Component
public class VideoCheckUtil {

    /**
     * 验证视频元数据
     */
    public static boolean checkVideoMetaData(FFmpegFrameGrabber grabber,
                                          OriginalAlarmRecord record,
                                          String alarmId) throws FFmpegFrameGrabber.Exception {
        String format = Optional.ofNullable(grabber.getFormat()).orElse("").toLowerCase();
        String codec = Optional.ofNullable(grabber.getVideoCodecName()).orElse("");
        int width = grabber.getImageWidth();
        int height = grabber.getImageHeight();
        double duration = grabber.getLengthInTime() / 1_000_000.0;
        int totalFrames = grabber.getLengthInFrames();

        log.info("视频格式:{},视频编码类型:{},视频分辨率:{}x{},视频时长(秒):{},总帧数:{}",
                format, codec, width, height, duration, totalFrames);

        if (grabber.grabImage() == null) {
            log.info("视频为空:id->{}, alarmId->{}, imagePath->{}, videoPath->{}",
                    record.getTblId(), alarmId, record.getImagePath(), record.getVideoPath());
            return false;
        }

        if (!isDurationSupported(record.getEventType(), duration)) {
            log.info("视频时长不支持: id->{}, alarmId->{}, imagePath->{}, videoPath->{}, duration->{}",
                    record.getTblId(), alarmId, record.getImagePath(), record.getVideoPath(), duration);
            return false;
        }

        if (!isFormatSupported(format)) {
            log.info("视频格式不支持: id->{}, alarmId->{}, imagePath->{}, videoPath->{}, format->{}",
                    record.getTblId(), alarmId, record.getImagePath(), record.getVideoPath(), format);
            return false;
        }

        if (!isCodecSupported(codec)) {
            log.info("视频编码器不支持: id->{}, alarmId->{}, imagePath->{}, videoPath->{}, codec->{}",
                    record.getTblId(), alarmId, record.getImagePath(), record.getVideoPath(), codec);
            return false;
        }

        if (!isResolutionSupported(width, height)) {
            log.info("视频分辨率不支持: id->{}, alarmId->{}, imagePath->{}, videoPath->{}, 分辨率->{}x{}",
                    record.getTblId(), alarmId, record.getImagePath(), record.getVideoPath(), width, height);
            return false;
        }

        if (totalFrames <= 0) {
            log.info("视频帧数非法: totalFrames->{}", totalFrames);
            return false;
        }
        return true;
    }

    /**
     * 检查视频格式
     */
    public static  boolean isFormatSupported(String format) {
        return format.contains("mp4") || format.contains("avi") || format.contains("matroska");
    }

    /**
     * 检查视频编码
     */
    public static  boolean isCodecSupported(String codec) {
        return codec.equalsIgnoreCase("h264") || codec.equalsIgnoreCase("hevc");
    }

    /**
     * 检查视频分辨率
     */
    public static  boolean isResolutionSupported(int width, int height) {
        return width >= 1280 && height >= 720 && width <= 4096 && height <= 3072;
    }

    /**
     * 检查视频时长
     */
    public static  boolean isDurationSupported(String eventType, double duration) {
        if ("停驶".equals(eventType) && duration >= 9.0) {
            return true;
        } else if(("抛洒物".equals(eventType) || "行人".equals(eventType)) && duration >= 5.0 ){
            return true;
        }
        return false;
    }
}