package com.itcjy.emp.service.impl.market;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.pay.OrderPaymentMapper;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.req.pay.OrderPaymentCreateReq;
import com.itcjy.emp.pojo.req.pay.OrderPaymentPageReq;
import com.itcjy.emp.pojo.req.pay.OrderPaymentUpdateReq;
import com.itcjy.emp.pojo.res.pay.OrderPaymentRes;
import com.itcjy.emp.service.market.IOrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl
        extends ServiceImpl<OrderPaymentMapper, OrderPayment>
        implements IOrderPaymentService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPaymentRes addOrderPayment(OrderPaymentCreateReq req) {
        boolean exists = this.lambdaQuery()
                .eq(OrderPayment::getOrderNo, StrUtil.trim(req.getOrderNo()))
                .exists();
        if (exists) {
            throw BusinessException.ORDER_PAYMENT_EXIST.newInstance("支付订单号已存在");
        }
        OrderPayment order = new OrderPayment();
        order.setOrderNo(StrUtil.trim(req.getOrderNo()));
        order.setStudentPhone(StrUtil.trim(req.getStudentPhone()));
        order.setStudentName(StrUtil.trim(req.getStudentName()));
        order.setProductId(StrUtil.trim(req.getProductId()));
        order.setOrderAmount(req.getOrderAmount());
        order.setRefundedAmount(BigDecimal.ZERO);
        order.setPrepaymentOrderId(StrUtil.trim(req.getPrepaymentOrderId()));
        order.setStatus(StrUtil.trim(req.getStatus()));
        this.save(order);
        return OrderPaymentRes.from(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderPayment(Long id, OrderPaymentUpdateReq req) {
        OrderPayment order = requireOrderPayment(id);
        if (StrUtil.isNotBlank(req.getStatus())) {
            order.setStatus(StrUtil.trim(req.getStatus()));
        }
        if (req.getRefundedAmount() != null) {
            order.setRefundedAmount(req.getRefundedAmount());
        }
        if (req.getUniqueOrderNo() != null) {
            order.setUniqueOrderNo(StrUtil.trim(req.getUniqueOrderNo()));
        }
        if (req.getPaySuccessTime() != null) {
            order.setPaySuccessTime(req.getPaySuccessTime());
        }
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrderPayment(Long id) {
        requireOrderPayment(id);
        this.removeById(id);
    }

    @Override
    public OrderPaymentRes getOrderPaymentDetail(Long id) {
        return OrderPaymentRes.from(requireOrderPayment(id));
    }

    @Override
    public PageResult<OrderPaymentRes> pageOrderPayments(OrderPaymentPageReq req) {
        IPage<OrderPayment> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<OrderPayment>lambdaQuery()
                        .eq(StrUtil.isNotBlank(req.getStudentPhone()),
                                OrderPayment::getStudentPhone, StrUtil.trim(req.getStudentPhone()))
                        .eq(StrUtil.isNotBlank(req.getOrderNo()),
                                OrderPayment::getOrderNo, StrUtil.trim(req.getOrderNo()))
                        .eq(StrUtil.isNotBlank(req.getStatus()),
                                OrderPayment::getStatus, StrUtil.trim(req.getStatus()))
                        .orderByDesc(OrderPayment::getId)
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        return new PageResult<>(
                page.getTotal(),
                page.getRecords().stream().map(OrderPaymentRes::from).toList()
        );
    }

    private OrderPayment requireOrderPayment(Long id) {
        OrderPayment order = this.getById(id);
        if (order == null) {
            throw BusinessException.ORDER_PAYMENT_NOT_EXIST.newInstance("支付订单不存在");
        }
        return order;
    }
}
