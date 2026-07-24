package com.itcjy.emp.controller.system;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.system.ClassScheduleRuleReq;
import com.itcjy.emp.pojo.req.system.SysConfigItemPageReq;
import com.itcjy.emp.pojo.req.system.SysConfigItemReq;
import com.itcjy.emp.pojo.req.system.SysConfigTypePageReq;
import com.itcjy.emp.pojo.req.system.SysConfigTypeReq;
import com.itcjy.emp.pojo.res.system.ClassScheduleRuleRes;
import com.itcjy.emp.pojo.res.system.SysConfigItemRes;
import com.itcjy.emp.pojo.res.system.SysConfigTypeRes;
import com.itcjy.emp.service.system.ISysConfigService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/emp/sysConfig")
@Tag(name = "系统配置管理", description = "动态配置类型、配置项及排课规则维护接口")
public class SysConfigController {

    private final ISysConfigService sysConfigService;

    @PostMapping("/type/add")
    @Operation(summary = "新增配置类型")
    @HasPermission(code = "sys:config:type:add", name = "新增配置类型", description = "新增系统配置类型")
    public ApiResponse<Void> addType(@Valid @RequestBody SysConfigTypeReq req) {
        sysConfigService.addType(req);
        return ApiResponse.success("新增成功");
    }

    @PutMapping("/type/update/{id}")
    @Operation(summary = "修改配置类型")
    @HasPermission(code = "sys:config:type:update", name = "修改配置类型", description = "修改系统配置类型")
    public ApiResponse<Void> updateType(@PathVariable @NotNull Long id,
                                        @Valid @RequestBody SysConfigTypeReq req) {
        sysConfigService.updateType(id, req);
        return ApiResponse.success("修改成功");
    }

    @DeleteMapping("/type/delete/{id}")
    @Operation(summary = "删除配置类型")
    @HasPermission(code = "sys:config:type:delete", name = "删除配置类型", description = "删除系统配置类型及其配置项")
    public ApiResponse<Void> deleteType(@PathVariable @NotNull Long id) {
        sysConfigService.deleteType(id);
        return ApiResponse.success("删除成功");
    }

    @GetMapping("/type/get/{id}")
    @Operation(summary = "查询配置类型详情")
    @HasPermission(code = "sys:config:type:get", name = "查询配置类型详情", description = "查询系统配置类型详情")
    public ApiResponse<SysConfigTypeRes> getType(@PathVariable @NotNull Long id) {
        return ApiResponse.success(sysConfigService.getType(id));
    }

    @GetMapping("/type/page")
    @Operation(summary = "分页查询配置类型")
    @HasPermission(code = "sys:config:type:page", name = "分页查询配置类型", description = "分页查询系统配置类型")
    public ApiResponse<PageResult<SysConfigTypeRes>> pageTypes(@Valid @ParameterObject SysConfigTypePageReq req) {
        return ApiResponse.success(sysConfigService.pageTypes(req));
    }

    @GetMapping("/type/list")
    @Operation(summary = "查询配置类型列表")
    @HasPermission(code = "sys:config:type:list", name = "查询配置类型列表", description = "查询系统配置类型列表")
    public ApiResponse<List<SysConfigTypeRes>> listTypes() {
        return ApiResponse.success(sysConfigService.listTypes());
    }

    @PostMapping("/item/add")
    @Operation(summary = "新增配置项")
    @HasPermission(code = "sys:config:item:add", name = "新增配置项", description = "新增系统配置项")
    public ApiResponse<Void> addItem(@Valid @RequestBody SysConfigItemReq req) {
        sysConfigService.addItem(req);
        return ApiResponse.success("新增成功");
    }

    @PutMapping("/item/update/{id}")
    @Operation(summary = "修改配置项")
    @HasPermission(code = "sys:config:item:update", name = "修改配置项", description = "修改系统配置项")
    public ApiResponse<Void> updateItem(@PathVariable @NotNull Long id,
                                        @Valid @RequestBody SysConfigItemReq req) {
        sysConfigService.updateItem(id, req);
        return ApiResponse.success("修改成功");
    }

    @DeleteMapping("/item/delete/{id}")
    @Operation(summary = "删除配置项")
    @HasPermission(code = "sys:config:item:delete", name = "删除配置项", description = "删除系统配置项")
    public ApiResponse<Void> deleteItem(@PathVariable @NotNull Long id) {
        sysConfigService.deleteItem(id);
        return ApiResponse.success("删除成功");
    }

    @GetMapping("/item/get/{id}")
    @Operation(summary = "查询配置项详情")
    @HasPermission(code = "sys:config:item:get", name = "查询配置项详情", description = "查询系统配置项详情")
    public ApiResponse<SysConfigItemRes> getItem(@PathVariable @NotNull Long id) {
        return ApiResponse.success(sysConfigService.getItem(id));
    }

    @GetMapping("/item/page")
    @Operation(summary = "分页查询配置项")
    @HasPermission(code = "sys:config:item:page", name = "分页查询配置项", description = "分页查询系统配置项")
    public ApiResponse<PageResult<SysConfigItemRes>> pageItems(@Valid @ParameterObject SysConfigItemPageReq req) {
        return ApiResponse.success(sysConfigService.pageItems(req));
    }

    @GetMapping("/item/list/{typeId}")
    @Operation(summary = "按类型查询配置项")
    @HasPermission(code = "sys:config:item:list", name = "查询配置项列表", description = "按类型查询系统配置项")
    public ApiResponse<List<SysConfigItemRes>> listItems(@PathVariable @NotNull Long typeId) {
        return ApiResponse.success(sysConfigService.listItems(typeId));
    }

    @GetMapping("/class-schedule-rule")
    @Operation(summary = "查询排课规则")
    @HasPermission(code = "sys:config:schedule:get", name = "查询排课规则", description = "查询当前生效的排课规则")
    public ApiResponse<ClassScheduleRuleRes> getClassScheduleRule() {
        return ApiResponse.success(sysConfigService.getClassScheduleRule());
    }

    @PutMapping("/class-schedule-rule")
    @Operation(summary = "修改排课规则")
    @HasPermission(code = "sys:config:schedule:update", name = "修改排课规则", description = "整组修改当前生效的排课规则")
    public ApiResponse<Void> updateClassScheduleRule(@Valid @RequestBody ClassScheduleRuleReq req) {
        sysConfigService.updateClassScheduleRule(req);
        return ApiResponse.success("排课规则修改成功");
    }
}
