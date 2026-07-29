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

/**
 * 考试超时服务实现类
 * <p>基于 RocketMQ 延时消息 + 事务性发件箱模式实现考试超时自动交卷，
 * 并通过定时任务进行消息重发和过期记录补偿</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamTimeoutServiceImpl implements IExamTimeoutService {
    /** RocketMQ 消息发送超时时间（毫秒） */
    private static final int SEND_TIMEOUT_MILLIS = 3000;
    private final ExamTimeoutOutboxMapper outboxMapper;
    private final StudentExamRecordMapper recordMapper;
    private final ExamMapper examMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ExamProperties properties;
    private final IExamSubmissionService submissionService;

    /**
     * 调度超时交卷任务
     * <p>在事务内写入发件箱记录，事务提交后立即尝试发送延时消息</p>
     *
     * @param recordId     考试记录ID
     * @param deadlineTime 答题截止时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void schedule(Long recordId, LocalDateTime deadlineTime) {
        // 幂等性检查：同一记录只创建一条发件箱记录
        if (!outboxMapper.exists(Wrappers.<ExamTimeoutOutbox>lambdaQuery()
                .eq(ExamTimeoutOutbox::getRecordId, recordId))) {
            ExamTimeoutOutbox outbox = new ExamTimeoutOutbox();
            outbox.setRecordId(recordId); outbox.setDeadlineTime(deadlineTime);
            outbox.setStatus(ExamTimeoutOutboxStatus.PENDING.name()); outbox.setPublishAttempts(0);
            outboxMapper.insert(outbox);
        }
        // 事务提交后再发送消息，避免事务回滚但消息已发出
        Runnable publish = () -> publishPendingForRecord(recordId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { publish.run(); }
            });
        } else publish.run();
    }

    /**
     * 发布指定记录的待发送超时消息
     * <p>发送成功则更新状态为 SENT，失败则记录错误信息并增加重试次数</p>
     *
     * @param recordId 考试记录ID
     */
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

    /**
     * 处理超时消息（由 RocketMQ 消费者调用）
     * <p>若考试仍在进行中且未到截止时间，则重新发送延时消息（分段延时）；
     * 若已到截止时间则执行超时交卷</p>
     *
     * @param message 超时消息
     */
    @Override
    public void handle(ExamTimeoutMessage message) {
        StudentExamRecord record = recordMapper.selectById(message.recordId());
        // 只处理进行中的考试
        if (record == null || !ExamRecordStatus.IN_PROGRESS.name().equals(record.getStatus())) return;
        LocalDateTime deadline = record.getDeadlineTime();
        // 未到截止时间，继续发送下一段延时消息
        if (deadline != null && LocalDateTime.now().isBefore(deadline)) {
            sendSegment(new ExamTimeoutMessage(record.getId(), deadline));
            return;
        }
        // 已到截止时间，执行超时自动交卷
        submissionService.submit(record.getId(), ExamSubmitReason.TIMEOUT);
    }

    /**
     * 定时重发待发送的发件箱消息
     * <p>防止因网络异常等原因导致消息发送失败，每次最多处理 100 条</p>
     */
    @Scheduled(fixedDelayString = "${app.exam.outbox-retry-delay-millis:60000}")
    public void republishPending() {
        outboxMapper.selectList(Wrappers.<ExamTimeoutOutbox>lambdaQuery()
                        .eq(ExamTimeoutOutbox::getStatus, ExamTimeoutOutboxStatus.PENDING.name())
                        .orderByAsc(ExamTimeoutOutbox::getId).last("LIMIT 100"))
                .forEach(item -> publishPendingForRecord(item.getRecordId()));
    }

    /**
     * 定时补偿过期记录
     * <p>1. 将已超过截止时间但仍在进行中的考试记录执行超时交卷；
     * 2. 将已过最晚入场时间且未开始的考试记录标记为缺考</p>
     */
    @Scheduled(fixedDelayString = "${app.exam.compensation-delay-millis:15000}")
    public void compensateExpiredRecords() {
        LocalDateTime now = LocalDateTime.now();
        // 补偿已超过截止时间的进行中记录
        recordMapper.selectList(Wrappers.<StudentExamRecord>lambdaQuery()
                        .eq(StudentExamRecord::getStatus, ExamRecordStatus.IN_PROGRESS.name())
                        .le(StudentExamRecord::getDeadlineTime, now).last("LIMIT 200"))
                .forEach(record -> submissionService.submit(record.getId(), ExamSubmitReason.TIMEOUT));

        // 将已过最晚入场时间的未开始记录标记为缺考
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

    /**
     * 发送分段延时消息到 RocketMQ
     * <p>根据剩余时间选择合适的延时等级，通过多次分段投递逼近精确超时</p>
     */
    private void sendSegment(ExamTimeoutMessage message) {
        int delayLevel = resolveDelayLevel(Duration.between(LocalDateTime.now(), message.deadlineTime()));
        SendResult result = rocketMQTemplate.syncSend(
                properties.getTimeoutTopic() + ":" + properties.getTimeoutTag(),
                MessageBuilder.withPayload(message).build(), SEND_TIMEOUT_MILLIS, delayLevel);
        log.info("Exam timeout segment sent, recordId={}, delayLevel={}, messageId={}",
                message.recordId(), delayLevel, result.getMsgId());
    }

    /**
     * 根据剩余时间解析 RocketMQ 延时等级
     * <p>RocketMQ 延时等级对应: 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h</p>
     *
     * @param remaining 剩余时间
     * @return 延时等级（1-18）
     */
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

    /** 截断错误信息，最多保留 500 字符 */
    private String abbreviate(String value) {
        if (value == null) return "Unknown RocketMQ publish failure";
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
