package com.yuce.entity;

/**
 * @ClassName IndexStatResult
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/11/5 16:29
 * @Version 1.0
 */
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 指标统计结果实体类
 */
@Data
@TableName("index_stat_result")
public class IndexStatResult {

    @TableId(type = IdType.AUTO)
    private Integer id; // 主键ID

    /** 统计日期 */
    private LocalDate statDate;

    /**指标名称*/
    private String indexName;

    /**指标类型*/
    private Integer indexType;

    /** 分子（符合条件的告警数） */
    private Long numerator;

    /** 分母（总告警数） */
    private Long denominator;

    /** 计算结果（numerator/denominator） */
    private BigDecimal result;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime modifyTime;
}