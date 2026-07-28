package com.itcjy.emp.pojo.req.exam;
import jakarta.validation.constraints.NotNull;
public record ExamVisibilityReq(@NotNull Boolean answerVisible) {}
