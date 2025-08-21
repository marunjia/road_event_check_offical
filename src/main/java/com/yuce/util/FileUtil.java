package com.yuce.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.File;

/**
 * @ClassName FileUtil
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/8/11 17:19
 * @Version 1.0
 */

@Component
@Slf4j
public class FileUtil {

    public static void fileExists(String filePath){
        // 判断目录是否存在，不存在则创建
        File dir = new File(filePath);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("目录文件创建成功：" + dir.getAbsolutePath());
            } else {
                log.info("目录文件创建失败：" + dir.getAbsolutePath());
            }
        }
    }

    /**
     * @desc 删除文件
     * @param filePath
     */
    public void deleteFile(String filePath){
        File videoFile = new File(filePath);
        if (videoFile.exists()) {
            boolean deleted = videoFile.delete();
            if (deleted) {
                log.info("成功删除下载文件: {}", filePath);
            } else {
                log.warn("删除下载文件失败: {}", filePath);
            }
        }
    }
}