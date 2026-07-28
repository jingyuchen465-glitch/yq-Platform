package com.itcjy.emp.controller.pay;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.pay.OrderPaymentPageReq;
import com.itcjy.emp.pojo.res.pay.OrderPaymentRes;
import com.itcjy.emp.service.market.IOrderPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/orderPayment")
@Tag(name = "Payment order management")
public class OrderPaymentController {

    private final IOrderPaymentService orderPaymentService;

    @Operation(summary = "Page payment orders")
    @GetMapping("/page")
    @HasPermission(code = "pay:order-payment:page", name = "Page payment orders", description = "Page payment orders")
    public ApiResponse<PageResult<OrderPaymentRes>> pageOrders(@Valid @ParameterObject OrderPaymentPageReq req) {
        return ApiResponse.success(orderPaymentService.pageOrderPayments(req));
    }

    @Operation(summary = "Get payment order")
    @GetMapping("/get/{id}")
    @HasPermission(code = "pay:order-payment:get", name = "Get payment order", description = "Get payment order")
    public ApiResponse<OrderPaymentRes> getOrder(
            @Parameter(required = true) @PathVariable @NotNull Long id) {
        return ApiResponse.success(orderPaymentService.getOrderPaymentDetail(id));
    }
}
