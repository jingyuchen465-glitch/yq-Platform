package com.itcjy.stu.controller;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.stu.pojo.DTO.LoginDTO;
import com.itcjy.stu.pojo.VO.LoginVO;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/stu")
@Tag(name = "学生端登录", description = "学生登录相关接口")
public class LoginController {

    private final LoginService loginService;

    @Operation(summary = "学生登录", description = "根据手机号和密码登录，返回 token 及学生信息")
    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ApiResponse.success(loginService.login(loginDTO));
    }

    @Operation(summary = "获取当前学生", description = "返回当前登录学生的基本资料")
    @GetMapping("/me")
    public ApiResponse<StudentDetailsVO> getCurrentStudent() {
        return ApiResponse.success(loginService.getCurrentStudent());
    }

    @Operation(summary = "学生退出登录", description = "清理当前学生在 Redis 中的登录态")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        loginService.logout();
        return ApiResponse.success("退出成功");
    }
}
