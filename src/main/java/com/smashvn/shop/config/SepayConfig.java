package com.smashvn.shop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sepay")
@Data
@lombok.ToString(exclude = {"apiKey", "secretKey", "ipnSecret"})
public class SepayConfig {
    private String apiKey;
    private String secretKey;
    private String ipnSecret;
    private String baseUrl;
    private String bankAccount;
    private String bankName;
    private String memoPrefix;
    private boolean debug = false;
    private boolean ipVerification = false;
    private String ipRanges;
}
