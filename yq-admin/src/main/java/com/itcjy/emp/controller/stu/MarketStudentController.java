package com.itcjy.emp.controller.stu;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.stu.StudentPageReq;
import com.itcjy.emp.pojo.req.stu.StudentUpdateReq;
import com.itcjy.emp.pojo.res.stu.StudentClassOptionRes;
import com.itcjy.emp.pojo.res.stu.StudentRes;
import com.itcjy.emp.service.stu.IStudentService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/marketStudent")
@Tag(name = "学员管理", description = "产品运营学员资料与状态管理接口")
public class MarketStudentController {

    private final IStudentService studentService;

    @Operation(summary = "分页查询学员")
    @GetMapping("/page")
    @HasPermission(code = "market:student:page", name = "分页查询学员", description = "分页筛选学员资料")
    public ApiResponse<PageResult<StudentRes>> pageStudents(@Valid @ParameterObject StudentPageReq req) {
        return ApiResponse.success(studentService.pageStudents(req));
    }

    @Operation(summary = "查询学员详情")
    @GetMapping("/get/{id}")
    @HasPermission(code = "market:student:get", name = "查询学员详情", description = "根据ID查询学员资料")
    public ApiResponse<StudentRes> getStudent(
            @Parameter(description = "学员ID", required = true)
            @PathVariable @NotNull(message = "学员ID不能为空") Long id) {
        return ApiResponse.success(studentService.getStudentDetail(id));
    }

    @Operation(summary = "查询学员班级选项")
    @GetMapping("/classOptions")
    @HasPermission(code = "market:student:options", name = "查询学员班级选项", description = "查询学员编辑和筛选可用班级")
    public ApiResponse<List<StudentClassOptionRes>> listClassOptions() {
        return ApiResponse.success(studentService.listClassOptions());
    }

    @Operation(summary = "修改学员资料")
    @PutMapping("/update/{id}")
    @HasPermission(code = "market:student:update", name = "修改学员资料", description = "修改学员联系方式、班级和状态")
    public ApiResponse<Void> updateStudent(
            @Parameter(description = "学员ID", required = true)
            @PathVariable @NotNull(message = "学员ID不能为空") Long id,
            @Valid @RequestBody StudentUpdateReq req) {
        studentService.updateStudent(id, req);
        return ApiResponse.success("学员资料已更新");
    }

    @Operation(summary = "办理学员退学")
    @DeleteMapping("/delete/{id}")
    @HasPermission(code = "market:student:delete", name = "办理学员退学", description = "将学员状态更新为已退学并终止登录会话")
    public ApiResponse<Void> withdrawStudent(
            @Parameter(description = "学员ID", required = true)
            @PathVariable @NotNull(message = "学员ID不能为空") Long id) {
        studentService.withdrawStudent(id);
        return ApiResponse.success("学员已办理退学");
    }
}
