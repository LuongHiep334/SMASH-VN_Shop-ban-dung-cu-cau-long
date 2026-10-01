package com.smashvn.shop.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.smashvn.shop.config.ShopContactProperties;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalShopContactAdvice {

    private final ShopContactProperties shopContactProperties;

    @ModelAttribute("shopAddress")
    public String getShopAddress() {
        return shopContactProperties.getAddress();
    }
}
