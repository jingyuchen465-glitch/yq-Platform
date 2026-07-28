package com.itcjy.stu.pojo.VO;

import com.itcjy.stu.pojo.entity.Student;
import com.itcjy.stu.pojo.projection.StudentClassProfileRow;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentDetailsVO {
    @Schema(description = "学生主键", example = "1")
    private Long id;

    @Schema(description = "学生姓名", example = "张三")
    private String name;

    @Schema(description = "登录手机号", example = "13800138000")
    private String phone;

    private String email;


    private Long classId;

    @Schema(description = "班级名称", example = "Java第18期")
    private String className;

    @Schema(description = "校区ID", example = "2")
    private Long campusId;

    @Schema(description = "校区地点", example = "北京市朝阳区")
    private String campusLocation;

    @Schema(description = "课程ID", example = "5")
    private Long courseId;

    @Schema(description = "课程名称", example = "Java高级编程")
    private String courseName;

    @Schema(description = "班主任ID", example = "10")
    private Long headTeacherId;

    @Schema(description = "班主任姓名", example = "李老师")
    private String headTeacherName;

    @Schema(description = "班主任手机号", example = "13800138000")
    private String headTeacherPhone;

    @Schema(description = "班主任邮箱", example = "teacher@example.com")
    private String headTeacherEmail;


    private String status;


    private LocalDateTime createdAt;


    private LocalDateTime updatedAt;

    public static StudentDetailsVO from(Student student, StudentClassProfileRow classProfile) {
        StudentDetailsVO details = new StudentDetailsVO();
        details.setId(student.getId());
        details.setName(student.getName());
        details.setPhone(student.getPhone());
        details.setEmail(student.getEmail());
        details.setClassId(student.getClassId());
        details.setStatus(student.getStatus());
        details.setCreatedAt(student.getCreatedAt());
        details.setUpdatedAt(student.getUpdatedAt());
        if (classProfile != null) {
            details.setClassName(classProfile.getClassName());
            details.setCampusId(classProfile.getCampusId());
            details.setCampusLocation(classProfile.getCampusLocation());
            details.setCourseId(classProfile.getCourseId());
            details.setCourseName(classProfile.getCourseName());
            details.setHeadTeacherId(classProfile.getHeadTeacherId());
            details.setHeadTeacherName(classProfile.getHeadTeacherName());
            details.setHeadTeacherPhone(classProfile.getHeadTeacherPhone());
            details.setHeadTeacherEmail(classProfile.getHeadTeacherEmail());
        }
        return details;
    }
}
