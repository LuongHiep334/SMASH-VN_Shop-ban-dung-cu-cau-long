package com.smashvn.shop.controller.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import com.smashvn.shop.config.ShopContactProperties;

class GlobalShopContactAdviceTest {

    @Test
    void exposesConfiguredWarehouseAddressToAllViews() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("ghn.from-address", "10 Kim Mã, Ba Đình, Hà Nội")
                .withProperty("shop.contact.address", "${ghn.from-address}");
        ShopContactProperties properties = Binder.get(environment)
                .bind("shop.contact", Bindable.of(ShopContactProperties.class))
                .orElseThrow(() -> new AssertionError("Không ánh xạ được địa chỉ liên hệ của shop"));

        GlobalShopContactAdvice advice = new GlobalShopContactAdvice(properties);

        assertEquals("10 Kim Mã, Ba Đình, Hà Nội", advice.getShopAddress());
    }
}
