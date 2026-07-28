package com.itcjy.stu.pojo.VO;

import com.itcjy.emp.pojo.res.academic.SysClassScheduleRes;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "学生课程表中的单日安排")
public record StudentScheduleCourseVO(
        @Schema(description = "课表ID") Long id,
        @Schema(description = "排课日期") LocalDate scheduleDate,
        @Schema(description = "课程详情ID") Long courseDetailId,
        @Schema(description = "课程阶段") String stageName,
        @Schema(description = "课程内容") String courseContent,
        @Schema(description = "类型：CLASS/SELF_STUDY/REST/HOLIDAY") String classType,
        @Schema(description = "类型名称") String classTypeName,
        @Schema(description = "授课教师ID") Long teacherId,
        @Schema(description = "授课教师姓名") String teacherName
) {

    public static StudentScheduleCourseVO from(SysClassScheduleRes schedule) {
        return new StudentScheduleCourseVO(
                schedule.id(),
                schedule.scheduleDate(),
                schedule.courseDetailId(),
                schedule.stageName(),
                schedule.courseContent(),
                schedule.classType(),
                schedule.classTypeName(),
                schedule.teacherId(),
                schedule.teacherName()
        );
    }
}
