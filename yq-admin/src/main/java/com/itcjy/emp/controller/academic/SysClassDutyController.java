package com.itcjy.emp.controller.academic;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.academic.SysClassDutyDailyReq;
import com.itcjy.emp.pojo.req.academic.SysClassDutySaveReq;
import com.itcjy.emp.pojo.res.academic.SysClassDutyDailyRes;
import com.itcjy.emp.pojo.res.academic.SysClassDutyOptionsRes;
import com.itcjy.emp.service.academic.ISysClassDutyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/class-duties")
@Tag(name = "值班管理", description = "按日期和校区维护教师值班安排")
public class SysClassDutyController {

    private final ISysClassDutyService classDutyService;

    @GetMapping("/options")
    @Operation(summary = "查询值班管理选项")
    @HasPermission(code = "sys:duty:options", name = "查询值班选项", description = "查询校区、讲师和值班类型")
    public ApiResponse<SysClassDutyOptionsRes> listOptions() {
        return ApiResponse.success(classDutyService.listOptions());
    }

    @GetMapping("/daily")
    @Operation(summary = "查询校区每日值班")
    @HasPermission(code = "sys:duty:list", name = "查看值班安排", description = "按日期和校区查看值班安排")
    public ApiResponse<SysClassDutyDailyRes> getDailyDuties(
            @Valid @ParameterObject SysClassDutyDailyReq req) {
        return ApiResponse.success(classDutyService.getDailyDuties(req));
    }

    @PutMapping
    @Operation(summary = "保存值班安排", description = "按校区、日期、类型和班级新增或覆盖值班安排")
    @HasPermission(code = "sys:duty:save", name = "保存值班安排", description = "新增或修改值班老师及备注")
    public ApiResponse<Void> saveDuty(@Valid @RequestBody SysClassDutySaveReq req) {
        classDutyService.saveDuty(req);
        return ApiResponse.success("保存成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "清空值班安排")
    @HasPermission(code = "sys:duty:delete", name = "清空值班安排", description = "删除一条值班安排")
    public ApiResponse<Void> deleteDuty(
            @Parameter(description = "值班ID", required = true)
            @PathVariable @NotNull(message = "值班ID不能为空")
            @Positive(message = "值班ID必须大于0") Long id) {
        classDutyService.deleteDuty(id);
        return ApiResponse.success("已清空值班安排");
    }
}
