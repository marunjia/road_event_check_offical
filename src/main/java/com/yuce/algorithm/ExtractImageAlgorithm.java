package com.yuce.algorithm;

import com.yuce.config.VideoProperties;
import com.yuce.entity.CheckAlarmProcess;
import com.yuce.entity.ExtractImageRecord;
import com.yuce.entity.ExtractWindowRecord;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.mapper.CheckAlarmProcessMapper;
import com.yuce.mapper.ExtractImageMapper;
import com.yuce.mapper.ExtractWindowMapper;
import com.yuce.service.impl.ExtractImageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.format.DateTimeFormatter;

/**
 * @ClassName ExtractImageServiceImpl
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/7/21 19:03
 * @Version 1.0
 */

@Component
@Slf4j
public class ExtractImageAlgorithm {

    private String baseDir;

    @Autowired
    private CheckAlarmProcessMapper checkAlarmProcessMapper;

    @Autowired
    private ExtractWindowMapper extractWindowMapper;

    @Autowired
    private ExtractImageServiceImpl extractImageServiceImpl;

    @Autowired
    private VideoProperties videoProperties;

    @PostConstruct
    public void init() {
        baseDir = videoProperties.getOutputDir();
    }

    public void extractImage(OriginalAlarmRecord record) {
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        String date = record.getAlarmTime().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 获取 IOU top1 图片标识
        CheckAlarmProcess checkAlarmProcess = checkAlarmProcessMapper.getIouTop1ByKey(alarmId, imagePath, videoPath);
        String imageId = checkAlarmProcess.getImageId();

        // 拼接图片的 URL 路径（这里假设 imageId 可以定位到图片）
        String imageUrl = baseDir + File.separator + date + File.separator + "frame_" + imageId + ".jpg";

        // 获取抠图窗口坐标
        ExtractWindowRecord extractWindowRecord = extractWindowMapper.getWindowByKey(alarmId, imagePath, videoPath);
        int point1X = extractWindowRecord.getPoint1X();
        int point1Y = extractWindowRecord.getPoint1Y();
        int point2X = extractWindowRecord.getPoint2X();
        int point2Y = extractWindowRecord.getPoint2Y();

        try {
            // 读取本地图片（如果 imageUrl是网络地址，用 URL）
            File inputFile = new File(imageUrl);
            if (!inputFile.exists()) {
                log.warn("原始图片不存在: {}", imageUrl);
                return;
            }

            BufferedImage originalImage = ImageIO.read(inputFile);

            // 检查坐标合法性
            if (point1X < 0 || point1Y < 0 || point2X > originalImage.getWidth() || point2Y > originalImage.getHeight()) {
                log.warn("坐标超出图片范围，跳过处理: {}, 坐标({},{})-({},{})", imageUrl, point1X, point1Y, point2X, point2Y);
                return;
            }

            // 裁剪区域尺寸
            int width = point2X - point1X;
            int height = point2Y - point1Y;

            BufferedImage croppedImage = originalImage.getSubimage(point1X, point1Y, width, height);// 抠图
            File saveDirFile = new File(baseDir + File.separator + date);//保存路径
            if (!saveDirFile.exists()) saveDirFile.mkdirs();

            String croppedPath = baseDir + File.separator + date + File.separator + "cropped_" + imageId + ".jpg";
            ImageIO.write(croppedImage, "jpg", new File(croppedPath));
            log.info("抠图成功: {}, 存储路径: {}", imageUrl, croppedPath);

            ExtractImageRecord extractImageRecord = new ExtractImageRecord();
            extractImageRecord.setAlarmId(alarmId);
            extractImageRecord.setImagePath(imagePath);
            extractImageRecord.setVideoPath(videoPath);
            extractImageRecord.setImageId(imageId);
            extractImageRecord.setImageUrl(imageUrl);
            extractImageRecord.setCroppedImageUrl(croppedPath);
            extractImageRecord.setPoint1X(point1X);
            extractImageRecord.setPoint1Y(point1Y);
            extractImageRecord.setPoint2X(point2X);
            extractImageRecord.setPoint2Y(point2Y);
            extractImageServiceImpl.insertImage(extractImageRecord);
        } catch (Exception e) {
            log.error("抠图失败: alarmId->{}, imagePath->{}, videoPath->{}, checkAlarmProcessId->{}, 坐标({},{})-({},{})，错误信息: {}", alarmId, imagePath, videoPath , checkAlarmProcess.getId(), point1X, point1Y, point2X, point2Y, e.getMessage());
        }
    }
}