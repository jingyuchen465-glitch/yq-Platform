package com.itcjy.emp.controller;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.SysClassScheduleGenerateReq;
import com.itcjy.emp.pojo.res.SysClassScheduleGenerateRes;
import com.itcjy.emp.pojo.res.SysClassScheduleRes;
import com.itcjy.emp.service.ISysClassScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/sysClassSchedule")
@Tag(name = "课程表管理", description = "班级课程表相关接口")
public class SysClassScheduleController {

    private final ISysClassScheduleService sysClassScheduleService;

    @Operation(summary = "查询班级课程表", description = "按日期升序返回指定班级的完整课程表")
    @GetMapping("/list/{classId}")
    @HasPermission(code = "sys:class:schedule:list", name = "查看课程表", description = "查看班级月历课程表")
    public ApiResponse<List<SysClassScheduleRes>> listSchedule(
            @PathVariable
            @Positive(message = "班级ID必须大于0") Long classId) {
        return ApiResponse.success(sysClassScheduleService.listSchedule(classId));
    }

    @Operation(summary = "生成班级课程表", description = "按照排课规则和法定节假日生成并覆盖班级课程表")
    @PostMapping("/generate")
    @HasPermission(code = "sys:class:schedule:generate", name = "生成课程表", description = "生成并覆盖班级课程表")
    public ApiResponse<SysClassScheduleGenerateRes> generateSchedule(
            @Valid @RequestBody SysClassScheduleGenerateReq req) {
        return ApiResponse.success("课程表生成成功", sysClassScheduleService.generateSchedule(req));
    }
}
