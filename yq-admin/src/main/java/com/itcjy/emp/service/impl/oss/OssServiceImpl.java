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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
        return generateDownloadUrl(req.getObjectKey(), Boolean.TRUE.equals(req.getPreview()));
    }

    @Override
    public OssDownloadUrlRes generateDownloadUrl(String objectKey, boolean preview) {
        return generateDownloadUrl(objectKey, preview, extractFileName(objectKey));
    }

    @Override
    public OssDownloadUrlRes generateDownloadUrl(String objectKey, boolean preview, String fileName) {
        Date expiration = new Date(System.currentTimeMillis() + ossProperties.getDownloadExpireSeconds() * 1000L);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossProperties.getBucketName(), objectKey, HttpMethod.GET);
        request.setExpiration(expiration);

        // 根据 preview 参数设置 Content-Disposition 响应头
        ResponseHeaderOverrides headers = new ResponseHeaderOverrides();
        String safeFileName = sanitizeFileName(fileName, objectKey);
        headers.setContentDisposition(buildContentDisposition(preview, safeFileName));
        request.setResponseHeaders(headers);

        URL url = ossClient.generatePresignedUrl(request);
        log.info("生成预签名下载URL, objectKey={}, preview={}, expireSeconds={}",
                objectKey, preview, ossProperties.getDownloadExpireSeconds());

        return new OssDownloadUrlRes(url.toString(), ossProperties.getDownloadExpireSeconds());
    }

    private String sanitizeFileName(String fileName, String objectKey) {
        String resolved = fileName == null || fileName.isBlank() ? extractFileName(objectKey) : fileName.trim();
        return resolved.replace("\r", "").replace("\n", "");
    }

    /**
     * 构建 OSS 兼容的 Content-Disposition 值。
     * 阿里云 OSS 不接受 Spring ContentDisposition 生成的 RFC 2047 编码（=?UTF-8?Q?...?=），
     * 需使用 RFC 6266 格式：filename="ascii-fallback"; filename*=UTF-8''percent-encoded
     */
    private String buildContentDisposition(boolean preview, String fileName) {
        String type = preview ? "inline" : "attachment";
        boolean asciiOnly = fileName.chars().allMatch(c -> c >= 0x20 && c <= 0x7E && c != '"' && c != '\\');
        if (asciiOnly) {
            return type + "; filename=\"" + fileName + "\"";
        }
        // 非 ASCII 文件名：提供 ASCII 回退 + RFC 5987 编码
        String asciiFallback = fileName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return type + "; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + encoded;
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
