package com.itcjy.stu.pojo.VO;

import com.itcjy.stu.pojo.entity.Student;
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


    private String status;


    private LocalDateTime createdAt;


    private LocalDateTime updatedAt;

    public static StudentDetailsVO from(Student student) {
        StudentDetailsVO details = new StudentDetailsVO();
        details.setId(student.getId());
        details.setName(student.getName());
        details.setPhone(student.getPhone());
        details.setEmail(student.getEmail());
        details.setClassId(student.getClassId());
        details.setStatus(student.getStatus());
        details.setCreatedAt(student.getCreatedAt());
        details.setUpdatedAt(student.getUpdatedAt());
        return details;
    }
}
