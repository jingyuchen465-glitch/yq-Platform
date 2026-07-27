package com.itcjy.emp.service.impl.pay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.itcjy.common.exception.BusinessException;
import com.itcjy.common.properties.AlipayProperties;
import com.itcjy.emp.pojo.req.pay.AlipayTradeCreateReq;
import com.itcjy.emp.pojo.res.pay.AlipayTradeCreateRes;
import com.itcjy.emp.pojo.res.pay.AlipayTradeQueryRes;
import com.itcjy.emp.service.pay.IAlipayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * 支付宝支付服务实现类
 * <p>
 * 封装支付宝开放平台的交易能力，包括：
 * <ul>
 *     <li>电脑网站支付（页面跳转下单）</li>
 *     <li>交易状态查询</li>
 *     <li>异步通知（回调）处理与验签</li>
 * </ul>
 *
 * @author itcjy
 * @see IAlipayService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlipayServiceImpl implements IAlipayService {

    /** 支付宝SDK客户端，由配置类注入 */
    private final AlipayClient alipayClient;

    /** 支付宝配置属性（appId、密钥、回调地址等） */
    private final AlipayProperties alipayProperties;

    /**
     * 创建电脑网站支付订单（PC端页面跳转支付）
     * <p>
     * 调用支付宝 alipay.trade.page.pay 接口，生成可直接提交的支付表单HTML。
     * 前端拿到返回的 form 表单后自动跳转至支付宝收银台。
     *
     * @param req 下单请求参数（商户订单号、金额、标题等）
     * @return 包含支付表单HTML和商户订单号的响应对象
     * @throws BusinessException 下单失败或SDK调用异常时抛出
     */
    @Override
    public AlipayTradeCreateRes createTradePagePay(AlipayTradeCreateReq req) {
        // 构建支付请求，设置异步通知和同步跳转地址
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayProperties.getNotifyUrl());
        request.setReturnUrl(alipayProperties.getReturnUrl());

        // 组装业务参数
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(req.getOutTradeNo());
        model.setTotalAmount(req.getTotalAmount().toPlainString());
        model.setSubject(req.getSubject());
        // 固定为电脑网站支付产品码
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        // 可选参数：商品描述
        if (req.getBody() != null) {
            model.setBody(req.getBody());
        }
        // 可选参数：订单超时时间（如 "30m"）
        if (req.getTimeoutExpress() != null) {
            model.setTimeoutExpress(req.getTimeoutExpress());
        }

        request.setBizModel(model);

        try {
            AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "POST");
            if (response.isSuccess()) {
                log.info("支付宝下单成功，outTradeNo={}", req.getOutTradeNo());
                return new AlipayTradeCreateRes(response.getBody(), req.getOutTradeNo());
            } else {
                log.error("支付宝下单失败，outTradeNo={}，code={}，msg={}，subMsg={}",
                        req.getOutTradeNo(), response.getCode(), response.getMsg(), response.getSubMsg());
                throw new BusinessException(21001, "支付宝下单失败：" + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝SDK调用异常，outTradeNo={}", req.getOutTradeNo(), e);
            throw new BusinessException(21002, "支付宝接口调用异常：" + e.getErrMsg());
        }
    }

    /**
     * 查询交易状态
     * <p>
     * 调用支付宝 alipay.trade.query 接口，根据商户订单号查询交易的最新状态。
     * 可用于前端轮询支付结果或后台对账。
     *
     * @param outTradeNo 商户订单号
     * @return 交易查询结果（交易号、状态、金额、买家账号等）
     * @throws BusinessException 查询失败或SDK调用异常时抛出
     */
    @Override
    public AlipayTradeQueryRes queryTrade(String outTradeNo) {
        // 构建查询请求
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        request.setBizModel(model);

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                // 组装查询结果响应对象
                AlipayTradeQueryRes res = new AlipayTradeQueryRes();
                res.setTradeNo(response.getTradeNo());           // 支付宝交易号
                res.setOutTradeNo(response.getOutTradeNo());     // 商户订单号
                res.setTradeStatus(response.getTradeStatus());   // 交易状态
                res.setTotalAmount(new BigDecimal(response.getTotalAmount())); // 订单金额
                res.setBuyerLogonId(response.getBuyerLogonId()); // 买家支付宝账号
                res.setSendPayDate(response.getSendPayDate() != null
                        ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(response.getSendPayDate())
                        : null); // 打款时间，格式化为字符串
                return res;
            } else {
                log.error("支付宝查询失败，outTradeNo={}，code={}，subMsg={}",
                        outTradeNo, response.getCode(), response.getSubMsg());
                throw new BusinessException(21003, "支付宝查询失败：" + response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝查询SDK异常，outTradeNo={}", outTradeNo, e);
            throw new BusinessException(21002, "支付宝接口调用异常：" + e.getErrMsg());
        }
    }

    /**
     * 处理支付宝异步通知（回调）
     * <p>
     * 支付宝在用户完成支付后，会向 notifyUrl 发送 POST 异步通知。
     * 本方法完成以下处理流程：
     * <ol>
     *     <li>RSA 验签，确保通知来源合法</li>
     *     <li>校验 app_id 是否为本应用</li>
     *     <li>解析交易状态并执行对应业务逻辑</li>
     * </ol>
     *
     * @param params 支付宝回调的全部参数（已由Controller层转为Map）
     * @return true-通知处理成功（返回 "success" 给支付宝）；false-验签失败或处理异常
     */
    @Override
    public boolean handleNotify(Map<String, String> params) {
        try {
            // 1. 验签
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );
            if (!signVerified) {
                log.warn("支付宝异步通知验签失败，params={}", params);
                return false;
            }

            // 2. 校验 appId 是否匹配
            String appId = params.get("app_id");
            if (!alipayProperties.getAppId().equals(appId)) {
                log.warn("支付宝异步通知appId不匹配，expected={}，actual={}", alipayProperties.getAppId(), appId);
                return false;
            }

            // 3. 获取交易状态
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String totalAmount = params.get("total_amount");

            log.info("支付宝异步通知：outTradeNo={}，tradeNo={}，tradeStatus={}，totalAmount={}",
                    outTradeNo, tradeNo, tradeStatus, totalAmount);

            // 4. 根据交易状态处理业务逻辑
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // TODO: 在此处处理支付成功的业务逻辑
                // 例如：更新订单状态、开通课程权限等
                log.info("支付成功，outTradeNo={}，tradeNo={}", outTradeNo, tradeNo);
            }

            return true;
        } catch (AlipayApiException e) {
            log.error("支付宝异步通知验签异常", e);
            return false;
        }
    }
}
