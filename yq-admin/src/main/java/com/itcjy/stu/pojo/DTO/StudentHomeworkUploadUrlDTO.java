package com.itcjy.stu.pojo.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "学生作业附件预签名上传请求")
public record StudentHomeworkUploadUrlDTO(
        @Schema(description = "原始文件名")
        @NotBlank(message = "文件名不能为空")
        @Size(max = 255, message = "文件名长度不能超过255个字符")
        String fileName,

        @Schema(description = "文件 MIME 类型")
        @NotBlank(message = "contentType不能为空")
        @Size(max = 128, message = "contentType长度不能超过128个字符")
        String contentType
) {
}
