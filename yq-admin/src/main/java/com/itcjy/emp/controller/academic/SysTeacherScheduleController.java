package com.itcjy.emp.controller.academic;

import com.itcjy.common.annotations.HasPermission;
import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.emp.pojo.req.academic.SysTeacherScheduleCalendarReq;
import com.itcjy.emp.pojo.res.academic.SysTeacherScheduleCalendarRes;
import com.itcjy.emp.service.academic.ISysTeacherScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/emp/teacher-schedules")
@Tag(name = "教师课程表", description = "按教师和月份查询课程安排")
public class SysTeacherScheduleController {

    private final ISysTeacherScheduleService teacherScheduleService;

    @GetMapping("/calendar")
    @Operation(summary = "查询教师月度课程表", description = "返回教师选项、当前教师课程和本月无课教师")
    @HasPermission(code = "sys:teacher:schedule:calendar", name = "查看教师课程表", description = "按教师和月份查看课程安排")
    public ApiResponse<SysTeacherScheduleCalendarRes> getCalendar(
            @Valid @ParameterObject SysTeacherScheduleCalendarReq req) {
        return ApiResponse.success(teacherScheduleService.getTeacherCalendar(req));
    }
}
