package com.itcjy.emp.service.oss;

import com.itcjy.emp.pojo.req.oss.OssDownloadUrlReq;
import com.itcjy.emp.pojo.req.oss.OssUploadUrlReq;
import com.itcjy.emp.pojo.res.oss.OssDownloadUrlRes;
import com.itcjy.emp.pojo.res.oss.OssUploadUrlRes;

public interface IOssService {

    /**
     * 生成预签名上传 URL
     *
     * @param req 包含文件名、MIME类型、业务类型
     * @return 预签名上传URL及objectKey
     */
    OssUploadUrlRes generateUploadUrl(OssUploadUrlReq req);

    /**
     * 生成预签名下载 URL
     *
     * @param req 包含objectKey
     * @return 预签名下载URL
     */
    OssDownloadUrlRes generateDownloadUrl(OssDownloadUrlReq req);

    OssDownloadUrlRes generateDownloadUrl(String objectKey, boolean preview);

    OssDownloadUrlRes generateDownloadUrl(String objectKey, boolean preview, String fileName);
}
