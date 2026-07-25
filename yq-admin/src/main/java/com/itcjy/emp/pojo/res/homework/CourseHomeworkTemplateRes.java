package com.itcjy.emp.pojo.res.homework;

import com.itcjy.emp.pojo.entity.CourseHomeworkTemplate;

import java.time.LocalDateTime;

public record CourseHomeworkTemplateRes(
        Long id,
        Long courseId,
        String courseName,
        String teachingMode,
        String stageName,
        Integer dayNumber,
        String contentObjectKey,
        String contentFileName,
        String status,
        String remark,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CourseHomeworkTemplateRes from(CourseHomeworkTemplate entity, String courseName) {
        return new CourseHomeworkTemplateRes(
                entity.getId(), entity.getCourseId(), courseName, entity.getTeachingMode(),
                entity.getStageName(), entity.getDayNumber(), entity.getContentObjectKey(),
                entity.getContentFileName(), entity.getStatus(), entity.getRemark(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
