package com.itcjy.emp.service.impl.pay;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.emp.mapper.pay.OrderPaymentMapper;
import com.itcjy.emp.mapper.pay.PaymentRefundRequestMapper;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.entity.PaymentRefundRequest;
import com.itcjy.emp.pojo.enums.PaymentOrderStatus;
import com.itcjy.emp.pojo.enums.PaymentRefundStatus;
import com.itcjy.emp.pojo.req.pay.PaymentRefundCreateReq;
import com.itcjy.emp.pojo.res.pay.PaymentRefundRes;
import com.itcjy.emp.service.market.IMarketPrepaymentOrderService;
import com.itcjy.emp.service.pay.IAlipayService;
import com.itcjy.emp.service.stu.IStudentService;
import com.itcjy.emp.service.system.ISysUserService;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.service.LoginService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRefundServiceImplTest {

    private OrderPaymentMapper paymentMapper;
    private PaymentRefundRequestMapper refundMapper;
    private LoginService loginService;
    private PaymentRefundServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "payment-order-test"), OrderPayment.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "refund-request-test"), PaymentRefundRequest.class);
        paymentMapper = mock(OrderPaymentMapper.class);
        refundMapper = mock(PaymentRefundRequestMapper.class);
        loginService = mock(LoginService.class);
        service = new PaymentRefundServiceImpl(
                paymentMapper,
                refundMapper,
                mock(IMarketPrepaymentOrderService.class),
                mock(IAlipayService.class),
                mock(IStudentService.class),
                mock(ISysUserService.class),
                loginService
        );
    }

    @Test
    @DisplayName("Creates a full refund request for the current student's paid order")
    void shouldCreateFullRefundRequestForOwnedPaidOrder() {
        OrderPayment payment = paidOrder("13800138000");
        StudentDetailsVO student = new StudentDetailsVO();
        student.setPhone("13800138000");
        when(paymentMapper.selectById(11L)).thenReturn(payment);
        when(loginService.getCurrentStudent()).thenReturn(student);
        when(refundMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        PaymentRefundRes result = service.createCurrentStudentRefundRequest(11L, new PaymentRefundCreateReq("课程计划调整"));

        ArgumentCaptor<PaymentRefundRequest> captor = ArgumentCaptor.forClass(PaymentRefundRequest.class);
        verify(refundMapper).insert(captor.capture());
        PaymentRefundRequest saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PaymentRefundStatus.PENDING.name());
        assertThat(saved.getRefundAmount()).isEqualByComparingTo("88.88");
        assertThat(saved.getReason()).isEqualTo("课程计划调整");
        assertThat(result.paymentOrderId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Rejects a refund request for another student's order")
    void shouldRejectRefundForAnotherStudent() {
        OrderPayment payment = paidOrder("13800138000");
        StudentDetailsVO student = new StudentDetailsVO();
        student.setPhone("13900139000");
        when(paymentMapper.selectById(11L)).thenReturn(payment);
        when(loginService.getCurrentStudent()).thenReturn(student);

        assertThatThrownBy(() -> service.createCurrentStudentRefundRequest(11L, new PaymentRefundCreateReq("课程计划调整")))
                .isInstanceOf(BusinessException.class);
    }

    private OrderPayment paidOrder(String phone) {
        OrderPayment payment = new OrderPayment();
        payment.setId(11L);
        payment.setOrderNo("YQPtest");
        payment.setStudentPhone(phone);
        payment.setStudentName("Student");
        payment.setOrderAmount(new BigDecimal("88.88"));
        payment.setRefundedAmount(BigDecimal.ZERO);
        payment.setStatus(PaymentOrderStatus.PAID.name());
        return payment;
    }
}
