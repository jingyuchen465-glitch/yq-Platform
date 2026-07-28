package com.itcjy.emp.service.impl.pay;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.itcjy.common.properties.PaymentProperties;
import com.itcjy.emp.mapper.pay.PaymentTimeoutOutboxMapper;
import com.itcjy.emp.pojo.entity.PaymentTimeoutOutbox;
import com.itcjy.emp.pojo.enums.PaymentTimeoutOutboxStatus;
import com.itcjy.emp.pojo.message.PaymentTimeoutMessage;
import com.itcjy.emp.service.pay.IPaymentTimeoutOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付超时发件箱（Outbox）服务实现类
 * <p>
 * 基于 Transactional Outbox 模式，确保支付超时检测消息可靠投递到 RocketMQ：
 * <ul>
 *     <li>支付单创建时，超时消息先写入本地 outbox 表（与业务同事务）</li>
 *     <li>本服务负责将 PENDING 状态的消息同步发送到 MQ</li>
 *     <li>发送失败时记录错误，由定时任务重试，保证最终一致性</li>
 * </ul>
 *
 * @author itcjy
 * @see IPaymentTimeoutOutboxService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTimeoutOutboxServiceImpl implements IPaymentTimeoutOutboxService {

    /** RocketMQ 延迟等级 16 对应 30 分钟延迟 */
    private static final int THIRTY_MINUTE_DELAY_LEVEL = 16;
    /** MQ 同步发送超时时间（毫秒） */
    private static final int SEND_TIMEOUT_MILLIS = 3_000;

    /** Outbox 表 Mapper */
    private final PaymentTimeoutOutboxMapper outboxMapper;
    /** RocketMQ 消息发送模板 */
    private final RocketMQTemplate rocketMQTemplate;
    /** 支付配置属性（Topic、Tag 等） */
    private final PaymentProperties paymentProperties;

    /**
     * 发布指定支付单的待发送超时消息
     * <p>
     * 在支付单创建后调用，查找该支付单对应的 PENDING 状态 outbox 记录并尝试发送
     *
     * @param paymentOrderId 支付单 ID
     */
    @Override
    public void publishPendingForPayment(Long paymentOrderId) {
        // 查询该支付单下状态为 PENDING 的 outbox 记录
        PaymentTimeoutOutbox outbox = outboxMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentTimeoutOutbox>()
                        .eq(PaymentTimeoutOutbox::getPaymentOrderId, paymentOrderId)
                        .eq(PaymentTimeoutOutbox::getStatus, PaymentTimeoutOutboxStatus.PENDING.name())
        );
        if (outbox != null) {
            publish(outbox);
        }
    }

    /**
     * 定时重发未成功的超时消息（补偿机制）
     * <p>
     * 每隔配置时间（默认 60 秒）扫描 PENDING 状态的 outbox 记录，
     * 每次最多处理 100 条，确保发送失败的消息最终能被投递
     */
    @Override
    @Scheduled(fixedDelayString = "${app.payment.outbox-retry-delay-millis:60000}")
    public void republishPendingMessages() {
        // 查询所有 PENDING 状态的记录，按 ID 升序，限制 100 条
        List<PaymentTimeoutOutbox> pending = outboxMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaymentTimeoutOutbox>()
                        .eq(PaymentTimeoutOutbox::getStatus, PaymentTimeoutOutboxStatus.PENDING.name())
                        .orderByAsc(PaymentTimeoutOutbox::getId)
                        .last("LIMIT 100")
        );
        pending.forEach(this::publish);
    }

    /**
     * 发送单条超时消息到 RocketMQ
     * <p>
     * 发送成功：将 outbox 状态更新为 SENT，记录发布时间
     * 发送失败：记录错误信息，保持 PENDING 状态等待下次重试
     *
     * @param outbox 发件箱记录
     */
    private void publish(PaymentTimeoutOutbox outbox) {
        try {
            // 构建超时消息体
            PaymentTimeoutMessage payload = new PaymentTimeoutMessage(
                    outbox.getPaymentOrderId(), outbox.getOrderNo(), outbox.getExpireAt());
            // 同步发送延迟消息（30分钟后触发超时检测）
            SendResult result = rocketMQTemplate.syncSend(
                    paymentProperties.getTimeoutTopic() + ":" + paymentProperties.getTimeoutTag(),
                    MessageBuilder.withPayload(payload).build(),
                    SEND_TIMEOUT_MILLIS,
                    THIRTY_MINUTE_DELAY_LEVEL
            );
            // 发送成功，更新状态为 SENT（乐观锁：仅当仍为 PENDING 时更新）
            outboxMapper.update(null, new LambdaUpdateWrapper<PaymentTimeoutOutbox>()
                    .eq(PaymentTimeoutOutbox::getId, outbox.getId())
                    .eq(PaymentTimeoutOutbox::getStatus, PaymentTimeoutOutboxStatus.PENDING.name())
                    .set(PaymentTimeoutOutbox::getStatus, PaymentTimeoutOutboxStatus.SENT.name())
                    .set(PaymentTimeoutOutbox::getPublishedAt, LocalDateTime.now())
                    .set(PaymentTimeoutOutbox::getLastError, null)
                    .setSql("publish_attempts = publish_attempts + 1"));
            log.info("Payment timeout message sent, outboxId={}, messageId={}", outbox.getId(), result.getMsgId());
        } catch (Exception ex) {
            // 发送失败，记录错误信息并增加尝试次数，保持 PENDING 等待重试
            outboxMapper.update(null, new LambdaUpdateWrapper<PaymentTimeoutOutbox>()
                    .eq(PaymentTimeoutOutbox::getId, outbox.getId())
                    .eq(PaymentTimeoutOutbox::getStatus, PaymentTimeoutOutboxStatus.PENDING.name())
                    .set(PaymentTimeoutOutbox::getLastError, abbreviate(ex.getMessage()))
                    .setSql("publish_attempts = publish_attempts + 1"));
            log.error("Failed to publish payment timeout message, outboxId={}", outbox.getId(), ex);
        }
    }

    /**
     * 截断错误信息，防止超出数据库字段长度限制（最多 500 字符）
     *
     * @param value 原始错误信息
     * @return 截断后的错误描述
     */
    private String abbreviate(String value) {
        if (value == null) {
            return "Unknown RocketMQ publish failure";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
