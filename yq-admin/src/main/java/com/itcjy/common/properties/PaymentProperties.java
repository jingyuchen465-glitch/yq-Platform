package com.itcjy.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.payment")
public class PaymentProperties {

    private int timeoutMinutes = 30;
    private String timeoutTopic = "yq-payment-timeout";
    private String timeoutTag = "payment-timeout";
    private String timeoutConsumerGroup = "yq-payment-timeout-consumer";
}
