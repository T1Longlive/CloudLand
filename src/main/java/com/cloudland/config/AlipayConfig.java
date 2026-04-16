package com.cloudland.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Value("${alipay.appId}")
    private String appId;
    @Value("${alipay.privateKey}")
    private String privateKey;
    @Value("${alipay.publicKey}")
    private String publicKey;
    @Value("${alipay.gatewayUrl}")
    private String gatewayUrl;

    @Bean
    public AlipayClient alipayClient() throws Exception {
        return new DefaultAlipayClient(gatewayUrl, appId, privateKey, "json", "UTF-8", publicKey, "RSA2");
    }
}
