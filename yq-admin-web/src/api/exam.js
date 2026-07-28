import request from '@/utils/request'

export const pageExamQuestions = params => request({ url: '/emp/exam/questions', method: 'get', params })
export const getExamQuestion = id => request({ url: `/emp/exam/questions/${id}`, method: 'get' })
export const createExamQuestion = data => request({ url: '/emp/exam/questions', method: 'post', data })
export const updateExamQuestion = (id, data) => request({ url: `/emp/exam/questions/${id}`, method: 'put', data })
export const updateExamQuestionStatus = (id, status) => request({ url: `/emp/exam/questions/${id}/status`, method: 'patch', data: { status } })

export const pageExamPapers = params => request({ url: '/emp/exam/papers', method: 'get', params })
export const getExamPaper = id => request({ url: `/emp/exam/papers/${id}`, method: 'get' })
export const createExamPaper = data => request({ url: '/emp/exam/papers', method: 'post', data })
export const updateExamPaper = (id, data) => request({ url: `/emp/exam/papers/${id}`, method: 'put', data })

export const publishExam = data => request({ url: '/emp/exams', method: 'post', data })
export const pageExams = params => request({ url: '/emp/exams', method: 'get', params })
export const getExam = id => request({ url: `/emp/exams/${id}`, method: 'get' })
export const pageExamRecords = (id, params) => request({ url: `/emp/exams/${id}/records`, method: 'get', params })
export const updateExamVisibility = (id, answerVisible) => request({ url: `/emp/exams/${id}/answer-visibility`, method: 'patch', data: { answerVisible } })
export const getExamGrading = recordId => request({ url: `/emp/exam-records/${recordId}/grading`, method: 'get' })
export const saveExamGrading = (recordId, data) => request({ url: `/emp/exam-records/${recordId}/grading`, method: 'put', data })
