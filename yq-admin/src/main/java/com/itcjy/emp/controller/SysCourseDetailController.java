package com.itcjy.emp.controller;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysCourseDetail;
import com.itcjy.emp.pojo.req.SysCourseDetailPageReq;
import com.itcjy.emp.pojo.req.SysCourseDetailReq;
import com.itcjy.emp.pojo.req.SysCourseDetailUpdateReq;
import com.itcjy.emp.service.ISysCourseDetailService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequestMapping("/emp/sysCourseDetail")
@Tag(name = "课程详情管理", description = "课程详情相关接口")
public class SysCourseDetailController {

    @Resource
    private ISysCourseDetailService sysCourseDetailService;

    @Operation(summary = "新增课程详情", description = "为指定课程新增详情记录")
    @PostMapping("/add")
    @HasPermission(code = "sys:course:detail:add", name = "新增课程详情", description = "新增课程详情记录")
    public ApiResponse<Void> addCourseDetail(@Valid @RequestBody SysCourseDetailReq req) {
        sysCourseDetailService.addCourseDetail(req);
        return ApiResponse.success("新增成功");
    }

    @Operation(summary = "删除课程详情", description = "根据ID删除课程详情记录")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "sys:course:detail:delete", name = "删除课程详情", description = "根据ID删除课程详情")
    public ApiResponse<Void> deleteCourseDetail(@Parameter(description = "课程详情ID", required = true)
                                                @PathVariable @NotNull(message = "课程详情ID不能为空") Long id) {
        sysCourseDetailService.deleteCourseDetail(id);
        return ApiResponse.success("删除成功");
    }

    @Operation(summary = "批量删除课程详情", description = "根据ID列表批量删除课程详情，并重新调整课程天数顺序")
    @DeleteMapping("/batchDelete")
    @HasPermission(code = "sys:course:detail:batchDelete", name = "批量删除课程详情", description = "批量删除课程详情记录")
    public ApiResponse<Void> batchDeleteCourseDetail(@RequestBody List<@NotNull(message = "课程详情ID不能为空") Long> ids) {
        sysCourseDetailService.deleteCourseDetails(ids);
        return ApiResponse.success("批量删除成功");
    }

    @Operation(summary = "修改课程详情", description = "根据ID修改课程详情记录")
    @PutMapping("/update/{id}")
    @HasPermission(code = "sys:course:detail:update", name = "修改课程详情", description = "根据ID修改课程详情")
    public ApiResponse<Void> updateCourseDetail(@Parameter(description = "课程详情ID", required = true)
                                                @PathVariable @NotNull(message = "课程详情ID不能为空") Long id,
                                                @Valid @RequestBody SysCourseDetailUpdateReq req) {
        sysCourseDetailService.updateCourseDetail(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "分页查询课程详情", description = "根据课程ID分页查询详情，支持阶段模糊搜索")
    @GetMapping("/page")
    @HasPermission(code = "sys:course:detail:page", name = "分页查询课程详情", description = "分页查询课程详情列表")
    public ApiResponse<PageResult<SysCourseDetail>> pageCourseDetail(@Valid @ParameterObject SysCourseDetailPageReq req) {
        return ApiResponse.success(sysCourseDetailService.pageCourseDetail(req));
    }

    @Operation(summary = "Excel导入课程详情", description = "上传 Excel 文件批量导入课程详情，列顺序：阶段 | 第几天 | 上课内容")
    @PostMapping("/import")
    @HasPermission(code = "sys:course:detail:import", name = "导入课程详情", description = "通过Excel批量导入课程详情")
    public ApiResponse<Integer> importCourseDetail(
            @Parameter(description = "课程ID", required = true)
            @RequestParam @NotNull(message = "课程ID不能为空") Long courseId,
            @Parameter(description = "Excel文件", required = true)
            @RequestParam("file") MultipartFile file) {
        int count = sysCourseDetailService.importCourseDetail(courseId, file);
        return ApiResponse.success("导入成功", count);
    }
}
