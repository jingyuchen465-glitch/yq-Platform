package com.itcjy.emp.mapper.academic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import com.itcjy.emp.pojo.projection.TeacherScheduleRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SysClassScheduleMapper extends BaseMapper<SysClassSchedule> {

    @Select("SELECT * FROM sys_class_schedule WHERE id = #{id} FOR UPDATE")
    SysClassSchedule selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT DISTINCT teacher_id
            FROM sys_class_schedule
            WHERE teacher_id IS NOT NULL
              AND class_type = 'CLASS'
              AND schedule_date BETWEEN #{startDate} AND #{endDate}
            """)
    List<Long> selectTeachingTeacherIds(@Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT s.id AS schedule_id,
                   s.schedule_date,
                   s.class_id,
                   c.class_period AS class_name,
                   s.course_detail_id,
                   cd.stage_name,
                   s.course_content
            FROM sys_class_schedule s
            INNER JOIN sys_class c ON c.id = s.class_id
            LEFT JOIN sys_course_detail cd ON cd.id = s.course_detail_id
            WHERE s.teacher_id = #{teacherId}
              AND s.class_type = 'CLASS'
              AND s.schedule_date BETWEEN #{startDate} AND #{endDate}
            ORDER BY s.schedule_date ASC, s.id ASC
            """)
    List<TeacherScheduleRow> selectTeacherScheduleRows(@Param("teacherId") Long teacherId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);
}
