package com.itcjy.emp.controller.exam;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.*;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.*;
import com.itcjy.emp.service.exam.IExamManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/exams")
public class ExamManagementController {
    private final IExamManagementService examService;

    @PostMapping
    @HasPermission(code = "sys:exam:publish", name = "发布考试")
    public ResponseEntity<ApiResponse<List<ExamAdminRes>>> publish(@Valid @RequestBody ExamPublishReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(examService.publish(req)));
    }

    @GetMapping
    @HasPermission(code = "sys:exam:page", name = "查询考试")
    public ApiResponse<PageResult<ExamAdminRes>> page(@Valid @ParameterObject ExamPageReq req) {
        return ApiResponse.success(examService.page(req));
    }

    @GetMapping("/{id}")
    @HasPermission(code = "sys:exam:detail", name = "查看考试")
    public ApiResponse<ExamAdminRes> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(examService.detail(id));
    }

    @GetMapping("/{id}/records")
    @HasPermission(code = "sys:exam:record:page", name = "查询考生记录")
    public ApiResponse<PageResult<ExamRecordRes>> records(@PathVariable @Positive Long id,
                                                          @Valid @ParameterObject ExamRecordPageReq req) {
        return ApiResponse.success(examService.records(id, req));
    }

    @PatchMapping("/{id}/answer-visibility")
    @HasPermission(code = "sys:exam:answer:visibility", name = "公布考试结果")
    public ApiResponse<ExamAdminRes> visibility(@PathVariable @Positive Long id,
                                                @Valid @RequestBody ExamVisibilityReq req) {
        return ApiResponse.success(examService.updateVisibility(id, req));
    }
}
