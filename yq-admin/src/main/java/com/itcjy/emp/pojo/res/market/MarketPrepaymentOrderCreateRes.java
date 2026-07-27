package com.itcjy.emp.pojo.res.market;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "预订单新增结果")
public record MarketPrepaymentOrderCreateRes(
        @Schema(description = "预订单ID") Long orderId,
        @Schema(description = "本次是否新建学生账号") boolean studentCreated,
        @Schema(description = "学生登录手机号") String studentPhone,
        @Schema(description = "一次性初始密码，仅新建学生时返回") String initialPassword
) {
}
