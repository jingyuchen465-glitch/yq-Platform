package com.itcjy.emp.service.pay;

import com.itcjy.emp.pojo.req.pay.AlipayTradeCreateReq;
import com.itcjy.emp.pojo.res.pay.AlipayTradeCreateRes;
import com.itcjy.emp.pojo.res.pay.AlipayTradeQueryRes;

import java.util.Map;

public interface IAlipayService {

    /**
     * 创建PC网站支付订单，返回支付宝收银台跳转表单HTML
     */
    AlipayTradeCreateRes createTradePagePay(AlipayTradeCreateReq req);

    /**
     * 主动查询订单支付状态
     */
    AlipayTradeQueryRes queryTrade(String outTradeNo);

    /**
     * 处理支付宝异步通知（验签 + 业务处理）
     *
     * @param params 支付宝POST过来的全部参数
     * @return 验签及业务处理是否成功
     */
    boolean handleNotify(Map<String, String> params);
}
