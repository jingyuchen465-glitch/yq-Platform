package com.itcjy.stu.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itcjy.emp.mapper.market.MarketPrepaymentOrderMapper;
import com.itcjy.emp.mapper.pay.OrderPaymentMapper;
import com.itcjy.emp.mapper.pay.PaymentRefundRequestMapper;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.entity.PaymentRefundRequest;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.VO.StudentPrepaymentOrderVO;
import com.itcjy.stu.service.LoginService;
import com.itcjy.stu.service.StudentPrepaymentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentPrepaymentOrderServiceImpl implements StudentPrepaymentOrderService {

    private final LoginService loginService;
    private final MarketPrepaymentOrderMapper prepaymentOrderMapper;
    private final OrderPaymentMapper orderPaymentMapper;
    private final PaymentRefundRequestMapper paymentRefundRequestMapper;

    @Override
    public List<StudentPrepaymentOrderVO> listCurrentStudentOrders() {
        StudentDetailsVO student = loginService.getCurrentStudent();
        List<MarketPrepaymentOrder> prepaymentOrders = prepaymentOrderMapper.selectList(
                Wrappers.<MarketPrepaymentOrder>lambdaQuery()
                        .eq(MarketPrepaymentOrder::getPhone, student.getPhone())
                        .orderByDesc(MarketPrepaymentOrder::getCreatedAt)
                        .orderByDesc(MarketPrepaymentOrder::getId)
        );
        if (prepaymentOrders.isEmpty()) {
            return List.of();
        }
        Map<Long, OrderPayment> latestPaymentByPrepaymentId = orderPaymentMapper.selectList(
                Wrappers.<OrderPayment>lambdaQuery()
                        .in(OrderPayment::getPrepaymentOrderId,
                                prepaymentOrders.stream().map(MarketPrepaymentOrder::getId).toList())
                        .orderByDesc(OrderPayment::getId)
        ).stream().collect(Collectors.toMap(
                OrderPayment::getPrepaymentOrderId,
                Function.identity(),
                (first, ignored) -> first
        ));
        if (latestPaymentByPrepaymentId.isEmpty()) {
            return prepaymentOrders.stream().map(order -> StudentPrepaymentOrderVO.from(order, null, null)).toList();
        }
        Map<Long, String> latestRefundStatusByPaymentId = paymentRefundRequestMapper.selectList(
                Wrappers.<PaymentRefundRequest>lambdaQuery()
                        .in(PaymentRefundRequest::getPaymentOrderId,
                                latestPaymentByPrepaymentId.values().stream().map(OrderPayment::getId).toList())
                        .orderByDesc(PaymentRefundRequest::getId)
        ).stream().collect(Collectors.toMap(
                PaymentRefundRequest::getPaymentOrderId,
                PaymentRefundRequest::getStatus,
                (first, ignored) -> first
        ));
        return prepaymentOrders.stream()
                .map(order -> {
                    OrderPayment payment = latestPaymentByPrepaymentId.get(order.getId());
                    return StudentPrepaymentOrderVO.from(order, payment,
                            payment == null ? null : latestRefundStatusByPaymentId.get(payment.getId()));
                })
                .toList();
    }
}
