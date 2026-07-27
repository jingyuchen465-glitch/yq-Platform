package com.itcjy.emp.pojo.res.market;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预订单学历下拉选项")
public record MarketEducationOptionRes(
        @Schema(description = "学历规则值，保存到预订单") String value,
        @Schema(description = "学历显示名称") String label
) {
}
