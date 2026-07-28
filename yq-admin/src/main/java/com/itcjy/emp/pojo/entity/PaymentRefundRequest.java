package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_refund_request")
public class PaymentRefundRequest {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long paymentOrderId;
    private String refundNo;
    private BigDecimal refundAmount;
    private String reason;
    private String status;
    private Long reviewerUserId;
    private String reviewerName;
    private String reviewRemark;
    private String channelRefundNo;
    private String failureReason;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime refundedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
