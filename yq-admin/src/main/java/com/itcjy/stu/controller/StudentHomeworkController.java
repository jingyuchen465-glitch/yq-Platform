package com.itcjy.stu.controller;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.pojo.res.oss.OssUploadUrlRes;
import com.itcjy.stu.pojo.DTO.StudentHomeworkSubmissionDTO;
import com.itcjy.stu.pojo.DTO.StudentHomeworkUploadUrlDTO;
import com.itcjy.stu.pojo.VO.StudentHomeworkVO;
import com.itcjy.stu.service.StudentHomeworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/stu/homeworks")
@Tag(name = "学生端作业", description = "当前登录学生的作业查询接口")
public class StudentHomeworkController {

    private final StudentHomeworkService studentHomeworkService;

    @GetMapping
    @Operation(summary = "查询我的作业", description = "仅返回当前学生班级的作业及本人的提交状态")
    public ApiResponse<List<StudentHomeworkVO>> listCurrentStudentHomeworks() {
        return ApiResponse.success(studentHomeworkService.listCurrentStudentHomeworks());
    }

    @PostMapping("/{homeworkId}/submissions/upload-url")
    @Operation(summary = "获取学生作业预签名上传地址", description = "前端使用返回地址将文件直传 OSS")
    public ApiResponse<OssUploadUrlRes> getSubmissionUploadUrl(
            @PathVariable @Positive(message = "作业ID必须大于0") Long homeworkId,
            @Valid @RequestBody StudentHomeworkUploadUrlDTO dto) {
        return ApiResponse.success(studentHomeworkService.generateSubmissionUploadUrl(homeworkId, dto));
    }

    @GetMapping("/{homeworkId}/download-url")
    @Operation(summary = "获取老师附件预签名地址", description = "preview=true 为浏览器预览，false 为强制下载")
    public ApiResponse<OssDownloadUrlRes> getHomeworkFileUrl(
            @PathVariable @Positive(message = "作业ID必须大于0") Long homeworkId,
            @RequestParam(defaultValue = "true") boolean preview) {
        return ApiResponse.success(studentHomeworkService.generateHomeworkFileUrl(homeworkId, preview));
    }

    @PostMapping("/{homeworkId}/submissions")
    @Operation(summary = "提交学生作业", description = "保存已上传到 OSS 的文件对象信息")
    public ApiResponse<Void> submitHomework(
            @PathVariable @Positive(message = "作业ID必须大于0") Long homeworkId,
            @Valid @RequestBody StudentHomeworkSubmissionDTO dto) {
        studentHomeworkService.submitHomework(homeworkId, dto);
        return ApiResponse.success("作业提交成功");
    }
}
