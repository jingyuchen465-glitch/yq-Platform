package com.itcjy.emp.pojo.req.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ExamPaperSaveReq(
        @NotBlank @Size(max = 128) String paperName,
        @NotNull @Positive Long courseId,
        @NotEmpty List<@NotBlank @Size(max = 64) String> stageNames,
        @NotEmpty @Valid List<QuestionItem> questions) {

    public record QuestionItem(
            @NotNull @Positive Long questionId,
            @NotNull @DecimalMin(value = "0.1") BigDecimal questionScore,
            @NotNull @Min(0) Integer sortOrder) {}
}
