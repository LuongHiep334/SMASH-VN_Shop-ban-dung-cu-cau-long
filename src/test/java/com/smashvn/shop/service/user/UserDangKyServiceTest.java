package com.smashvn.shop.service.user;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.NewsletterSubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserDangKyServiceTest {

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private KhachHangRepository khachHangRepository;

    @Mock
    private NewsletterSubscriberRepository newsletterSubscriberRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private UserDangKyService userDangKyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Mock default behavior for newsletter subscriber check
        when(newsletterSubscriberRepository.findByEmail(any(String.class))).thenReturn(java.util.Optional.empty());
        userDangKyService = new UserDangKyService(taiKhoanRepository, khachHangRepository, newsletterSubscriberRepository, passwordEncoder);
    }

    @Test
    void testDangKy_Success() {
        String email = "newcustomer@gmail.com";
        String matKhau = "SecurePass123";

        when(taiKhoanRepository.existsByUsername(email)).thenReturn(false);
        
        // Mock save returning the same entity
        when(taiKhoanRepository.save(any(TaiKhoan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaiKhoan result = userDangKyService.dangKy(email, matKhau);

        assertNotNull(result);
        assertEquals(email, result.getUsername());

        assertEquals("KH", result.getVaiTro());
        assertEquals("hoat_dong", result.getTrangThai());

        // Verify profile is created
        ArgumentCaptor<KhachHang> profileCaptor = ArgumentCaptor.forClass(KhachHang.class);
        verify(khachHangRepository).save(profileCaptor.capture());
        
        KhachHang createdProfile = profileCaptor.getValue();
        assertNotNull(createdProfile);
        assertEquals("Người dùng mới", createdProfile.getTenKh());
        assertEquals(result, createdProfile.getTaiKhoan());
    }

    @Test
    void testDangKy_DuplicateEmail() {
        String email = "duplicate@gmail.com";
        String matKhau = "SecurePass123";

        when(taiKhoanRepository.existsByUsername(email)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Email này đã được sử dụng!", exception.getMessage());
        verify(taiKhoanRepository, never()).save(any(TaiKhoan.class));
    }

    @Test
    void testDangKy_InvalidEmailFormat() {
        String email = "invalid-email";
        String matKhau = "SecurePass123";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Định dạng email không hợp lệ!", exception.getMessage());
    }

    @Test
    void testDangKy_EmptyEmail() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy("", "SecurePass123");
        });
        assertEquals("Email không được để trống!", exception.getMessage());
    }

    @Test
    void testDangKy_ExcessiveEmailLength() {
        String longEmail = "a".repeat(95) + "@g.com"; // > 100 chars
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(longEmail, "SecurePass123");
        });
        assertEquals("Email không được vượt quá 100 ký tự!", exception.getMessage());
    }

    @Test
    void testDangKy_WeakPassword_Short() {
        String email = "valid@gmail.com";
        String matKhau = "weak"; // < 8 chars

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Mật khẩu phải dài từ 8 đến 25 ký tự!", exception.getMessage());
    }

    @Test
    void testDangKy_Password_Long() {
        String email = "valid@gmail.com";
        String matKhau = "A1" + "a".repeat(24); // 26 chars, over the 25-character limit

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Mật khẩu phải dài từ 8 đến 25 ký tự!", exception.getMessage());
    }

    @Test
    void testDangKy_Password_WithSpaces() {
        String email = "valid@gmail.com";
        String matKhau = "Secure Pass123";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Mật khẩu không được chứa khoảng trắng!", exception.getMessage());
    }

    @Test
    void testDangKy_Password_WithoutNumber() {
        String email = "valid@gmail.com";
        String matKhau = "SecureLettersOnly";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Mật khẩu phải có ít nhất 1 chữ số!", exception.getMessage());
    }

    @Test
    void testDangKy_Password_WithoutUppercase() {
        String email = "valid@gmail.com";
        String matKhau = "securepass123";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userDangKyService.dangKy(email, matKhau);
        });

        assertEquals("Mật khẩu phải có ít nhất 1 chữ in hoa!", exception.getMessage());
    }

    @Test
    void testDangKy_Password_Exactly25Characters() {
        String email = "valid@gmail.com";
        String matKhau = "A1" + "a".repeat(23);

        when(taiKhoanRepository.existsByUsername(email)).thenReturn(false);
        when(taiKhoanRepository.save(any(TaiKhoan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaiKhoan result = userDangKyService.dangKy(email, matKhau);

        assertNotNull(result);
        assertEquals(matKhau, result.getMatKhau());
    }
}
