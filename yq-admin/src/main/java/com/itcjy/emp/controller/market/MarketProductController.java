package com.itcjy.emp.controller.market;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.market.MarketProductCreateReq;
import com.itcjy.emp.pojo.req.market.MarketProductPageReq;
import com.itcjy.emp.pojo.req.market.MarketProductUpdateReq;
import com.itcjy.emp.pojo.res.market.MarketCourseOptionRes;
import com.itcjy.emp.pojo.res.market.MarketProductRes;
import com.itcjy.emp.service.market.IMarketProductService;
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
@RequestMapping("/emp/marketProduct")
@Tag(name = "产品管理", description = "产品及其课程组合管理接口")
public class MarketProductController {

    private final IMarketProductService marketProductService;

    @Operation(summary = "分页查询产品", description = "支持按产品名称或课程名称模糊搜索")
    @GetMapping("/page")
    @HasPermission(code = "market:product:page", name = "分页查询产品", description = "分页查询产品及课程组合")
    public ApiResponse<PageResult<MarketProductRes>> pageProducts(@Valid @ParameterObject MarketProductPageReq req) {
        return ApiResponse.success(marketProductService.pageProducts(req));
    }

    @Operation(summary = "查询产品详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "market:product:get", name = "查询产品详情", description = "根据ID查询产品及关联课程")
    public ApiResponse<MarketProductRes> getProduct(
            @Parameter(description = "产品ID", required = true)
            @PathVariable @NotNull(message = "产品ID不能为空") Long id) {
        return ApiResponse.success(marketProductService.getProductDetail(id));
    }

    @Operation(summary = "查询产品可选课程")
    @GetMapping("/courseOptions")
    @HasPermission(code = "market:product:options", name = "查询产品可选课程", description = "查询产品表单的课程选项")
    public ApiResponse<List<MarketCourseOptionRes>> listCourseOptions() {
        return ApiResponse.success(marketProductService.listCourseOptions());
    }

    @Operation(summary = "新增产品")
    @PostMapping("/add")
    @HasPermission(code = "market:product:add", name = "新增产品", description = "新增产品及课程组合")
    public ApiResponse<Void> addProduct(@Valid @RequestBody MarketProductCreateReq req) {
        marketProductService.addProduct(req);
        return ApiResponse.success("新增成功");
    }

    @Operation(summary = "修改产品")
    @PutMapping("/update/{id}")
    @HasPermission(code = "market:product:update", name = "修改产品", description = "修改产品及课程组合")
    public ApiResponse<Void> updateProduct(
            @Parameter(description = "产品ID", required = true)
            @PathVariable @NotNull(message = "产品ID不能为空") Long id,
            @Valid @RequestBody MarketProductUpdateReq req) {
        marketProductService.updateProduct(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "删除产品")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "market:product:delete", name = "删除产品", description = "删除产品及课程关联")
    public ApiResponse<Void> deleteProduct(
            @Parameter(description = "产品ID", required = true)
            @PathVariable @NotNull(message = "产品ID不能为空") Long id) {
        marketProductService.deleteProduct(id);
        return ApiResponse.success("删除成功");
    }
}
