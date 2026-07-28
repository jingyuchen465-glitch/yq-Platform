package com.itcjy.emp.controller.pay;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.pay.PaymentRefundPageReq;
import com.itcjy.emp.pojo.req.pay.PaymentRefundReviewReq;
import com.itcjy.emp.pojo.res.pay.PaymentRefundRes;
import com.itcjy.emp.service.pay.IPaymentRefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/emp/payment-refunds")
public class PaymentRefundController {

    private final IPaymentRefundService paymentRefundService;

    @GetMapping("/page")
    @HasPermission(code = "pay:refund:page", name = "Page refund requests", description = "Page refund requests")
    public ApiResponse<PageResult<PaymentRefundRes>> pageRefundRequests(@Valid @ParameterObject PaymentRefundPageReq request) {
        return ApiResponse.success(paymentRefundService.pageRefundRequests(request));
    }

    @PostMapping("/{id}/approve")
    @HasPermission(code = "pay:refund:approve", name = "Approve refund request", description = "Approve refund request")
    public ApiResponse<PaymentRefundRes> approveRefundRequest(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody PaymentRefundReviewReq request) {
        return ApiResponse.success(paymentRefundService.approveRefundRequest(id, request));
    }

    @PostMapping("/{id}/reject")
    @HasPermission(code = "pay:refund:reject", name = "Reject refund request", description = "Reject refund request")
    public ApiResponse<Void> rejectRefundRequest(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody PaymentRefundReviewReq request) {
        paymentRefundService.rejectRefundRequest(id, request);
        return ApiResponse.success();
    }
}
