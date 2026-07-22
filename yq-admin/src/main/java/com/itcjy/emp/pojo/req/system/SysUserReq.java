package com.itcjy.emp.pojo.req.system;

import com.itcjy.common.annotations.StatusEnum;
import com.itcjy.common.myEnum.ActiveEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "员工用户请求参数")
public class SysUserReq {

    @Schema(description = "登录用户名", example = "zhangsan")
    @NotBlank(message = "登录用户名不能为空")
    @Size(max = 64, message = "登录用户名长度不能超过64个字符")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    @NotBlank(message = "登录密码不能为空")
    @Size(min = 6, max = 255, message = "登录密码长度必须在6到255个字符之间")
    private String password;

    @Schema(description = "用户昵称", example = "张三")
    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 64, message = "用户昵称长度不能超过64个字符")
    private String nickname;

    @Schema(description = "真实姓名", example = "张三")
    @Size(max = 64, message = "真实姓名长度不能超过64个字符")
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Schema(description = "飞书 union_id", example = "on_123456789")
    @Size(max = 128, message = "飞书 union_id 长度不能超过128个字符")
    private String unionId;

    @Schema(description = "状态", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    @NotBlank(message = "状态不能为空")
    @StatusEnum(enumClass = ActiveEnum.class, message = "状态值不正确")
    private String status;
}
