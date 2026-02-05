package com.yuce.algorithm;

import com.yuce.config.VideoProperties;
import com.yuce.entity.FrameImageInfo;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.impl.FrameImageServiceImpl;
import com.yuce.util.FileUtil;
import com.yuce.util.VideoCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 视频抽帧算法服务（带视频连通性与元数据校验）
 */
@Service
@Slf4j
public class ExtractFrameAlgorithm {

    private static final int MAX_RETRY_TIMES = 3;
    private static final long RETRY_INTERVAL_MS = 5_000;
    private static final String IMAGE_FORMAT = "jpg";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final long[] STOPPED_TIMESTAMPS = {7_000_000L, 8_000_000L, 9_000_000L};
    private static final int[] DEFAULT_PERCENTAGES = {50, 80, 95};

    private int targetFrameCount;
    private String videoBaseDir;
    private String imageBaseDir;

    @Autowired
    private VideoProperties videoProperties;

    @Autowired
    private FrameImageServiceImpl frameImageServiceImpl;

    @PostConstruct
    public void init() {
        this.targetFrameCount = videoProperties.getFrameCount();
        this.videoBaseDir = videoProperties.getVideoBaseDir();
        this.imageBaseDir = videoProperties.getImageBaseDir();

        Assert.isTrue(targetFrameCount > 0, "抽帧图片数量必须大于0");
        Assert.hasText(videoBaseDir, "videoBaseDir配置对应目录不存在");
        Assert.hasText(imageBaseDir, "imageBaseDir配置对应目录不存在");

        avutil.av_log_set_level(avutil.AV_LOG_QUIET);
        FFmpegLogCallback.set();

        log.info("抽帧算法初始化完成, 目标抽帧图片数量:{}, 视频存储路径:{}, 图片存储路径:{}", targetFrameCount, videoBaseDir, imageBaseDir);
    }

