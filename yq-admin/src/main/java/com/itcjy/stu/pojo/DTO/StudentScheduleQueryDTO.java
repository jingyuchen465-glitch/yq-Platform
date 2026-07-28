package com.itcjy.stu.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "学生课程表查询条件")
public class StudentScheduleQueryDTO {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "开始日期", example = "2026-07-27")
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "结束日期", example = "2026-08-02")
    private LocalDate endDate;
}
