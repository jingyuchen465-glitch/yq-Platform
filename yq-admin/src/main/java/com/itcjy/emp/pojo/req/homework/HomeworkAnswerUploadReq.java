package com.itcjy.emp.pojo.req.homework;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "保存作业标准答案请求")
public record HomeworkAnswerUploadReq(
        @Schema(description = "标准答案对象存储Key")
        @NotBlank(message = "标准答案objectKey不能为空")
        @Size(max = 500, message = "标准答案objectKey长度不能超过500个字符")
        @Pattern(regexp = "(?i)^.+\\.(md|markdown|pdf|doc|docx|txt)$", message = "标准答案文件格式不支持")
        String answerObjectKey,

        @Schema(description = "标准答案文件名")
        @NotBlank(message = "标准答案文件名不能为空")
        @Size(max = 255, message = "标准答案文件名长度不能超过255个字符")
        @Pattern(regexp = "(?i)^.+\\.(md|markdown|pdf|doc|docx|txt)$", message = "标准答案文件格式不支持")
        String answerFileName
) {
}
