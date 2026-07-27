package com.itcjy.emp.pojo.res.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "支付宝交易查询响应")
public class AlipayTradeQueryRes {

    @Schema(description = "支付宝交易号")
    private String tradeNo;

    @Schema(description = "商户订单号")
    private String outTradeNo;

    @Schema(description = "交易状态（WAIT_BUYER_PAY/TRADE_CLOSED/TRADE_SUCCESS/TRADE_FINISHED）")
    private String tradeStatus;

    @Schema(description = "订单金额（元）")
    private BigDecimal totalAmount;

    @Schema(description = "买家支付宝账号（脱敏）")
    private String buyerLogonId;

    @Schema(description = "交易支付时间")
    private String sendPayDate;
}
