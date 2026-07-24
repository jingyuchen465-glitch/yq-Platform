package com.itcjy.emp.pojo.req.academic;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

@Data
@Schema(description = "教师月度课表查询参数")
public class SysTeacherScheduleCalendarReq {

    @Positive(message = "教师ID必须大于0")
    @Schema(description = "教师ID；为空时默认选择第一位有效讲师")
    private Long teacherId;

    @NotNull(message = "查询月份不能为空")
    @DateTimeFormat(pattern = "yyyy-MM")
    @Schema(description = "查询月份", example = "2026-07")
    private YearMonth month;
}
