package com.itcjy.emp.pojo.req.exam;
import jakarta.validation.constraints.NotBlank;
public record ExamQuestionStatusReq(@NotBlank String status) {}
