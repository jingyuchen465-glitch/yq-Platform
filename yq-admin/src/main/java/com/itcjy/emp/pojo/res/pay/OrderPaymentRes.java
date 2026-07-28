package com.itcjy.emp.pojo.res.pay;

import com.itcjy.emp.pojo.entity.OrderPayment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payment order response")
public record OrderPaymentRes(
        Long id,
        String orderNo,
        String studentPhone,
        String studentName,
        String productId,
        BigDecimal orderAmount,
        BigDecimal refundedAmount,
        Long prepaymentOrderId,
        String paymentChannel,
        String status,
        String channelTradeNo,
        LocalDateTime paySuccessTime,
        LocalDateTime expireAt,
        LocalDateTime closedAt,
        String closeReason,
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
                order.getPaymentChannel(),
                order.getStatus(),
                order.getChannelTradeNo(),
                order.getPaySuccessTime(),
                order.getExpireAt(),
                order.getClosedAt(),
                order.getCloseReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
