package com.itcjy.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.alipay")
public class AlipayProperties {

    /**
     * 支付宝应用APPID
     */
    private String appId;

    /**
     * 应用私钥（PKCS8格式）
     */
    private String privateKey;

    /**
     * 支付宝公钥（用于验签）
     */
    private String alipayPublicKey;

    /**
     * 支付宝网关地址
     * 沙箱：https://openapi-sandbox.dl.alipaydev.com/gateway.do
     * 生产：https://openapi.alipay.com/gateway.do
     */
    private String serverUrl;

    /**
     * 支付成功异步通知回调地址（后端接口）
     */
    private String notifyUrl;

    /**
     * 支付完成同步跳转地址（前端页面）
     */
    private String returnUrl;

    /**
     * 签名算法类型，默认 RSA2
     */
    private String signType = "RSA2";

    /**
     * 字符编码，默认 UTF-8
     */
    private String charset = "UTF-8";

    /**
     * 响应格式，默认 json
     */
    private String format = "json";
}
