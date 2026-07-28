package com.itcjy.emp.controller.exam;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.*;
import com.itcjy.emp.pojo.req.exam.*;
import com.itcjy.emp.pojo.res.exam.ExamPaperRes;
import com.itcjy.emp.service.exam.IExamPaperService;
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
@RequestMapping("/emp/exam/papers")
public class ExamPaperController {
    private final IExamPaperService paperService;

    @GetMapping
    @HasPermission(code = "sys:exam:paper:page", name = "查询试卷")
    public ApiResponse<PageResult<ExamPaperRes>> page(@Valid @ParameterObject ExamPaperPageReq req) {
        return ApiResponse.success(paperService.page(req));
    }

    @GetMapping("/{id}")
    @HasPermission(code = "sys:exam:paper:detail", name = "查看试卷")
    public ApiResponse<ExamPaperRes> detail(@PathVariable @Positive Long id) {
        return ApiResponse.success(paperService.detail(id));
    }

    @PostMapping
    @HasPermission(code = "sys:exam:paper:create", name = "新增试卷")
    public ResponseEntity<ApiResponse<ExamPaperRes>> create(@Valid @RequestBody ExamPaperSaveReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(paperService.create(req)));
    }

    @PutMapping("/{id}")
    @HasPermission(code = "sys:exam:paper:update", name = "编辑试卷")
    public ApiResponse<ExamPaperRes> update(@PathVariable @Positive Long id,
                                            @Valid @RequestBody ExamPaperSaveReq req) {
        return ApiResponse.success(paperService.update(id, req));
    }
}
