package com.itcjy.emp.pojo.projection;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TeacherScheduleRow {
    private Long scheduleId;
    private LocalDate scheduleDate;
    private Long classId;
    private String className;
    private Long courseDetailId;
    private String stageName;
    private String courseContent;
}
