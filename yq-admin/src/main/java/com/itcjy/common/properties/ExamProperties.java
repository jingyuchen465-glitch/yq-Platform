package com.itcjy.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.exam")
public class ExamProperties {
    private String timeoutTopic = "yq-exam-timeout";
    private String timeoutTag = "exam-timeout";
    private String timeoutConsumerGroup = "yq-exam-timeout-consumer";
    private long outboxRetryDelayMillis = 60000;
    private long compensationDelayMillis = 15000;
}
