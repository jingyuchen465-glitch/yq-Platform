package com.itcjy.emp.pojo.res.exam;

import com.itcjy.emp.pojo.entity.exam.StudentExamRecord;
import com.itcjy.stu.pojo.entity.Student;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamRecordRes(
        Long id, Long examId, Long studentId, String studentName,
        String status, String gradingStatus, LocalDateTime startTime,
        LocalDateTime deadlineTime, LocalDateTime submitTime,
        BigDecimal objectiveScore, BigDecimal subjectiveScore, BigDecimal score) {
    public static ExamRecordRes from(StudentExamRecord record, Student student) {
        return new ExamRecordRes(record.getId(), record.getExamId(), record.getStudentId(),
                student == null ? "未知学生" : student.getName(), record.getStatus(),
                record.getGradingStatus(), record.getStartTime(), record.getDeadlineTime(),
                record.getSubmitTime(), record.getObjectiveScore(), record.getSubjectiveScore(), record.getScore());
    }
}
