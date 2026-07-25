package com.itcjy.emp.pojo.req.homework;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "作业批改请求，仅允许更新分数和教师评语")
public record HomeworkSubmissionGradeReq(
        @Schema(description = "分数", example = "95")
        @NotNull(message = "分数不能为空")
        @Min(value = 0, message = "分数不能小于0")
        @Max(value = 100, message = "分数不能大于100")
        Integer score,

        @Schema(description = "教师评语")
        @Size(max = 500, message = "教师评语不能超过500个字符")
        String teacherRemark
) {
}
