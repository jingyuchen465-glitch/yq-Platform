import request from '@/utils/request'

/**
 * OSS文件服务接口 - 对应后端 OssController
 * 基础路径: /emp/oss
 */

// 获取预签名上传URL
export function getUploadUrl(data) {
  return request({
    url: '/emp/oss/upload-url',
    method: 'post',
    data
  })
}

// 获取预签名下载URL
export function getDownloadUrl(data) {
  return request({
    url: '/emp/oss/download-url',
    method: 'post',
    data
  })
}
