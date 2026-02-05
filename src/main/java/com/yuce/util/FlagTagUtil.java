package com.yuce.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName FlagTagEnum
 * @Description TODO
 * @Author jacksparrow
 * @Email 18310124408@163.com
 * @Date 2025/11/28 16:19
 * @Version 1.0
 */
public class FlagTagUtil {

    /** 算法初检结果：无法判断 */
    public static final int CHECK_RESULT_UNKNOWN = 0;

    /** 算法初检结果：正检 */
    public static final int CHECK_RESULT_RIGHT = 1;

    /** 算法初检结果：误检 */
    public static final int CHECK_RESULT_ERROR = 2;

    /** 算法初检结果来源：通用算法 */
    public static final String CHECK_ALGO_SOURCE_GENERAL = "通用算法";

    /** 算法初检结果来源：研发算法 */
    public static final String CHECK_ALGO_SOURCE_DEVELOP = "自研算法";

    /** 处置建议：无法判断 */
    public static final int ADVICE_UNDETERMINED = 0;

    /** 处置建议：疑似误报 */
    public static final int ADVICE_FALSE_ALARM = 1;

    /** 处置建议：尽快确认 */
    public static final int ADVICE_CONFIRM = 2;

    /** 处置建议：无需处理（含重复告警） */
    public static final int ADVICE_NO_NEED = 3;

    /** 处置建议：重复告警 */
    public static final int ADVICE_REPEAT = 4;

    /** 处置建议：临时等待 */
    public static final int ADVICE_TMP_WAIT = 5;

    /** 拥堵判定阈值：满足条件的图片数量 */
    public static final int CONGESTION_IMG_THRESHOLD = 2;

    /** 路面占比阈值：判定拥堵的面积占比 */
    public static final double ROAD_AREA_THRESHOLD = 0.7;

    /** 车道占用阈值：判定占道的百分比 */
    public static final double LANE_OCCUPY_THRESHOLD = 0.1;

    /** 告警集来源类型：正常告警 */
    public static final int SOURCE_TYPE_NORMAL = 1;

    /** 告警集来源类型：非法车辆 */
    public static final int SOURCE_TYPE_ILLEGAL_ENTRY = 2;

    /** 告警集状态：开启 */
    public static final int COLLECTION_STATUS_OPEN = 1;

    /** 告警集状态：关闭 */
    public static final int COLLECTION_STATUS_CLOSED = 2;

    // ------------------------------ 常量定义（内部静态类封装，便于维护） ------------------------------
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ElementConstant {
        /** 人员类型清单 */
        public static final List<String> PERSON_LIST = new ArrayList<String>() {{
            add("person");
            add("traffic_police");
            add("medical_person");
            add("builder");
        }};

        /** 施救力量清单 */
        public static final List<String> RESCUE_LIST = new ArrayList<String>() {{
            add("anti_collision_vehicle");
            add("maintenance_construction_vehicle");
            add("police_car");
            add("ambulance");
            add("fire_fighting_truck");
            add("traffic_police");
            add("medical_person");
            add("builder");
        }};

        /** 忽略的车辆类型（无需处理） */
        public static final List<String> IGNORE_VEHICLE_LIST = new ArrayList<String>() {{
            add("anti_collision_vehicle");
            add("maintenance_construction_vehicle");
            add("police_car");
            add("ambulance");
            add("fire_fighting_truck");
        }};

        /** 忽略的人员类型（无需处理） */
        public static final List<String> IGNORE_PERSON_LIST = new ArrayList<String>() {{
            add("medical_person");
            add("builder");
            add("traffic_police");
        }};

        /** 忽略的抛洒物类型（无需处理） */
        public static final List<String> IGNORE_PSW_LIST = new ArrayList<String>() {{
            add("paper");
            add("plastic bags");
            add("plastic");
            add("cardboard");
            add("warning triangle");
        }};
    }
}