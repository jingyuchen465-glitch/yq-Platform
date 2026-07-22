package com.itcjy.emp.pojo.res;

import com.itcjy.common.constants.ClassScheduleConstants;
import com.itcjy.emp.pojo.entity.SysClassSchedule;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "班级课程表日期信息")
public record SysClassScheduleRes(
        @Schema(description = "课表ID") Long id,
        @Schema(description = "排课日期") LocalDate scheduleDate,
        @Schema(description = "课程详情ID，上课日使用") Long courseDetailId,
        @Schema(description = "课程阶段，上课日使用") String stageName,
        @Schema(description = "课程内容") String courseContent,
        @Schema(description = "类型：CLASS/SELF_STUDY/REST/HOLIDAY") String classType,
        @Schema(description = "类型名称") String classTypeName,
        @Schema(description = "授课教师ID，上课日使用") Long teacherId,
        @Schema(description = "授课教师姓名，上课日使用") String teacherName
) {
    public static SysClassScheduleRes from(SysClassSchedule schedule, String stageName, String teacherName) {
        return new SysClassScheduleRes(
                schedule.getId(),
                schedule.getScheduleDate(),
                schedule.getCourseDetailId(),
                stageName,
                schedule.getCourseContent(),
                schedule.getClassType(),
                getClassTypeName(schedule.getClassType()),
                schedule.getTeacherId(),
                teacherName
        );
    }

    private static String getClassTypeName(String classType) {
        return switch (classType) {
            case ClassScheduleConstants.DayType.CLASS -> "上课";
            case ClassScheduleConstants.DayType.SELF_STUDY -> "自习";
            case ClassScheduleConstants.DayType.REST -> "休息";
            case ClassScheduleConstants.DayType.HOLIDAY -> "节假日";
            default -> "未知";
        };
    }
}
