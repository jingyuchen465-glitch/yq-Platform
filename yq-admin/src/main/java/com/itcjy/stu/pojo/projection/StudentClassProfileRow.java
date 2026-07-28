package com.itcjy.stu.pojo.projection;

import lombok.Data;

@Data
public class StudentClassProfileRow {

    private Long classId;

    private String className;

    private Long campusId;

    private String campusLocation;

    private Long courseId;

    private String courseName;

    private Long headTeacherId;

    private String headTeacherName;

    private String headTeacherPhone;

    private String headTeacherEmail;
}
