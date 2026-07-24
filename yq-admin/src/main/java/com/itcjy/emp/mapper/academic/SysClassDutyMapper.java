package com.itcjy.emp.mapper.academic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.emp.pojo.entity.SysClassDuty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SysClassDutyMapper extends BaseMapper<SysClassDuty> {

    @Select("""
            SELECT *
            FROM sys_class_duty
            WHERE campus_id = #{campusId}
              AND duty_date = #{dutyDate}
            ORDER BY duty_type ASC, class_id ASC, id ASC
            """)
    List<SysClassDuty> selectDaily(@Param("campusId") Long campusId,
                                   @Param("dutyDate") LocalDate dutyDate);

    @Select("""
            SELECT *
            FROM sys_class_duty
            WHERE campus_id = #{campusId}
              AND duty_date = #{dutyDate}
              AND duty_type = #{dutyType}
              AND ((#{classId} IS NULL AND class_id IS NULL) OR class_id = #{classId})
            ORDER BY id ASC
            LIMIT 1
            FOR UPDATE
            """)
    SysClassDuty selectScopeForUpdate(@Param("campusId") Long campusId,
                                      @Param("classId") Long classId,
                                      @Param("dutyDate") LocalDate dutyDate,
                                      @Param("dutyType") String dutyType);

    @Select("""
            SELECT *
            FROM sys_class_duty
            WHERE teacher_id = #{teacherId}
              AND duty_date = #{dutyDate}
            ORDER BY start_time ASC, id ASC
            """)
    List<SysClassDuty> selectTeacherDuties(@Param("teacherId") Long teacherId,
                                           @Param("dutyDate") LocalDate dutyDate);
}
