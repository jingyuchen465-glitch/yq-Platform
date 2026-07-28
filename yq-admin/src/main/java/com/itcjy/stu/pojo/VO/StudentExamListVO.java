package com.itcjy.stu.pojo.VO;

import com.itcjy.emp.pojo.entity.exam.Exam;
import com.itcjy.emp.pojo.entity.exam.ExamPaper;
import com.itcjy.emp.pojo.entity.exam.StudentExamRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentExamListVO(
        Long examId, Long recordId, String paperName, BigDecimal totalScore,
        LocalDateTime startTime, LocalDateTime entryDeadlineTime, Integer durationMinutes,
        LocalDateTime closeTime, String status, String gradingStatus,
        LocalDateTime deadlineTime, Boolean resultVisible) {
    public static StudentExamListVO from(Exam exam, ExamPaper paper, StudentExamRecord record) {
        return new StudentExamListVO(exam.getId(), record.getId(), paper.getPaperName(), paper.getTotalScore(),
                exam.getStartTime(), exam.getEntryDeadlineTime(), exam.getDurationMinutes(), exam.getCloseTime(),
                record.getStatus(), record.getGradingStatus(), record.getDeadlineTime(),
                !LocalDateTime.now().isBefore(exam.getCloseTime())
                        && Boolean.TRUE.equals(exam.getAnswerVisible())
                        && "COMPLETED".equals(record.getGradingStatus()));
    }
}
