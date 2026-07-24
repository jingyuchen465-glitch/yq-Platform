package com.itcjy.emp.controller.oss;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.oss.OssDownloadUrlReq;
import com.itcjy.emp.pojo.req.oss.OssUploadUrlReq;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.pojo.res.oss.OssUploadUrlRes;
import com.itcjy.emp.service.oss.IOssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/oss")
@Tag(name = "OSS文件服务", description = "阿里云OSS预签名上传/下载接口")
public class OssController {

    private final IOssService ossService;

    @PostMapping("/upload-url")
    @Operation(summary = "获取预签名上传URL", description = "前端获取后直传文件到OSS，无需经过后端服务器")
    @HasPermission(code = "sys:oss:upload", name = "OSS上传", description = "获取预签名上传URL")
    public ApiResponse<OssUploadUrlRes> getUploadUrl(@Valid @RequestBody OssUploadUrlReq req) {
        return ApiResponse.success(ossService.generateUploadUrl(req));
    }

    @PostMapping("/download-url")
    @Operation(summary = "获取预签名下载URL", description = "前端获取后可直接下载/预览OSS文件")
    @HasPermission(code = "sys:oss:download", name = "OSS下载", description = "获取预签名下载URL")
    public ApiResponse<OssDownloadUrlRes> getDownloadUrl(@Valid @RequestBody OssDownloadUrlReq req) {
        return ApiResponse.success(ossService.generateDownloadUrl(req));
    }
}
