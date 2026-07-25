package com.itcjy.emp.pojo.res.homework;

public record CourseHomeworkTemplateMatchRes(
        Long templateId,
        String fileName,
        String stageName,
        Integer dayNumber
) {
}
