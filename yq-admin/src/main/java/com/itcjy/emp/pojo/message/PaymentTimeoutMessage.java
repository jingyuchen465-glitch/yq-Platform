package com.itcjy.emp.pojo.message;

import java.time.LocalDateTime;

public record PaymentTimeoutMessage(Long paymentOrderId, String orderNo, LocalDateTime expireAt) {
}
