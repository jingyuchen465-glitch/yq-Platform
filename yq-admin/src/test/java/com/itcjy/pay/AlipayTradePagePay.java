package com.itcjy.pay;


import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.ExtUserInfo;
import com.alipay.api.domain.InvoiceKeyInfo;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.domain.InvoiceInfo;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.domain.ExtendParams;
import com.alipay.api.domain.GoodsDetail;
import com.alipay.api.domain.SubMerchant;

import java.util.ArrayList;
import java.util.List;

public class AlipayTradePagePay {

    public static void main(String[] args) throws AlipayApiException {
        // 初始化SDK
        AlipayClient alipayClient = new DefaultAlipayClient(getAlipayConfig());

        // 构造请求参数以调用接口
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();

        // 设置商户订单号
        model.setOutTradeNo("20150320010101001");

        // 设置订单总金额
        model.setTotalAmount("88.88");

        // 设置订单标题
        model.setSubject("Iphone6 16G");

        // 设置产品码
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        // 设置PC扫码支付的方式
        model.setQrPayMode("1");

        // 设置商户自定义二维码宽度
        model.setQrcodeWidth(100L);

        // 设置订单包含的商品列表信息
        List<GoodsDetail> goodsDetail = new ArrayList<GoodsDetail>();
        GoodsDetail goodsDetail0 = new GoodsDetail();
        goodsDetail0.setGoodsName("ipad");
        goodsDetail0.setAlipayGoodsId("20010001");
        goodsDetail0.setQuantity(1L);
        goodsDetail0.setPrice("2000");
        goodsDetail0.setGoodsId("apple-01");
        goodsDetail0.setGoodsCategory("34543238");
        goodsDetail0.setCategoriesTree("124868003|126232002|126252004");
        goodsDetail0.setShowUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        goodsDetail.add(goodsDetail0);
        model.setGoodsDetail(goodsDetail);

        // 设置订单绝对超时时间
        model.setTimeExpire("2016-12-31 10:05:01");

        // 设置二级商户信息
        SubMerchant subMerchant = new SubMerchant();
        subMerchant.setMerchantId("2088000603999128");
        subMerchant.setMerchantType("alipay");
        model.setSubMerchant(subMerchant);

        // 设置业务扩展参数
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088511833207846");
        extendParams.setHbFqSellerPercent("100");
        extendParams.setHbFqNum("3");
        extendParams.setIndustryRefluxInfo("{\"scene_code\":\"metro_tradeorder\",\"channel\":\"xxxx\",\"scene_data\":{\"asset_name\":\"ALIPAY\"}}");
        extendParams.setCardType("S0JP0000");
        model.setExtendParams(extendParams);

        // 设置商户传入业务信息
        model.setBusinessParams("{\"mc_create_trade_ip\":\"127.0.0.1\"}");

        // 设置优惠参数
        model.setPromoParams("{\"storeIdType\":\"1\"}");

        // 设置请求后页面的集成方式
        model.setIntegrationType("PCWEB");

        // 设置请求来源地址
        model.setRequestFromUrl("https://");

        // 设置商户门店编号
        model.setStoreId("NJ_001");

        // 设置商户的原始订单号
        model.setMerchantOrderNo("20161008001");

        // 设置外部指定买家
        ExtUserInfo extUserInfo = new ExtUserInfo();
        extUserInfo.setCertType("IDENTITY_CARD");
        extUserInfo.setCertNo("362334768769238881");
        extUserInfo.setMobile("16587658765");
        extUserInfo.setName("李明");
        extUserInfo.setMinAge("18");
        extUserInfo.setNeedCheckInfo("F");
        model.setExtUserInfo(extUserInfo);

        // 设置开票信息
        InvoiceInfo invoiceInfo = new InvoiceInfo();
        InvoiceKeyInfo keyInfo = new InvoiceKeyInfo();
        keyInfo.setTaxNum("1464888883494");
        keyInfo.setIsSupportInvoice(true);
        keyInfo.setInvoiceMerchantName("ABC|003");
        invoiceInfo.setKeyInfo(keyInfo);
        invoiceInfo.setDetails("[{\"code\":\"100294400\",\"name\":\"服饰\",\"num\":\"2\",\"sumPrice\":\"200.00\",\"taxRate\":\"6%\"}]");
        model.setInvoiceInfo(invoiceInfo);

        request.setBizModel(model);
        // 第三方代调用模式下请设置app_auth_token
        // request.putOtherTextParam("app_auth_token", "<-- 请填写应用授权令牌 -->");

        AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "POST");
        // 如果需要返回GET请求，请使用
        // AlipayTradePagePayResponse response = alipayClient.pageExecute(request, "GET");
        String pageRedirectionData = response.getBody();
        System.out.println(pageRedirectionData);

