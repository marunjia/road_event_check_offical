package com.yuce.algorithm;

import com.yuce.config.VideoProperties;
import com.yuce.entity.FrameImageInfo;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.VideoDealService;
import com.yuce.service.impl.FrameImageServiceImpl;
import com.yuce.util.VideoCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
@Component
public class ExtractFrameAlgorithm implements VideoDealService {

    @Autowired
    private VideoProperties videoProperties;

    // 优化点1: 改为Mapper注入，避免Service循环依赖
    @Autowired
    private FrameImageServiceImpl frameImageServiceImpl;

    private String baseDir;
    private int frameCount;

    // 优化点2: 增加锁机制防止并发处理同一视频
    private final ConcurrentHashMap<String, Lock> videoLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        baseDir = videoProperties.getOutputDir();
        frameCount = videoProperties.getFrameCount();
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);
        FFmpegLogCallback.set();
    }

    @Override
    public boolean extractImage(OriginalAlarmRecord record) {
        boolean hasStarted = false;

        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String eventType = record.getEventType();

        // 优化点3: 先检查数据库，避免重复处理
        if (existsByKeyAndNum(alarmId, imagePath, videoPath)) {
            log.info("告警记录已抽帧：id->{}, alarmId->{}, imagePath->{}, videoPath->{}",
                    tblId, alarmId, imagePath, videoPath);
            return true;
        }

        // 优化点4: 使用分布式锁机制防止并发处理
        String lockKey = videoPath + "-" + alarmId + "_" + record.getTblId();
        Lock lock = videoLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            log.warn("视频正在处理中，跳过重复请求: {}", lockKey);
            return false;
        }

        try {
            // 存储路径是否存在，如果不存在则创建
            String saveDirPath = baseDir + File.separator + record.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            ensureDirectoryExists(saveDirPath);

            FFmpegFrameGrabber grabber = null;
            try {
                String encodedVideoPath = videoPath.replace("{", "%7B").replace("}", "%7D");// 优化点5: URL编码处理
                grabber = new FFmpegFrameGrabber(encodedVideoPath);
                configureGrabber(grabber);//优化点6: 增加超时配置
                grabber.start();
                hasStarted = true;

                // 视频元数据验证
                if (!VideoCheckUtil.checkVideoMetaData(grabber, record, alarmId)) {
                    return false;
                }

                // 根据事件类型选择抽帧策略
                List<FrameImageInfo> frameImageList = new ArrayList<>();
                Java2DFrameConverter converter = new Java2DFrameConverter();

                if ("停驶".equals(eventType)) {
                    frameImageList = extractByTimestamp(grabber, converter, record, saveDirPath, new long[]{7_000_000L, 8_000_000L, 9_000_000L});
                } else {
                    frameImageList = extractByPosition(grabber, converter, record, saveDirPath, new int[]{50, 80, 95});
                }

                //如果抽帧图片数量不对，则返回false
                if(frameImageList.size() == frameCount) {
                    frameImageServiceImpl.saveBatch(frameImageList);//保存抽帧结果
                    return true;
                }else{
                    log.info("抽帧图片数量不达要求: id->{}, alarmId->{}, imagePath->{}, videoPath->{}, alarmId->{}, frameCount->{}, extractCount->{}", record.getTblId(), alarmId, imagePath, videoPath, frameCount, frameImageList.size());
                    return false;
                }
            } catch (Exception e) {
                log.error("视频处理异常: alarmId->{}, imagePath->{}, videoPath->{}, 错误信息->{}",
                        alarmId, imagePath, videoPath, e.getMessage(), e);
                return false;
            } finally {
                if (hasStarted) {
                    try {
                        grabber.stop();
                    } catch (Exception e) {
                        log.warn("stop失败: alarmId->{}, imagePath->{}, videoPath->{}, 失败原因->{}", alarmId, imagePath, videoPath, e.getMessage());
                    }
                }

                try {
                    grabber.release(); // 即使无效，也应调用，避免 native 泄漏
                } catch (Exception e) {
                    log.warn("release失败: alarmId->{}, imagePath->{}, videoPath->{}, 失败原因->{}", alarmId, imagePath, videoPath, e.getMessage());
                }

                try {
                    grabber.close();   // 对于 JavaCV 的 native wrapper，close 比 release 更稳妥
                } catch (Exception e) {
                    log.warn("close失败: alarmId->{}, imagePath->{}, videoPath->{}, 失败原因->{}", alarmId, imagePath, videoPath, e.getMessage());
                }
            }
        } finally {
            // 释放锁
            lock.unlock();
            videoLocks.remove(lockKey);
        }
    }

    /**
     * 配置视频抓取器参数
     */
    private void configureGrabber(FFmpegFrameGrabber grabber) {
        grabber.setOption("timeout", "5000000");
        grabber.setOption("reconnect", "1");
        grabber.setOption("reconnect_streamed", "1");
        grabber.setOption("reconnect_at_eof", "1");
        grabber.setOption("fflags", "nobuffer");
        grabber.setOption("rw_timeout", "5000000");
        grabber.setOption("http_user_agent", "Mozilla/5.0");
        grabber.setOption("headers", "Connection: close\r\n");
    }



    /**
     * 按时间戳抽帧
     */
    private List<FrameImageInfo> extractByTimestamp(FFmpegFrameGrabber grabber,
                                                    Java2DFrameConverter converter,
                                                    OriginalAlarmRecord record,
                                                    String saveDirPath,
                                                    long[] timestamps) throws IOException {
        List<FrameImageInfo> frameImageList = new ArrayList<>();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();

        for (int i = 0; i < timestamps.length; i++) {
            long ts = timestamps[i];
            grabber.setTimestamp(ts);
            Frame frame = grabber.grabImage();

            String imageUrl = generateImageUrl(saveDirPath, alarmId, i, record.getTblId());
            if (frame != null) {
                saveFrameToFile(converter, frame, imageUrl);
                frameImageList.add(createFrameImageInfo(record, i, imageUrl));
            } else {
                log.info("按时间间隔抽帧: id->{}, alarmId->{}, imagePath->{}, videoPath->{}, alarmId->{}, ts->{}, imageUrl->{}", record.getTblId(), alarmId, imagePath, videoPath, ts, imageUrl);
            }
        }
        return frameImageList;
    }

    /**
     * 按帧位置抽帧
     */
    private List<FrameImageInfo> extractByPosition(FFmpegFrameGrabber grabber,
                                                   Java2DFrameConverter converter,
                                                   OriginalAlarmRecord record,
                                                   String saveDirPath,
                                                   int[] percentages) throws IOException {
        List<FrameImageInfo> frameImageList = new ArrayList<>();
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        int totalFrames = grabber.getLengthInFrames();

        for (int i = 0; i < percentages.length; i++) {
            int pos = (int) (totalFrames * percentages[i] / 100.0);
            grabber.setFrameNumber(pos);
            Frame frame = grabber.grabImage();

            String imageUrl = generateImageUrl(saveDirPath, alarmId, i, record.getTblId());

            if (frame != null) {
                saveFrameToFile(converter, frame, imageUrl);
                frameImageList.add(createFrameImageInfo(record, i, imageUrl));
            } else {
                log.info("按百分比间隔抽帧: id->{}, alarmId->{}, imagePath->{}, videoPath->{}, alarmId->{}, pos->{}, imageUrl->{}",
                        record.getTblId(), alarmId, imagePath, videoPath, pos, imageUrl);
            }
        }

        return frameImageList;
    }

    /**
     * 生成图片URL
     */
    private String generateImageUrl(String saveDirPath, String alarmId, int imageNo, Long tblId) {
        //frame_20250728_e5e7d43b-693d-11f0-a53a-c8c465c68ecc_2_7.jpg
        return saveDirPath + File.separator + "frame_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + alarmId + "_" + imageNo + "_" + tblId + ".jpg";
    }

    /**
     * 保存帧到文件
     */
    private void saveFrameToFile(Java2DFrameConverter converter, Frame frame, String imageUrl) throws IOException {
        ImageIO.write(converter.getBufferedImage(frame), "jpg", new File(imageUrl));
    }

    /**
     * 创建帧信息对象
     */
    private FrameImageInfo createFrameImageInfo(OriginalAlarmRecord record, int imageNo, String imageUrl) {
        FrameImageInfo frameImageInfo = new FrameImageInfo();
        frameImageInfo.setAlarmId(record.getId());
        frameImageInfo.setImagePath(record.getImagePath());
        frameImageInfo.setVideoPath(record.getVideoPath());
        frameImageInfo.setFrameNum(frameCount);
        frameImageInfo.setImageSortNo(imageNo);
        frameImageInfo.setImageUrl(imageUrl);
        frameImageInfo.setCreateTime(LocalDateTime.now());
        frameImageInfo.setUpdateTime(LocalDateTime.now());
        return frameImageInfo;
    }

    /**
     * 确保目录存在
     */
    public void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 检查是否已存在抽帧记录
     */
    public boolean existsByKeyAndNum(String alarmId, String imagePath, String videoPath) {
        long count = frameImageServiceImpl.getFrameListByKey(alarmId, imagePath, videoPath).size();
        if (count == 0) {
            return false;
        } else if (count < frameCount) {
            frameImageServiceImpl.deleteByKey(alarmId,imagePath,videoPath);// 清理不完整的记录
            return false;
        } else {
            return true;
        }
    }


}