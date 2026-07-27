package com.itcjy.emp.service.market;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.req.pay.OrderPaymentCreateReq;
import com.itcjy.emp.pojo.req.pay.OrderPaymentPageReq;
import com.itcjy.emp.pojo.req.pay.OrderPaymentUpdateReq;
import com.itcjy.emp.pojo.res.pay.OrderPaymentRes;

public interface IOrderPaymentService extends IService<OrderPayment> {

    OrderPaymentRes addOrderPayment(OrderPaymentCreateReq req);

    void updateOrderPayment(Long id, OrderPaymentUpdateReq req);

    void deleteOrderPayment(Long id);

    OrderPaymentRes getOrderPaymentDetail(Long id);

    PageResult<OrderPaymentRes> pageOrderPayments(OrderPaymentPageReq req);
}
