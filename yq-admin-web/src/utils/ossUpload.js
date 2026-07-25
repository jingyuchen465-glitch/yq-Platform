import { getUploadUrl, getDownloadUrl } from '@/api/oss'

/**
 * OSS 直传工具
 * 注意：直传 OSS 的请求不能走 request.js 拦截器（会附加签名头导致 OSS 拒绝），
 * 因此使用原生 fetch 进行 PUT 上传。
 */

/**
 * 上传文件到 OSS（通过预签名 URL 直传）
 * @param {File} file - 要上传的文件对象
 * @param {string} bizType - 业务类型，用于区分存储目录，如 'avatar'、'courseware'
 * @param {object} [options] - 可选配置
 * @param {function} [options.onProgress] - 上传进度回调 (percent: number) => void
 * @returns {Promise<{objectKey: string, uploadUrl: string}>} 上传成功返回 objectKey
 */
export async function uploadToOss(file, bizType, options = {}) {
  // 1. 调用后端获取预签名上传 URL
  const res = await getUploadUrl({
    fileName: file.name,
    contentType: file.type || 'application/octet-stream',
    bizType
  })

  const { uploadUrl, objectKey } = res.data

  // 2. 使用 fetch 直传文件到 OSS（不走 axios 拦截器）
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': file.type || 'application/octet-stream'
    },
    body: file
  })

  if (!response.ok) {
    throw new Error(`OSS上传失败: ${response.status} ${response.statusText}`)
  }

  return { objectKey, uploadUrl }
}

/**
 * 使用 XMLHttpRequest 上传（支持进度回调）
 * @param {File} file - 要上传的文件对象
 * @param {string} bizType - 业务类型
 * @param {function} [onProgress] - 进度回调 (percent: number) => void
 * @returns {Promise<{objectKey: string}>}
 */
export function uploadToOssWithProgress(file, bizType, onProgress) {
  return new Promise(async (resolve, reject) => {
    try {
      // 1. 获取预签名 URL
      const res = await getUploadUrl({
        fileName: file.name,
        contentType: file.type || 'application/octet-stream',
        bizType
      })

      const { uploadUrl, objectKey } = res.data

      // 2. 使用 XMLHttpRequest 上传以支持进度监听
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', uploadUrl, true)
      xhr.setRequestHeader('Content-Type', file.type || 'application/octet-stream')

      xhr.upload.onprogress = (event) => {
        if (event.lengthComputable && onProgress) {
          const percent = Math.round((event.loaded / event.total) * 100)
          onProgress(percent)
        }
      }

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve({ objectKey })
        } else {
          reject(new Error(`OSS上传失败: ${xhr.status} ${xhr.statusText}`))
        }
      }

      xhr.onerror = () => reject(new Error('OSS上传网络错误'))
      xhr.ontimeout = () => reject(new Error('OSS上传超时'))

      xhr.send(file)
    } catch (err) {
      reject(err)
    }
  })
}

/**
 * 获取文件的预签名下载 URL
 * @param {string} objectKey - 文件在 OSS 中的路径
 * @param {boolean} [preview=false] - 是否为预览模式（true=浏览器内联展示，false=强制下载）
 * @returns {Promise<string>} 预签名下载 URL
 */
export async function getOssDownloadUrl(objectKey, preview = false) {
  const res = await getDownloadUrl({ objectKey, preview })
  return res.data.downloadUrl
}

/**
 * 在新窗口打开文件预览（浏览器内联展示）
 * @param {string} objectKey - 文件在 OSS 中的路径
 */
export async function previewOssFile(objectKey) {
  const url = await getOssDownloadUrl(objectKey, true)
  window.open(url, '_blank')
}

/**
 * 触发文件下载（强制下载）
 * @param {string} objectKey - 文件在 OSS 中的路径
 * @param {string} [fileName] - 下载时的文件名
 */
export async function downloadOssFile(objectKey, fileName) {
  const url = await getOssDownloadUrl(objectKey, false)
  const link = document.createElement('a')
  link.href = url
  if (fileName) {
    link.download = fileName
  }
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
