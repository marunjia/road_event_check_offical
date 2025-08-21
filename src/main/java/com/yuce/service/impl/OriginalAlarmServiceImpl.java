package com.yuce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yuce.entity.AlarmTimeRange;
import com.yuce.entity.OriginalAlarmRecord;
import com.yuce.entity.QueryResultCheckRecord;
import com.yuce.mapper.OriginalAlarmMapper;
import com.yuce.service.OriginalAlarmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/**
 * @ClassName OriginalAlarmServiceImpl
 * @Description 原始告警事件业务操作实现类
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/5/14 19:01
 * @Version 1.0
 */
@Service
@Slf4j
public class OriginalAlarmServiceImpl extends ServiceImpl<OriginalAlarmMapper, OriginalAlarmRecord> implements OriginalAlarmService {

    @Autowired
    private OriginalAlarmMapper originalAlarmMapper;

    // 常量抽取：避免硬编码，统一维护
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DEAL_FLAG_EVENT = 1; // 1：被确定为事件
    private static final String TIME_PARSE_ERROR = "日期格式错误，需符合 yyyy-MM-dd 格式";

    /**
     * @param tblId
     * @return
     * @desc 根据告警id、图片路径、视频路径组成的联合主键判断记录是否存在
     */
    public OriginalAlarmRecord getRecordByTblId(long tblId) {
        Assert.isTrue(tblId > 0, "tblId 必须为正整数");// 校验主键合法性：tblId 为数据库自增ID，不能小于0
        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();// 使用 LambdaQueryWrapper：类型安全，避免字段名硬编码
        wrapper.eq(OriginalAlarmRecord::getTblId, tblId);
        wrapper.last("LIMIT 1");// 明确查询1条：防止因数据异常返回多条（MyBatis-Plus getOne 默认抛异常，此处显式限制条数更安全）
        return this.getOne(wrapper);
    }

