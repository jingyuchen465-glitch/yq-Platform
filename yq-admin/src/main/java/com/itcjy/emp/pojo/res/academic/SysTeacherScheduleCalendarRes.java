package com.itcjy.emp.pojo.res.academic;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Schema(description = "教师月度课程表")
public record SysTeacherScheduleCalendarRes(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM")
        @Schema(description = "查询月份", example = "2026-07") YearMonth month,
        @Schema(description = "当前教师ID") Long teacherId,
        @Schema(description = "当前教师名称") String teacherName,
        @Schema(description = "本月上课天数") Integer teachingDays,
        @Schema(description = "教师下拉选项") List<TeacherOption> teachers,
        @Schema(description = "当前教师课程安排") List<ScheduleItem> schedules,
        @Schema(description = "本月无课教师") List<TeacherOption> noCourseTeachers
) {

    @Schema(description = "教师选项")
    public record TeacherOption(
            @Schema(description = "教师ID") Long id,
            @Schema(description = "教师显示名称") String name
    ) {
    }

    @Schema(description = "教师课程日程")
    public record ScheduleItem(
            @Schema(description = "课表ID") Long scheduleId,
            @Schema(description = "上课日期") LocalDate scheduleDate,
            @Schema(description = "班级ID") Long classId,
            @Schema(description = "班级名称") String className,
            @Schema(description = "教师ID") Long teacherId,
            @Schema(description = "教师名称") String teacherName,
            @Schema(description = "课程详情ID") Long courseDetailId,
            @Schema(description = "课程阶段") String stageName,
            @Schema(description = "课程内容") String courseContent
    ) {
    }
}
