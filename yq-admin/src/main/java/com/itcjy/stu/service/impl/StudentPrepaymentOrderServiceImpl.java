package com.itcjy.stu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.emp.mapper.market.MarketPrepaymentOrderMapper;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.VO.StudentPrepaymentOrderVO;
import com.itcjy.stu.service.LoginService;
import com.itcjy.stu.service.StudentPrepaymentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentPrepaymentOrderServiceImpl implements StudentPrepaymentOrderService {

    private final LoginService loginService;
    private final MarketPrepaymentOrderMapper prepaymentOrderMapper;

    @Override
    public List<StudentPrepaymentOrderVO> listCurrentStudentOrders() {
        StudentDetailsVO student = loginService.getCurrentStudent();
        return prepaymentOrderMapper.selectList(
                        Wrappers.<MarketPrepaymentOrder>lambdaQuery()
                                .eq(MarketPrepaymentOrder::getPhone, student.getPhone())
                                .orderByDesc(MarketPrepaymentOrder::getCreatedAt)
                                .orderByDesc(MarketPrepaymentOrder::getId)
                ).stream()
                .map(StudentPrepaymentOrderVO::from)
                .toList();
    }
}
