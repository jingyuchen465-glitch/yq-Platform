package com.itcjy.stu.pojo.VO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "当前学生的课程表")
public record StudentScheduleVO(
        @Schema(description = "是否已分配班级") boolean classAssigned,
        @Schema(description = "学生当前班级ID，未分班时为 null") Long classId,
        @Schema(description = "课程安排") List<StudentScheduleCourseVO> courses
) {

    public static StudentScheduleVO unassigned() {
        return new StudentScheduleVO(false, null, List.of());
    }

    public static StudentScheduleVO assigned(Long classId, List<StudentScheduleCourseVO> courses) {
        return new StudentScheduleVO(true, classId, courses);
    }
}
