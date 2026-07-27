package com.itcjy.emp.pojo.res.market;

import com.itcjy.emp.pojo.entity.MarketProduct;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "产品响应")
public record MarketProductRes(
        @Schema(description = "产品ID") Long id,
        @Schema(description = "产品名称") String productName,
        @Schema(description = "原价") BigDecimal oldPrice,
        @Schema(description = "现价") BigDecimal newPrice,
        @Schema(description = "课程名称快照") String courseName,
        @Schema(description = "关联课程ID列表") List<Long> courseIds,
        @Schema(description = "创建人用户ID") Long createdUserId,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt
) {
    public static MarketProductRes from(MarketProduct product, List<Long> courseIds) {
        return new MarketProductRes(
                product.getId(),
                product.getProductName(),
                product.getOldPrice(),
                product.getNewPrice(),
                product.getCourseName(),
                courseIds,
                product.getCreatedUserId(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
