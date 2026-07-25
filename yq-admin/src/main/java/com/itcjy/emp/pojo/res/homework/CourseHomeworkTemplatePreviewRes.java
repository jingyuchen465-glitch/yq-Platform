package com.itcjy.emp.pojo.res.homework;

public record CourseHomeworkTemplatePreviewRes(
        Long templateId,
        String fileName,
        String previewUrl,
        Integer expireSeconds,
        String stageName,
        Integer dayNumber
) {
}
