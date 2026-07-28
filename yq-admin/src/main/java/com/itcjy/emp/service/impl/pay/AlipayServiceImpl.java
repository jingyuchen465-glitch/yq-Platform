package com.itcjy.emp.service.impl.pay;

import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.properties.AlipayProperties;
import com.itcjy.common.properties.PaymentProperties;
import com.itcjy.emp.pojo.entity.MarketPrepaymentOrder;
import com.itcjy.emp.pojo.entity.OrderPayment;
import com.itcjy.emp.pojo.enums.PaymentCloseReason;
import com.itcjy.emp.pojo.enums.PaymentOrderStatus;
import com.itcjy.emp.pojo.message.PaymentTimeoutMessage;
import com.itcjy.emp.pojo.res.pay.AlipayTradeCreateRes;
import com.itcjy.emp.pojo.entity.PaymentRefundRequest;
import com.itcjy.emp.pojo.res.pay.AlipayRefundRes;
import com.itcjy.emp.service.market.IMarketPrepaymentOrderService;
import com.itcjy.emp.service.market.IOrderPaymentService;
import com.itcjy.emp.service.pay.IAlipayService;
import com.itcjy.emp.service.pay.IPaymentTimeoutOutboxService;
import com.itcjy.emp.service.stu.IStudentService;
import com.itcjy.stu.pojo.VO.StudentDetailsVO;
import com.itcjy.stu.pojo.entity.Student;
import com.itcjy.stu.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/**
 * 支付宝支付服务实现类
 * <p>
 * 负责对接支付宝开放平台，提供以下核心能力：
 * <ul>
 *     <li>创建电脑网站支付（Page Pay）交易</li>
 *     <li>处理支付宝异步回调通知（验签、状态流转）</li>
 *     <li>处理支付超时事件（主动查询 + 关单 + 对账）</li>
 * </ul>
 *
 * @author itcjy
 * @see IAlipayService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements IAlipayService {

    /** 支付渠道标识：支付宝 */
    private static final String ALIPAY = "ALIPAY";
    /** 支付宝交易状态：等待买家付款 */
    private static final String WAIT_BUYER_PAY = "WAIT_BUYER_PAY";
    /** 支付宝交易状态：交易支付成功 */
    private static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    /** 支付宝交易状态：交易结束（不可退款） */
    private static final String TRADE_FINISHED = "TRADE_FINISHED";
    /** 支付宝交易状态：交易关闭（未付款超时/全额退款） */
    private static final String TRADE_CLOSED = "TRADE_CLOSED";
    /** 支付时间格式化器，格式：yyyy-MM-dd HH:mm:ss */
    private static final DateTimeFormatter PAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 支付宝 SDK 客户端，用于发起各类交易请求 */
    private final AlipayClient alipayClient;
    /** 支付宝配置属性（appId、密钥、回调地址等） */
    private final AlipayProperties alipayProperties;
    /** 支付通用配置属性（超时时间、Topic 等） */
    private final PaymentProperties paymentProperties;
    /** 预付款订单服务 */
    private final IMarketPrepaymentOrderService prepaymentOrderService;
    /** 学生服务 */
    private final IStudentService studentService;
    /** 支付单服务 */
    private final IOrderPaymentService orderPaymentService;
    /** 支付超时发件箱服务（Outbox 模式） */
    private final IPaymentTimeoutOutboxService timeoutOutboxService;
    /** 登录服务，用于获取当前登录学生信息 */
    private final LoginService loginService;

    /**
     * 创建支付宝电脑网站支付交易
     * <p>
     * 业务流程：
     * <ol>
     *     <li>校验当前学生身份与订单归属</li>
     *     <li>创建或复用待支付状态的支付单</li>
     *     <li>发布超时检测消息到 Outbox</li>
     *     <li>调用支付宝 SDK 生成支付页面</li>
     * </ol>
     *
     * @param prepaymentOrderId 预付款订单 ID
     * @return 包含支付页面 HTML 和商户订单号的响应对象
     * @throws BusinessException 订单不存在、无权支付、金额无效或支付单已过期时抛出
     */
    @Override
    public AlipayTradeCreateRes createTradePagePay(Long prepaymentOrderId) {
        // 获取当前登录学生信息
        StudentDetailsVO currentStudent = loginService.getCurrentStudent();
        // 查询预付款订单
        MarketPrepaymentOrder prepaymentOrder = prepaymentOrderService.getById(prepaymentOrderId);
        if (prepaymentOrder == null) {
            throw BusinessException.PREPAY_ORDER_NOT_EXIST.newInstance("订单不存在");
        }
        // 校验订单归属：当前学生手机号必须与订单绑定手机号一致
        if (!StrUtil.equals(currentStudent.getPhone(), prepaymentOrder.getPhone())) {
            throw BusinessException.DATA_ERROR.newInstance("You cannot pay for this prepayment order");
        }
        // 校验订单金额有效性
        if (prepaymentOrder.getProductPrice() == null || prepaymentOrder.getProductPrice().signum() <= 0) {
            throw BusinessException.DATA_ERROR.newInstance("Prepayment order amount is invalid");
        }

        // 创建或复用 PENDING 状态的支付单，设置超时时间
        LocalDateTime now = LocalDateTime.now();
        OrderPayment payment = orderPaymentService.createOrReusePendingPayment(
                prepaymentOrder,
                newOrderNo(),
                now.plusMinutes(paymentProperties.getTimeoutMinutes())
        );
        // 若支付单已过期（正在被关闭），提示稍后重试
        if (payment.getExpireAt().isBefore(now)) {
            throw BusinessException.DATA_ERROR.newInstance("The previous payment is being closed; retry shortly");
        }
        // 将超时检测消息写入 Outbox，由定时任务投递到 MQ
        timeoutOutboxService.publishPendingForPayment(payment.getId());

        try {
            // 调用支付宝 SDK 创建支付页面
            return createAlipayPage(payment, prepaymentOrder);
        } catch (RuntimeException ex) {
            // 创建失败时关闭支付单，标记为系统故障
            orderPaymentService.closeForSystemFailure(payment.getId());
            throw ex;
        }
    }

    /**
     * 处理支付宝异步回调通知
     * <p>
     * 处理逻辑：
     * <ol>
     *     <li>RSA 验签，确保通知来源合法</li>
     *     <li>校验 app_id 一致性</li>
     *     <li>根据 trade_status 执行对应状态流转：
     *         <ul>
     *             <li>TRADE_SUCCESS / TRADE_FINISHED → 标记支付成功</li>
     *             <li>TRADE_CLOSED → 关闭支付单</li>
     *         </ul>
     *     </li>
     * </ol>
     *
     * @param params 支付宝回调的全部参数
     * @return true 表示处理成功（支付宝不再重发），false 表示处理失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleNotify(Map<String, String> params) {
        try {
            // 第一步：RSA 签名验证
            if (!AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType())) {
                log.warn("Alipay notification signature verification failed");
                return false;
            }
            // 第二步：校验 app_id 防止跨应用伪造
            if (!alipayProperties.getAppId().equals(params.get("app_id"))) {
                log.warn("Alipay notification app id mismatch");
                return false;
            }

            String orderNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            // 支付成功或交易完成 → 标记为已支付
            if (TRADE_SUCCESS.equals(tradeStatus) || TRADE_FINISHED.equals(tradeStatus)) {
                markTradeSuccess(orderNo, params.get("trade_no"), parseAmount(params.get("total_amount")), params.get("gmt_payment"));
            } else if (TRADE_CLOSED.equals(tradeStatus)) {
                // 交易关闭 → 判断关闭原因（超时 or 渠道拒绝）
                OrderPayment payment = orderPaymentService.requireByOrderNo(orderNo);
                PaymentCloseReason reason = payment.getExpireAt().isAfter(LocalDateTime.now())
                        ? PaymentCloseReason.CHANNEL_REJECTED
                        : PaymentCloseReason.TIMEOUT;
                orderPaymentService.closePendingPayment(payment.getId(), reason, LocalDateTime.now());
            }
            return true;
        } catch (AlipayApiException | BusinessException | IllegalArgumentException ex) {
            log.error("Failed to process Alipay notification", ex);
            return false;
        }
    }

    /**
     * 处理支付超时事件（由 MQ 消费者触发）
     * <p>
     * 超时处理流程：
     * <ol>
     *     <li>幂等校验：支付单存在、订单号匹配、仍为 PENDING 且已过期</li>
     *     <li>主动查询支付宝交易状态</li>
     *     <li>若已支付成功 → 补偿标记为已支付</li>
     *     <li>若已关闭 → 直接关闭本地支付单</li>
     *     <li>若仍等待付款 → 调用关单接口，关单后再次对账确认</li>
     * </ol>
     *
     * @param message 超时消息，包含支付单 ID、订单号和过期时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleTimeout(PaymentTimeoutMessage message) {
        // 幂等校验：确保支付单仍处于待支付且已过期状态
        OrderPayment payment = orderPaymentService.getById(message.paymentOrderId());
        if (payment == null || !payment.getOrderNo().equals(message.orderNo())
                || !PaymentOrderStatus.PENDING.name().equals(payment.getStatus())
                || payment.getExpireAt().isAfter(LocalDateTime.now())) {
            return;
        }

        // 第一步：主动查询支付宝侧交易状态
        AlipayTradeQueryResponse query = queryTradeForTimeout(payment.getOrderNo());
        if (query == null) {
            orderPaymentService.closePendingPayment(payment.getId(), PaymentCloseReason.TIMEOUT, LocalDateTime.now());
            return;
        }
        if (TRADE_SUCCESS.equals(query.getTradeStatus()) || TRADE_FINISHED.equals(query.getTradeStatus())) {
            // 补偿场景：用户已付款但回调丢失
            markTradeSuccess(payment.getOrderNo(), query.getTradeNo(), new BigDecimal(query.getTotalAmount()), formatPayTime(query));
            return;
        }
        if (TRADE_CLOSED.equals(query.getTradeStatus())) {
            // 支付宝侧已关闭，同步关闭本地支付单
            orderPaymentService.closePendingPayment(payment.getId(), PaymentCloseReason.TIMEOUT, LocalDateTime.now());
            return;
        }
        if (!WAIT_BUYER_PAY.equals(query.getTradeStatus())) {
            throw BusinessException.REMOTE_ERROR.newInstance("Unexpected Alipay trade status " + query.getTradeStatus());
        }

        // 第二步：交易仍在等待付款，主动调用关单接口
        AlipayTradeCloseResponse close = closeTrade(payment.getOrderNo());
        if (close.isSuccess()) {
            orderPaymentService.closePendingPayment(payment.getId(), PaymentCloseReason.TIMEOUT, LocalDateTime.now());
            return;
        }

        // 第三步：关单失败，再次查询对账（防止关单瞬间用户完成付款）
        AlipayTradeQueryResponse reconciled = queryTradeForTimeout(payment.getOrderNo());
        if (reconciled == null) {
            orderPaymentService.closePendingPayment(payment.getId(), PaymentCloseReason.TIMEOUT, LocalDateTime.now());
            return;
        }
        if (TRADE_SUCCESS.equals(reconciled.getTradeStatus()) || TRADE_FINISHED.equals(reconciled.getTradeStatus())) {
            markTradeSuccess(payment.getOrderNo(), reconciled.getTradeNo(), new BigDecimal(reconciled.getTotalAmount()), formatPayTime(reconciled));
            return;
        }
        if (TRADE_CLOSED.equals(reconciled.getTradeStatus())) {
            orderPaymentService.closePendingPayment(payment.getId(), PaymentCloseReason.TIMEOUT, LocalDateTime.now());
            return;
        }
        // 关单和对账均失败，抛出异常触发 MQ 重试
        throw BusinessException.REMOTE_ERROR.newInstance("Alipay close failed: " + close.getSubMsg());
    }

    @Override
    public AlipayRefundRes refund(OrderPayment payment, PaymentRefundRequest refundRequest) {
        if (!ALIPAY.equals(payment.getPaymentChannel())) {
            throw BusinessException.DATA_ERROR.newInstance("Unsupported payment channel for refund");
        }
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(payment.getOrderNo());
        model.setRefundAmount(refundRequest.getRefundAmount().toPlainString());
        model.setOutRequestNo(refundRequest.getRefundNo());
        model.setRefundReason(refundRequest.getReason());
        request.setBizModel(model);
        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                throw BusinessException.REMOTE_ERROR.newInstance("Alipay refund failed: " + response.getSubMsg());
            }
            return new AlipayRefundRes(response.getTradeNo());
        } catch (AlipayApiException ex) {
            throw BusinessException.REMOTE_ERROR.newInstance("Alipay refund request failed: " + ex.getErrMsg());
        }
    }

    /**
     * 调用支付宝 SDK 创建电脑网站支付页面
     *
     * @param payment         支付单
     * @param prepaymentOrder 预付款订单（用于获取商品名称）
     * @return 支付页面 HTML 及商户订单号
     */
    private AlipayTradeCreateRes createAlipayPage(OrderPayment payment, MarketPrepaymentOrder prepaymentOrder) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 设置异步通知地址（支付宝服务器回调）
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        // 设置同步跳转地址（用户支付完成后浏览器跳转）
        request.setReturnUrl(alipayProperties.getReturnUrl());

        // 构建支付业务参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(payment.getOrderNo());
        model.setTotalAmount(payment.getOrderAmount().toPlainString());
        model.setSubject(prepaymentOrder.getProductName() + " payment");
        model.setBody("Payment order " + payment.getOrderNo());
        model.setProductCode("FAST_INSTANT_TRADE_PAY"); // 电脑网站支付产品码
        model.setTimeoutExpress(paymentProperties.getTimeoutMinutes() + "m"); // 超时关闭时间
        request.setBizModel(model);

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "POST");
            if (!response.isSuccess()) {
                throw BusinessException.REMOTE_ERROR.newInstance("Alipay create failed: " + response.getSubMsg());
            }
            return new AlipayTradeCreateRes(response.getBody(), payment.getOrderNo());
        } catch (AlipayApiException ex) {
            throw BusinessException.REMOTE_ERROR.newInstance("Alipay create request failed: " + ex.getErrMsg());
        }
    }

    /**
     * 标记交易支付成功
     * <p>
     * 若为首次标记成功（幂等），则同步更新学生状态为「在校」
     *
     * @param orderNo     商户订单号
     * @param tradeNo     支付宝交易号
     * @param amount      实际支付金额
     * @param paymentTime 支付时间字符串
     */
    private void markTradeSuccess(String orderNo, String tradeNo, BigDecimal amount, String paymentTime) {
        // 幂等标记：若已支付过则直接返回
        boolean newlyPaid = orderPaymentService.markPaid(orderNo, tradeNo, amount, parsePaymentTime(paymentTime));
        if (!newlyPaid) {
            return;
        }
        // 支付成功后，将对应学生状态更新为在校
        OrderPayment payment = orderPaymentService.requireByOrderNo(orderNo);
        Student student = studentService.lambdaQuery().eq(Student::getPhone, payment.getStudentPhone()).one();
        if (student != null && !Student.AT_SCHOOL.equals(student.getStatus())) {
            student.setStatus(Student.AT_SCHOOL);
            student.setUpdatedAt(LocalDateTime.now());
            studentService.updateById(student);
        }
    }

    /**
     * 查询支付宝交易状态
     *
     * @param orderNo 商户订单号
     * @return 支付宝交易查询响应
     * @throws BusinessException 查询失败时抛出
     */
    private AlipayTradeQueryResponse queryTrade(String orderNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(orderNo);
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (!response.isSuccess()) {
                throw BusinessException.REMOTE_ERROR.newInstance("Alipay query failed: " + response.getSubMsg());
            }
            return response;
        } catch (AlipayApiException ex) {
            throw BusinessException.REMOTE_ERROR.newInstance("Alipay query request failed: " + ex.getErrMsg());
        }
    }

    /**
     * 页面支付只生成收银台表单，买家尚未进入收银台时支付宝可能还没有创建交易。
     * 对超时关单而言，该状态等价于未支付且无需再调用关单接口。
     */
    private AlipayTradeQueryResponse queryTradeForTimeout(String orderNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(orderNo);
        request.setBizModel(model);
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response;
            }
            if ("ACQ.TRADE_NOT_EXIST".equals(response.getSubCode())) {
                log.info("Alipay trade does not exist at timeout; closing local payment order, orderNo={}", orderNo);
                return null;
            }
            throw BusinessException.REMOTE_ERROR.newInstance("Alipay query failed: " + response.getSubMsg());
        } catch (AlipayApiException ex) {
            throw BusinessException.REMOTE_ERROR.newInstance("Alipay query request failed: " + ex.getErrMsg());
        }
    }

    /**
     * 关闭支付宝交易（未付款超时关单）
     *
     * @param orderNo 商户订单号
     * @return 支付宝关单响应
     * @throws BusinessException 请求异常时抛出
     */
    private AlipayTradeCloseResponse closeTrade(String orderNo) {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        model.setOutTradeNo(orderNo);
        request.setBizModel(model);
        try {
            return alipayClient.execute(request);
        } catch (AlipayApiException ex) {
            throw BusinessException.REMOTE_ERROR.newInstance("Alipay close request failed: " + ex.getErrMsg());
        }
    }

    /**
     * 生成唯一商户订单号，格式：YQP + 32位UUID
     */
    private String newOrderNo() {
        return "YQP" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 解析支付宝回调中的金额字符串为 BigDecimal
     *
     * @param amount 金额字符串
     * @return 解析后的金额
     * @throws BusinessException 金额为空时抛出
     */
    private BigDecimal parseAmount(String amount) {
        if (StrUtil.isBlank(amount)) {
            throw BusinessException.DATA_ERROR.newInstance("Alipay notification amount is missing");
        }
        return new BigDecimal(amount);
    }

    /**
     * 解析支付时间字符串为 LocalDateTime
     * <p>
     * 若为空则返回当前时间（兜底策略）
     *
     * @param paymentTime 支付时间字符串，格式 yyyy-MM-dd HH:mm:ss
     * @return 解析后的支付时间
     */
    private LocalDateTime parsePaymentTime(String paymentTime) {
        if (StrUtil.isBlank(paymentTime)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(paymentTime, PAY_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw BusinessException.DATA_ERROR.newInstance("Invalid Alipay payment time");
        }
    }

    /**
     * 将支付宝查询响应中的付款时间格式化为标准字符串
     *
     * @param response 支付宝交易查询响应
     * @return 格式化后的支付时间字符串，若为空返回 null
     */
    private String formatPayTime(AlipayTradeQueryResponse response) {
        return response.getSendPayDate() == null ? null : PAY_TIME_FORMATTER.format(response.getSendPayDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
    }
}
