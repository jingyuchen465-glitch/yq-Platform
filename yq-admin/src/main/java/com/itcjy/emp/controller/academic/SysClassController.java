package com.itcjy.emp.controller.academic;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.academic.SysClassPageReq;
import com.itcjy.emp.pojo.req.academic.SysClassReq;
import com.itcjy.emp.pojo.req.academic.SysClassUpdateReq;
import com.itcjy.emp.pojo.res.academic.SysClassFormOptionsRes;
import com.itcjy.emp.pojo.res.academic.SysClassRes;
import com.itcjy.emp.service.academic.ISysClassService;
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
@RequestMapping("/emp/sysClass")
@Tag(name = "班级管理", description = "班级相关接口")
public class SysClassController {

    private final ISysClassService sysClassService;

    @Operation(summary = "新增班级")
    @PostMapping("/add")
    @HasPermission(code = "sys:class:add", name = "新增班级", description = "新增班级信息")
    public ApiResponse<Void> addClass(@Valid @RequestBody SysClassReq req) {
        sysClassService.addClass(req);
        return ApiResponse.success("新增成功");
    }

    @Operation(summary = "修改班级")
    @PutMapping("/update/{id}")
    @HasPermission(code = "sys:class:update", name = "修改班级", description = "根据ID修改班级信息")
    public ApiResponse<Void> updateClass(
            @Parameter(description = "班级ID", required = true)
            @PathVariable @NotNull(message = "班级ID不能为空") Long id,
            @Valid @RequestBody SysClassUpdateReq req) {
        sysClassService.updateClass(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "删除班级")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "sys:class:delete", name = "删除班级", description = "根据ID删除班级")
    public ApiResponse<Void> deleteClass(
            @Parameter(description = "班级ID", required = true)
            @PathVariable @NotNull(message = "班级ID不能为空") Long id) {
        sysClassService.deleteClass(id);
        return ApiResponse.success("删除成功");
    }

    @Operation(summary = "查询班级详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "sys:class:get", name = "查询班级详情", description = "根据ID查询班级详情")
    public ApiResponse<SysClassRes> getClassDetail(
            @Parameter(description = "班级ID", required = true)
            @PathVariable @NotNull(message = "班级ID不能为空") Long id) {
        return ApiResponse.success(sysClassService.getClassDetail(id));
    }

    @Operation(summary = "分页查询班级")
    @GetMapping("/page")
    @HasPermission(code = "sys:class:page", name = "分页查询班级", description = "分页查询班级列表")
    public ApiResponse<PageResult<SysClassRes>> pageClasses(@Valid @ParameterObject SysClassPageReq req) {
        return ApiResponse.success(sysClassService.pageClasses(req));
    }

    @Operation(summary = "查询班级表单下拉选项", description = "返回全部校区、COORDINATOR角色用户和线下课程")
    @GetMapping("/formOptions")
    @HasPermission(code = "sys:class:options", name = "查询班级下拉选项", description = "查询班级表单关联数据")
    public ApiResponse<SysClassFormOptionsRes> listFormOptions() {
        return ApiResponse.success(sysClassService.listFormOptions());
    }
}
