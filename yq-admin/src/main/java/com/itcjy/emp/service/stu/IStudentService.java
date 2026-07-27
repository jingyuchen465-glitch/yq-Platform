package com.itcjy.emp.service.stu;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.stu.pojo.entity.Student;

public interface IStudentService extends IService<Student> {

    StudentCreationResult createIfAbsent(String name, String phone, String email, Long createdUserId);

    record StudentCreationResult(boolean created, String initialPassword) {
        public static StudentCreationResult existing() {
            return new StudentCreationResult(false, null);
        }

        public static StudentCreationResult created(String initialPassword) {
            return new StudentCreationResult(true, initialPassword);
        }
    }
}
