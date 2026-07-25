package com.itcjy.emp.pojo.res.homework;

public record CourseHomeworkTemplateDownloadRes(
        Long templateId,
        String fileName,
        String downloadUrl,
        Integer expireSeconds
) {
}
