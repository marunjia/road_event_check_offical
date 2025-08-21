package com.yuce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuce.entity.CheckAlarmProcess;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface CheckAlarmProcessMapper extends BaseMapper<CheckAlarmProcess> {

    //根据告警记录id、图片路径、视频路径的联合主键统计检测的照片数量
    @Select("SELECT COUNT(DISTINCT image_id) FROM algorithm_check_process WHERE alarm_id = #{alarmId} and image_path = #{imagePath} and video_path = #{videoPath} ")
    int countDistinctImageId(@Param("alarmId") String alarmId, @Param("imagePath") String imagePath, @Param("videoPath") String videoPath);


    //根据告警记录id、图片路径、视频路径的联合主键查询 iou top1
    @Select("SELECT * FROM algorithm_check_process WHERE alarm_id = #{alarmId} AND image_path = #{imagePath} AND video_path = #{videoPath} ORDER BY iou DESC LIMIT 1")
    CheckAlarmProcess getIouTop1ByKey(@Param("alarmId") String alarmId,
                                      @Param("imagePath") String imagePath,
                                      @Param("videoPath") String videoPath);

    //根据告警记录id、图片路径、视频路径、图片id的联合主键查询 iou top1
    @Select("SELECT * FROM algorithm_check_process WHERE alarm_id = #{alarmId} AND image_path = #{imagePath} AND video_path = #{videoPath} AND image_id = #{imageId} ORDER BY iou DESC LIMIT 1")
    CheckAlarmProcess getIouTop1ByKeyAndPic(@Param("alarmId") String alarmId,
                                            @Param("imagePath") String imagePath,
                                            @Param("videoPath") String videoPath,
                                            @Param("imageId") String imageId);

    //根据告警记录id、图片路径、视频路径、type的联合主键查询对应记录明细
    @Select("SELECT * FROM algorithm_check_process WHERE alarm_id = #{alarmId} AND image_path = #{imagePath} AND video_path = #{videoPath} AND type = #{type} ORDER BY iou desc, score desc")
    List<CheckAlarmProcess> getListByKeyAndType(@Param("alarmId") String alarmId,
                                                @Param("imagePath") String imagePath,
                                                @Param("videoPath") String videoPath,
                                                @Param("type") String type);

    //根据告警记录id、图片路径、视频路径、type的联合主键查询对应记录明细
    @Select("SELECT * FROM algorithm_check_process WHERE alarm_id = #{alarmId} AND image_path = #{imagePath} AND video_path = #{videoPath}")
    List<CheckAlarmProcess> getListByKey(@Param("alarmId") String alarmId,
                                         @Param("imagePath") String imagePath,
                                         @Param("videoPath") String videoPath);

    //查询某张图片检测结果中除iou top1以外的物体分类数量，并按照优先级进行排序
    @Select("select t.name,count(t.name) as occur_num\n" +
            "from \n" +
            "(SELECT \n" +
            "    type,\n" +
            "    name,\n" +
            "    case when name = 'person' then 1\n" +
            "         when name = 'builder' then 2\n" +
            "         when name = 'traffic_police' then 3\n" +
            "         when name = 'medical_person' then 4\n" +
            "         when name = 'dangerous_goods_vehicle' then 5\n" +
            "         when name = 'ambulance' then 6\n" +
            "         when name = 'fire_fighting_truck' then 7\n" +
            "         when name = 'police_car' then 8\n" +
            "         when name = 'maintenance_construction_vehicle' then 9\n" +
            "         when name = 'anti_collision_vehicle' then 10\n" +
            "         when name = 'motorcycle' then 11\n" +
            "         when name = 'cone_barrel' then 12\n" +
            "         when name = 'tripod' then 13\n" +
            "         when name = 'water_filled_barrier' then 14\n" +
            "         when name = 'truck' then 15\n" +
            "         when name = 'coach' then 16\n" +
            "         when name = 'sedan' then 17\n" +
            "    end as level\n" +
            "FROM algorithm_check_process \n" +
            "WHERE alarm_id = #{alarmId} \n" +
            "AND image_path = #{imagePath} \n" +
            "AND video_path = #{videoPath} \n" +
            "AND image_id = #{imageId} \n" +
            "AND id != #{id} \n" +
            "AND score >= 0.35 \n" +
            ") t group by t.name,t.level\n" +
            "order by t.level \n" +
            "limit 4;")
    List<Map<String, Integer>> getElementGroupByKey(
            @Param("alarmId") String alarmId,
            @Param("imagePath") String imagePath,
            @Param("videoPath") String videoPath,
            @Param("imageId") String imageId,
            @Param("id") int id
    );
}