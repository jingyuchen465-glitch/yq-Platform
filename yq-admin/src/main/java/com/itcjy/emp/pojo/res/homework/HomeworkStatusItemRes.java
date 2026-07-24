package com.itcjy.emp.pojo.res.homework;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itcjy.emp.pojo.entity.Homework;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "班级作业发布明细")
public record HomeworkStatusItemRes(
        @Schema(description = "作业ID") Long id,
        @Schema(description = "作业标题") String title,
        @Schema(description = "作业日期") LocalDate homeworkDate,
        @Schema(description = "课程内容") String classContent,
        @Schema(description = "开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @Schema(description = "截止时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime deadline,
        @Schema(description = "作业文件名") String contentFileName,
        @Schema(description = "标准答案对象存储Key") String answerObjectKey,
        @Schema(description = "标准答案文件名") String answerFileName,
        @Schema(description = "标准答案学生是否可见") Boolean answerStudentVisible,
        @Schema(description = "发布时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt
) {
    public static HomeworkStatusItemRes from(Homework homework) {
        return new HomeworkStatusItemRes(
                homework.getId(),
                homework.getTitle(),
                homework.getHomeworkDate(),
                homework.getClassContent(),
                homework.getStartTime(),
                homework.getDeadline(),
                homework.getContentFileName(),
                homework.getAnswerObjectKey(),
                homework.getAnswerFileName(),
                Boolean.TRUE.equals(homework.getAnswerStudentVisible()),
                homework.getCreatedAt()
        );
    }
}
