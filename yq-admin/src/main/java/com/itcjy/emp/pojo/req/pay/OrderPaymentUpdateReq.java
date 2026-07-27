package com.itcjy.emp.pojo.req.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "修改支付订单请求")
public class OrderPaymentUpdateReq {

    @Size(max = 30, message = "状态长度不能超过30个字符")
    @Schema(description = "支付订单状态")
    private String status;

    @DecimalMin(value = "0.00", message = "已退款金额不能为负数")
    @Schema(description = "已申请退款金额")
    private BigDecimal refundedAmount;

    @Size(max = 128, message = "支付渠道订单号长度不能超过128个字符")
    @Schema(description = "支付渠道唯一订单号")
    private String uniqueOrderNo;

    @Schema(description = "支付成功时间")
    private LocalDateTime paySuccessTime;
}
