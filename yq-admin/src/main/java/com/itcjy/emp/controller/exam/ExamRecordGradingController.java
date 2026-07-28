package com.itcjy.emp.controller.exam;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.exam.ExamGradeReq;
import com.itcjy.emp.pojo.res.exam.ExamGradingRes;
import com.itcjy.emp.service.exam.IExamManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/exam-records")
public class ExamRecordGradingController {
    private final IExamManagementService examService;

    @GetMapping("/{recordId}/grading")
    @HasPermission(code = "sys:exam:grading:detail", name = "查看主观题答卷")
    public ApiResponse<ExamGradingRes> detail(@PathVariable @Positive Long recordId) {
        return ApiResponse.success(examService.gradingDetail(recordId));
    }

    @PutMapping("/{recordId}/grading")
    @HasPermission(code = "sys:exam:grading:update", name = "批改主观题")
    public ApiResponse<ExamGradingRes> grade(@PathVariable @Positive Long recordId,
                                             @Valid @RequestBody ExamGradeReq req) {
        return ApiResponse.success(examService.grade(recordId, req));
    }
}
