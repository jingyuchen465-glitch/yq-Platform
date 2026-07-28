import request from '@/utils/request'

const MIME_BY_EXTENSION = {
  md: 'text/markdown; charset=UTF-8', markdown: 'text/markdown; charset=UTF-8',
  txt: 'text/plain; charset=UTF-8', pdf: 'application/pdf',
  png: 'image/png', jpg: 'image/jpeg', jpeg: 'image/jpeg',
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
}

function resolveContentType(file) {
  if (file.type) return file.type
  const extension = (file.name.split('.').pop() || '').toLowerCase()
  return MIME_BY_EXTENSION[extension] || 'application/octet-stream'
}

export function getStudentSchedule(params) {
  return request({ url: '/stu/schedules', method: 'get', params })
}

export function getStudentHomeworks(params) {
  return request({ url: '/stu/homeworks', method: 'get', params })
}

export function getHomeworkFileUrl(homeworkId, preview = true) {
  return request({
    url: `/stu/homeworks/${homeworkId}/download-url`,
    method: 'get',
    params: { preview }
  })
}

export async function uploadStudentHomeworkFile(homeworkId, file) {
  const contentType = resolveContentType(file)
  const signed = await request({
    url: `/stu/homeworks/${homeworkId}/submissions/upload-url`,
    method: 'post',
    data: { fileName: file.name, contentType }
  })
  const { uploadUrl, objectKey } = signed.data || {}
  if (!uploadUrl || !objectKey) throw new Error('未获取到文件上传地址')
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': contentType },
    body: file
  })
  if (!response.ok) throw new Error(`文件上传失败: ${response.status}`)
  return { contentObjectKey: objectKey, contentFileName: file.name }
}

export function submitStudentHomework(homeworkId, submission) {
  return request({
    url: `/stu/homeworks/${homeworkId}/submissions`,
    method: 'post',
    data: submission,
    timeout: 60000
  })
}
