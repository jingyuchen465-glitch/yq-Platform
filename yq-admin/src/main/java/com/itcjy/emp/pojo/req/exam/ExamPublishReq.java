package com.itcjy.emp.pojo.req.exam;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public record ExamPublishReq(
        @NotNull @Positive Long paperId,
        @NotEmpty List<@Positive Long> classIds,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime entryDeadlineTime,
        @NotNull @Min(1) @Max(1440) Integer durationMinutes,
        @Positive Long invigilatorUserId) {}
