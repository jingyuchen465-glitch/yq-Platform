package com.itcjy.stu.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.itcjy.emp.mapper.market.MarketPrepaymentOrderMapper;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.VO.StudentPrepaymentOrderVO;
import com.itcjy.stu.service.LoginService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentPrepaymentOrderServiceImplTest {

    private LoginService loginService;
    private MarketPrepaymentOrderMapper orderMapper;
    private StudentPrepaymentOrderServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "prepayment-order-test"),
                MarketPrepaymentOrder.class
        );
        loginService = mock(LoginService.class);
        orderMapper = mock(MarketPrepaymentOrderMapper.class);
        service = new StudentPrepaymentOrderServiceImpl(loginService, orderMapper);
    }

    @Test
    @DisplayName("应只按当前登录学生手机号查询并返回支付所需字段")
    @SuppressWarnings("unchecked")
    void shouldListOrdersForCurrentStudentPhone() {
        StudentDetailsVO student = new StudentDetailsVO();
        student.setPhone("13800138000");
        MarketPrepaymentOrder order = new MarketPrepaymentOrder();
        order.setId(27L);
        order.setProductName("Java 高级课程");
        order.setProductPrice(new BigDecimal("88.88"));
        order.setSalespersonName("李老师");
        order.setCreatedAt(LocalDateTime.of(2026, 7, 27, 10, 30));

        when(loginService.getCurrentStudent()).thenReturn(student);
        when(orderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(order));

        List<StudentPrepaymentOrderVO> result = service.listCurrentStudentOrders();

        ArgumentCaptor<Wrapper<MarketPrepaymentOrder>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(orderMapper).selectList(wrapperCaptor.capture());
        LambdaQueryWrapper<MarketPrepaymentOrder> query =
                (LambdaQueryWrapper<MarketPrepaymentOrder>) wrapperCaptor.getValue();
        assertThat(query.getSqlSegment()).contains("phone", "created_at", "id");
        assertThat(query.getParamNameValuePairs()).containsValue("13800138000");
        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(27L);
            assertThat(item.productName()).isEqualTo("Java 高级课程");
            assertThat(item.productPrice()).isEqualByComparingTo("88.88");
            assertThat(item.outTradeNo()).isEqualTo("YQPREPAY27");
        });
        verify(loginService).getCurrentStudent();
    }
}
