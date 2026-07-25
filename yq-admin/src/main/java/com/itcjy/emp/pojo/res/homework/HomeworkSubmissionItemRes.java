package com.itcjy.emp.pojo.res.homework;

import com.itcjy.emp.pojo.entity.HomeworkSubmission;
import com.itcjy.emp.pojo.entity.SysUser;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "作业提交明细")
public record HomeworkSubmissionItemRes(
        Long id,
        Long studentId,
        String studentName,
        String studentUsername,
        String contentFileName,
        LocalDateTime submitTime,
        boolean lateSubmitted,
        String teacherRemark,
        Integer score,
        boolean reviewed,
        LocalDateTime updatedAt
) {
    public static HomeworkSubmissionItemRes from(HomeworkSubmission submission, SysUser student) {
        return new HomeworkSubmissionItemRes(
                submission.getId(),
                submission.getStudentId(),
                resolveStudentName(submission.getStudentId(), student),
                student == null ? null : student.getUsername(),
                submission.getContentFileName(),
                submission.getSubmitTime(),
                Boolean.TRUE.equals(submission.getLateSubmitted()),
                submission.getTeacherRemark(),
                submission.getScore(),
                submission.getScore() != null,
                submission.getUpdatedAt()
        );
    }

    private static String resolveStudentName(Long studentId, SysUser student) {
        if (student == null) {
            return "学生 #" + studentId;
        }
        if (student.getRealName() != null && !student.getRealName().isBlank()) {
            return student.getRealName();
        }
        if (student.getNickname() != null && !student.getNickname().isBlank()) {
            return student.getNickname();
        }
        return student.getUsername();
    }
}
