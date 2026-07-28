package com.itcjy.stu.controller;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.res.pay.AlipayTradeCreateRes;
import com.itcjy.emp.service.pay.IAlipayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/stu/payments")
@Tag(name = "Student payments")
public class StudentPaymentController {

    private final IAlipayService alipayService;

    @Operation(summary = "Create Alipay payment for the current student's prepayment order")
    @PostMapping("/{prepaymentOrderId}/alipay")
    public ApiResponse<AlipayTradeCreateRes> createAlipayTrade(
            @Parameter(required = true) @PathVariable @NotNull Long prepaymentOrderId) {
        return ApiResponse.success(alipayService.createTradePagePay(prepaymentOrderId));
    }
}
