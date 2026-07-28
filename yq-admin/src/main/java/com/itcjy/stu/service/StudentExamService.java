package com.itcjy.stu.service;

import com.itcjy.stu.pojo.DTO.StudentExamAnswerDTO;
import com.itcjy.stu.pojo.VO.*;
import java.util.List;

public interface StudentExamService {
    List<StudentExamListVO> listCurrentStudentExams();
    StudentExamVO start(Long examId);
    StudentExamVO detail(Long recordId);
    void saveAnswer(Long recordId, Long paperQuestionId, StudentExamAnswerDTO dto);
    StudentExamVO submit(Long recordId);
    StudentExamResultVO result(Long recordId);
}
