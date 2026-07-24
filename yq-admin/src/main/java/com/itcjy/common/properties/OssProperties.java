package com.itcjy.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.oss")
public class OssProperties {

    /**
     * OSS 服务端点，如 oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 预签名上传 URL 有效期（秒），默认 5 分钟
     */
    private int uploadExpireSeconds = 300;

    /**
     * 预签名下载 URL 有效期（秒），默认 1 小时
     */
    private int downloadExpireSeconds = 3600;
}
