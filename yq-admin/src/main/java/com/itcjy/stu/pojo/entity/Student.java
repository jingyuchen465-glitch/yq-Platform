package com.itcjy.stu.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student")
@Schema(description = "学生实体")
public class Student {
    // 学生状态：TEMPORARY-临时，ATSCHOOL-在校，GRADUATE-毕业，WITCHDRAWAL-退学
    public static final String TEMPORARY = "TEMPORARY"; // 临时
    public static final String AT_SCHOOL = "ATSCHOOL"; // 在校
    public static final String GRADUATE = "GRADUATE"; // 毕业
    public static final String WITCHDRAWAL = "WITCHDRAWAL"; // 退学



    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "学生主键", example = "1")
    private Long id;

    @Schema(description = "学生姓名", example = "张三")
    private String name;

    @Schema(description = "登录手机号", example = "13800138000")
    private String phone;

    @Schema(description = "登录密码（BCrypt 加密保存）")
    private String password;

    @Schema(description = "学生邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "学生班级ID", example = "1")
    private Long classId;

    @Schema(description = "学生状态：TEMPORARY-临时", example = "TEMPORARY")
    private String status;

    @Schema(description = "创建学生的后台用户ID", example = "1")
    private Long createdUserId;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
