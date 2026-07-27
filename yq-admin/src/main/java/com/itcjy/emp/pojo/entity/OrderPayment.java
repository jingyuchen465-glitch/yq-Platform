package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_payment")
@Schema(description = "支付订单实体")
public class OrderPayment {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "支付订单ID")
    private Long id;

    @Schema(description = "支付订单号")
    private String orderNo;

    @Schema(description = "学生手机号")
    private String studentPhone;

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "产品ID")
    private String productId;

    @Schema(description = "订单支付金额")
    private BigDecimal orderAmount;

    @Schema(description = "已申请退款金额（含退款处理中和退款成功）")
    private BigDecimal refundedAmount;

    @TableField("prepayment_orderId")
    @Schema(description = "预支付订单号")
    private String prepaymentOrderId;

    @Schema(description = "支付订单状态")
    private String status;

    @Schema(description = "支付渠道唯一订单号")
    private String uniqueOrderNo;

    @Schema(description = "支付成功时间")
    private LocalDateTime paySuccessTime;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
