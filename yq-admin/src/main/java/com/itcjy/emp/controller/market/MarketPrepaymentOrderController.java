package com.itcjy.emp.controller.market;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderCreateReq;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderPageReq;
import com.itcjy.emp.pojo.req.market.MarketPrepaymentOrderUpdateReq;
import com.itcjy.emp.pojo.res.market.MarketEducationOptionRes;
import com.itcjy.emp.pojo.res.market.MarketPrepaymentOrderCreateRes;
import com.itcjy.emp.pojo.res.market.MarketPrepaymentOrderRes;
import com.itcjy.emp.pojo.res.market.MarketProductOptionRes;
import com.itcjy.emp.pojo.res.market.MarketSalespersonOptionRes;
import com.itcjy.emp.service.market.IMarketPrepaymentOrderService;
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

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/prepaymentOrder")
@Tag(name = "预订单管理", description = "销售预订单及学生自动建档接口")
public class MarketPrepaymentOrderController {

    private final IMarketPrepaymentOrderService prepaymentOrderService;

    @Operation(summary = "分页查询预订单", description = "支持学生、联系方式、产品、销售和院校关键字搜索")
    @GetMapping("/page")
    @HasPermission(code = "market:prepayment-order:page", name = "分页查询预订单", description = "分页查询销售预订单")
    public ApiResponse<PageResult<MarketPrepaymentOrderRes>> pageOrders(
            @Valid @ParameterObject MarketPrepaymentOrderPageReq req) {
        return ApiResponse.success(prepaymentOrderService.pageOrders(req));
    }

    @Operation(summary = "查询预订单详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "market:prepayment-order:get", name = "查询预订单详情", description = "根据ID查询预订单")
    public ApiResponse<MarketPrepaymentOrderRes> getOrder(
            @Parameter(description = "预订单ID", required = true)
            @PathVariable @NotNull(message = "预订单ID不能为空") Long id) {
        return ApiResponse.success(prepaymentOrderService.getOrderDetail(id));
    }

    @Operation(summary = "查询销售人员下拉选项", description = "仅返回启用且拥有SALES角色的用户")
    @GetMapping("/salespersonOptions")
    @HasPermission(code = "market:prepayment-order:sales-options", name = "查询销售人员选项", description = "查询SALES角色用户")
    public ApiResponse<List<MarketSalespersonOptionRes>> listSalespersonOptions() {
        return ApiResponse.success(prepaymentOrderService.listSalespersonOptions());
    }

    @Operation(summary = "查询意向产品下拉选项")
    @GetMapping("/productOptions")
    @HasPermission(code = "market:prepayment-order:product-options", name = "查询预订单产品选项", description = "查询预订单可选产品")
    public ApiResponse<List<MarketProductOptionRes>> listProductOptions() {
        return ApiResponse.success(prepaymentOrderService.listProductOptions());
    }

    @Operation(summary = "查询学历下拉选项", description = "读取当前启用的学历规则配置")
    @GetMapping("/educationOptions")
    @HasPermission(code = "market:prepayment-order:education-options", name = "查询预订单学历选项", description = "查询启用的学历规则")
    public ApiResponse<List<MarketEducationOptionRes>> listEducationOptions() {
        return ApiResponse.success(prepaymentOrderService.listEducationOptions());
    }

    @Operation(summary = "新增预订单", description = "保存预订单，并在手机号不存在时自动创建学生账号")
    @PostMapping("/add")
    @HasPermission(code = "market:prepayment-order:add", name = "新增预订单", description = "新增预订单并自动创建学生")
    public ApiResponse<MarketPrepaymentOrderCreateRes> addOrder(
            @Valid @RequestBody MarketPrepaymentOrderCreateReq req) {
        return ApiResponse.success("新增成功", prepaymentOrderService.addOrder(req));
    }

    @Operation(summary = "修改预订单")
    @PutMapping("/update/{id}")
    @HasPermission(code = "market:prepayment-order:update", name = "修改预订单", description = "修改预订单及意向快照")
    public ApiResponse<Void> updateOrder(
            @Parameter(description = "预订单ID", required = true)
            @PathVariable @NotNull(message = "预订单ID不能为空") Long id,
            @Valid @RequestBody MarketPrepaymentOrderUpdateReq req) {
        prepaymentOrderService.updateOrder(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "删除预订单")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "market:prepayment-order:delete", name = "删除预订单", description = "删除预订单但保留学生账号")
    public ApiResponse<Void> deleteOrder(
            @Parameter(description = "预订单ID", required = true)
            @PathVariable @NotNull(message = "预订单ID不能为空") Long id) {
        prepaymentOrderService.deleteOrder(id);
        return ApiResponse.success("删除成功");
    }
}
