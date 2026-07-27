package com.itcjy.emp.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("market_prepayment_order")
@Schema(description = "预订单实体")
public class MarketPrepaymentOrder {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "预订单ID")
    private Long id;

    @Schema(description = "学生姓名")
    private String name;

    @Schema(description = "学生手机号")
    private String phone;

    @Schema(description = "学生邮箱")
    private String email;

    @Schema(description = "学历")
    private String education;

    @Schema(description = "毕业院校")
    private String graduateSchool;

    @Schema(description = "家庭地址")
    private String homeAddress;

    @Schema(description = "出生日期")
    private LocalDate birthday;

    @Schema(description = "意向产品ID")
    private Long productId;

    @Schema(description = "产品名称快照")
    private String productName;

    @Schema(description = "产品价格快照")
    private BigDecimal productPrice;

    @Schema(description = "销售人员用户ID")
    private Long salespersonUserId;

    @Schema(description = "销售人员姓名快照")
    private String salespersonName;

    @Schema(description = "后台录入人用户ID")
    private Long createdUserId;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
