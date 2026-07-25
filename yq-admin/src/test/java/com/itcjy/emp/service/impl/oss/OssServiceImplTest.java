package com.itcjy.emp.service.impl.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.itcjy.common.properties.OssProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OssServiceImplTest {

    @Mock
    private OSS ossClient;

    private OssServiceImpl service;

    @BeforeEach
    void setUp() {
        OssProperties properties = new OssProperties();
        properties.setBucketName("test-bucket");
        properties.setDownloadExpireSeconds(3600);
        service = new OssServiceImpl(ossClient, properties);
    }

    @Test
    @DisplayName("预览签名只覆盖 inline，不覆盖 OSS 对象的 Content-Type")
    void shouldNotOverrideContentTypeForPreview() throws MalformedURLException {
        when(ossClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(new URL("https://oss.example/preview"));

        service.generateDownloadUrl("homework-answer/answer.md", true, "标准答案.md");

        ArgumentCaptor<GeneratePresignedUrlRequest> captor =
                ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(ossClient).generatePresignedUrl(captor.capture());
        GeneratePresignedUrlRequest request = captor.getValue();
        assertThat(request.getResponseHeaders().getContentDisposition()).startsWith("inline;");
        assertThat(request.getResponseHeaders().getContentType()).isNull();
    }

    @Test
    @DisplayName("下载签名使用 attachment")
    void shouldUseAttachmentForDownload() throws MalformedURLException {
        when(ossClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(new URL("https://oss.example/download"));

        service.generateDownloadUrl("homework-answer/answer.md", false, "标准答案.md");

        ArgumentCaptor<GeneratePresignedUrlRequest> captor =
                ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(ossClient).generatePresignedUrl(captor.capture());
        assertThat(captor.getValue().getResponseHeaders().getContentDisposition())
                .startsWith("attachment;");
    }
}
