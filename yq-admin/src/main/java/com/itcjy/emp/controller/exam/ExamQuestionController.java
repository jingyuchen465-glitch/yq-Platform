package com.itcjy.emp.controller.exam;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.*;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.ExamQuestionRes;
import com.itcjy.emp.service.exam.IExamQuestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/exam/questions")
public class ExamQuestionController {
    private final IExamQuestionService questionService;

    @GetMapping
    @HasPermission(code = "sys:exam:question:page", name = "查询题库")
    public ApiResponse<PageResult<ExamQuestionRes>> page(@Valid @ParameterObject ExamQuestionPageReq req) {
        return ApiResponse.success(questionService.page(req));
    }

    @GetMapping("/{id}")
    @HasPermission(code = "sys:exam:question:detail", name = "查看题目")
    public ApiResponse<ExamQuestionRes> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(questionService.detail(id));
    }

    @PostMapping
    @HasPermission(code = "sys:exam:question:create", name = "新增题目")
    public ResponseEntity<ApiResponse<ExamQuestionRes>> create(@Valid @RequestBody ExamQuestionSaveReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(questionService.create(req)));
    }

    @PutMapping("/{id}")
    @HasPermission(code = "sys:exam:question:update", name = "编辑题目")
    public ApiResponse<ExamQuestionRes> update(@PathVariable @Positive Long id,
                                               @Valid @RequestBody ExamQuestionSaveReq req) {
        return ApiResponse.success(questionService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    @HasPermission(code = "sys:exam:question:status", name = "设置题目状态")
    public ApiResponse<ExamQuestionRes> status(@PathVariable @Positive Long id,
                                               @Valid @RequestBody ExamQuestionStatusReq req) {
        return ApiResponse.success(questionService.updateStatus(id, req));
    }
}
