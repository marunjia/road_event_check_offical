package com.yuce.algorithm;

import com.yuce.config.VideoProperties;
import com.yuce.entity.FrameImageInfo;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.service.impl.FrameImageServiceImpl;
import com.yuce.util.FileUtil;
import com.yuce.util.VideoCheckUtil;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
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
 * 视频抽帧算法服务：负责视频连通性校验、按规则抽帧、图片存储及抽帧信息持久化
 */
@Service
@Slf4j
// 移除冗余@Component：@Service已包含组件扫描，避免重复注册
public class ExtractFrameAlgorithm {

    // ------------------------------ 常量定义（统一维护，避免硬编码） ------------------------------
    private static final int MAX_RETRY_TIMES = 3;        // 视频连接最大重试次数
    private static final long RETRY_INTERVAL_MS = 10_000;// 重试间隔（10秒）
    private static final String IMAGE_FORMAT = "jpg";    // 抽帧图片格式
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");// 日期格式化
    private static final long[] STOPPED_TIMESTAMPS = {7_000_000L, 8_000_000L, 9_000_000L};// 停驶告警抽帧时间戳（微秒）
    private static final int[] DEFAULT_PERCENTAGES = {50, 80, 95};// 非停驶告警抽帧百分比


    // ------------------------------ 成员变量 ------------------------------
    private int targetFrameCount;  // 目标抽帧数量（从配置读取）
    private String videoBaseDir;   // 视频基础路径（配置读取）
    private String imageBaseDir;   // 图片存储根路径（配置读取）

    @Autowired
    private VideoProperties videoProperties;

    @Autowired
    private FrameImageServiceImpl frameImageService; // 简化变量名，Impl后缀可省略