    /**
     * @desc 视频抽帧处理
     * @param record
     * @throws Exception
     */
    public boolean extractFrame(OriginalAlarmRecord record){
        long tblId = record.getTblId();
        String alarmId = record.getId();
        String videoPath = record.getVideoPath();
        String imagePath = record.getImagePath();
        String eventType = record.getEventType();

        log.info("start extractFrame：alarm record tblId:{}", tblId);

        /**
         * 执行抽帧：
         * 1、转换视频路径编码
         * 2、检查抽帧图片存储路径是否存在
         * 3、根据告警类型采用对应形式抽帧
         *      3.1、停驶按照时间抽帧
         *      3.2、其他类型按照百分比抽帧
         */
        String encodedVideoUrl = encodeVideoUrl(videoPath);
        String imageSaveDir = buildImageSaveDir(record.getAlarmTime());
        FileUtil.fileExists(imageSaveDir);

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(encodedVideoUrl);
             Java2DFrameConverter converter = new Java2DFrameConverter()) {

            configureGrabber(grabber);
            grabber.start();

            List<FrameImageInfo> frameImageList = "停驶".equals(eventType)
                    ? extractByTimestamp(grabber, converter, record, imageSaveDir)
                    : extractByPercentage(grabber, converter, record, imageSaveDir);

            validateFrameCount(frameImageList.size(), tblId);
            frameImageServiceImpl.saveBatch(frameImageList);

            log.info("抽帧算法处理完成, 告警记录信息：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} |  抽帧图片数量:{} | 图片存储路径:{}", tblId, alarmId, imagePath, videoPath, frameImageList.size(), imageSaveDir);
            return true;
        } catch (Exception e) {
            log.info("抽帧算法处理异常, 告警记录信息：tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常信息:{}", tblId, alarmId, imagePath, videoPath, e.getMessage());
            return false;
        }
    }

    /**
     * @desc 视频连通性&&元数据校验
     * @param record
     * @return
     */
    public boolean checkVideoConnectivityWithRetry(OriginalAlarmRecord record){
        long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String encodedUrl = encodeVideoUrl(videoPath);

        for (int retry = 1; retry <= MAX_RETRY_TIMES; retry++) {
            FFmpegFrameGrabber grabber = null;
            try {
                // 步骤1：检查连接性
                if (!checkSingleConnectivity(encodedUrl)) {
                    log.warn("第{}次连接测试失败| tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", retry, tblId, alarmId, imagePath, videoPath);
                    if (retry < MAX_RETRY_TIMES) {
                        Thread.sleep(RETRY_INTERVAL_MS);
                        log.warn("等待{}ms后重试 | tblId:{} | alarmId:{} |imagePath:{} | videoPath:{}", retry, tblId, alarmId, imagePath, videoPath);
                    }
                    continue;
                }
                log.info("视频连接测试成功 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);

                // 步骤2：元数据校验（失败时也重试）
                grabber = new FFmpegFrameGrabber(encodedUrl);
                configureGrabber(grabber); // 假设configureGrabber是当前类的方法，用于配置grabber
                grabber.start();

                boolean metaValid = VideoCheckUtil.checkVideoMetaData(grabber, record, alarmId);
                if (metaValid) {
                    log.info("视频元数据校验成功 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                    return true;
                } else {
                    log.warn("视频元数据校验失败 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
                    return false;
                }
            } catch (InterruptedException e) {
                log.error("视频连通测试and元数据校验中断 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:{}", tblId, alarmId, imagePath, videoPath, e.getMessage());
                Thread.currentThread().interrupt(); // 恢复中断状态
                return false; // 中断后直接退出，不重试
            } catch (IOException e) {
                log.error("视频连通测试and元数据校验IO异常 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:{}", tblId, alarmId, imagePath, videoPath, e.getMessage());
                return false;
            } catch (Exception e) { // 捕获其他未知异常
                log.error("视频连通测试and元数据校验未知异常 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:{}", tblId, alarmId, imagePath, videoPath, e.getMessage());
                return false;
            } finally {
                // 确保资源释放
                if (grabber != null) {
                    try {
                        grabber.stop();
                        grabber.close();
                    } catch (Exception e) {
                        log.error("视频连接测试资源释放异常 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:{}", tblId, alarmId, imagePath, videoPath, e.getMessage());
                        return false;
                    }
                }
            }
        }
        log.error("有效等待时长and重试次数中，视频连通and元数据未测试成功 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{}", tblId, alarmId, imagePath, videoPath);
        return false;
    }

    /**
     * @desc 视频链接单次连通性校验
     * @param encodedUrl
     * @return
     * @throws IOException
     */
    private boolean checkSingleConnectivity(String encodedUrl) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(encodedUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int code = conn.getResponseCode();
            return code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_MOVED_TEMP;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * @desc 视频抽帧逻辑_根据时间戳分布抽帧
     * @param grabber
     * @param converter
     * @param record
     * @param saveDir
     * @return
     * @throws IOException
     */
    private List<FrameImageInfo> extractByTimestamp(FFmpegFrameGrabber grabber,
                                                    Java2DFrameConverter converter,
                                                    OriginalAlarmRecord record,
                                                    String saveDir) throws IOException {
        List<FrameImageInfo> list = new ArrayList<>();
        for (int i = 0; i < STOPPED_TIMESTAMPS.length; i++) {
            grabber.setTimestamp(STOPPED_TIMESTAMPS[i]);
            Frame frame = grabber.grabImage();
            if (frame != null) {
                String imageUrl = generateImageUrl(saveDir, record.getId(), i, record.getTblId());
                saveFrameToFile(converter, frame, imageUrl);
                list.add(buildFrameImageInfo(record, i, imageUrl));
            }
        }
        return list;
    }

    /**
     * @desc 根据百分比抽帧逻辑
     * @param grabber
     * @param converter
     * @param record
     * @param saveDir
     * @return
     * @throws IOException
     */
    private List<FrameImageInfo> extractByPercentage(FFmpegFrameGrabber grabber,
                                                     Java2DFrameConverter converter,
                                                     OriginalAlarmRecord record,
                                                     String saveDir) throws IOException {
        List<FrameImageInfo> list = new ArrayList<>();
        int totalFrames = grabber.getLengthInFrames();
        if (totalFrames <= 0) {
            throw new IllegalStateException("抽帧数量为0");
        }
        for (int i = 0; i < DEFAULT_PERCENTAGES.length; i++) {
            int framePos = (int) (totalFrames * DEFAULT_PERCENTAGES[i] / 100.0);
            grabber.setFrameNumber(framePos);
            Frame frame = grabber.grabImage();
            if (frame != null) {
                String imageUrl = generateImageUrl(saveDir, record.getId(), i, record.getTblId());
                saveFrameToFile(converter, frame, imageUrl);
                list.add(buildFrameImageInfo(record, i, imageUrl));
            }
        }
        return list;
    }

    /**
     * @desc 视频url编码转换
     * @param videoPath
     * @return
     */
    private String encodeVideoUrl(String videoPath) {
        String encodeUrl = videoPath.replace("{", "%7B").replace("}", "%7D");
        log.info("抽帧视频路径编码：原路径:{}, 编码后路径:{}", videoPath, encodeUrl);
        return encodeUrl;
    }

    /**
     * @desc 抽帧图片路径逻辑
     * @param time
     * @return
     */
    private String buildImageSaveDir(LocalDateTime time) {
        return imageBaseDir + File.separator + time.format(DATE_FORMATTER);
    }

    /**
     * @desc grabber配置
     * @param grabber
     */
    private void configureGrabber(FFmpegFrameGrabber grabber) {
        grabber.setOption("timeout", "5000000");
        grabber.setOption("reconnect", "1");
        grabber.setOption("reconnect_streamed", "1");
        grabber.setOption("fflags", "nobuffer");
    }

    /**
     * @desc 抽帧文件存储
     * @param converter
     * @param frame
     * @param imageUrl
     * @throws IOException
     */
    private void saveFrameToFile(Java2DFrameConverter converter, Frame frame, String imageUrl) throws IOException {
        File file = new File(imageUrl);
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        ImageIO.write(converter.getBufferedImage(frame), IMAGE_FORMAT, file);
    }

    /**
     * @desc 图片文件名称生成
     * @param saveDir
     * @param alarmId
     * @param imageNo
     * @param tblId
     * @return
     */
    private String generateImageUrl(String saveDir, String alarmId, int imageNo, Long tblId) {
        return saveDir + File.separator + String.format("frame_%s_%s_%d_%d.%s",
                LocalDateTime.now().format(DATE_FORMATTER), alarmId, imageNo, tblId, IMAGE_FORMAT);
    }

    /**
     * @desc 创建抽帧图片对象信息
     * @param record
     * @param imageNo
     * @param imageUrl
     * @return
     */
    private FrameImageInfo buildFrameImageInfo(OriginalAlarmRecord record, int imageNo, String imageUrl) {
        FrameImageInfo info = new FrameImageInfo();
        info.setTblId(record.getTblId());
        info.setAlarmId(record.getId());
        info.setImagePath(record.getImagePath());
        info.setVideoPath(record.getVideoPath());
        info.setFrameNum(targetFrameCount);
        info.setImageSortNo(imageNo);
        info.setImageUrl(imageUrl);
        info.setCreateTime(LocalDateTime.now());
        info.setUpdateTime(LocalDateTime.now());
        return info;
    }

    /**
     * @desc 验证抽帧图片数量
     * @param count
     * @param tblId
     */
    private void validateFrameCount(int count, long tblId) {
        if (count != targetFrameCount) {
            throw new IllegalStateException(String.format(
                    "抽帧数量异常, tblId:{}, 目标抽帧数量:{}, 实际抽帧数量:{}", tblId, count, targetFrameCount));
        }
    }
}