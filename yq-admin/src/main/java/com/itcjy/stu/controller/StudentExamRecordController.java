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

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/stu/exam-records")
public class StudentExamRecordController {
    private final StudentExamService examService;

    @GetMapping("/{recordId}")
    public ApiResponse<StudentExamVO> detail(@PathVariable @Positive Long recordId) {
        return ApiResponse.success(examService.detail(recordId));
    }

    @PutMapping("/{recordId}/answers/{paperQuestionId}")
    public ApiResponse<Void> saveAnswer(@PathVariable @Positive Long recordId,
                                        @PathVariable @Positive Long paperQuestionId,
                                        @Valid @RequestBody StudentExamAnswerDTO dto) {
        examService.saveAnswer(recordId, paperQuestionId, dto);
        return ApiResponse.success();
    }

    @PostMapping("/{recordId}/submit")
    public ApiResponse<StudentExamVO> submit(@PathVariable @Positive Long recordId) {
        return ApiResponse.success(examService.submit(recordId));
    }

    @GetMapping("/{recordId}/result")
    public ApiResponse<StudentExamResultVO> result(@PathVariable @Positive Long recordId) {
        return ApiResponse.success(examService.result(recordId));
    }
}
