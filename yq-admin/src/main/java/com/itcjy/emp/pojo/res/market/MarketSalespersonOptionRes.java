package com.itcjy.emp.pojo.res.market;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "销售人员下拉选项")
public record MarketSalespersonOptionRes(
        Long id,
        String name,
        String phone
) {
}
