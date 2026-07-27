package com.itcjy.emp.pojo.res.pay;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "支付宝PC网站支付下单响应")
public record AlipayTradeCreateRes(
        @Schema(description = "支付宝收银台跳转表单HTML，前端直接渲染即可跳转到支付宝") String payForm,
        @Schema(description = "商户订单号") String outTradeNo
) {
}
