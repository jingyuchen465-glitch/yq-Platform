package com.itcjy.emp.controller.pay;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.pay.OrderPaymentCreateReq;
import com.itcjy.emp.pojo.req.pay.OrderPaymentPageReq;
import com.itcjy.emp.pojo.req.pay.OrderPaymentUpdateReq;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/orderPayment")
@Tag(name = "支付订单管理", description = "支付订单CRUD接口")
public class OrderPaymentController {

    private final IOrderPaymentService orderPaymentService;

    @Operation(summary = "分页查询支付订单", description = "支持按学生手机号、订单号、状态精确过滤")
    @GetMapping("/page")
    @HasPermission(code = "pay:order-payment:page", name = "分页查询支付订单", description = "分页查询支付订单列表")
    public ApiResponse<PageResult<OrderPaymentRes>> pageOrders(
            @Valid @ParameterObject OrderPaymentPageReq req) {
        return ApiResponse.success(orderPaymentService.pageOrderPayments(req));
    }

    @Operation(summary = "查询支付订单详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "pay:order-payment:get", name = "查询支付订单详情", description = "根据ID查询支付订单")
    public ApiResponse<OrderPaymentRes> getOrder(
            @Parameter(description = "支付订单ID", required = true)
            @PathVariable @NotNull(message = "支付订单ID不能为空") Long id) {
        return ApiResponse.success(orderPaymentService.getOrderPaymentDetail(id));
    }

    @Operation(summary = "新增支付订单")
    @PostMapping("/add")
    @HasPermission(code = "pay:order-payment:add", name = "新增支付订单", description = "手动录入支付订单")
    public ApiResponse<OrderPaymentRes> addOrder(
            @Valid @RequestBody OrderPaymentCreateReq req) {
        return ApiResponse.success("新增成功", orderPaymentService.addOrderPayment(req));
    }

    @Operation(summary = "修改支付订单", description = "支持修改状态、退款金额、渠道订单号及支付成功时间")
    @PutMapping("/update/{id}")
    @HasPermission(code = "pay:order-payment:update", name = "修改支付订单", description = "修改支付订单状态及退款信息")
    public ApiResponse<Void> updateOrder(
            @Parameter(description = "支付订单ID", required = true)
            @PathVariable @NotNull(message = "支付订单ID不能为空") Long id,
            @Valid @RequestBody OrderPaymentUpdateReq req) {
        orderPaymentService.updateOrderPayment(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "删除支付订单")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "pay:order-payment:delete", name = "删除支付订单", description = "根据ID删除支付订单")
    public ApiResponse<Void> deleteOrder(
            @Parameter(description = "支付订单ID", required = true)
            @PathVariable @NotNull(message = "支付订单ID不能为空") Long id) {
        orderPaymentService.deleteOrderPayment(id);
        return ApiResponse.success("删除成功");
    }
}
