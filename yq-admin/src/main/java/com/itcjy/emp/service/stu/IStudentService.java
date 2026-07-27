package com.itcjy.emp.service.stu;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.stu.StudentPageReq;
import com.itcjy.emp.pojo.req.stu.StudentUpdateReq;
import com.itcjy.emp.pojo.res.stu.StudentClassOptionRes;
import com.itcjy.emp.pojo.res.stu.StudentRes;
import com.itcjy.stu.pojo.entity.Student;

import java.util.List;

public interface IStudentService extends IService<Student> {

    StudentCreationResult createIfAbsent(String name, String phone, String email, Long createdUserId);

    PageResult<StudentRes> pageStudents(StudentPageReq req);

    StudentRes getStudentDetail(Long id);

    List<StudentClassOptionRes> listClassOptions();

    void updateStudent(Long id, StudentUpdateReq req);

    void withdrawStudent(Long id);

    record StudentCreationResult(boolean created, String initialPassword) {
        public static StudentCreationResult existing() {
            return new StudentCreationResult(false, null);
        }

        public static StudentCreationResult created(String initialPassword) {
            return new StudentCreationResult(true, initialPassword);
        }
    }
}
