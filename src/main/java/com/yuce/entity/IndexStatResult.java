package com.yuce.entity;

/**
 * @ClassName IndexStatResult
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/11/5 16:29
 * @Version 1.0
 */

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 指标统计结果实体类
 */
@Data
public class IndexStatResult {
    /** 统计日期 */
    private LocalDate statDate;
    /** 分子（符合条件的告警数） */
    private Long numerator;
    /** 分母（总告警数） */
    private Long denominator;
    /** 计算结果（numerator/denominator） */
    private BigDecimal result;
}