package com.itcjy.emp.pojo.res.exam;

import com.itcjy.emp.pojo.entity.exam.Exam;
import java.time.LocalDateTime;

public record ExamAdminRes(
        Long id, Long paperId, String paperName, Long classId, String className,
        LocalDateTime startTime, LocalDateTime entryDeadlineTime, Integer durationMinutes,
        LocalDateTime closeTime, Long invigilatorUserId, Boolean answerVisible,
        String lifecycle, long notStartedCount, long inProgressCount, long submittedCount,
        long timeoutCount, long absentCount, long pendingGradingCount, long completedGradingCount) {

    public static String lifecycle(Exam exam, LocalDateTime now) {
        if (now.isBefore(exam.getStartTime())) return "SCHEDULED";
        if (now.isAfter(exam.getCloseTime())) return "CLOSED";
        return "OPEN";
    }
}
