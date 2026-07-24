package com.itcjy.emp.pojo.req.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "获取预签名下载URL请求参数")
public class OssDownloadUrlReq {

    @Schema(description = "OSS中的文件路径(objectKey)", example = "courseware/2026/07/24/uuid.png")
    @NotBlank(message = "objectKey不能为空")
    private String objectKey;
}