        if (response.isSuccess()) {
            System.out.println("调用成功");
        } else {
            System.out.println("调用失败");
            // sdk版本是"4.38.0.ALL"及以上,可以参考下面的示例获取诊断链接
            // String diagnosisUrl = DiagnosisUtils.getDiagnosisUrl(response);
            // System.out.println(diagnosisUrl);
        }
    }

    private static AlipayConfig getAlipayConfig() {
        String privateKey="MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCTPsGc8l11Jhac9O4+jZZbjsYf8O6GSak3OrdfgA7iuV2uOJAALRUfv3f2D9khyiUWOu/Mw2P4hZYSuR/ROxoDNGB981NCoo0yVjQRst6peaaqlv2KjDkvnJ4R/NOiMFNvXfwBWVQgn5SSwj3siJ4ra0/3U8yD0LOrjiH3MQV8dAtEPolk4M+EMxtTKJsiwp1764SGyPiJ/nYmkjB1QA7gs9q49Mzzju2LgK14FmPuEgx2GloorIaDWpT9NSNQDo16G+wsK0FiJGheqnLI8QC98A9A5Oood1kCbZQ9Yxng/mhc1/UeVO2cxnMJabHcE8kHx/QyVScN0eGlzd8UrcNnAgMBAAECggEALuIFfbaB4WIeQx2Nmic037QnC1aIbAOaOHaemm7IzpHc7TMfTp8MIc6yIIvNQ//8LQbZAaaY9zF5pj6Bjfo2fxy3OIM4WHgvYv6ll5kPYoDUe26WDf412x4QCoQo0nR2rHgNXFSX/8cv5EaqttyNGCAUFxj8fl/+RTp9QBrAt24z2GHBQUaF3pokE8MpWfcg2CXsiiC+5ShaEQPnKpZVUXdqx/HDJ0zco0L0iXX9vJXoF55/S6fc8mgyx+zJ8Lqhb2Hhi0bMwzNXFxPlJvOLdOcmMGpWCf9ZUcN92+2pBOvdcHEdOv/3zcpIbKdVrjvhH378jGwtW+oOjSO/XQX+aQKBgQDGt3s+BCEBfOzRuE6+yJxtQLb6oMGTl49mN3M7rndOEKk5yhX6/beEIrAEXnZiBVW2hA9j5OeckeAm2dHeRiza8ZlwEmaXTpoh1f3hkK3hJG+rg+iQ6HaK+PMIoWxaKMwHB88I1oB6dgP9zkig3d6tY2DxVVM0lORqYo7F8mnY3QKBgQC9sN99vQh98LN0I8w3lsjtlk/A2Nlv/VMvqtYt2rOan9GN1uEjPHgZ35N1BD9U8gCmHI6c4H5BCB+eb8ALE8/VOjwgsEUclmQnnOqww1zLtE/0JvnwQT/u1zsGfpAdLwDTGnv9fc6yzTmzs4gwRRYKY3cxPofbYe6TmYmCwN0nEwKBgQC5pSyD8YOMSlNMUi1u2ygYgT7wwkJWSl9zlRYYiZN34OTX0k3aoDFURru+OSnLxUQgMRvXrUAb68RHR3NjfpflK5S3ONmTPaGNPsfidTGchle1GpSo3CPzDUSvqW76g+aoIslubka1IY/1C0Sa1Ox1e8RTce070GXcZZsQnrn4GQKBgCnQxsOiGzAgEn8ksQ0ECVEOlJAvfWdJkb/tSf+buYH5fylkWAb7eoJBnfDqucdQ/cGgg/OjLVHKp6W2fsAyYttfVgfis6mEoZl15uqGdntVSVbKDMjgqWZOPe3FkqFMTkM7EGAmDp2tze2GQxxY7m3l1U2eujYrVXtmGkEfoODPAoGBAK0MCDSRw2+hJisHUO8vXGBEZDjawiJ7GEYjl4/biyC2qPPi0UTz9zXptE6L4WoCswVYt1EFnxUNmG3vBUKzr9d8DgZpQtVGHj0EflTR4svaSOK/agpR2oDHzs2Hr2RH/5UOc3FZIMMfk9hEhwJvamZhP/lUlv9mbDI7IrnGs8Pr";
        String alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArBGceYzvJoZaYgZydivsYs4q27QpeIyPeJnNmK6k1JqIFBhxgl62SKklHH50ypMb1jt/bpFLdTU3fI1xTAFsW1mjdyXwqEWjP3vuFD7uiEY3j3RHDId5eH0I66/mRc9DhWMfagHFfmobknWOp7/4ME53otMXCHegBSJd8SkpAKtVbKbLIGADRPNpntUEYxtKKa0H+t604/eDtC39eL0x+NsAl5PTgdvvDAxflQI+qqEFQlgTF7tGF6EAFc6yn7adsWhNWnMRER8VT06s/WmxSTKqVanM5gs74Gs4e7AMladnEZTNrEfmLG3DaSLjOWeNtso3j2zxjsu6WXMELuM2KwIDAQAB";
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl("https://openapi-sandbox.dl.alipaydev.com/gateway.do");
        alipayConfig.setAppId("9021000165644712");
        alipayConfig.setPrivateKey(privateKey);
        alipayConfig.setFormat("json");
        alipayConfig.setAlipayPublicKey(alipayPublicKey);
        alipayConfig.setCharset("UTF-8");
        alipayConfig.setSignType("RSA2");
        return alipayConfig;
    }
}