package com.itcjy.emp.service.pay;

import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.pojo.req.pay.PaymentRefundCreateReq;
import com.itcjy.emp.pojo.req.pay.PaymentRefundPageReq;
import com.itcjy.emp.pojo.req.pay.PaymentRefundReviewReq;
import com.itcjy.emp.pojo.res.pay.PaymentRefundRes;
import com.itcjy.stu.pojo.VO.StudentPaymentDetailVO;

public interface IPaymentRefundService {

    StudentPaymentDetailVO getCurrentStudentPaymentDetail(Long paymentOrderId);

    PaymentRefundRes createCurrentStudentRefundRequest(Long paymentOrderId, PaymentRefundCreateReq request);

    PageResult<PaymentRefundRes> pageRefundRequests(PaymentRefundPageReq request);

    PaymentRefundRes approveRefundRequest(Long refundRequestId, PaymentRefundReviewReq request);

    void rejectRefundRequest(Long refundRequestId, PaymentRefundReviewReq request);
}
