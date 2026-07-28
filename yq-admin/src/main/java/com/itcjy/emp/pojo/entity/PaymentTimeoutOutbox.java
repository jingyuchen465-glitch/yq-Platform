package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("payment_timeout_outbox")
public class PaymentTimeoutOutbox {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long paymentOrderId;
    private String orderNo;
    private LocalDateTime expireAt;
    private String status;
    private Integer publishAttempts;
    private String lastError;
    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
