package com.itcjy.emp.controller.academic;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.SysCourse;
import com.itcjy.emp.pojo.req.academic.SysCoursePageReq;
import com.itcjy.emp.pojo.req.academic.SysCourseReq;
import com.itcjy.emp.pojo.req.academic.SysCourseUpdateReq;
import com.itcjy.emp.service.academic.ISysCourseService;
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
@RequestMapping("/emp/sysCourse")
@Tag(name = "课程管理", description = "课程相关接口")
public class SysCourseController {

    @Resource
    private ISysCourseService sysCourseService;

    @Operation(summary = "新增课程", description = "新增课程信息")
    @PostMapping("/add")
    @HasPermission(code = "sys:course:add", name = "新增课程", description = "新增课程信息")
    public ApiResponse<Void> addCourse(@Valid @RequestBody SysCourseReq req) {
        sysCourseService.addCourse(req);
        return ApiResponse.success("新增成功");
    }

    @Operation(summary = "删除课程", description = "根据ID删除课程")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "sys:course:delete", name = "删除课程", description = "根据ID删除课程")
    public ApiResponse<Void> deleteCourse(@Parameter(description = "课程ID", required = true)
                                          @PathVariable @NotNull(message = "课程ID不能为空") Long id) {
        sysCourseService.deleteCourse(id);
        return ApiResponse.success("删除成功");
    }

    @Operation(summary = "修改课程", description = "根据ID修改课程信息")
    @PutMapping("/update/{id}")
    @HasPermission(code = "sys:course:update", name = "修改课程", description = "根据ID修改课程信息")
    public ApiResponse<Void> updateCourse(@Parameter(description = "课程ID", required = true)
                                          @PathVariable @NotNull(message = "课程ID不能为空") Long id,
                                          @Valid @RequestBody SysCourseUpdateReq req) {
        sysCourseService.updateCourse(id, req);
        return ApiResponse.success("修改成功");
    }

    @Operation(summary = "查询课程详情", description = "根据ID查询课程详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "sys:course:get", name = "查询课程详情", description = "根据ID查询课程详情")
    public ApiResponse<SysCourse> getCourse(@Parameter(description = "课程ID", required = true)
                                            @PathVariable @NotNull(message = "课程ID不能为空") Long id) {
        SysCourse course = sysCourseService.getById(id);
        if (course == null) {
            throw BusinessException.COURSE_NOT_EXIST.newInstance("课程不存在");
        }
        return ApiResponse.success(course);
    }

    @Operation(summary = "分页查询课程", description = "支持课程名称模糊查询、上课方式精确筛选")
    @GetMapping("/page")
    @HasPermission(code = "sys:course:page", name = "分页查询课程", description = "分页查询课程列表")
    public ApiResponse<PageResult<SysCourse>> pageCourse(@Valid @ParameterObject SysCoursePageReq req) {
        return ApiResponse.success(sysCourseService.pageCourse(req));
    }
}
