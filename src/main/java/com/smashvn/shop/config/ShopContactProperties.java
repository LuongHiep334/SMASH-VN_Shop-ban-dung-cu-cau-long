package com.smashvn.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "shop.contact")
@Data
public class ShopContactProperties {
    private String address = "";
    private String email = "";
    private String phone = "";
}
