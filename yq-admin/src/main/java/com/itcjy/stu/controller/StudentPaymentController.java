package com.itcjy.stu.controller;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.res.pay.AlipayTradeCreateRes;
import com.itcjy.emp.pojo.req.pay.PaymentRefundCreateReq;
import com.itcjy.emp.pojo.res.pay.PaymentRefundRes;
import com.itcjy.emp.service.pay.IAlipayService;
import com.itcjy.emp.service.pay.IPaymentRefundService;
import com.itcjy.stu.pojo.VO.StudentPaymentDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/stu/payments")
@Tag(name = "Student payments")
public class StudentPaymentController {

    private final IAlipayService alipayService;
    private final IPaymentRefundService paymentRefundService;

    @Operation(summary = "Create Alipay payment for the current student's prepayment order")
    @PostMapping("/{prepaymentOrderId}/alipay")
    public ApiResponse<AlipayTradeCreateRes> createAlipayTrade(
            @Parameter(required = true) @PathVariable @NotNull Long prepaymentOrderId) {
        return ApiResponse.success(alipayService.createTradePagePay(prepaymentOrderId));
    }

    @Operation(summary = "Get current student's payment detail")
    @GetMapping("/{paymentOrderId}")
    public ApiResponse<StudentPaymentDetailVO> getPaymentDetail(
            @Parameter(required = true) @PathVariable @NotNull Long paymentOrderId) {
        return ApiResponse.success(paymentRefundService.getCurrentStudentPaymentDetail(paymentOrderId));
    }

    @Operation(summary = "Create a full refund request for a paid order")
    @PostMapping("/{paymentOrderId}/refund-requests")
    public ApiResponse<PaymentRefundRes> createRefundRequest(
            @Parameter(required = true) @PathVariable @NotNull Long paymentOrderId,
            @Valid @RequestBody PaymentRefundCreateReq request) {
        return ApiResponse.success(paymentRefundService.createCurrentStudentRefundRequest(paymentOrderId, request));
    }
}
