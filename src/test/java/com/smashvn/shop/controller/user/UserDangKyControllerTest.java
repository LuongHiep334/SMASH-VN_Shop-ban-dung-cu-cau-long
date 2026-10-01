package com.smashvn.shop.controller.user;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smashvn.shop.security.RegisterRateLimiter;
import com.smashvn.shop.service.user.UserDangKyService;

@ExtendWith(MockitoExtension.class)
class UserDangKyControllerTest {

    @Mock
    private UserDangKyService userDangKyService;

    @Mock
    private RegisterRateLimiter registerRateLimiter;

    @InjectMocks
    private UserDangKyController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(registerRateLimiter.isBlocked(anyString())).thenReturn(false);
    }

    @Test
    void weakPasswordErrorIsAttachedToPasswordField() throws Exception {
        when(userDangKyService.dangKy("user@gmail.com", "password1"))
                .thenThrow(new RuntimeException("Mật khẩu phải có ít nhất 1 chữ in hoa!"));

        mockMvc.perform(post("/user/dang-ky")
                        .param("username", " user@gmail.com ")
                        .param("matKhau", "password1")
                        .param("xacNhanMatKhau", "password1"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("passwordError", "Mật khẩu phải có ít nhất 1 chữ in hoa!"))
                .andExpect(model().attribute("username", "user@gmail.com"))
                .andExpect(model().attributeDoesNotExist("loi"));
    }

    @Test
    void duplicateEmailErrorIsAttachedToUsernameField() throws Exception {
        when(userDangKyService.dangKy("used@gmail.com", "Secure123"))
                .thenThrow(new RuntimeException("Email này đã được sử dụng!"));

        mockMvc.perform(post("/user/dang-ky")
                        .param("username", "used@gmail.com")
                        .param("matKhau", "Secure123")
                        .param("xacNhanMatKhau", "Secure123"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("usernameError", "Email này đã được sử dụng!"))
                .andExpect(model().attribute("username", "used@gmail.com"))
                .andExpect(model().attributeDoesNotExist("loi"));
    }

    @Test
    void mismatchedConfirmationKeepsUsernameAndTargetsConfirmationField() throws Exception {
        mockMvc.perform(post("/user/dang-ky")
                        .param("username", "user@gmail.com")
                        .param("matKhau", "Secure123")
                        .param("xacNhanMatKhau", "Secure124"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("confirmError", "Mật khẩu xác nhận không trùng khớp!"))
                .andExpect(model().attribute("username", "user@gmail.com"));

        verify(userDangKyService, never()).dangKy(anyString(), anyString());
    }

    @Test
    void validRegistrationRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/user/dang-ky")
                        .param("username", "user@gmail.com")
                        .param("matKhau", "Secure123")
                        .param("xacNhanMatKhau", "Secure123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap?thanhcong"));

        verify(registerRateLimiter).registerSucceeded(anyString());
    }
}
