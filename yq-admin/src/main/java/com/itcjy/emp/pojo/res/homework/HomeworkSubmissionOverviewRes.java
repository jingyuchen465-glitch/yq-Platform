package com.itcjy.emp.pojo.res.homework;

import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.Homework;
import com.itcjy.emp.pojo.entity.SysClass;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "作业提交与批改概览")
public record HomeworkSubmissionOverviewRes(
        Long homeworkId,
        String homeworkTitle,
        Long classId,
        String className,
        LocalDate homeworkDate,
        LocalDateTime deadline,
        long submissionCount,
        long reviewedCount,
        long lateCount,
        PageResult<HomeworkSubmissionItemRes> submissions
) {
    public static HomeworkSubmissionOverviewRes from(
            Homework homework,
            SysClass sysClass,
            long submissionCount,
            long reviewedCount,
            long lateCount,
            PageResult<HomeworkSubmissionItemRes> submissions) {
        return new HomeworkSubmissionOverviewRes(
                homework.getId(),
                homework.getTitle(),
                homework.getClassId(),
                sysClass == null ? "班级 #" + homework.getClassId() : sysClass.getClassPeriod(),
                homework.getHomeworkDate(),
                homework.getDeadline(),
                submissionCount,
                reviewedCount,
                lateCount,
                submissions
        );
    }
}
