package com.itcjy.emp.service.impl.exam;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.common.properties.ExamProperties;
import com.itcjy.emp.mapper.exam.*;
import com.itcjy.emp.pojo.entity.exam.*;
import com.itcjy.emp.pojo.enums.exam.*;
import com.itcjy.emp.pojo.message.ExamTimeoutMessage;
import com.itcjy.emp.service.exam.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamTimeoutServiceImpl implements IExamTimeoutService {
    private static final int SEND_TIMEOUT_MILLIS = 3000;
    private final ExamTimeoutOutboxMapper outboxMapper;
    private final StudentExamRecordMapper recordMapper;
    private final ExamMapper examMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ExamProperties properties;
    private final IExamSubmissionService submissionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void schedule(Long recordId, LocalDateTime deadlineTime) {
        if (!outboxMapper.exists(Wrappers.<ExamTimeoutOutbox>lambdaQuery()
                .eq(ExamTimeoutOutbox::getRecordId, recordId))) {
            ExamTimeoutOutbox outbox = new ExamTimeoutOutbox();
            outbox.setRecordId(recordId); outbox.setDeadlineTime(deadlineTime);
            outbox.setStatus(ExamTimeoutOutboxStatus.PENDING.name()); outbox.setPublishAttempts(0);
            outboxMapper.insert(outbox);
        }
        Runnable publish = () -> publishPendingForRecord(recordId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { publish.run(); }
            });
        } else publish.run();
    }

    @Override
    public void publishPendingForRecord(Long recordId) {
        ExamTimeoutOutbox outbox = outboxMapper.selectOne(Wrappers.<ExamTimeoutOutbox>lambdaQuery()
                .eq(ExamTimeoutOutbox::getRecordId, recordId)
                .eq(ExamTimeoutOutbox::getStatus, ExamTimeoutOutboxStatus.PENDING.name()));
        if (outbox == null) return;
        try {
            sendSegment(new ExamTimeoutMessage(recordId, outbox.getDeadlineTime()));
            outboxMapper.update(null, Wrappers.<ExamTimeoutOutbox>lambdaUpdate()
                    .eq(ExamTimeoutOutbox::getId, outbox.getId())
                    .eq(ExamTimeoutOutbox::getStatus, ExamTimeoutOutboxStatus.PENDING.name())
                    .set(ExamTimeoutOutbox::getStatus, ExamTimeoutOutboxStatus.SENT.name())
                    .set(ExamTimeoutOutbox::getPublishedAt, LocalDateTime.now())
                    .set(ExamTimeoutOutbox::getLastError, null)
                    .setSql("publish_attempts = publish_attempts + 1"));
        } catch (Exception ex) {
            outboxMapper.update(null, Wrappers.<ExamTimeoutOutbox>lambdaUpdate()
                    .eq(ExamTimeoutOutbox::getId, outbox.getId())
                    .eq(ExamTimeoutOutbox::getStatus, ExamTimeoutOutboxStatus.PENDING.name())
                    .set(ExamTimeoutOutbox::getLastError, abbreviate(ex.getMessage()))
                    .setSql("publish_attempts = publish_attempts + 1"));
            log.error("Failed to publish exam timeout message, recordId={}", recordId, ex);
        }
    }

    @Override
    public void handle(ExamTimeoutMessage message) {
        StudentExamRecord record = recordMapper.selectById(message.recordId());
        if (record == null || !ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) return;
        LocalDateTime deadline = record.getDeadlineTime();
        if (deadline != null && LocalDateTime.now().isBefore(deadline)) {
            sendSegment(new ExamTimeoutMessage(record.getId(), deadline));
            return;
        }
        submissionService.submit(record.getId(), ExamSubmitReason.TIMEOUT);
    }

    @Scheduled(fixedDelayString = "${app.exam.outbox-retry-delay-millis:60000}")
    public void republishPending() {
        outboxMapper.selectList(Wrappers.<ExamTimeoutOutbox>lambdaQuery()
                        .eq(ExamTimeoutOutbox::getStatus, ExamTimeoutOutboxStatus.PENDING.name())
                        .orderByAsc(ExamTimeoutOutbox::getId).last("LIMIT 100"))
                .forEach(item -> publishPendingForRecord(item.getRecordId()));
    }

    @Scheduled(fixedDelayString = "${app.exam.compensation-delay-millis:15000}")
    public void compensateExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        recordMapper.selectList(Wrappers.<StudentExamRecord>lambdaQuery()
                        .eq(StudentExamRecord::getStatus, ExamRecordStatus.IN_PROGRESS.name())
                        .le(StudentExamRecord::getDeadlineTime, now).last("LIMIT 200"))
                .forEach(record -> submissionService.submit(record.getId(), ExamSubmitReason.TIMEOUT));

        List<Long> closedEntryExamIds = examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                        .select(Exam::getId).lt(Exam::getEntryDeadlineTime, now))
                .stream().map(Exam::getId).toList();
        if (!closedEntryExamIds.isEmpty()) {
            recordMapper.update(null, Wrappers.<StudentExamRecord>lambdaUpdate()
                    .in(StudentExamRecord::getExamId, closedEntryExamIds)
                    .eq(StudentExamRecord::getStatus, ExamRecordStatus.NOT_STARTED.name())
                    .set(StudentExamRecord::getStatus, ExamRecordStatus.ABSENT.name())
                    .set(StudentExamRecord::getSubmitReason, ExamSubmitReason.ABSENT.name())
                    .set(StudentExamRecord::getSubmitTime, now)
                    .set(StudentExamRecord::getObjectiveScore, BigDecimal.ZERO)
                    .set(StudentExamRecord::getSubjectiveScore, BigDecimal.ZERO)
                    .set(StudentExamRecord::getScore, BigDecimal.ZERO)
                    .set(StudentExamRecord::getGradingStatus, GradingStatus.COMPLETED.name()));
        }
    }

    private void sendSegment(ExamTimeoutMessage message) {
        int delayLevel = resolveDelayLevel(Duration.between(LocalDateTime.now(), message.deadlineTime()));
        SendResult result = rocketMQTemplate.syncSend(
                properties.getTimeoutTopic() + ":" + properties.getTimeoutTag(),
                MessageBuilder.withPayload(message).build(), SEND_TIMEOUT_MILLIS, delayLevel);
        log.info("Exam timeout segment sent, recordId={}, delayLevel={}, messageId={}",
                message.recordId(), delayLevel, result.getMsgId());
    }

    static int resolveDelayLevel(Duration remaining) {
        long seconds = Math.max(1, remaining.getSeconds());
        if (seconds >= 7200) return 18;
        if (seconds >= 3600) return 17;
        if (seconds >= 1800) return 16;
        if (seconds >= 1200) return 15;
        if (seconds >= 600) return 14;
        if (seconds >= 540) return 13;
        if (seconds >= 480) return 12;
        if (seconds >= 420) return 11;
        if (seconds >= 360) return 10;
        if (seconds >= 300) return 9;
        if (seconds >= 240) return 8;
        if (seconds >= 180) return 7;
        if (seconds >= 120) return 6;
        if (seconds >= 60) return 5;
        if (seconds >= 30) return 4;
        if (seconds >= 10) return 3;
        if (seconds >= 5) return 2;
        return 1;
    }

    private String abbreviate(String value) {
        if (value == null) return "Unknown RocketMQ publish failure";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
