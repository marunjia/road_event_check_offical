package com.yuce.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * @ClassName IouUtil
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/6/18 13:44
 * @Version 1.0
 */

@Component
@Slf4j
public class IouUtil {

    public static double calculateIoU(int x1A, int y1A, int x2A, int y2A,
                                          int x1B, int y1B, int x2B, int y2B) {
        // 交集的左上角
        int xLeft = Math.max(x1A, x1B);
        int yTop = Math.max(y1A, y1B);

        // 交集的右下角
        int xRight = Math.min(x2A, x2B);
        int yBottom = Math.min(y2A, y2B);

        // 无交集
        if (xRight < xLeft || yBottom < yTop) {
            return 0.0;
        }

        // 交集面积
        int intersectionArea = (xRight - xLeft) * (yBottom - yTop);

        // 各自面积
        int areaA = (x2A - x1A) * (y2A - y1A);
        int areaB = (x2B - x1B) * (y2B - y1B);

        // 并集面积
        int unionArea = areaA + areaB - intersectionArea;

        // IoU = 交集 / 并集
        return (double) intersectionArea / unionArea;
    }
}