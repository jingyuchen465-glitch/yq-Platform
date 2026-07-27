package com.itcjy.emp.pojo.res.stu;

import com.itcjy.stu.pojo.entity.Student;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "学员管理响应")
public record StudentRes(
        Long id,
        String name,
        String phone,
        String email,
        Long classId,
        String className,
        String status,
        String statusDesc,
        Long createdUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static StudentRes from(Student student, String className) {
        return new StudentRes(
                student.getId(),
                student.getName(),
                student.getPhone(),
                student.getEmail(),
                student.getClassId(),
                className,
                student.getStatus(),
                statusDescription(student.getStatus()),
                student.getCreatedUserId(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }

    private static String statusDescription(String status) {
        if (Student.TEMPORARY.equals(status)) {
            return "临时学员";
        }
        if (Student.AT_SCHOOL.equals(status)) {
            return "在校学习";
        }
        if (Student.GRADUATE.equals(status)) {
            return "已毕业";
        }
        if (Student.WITCHDRAWAL.equals(status)) {
            return "已退学";
        }
        return "未知状态";
    }
}
