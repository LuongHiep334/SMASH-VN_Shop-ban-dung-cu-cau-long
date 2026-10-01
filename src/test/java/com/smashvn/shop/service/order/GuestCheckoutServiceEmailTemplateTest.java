package com.smashvn.shop.service.order;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class GuestCheckoutServiceEmailTemplateTest {

    @Test
    void activationEmailUsesWebsiteBrandAndEscapesActivationUrl() {
        String html = GuestCheckoutService.buildGuestActivationEmailHtml(
                "http://localhost:8080/user/thiet-lap-mat-khau?token=abc&source=email");

        assertTrue(html.contains("cid:smashLogo"));
        assertTrue(html.contains("border-top: 4px solid #ff4500"));
        assertTrue(html.contains("background-color: #000000"));
        assertTrue(html.contains("token=abc&amp;source=email"));
        assertFalse(html.contains("background-color: #1e2229"));
        assertFalse(html.contains("{{ACTIVATION_URL}}"));
        assertTrue(new ClassPathResource("static/images/logo/logo-2.png").exists());
    }
}
