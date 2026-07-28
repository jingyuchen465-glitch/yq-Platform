package com.itcjy.stu.controller;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.stu.pojo.DTO.StudentExamAnswerDTO;
import com.itcjy.stu.pojo.VO.*;
import com.itcjy.stu.service.StudentExamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/stu/exams")
public class StudentExamController {
    private final StudentExamService examService;

    @GetMapping
    public ApiResponse<List<StudentExamListVO>> list() {
        return ApiResponse.success(examService.listCurrentStudentExams());
    }

    @PostMapping("/{examId}/start")
    public ApiResponse<StudentExamVO> start(@PathVariable @Positive Long examId) {
        return ApiResponse.success(examService.start(examId));
    }
}
