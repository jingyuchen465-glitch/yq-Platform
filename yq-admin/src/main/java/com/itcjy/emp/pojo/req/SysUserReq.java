package com.itcjy.emp.pojo.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "员工用户请求参数")
public class SysUserReq {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "登录用户名", example = "zhangsan")
    private String username;

    @Schema(description = "登录密码", example = "123456")
    private String password;

    @Schema(description = "用户昵称", example = "张三")
    private String nickname;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "飞书 union_id", example = "on_123456789")
    private String unionId;

    @Schema(description = "状态", example = "ENABLE")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
