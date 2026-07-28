package com.itcjy.emp.service.impl.pay;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.itcjy.common.properties.AlipayProperties;
import com.itcjy.common.properties.PaymentProperties;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.enums.PaymentCloseReason;
import com.itcjy.emp.pojo.enums.PaymentOrderStatus;
import com.itcjy.emp.pojo.message.PaymentTimeoutMessage;
import com.itcjy.emp.pojo.res.pay.AlipayTradeCreateRes;
import com.itcjy.emp.service.market.IMarketPrepaymentOrderService;
import com.itcjy.emp.service.market.IOrderPaymentService;
import com.itcjy.emp.service.pay.IPaymentTimeoutOutboxService;
import com.itcjy.emp.service.stu.IStudentService;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.service.LoginService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlipayServiceImplTest {

    private final AlipayClient alipayClient = mock(AlipayClient.class);
    private final AlipayProperties alipayProperties = new AlipayProperties();
    private final PaymentProperties paymentProperties = new PaymentProperties();
    private final IMarketPrepaymentOrderService prepaymentOrderService = mock(IMarketPrepaymentOrderService.class);
    private final IStudentService studentService = mock(IStudentService.class);
    private final IOrderPaymentService orderPaymentService = mock(IOrderPaymentService.class);
    private final IPaymentTimeoutOutboxService timeoutOutboxService = mock(IPaymentTimeoutOutboxService.class);
    private final LoginService loginService = mock(LoginService.class);

    @Test
    void shouldCreatePaymentUsingServerSidePrepaymentSnapshot() throws Exception {
        StudentDetailsVO currentStudent = new StudentDetailsVO();
        currentStudent.setPhone("13800138000");
        MarketPrepaymentOrder prepayment = new MarketPrepaymentOrder();
        prepayment.setId(9L);
        prepayment.setPhone("13800138000");
        prepayment.setName("Student");
        prepayment.setProductId(3L);
        prepayment.setProductName("Java course");
        prepayment.setProductPrice(new BigDecimal("88.88"));
        OrderPayment payment = pendingPayment(19L, "YQPtest");

        AlipayTradePagePayResponse response = new AlipayTradePagePayResponse();
        response.setCode("10000");
        response.setBody("<form></form>");
        when(loginService.getCurrentStudent()).thenReturn(currentStudent);
        when(prepaymentOrderService.getById(9L)).thenReturn(prepayment);
        when(orderPaymentService.createOrReusePendingPayment(any(), any(), any())).thenReturn(payment);
        when(alipayClient.pageExecute(any(AlipayTradePagePayRequest.class), org.mockito.ArgumentMatchers.eq("POST")))
                .thenReturn(response);

        AlipayTradeCreateRes result = service().createTradePagePay(9L);

        assertThat(result.outTradeNo()).isEqualTo("YQPtest");
        assertThat(result.payForm()).isEqualTo("<form></form>");
        verify(timeoutOutboxService).publishPendingForPayment(19L);
        verify(orderPaymentService).createOrReusePendingPayment(
                org.mockito.ArgumentMatchers.same(prepayment), any(), any());
    }

    @Test
    void shouldClosePendingOrderWhenTimeoutMessageFindsClosedTrade() throws Exception {
        OrderPayment payment = pendingPayment(19L, "YQPtimeout");
        payment.setExpireAt(LocalDateTime.now().minusMinutes(1));
        AlipayTradeQueryResponse response = new AlipayTradeQueryResponse();
        response.setCode("10000");
        response.setTradeStatus("TRADE_CLOSED");
        when(orderPaymentService.getById(19L)).thenReturn(payment);
        when(alipayClient.execute(any(AlipayTradeQueryRequest.class))).thenReturn(response);

        service().handleTimeout(new PaymentTimeoutMessage(19L, "YQPtimeout", payment.getExpireAt()));

        verify(orderPaymentService).closePendingPayment(
                org.mockito.ArgumentMatchers.eq(19L),
                org.mockito.ArgumentMatchers.eq(PaymentCloseReason.TIMEOUT),
                any());
    }

    @Test
    void shouldIgnoreTimeoutMessageForAlreadyPaidOrder() throws Exception {
        OrderPayment payment = pendingPayment(19L, "YQPpaid");
        payment.setStatus(PaymentOrderStatus.PAID.name());
        when(orderPaymentService.getById(19L)).thenReturn(payment);

        service().handleTimeout(new PaymentTimeoutMessage(19L, "YQPpaid", LocalDateTime.now().minusMinutes(1)));

        verify(alipayClient, never()).execute(any(AlipayTradeQueryRequest.class));
    }

    private AlipayServiceImpl service() {
        return new AlipayServiceImpl(
                alipayClient,
                alipayProperties,
                paymentProperties,
                prepaymentOrderService,
                studentService,
                orderPaymentService,
                timeoutOutboxService,
                loginService
        );
    }

    private OrderPayment pendingPayment(Long id, String orderNo) {
        OrderPayment payment = new OrderPayment();
        payment.setId(id);
        payment.setOrderNo(orderNo);
        payment.setOrderAmount(new BigDecimal("88.88"));
        payment.setStatus(PaymentOrderStatus.PENDING.name());
        payment.setExpireAt(LocalDateTime.now().plusMinutes(30));
        return payment;
    }
}
