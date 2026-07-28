package com.itcjy.emp.mq;

import com.itcjy.emp.pojo.message.PaymentTimeoutMessage;
import com.itcjy.emp.service.pay.IAlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "yq-payment-timeout",
        selectorExpression = "payment-timeout",
        consumerGroup = "${app.payment.timeout-consumer-group}"
)
public class PaymentTimeoutConsumer implements RocketMQListener<PaymentTimeoutMessage> {

    private final IAlipayService alipayService;

    @Override
    public void onMessage(PaymentTimeoutMessage message) {
        log.info("Received payment timeout message, paymentOrderId={}, orderNo={}", message.paymentOrderId(), message.orderNo());
        alipayService.handleTimeout(message);
    }
}
