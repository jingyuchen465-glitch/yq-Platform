package com.itcjy.emp.pojo.res.pay;

import com.itcjy.emp.pojo.entity.PaymentRefundRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRefundRes(
        Long id,
        Long paymentOrderId,
        String paymentOrderNo,
        String studentName,
        String studentPhone,
        String refundNo,
        BigDecimal refundAmount,
        String reason,
        String status,
        String reviewerName,
        String reviewRemark,
        String channelRefundNo,
        String failureReason,
        LocalDateTime requestedAt,
        LocalDateTime reviewedAt,
        LocalDateTime refundedAt
) {
    public static PaymentRefundRes from(PaymentRefundRequest request, String paymentOrderNo, String studentName, String studentPhone) {
        return new PaymentRefundRes(
                request.getId(), request.getPaymentOrderId(), paymentOrderNo, studentName, studentPhone,
                request.getRefundNo(), request.getRefundAmount(), request.getReason(), request.getStatus(),
                request.getReviewerName(), request.getReviewRemark(), request.getChannelRefundNo(), request.getFailureReason(),
                request.getRequestedAt(), request.getReviewedAt(), request.getRefundedAt()
        );
    }
}
