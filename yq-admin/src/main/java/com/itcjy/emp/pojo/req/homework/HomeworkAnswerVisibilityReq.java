package com.itcjy.emp.pojo.req.homework;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "修改标准答案学生可见性请求")
public record HomeworkAnswerVisibilityReq(
        @Schema(description = "学生是否可见")
        @NotNull(message = "学生可见状态不能为空")
        Boolean studentVisible
) {
}
