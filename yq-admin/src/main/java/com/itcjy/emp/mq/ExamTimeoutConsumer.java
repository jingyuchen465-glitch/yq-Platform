package com.itcjy.emp.mq;

import com.itcjy.emp.pojo.message.ExamTimeoutMessage;
import com.itcjy.emp.service.exam.IExamTimeoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${app.exam.timeout-topic:yq-exam-timeout}",
        selectorExpression = "${app.exam.timeout-tag:exam-timeout}",
        consumerGroup = "${app.exam.timeout-consumer-group:yq-exam-timeout-consumer}")
public class ExamTimeoutConsumer implements RocketMQListener<ExamTimeoutMessage> {
    private final IExamTimeoutService timeoutService;
    @Override public void onMessage(ExamTimeoutMessage message) {
        log.info("Received exam timeout message, recordId={}", message.recordId());
        timeoutService.handle(message);
    }
}
