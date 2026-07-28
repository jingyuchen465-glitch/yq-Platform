package com.itcjy.stu.pojo.DTO;
import jakarta.validation.constraints.Size;
public record StudentExamAnswerDTO(@Size(max = 20000) String answerContent) {}
