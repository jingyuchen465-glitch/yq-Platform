package com.itcjy.emp.pojo.req.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ExamGradeReq(@NotEmpty @Valid List<AnswerGrade> answers) {
    public record AnswerGrade(
            @NotNull @Positive Long answerId,
            @NotNull @DecimalMin("0") BigDecimal score,
            @Size(max = 500) String comment) {}
}
