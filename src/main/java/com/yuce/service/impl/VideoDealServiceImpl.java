package com.yuce.service.impl;

import com.yuce.config.VideoProperties;
import com.yuce.entity.FrameImageInfo;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.VideoDealService;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class VideoDealServiceImpl implements VideoDealService {

    @Autowired
    private VideoProperties videoProperties;

    @Autowired
    private FrameImageServiceImpl frameImageServiceImpl;

    private String outputDir;
    private int frameCount;

    @PostConstruct
    public void init() {
        outputDir = videoProperties.getOutputDir();
        frameCount = videoProperties.getFrameCount();
        FFmpegLogCallback.set();
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);
    }

    @Override
    public boolean processVideo(OriginalAlarmRecord originalAlarmRecord) {
        String eventType = originalAlarmRecord.getEventType();
        String videoPath = originalAlarmRecord.getVideoPath();
        if (videoPath == null || videoPath.trim().isEmpty()) {
            log.info("视频链接不能为空");
            return false;
        }

        FFmpegFrameGrabber grabber = null;
        try {
            grabber = new FFmpegFrameGrabber(videoPath);
            grabber.start();

            if (grabber.grabImage() == null) {
                log.info("视频为空，路径：{}", videoPath);
                return false;
            }

            String format = Optional.ofNullable(grabber.getFormat()).orElse("").toLowerCase();
            String codec = Optional.ofNullable(grabber.getVideoCodecName()).orElse("");
            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            double duration = grabber.getLengthInTime() / 1_000_000.0;

            if ("停驶".equals(eventType)) {
                if (duration < 9.0) {
                    log.info("视频时长不足10秒：{} 秒，路径：{}", duration, videoPath);
                    return false;
                }
            } else {
                if (duration < 5.0) {
                    log.info("视频时长不足5秒：{} 秒，路径：{}", duration, videoPath);
                    return false;
                }
            }

            if (!isFormatSupported(format)) {
                log.info("不支持的视频格式：{}，路径：{}", format, videoPath);
                return false;
            }

            if (!isCodecSupported(codec)) {
                log.info("不支持的视频编码器：{}，路径：{}", codec, videoPath);
                return false;
            }

            if (!isResolutionSupported(width, height)) {
                log.info("不支持的视频分辨率：{}x{}，路径：{}", width, height, videoPath);
                return false;
            }

            int totalFrames = grabber.getLengthInFrames();
            if (totalFrames <= 0) {
                log.warn("视频帧数非法，id={}", originalAlarmRecord.getId());
                return false;
            }

            Java2DFrameConverter converter = new Java2DFrameConverter();
            int saved = 0;

            if ("停驶".equals(eventType)) {
                // 使用时间戳抓帧：7s, 8s, 9s
                long[] timestamps = {7_000_000L, 8_000_000L, 9_000_000L};

                for (int i = 0; i < timestamps.length; i++) {
                    grabber.setTimestamp(timestamps[i]);
                    Frame frame = grabber.grabImage();
                    if (frame != null) {
                        BufferedImage image = converter.getBufferedImage(frame);
                        String filePath = outputDir + File.separator + "frame_" + originalAlarmRecord.getId() + "_" + saved + ".jpg";
                        ImageIO.write(image, "jpg", new File(filePath));

                        FrameImageInfo frameImageInfo = new FrameImageInfo();
                        frameImageInfo.setAlarmId(originalAlarmRecord.getId());
                        frameImageInfo.setFrameNum(timestamps.length);
                        frameImageInfo.setImageSortNo(saved);
                        frameImageInfo.setImageUrl(filePath);
                        frameImageInfo.setCreateTime(LocalDateTime.now());
                        frameImageInfo.setUpdateTime(LocalDateTime.now());
                        frameImageServiceImpl.save(frameImageInfo);

                        saved++;
                    } else {
                        log.warn("按时间戳抓帧失败：{} 微秒，视频：{}", timestamps[i], videoPath);
                    }
                }
                grabber.stop();
                return saved == timestamps.length;

            } else {
                // 使用帧位置抓帧
                int[] framePositions = {
                        (int) (totalFrames * 0.5),
                        (int) (totalFrames * 0.8),
                        (int) (totalFrames * 0.9)
                };

                for (int i = 0; i < framePositions.length; i++) {
                    grabber.setFrameNumber(framePositions[i]);
                    Frame frame = grabber.grabImage();
                    if (frame != null) {
                        BufferedImage image = converter.getBufferedImage(frame);
                        String filePath = outputDir + File.separator + "frame_" + originalAlarmRecord.getId() + "_" + saved + ".jpg";
                        ImageIO.write(image, "jpg", new File(filePath));

                        FrameImageInfo frameImageInfo = new FrameImageInfo();
                        frameImageInfo.setAlarmId(originalAlarmRecord.getId());
                        frameImageInfo.setFrameNum(framePositions.length);
                        frameImageInfo.setImageSortNo(saved);
                        frameImageInfo.setImageUrl(filePath);
                        frameImageInfo.setCreateTime(LocalDateTime.now());
                        frameImageInfo.setUpdateTime(LocalDateTime.now());
                        frameImageServiceImpl.save(frameImageInfo);

                        saved++;
                    } else {
                        log.warn("按帧抓取失败，帧编号：{}，视频：{}", framePositions[i], videoPath);
                    }
                }
                grabber.stop();
                return saved == framePositions.length;
            }

        } catch (Exception e) {
            log.error("视频处理失败，id={}, url={}, 原因：{}", originalAlarmRecord.getId(), videoPath, e.getMessage(), e);
            try {
                if (grabber != null) grabber.stop();
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    private boolean isFormatSupported(String format) {
        return format.contains("mp4") || format.contains("avi") || format.contains("matroska");
    }

    private boolean isCodecSupported(String codec) {
        return codec.equalsIgnoreCase("h264") || codec.equalsIgnoreCase("hevc");
    }

    private boolean isResolutionSupported(int width, int height) {
        return width >= 1280 && height >= 720 && width <= 4096 && height <= 3072;
    }
}