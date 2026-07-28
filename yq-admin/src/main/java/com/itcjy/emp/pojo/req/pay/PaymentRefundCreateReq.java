package com.itcjy.emp.pojo.req.pay;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRefundCreateReq(
        @NotBlank(message = "Refund reason is required")
        @Size(max = 500, message = "Refund reason is too long")
        String reason
) {
}
