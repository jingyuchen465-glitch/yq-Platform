package com.itcjy.emp.pojo.res.market;

import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "预订单响应")
public record MarketPrepaymentOrderRes(
        Long id,
        String name,
        String phone,
        String email,
        String education,
        String graduateSchool,
        String homeAddress,
        LocalDate birthday,
        Long productId,
        String productName,
        BigDecimal productPrice,
        Long salespersonUserId,
        String salespersonName,
        Long createdUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MarketPrepaymentOrderRes from(MarketPrepaymentOrder order) {
        return new MarketPrepaymentOrderRes(
                order.getId(),
                order.getName(),
                order.getPhone(),
                order.getEmail(),
                order.getEducation(),
                order.getGraduateSchool(),
                order.getHomeAddress(),
                order.getBirthday(),
                order.getProductId(),
                order.getProductName(),
                order.getProductPrice(),
                order.getSalespersonUserId(),
                order.getSalespersonName(),
                order.getCreatedUserId(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
