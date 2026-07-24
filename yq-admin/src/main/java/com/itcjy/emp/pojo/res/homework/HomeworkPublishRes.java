package com.itcjy.emp.pojo.res.homework;

import com.itcjy.emp.pojo.entity.Homework;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "发布作业结果")
public record HomeworkPublishRes(
        @Schema(description = "作业ID") Long id,
        @Schema(description = "作业标题") String title,
        @Schema(description = "班级ID") Long classId,
        @Schema(description = "作业日期") LocalDate homeworkDate
) {
    public static HomeworkPublishRes from(Homework homework) {
        return new HomeworkPublishRes(
                homework.getId(),
                homework.getTitle(),
                homework.getClassId(),
                homework.getHomeworkDate()
        );
    }
}
