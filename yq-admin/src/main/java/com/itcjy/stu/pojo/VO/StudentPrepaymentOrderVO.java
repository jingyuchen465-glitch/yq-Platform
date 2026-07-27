package com.itcjy.stu.pojo.VO;

import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "学生端预订单")
public record StudentPrepaymentOrderVO(
        Long id,
        String productName,
        BigDecimal productPrice,
        String salespersonName,
        LocalDateTime createdAt,
        String outTradeNo
) {
    private static final String TRADE_NO_PREFIX = "YQPREPAY";

    public static StudentPrepaymentOrderVO from(MarketPrepaymentOrder order) {
        return new StudentPrepaymentOrderVO(
                order.getId(),
                order.getProductName(),
                order.getProductPrice(),
                order.getSalespersonName(),
                order.getCreatedAt(),
                TRADE_NO_PREFIX + order.getId()
        );
    }
}
