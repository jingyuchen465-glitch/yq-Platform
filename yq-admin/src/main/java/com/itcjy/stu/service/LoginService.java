package com.itcjy.stu.service;

import com.itcjy.stu.pojo.DTO.LoginDTO;
import com.itcjy.stu.pojo.VO.LoginVO;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;

public interface LoginService {

    /**
     * 学生登录：根据手机号 + 密码验证身份，返回 JWT token 及学生详情。
     */
    LoginVO login(LoginDTO loginDTO);

    void logout();

    StudentDetailsVO getCurrentStudent();
}
