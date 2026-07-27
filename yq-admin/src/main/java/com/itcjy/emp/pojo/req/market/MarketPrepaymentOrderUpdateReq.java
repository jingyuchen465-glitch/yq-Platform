package com.itcjy.emp.pojo.req.market;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "预订单修改请求参数")
public class MarketPrepaymentOrderUpdateReq {

    @NotBlank(message = "学生姓名不能为空")
    @Size(max = 30, message = "学生姓名长度不能超过30个字符")
    private String name;

    @NotBlank(message = "学生手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    private String phone;

    @Email(message = "请输入正确的邮箱地址")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Size(max = 20, message = "学历长度不能超过20个字符")
    private String education;

    @Size(max = 100, message = "毕业院校长度不能超过100个字符")
    private String graduateSchool;

    @Size(max = 200, message = "家庭地址长度不能超过200个字符")
    private String homeAddress;

    @PastOrPresent(message = "出生日期不能晚于今天")
    private LocalDate birthday;

    @NotNull(message = "请选择意向产品")
    private Long productId;

    @NotNull(message = "请选择销售人员")
    private Long salespersonUserId;
}
