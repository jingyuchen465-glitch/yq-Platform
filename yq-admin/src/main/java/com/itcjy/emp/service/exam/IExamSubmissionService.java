package com.itcjy.emp.service.exam;
import com.itcjy.emp.pojo.entity.exam.StudentExamRecord;
import com.itcjy.emp.pojo.enums.exam.ExamSubmitReason;
public interface IExamSubmissionService {
    StudentExamRecord submit(Long recordId, ExamSubmitReason reason);
}
