package com.smashvn.shop.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Data
public class GhnConfig {

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private Integer shopId;

    @Value("${ghn.base-url}")
    private String baseUrl;

    @Value("${ghn.from-district-id}")
    private Integer fromDistrictId;

    @Value("${ghn.from-ward-code}")
    private String fromWardCode;

    @Value("${ghn.from-address}")
    private String fromAddress;

    @Value("${ghn.webhook-token:}")
    private String webhookToken;


    @Bean
    public RestTemplate restTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }
}
