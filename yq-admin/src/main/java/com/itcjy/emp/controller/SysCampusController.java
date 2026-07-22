package com.itcjy.emp.controller;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysCampus;
import com.itcjy.emp.pojo.req.SysCampusPageReq;
import com.itcjy.emp.pojo.req.SysCampusReq;
import com.itcjy.emp.pojo.req.SysCampusUpdateReq;
import com.itcjy.emp.service.ISysCampusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@Validated
@RequestMapping("/emp/sysCampus")
@Tag(name = "校区管理", description = "校区相关接口")
public class SysCampusController {

    @Resource
    private ISysCampusService sysCampusService;

    @Operation(summary = "新增校区", description = "新增校区信息")
    @PostMapping("/add")
    @HasPermission(code = "sys:campus:add", name = "新增校区", description = "新增校区信息")
    public ApiResponse<Void> addCampus(@Valid @RequestBody SysCampusReq req) {
        sysCampusService.addCampus(req);
        return ApiResponse.success("新增成功");
    }

    @Operation(summary = "删除校区", description = "根据ID删除校区")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "sys:campus:delete", name = "删除校区", description = "根据ID删除校区")
    public ApiResponse<Void> deleteCampus(@Parameter(description = "校区ID", required = true)
                                          @PathVariable @NotNull(message = "校区ID不能为空") Long id) {
        sysCampusService.deleteCampus(id);
        return ApiResponse.success("删除成功");
    }

    @Operation(summary = "修改校区", description = "根据ID修改校区信息")
    @PutMapping("/update/{id}")
    @HasPermission(code = "sys:campus:update", name = "修改校区", description = "根据ID修改校区信息")
    public ApiResponse<Void> updateCampus(@Parameter(description = "校区ID", required = true)
                                          @PathVariable @NotNull(message = "校区ID不能为空") Long id,
                                          @Valid @RequestBody SysCampusUpdateReq req) {
        sysCampusService.updateCampus(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "查询校区详情", description = "根据ID查询校区详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "sys:campus:get", name = "查询校区详情", description = "根据ID查询校区详情")
    public ApiResponse<SysCampus> getCampus(@Parameter(description = "校区ID", required = true)
                                            @PathVariable @NotNull(message = "校区ID不能为空") Long id) {
        SysCampus campus = sysCampusService.getById(id);
        if (campus == null) {
            throw BusinessException.CAMPUS_NOT_EXIST.newInstance("校区不存在");
        }
        return ApiResponse.success(campus);
    }

    @Operation(summary = "分页查询校区", description = "支持校区地点、负责人模糊查询")
    @GetMapping("/page")
    @HasPermission(code = "sys:campus:page", name = "分页查询校区", description = "分页查询校区列表")
    public ApiResponse<PageResult<SysCampus>> pageCampus(@Valid @ParameterObject SysCampusPageReq req) {
        return ApiResponse.success(sysCampusService.pageCampus(req));
    }
}
