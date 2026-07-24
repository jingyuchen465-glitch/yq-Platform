package com.itcjy.emp.pojo.res.oss;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预签名下载URL响应")
public record OssDownloadUrlRes(
        @Schema(description = "预签名下载URL，浏览器可直接访问") String downloadUrl,
        @Schema(description = "URL有效期（秒）") Integer expireSeconds
) {
}