    // ------------------------------ 初始化方法 ------------------------------
    @PostConstruct
    public void init() {
        // 读取配置并校验合法性
        this.targetFrameCount = videoProperties.getFrameCount();
        this.videoBaseDir = videoProperties.getVideoBaseDir();
        this.imageBaseDir = videoProperties.getImageBaseDir();

        // 配置校验：避免空配置导致后续异常
        Assert.isTrue(targetFrameCount > 0, "抽帧目标数量(frameCount)必须大于0");
        Assert.hasText(videoBaseDir, "视频文件存储路径(videoBaseDir)不能为空");
        Assert.hasText(imageBaseDir, "图片文件存储路径(imageBaseDir)不能为空");

        // 初始化FFmpeg日志（静默模式，减少冗余日志）
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);
        FFmpegLogCallback.set();
        log.info("抽帧服务初始化完成 | 目标抽帧数:{} | 图片根路径:{} | 视频根路径:{}", targetFrameCount, imageBaseDir, videoBaseDir);
    }


    // ------------------------------ 核心业务方法 ------------------------------
    /**
     * 核心抽帧入口：包含视频连通性校验、抽帧执行、结果校验、信息持久化
     * @param record 原始告警记录（需含tblId、alarmId、videoPath、alarmTime等核心字段）
     * @throws Exception 抽帧异常（连接失败、抽帧数量不足、IO异常等）
     */
    public void extractFrame(OriginalAlarmRecord record) throws Exception {
        // 1. 入参校验：提前阻断非法数据
        validateAlarmRecord(record);
        Long tblId = record.getTblId();
        String alarmId = record.getId();
        String videoPath = record.getVideoPath();
        String imagePath = record.getImagePath();
        String eventType = record.getEventType();
        log.info("开始抽帧 | tblId:{} | alarmId:{} | 图片路径:{} | 视频路径:{}", tblId, alarmId, imagePath, videoPath);

        // 2. 视频路径编码：处理特殊字符（如{}），避免URL解析失败
        String encodedVideoUrl = encodeVideoUrl(videoPath);
        log.debug("视频路径编码 | 原始:{} → 编码后:{}", videoPath, encodedVideoUrl);

        // 3. 视频连通性校验（带重试）
        if (!checkVideoConnectivityWithRetry(encodedVideoUrl, alarmId)) {
            throw new IOException(String.format("视频连通性校验失败（重试%d次）| alarmId:%s | imagePath:%s | videoPath:%s", MAX_RETRY_TIMES, alarmId, imagePath, videoPath));
        }

        // 4. 构建图片存储目录（按告警时间分目录，避免文件堆积）
        String imageSaveDir = buildImageSaveDir(record);
        FileUtil.fileExists(imageSaveDir); // 确保目录存在（不存在则创建）
        log.debug("图片存储目录 | {}", imageSaveDir);

        // 5. 抽帧执行：try-with-resources自动关闭资源，避免泄漏
        List<FrameImageInfo> frameImageList;
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(encodedVideoUrl);
             Java2DFrameConverter converter = new Java2DFrameConverter()) {

            // 配置grabber参数
            configureGrabber(grabber);
            grabber.start();

            // 6. 视频元数据校验（过滤非法视频）
            if (!VideoCheckUtil.checkVideoMetaData(grabber, record, alarmId)) {
                throw new IllegalStateException(String.format("视频元数据校验失败| alarmId:%s | imagePath:%s | videoPath:%s", alarmId, imagePath, videoPath));
            }

            // 7. 按告警类型选择抽帧策略
            frameImageList = "停驶".equals(eventType)
                    ? extractByTimestamp(grabber, converter, record, imageSaveDir)
                    : extractByPercentage(grabber, converter, record, imageSaveDir);

            // 8. 抽帧数量校验
            validateFrameCount(frameImageList.size(), alarmId, tblId);

            // 9. 抽帧信息持久化
            frameImageService.saveBatch(frameImageList);
            log.info("抽帧完成 | alarmId:{} | imagePath:{} | videoPath:{} | 成功抽帧:{}张 | 存储目录:{}", alarmId, imagePath, videoPath, frameImageList.size(), imageSaveDir);

        } catch (Exception e) {
            log.error("抽帧异常 | tblId:{} | alarmId:{} | imagePath:{} | videoPath:{} | 异常详情:", tblId, alarmId, imagePath, videoPath, e);
            throw e; // 抛出异常由上层处理（如标记告警状态）
        }
    }


    // ------------------------------ 私有工具方法 ------------------------------
    /**
     * 校验原始告警记录核心字段
     */
    private void validateAlarmRecord(OriginalAlarmRecord record) {
        Assert.notNull(record, "原始告警记录不能为空");
        Assert.notNull(record.getTblId(), "tblId不能为空");
        Assert.hasText(record.getId(), "alarmId不能为空");
        Assert.hasText(record.getVideoPath(), "videoPath不能为空");
        Assert.notNull(record.getAlarmTime(), "alarmTime不能为空");
        Assert.hasText(record.getEventType(), "eventType不能为空");
    }

    /**
     * 视频路径编码：处理URL中的特殊字符
     */
    private String encodeVideoUrl(String videoPath) {
        if (!StringUtils.hasText(videoPath)) {
            return "";
        }
        // 替换{}为URL编码（{}在URL中需转义为%7B%7D）
        return videoPath.replace("{", "%7B").replace("}", "%7D");
    }

    /**
     * 视频连通性校验（带重试）
     */
    private boolean checkVideoConnectivityWithRetry(String encodedUrl, String alarmId) throws Exception {
        for (int retry = 1; retry <= MAX_RETRY_TIMES; retry++) {
            try {
                if (checkSingleConnectivity(encodedUrl)) {
                    log.info("视频连通性校验成功 | 重试次数:{} | alarmId:{} | 路径:{}",
                            retry, alarmId, encodedUrl);
                    return true;
                }
            } catch (IOException e) {
                log.warn("第{}次连通性校验失败 | alarmId:{} | 原因:{}",
                        retry, alarmId, e.getMessage());
            }

            // 非最后一次重试：等待后继续
            if (retry < MAX_RETRY_TIMES) {
                Thread.sleep(RETRY_INTERVAL_MS);
                log.debug("等待{}ms后进行第{}次重试 | alarmId:{}",
                        RETRY_INTERVAL_MS, retry + 1, alarmId);
            }
        }
        return false;
    }

    /**
     * 单次视频连通性校验（发送HEAD请求，避免下载完整视频）
     * 兼容Java 8及以下版本，手动管理连接资源
     */
    private boolean checkSingleConnectivity(String encodedUrl) throws IOException {
        HttpURLConnection conn = null; // 声明连接对象，在finally中关闭
        try {
            URL url = new URL(encodedUrl);
            conn = (HttpURLConnection) url.openConnection();

            // 配置请求参数
            conn.setRequestMethod("HEAD"); // 仅请求头部，不下载内容
            conn.setConnectTimeout(5000);  // 连接超时：5秒
            conn.setReadTimeout(5000);     // 读取超时：5秒

            // 校验响应码（200=成功，302=临时重定向）
            int responseCode = conn.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_MOVED_TEMP;
        } finally {
            // 手动关闭连接，确保资源释放（无论是否发生异常）
            if (conn != null) {
                conn.disconnect(); // 关闭TCP连接，避免资源泄漏
            }
        }
    }

    /**
     * 构建图片存储目录（格式：imageBaseDir/yyyyMMdd）
     */
    private String buildImageSaveDir(OriginalAlarmRecord record) {
        String dateStr = record.getAlarmTime().format(DATE_FORMATTER);
        return imageBaseDir + File.separator + dateStr;
    }

    /**
     * 配置FFmpegFrameGrabber参数（超时、重连、缓存策略）
     */
    private void configureGrabber(FFmpegFrameGrabber grabber) {
        grabber.setOption("timeout", "5000000");       // 超时5秒（微秒）
        grabber.setOption("reconnect", "1");           // 启用重连
        grabber.setOption("reconnect_streamed", "1");  // 流数据重连
        grabber.setOption("reconnect_at_eof", "1");    // EOF时重连
        grabber.setOption("fflags", "nobuffer");       // 禁用缓存
        grabber.setOption("rw_timeout", "5000000");    // 读写超时5秒
        grabber.setOption("http_user_agent", "Mozilla/5.0"); // 模拟浏览器UA
        grabber.setOption("headers", "Connection: close\r\n"); // 关闭长连接
    }

    /**
     * 按时间戳抽帧（针对停驶告警）
     */
    private List<FrameImageInfo> extractByTimestamp(FFmpegFrameGrabber grabber,
                                                    Java2DFrameConverter converter,
                                                    OriginalAlarmRecord record,
                                                    String saveDir) throws IOException {
        List<FrameImageInfo> list = new ArrayList<>(STOPPED_TIMESTAMPS.length);
        String alarmId = record.getId();
        Long tblId = record.getTblId();

        for (int i = 0; i < STOPPED_TIMESTAMPS.length; i++) {
            long ts = STOPPED_TIMESTAMPS[i];
            grabber.setTimestamp(ts);
            Frame frame = grabber.grabImage();

            String imageUrl = generateImageUrl(saveDir, alarmId, i, tblId);
            if (frame != null) {
                saveFrameToFile(converter, frame, imageUrl);
                list.add(buildFrameImageInfo(record, i, imageUrl));
                log.debug("时间戳抽帧成功 | 序号:{} | 时间戳:{}微秒 | 路径:{}", i, ts, imageUrl);
            } else {
                log.warn("时间戳抽帧失败 | alarmId:{} | 序号:{} | 时间戳:{}微秒", alarmId, i, ts);
            }
        }
        return list;
    }

    /**
     * 按百分比抽帧（针对非停驶告警）
     */
    private List<FrameImageInfo> extractByPercentage(FFmpegFrameGrabber grabber,
                                                     Java2DFrameConverter converter,
                                                     OriginalAlarmRecord record,
                                                     String saveDir) throws IOException {
        List<FrameImageInfo> list = new ArrayList<>(DEFAULT_PERCENTAGES.length);
        String alarmId = record.getId();
        Long tblId = record.getTblId();
        int totalFrames = grabber.getLengthInFrames();

        // 校验总帧数合法性
        if (totalFrames <= 0) {
            throw new IllegalStateException(String.format("视频总帧数非法 | alarmId:%s | 总帧数:%d",
                    alarmId, totalFrames));
        }

        for (int i = 0; i < DEFAULT_PERCENTAGES.length; i++) {
            int percent = DEFAULT_PERCENTAGES[i];
            int framePos = (int) (totalFrames * percent / 100.0); // 计算抽帧位置
            grabber.setFrameNumber(framePos);
            Frame frame = grabber.grabImage();

            String imageUrl = generateImageUrl(saveDir, alarmId, i, tblId);
            if (frame != null) {
                saveFrameToFile(converter, frame, imageUrl);
                list.add(buildFrameImageInfo(record, i, imageUrl));
                log.debug("百分比抽帧成功 | 序号:{} | 百分比:{}% | 位置:{}帧 | 路径:{}",
                        i, percent, framePos, imageUrl);
            } else {
                log.warn("百分比抽帧失败 | alarmId:{} | 序号:{} | 百分比:{}%",
                        alarmId, i, percent);
            }
        }
        return list;
    }

    /**
     * 生成图片URL（格式：saveDir/frame_yyyyMMdd_alarmId_imageNo_tblId.jpg）
     */
    private String generateImageUrl(String saveDir, String alarmId, int imageNo, Long tblId) {
        String fileName = String.format("frame_%s_%s_%d_%d.%s",
                LocalDateTime.now().format(DATE_FORMATTER), // 当前日期（避免同一天重名）
                alarmId,
                imageNo,
                tblId,
                IMAGE_FORMAT);
        return saveDir + File.separator + fileName;
    }

    /**
     * 保存帧为图片文件
     */
    private void saveFrameToFile(Java2DFrameConverter converter, Frame frame, String imageUrl) throws IOException {
        File imageFile = new File(imageUrl);
        // 确保父目录存在（防止目录创建延迟导致的文件创建失败）
        if (!imageFile.getParentFile().exists()) {
            boolean mkdirs = imageFile.getParentFile().mkdirs();
            if (!mkdirs) {
                throw new IOException("创建图片父目录失败 | 路径:" + imageFile.getParent());
            }
        }
        // 写入图片
        ImageIO.write(converter.getBufferedImage(frame), IMAGE_FORMAT, imageFile);
    }

    /**
     * 构建FrameImageInfo对象（抽帧信息持久化用）
     */
    private FrameImageInfo buildFrameImageInfo(OriginalAlarmRecord record, int imageNo, String imageUrl) {
        FrameImageInfo info = new FrameImageInfo();
        info.setAlarmId(record.getId());
        info.setImagePath(record.getImagePath());
        info.setVideoPath(record.getVideoPath());
        info.setFrameNum(targetFrameCount); // 目标抽帧数量
        info.setImageSortNo(imageNo);       // 图片序号
        info.setImageUrl(imageUrl);         // 图片存储路径
        info.setCreateTime(LocalDateTime.now());
        info.setUpdateTime(LocalDateTime.now());
        return info;
    }

    /**
     * 抽帧数量校验：确保符合目标数量
     */
    private void validateFrameCount(int actualCount, String alarmId, Long tblId) {
        if (actualCount != targetFrameCount) {
            throw new IllegalStateException(String.format("抽帧数量不足 | alarmId:%s | tblId:%d | 实际:%d | 目标:%d",
                    alarmId, tblId, actualCount, targetFrameCount));
        }
    }
}