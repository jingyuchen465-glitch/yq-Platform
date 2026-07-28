package com.itcjy.emp.pojo.req.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ExamQuestionSaveReq(
        @NotBlank @Size(max = 32) String questionType,
        @NotBlank String questionContent,
        @NotBlank String answerContent,
        String analysisContent,
        @NotBlank @Size(max = 32) String difficulty,
        @Valid List<OptionItem> options,
        @NotEmpty @Valid List<CourseStageItem> courseStages) {

    public record OptionItem(
            @NotBlank @Size(max = 16) String optionKey,
            @NotBlank String optionContent,
            @NotNull @Min(0) Integer sortOrder) {}

    public record CourseStageItem(
            @NotNull @Positive Long courseId,
            @NotBlank @Size(max = 64) String stageName) {}
}