    /**
     * 根据 告警ID+图片路径+视频路径 联合主键查询记录
     * @param alarmId 告警ID（非空）
     * @param imagePath 图片路径（非空）
     * @param videoPath 视频路径（非空）
     * @return 匹配的告警记录，无匹配时返回 null
     */
    public OriginalAlarmRecord getRecordByKey(String alarmId, String imagePath, String videoPath) {
        // 空值校验：联合主键字段不可为空，避免无效查询或 SQL 语法错误
        validateUnionKey(alarmId, imagePath, videoPath);
        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OriginalAlarmRecord::getId, alarmId) // 假设 entity 中 alarmId 对应字段是 getId()，需与实际字段匹配
                .eq(OriginalAlarmRecord::getImagePath, imagePath)
                .eq(OriginalAlarmRecord::getVideoPath, videoPath)
                .last("LIMIT 1"); // 强制查询1条，避免数据重复导致的歧义
        return this.getOne(wrapper);
    }

    /**
     * 插入或更新记录（存在则更新，不存在则插入）
     * @param record 告警记录（非空，且需包含联合主键字段）
     */
    @Override
    public void saveIfNotExists(OriginalAlarmRecord record) {
        // 1. 入参非空校验：避免空指针
        Assert.notNull(record, "待保存的告警记录不能为空");
        // 2. 联合主键字段校验：确保关键字段有值
        String alarmId = record.getId();
        String imagePath = record.getImagePath();
        String videoPath = record.getVideoPath();
        validateUnionKey(alarmId, imagePath, videoPath);

        // 3. 查询已有记录
        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OriginalAlarmRecord::getId, alarmId)
                .eq(OriginalAlarmRecord::getImagePath, imagePath)
                .eq(OriginalAlarmRecord::getVideoPath, videoPath)
                .last("LIMIT 1");
        OriginalAlarmRecord existingRecord = this.getOne(wrapper);

        // 4. 执行新增/更新
        if (Objects.nonNull(existingRecord)) {
            // 更新逻辑：复用已有记录的 tblId，避免主键冲突
            record.setTblId(existingRecord.getTblId());
            record.setDbUpdateTime(LocalDateTime.now()); // 强制更新时间戳，确保数据一致性
            this.updateByKey(record);
            log.info("告警记录更新成功 | alarmId:{} | imagePath:{} | videoPath:{} | tblId:{}",
                    alarmId, imagePath, videoPath, existingRecord.getTblId());
        } else {
            // 新增逻辑：设置创建/更新时间戳
            record.setDbCreateTime(LocalDateTime.now());
            record.setDbUpdateTime(LocalDateTime.now());
            this.save(record);
            log.info("告警记录新增成功 | alarmId:{} | imagePath:{} | videoPath:{}", alarmId, imagePath, videoPath);
        }
    }

    /**
     * 根据关联 tblId 列表查询所有告警记录（按告警时间倒序）
     * @param relatedTblIdList tblId 列表（非空且非空列表）
     * @return 匹配的告警记录列表，无匹配时返回空列表（非 null）
     */
    public List<OriginalAlarmRecord> getListByTblIdList(List<String> relatedTblIdList) {
        // 校验列表合法性：避免 in 空列表导致的 SQL 错误（如 WHERE tbl_id IN ()）
        Assert.notEmpty(relatedTblIdList, "tblId 列表不能为空且不能包含空元素");
        Assert.isTrue(relatedTblIdList.stream().noneMatch(StringUtils::isEmpty), "tblId 列表中不能包含空元素");

        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OriginalAlarmRecord::getTblId, relatedTblIdList)
                .orderByDesc(OriginalAlarmRecord::getAlarmTime);

        // 安全查询：即使无数据也返回空列表，避免上层处理 null
        return this.list(wrapper);
    }

    /**
     * 根据关联 tblId 列表 + 检查标识查询告警记录
     * @param relatedTblIdList tblId 列表（非空且非空列表）
     * @param checkFlag 检查标识（非空）
     * @return 匹配的告警记录列表，无匹配时返回空列表（非 null）
     */
    public List<OriginalAlarmRecord> getListByTblIdList(List<String> relatedTblIdList, Integer checkFlag) {
        // 入参校验：覆盖所有关键参数
        Assert.notEmpty(relatedTblIdList, "tblId 列表不能为空且不能包含空元素");
        Assert.isTrue(relatedTblIdList.stream().noneMatch(StringUtils::isEmpty), "tblId 列表中不能包含空元素");
        Assert.notNull(checkFlag, "checkFlag 不能为空");

        // 调用 Mapper 方法：传递校验后的参数，降低 Mapper 层压力
        return originalAlarmMapper.getListByTblIdList(relatedTblIdList, checkFlag);
    }

    /**
     * 根据关联 tblId 列表查询告警时间范围
     * @param relatedTblIdList tblId 列表（非空且非空列表）
     * @return 时间范围对象（包含最大/最小告警时间），无数据时返回 null（需上层处理）
     */
    public AlarmTimeRange getTimeRangeByTblIdList(List<String> relatedTblIdList) {
        // 入参校验：避免无效查询
        Assert.notEmpty(relatedTblIdList, "tblId 列表不能为空且不能包含空元素");
        Assert.isTrue(relatedTblIdList.stream().noneMatch(StringUtils::isEmpty), "tblId 列表中不能包含空元素");
        return originalAlarmMapper.getTimeRangeByTblIdList(relatedTblIdList);
    }

    /**
     * 根据 tblId 列表 + 告警时间查询「小于该时间」的最新一条记录
     * @param relatedTblIdList tblId 列表（非空且非空列表）
     * @param alarmTime 告警时间（非空）
     * @return 匹配的最新记录，无匹配时返回 null
     */
    public OriginalAlarmRecord getRecordByTblIdListAndTime(List<String> relatedTblIdList, LocalDateTime alarmTime) {
        // 入参校验：时间不可为空，避免 SQL 逻辑错误
        Assert.notEmpty(relatedTblIdList, "tblId 列表不能为空且不能包含空元素");
        Assert.isTrue(relatedTblIdList.stream().noneMatch(StringUtils::isEmpty), "tblId 列表中不能包含空元素");
        Assert.notNull(alarmTime, "alarmTime 不能为空");

        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OriginalAlarmRecord::getTblId, relatedTblIdList)
                .lt(OriginalAlarmRecord::getAlarmTime, alarmTime)
                .orderByDesc(OriginalAlarmRecord::getAlarmTime)
                .last("LIMIT 1"); // 确保只返回最新一条
        return this.getOne(wrapper);
    }

    /**
     * 查询告警集关联记录中「被打标为事件」的记录
     * @param relatedIdList tblId 列表（非空且非空列表）
     * @return 匹配的记录列表，无匹配时返回空列表
     */
    public List<OriginalAlarmRecord> getEventByIdList(List<String> relatedIdList) {
        Assert.notEmpty(relatedIdList, "tblId 列表不能为空且不能包含空元素");
        Assert.isTrue(relatedIdList.stream().noneMatch(StringUtils::isEmpty), "tblId 列表中不能包含空元素");

        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OriginalAlarmRecord::getDealFlag, DEAL_FLAG_EVENT) // 用常量替代硬编码，降低修改成本
                .in(OriginalAlarmRecord::getTblId, relatedIdList)
                .orderByDesc(OriginalAlarmRecord::getAlarmTime);
        return this.list(wrapper);
    }

    /**
     * 查询告警集关联记录中「未被打标为事件」的记录
     * @param relatedIdList tblId 列表（非空且非空列表）
     * @return 匹配的记录列表，无匹配时返回空列表
     */
    public List<OriginalAlarmRecord> getNoEventByIdList(List<String> relatedIdList) {
        Assert.notEmpty(relatedIdList, "tblId 列表不能为空且不能包含空元素");
        Assert.isTrue(relatedIdList.stream().noneMatch(StringUtils::isEmpty), "tblId 列表中不能包含空元素");

        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(OriginalAlarmRecord::getDealFlag, DEAL_FLAG_EVENT)
                .in(OriginalAlarmRecord::getTblId, relatedIdList)
                .orderByDesc(OriginalAlarmRecord::getAlarmTime);
        return this.list(wrapper);
    }

    /**
     * 查询告警集关联记录中「最新一条被确认为事件」的记录
     * @param relatedIdList tblId 列表（非空且非空列表）
     * @return 最新事件记录，无匹配时返回 null
     */
    public OriginalAlarmRecord getLatestConfirm(List<String> relatedIdList) {
        Assert.notEmpty(relatedIdList, "tblId 列表不能为空且不能包含空元素");
        Assert.isTrue(relatedIdList.stream().noneMatch(StringUtils::isEmpty), "tblId 列表中不能包含空元素");

        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OriginalAlarmRecord::getTblId, relatedIdList)
                .eq(OriginalAlarmRecord::getDealFlag, DEAL_FLAG_EVENT)
                .orderByDesc(OriginalAlarmRecord::getAlarmTime)
                .last("LIMIT 1");

        return this.getOne(wrapper);
    }

    /**
     * 查询告警集关联记录中「最新事件之后」的所有告警
     * @param relatedIdList tblId 列表（非空且非空列表）
     * @param alarmTime 最新事件的告警时间（非空）
     * @return 匹配的告警列表，无匹配时返回空列表
     */
    public List<OriginalAlarmRecord> getUnConfirmListByTime(List<String> relatedIdList, LocalDateTime alarmTime) {
        Assert.notEmpty(relatedIdList, "tblId 列表不能为空且不能包含空元素");
        Assert.isTrue(relatedIdList.stream().noneMatch(StringUtils::isEmpty), "tblId 列表中不能包含空元素");
        Assert.notNull(alarmTime, "alarmTime 不能为空");

        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OriginalAlarmRecord::getTblId, relatedIdList)
                .gt(OriginalAlarmRecord::getAlarmTime, alarmTime)
                .orderByDesc(OriginalAlarmRecord::getAlarmTime);
        return this.list(wrapper);
    }


    /**
     * 插入新记录（带时间戳初始化）
     * @param record 待插入的告警记录（非空）
     */
    public void insert(OriginalAlarmRecord record) {
        Assert.notNull(record, "待插入的告警记录不能为空");

        // 初始化时间戳：避免手动设置遗漏，确保数据一致性
        LocalDateTime now = LocalDateTime.now();
        record.setDbCreateTime(now);
        record.setDbUpdateTime(now);
        this.save(record);
        log.debug("告警记录插入成功 | alarmId:{} | imagePath:{} | videoPath:{}", record.getId(), record.getImagePath(), record.getVideoPath());
    }

    /**
     * 根据联合主键更新记录（带时间戳更新）
     * @param record 待更新的告警记录（非空，且需包含联合主键字段）
     */
    public void updateByKey(OriginalAlarmRecord record) {
        Assert.notNull(record, "待更新的告警记录不能为空");
        record.setDbUpdateTime(LocalDateTime.now());
        originalAlarmMapper.updateById(record);
    }

    /**
     * 根据联合主键（alarmId+imagePath+videoPath）判断记录是否存在
     * @param alarmId 告警ID（非空且非空白）
     * @param imagePath 图片路径（非空且非空白）
     * @param videoPath 视频路径（非空且非空白）
     * @return true：存在，false：不存在
     */
    public boolean existsByKey(String alarmId, String imagePath, String videoPath) {
        // 1. 联合主键空值校验：提前阻断非法参数，避免无效查询
        validateUnionKey(alarmId, imagePath, videoPath);

        // 2. 使用 LambdaQueryWrapper：类型安全，避免字段名硬编码（防SQL字段写错）
        LambdaQueryWrapper<OriginalAlarmRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OriginalAlarmRecord::getId, alarmId)  // 与实体类字段映射保持一致
                .eq(OriginalAlarmRecord::getImagePath, imagePath)
                .eq(OriginalAlarmRecord::getVideoPath, videoPath);

        // 3. 高效判断：用 count() == 0 替代 count() > 0，底层SQL执行逻辑一致，语义更直观
        return this.count(wrapper) > 0;
    }

    /**
     * 多条件联合分页查询（关联原始告警字段）
     * @param alarmId 告警ID（可选）
     * @param startDate 开始日期（可选，格式 yyyy-MM-dd）
     * @param endDate 结束日期（可选，格式 yyyy-MM-dd）
     * @param deviceName 设备名称（可选，模糊匹配）
     * @param roadId 道路ID（可选）
     * @param directionDes 方向描述（可选，模糊匹配）
     * @param eventType 告警类型（可选）
     * @param dealFlag 处理标识（可选）
     * @param checkFlag 检查标识（可选）
     * @param disposalAdvice 处置建议（可选）
     * @param adviceReason 建议原因（可选，模糊匹配）
     * @param deviceId 设备ID（可选）
     * @param pageNo 页码（必须为正整数，从1开始）
     * @param pageSize 每页条数（必须为正整数）
     * @return 分页查询结果（含总条数、当前页数据，无数据时返回空列表）
     */
    public IPage<QueryResultCheckRecord> selectWithOriginaleField(String alarmId, String startDate, String endDate, String deviceName, String roadId, String directionDes, String eventType, Integer dealFlag, Integer checkFlag, Integer disposalAdvice, String adviceReason, String deviceId, int pageNo, int pageSize) {
        // 1. 分页参数校验：防止非法页码（如0、负数）导致的分页异常
        Assert.isTrue(pageNo > 0 && pageSize > 0, "分页参数（pageNo/pageSize）必须为正整数");

        // 2. 初始化分页对象（MyBatis-Plus 分页插件自动生效）
        Page<QueryResultCheckRecord> page = new Page<>(pageNo, pageSize);
        QueryWrapper<QueryResultCheckRecord> query = new QueryWrapper<>();

        if (alarmId != null) {
            query.eq("o.alarm_id", alarmId);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.hasText(startDate)) {
            LocalDate date = LocalDate.parse(startDate, formatter);
            query.ge("o.alarm_time", date.atStartOfDay());
        }

        if (StringUtils.hasText(endDate)) {
            LocalDate date = LocalDate.parse(endDate, formatter);
            query.le("o.alarm_time", date.atTime(23, 59, 59));
        }

        if (StringUtils.hasText(deviceName)) {
            query.like("o.content", deviceName);
        }

        if (roadId != null) {
            query.eq("o.road_id", roadId);
        }

        if (directionDes != null) {
            query.like("o.direction_des", directionDes);
        }

        if (StringUtils.hasText(eventType)) {
            query.eq("o.event_type", eventType);
        }

        if (dealFlag != null) {
            query.eq("o.deal_flag", dealFlag);
        }

        if (checkFlag != null) {
            query.eq("a.check_flag", checkFlag);
        }

        if (disposalAdvice != null) {
            query.eq("f.disposal_advice", disposalAdvice);
        }

        if (StringUtils.hasText(adviceReason)) {
            query.like("f.advice_reason", adviceReason);
        }

        if (StringUtils.hasText(deviceId)) {
            query.eq("o.device_id", deviceId);
        }

        query.orderByDesc("o.alarm_time");

        // 调用 Mapper 方法执行分页查询
        IPage<QueryResultCheckRecord> result = originalAlarmMapper.selectWithJoin(page, query);

        return result;
    }

    /**
     * 校验联合主键（alarmId + imagePath + videoPath）非空
     * @param alarmId 告警ID
     * @param imagePath 图片路径
     * @param videoPath 视频路径
     */
    private void validateUnionKey(String alarmId, String imagePath, String videoPath) {
        Assert.hasText(alarmId, "联合主键[alarmId]不能为空或空白");
        Assert.hasText(imagePath, "联合主键[imagePath]不能为空或空白");
        Assert.hasText(videoPath, "联合主键[videoPath]不能为空或空白");
    }

    /**
     * 处理日期范围查询条件（解析日期 + 避免格式异常 + 设置时间边界）
     * @param query 查询条件包装器
     * @param startDate 开始日期（格式 yyyy-MM-dd）
     * @param endDate 结束日期（格式 yyyy-MM-dd）
     */
    private void handleDateRangeCondition(LambdaQueryWrapper<QueryResultCheckRecord> query, String startDate, String endDate) {
        // 处理开始日期：解析为 00:00:00
        if (StringUtils.hasText(startDate)) {
            try {
                LocalDate startLocalDate = LocalDate.parse(startDate, DATE_FORMATTER);
                LocalDateTime startDateTime = startLocalDate.atStartOfDay(); // 如 2025-01-01 00:00:00
                query.ge(QueryResultCheckRecord::getAlarmTime, startDateTime);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("开始" + TIME_PARSE_ERROR, e);
            }
        }

        // 处理结束日期：解析为 23:59:59
        if (StringUtils.hasText(endDate)) {
            try {
                LocalDate endLocalDate = LocalDate.parse(endDate, DATE_FORMATTER);
                LocalDateTime endDateTime = endLocalDate.atTime(23, 59, 59); // 如 2025-01-01 23:59:59
                query.le(QueryResultCheckRecord::getAlarmTime, endDateTime);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("结束" + TIME_PARSE_ERROR, e);
            }
        }
    }
}