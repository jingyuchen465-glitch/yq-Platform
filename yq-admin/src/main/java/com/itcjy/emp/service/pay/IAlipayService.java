package com.itcjy.emp.service.pay;

import com.itcjy.emp.pojo.message.PaymentTimeoutMessage;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.entity.PaymentRefundRequest;
import com.itcjy.emp.pojo.res.pay.AlipayTradeCreateRes;
import com.itcjy.emp.pojo.res.pay.AlipayRefundRes;

import java.util.Map;

public interface IAlipayService {

    AlipayTradeCreateRes createTradePagePay(Long prepaymentOrderId);

    boolean handleNotify(Map<String, String> params);

    void handleTimeout(PaymentTimeoutMessage message);

    AlipayRefundRes refund(OrderPayment payment, PaymentRefundRequest refundRequest);
}
