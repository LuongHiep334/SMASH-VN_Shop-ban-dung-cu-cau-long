package com.smashvn.shop.controller.user;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class SignupTemplateVisibilityTest {

    @Test
    void initialPageHidesIdentifierHelpAndPasswordRequirements() throws IOException {
        String html = readClasspathResource("templates/signup.html");

        assertTrue(html.contains("id=\"username-help\" hidden"));
        assertTrue(html.contains("id=\"password-requirements\" hidden"));
    }

    @Test
    void identifierHelpOnlyAppearsDuringInteraction() throws IOException {
        String html = readClasspathResource("templates/signup.html");

        assertTrue(html.contains("usernameInput.addEventListener(\"focus\""));
        assertTrue(html.contains("usernameInput.addEventListener(\"input\""));
        assertTrue(html.contains("usernameInput.addEventListener(\"blur\""));
        assertTrue(html.contains("usernameHelp.hidden = false;"));
        assertTrue(html.contains("usernameHelp.hidden = true;"));
    }

    @Test
    void hiddenHelpersAreNotDisplayedByCss() throws IOException {
        String css = readClasspathResource("static/css/app.css");

        assertTrue(css.contains(".form-field__help[hidden]"));
        assertTrue(css.contains(".password-requirements[hidden]"));
    }

    private String readClasspathResource(String path) throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Không tìm thấy tài nguyên kiểm thử: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
