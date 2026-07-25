package com.itcjy.emp.pojo.res.homework;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "发布作业预填信息")
public record HomeworkPrefillRes(
        @Schema(description = "班级ID") Long classId,
        @Schema(description = "班级名称") String className,
        @Schema(description = "归一后的作业日期") LocalDate homeworkDate,
        @Schema(description = "默认标题") String title,
        @Schema(description = "当天课程内容") String classContent,
        @Schema(description = "默认开始时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @Schema(description = "默认截止时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime deadline,
        @Schema(description = "当天课程匹配的已生效作业标准，未配置时为null")
        CourseHomeworkTemplateMatchRes homeworkTemplate
) {
}
