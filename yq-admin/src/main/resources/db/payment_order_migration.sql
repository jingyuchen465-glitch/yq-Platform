-- Run this script after backing up the existing payment data.
-- Existing successful rows need a valid prepayment_order_id before the column is made NOT NULL.

ALTER TABLE order_payment
    CHANGE COLUMN prepayment_orderId prepayment_order_id BIGINT NULL COMMENT '预订单ID',
    CHANGE COLUMN unique_order_no channel_trade_no VARCHAR(128) NULL COMMENT '支付渠道交易号',
    ADD COLUMN payment_channel VARCHAR(32) NOT NULL DEFAULT 'ALIPAY' COMMENT '支付渠道' AFTER prepayment_order_id,
    ADD COLUMN expire_at DATETIME NULL COMMENT '支付过期时间' AFTER status,
    ADD COLUMN closed_at DATETIME NULL COMMENT '关闭完成时间' AFTER expire_at,
    ADD COLUMN close_reason VARCHAR(32) NULL COMMENT '关闭原因' AFTER closed_at;

UPDATE order_payment
SET expire_at = COALESCE(pay_success_time, created_at)
WHERE expire_at IS NULL;

DROP INDEX idx_prepayment_orderId ON order_payment;

ALTER TABLE order_payment
    MODIFY COLUMN prepayment_order_id BIGINT NOT NULL,
    MODIFY COLUMN expire_at DATETIME NOT NULL,
    ADD INDEX idx_prepayment_order_id (prepayment_order_id),
    ADD INDEX idx_status_expire_at (status, expire_at),
    ADD UNIQUE KEY uk_channel_trade_no (channel_trade_no);

CREATE TABLE payment_timeout_outbox
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '超时消息Outbox ID',
    payment_order_id BIGINT       NOT NULL COMMENT '支付订单ID',
    order_no         VARCHAR(64)  NOT NULL COMMENT '商户支付单号',
    expire_at        DATETIME     NOT NULL COMMENT '支付过期时间',
    status           VARCHAR(16)  NOT NULL COMMENT '投递状态：PENDING/SENT',
    publish_attempts INT          NOT NULL DEFAULT 0 COMMENT '投递尝试次数',
    last_error       VARCHAR(500) NULL COMMENT '最近投递错误',
    published_at     DATETIME     NULL COMMENT '成功投递时间',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_payment_order_id (payment_order_id),
    INDEX idx_status_id (status, id)
) COMMENT '支付超时延时消息Outbox';
