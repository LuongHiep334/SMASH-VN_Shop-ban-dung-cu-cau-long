package com.smashvn.shop.controller.user;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.exception.AccountLockedException;
import com.smashvn.shop.exception.AccountNotFoundException;
import com.smashvn.shop.exception.InvalidPasswordException;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.security.LoginRateLimiter;
import com.smashvn.shop.service.user.UserDangNhapService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserDangNhapControllerTest {

    @Mock
    private UserDangNhapService userDangNhapService;



    @Mock
    private KhachHangRepository khachHangRepository;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @Mock private jakarta.servlet.http.HttpServletResponse httpResponse;

    @org.junit.jupiter.api.AfterEach void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private UserDangNhapController userDangNhapController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDangNhapController = new UserDangNhapController(
                userDangNhapService,
                khachHangRepository,
                loginRateLimiter
        );

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession(anyBoolean())).thenReturn(session);
        when(request.isRequestedSessionIdValid()).thenReturn(true);
        when(request.getSession()).thenReturn(session);
        when(session.getId()).thenReturn("unit-session-before");
        when(request.changeSessionId()).thenReturn("unit-session-after");
    }

    @Test
    void testHienThiFormFormDangNhap() {
        Model model = new ConcurrentModel();
        String view = userDangNhapController.hienThiFormFormDangNhap("Lỗi test", null, model);
        assertEquals("signin", view);
        assertEquals("Lỗi test", model.getAttribute("loi"));
    }

    @Test
    void testHienThiFormFormDangNhap_InitialPageHasNoError() {
        Model model = new ConcurrentModel();

        String view = userDangNhapController.hienThiFormFormDangNhap(null, null, model);

        assertEquals("signin", view);
        assertNull(model.getAttribute("loi"));
        assertNull(model.getAttribute("usernameError"));
        assertNull(model.getAttribute("passwordError"));
    }

    @Test
    void testXuLyDangNhap_Success() {
        String email = "customer@gmail.com";
        String matKhau = "password";
        String accountKey = "customer@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername(email);
        tk.setVaiTro("KH");

        KhachHang kh = new KhachHang();
        kh.setHoKh("Nguyen");
        kh.setTenKh("Van A");

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);
        when(khachHangRepository.findByTaiKhoan_Id(1)).thenReturn(kh);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("redirect:/", view);
        verify(loginRateLimiter).loginSucceeded(accountKey);
        verify(request).changeSessionId();
        verify(session).setAttribute("nguoiDungDangNhap", email);
        verify(session).setAttribute("idNguoiDung", 1);
        verify(session).setAttribute("vaiTro", "KH");
        verify(session).setAttribute("tenHienThi", "Nguyen Van A");
    }

    @Test
    void guestTemporaryPasswordCreatesOnlyPendingSetupSession() {
        String email = "guest@gmail.com";
        TaiKhoan guest = new TaiKhoan();
        guest.setId(8);
        guest.setUsername(email);
        guest.setVaiTro("KH");
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);

        when(loginRateLimiter.isBlocked(email)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, "Temporary123")).thenReturn(guest);

        String view = userDangNhapController.xuLyDangNhap(
                email, "Temporary123", request, session, new ConcurrentModel(), httpResponse);

        assertEquals("redirect:/user/setup-password", view);
        verify(session).setAttribute("pendingPasswordSetupAccountId", 8);
        verify(session).setAttribute("temporaryPasswordVerified", true);
        verify(session).setAttribute("isGuestView", true);
        verify(session, never()).setAttribute("activeRole", "KH");
    }

    @Test
    void testXuLyDangNhap_BlockedAccount() {
        String email = "customer@gmail.com";
        String matKhau = "password";
        String accountKey = "customer@gmail.com";

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertTrue(model.getAttribute("loi").toString().contains("tạm thời bị khóa"));
        assertEquals(email, model.getAttribute("usernameNhap"));
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_BlockedAccountDoesNotBlockOtherAccountInSameBrowser() {
        String blockedEmail = "locked@gmail.com";
        String email = "customer@gmail.com";
        String matKhau = "password";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername(email);
        tk.setVaiTro("KH");

        when(loginRateLimiter.isBlocked(blockedEmail)).thenReturn(true);
        when(loginRateLimiter.isBlocked(email)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("redirect:/", view);
        verify(loginRateLimiter, never()).isBlocked("127.0.0.1");
        verify(userDangNhapService).kiemTraDangNhap(email, matKhau);
    }

    @Test
    void testXuLyDangNhap_WrongPasswordUsesPasswordFieldError() {
        String email = "wrong@gmail.com";
        String matKhau = "wrongpass";
        String accountKey = "wrong@gmail.com";

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau))
                .thenThrow(new InvalidPasswordException());

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Mật khẩu không chính xác.", model.getAttribute("passwordError"));
        assertEquals(email, model.getAttribute("usernameNhap"));
        assertNull(model.getAttribute("usernameError"));
        assertNull(model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }

    @Test
    void testXuLyDangNhap_AccountNotFoundUsesUsernameFieldErrorOnly() {
        String email = "notfound@gmail.com";
        String matKhau = "password";

        when(loginRateLimiter.isBlocked(email)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau))
                .thenThrow(new AccountNotFoundException(email));

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Email này chưa được đăng ký.", model.getAttribute("usernameError"));
        assertEquals(email, model.getAttribute("usernameNhap"));
        assertNull(model.getAttribute("passwordError"));
        assertNull(model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(email);
    }

    @Test
    void testXuLyDangNhap_LockedAccountUsesFormLevelError() {
        String email = "locked@gmail.com";
        String matKhau = "Password1";

        when(loginRateLimiter.isBlocked(email)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau))
                .thenThrow(new AccountLockedException());

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Tài khoản của bạn đã bị khóa.", model.getAttribute("loi"));
        assertNull(model.getAttribute("usernameError"));
        assertNull(model.getAttribute("passwordError"));
        verify(loginRateLimiter).loginFailed(email);
    }

    @Test
    void testXuLyDangNhap_UnexpectedFailureUsesSystemError() {
        String email = "customer@gmail.com";
        String matKhau = "Password1";

        when(loginRateLimiter.isBlocked(email)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau))
                .thenThrow(new RuntimeException("database unavailable"));

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Không thể đăng nhập lúc này. Vui lòng thử lại sau.", model.getAttribute("loi"));
        assertNull(model.getAttribute("usernameError"));
        assertNull(model.getAttribute("passwordError"));
        verify(loginRateLimiter).loginFailed(email);
    }

    @Test
    void testXuLyDangNhap_NotCustomerAccount() {
        String email = "admin@gmail.com";
        String matKhau = "adminpass";
        String accountKey = "admin@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(2);
        tk.setUsername(email);
        tk.setVaiTro("NV"); // Not a customer account

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Tài khoản này không được phép đăng nhập tại trang khách hàng.", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }

    @Test
    void testXuLyDangNhap_BlankEmail() {
        String email = "";
        String matKhau = "password";

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Vui lòng nhập email hoặc số điện thoại.", model.getAttribute("usernameError"));
        assertNull(model.getAttribute("loi"));
        verifyNoInteractions(loginRateLimiter);
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_BlankPassword() {
        String email = "customer@gmail.com";
        String matKhau = "";
        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Vui lòng nhập mật khẩu.", model.getAttribute("passwordError"));
        assertEquals(email, model.getAttribute("usernameNhap"));
        assertNull(model.getAttribute("loi"));
        verifyNoInteractions(loginRateLimiter);
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_InvalidEmailFormat() {
        String email = "invalid@email";
        String matKhau = "password";

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Định dạng email không hợp lệ.", model.getAttribute("usernameError"));
        assertEquals(email, model.getAttribute("usernameNhap"));
        assertNull(model.getAttribute("loi"));
        verifyNoInteractions(loginRateLimiter);
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_RejectsLegacyUsernameOnCustomerPage() {
        String username = "admin";

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(username, "Password1", request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Vui lòng nhập email hoặc số điện thoại hợp lệ.", model.getAttribute("usernameError"));
        assertNull(model.getAttribute("loi"));
        verifyNoInteractions(loginRateLimiter);
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_InvalidPhoneFormatUsesPhoneError() {
        String phone = "012345";

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(phone, "Password1", request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Số điện thoại Việt Nam không hợp lệ.", model.getAttribute("usernameError"));
        assertNull(model.getAttribute("loi"));
        verifyNoInteractions(loginRateLimiter);
        verifyNoInteractions(userDangNhapService);
    }

    @Test
    void testXuLyDangNhap_TrimsEmail() {
        String email = " customer@gmail.com ";
        String matKhau = "password";
        String accountKey = "customer@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername("customer@gmail.com");
        tk.setVaiTro("KH");

        KhachHang kh = new KhachHang();
        kh.setHoKh("Nguyen");
        kh.setTenKh("Van A");

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap("customer@gmail.com", matKhau)).thenReturn(tk);
        when(khachHangRepository.findByTaiKhoan_Id(1)).thenReturn(kh);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("redirect:/", view);
        verify(userDangNhapService).kiemTraDangNhap("customer@gmail.com", matKhau);
    }

    @Test
    void testXuLyDangNhap_NormalizesVietnamesePhone() {
        String phoneInput = "+84 912-345-678";
        String normalizedPhone = "0912345678";
        String matKhau = "password";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(6);
        tk.setUsername(normalizedPhone);
        tk.setVaiTro("KH");

        when(loginRateLimiter.isBlocked(normalizedPhone)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(normalizedPhone, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(phoneInput, matKhau, request, session, model, httpResponse);

        assertEquals("redirect:/", view);
        verify(userDangNhapService).kiemTraDangNhap(normalizedPhone, matKhau);
        verify(loginRateLimiter).loginSucceeded(normalizedPhone);
    }

    @Test
    void testXuLyDangXuat() {
        String view = userDangNhapController.xuLyDangXuat(session);
        assertEquals("redirect:/user/dang-nhap", view);
        verify(session).invalidate();
    }

    @Test
    void testXuLyDangNhap_NullRole() {
        String email = "nullrole@gmail.com";
        String matKhau = "password";
        String accountKey = "nullrole@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(3);
        tk.setUsername(email);
        tk.setVaiTro(null);

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Tài khoản này không được phép đăng nhập tại trang khách hàng.", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }

    @Test
    void testXuLyDangNhap_EmptyRole() {
        String email = "emptyrole@gmail.com";
        String matKhau = "password";
        String accountKey = "emptyrole@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(4);
        tk.setUsername(email);
        tk.setVaiTro("");

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Tài khoản này không được phép đăng nhập tại trang khách hàng.", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }

    @Test
    void testXuLyDangNhap_InvalidRole() {
        String email = "invalidrole@gmail.com";
        String matKhau = "password";
        String accountKey = "invalidrole@gmail.com";

        TaiKhoan tk = new TaiKhoan();
        tk.setId(5);
        tk.setUsername(email);
        tk.setVaiTro("UNKNOWN_ROLE");

        when(loginRateLimiter.isBlocked(accountKey)).thenReturn(false);
        when(userDangNhapService.kiemTraDangNhap(email, matKhau)).thenReturn(tk);

        Model model = new ConcurrentModel();
        String view = userDangNhapController.xuLyDangNhap(email, matKhau, request, session, model, httpResponse);

        assertEquals("signin", view);
        assertEquals("Tài khoản này không được phép đăng nhập tại trang khách hàng.", model.getAttribute("loi"));
        verify(loginRateLimiter).loginFailed(accountKey);
    }
}
