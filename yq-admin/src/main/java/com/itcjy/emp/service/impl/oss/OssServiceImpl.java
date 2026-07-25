package com.itcjy.emp.service.impl.oss;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.itcjy.common.properties.OssProperties;
import com.itcjy.emp.pojo.req.oss.OssDownloadUrlReq;
import com.itcjy.emp.pojo.req.oss.OssUploadUrlReq;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.pojo.res.oss.OssUploadUrlRes;
import com.itcjy.emp.service.oss.IOssService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssServiceImpl implements IOssService {

    private final OSS ossClient;
    private final OssProperties ossProperties;

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Override
    public OssUploadUrlRes generateUploadUrl(OssUploadUrlReq req) {
        // 生成 objectKey: bizType/yyyy/MM/dd/uuid.ext
        String extension = extractExtension(req.getFileName());
        String objectKey = req.getBizType() + "/"
                + LocalDate.now().format(DATE_PATH) + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + (extension.isEmpty() ? "" : "." + extension);

        // 设置过期时间
        Date expiration = new Date(System.currentTimeMillis() + ossProperties.getUploadExpireSeconds() * 1000L);

        // 构建预签名请求，指定 PUT 方法和 Content-Type
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossProperties.getBucketName(), objectKey, HttpMethod.PUT);
        request.setExpiration(expiration);
        request.setContentType(req.getContentType());

        URL url = ossClient.generatePresignedUrl(request);
        log.info("生成预签名上传URL, objectKey={}, expireSeconds={}", objectKey, ossProperties.getUploadExpireSeconds());

        return new OssUploadUrlRes(url.toString(), objectKey, ossProperties.getUploadExpireSeconds());
    }

    @Override
    public OssDownloadUrlRes generateDownloadUrl(OssDownloadUrlReq req) {
        Date expiration = new Date(System.currentTimeMillis() + ossProperties.getDownloadExpireSeconds() * 1000L);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossProperties.getBucketName(), req.getObjectKey(), HttpMethod.GET);
        request.setExpiration(expiration);

        // 根据 preview 参数设置 Content-Disposition 响应头
        ResponseHeaderOverrides headers = new ResponseHeaderOverrides();
        if (Boolean.TRUE.equals(req.getPreview())) {
            headers.setContentDisposition("inline");
        } else {
            String fileName = extractFileName(req.getObjectKey());
            headers.setContentDisposition("attachment; filename=\"" + fileName + "\"");
        }
        request.setResponseHeaders(headers);

        URL url = ossClient.generatePresignedUrl(request);
        log.info("生成预签名下载URL, objectKey={}, preview={}, expireSeconds={}",
                req.getObjectKey(), req.getPreview(), ossProperties.getDownloadExpireSeconds());

        return new OssDownloadUrlRes(url.toString(), ossProperties.getDownloadExpireSeconds());
    }

    /**
     * 从 objectKey 中提取文件名（最后一段路径）
     */
    private String extractFileName(String objectKey) {
        if (objectKey == null || !objectKey.contains("/")) {
            return objectKey == null ? "download" : objectKey;
        }
        return objectKey.substring(objectKey.lastIndexOf('/') + 1);
    }

    /**
     * 从文件名中提取扩展名（不含点号）
     */
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
