package com.yuce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("collection_group_record")
public class CollectionGroupRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String collectionId;

    private String groupId;

    private String groupImageUrl;

    private String groupEventType;

    private String alarmId;

    private String imagePath;

    private String videoPath;

    private String eventType;

    private LocalDateTime alarmTime;

    private String extractImageUrl;

    private String alarmElement;

    private Integer point1X;

    private Integer point1Y;

    private Integer point2X;

    private Integer point2Y;

    private double leadCompareIou;

    private double leadCompareMinute;

    private LocalDateTime createTime;

    private LocalDateTime modifyTime;
}