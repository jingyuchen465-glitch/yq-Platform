package com.itcjy.emp.controller.homework;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.homework.HomeworkSubmissionGradeReq;
import com.itcjy.emp.pojo.req.homework.HomeworkSubmissionPageReq;
import com.itcjy.emp.pojo.res.homework.HomeworkSubmissionItemRes;
import com.itcjy.emp.pojo.res.homework.HomeworkSubmissionOverviewRes;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.service.homework.IHomeworkSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp")
@Tag(name = "作业提交批改", description = "教师查询、预览并批改学生作业")
public class HomeworkSubmissionController {

    private final IHomeworkSubmissionService homeworkSubmissionService;

    @GetMapping("/homeworks/{homeworkId}/submissions")
    @Operation(summary = "分页查询指定作业的提交情况")
    @HasPermission(code = "sys:homework:submission:page", name = "查询作业提交", description = "分页查询指定作业的学生提交情况")
    public ApiResponse<HomeworkSubmissionOverviewRes> pageByHomework(
            @PathVariable @Positive(message = "作业ID必须大于0") Long homeworkId,
            @Valid @ParameterObject HomeworkSubmissionPageReq req) {
        return ApiResponse.success(homeworkSubmissionService.pageByHomework(homeworkId, req));
    }

    @GetMapping("/homework-submissions/{submissionId}/download-url")
    @Operation(summary = "获取学生提交文件的预签名地址", description = "preview=true 时浏览器内联展示，false 时强制下载")
    @HasPermission(code = "sys:homework:submission:download", name = "预览学生作业", description = "获取学生提交文件的下载预签名地址")
    public ApiResponse<OssDownloadUrlRes> getDownloadUrl(
            @PathVariable @Positive(message = "提交ID必须大于0") Long submissionId,
            @RequestParam(defaultValue = "true") boolean preview) {
        return ApiResponse.success(homeworkSubmissionService.generateDownloadUrl(submissionId, preview));
    }

    @PatchMapping("/homework-submissions/{submissionId}/grading")
    @Operation(summary = "保存学生作业分数和教师评语", description = "仅更新score和teacher_remark")
    @HasPermission(code = "sys:homework:submission:grade", name = "批改学生作业", description = "仅更新学生作业的分数和教师评语")
    public ApiResponse<HomeworkSubmissionItemRes> grade(
            @PathVariable @Positive(message = "提交ID必须大于0") Long submissionId,
            @Valid @RequestBody HomeworkSubmissionGradeReq req) {
        return ApiResponse.success("批改已保存", homeworkSubmissionService.grade(submissionId, req));
    }
}
