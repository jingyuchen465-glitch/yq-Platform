import request from '@/utils/request'

export const listStudentExams = () => request({ url: '/stu/exams', method: 'get' })
export const startStudentExam = examId => request({ url: `/stu/exams/${examId}/start`, method: 'post' })
export const getStudentExamRecord = recordId => request({ url: `/stu/exam-records/${recordId}`, method: 'get' })
export const saveStudentExamAnswer = (recordId, paperQuestionId, answerContent) => request({
  url: `/stu/exam-records/${recordId}/answers/${paperQuestionId}`,
  method: 'put',
  data: { answerContent }
})
export const submitStudentExam = recordId => request({ url: `/stu/exam-records/${recordId}/submit`, method: 'post' })
export const getStudentExamResult = recordId => request({ url: `/stu/exam-records/${recordId}/result`, method: 'get' })
