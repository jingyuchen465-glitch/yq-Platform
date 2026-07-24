package com.itcjy.emp.pojo.req.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "获取预签名上传URL请求参数")
public class OssUploadUrlReq {

    @Schema(description = "原始文件名", example = "课程表.png")
    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255个字符")
    private String fileName;

    @Schema(description = "文件MIME类型", example = "image/png")
    @NotBlank(message = "contentType不能为空")
    private String contentType;

    @Schema(description = "业务类型，用于区分存储目录", example = "courseware")
    @NotBlank(message = "业务类型不能为空")
    @Size(max = 32, message = "业务类型长度不能超过32个字符")
    private String bizType;
}
