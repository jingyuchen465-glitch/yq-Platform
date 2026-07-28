package com.itcjy.emp.pojo.req.pay;

import com.itcjy.common.pojo.BasePageReq;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentRefundPageReq extends BasePageReq {

    @Size(max = 16, message = "Refund status is too long")
    private String status;

    @Size(max = 64, message = "Keyword is too long")
    private String keyword;
}
