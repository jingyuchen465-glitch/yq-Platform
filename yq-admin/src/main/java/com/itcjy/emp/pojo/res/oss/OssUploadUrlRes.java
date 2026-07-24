package com.itcjy.emp.pojo.res.oss;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预签名上传URL响应")
public record OssUploadUrlRes(
        @Schema(description = "预签名上传URL，前端用此URL直传文件到OSS") String uploadUrl,
        @Schema(description = "文件在OSS中的存储路径") String objectKey,
        @Schema(description = "URL有效期（秒）") Integer expireSeconds
) {
}
