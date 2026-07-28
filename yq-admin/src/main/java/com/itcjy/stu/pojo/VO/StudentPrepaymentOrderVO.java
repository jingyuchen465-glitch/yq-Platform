package com.itcjy.stu.pojo.VO;

import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.OrderPayment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Student prepayment order")
public record StudentPrepaymentOrderVO(
        Long id,
        String productName,
        BigDecimal productPrice,
        String salespersonName,
        LocalDateTime createdAt,
        Long paymentId,
        String outTradeNo,
        String paymentStatus,
        String refundStatus,
        LocalDateTime paymentExpireAt,
        LocalDateTime paymentClosedAt
) {
    public static StudentPrepaymentOrderVO from(MarketPrepaymentOrder order, OrderPayment payment, String refundStatus) {
        return new StudentPrepaymentOrderVO(
                order.getId(),
                order.getProductName(),
                order.getProductPrice(),
                order.getSalespersonName(),
                order.getCreatedAt(),
                payment == null ? null : payment.getId(),
                payment == null ? null : payment.getOrderNo(),
                payment == null ? null : payment.getStatus(),
                refundStatus,
                payment == null ? null : payment.getExpireAt(),
                payment == null ? null : payment.getClosedAt()
        );
    }
}
