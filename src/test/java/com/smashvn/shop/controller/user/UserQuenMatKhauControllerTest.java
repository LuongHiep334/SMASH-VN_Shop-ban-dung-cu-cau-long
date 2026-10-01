package com.smashvn.shop.controller.user;

import com.smashvn.shop.security.ForgotPasswordRateLimiter;
import com.smashvn.shop.service.user.UserQuenMatKhauService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserQuenMatKhauControllerTest {

    @Mock
    private UserQuenMatKhauService quenMatKhauService;

    @Mock
    private ForgotPasswordRateLimiter rateLimiter;

    @Mock
    private HttpServletRequest request;

    private UserQuenMatKhauController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new UserQuenMatKhauController(quenMatKhauService, rateLimiter);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
    }

    @Test
    void testHienThiTrangQuenMK() {
        assertEquals("lost-password", controller.hienThiTrangQuenMK());
    }

    @Test
    void testHienThiTrangQuenMK_WithEmail() {
        Model model = new ConcurrentModel();
        String view = controller.hienThiTrangQuenMK("guest@example.com", model);
        assertEquals("lost-password", view);
        assertEquals("guest@example.com", model.getAttribute("email"));
    }

    @Test
    void testXuLyQuenMK_Success() {
        String email = "test@gmail.com";
        when(rateLimiter.isBlocked("127.0.0.1")).thenReturn(false);

        Model model = new ConcurrentModel();
        String view = controller.xuLyQuenMK(email, request, model);

        assertEquals("lost-password", view);
        assertNotNull(model.getAttribute("thongBao"));
        verify(rateLimiter).forgotPasswordSucceeded("127.0.0.1");
        verify(quenMatKhauService).guiYeuCauKhoiPhuc(email, "http://localhost:8080");
    }

    @Test
    void testXuLyQuenMK_Blocked() {
        String email = "test@gmail.com";
        when(rateLimiter.isBlocked("127.0.0.1")).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = controller.xuLyQuenMK(email, request, model);

        assertEquals("lost-password", view);
        assertTrue(model.getAttribute("loi").toString().contains("bị chặn"));
        verifyNoInteractions(quenMatKhauService);
    }

    @Test
    void testXuLyQuenMK_InvalidEmailFormat() {
        String email = "invalid-email";
        when(rateLimiter.isBlocked("127.0.0.1")).thenReturn(false);

        Model model = new ConcurrentModel();
        String view = controller.xuLyQuenMK(email, request, model);

        assertEquals("lost-password", view);
        assertEquals("Định dạng email không hợp lệ!", model.getAttribute("loi"));
        verify(rateLimiter).forgotPasswordFailed("127.0.0.1");
        verifyNoInteractions(quenMatKhauService);
    }

    @Test
    void testXuLyQuenMK_EmailTooLong() {
        String email = "a".repeat(95) + "@g.com"; // > 100 chars
        when(rateLimiter.isBlocked("127.0.0.1")).thenReturn(false);

        Model model = new ConcurrentModel();
        String view = controller.xuLyQuenMK(email, request, model);

        assertEquals("lost-password", view);
        assertEquals("Email không được vượt quá 100 ký tự!", model.getAttribute("loi"));
        verify(rateLimiter).forgotPasswordFailed("127.0.0.1");
        verifyNoInteractions(quenMatKhauService);
    }

    @Test
    void testXuLyDatLaiMK_Success() {
        String token = "valid-token";
        String pass = "SecurePass123";
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = controller.xuLyDatLaiMK(token, pass, pass, redirectAttributes, model);

        assertEquals("redirect:/user/dang-nhap", view);
        verify(quenMatKhauService).datLaiMatKhau(token, pass);
    }

    @Test
    void testXuLyDatLaiMK_MismatchPassword() {
        String token = "valid-token";
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = controller.xuLyDatLaiMK(token, "SecurePass123", "Mismatch123", redirectAttributes, model);

        assertEquals("reset-password", view);
        assertEquals("Mật khẩu xác nhận không trùng khớp!", model.getAttribute("loi"));
        verifyNoInteractions(quenMatKhauService);
    }

    @Test
    void testXuLyDatLaiMK_WeakPassword() {
        String token = "valid-token";
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = controller.xuLyDatLaiMK(token, "weak", "weak", redirectAttributes, model);

        assertEquals("reset-password", view);
        assertEquals("Mật khẩu phải dài từ 8 đến 30 ký tự!", model.getAttribute("loi"));
        verifyNoInteractions(quenMatKhauService);
    }
}
