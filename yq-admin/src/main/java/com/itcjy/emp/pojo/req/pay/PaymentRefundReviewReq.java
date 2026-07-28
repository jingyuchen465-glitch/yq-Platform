package com.itcjy.emp.pojo.req.pay;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentRefundReviewReq(
        @NotBlank(message = "Review remark is required")
        @Size(max = 500, message = "Review remark is too long")
        String remark
) {
}
