package com.itcjy.stu.pojo.VO;

import com.itcjy.emp.pojo.entity.Homework;
import com.itcjy.emp.pojo.entity.HomeworkSubmission;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "学生端作业列表项")
public record StudentHomeworkVO(
        @Schema(description = "作业ID") Long id,
        @Schema(description = "作业标题") String title,
        @Schema(description = "作业发布日期") LocalDate homeworkDate,
        @Schema(description = "课程内容") String classContent,
        @Schema(description = "作业附件文件名") String contentFileName,
        @Schema(description = "开始时间") LocalDateTime startTime,
        @Schema(description = "截止时间") LocalDateTime deadline,
        @Schema(description = "作业备注") String remark,
        @Schema(description = "当前学生的提交记录ID") Long submissionId,
        @Schema(description = "当前学生提交的文件名") String submissionFileName,
        @Schema(description = "提交时间") LocalDateTime submitTime,
        @Schema(description = "是否逾期提交") Boolean lateSubmitted,
        @Schema(description = "作业得分") Integer score,
        @Schema(description = "教师评语") String teacherRemark
) {

    public static StudentHomeworkVO from(Homework homework, HomeworkSubmission submission) {
        return new StudentHomeworkVO(
                homework.getId(),
                homework.getTitle(),
                homework.getHomeworkDate(),
                homework.getClassContent(),
                homework.getContentFileName(),
                homework.getStartTime(),
                homework.getDeadline(),
                homework.getRemark(),
                submission == null ? null : submission.getId(),
                submission == null ? null : submission.getContentFileName(),
                submission == null ? null : submission.getSubmitTime(),
                submission == null ? null : submission.getLateSubmitted(),
                submission == null ? null : submission.getScore(),
                submission == null ? null : submission.getTeacherRemark()
        );
    }
}
