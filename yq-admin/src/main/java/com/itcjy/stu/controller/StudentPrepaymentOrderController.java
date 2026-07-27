package com.itcjy.stu.controller;

import com.itcjy.common.pojo.ApiResponse;
import com.itcjy.stu.pojo.VO.StudentPrepaymentOrderVO;
import com.itcjy.stu.service.StudentPrepaymentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/stu/prepayment-orders")
@Tag(name = "学生端预订单", description = "当前登录学生的预订单查询接口")
public class StudentPrepaymentOrderController {

    private final StudentPrepaymentOrderService prepaymentOrderService;

    @Operation(summary = "查询本人预订单", description = "按当前登录学生的手机号查询预订单")
    @GetMapping
    public ApiResponse<List<StudentPrepaymentOrderVO>> listCurrentStudentOrders() {
        return ApiResponse.success(prepaymentOrderService.listCurrentStudentOrders());
    }
}
