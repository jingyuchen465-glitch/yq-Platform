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
@TableName("market_product")
@Schema(description = "产品实体")
public class MarketProduct {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "产品ID")
    private Long id;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "原价")
    private BigDecimal oldPrice;

    @Schema(description = "现价")
    private BigDecimal newPrice;

    @Schema(description = "课程名称快照，使用 / 分隔")
    private String courseName;

    @Schema(description = "创建人用户ID")
    private Long createdUserId;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
