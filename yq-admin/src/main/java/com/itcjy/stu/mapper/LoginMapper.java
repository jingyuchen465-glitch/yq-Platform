package com.itcjy.stu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcjy.stu.pojo.projection.StudentClassProfileRow;
import com.itcjy.stu.pojo.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginMapper extends BaseMapper<Student> {

    @Select("""
            SELECT c.id AS class_id,
                   c.class_period AS class_name,
                   c.campus_id,
                   campus.campus_location,
                   c.course_id,
                   course.course_name,
                   c.head_teacher_id,
                   COALESCE(NULLIF(teacher.real_name, ''), NULLIF(teacher.nickname, ''), teacher.username)
                       AS head_teacher_name,
                   teacher.phone AS head_teacher_phone,
                   teacher.email AS head_teacher_email
            FROM sys_class c
            LEFT JOIN sys_campus campus ON campus.id = c.campus_id
            LEFT JOIN sys_course course ON course.id = c.course_id
            LEFT JOIN sys_user teacher ON teacher.id = c.head_teacher_id
            WHERE c.id = #{classId}
            LIMIT 1
            """)
    StudentClassProfileRow selectStudentClassProfile(@Param("classId") Long classId);
}
