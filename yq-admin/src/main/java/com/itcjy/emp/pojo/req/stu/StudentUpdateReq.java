package com.itcjy.emp.pojo.req.stu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "学员资料修改请求")
public record StudentUpdateReq(
        @Schema(description = "学员姓名")
        @NotBlank(message = "学员姓名不能为空")
        @Size(max = 30, message = "学员姓名长度不能超过30个字符")
        String name,

        @Schema(description = "登录手机号")
        @NotBlank(message = "学员手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
        String phone,

        @Schema(description = "邮箱")
        @Email(message = "请输入正确的邮箱地址")
        @Size(max = 128, message = "邮箱长度不能超过128个字符")
        String email,

        @Schema(description = "班级ID，可为空")
        @Positive(message = "班级ID必须大于0")
        Long classId,

        @Schema(description = "学员状态")
        @NotBlank(message = "请选择学员状态")
        @Pattern(
                regexp = "^(TEMPORARY|ATSCHOOL|GRADUATE|WITCHDRAWAL)$",
                message = "学员状态不合法"
        )
        String status
) {
}
