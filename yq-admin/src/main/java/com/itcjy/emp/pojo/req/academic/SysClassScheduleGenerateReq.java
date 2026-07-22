package com.itcjy.emp.pojo.req.academic;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

@Schema(description = "生成班级课程表请求参数")
public record SysClassScheduleGenerateReq(
        @Schema(description = "班级ID", example = "1")
        @NotNull(message = "班级ID不能为空")
        @Positive(message = "班级ID必须大于0")
        Long classId,

        @Schema(description = "第一天上课日期", example = "2026-07-27")
        @NotNull(message = "开始日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate startDate
) {
}
