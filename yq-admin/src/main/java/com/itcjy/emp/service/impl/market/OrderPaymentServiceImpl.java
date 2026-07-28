package com.itcjy.emp.service.impl.market;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.pay.OrderPaymentMapper;
import com.itcjy.emp.mapper.pay.PaymentTimeoutOutboxMapper;
import com.itcjy.emp.mapper.market.MarketPrepaymentOrderMapper;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.entity.PaymentTimeoutOutbox;
import com.itcjy.emp.pojo.enums.PaymentCloseReason;
import com.itcjy.emp.pojo.enums.PaymentOrderStatus;
import com.itcjy.emp.pojo.enums.PaymentTimeoutOutboxStatus;
import com.itcjy.emp.pojo.req.pay.OrderPaymentPageReq;
import com.itcjy.emp.pojo.res.pay.OrderPaymentRes;
import com.itcjy.emp.service.market.IOrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl
        extends ServiceImpl<OrderPaymentMapper, OrderPayment>
        implements IOrderPaymentService {

    private static final String ALIPAY = "ALIPAY";

    private final PaymentTimeoutOutboxMapper paymentTimeoutOutboxMapper;
    private final MarketPrepaymentOrderMapper prepaymentOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderPayment createOrReusePendingPayment(
            MarketPrepaymentOrder prepaymentOrder,
            String orderNo,
            LocalDateTime expireAt) {
        MarketPrepaymentOrder lockedPrepaymentOrder = prepaymentOrderMapper.selectByIdForUpdate(prepaymentOrder.getId());
        if (lockedPrepaymentOrder == null) {
            throw BusinessException.PREPAY_ORDER_NOT_EXIST.newInstance("Prepayment order does not exist");
        }
        OrderPayment pending = this.lambdaQuery()
                .eq(OrderPayment::getPrepaymentOrderId, prepaymentOrder.getId())
                .eq(OrderPayment::getStatus, PaymentOrderStatus.PENDING.name())
                .orderByDesc(OrderPayment::getId)
                .last("LIMIT 1")
                .one();
        if (pending != null) {
            return pending;
        }

        OrderPayment payment = new OrderPayment();
        payment.setOrderNo(orderNo);
        payment.setStudentPhone(lockedPrepaymentOrder.getPhone());
        payment.setStudentName(lockedPrepaymentOrder.getName());
        payment.setProductId(String.valueOf(lockedPrepaymentOrder.getProductId()));
        payment.setOrderAmount(lockedPrepaymentOrder.getProductPrice());
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setPrepaymentOrderId(prepaymentOrder.getId());
        payment.setPaymentChannel(ALIPAY);
        payment.setStatus(PaymentOrderStatus.PENDING.name());
        payment.setExpireAt(expireAt);
        this.save(payment);

        PaymentTimeoutOutbox outbox = new PaymentTimeoutOutbox();
        outbox.setPaymentOrderId(payment.getId());
        outbox.setOrderNo(payment.getOrderNo());
        outbox.setExpireAt(payment.getExpireAt());
        outbox.setStatus(PaymentTimeoutOutboxStatus.PENDING.name());
        outbox.setPublishAttempts(0);
        paymentTimeoutOutboxMapper.insert(outbox);
        return payment;
    }

    @Override
    public OrderPayment requireByOrderNo(String orderNo) {
        OrderPayment payment = this.lambdaQuery().eq(OrderPayment::getOrderNo, orderNo).one();
        if (payment == null) {
            throw BusinessException.ORDER_PAYMENT_NOT_EXIST.newInstance("Payment order does not exist");
        }
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markPaid(String orderNo, String channelTradeNo, BigDecimal amount, LocalDateTime paidAt) {
        OrderPayment payment = requireByOrderNo(orderNo);
        if (payment.getOrderAmount().compareTo(amount) != 0) {
            throw BusinessException.DATA_ERROR.newInstance("Payment amount does not match the order");
        }
        if (PaymentOrderStatus.PAID.name().equals(payment.getStatus())) {
            if (StrUtil.isNotBlank(channelTradeNo)
                    && StrUtil.isNotBlank(payment.getChannelTradeNo())
                    && !channelTradeNo.equals(payment.getChannelTradeNo())) {
                throw BusinessException.DATA_ERROR.newInstance("Payment channel trade number does not match");
            }
            return false;
        }
        if (!PaymentOrderStatus.PENDING.name().equals(payment.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("Payment order cannot be marked paid from status " + payment.getStatus());
        }
        return this.update(new LambdaUpdateWrapper<OrderPayment>()
                .eq(OrderPayment::getId, payment.getId())
                .eq(OrderPayment::getStatus, PaymentOrderStatus.PENDING.name())
                .set(OrderPayment::getStatus, PaymentOrderStatus.PAID.name())
                .set(OrderPayment::getChannelTradeNo, StrUtil.trim(channelTradeNo))
                .set(OrderPayment::getPaySuccessTime, paidAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closePendingPayment(Long paymentOrderId, PaymentCloseReason closeReason, LocalDateTime closedAt) {
        return this.update(new LambdaUpdateWrapper<OrderPayment>()
                .eq(OrderPayment::getId, paymentOrderId)
                .eq(OrderPayment::getStatus, PaymentOrderStatus.PENDING.name())
                .set(OrderPayment::getStatus, PaymentOrderStatus.CLOSED.name())
                .set(OrderPayment::getClosedAt, closedAt)
                .set(OrderPayment::getCloseReason, closeReason.name()));
    }

    @Override
    public void closeForSystemFailure(Long paymentOrderId) {
        closePendingPayment(paymentOrderId, PaymentCloseReason.SYSTEM_FAILURE, LocalDateTime.now());
    }

    @Override
    public OrderPaymentRes getOrderPaymentDetail(Long id) {
        OrderPayment payment = this.getById(id);
        if (payment == null) {
            throw BusinessException.ORDER_PAYMENT_NOT_EXIST.newInstance("Payment order does not exist");
        }
        return OrderPaymentRes.from(payment);
    }

    @Override
    public PageResult<OrderPaymentRes> pageOrderPayments(OrderPaymentPageReq req) {
        IPage<OrderPayment> page = this.page(
                new Page<>(req.getCurrent(), req.getSize()),
                Wrappers.<OrderPayment>lambdaQuery()
                        .eq(StrUtil.isNotBlank(req.getStudentPhone()), OrderPayment::getStudentPhone, StrUtil.trim(req.getStudentPhone()))
                        .eq(StrUtil.isNotBlank(req.getOrderNo()), OrderPayment::getOrderNo, StrUtil.trim(req.getOrderNo()))
                        .eq(StrUtil.isNotBlank(req.getStatus()), OrderPayment::getStatus, StrUtil.trim(req.getStatus()))
                        .eq(StrUtil.isNotBlank(req.getPaymentChannel()), OrderPayment::getPaymentChannel, StrUtil.trim(req.getPaymentChannel()))
                        .orderByDesc(OrderPayment::getId)
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        return new PageResult<>(page.getTotal(), page.getRecords().stream().map(OrderPaymentRes::from).toList());
    }
}
