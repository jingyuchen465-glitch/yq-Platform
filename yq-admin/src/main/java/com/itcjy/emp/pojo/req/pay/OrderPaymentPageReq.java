package com.itcjy.emp.pojo.req.pay;

import com.itcjy.common.pojo.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "支付订单分页查询参数")
public class OrderPaymentPageReq extends BasePageReq {

    @Schema(description = "学生手机号（精确匹配）")
    @Size(max = 30, message = "手机号长度不能超过30个字符")
    private String studentPhone;

    @Schema(description = "支付订单号（精确匹配）")
    @Size(max = 64, message = "订单号长度不能超过64个字符")
    private String orderNo;

    @Schema(description = "支付订单状态（精确匹配）")
    @Size(max = 30, message = "状态长度不能超过30个字符")
    private String status;
}
