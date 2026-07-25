package com.itcjy.emp.controller.homework;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplateCreateReq;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplatePageReq;
import com.itcjy.emp.pojo.req.homework.CourseHomeworkTemplateUpdateReq;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateCourseOptionRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateDownloadRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplatePositionRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplatePreviewRes;
import com.itcjy.emp.pojo.res.homework.CourseHomeworkTemplateRes;
import com.itcjy.emp.service.homework.ICourseHomeworkTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/course-homework-templates")
@Tag(name = "作业标准管理", description = "课程阶段/天次作业标准与OSS文档管理")
public class CourseHomeworkTemplateController {

    private final ICourseHomeworkTemplateService templateService;

    @GetMapping
    @Operation(summary = "分页查询作业标准")
    @HasPermission(code = "sys:homework:template:page", name = "查询作业标准", description = "分页查询课程作业标准")
    public ApiResponse<PageResult<CourseHomeworkTemplateRes>> page(
            @Valid @ParameterObject CourseHomeworkTemplatePageReq req) {
        return ApiResponse.success(templateService.page(req));
    }

    @PostMapping
    @Operation(summary = "新增作业标准")
    @HasPermission(code = "sys:homework:template:create", name = "新增作业标准", description = "绑定课程阶段或天次并保存OSS文档")
    public ResponseEntity<ApiResponse<CourseHomeworkTemplateRes>> create(
            @Valid @RequestBody CourseHomeworkTemplateCreateReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("作业标准保存成功", templateService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改作业标准")
    @HasPermission(code = "sys:homework:template:update", name = "修改作业标准", description = "修改课程作业标准及OSS文档")
    public ApiResponse<CourseHomeworkTemplateRes> update(
            @PathVariable @Positive(message = "作业标准ID必须大于0") Long id,
            @Valid @RequestBody CourseHomeworkTemplateUpdateReq req) {
        return ApiResponse.success("作业标准更新成功", templateService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除作业标准")
    @HasPermission(code = "sys:homework:template:delete", name = "删除作业标准", description = "删除课程作业标准记录")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "作业标准ID必须大于0") Long id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/course-options")
    @Operation(summary = "查询可选课程")
    @HasPermission(code = "sys:homework:template:course-options", name = "查询作业标准课程", description = "查询作业标准可绑定的课程")
    public ApiResponse<List<CourseHomeworkTemplateCourseOptionRes>> listCourseOptions() {
        return ApiResponse.success(templateService.listCourseOptions());
    }

    @GetMapping("/position-options")
    @Operation(summary = "查询课程阶段/天次")
    @HasPermission(code = "sys:homework:template:position-options", name = "查询课程天次", description = "查询指定课程的阶段或天次")
    public ApiResponse<List<CourseHomeworkTemplatePositionRes>> listPositions(
            @RequestParam @Positive(message = "课程ID必须大于0") Long courseId) {
        return ApiResponse.success(templateService.listPositions(courseId));
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "获取作业标准预览地址")
    @HasPermission(code = "sys:homework:template:preview", name = "预览作业标准", description = "生成作业标准OSS临时预览地址")
    public ApiResponse<CourseHomeworkTemplatePreviewRes> preview(
            @PathVariable @Positive(message = "作业标准ID必须大于0") Long id) {
        return ApiResponse.success(templateService.getPreview(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "获取作业标准下载地址")
    @HasPermission(code = "sys:homework:template:download", name = "下载作业标准", description = "生成作业标准OSS临时下载地址")
    public ApiResponse<CourseHomeworkTemplateDownloadRes> download(
            @PathVariable @Positive(message = "作业标准ID必须大于0") Long id) {
        return ApiResponse.success(templateService.getDownload(id));
    }
}
