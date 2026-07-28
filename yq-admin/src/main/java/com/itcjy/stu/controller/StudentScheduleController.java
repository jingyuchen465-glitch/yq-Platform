package com.itcjy.stu.controller;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.stu.pojo.DTO.StudentScheduleQueryDTO;
import com.itcjy.stu.pojo.VO.StudentScheduleVO;
import com.itcjy.stu.service.StudentScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stu/schedules")
@Tag(name = "学生端课程表", description = "当前登录学生的班级课程表")
public class StudentScheduleController {

    private final StudentScheduleService studentScheduleService;

    @GetMapping
    @Operation(summary = "查询我的课程表", description = "未分班时返回 classAssigned=false；已分班时按可选日期范围返回课程安排")
    public ApiResponse<StudentScheduleVO> getCurrentStudentSchedule(
            @ParameterObject StudentScheduleQueryDTO query) {
        return ApiResponse.success(studentScheduleService.getCurrentStudentSchedule(query));
    }
}
