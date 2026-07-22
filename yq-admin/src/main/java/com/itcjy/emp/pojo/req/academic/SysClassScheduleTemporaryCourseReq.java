package com.itcjy.emp.pojo.req.academic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "临时加课请求")
public record SysClassScheduleTemporaryCourseReq(
        @Schema(description = "班级ID", example = "14")
        @NotNull(message = "班级ID不能为空")
        @Positive(message = "班级ID必须大于0") Long classId,

        @Schema(description = "课程阶段", example = "阶段一：基础入门")
        @NotBlank(message = "课程阶段不能为空")
        @Size(max = 255, message = "课程阶段长度不能超过255个字符") String stageName,

        @Schema(description = "授课教师ID", example = "41")
        @NotNull(message = "授课教师不能为空")
        @Positive(message = "授课教师ID必须大于0") Long teacherId,

        @Schema(description = "加课日期", example = "2026-07-30")
        @NotNull(message = "加课日期不能为空") LocalDate scheduleDate
) {
}
