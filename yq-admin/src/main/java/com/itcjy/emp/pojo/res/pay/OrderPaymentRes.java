package com.itcjy.emp.pojo.res.pay;

import com.itcjy.emp.pojo.entity.OrderPayment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "支付订单响应")
public record OrderPaymentRes(
        Long id,
        String orderNo,
        String studentPhone,
        String studentName,
        String productId,
        BigDecimal orderAmount,
        BigDecimal refundedAmount,
        String prepaymentOrderId,
        String status,
        String uniqueOrderNo,
        LocalDateTime paySuccessTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderPaymentRes from(OrderPayment order) {
        return new OrderPaymentRes(
                order.getId(),
                order.getOrderNo(),
                order.getStudentPhone(),
                order.getStudentName(),
                order.getProductId(),
                order.getOrderAmount(),
                order.getRefundedAmount(),
                order.getPrepaymentOrderId(),
                order.getStatus(),
                order.getUniqueOrderNo(),
                order.getPaySuccessTime(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
