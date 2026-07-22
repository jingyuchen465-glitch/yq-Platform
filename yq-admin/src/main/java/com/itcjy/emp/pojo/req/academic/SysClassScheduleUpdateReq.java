package com.itcjy.emp.pojo.req.academic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "单条课程日程修改请求")
public record SysClassScheduleUpdateReq(
        @Schema(description = "课程阶段", example = "阶段一：基础入门")
        @NotBlank(message = "课程阶段不能为空")
        @Size(max = 255, message = "课程阶段长度不能超过255个字符") String stageName,

        @Schema(description = "课程内容", example = "项目实战与集中答疑")
        @NotBlank(message = "课程内容不能为空")
        @Size(max = 500, message = "课程内容长度不能超过500个字符") String courseContent,

        @Schema(description = "授课教师ID", example = "41")
        @NotNull(message = "授课教师不能为空")
        @Positive(message = "授课教师ID必须大于0") Long teacherId
) {
}
