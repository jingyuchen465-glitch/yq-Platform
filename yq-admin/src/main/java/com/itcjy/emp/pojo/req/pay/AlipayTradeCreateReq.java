package com.itcjy.emp.pojo.req.pay;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "支付宝PC网站支付下单请求")
public class AlipayTradeCreateReq {

    @NotBlank(message = "商户订单号不能为空")
    @Schema(description = "商户订单号（需保证唯一）", example = "202607270001")
    private String outTradeNo;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额最小为0.01元")
    @Schema(description = "订单总金额（元）", example = "88.88")
    private BigDecimal totalAmount;

    @NotBlank(message = "订单标题不能为空")
    @Schema(description = "订单标题", example = "雁雀平台课程购买")
    private String subject;

    @Schema(description = "订单描述（可选）", example = "购买Java高级课程")
    private String body;

    @Schema(description = "订单超时时间（可选），如 30m 表示30分钟", example = "30m")
    private String timeoutExpress;
}
