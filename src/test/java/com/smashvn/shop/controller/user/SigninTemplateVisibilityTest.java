package com.smashvn.shop.controller.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class SigninTemplateVisibilityTest {

    @Test
    void initialPageKeepsValidationMessagesHidden() throws IOException {
        Document document = Jsoup.parse(readTemplate());
        Element form = document.selectFirst("form.l-f-o__form");
        Element usernameError = document.getElementById("username-error");
        Element passwordError = document.getElementById("password-error");
        Element loginAlert = document.getElementById("login-alert");

        assertNotNull(form);
        assertTrue(form.hasAttr("novalidate"));
        assertNotNull(usernameError);
        assertNotNull(passwordError);
        assertTrue(usernameError.hasAttr("th:hidden"));
        assertTrue(passwordError.hasAttr("th:hidden"));
        assertNotNull(loginAlert);
        assertTrue(loginAlert.hasAttr("th:if"));
    }

    @Test
    void scriptUsesTheRealIdentifierFieldAndAcceptsVietnamesePhones() throws IOException {
        String html = readTemplate();

        assertTrue(html.contains("document.getElementById(\"login-username\")"));
        assertFalse(html.contains("document.getElementById(\"login-email\")"));
        assertTrue(html.contains("const vietnamPhoneRegex"));
        assertTrue(html.contains("normalizePhone(value)"));
        assertTrue(html.contains("usernameInput.addEventListener(\"blur\""));
        assertTrue(html.contains("passwordInput.addEventListener(\"blur\""));
    }

    @Test
    void customerLoginAcceptsOnlyEmailOrPhoneAndUsesFieldSpecificMessages() throws IOException {
        String html = readTemplate();

        assertFalse(html.contains("const usernameRegex"));
        assertFalse(html.contains("validLegacyUsername"));
        assertFalse(html.contains("credentialError"));
        assertTrue(html.contains("Định dạng email không hợp lệ."));
        assertTrue(html.contains("Số điện thoại Việt Nam không hợp lệ."));
        assertTrue(html.contains("th:text=\"${usernameError}\""));
        assertTrue(html.contains("th:text=\"${passwordError}\""));
    }

    @Test
    void loginDoesNotApplyRegistrationPasswordRules() throws IOException {
        Document document = Jsoup.parse(readTemplate());
        Element password = document.getElementById("login-password");

        assertNotNull(password);
        assertFalse(password.hasAttr("minlength"));
        assertFalse(password.hasAttr("maxlength"));
        assertEquals("current-password", password.attr("autocomplete"));
    }

    @Test
    void usesOnlyTheExistingCreateAccountEntryPoint() throws IOException {
        String html = readTemplate();

        assertTrue(html.contains("th:href=\"@{/user/dang-ky}\""));
        assertFalse(html.contains("unregistered-banner"));
        assertFalse(html.contains("@{/user/dang-ky(email="));
        assertFalse(html.contains("Đi đến đăng ký"));
        assertFalse(html.contains("resetLoginForm"));
    }

    private String readTemplate() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("templates/signin.html")) {
            assertNotNull(input, "Không tìm thấy templates/signin.html");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
