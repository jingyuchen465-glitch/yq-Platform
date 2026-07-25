package com.itcjy.emp.pojo.res.homework;

public record CourseHomeworkTemplatePositionRes(
        Long courseDetailId,
        String stageName,
        Integer dayNumber,
        String classContent,
        String label
) {
}
