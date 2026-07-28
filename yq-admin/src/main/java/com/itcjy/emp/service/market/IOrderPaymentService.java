package com.itcjy.emp.service.market;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.enums.PaymentCloseReason;
import com.itcjy.emp.pojo.req.pay.OrderPaymentPageReq;
import com.itcjy.emp.pojo.res.pay.OrderPaymentRes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface IOrderPaymentService extends IService<OrderPayment> {

    OrderPayment createOrReusePendingPayment(MarketPrepaymentOrder prepaymentOrder, String orderNo, LocalDateTime expireAt);

    OrderPayment requireByOrderNo(String orderNo);

    boolean markPaid(String orderNo, String channelTradeNo, BigDecimal amount, LocalDateTime paidAt);

    boolean closePendingPayment(Long paymentOrderId, PaymentCloseReason closeReason, LocalDateTime closedAt);

    void closeForSystemFailure(Long paymentOrderId);

    OrderPaymentRes getOrderPaymentDetail(Long id);

    PageResult<OrderPaymentRes> pageOrderPayments(OrderPaymentPageReq req);
}
