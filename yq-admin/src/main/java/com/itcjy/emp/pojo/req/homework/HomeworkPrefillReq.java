package com.itcjy.emp.pojo.req.homework;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Schema(description = "发布作业预填请求")
public class HomeworkPrefillReq {

    @Schema(description = "班级ID", example = "1")
    @NotNull(message = "班级不能为空")
    @Positive(message = "班级ID必须大于0")
    private Long classId;

    @Schema(description = "作业日期", example = "2026-07-24")
    @NotNull(message = "作业日期不能为空")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate homeworkDate;
}
