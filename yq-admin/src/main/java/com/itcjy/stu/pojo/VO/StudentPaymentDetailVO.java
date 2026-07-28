package com.itcjy.stu.pojo.VO;

import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.entity.PaymentRefundRequest;
import com.itcjy.emp.pojo.res.pay.PaymentRefundRes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentPaymentDetailVO(
        Long paymentOrderId,
        String productName,
        String salespersonName,
        String orderNo,
        String channelTradeNo,
        BigDecimal orderAmount,
        BigDecimal refundedAmount,
        String paymentStatus,
        LocalDateTime paidAt,
        PaymentRefundRes latestRefund
) {
    public static StudentPaymentDetailVO from(
            OrderPayment payment,
            MarketPrepaymentOrder prepaymentOrder,
            PaymentRefundRequest refundRequest) {
        PaymentRefundRes refund = refundRequest == null ? null
                : PaymentRefundRes.from(refundRequest, payment.getOrderNo(), payment.getStudentName(), payment.getStudentPhone());
        return new StudentPaymentDetailVO(
                payment.getId(), prepaymentOrder.getProductName(), prepaymentOrder.getSalespersonName(),
                payment.getOrderNo(), payment.getChannelTradeNo(), payment.getOrderAmount(), payment.getRefundedAmount(),
                payment.getStatus(), payment.getPaySuccessTime(), refund
        );
    }
}
