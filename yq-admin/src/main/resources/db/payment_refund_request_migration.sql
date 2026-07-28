CREATE TABLE payment_refund_request
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '退款申请ID',
    payment_order_id  BIGINT         NOT NULL COMMENT '支付订单ID',
    refund_no         VARCHAR(64)    NOT NULL COMMENT '商户退款单号',
    refund_amount     DECIMAL(10, 2) NOT NULL COMMENT '退款金额',
    reason            VARCHAR(500)   NOT NULL COMMENT '申请退款原因',
    status            VARCHAR(16)    NOT NULL COMMENT 'PENDING/PROCESSING/REJECTED/REFUNDED/FAILED',
    reviewer_user_id  BIGINT         NULL COMMENT '审核人用户ID',
    reviewer_name     VARCHAR(50)    NULL COMMENT '审核人姓名',
    review_remark     VARCHAR(500)   NULL COMMENT '审核意见',
    channel_refund_no VARCHAR(128)   NULL COMMENT '支付渠道退款流水号',
    failure_reason    VARCHAR(500)   NULL COMMENT '退款失败原因',
    requested_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    reviewed_at       DATETIME       NULL COMMENT '审核时间',
    refunded_at       DATETIME       NULL COMMENT '退款完成时间',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_refund_no (refund_no),
    INDEX idx_payment_order_status (payment_order_id, status),
    INDEX idx_status_requested_at (status, requested_at)
) COMMENT '支付退款申请表';
