package com.itcjy.emp.service.pay;

public interface IPaymentTimeoutOutboxService {

    void publishPendingForPayment(Long paymentOrderId);

    void republishPendingMessages();
}
