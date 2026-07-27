package com.itcjy.emp.pojo.req.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "新增支付订单请求")
public class OrderPaymentCreateReq {

    @NotBlank(message = "支付订单号不能为空")
    @Size(max = 64, message = "订单号长度不能超过64个字符")
    @Schema(description = "支付订单号")
    private String orderNo;

    @NotBlank(message = "学生手机号不能为空")
    @Size(max = 30, message = "手机号长度不能超过30个字符")
    @Schema(description = "学生手机号")
    private String studentPhone;

    @NotBlank(message = "学生姓名不能为空")
    @Size(max = 50, message = "学生姓名长度不能超过50个字符")
    @Schema(description = "学生姓名")
    private String studentName;

    @NotBlank(message = "产品ID不能为空")
    @Size(max = 64, message = "产品ID长度不能超过64个字符")
    @Schema(description = "产品ID")
    private String productId;

    @NotNull(message = "订单支付金额不能为空")
    @DecimalMin(value = "0.01", message = "订单支付金额必须大于0")
    @Schema(description = "订单支付金额")
    private BigDecimal orderAmount;

    @NotBlank(message = "预支付订单号不能为空")
    @Size(max = 32, message = "预支付订单号长度不能超过32个字符")
    @Schema(description = "预支付订单号")
    private String prepaymentOrderId;

    @NotBlank(message = "支付订单状态不能为空")
    @Size(max = 30, message = "状态长度不能超过30个字符")
    @Schema(description = "支付订单状态")
    private String status;
}
