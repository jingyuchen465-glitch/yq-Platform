package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@Schema(description = "用户实体")
public class SysUser {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "登录用户名", example = "zhangsan")
    private String username;

    @Schema(description = "登录密码，BCrypt加密")
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
