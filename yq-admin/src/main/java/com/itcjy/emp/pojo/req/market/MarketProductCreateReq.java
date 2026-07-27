package com.itcjy.emp.pojo.req.market;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "产品新增请求参数")
public class MarketProductCreateReq {

    @Schema(description = "产品名称")
    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称长度不能超过128个字符")
    private String productName;

    @Schema(description = "原价")
    @NotNull(message = "原价不能为空")
    @DecimalMin(value = "0.00", message = "原价不能小于0")
    @Digits(integer = 8, fraction = 2, message = "原价格式不正确")
    private BigDecimal oldPrice;

    @Schema(description = "现价")
    @NotNull(message = "现价不能为空")
    @DecimalMin(value = "0.00", message = "现价不能小于0")
    @Digits(integer = 8, fraction = 2, message = "现价格式不正确")
    private BigDecimal newPrice;

    @Valid
    @Schema(description = "关联课程ID列表")
    @NotEmpty(message = "请至少选择一门关联课程")
    @Size(max = 100, message = "单个产品最多关联100门课程")
    private List<@NotNull(message = "课程ID不能为空") Long> courseIds;
}
