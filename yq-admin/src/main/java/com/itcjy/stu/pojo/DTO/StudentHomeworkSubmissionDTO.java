package com.itcjy.stu.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "学生作业提交请求")
public record StudentHomeworkSubmissionDTO(
        @Schema(description = "已直传到 OSS 的对象 Key")
        @NotBlank(message = "文件objectKey不能为空")
        @Size(max = 500, message = "文件objectKey长度不能超过500个字符")
        @Pattern(regexp = "^homework-submission/[A-Za-z0-9_./-]+$", message = "文件objectKey不合法")
        String contentObjectKey,

        @Schema(description = "学生选择的原始文件名")
        @NotBlank(message = "文件名不能为空")
        @Size(max = 255, message = "文件名长度不能超过255个字符")
        String contentFileName
) {
}
