package com.yuce.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yuce.entity.QueryResultCheckRecord;
import java.time.LocalDate;

public interface CheckAlarmResultService {
    IPage<QueryResultCheckRecord> selectWithOriginaleField(int pageNo, int pageSize,
                                                           LocalDate startDate, LocalDate endDate,
                                                           String eventType, String content,
                                                           Integer checkFlag, String roadId, String directiondes);
}
