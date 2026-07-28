package com.itcjy.emp.service.impl.pay;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.interceptor.AuthThreadlocal;
import com.itcjy.common.interceptor.LoginSession;
import com.itcjy.common.pojo.PageResult;
import com.itcjy.emp.mapper.pay.OrderPaymentMapper;
import com.itcjy.emp.mapper.pay.PaymentRefundRequestMapper;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.entity.PaymentRefundRequest;
import com.itcjy.emp.pojo.entity.SysUser;
import com.itcjy.emp.pojo.enums.PaymentOrderStatus;
import com.itcjy.emp.pojo.enums.PaymentRefundStatus;
import com.itcjy.emp.pojo.req.pay.PaymentRefundCreateReq;
import com.itcjy.emp.pojo.req.pay.PaymentRefundPageReq;
import com.itcjy.emp.pojo.req.pay.PaymentRefundReviewReq;
import com.itcjy.emp.pojo.res.pay.AlipayRefundRes;
import com.itcjy.emp.pojo.res.pay.PaymentRefundRes;
import com.itcjy.emp.service.market.IMarketPrepaymentOrderService;
import com.itcjy.emp.service.pay.IAlipayService;
import com.itcjy.emp.service.pay.IPaymentRefundService;
import com.itcjy.emp.service.stu.IStudentService;
import com.itcjy.emp.service.system.ISysUserService;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.VO.StudentPaymentDetailVO;
import com.itcjy.stu.pojo.entity.Student;
import com.itcjy.stu.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentRefundServiceImpl implements IPaymentRefundService {

    private final OrderPaymentMapper orderPaymentMapper;
    private final PaymentRefundRequestMapper refundRequestMapper;
    private final IMarketPrepaymentOrderService prepaymentOrderService;
    private final IAlipayService alipayService;
    private final IStudentService studentService;
    private final ISysUserService sysUserService;
    private final LoginService loginService;

    @Override
    public StudentPaymentDetailVO getCurrentStudentPaymentDetail(Long paymentOrderId) {
        OrderPayment payment = requireCurrentStudentPayment(paymentOrderId);
        MarketPrepaymentOrder prepaymentOrder = requirePrepaymentOrder(payment.getPrepaymentOrderId());
        return StudentPaymentDetailVO.from(payment, prepaymentOrder, findLatestRefund(paymentOrderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRefundRes createCurrentStudentRefundRequest(Long paymentOrderId, PaymentRefundCreateReq request) {
        OrderPayment payment = requireCurrentStudentPayment(paymentOrderId);
        if (!PaymentOrderStatus.PAID.name().equals(payment.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("Only paid orders can request a refund");
        }
        BigDecimal refundedAmount = payment.getRefundedAmount() == null ? BigDecimal.ZERO : payment.getRefundedAmount();
        if (refundedAmount.signum() > 0) {
            throw BusinessException.DATA_ERROR.newInstance("This order already has a refund record");
        }
        PaymentRefundRequest latestRequest = findLatestRefund(paymentOrderId);
        if (latestRequest != null && (PaymentRefundStatus.PENDING.name().equals(latestRequest.getStatus())
                || PaymentRefundStatus.PROCESSING.name().equals(latestRequest.getStatus()))) {
            throw BusinessException.DATA_ERROR.newInstance("A refund request is already being processed");
        }
        if (latestRequest != null && (PaymentRefundStatus.REFUNDED.name().equals(latestRequest.getStatus())
                || PaymentRefundStatus.FAILED.name().equals(latestRequest.getStatus()))) {
            throw BusinessException.DATA_ERROR.newInstance("This refund request must be handled by an administrator");
        }

        PaymentRefundRequest refundRequest = new PaymentRefundRequest();
        refundRequest.setPaymentOrderId(payment.getId());
        refundRequest.setRefundNo(newRefundNo());
        refundRequest.setRefundAmount(payment.getOrderAmount());
        refundRequest.setReason(StrUtil.trim(request.reason()));
        refundRequest.setStatus(PaymentRefundStatus.PENDING.name());
        refundRequest.setRequestedAt(LocalDateTime.now());
        refundRequestMapper.insert(refundRequest);
        return PaymentRefundRes.from(refundRequest, payment.getOrderNo(), payment.getStudentName(), payment.getStudentPhone());
    }

    @Override
    public PageResult<PaymentRefundRes> pageRefundRequests(PaymentRefundPageReq request) {
        String keyword = StrUtil.trim(request.getKeyword());
        List<Long> paymentOrderIds = Collections.emptyList();
        if (StrUtil.isNotBlank(keyword)) {
            paymentOrderIds = orderPaymentMapper.selectList(Wrappers.<OrderPayment>lambdaQuery()
                            .and(query -> query.like(OrderPayment::getOrderNo, keyword)
                                    .or().like(OrderPayment::getStudentName, keyword)
                                    .or().like(OrderPayment::getStudentPhone, keyword)))
                    .stream().map(OrderPayment::getId).toList();
        }
        List<Long> keywordPaymentOrderIds = paymentOrderIds;
        LambdaQueryWrapper<PaymentRefundRequest> query = Wrappers.<PaymentRefundRequest>lambdaQuery()
                .eq(StrUtil.isNotBlank(request.getStatus()), PaymentRefundRequest::getStatus, StrUtil.trim(request.getStatus()));
        if (StrUtil.isNotBlank(keyword)) {
            if (keywordPaymentOrderIds.isEmpty()) {
                query.like(PaymentRefundRequest::getRefundNo, keyword);
            } else {
                query.and(condition -> condition.like(PaymentRefundRequest::getRefundNo, keyword)
                        .or().in(PaymentRefundRequest::getPaymentOrderId, keywordPaymentOrderIds));
            }
        }
        query.orderByDesc(PaymentRefundRequest::getRequestedAt).orderByDesc(PaymentRefundRequest::getId);
        IPage<PaymentRefundRequest> page = refundRequestMapper.selectPage(
                new Page<>(request.getCurrent(), request.getSize()),
                query
        );
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(page.getTotal(), Collections.emptyList());
        }
        Map<Long, OrderPayment> payments = orderPaymentMapper.selectBatchIds(
                        page.getRecords().stream().map(PaymentRefundRequest::getPaymentOrderId).distinct().toList())
                .stream().collect(Collectors.toMap(OrderPayment::getId, Function.identity()));
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(refund -> toResponse(refund, payments.get(refund.getPaymentOrderId())))
                .toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentRefundRes approveRefundRequest(Long refundRequestId, PaymentRefundReviewReq request) {
        Reviewer reviewer = currentReviewer();
        PaymentRefundRequest refundRequest = requireRefundRequest(refundRequestId);
        if (!PaymentRefundStatus.PENDING.name().equals(refundRequest.getStatus())
                && !PaymentRefundStatus.FAILED.name().equals(refundRequest.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("Refund request cannot be approved from status " + refundRequest.getStatus());
        }
        OrderPayment payment = requirePayment(refundRequest.getPaymentOrderId());
        if (!PaymentOrderStatus.PAID.name().equals(payment.getStatus())) {
            throw BusinessException.DATA_ERROR.newInstance("Payment order cannot be refunded from status " + payment.getStatus());
        }

        boolean claimed = refundRequestMapper.update(null, new LambdaUpdateWrapper<PaymentRefundRequest>()
                .eq(PaymentRefundRequest::getId, refundRequestId)
                .in(PaymentRefundRequest::getStatus, PaymentRefundStatus.PENDING.name(), PaymentRefundStatus.FAILED.name())
                .set(PaymentRefundRequest::getStatus, PaymentRefundStatus.PROCESSING.name())
                .set(PaymentRefundRequest::getReviewerUserId, reviewer.id())
                .set(PaymentRefundRequest::getReviewerName, reviewer.name())
                .set(PaymentRefundRequest::getReviewRemark, StrUtil.trim(request.remark()))
                .set(PaymentRefundRequest::getReviewedAt, LocalDateTime.now())
                .set(PaymentRefundRequest::getFailureReason, null)) == 1;
        if (!claimed) {
            throw BusinessException.DATA_ERROR.newInstance("Refund request is being processed by another administrator");
        }
        orderPaymentMapper.update(null, new LambdaUpdateWrapper<OrderPayment>()
                .eq(OrderPayment::getId, payment.getId())
                .eq(OrderPayment::getStatus, PaymentOrderStatus.PAID.name())
                .set(OrderPayment::getStatus, PaymentOrderStatus.REFUNDING.name()));

        try {
            AlipayRefundRes result = alipayService.refund(payment, refundRequest);
            completeRefund(refundRequest, payment, result);
        } catch (RuntimeException ex) {
            refundRequestMapper.update(null, new LambdaUpdateWrapper<PaymentRefundRequest>()
                    .eq(PaymentRefundRequest::getId, refundRequestId)
                    .eq(PaymentRefundRequest::getStatus, PaymentRefundStatus.PROCESSING.name())
                    .set(PaymentRefundRequest::getStatus, PaymentRefundStatus.FAILED.name())
                    .set(PaymentRefundRequest::getFailureReason, abbreviate(ex.getMessage())));
            orderPaymentMapper.update(null, new LambdaUpdateWrapper<OrderPayment>()
                    .eq(OrderPayment::getId, payment.getId())
                    .eq(OrderPayment::getStatus, PaymentOrderStatus.REFUNDING.name())
                    .set(OrderPayment::getStatus, PaymentOrderStatus.PAID.name()));
        }
        return toResponse(requireRefundRequest(refundRequestId), payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefundRequest(Long refundRequestId, PaymentRefundReviewReq request) {
        Reviewer reviewer = currentReviewer();
        boolean rejected = refundRequestMapper.update(null, new LambdaUpdateWrapper<PaymentRefundRequest>()
                .eq(PaymentRefundRequest::getId, refundRequestId)
                .eq(PaymentRefundRequest::getStatus, PaymentRefundStatus.PENDING.name())
                .set(PaymentRefundRequest::getStatus, PaymentRefundStatus.REJECTED.name())
                .set(PaymentRefundRequest::getReviewerUserId, reviewer.id())
                .set(PaymentRefundRequest::getReviewerName, reviewer.name())
                .set(PaymentRefundRequest::getReviewRemark, StrUtil.trim(request.remark()))
                .set(PaymentRefundRequest::getReviewedAt, LocalDateTime.now())) == 1;
        if (!rejected) {
            throw BusinessException.DATA_ERROR.newInstance("Refund request cannot be rejected");
        }
    }

    private void completeRefund(PaymentRefundRequest refundRequest, OrderPayment payment, AlipayRefundRes result) {
        LocalDateTime now = LocalDateTime.now();
        refundRequestMapper.update(null, new LambdaUpdateWrapper<PaymentRefundRequest>()
                .eq(PaymentRefundRequest::getId, refundRequest.getId())
                .eq(PaymentRefundRequest::getStatus, PaymentRefundStatus.PROCESSING.name())
                .set(PaymentRefundRequest::getStatus, PaymentRefundStatus.REFUNDED.name())
                .set(PaymentRefundRequest::getChannelRefundNo, result.channelRefundNo())
                .set(PaymentRefundRequest::getRefundedAt, now));
        orderPaymentMapper.update(null, new LambdaUpdateWrapper<OrderPayment>()
                .eq(OrderPayment::getId, payment.getId())
                .eq(OrderPayment::getStatus, PaymentOrderStatus.REFUNDING.name())
                .set(OrderPayment::getStatus, PaymentOrderStatus.REFUNDED.name())
                .set(OrderPayment::getRefundedAmount, payment.getOrderAmount()));
        Student student = studentService.lambdaQuery().eq(Student::getPhone, payment.getStudentPhone()).one();
        if (student != null) {
            student.setStatus(Student.WITCHDRAWAL);
            student.setUpdatedAt(now);
            studentService.updateById(student);
        }
    }

    private OrderPayment requireCurrentStudentPayment(Long paymentOrderId) {
        OrderPayment payment = requirePayment(paymentOrderId);
        StudentDetailsVO currentStudent = loginService.getCurrentStudent();
        if (currentStudent == null || !StrUtil.equals(currentStudent.getPhone(), payment.getStudentPhone())) {
            throw BusinessException.ORDER_PAYMENT_NOT_EXIST.newInstance("Payment order does not exist");
        }
        return payment;
    }

    private OrderPayment requirePayment(Long paymentOrderId) {
        OrderPayment payment = orderPaymentMapper.selectById(paymentOrderId);
        if (payment == null) {
            throw BusinessException.ORDER_PAYMENT_NOT_EXIST.newInstance("Payment order does not exist");
        }
        return payment;
    }

    private PaymentRefundRequest requireRefundRequest(Long refundRequestId) {
        PaymentRefundRequest request = refundRequestMapper.selectById(refundRequestId);
        if (request == null) {
            throw BusinessException.DATA_ERROR.newInstance("Refund request does not exist");
        }
        return request;
    }

    private MarketPrepaymentOrder requirePrepaymentOrder(Long prepaymentOrderId) {
        MarketPrepaymentOrder prepaymentOrder = prepaymentOrderService.getById(prepaymentOrderId);
        if (prepaymentOrder == null) {
            throw BusinessException.PREPAY_ORDER_NOT_EXIST.newInstance("Prepayment order does not exist");
        }
        return prepaymentOrder;
    }

    private PaymentRefundRequest findLatestRefund(Long paymentOrderId) {
        return refundRequestMapper.selectOne(Wrappers.<PaymentRefundRequest>lambdaQuery()
                .eq(PaymentRefundRequest::getPaymentOrderId, paymentOrderId)
                .orderByDesc(PaymentRefundRequest::getId)
                .last("LIMIT 1"));
    }

    private Reviewer currentReviewer() {
        LoginSession session = AuthThreadlocal.getLoginInfo();
        if (session == null || session.getPrincipalId() == null) {
            throw BusinessException.USER_NO_TOKEN;
        }
        SysUser user = sysUserService.getById(session.getPrincipalId());
        if (user == null) {
            throw BusinessException.USER_NOT_EXIST.newInstance("Reviewer does not exist");
        }
        String name = StrUtil.blankToDefault(user.getRealName(), StrUtil.blankToDefault(user.getNickname(), user.getUsername()));
        return new Reviewer(user.getId(), name);
    }

    private PaymentRefundRes toResponse(PaymentRefundRequest request, OrderPayment payment) {
        return PaymentRefundRes.from(request,
                payment == null ? null : payment.getOrderNo(),
                payment == null ? null : payment.getStudentName(),
                payment == null ? null : payment.getStudentPhone());
    }

    private String newRefundNo() {
        return "YQR" + UUID.randomUUID().toString().replace("-", "");
    }

    private String abbreviate(String value) {
        if (StrUtil.isBlank(value)) {
            return "Alipay refund failed";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private record Reviewer(Long id, String name) {
    }
}
