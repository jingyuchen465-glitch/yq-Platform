package com.itcjy.emp.controller;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.SysClassScheduleGenerateReq;
import com.itcjy.emp.pojo.req.SysClassScheduleTeacherAssignReq;
import com.itcjy.emp.pojo.req.SysClassScheduleTemporaryCourseReq;
import com.itcjy.emp.pojo.req.SysClassScheduleUpdateReq;
import com.itcjy.emp.pojo.res.SysClassScheduleGenerateRes;
import com.itcjy.emp.pojo.res.SysClassScheduleRes;
import com.itcjy.emp.pojo.res.SysClassScheduleTeacherAssignRes;
import com.itcjy.emp.pojo.res.SysClassScheduleTeacherAssignmentOptionsRes;
import com.itcjy.emp.pojo.res.SysClassScheduleTemporaryCourseOptionsRes;
import com.itcjy.emp.pojo.res.SysClassScheduleTemporaryCourseRes;
import com.itcjy.emp.service.ISysClassScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @Operation(summary = "查询单条课程日程", description = "查询日历中指定课程格子的详细信息")
    @GetMapping("/get/{scheduleId}")
    @HasPermission(code = "sys:class:schedule:get", name = "查询课程日程详情", description = "查询单条课程日程")
    public ApiResponse<SysClassScheduleRes> getSchedule(
            @PathVariable
            @Positive(message = "课表ID必须大于0") Long scheduleId) {
        return ApiResponse.success(sysClassScheduleService.getSchedule(scheduleId));
    }

    @Operation(summary = "修改单条课程日程", description = "修改指定上课日程的阶段和授课教师")
    @PutMapping("/update/{scheduleId}")
    @HasPermission(code = "sys:class:schedule:update", name = "修改课程日程", description = "修改单条课程日程阶段和教师")
    public ApiResponse<SysClassScheduleRes> updateSchedule(
            @PathVariable
            @Positive(message = "课表ID必须大于0") Long scheduleId,
            @Valid @RequestBody SysClassScheduleUpdateReq req) {
        return ApiResponse.success("课程日程修改成功", sysClassScheduleService.updateSchedule(scheduleId, req));
    }

    @Operation(summary = "删除单条课程日程", description = "删除日历中指定的上课日程，不移动其他课程")
    @DeleteMapping("/delete/{scheduleId}")
    @HasPermission(code = "sys:class:schedule:delete", name = "删除课程日程", description = "删除单条课程日程")
    public ApiResponse<Void> deleteSchedule(
            @PathVariable
            @Positive(message = "课表ID必须大于0") Long scheduleId) {
        sysClassScheduleService.deleteSchedule(scheduleId);
        return ApiResponse.success("课程日程删除成功");
    }

    @Operation(summary = "生成班级课程表", description = "按照排课规则和法定节假日生成并覆盖班级课程表")
    @PostMapping("/generate")
    @HasPermission(code = "sys:class:schedule:generate", name = "生成课程表", description = "生成并覆盖班级课程表")
    public ApiResponse<SysClassScheduleGenerateRes> generateSchedule(
            @Valid @RequestBody SysClassScheduleGenerateReq req) {
        return ApiResponse.success("课程表生成成功", sysClassScheduleService.generateSchedule(req));
    }

    @Operation(summary = "查询阶段教师分配选项", description = "返回课程阶段、讲师及讲师在其他班级已占用的上课日期")
    @GetMapping("/teacherAssignmentOptions/{classId}")
    @HasPermission(code = "sys:class:schedule:teacher:options", name = "查询课表教师选项", description = "查询按阶段分配教师所需数据")
    public ApiResponse<SysClassScheduleTeacherAssignmentOptionsRes> listTeacherAssignmentOptions(
            @PathVariable
            @Positive(message = "班级ID必须大于0") Long classId) {
        return ApiResponse.success(sysClassScheduleService.listTeacherAssignmentOptions(classId));
    }

    @Operation(summary = "按阶段分配授课教师", description = "校验教师授课时间冲突后，批量更新指定阶段的课表教师")
    @PutMapping("/assignTeacher")
    @HasPermission(code = "sys:class:schedule:teacher:assign", name = "分配课表教师", description = "按课程阶段批量分配授课教师")
    public ApiResponse<SysClassScheduleTeacherAssignRes> assignTeacherByStage(
            @Valid @RequestBody SysClassScheduleTeacherAssignReq req) {
        return ApiResponse.success("教师分配成功", sysClassScheduleService.assignTeacherByStage(req));
    }

    @Operation(summary = "查询临时加课选项", description = "返回当前班级课程详情和有效讲师")
    @GetMapping("/temporaryCourseOptions/{classId}")
    @HasPermission(code = "sys:class:schedule:temporary:options", name = "查询临时加课选项", description = "查询临时加课课程和教师选项")
    public ApiResponse<SysClassScheduleTemporaryCourseOptionsRes> listTemporaryCourseOptions(
            @PathVariable
            @Positive(message = "班级ID必须大于0") Long classId) {
        return ApiResponse.success(sysClassScheduleService.listTemporaryCourseOptions(classId));
    }

    @Operation(summary = "临时增加课程", description = "非上课日直接加课，上课日将原课程及后续课程依次顺延")
    @PostMapping("/temporaryCourse")
    @HasPermission(code = "sys:class:schedule:temporary:add", name = "临时加课", description = "在班级课表中临时增加课程")
    public ApiResponse<SysClassScheduleTemporaryCourseRes> addTemporaryCourse(
            @Valid @RequestBody SysClassScheduleTemporaryCourseReq req) {
        return ApiResponse.success("临时加课成功", sysClassScheduleService.addTemporaryCourse(req));
    }
}
