package com.itcjy.emp.pojo.res.market;

import com.itcjy.emp.pojo.entity.MarketProduct;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "预订单意向产品下拉选项")
public record MarketProductOptionRes(
        Long id,
        String productName,
        BigDecimal productPrice,
        String courseName
) {
    public static MarketProductOptionRes from(MarketProduct product) {
        return new MarketProductOptionRes(
                product.getId(),
                product.getProductName(),
                product.getNewPrice(),
                product.getCourseName()
        );
    }
}
