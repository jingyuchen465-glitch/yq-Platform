package com.itcjy.emp.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.itcjy.common.properties.AlipayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Bean
    public AlipayClient alipayClient(AlipayProperties props) {
        return new DefaultAlipayClient(
                props.getServerUrl(),
                props.getAppId(),
                props.getPrivateKey(),
                props.getFormat(),
                props.getCharset(),
                props.getAlipayPublicKey(),
                props.getSignType()
        );
    }
}
