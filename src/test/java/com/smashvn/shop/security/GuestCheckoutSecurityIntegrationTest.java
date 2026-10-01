package com.smashvn.shop.security;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.service.order.GuestCheckoutService;
import com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess;

@SpringBootTest
@Transactional
public class GuestCheckoutSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private TokenKhoiPhucRepository tokenRepository;
    @Autowired
    private com.smashvn.shop.dao.DonViVanChuyenDAO donViVanChuyenDAO;
    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    @Autowired
    private GuestCheckoutService guestCheckoutService;
    @Autowired
    private com.smashvn.shop.service.user.UserQuenMatKhauService userQuenMatKhauService;
    @Autowired
    private com.smashvn.shop.service.user.UserDangNhapService userDangNhapService;
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private DonViVanChuyen testDvvc;
    private PhuongThucThanhToan testPttt;
    private CsrfToken csrfToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        List<DonViVanChuyen> vcs = donViVanChuyenDAO.findAll();
        if (vcs.isEmpty()) {
            DonViVanChuyen vc = new DonViVanChuyen();
            vc.setTenDonVi("Standard Shipping");
            vc.setHotline("1900");
            vc.setWebsite("https://smashvn.com");
            testDvvc = donViVanChuyenDAO.save(vc);
        } else {
            testDvvc = vcs.get(0);
        }

        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        if (ptts.isEmpty()) {
            PhuongThucThanhToan pt = new PhuongThucThanhToan();
            pt.setTenPhuongThuc("COD");
            testPttt = phuongThucThanhToanDAO.save(pt);
        } else {
            testPttt = ptts.get(0);
        }
    }

    @Test
    void testGuestActivationTokenCreatedSynchronously() {
        String email = "guest-token-" + System.nanoTime() + "@example.com";
        String phone = "09" + String.format("%08d", (int) (System.nanoTime() % 100000000));

        GuestCheckoutService.GuestRegisterResult result =
                guestCheckoutService.autoRegisterGuest("Guest Token", phone, email);

        assertNotNull(result.getTaiKhoan());
        assertNotNull(result.getToken());

        com.smashvn.shop.entity.TokenKhoiPhuc token = tokenRepository.findByMaXacNhan(result.getToken());
        assertNotNull(token);
        assertEquals(result.getTaiKhoan().getId(), token.getTaiKhoan().getId());
        assertEquals("GUEST_ACTIVATION", token.getLoaiXacNhan());
        assertFalse(token.isDaSuDung());
    }

    private TaiKhoan createGuestAccount(String email) {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(email);
        tk.setMatKhau(null);
        tk.setVaiTro("KH");
        // Set GUEST status AFTER setTrangThai to avoid the else→ACTIVE overwrite
        tk.setTrangThaiTaiKhoan(AccountStatus.GUEST);

        tk = taiKhoanRepository.save(tk);

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoKh("");
        kh.setTenKh("Guest User");
        String uniqueSdt = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        kh.setSoDienThoaiKh(uniqueSdt);
        kh.setLaTaiKhoanNoiBo(false);
        khachHangRepository.save(kh);

        return tk;
    }

    private HoaDon createGuestOrder(KhachHang kh) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setTrangThaiDonHang("cho_xac_nhan");
        hd.setTongTien(new BigDecimal("500000"));
        hd.setDiaChiNhan("123 Street");
        hd.setSdtNhan(kh.getSoDienThoaiKh());
        hd.setMaDonHang("GUEST-ORDER-" + System.nanoTime());
        hd.setDonViVanChuyen(testDvvc);
        hd.setPhuongThucThanhToan(testPttt);
        return hoaDonRepository.save(hd);
    }

    @Test
    void testGuestCheckoutSessionIsolation() throws Exception {
        // Assert that a guest checkout session does NOT have idNguoiDung (Anonymous) and cannot access /user/dashboard
        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("guestCheckoutEmail", "guest@example.com");

        mockMvc.perform(get("/user/dashboard").session(guestSession).requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void testGuestAccountSessionCannotAccessDashboard() throws Exception {
        String email = "guest-dashboard-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);

        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("idNguoiDung", tk.getId());
        guestSession.setAttribute("isGuestView", true);
        guestSession.setAttribute("guestCheckoutEmail", email);
        guestSession.setAttribute("vaiTro", "KH");

        mockMvc.perform(get("/user/dashboard").session(guestSession).requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void testGuestOrderAccess_ValidDuration() throws Exception {
        String email = "guest-valid-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        HoaDon hd = createGuestOrder(kh);

        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("guestCheckoutEmail", email);
        
        List<GuestOrderAccess> allowedAccesses = new ArrayList<>();
        allowedAccesses.add(new GuestOrderAccess(hd.getId(), Instant.now().plus(30, ChronoUnit.MINUTES)));
        guestSession.setAttribute("allowedGuestOrderAccesses", allowedAccesses);

        mockMvc.perform(get("/user/manage-order/" + hd.getId()).session(guestSession).requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andExpect(view().name("dash-manage-order"));
    }

    @Test
    void testGuestOrderAccess_Expired() throws Exception {
        String email = "guest-expired-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        HoaDon hd = createGuestOrder(kh);

        MockHttpSession guestSession = new MockHttpSession();
        guestSession.setAttribute("guestCheckoutEmail", email);
        
        List<GuestOrderAccess> allowedAccesses = new ArrayList<>();
        // Expired 1 second ago
        allowedAccesses.add(new GuestOrderAccess(hd.getId(), Instant.now().minus(1, ChronoUnit.SECONDS)));
        guestSession.setAttribute("allowedGuestOrderAccesses", allowedAccesses);

        mockMvc.perform(get("/user/manage-order/" + hd.getId()).session(guestSession).requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void testGuestOrderAccess_IDOR_CrossGuestBlocked() throws Exception {
        // Guest A
        String emailA = "guest-a-" + System.nanoTime() + "@example.com";
        TaiKhoan tkA = createGuestAccount(emailA);
        KhachHang khA = khachHangRepository.findByTaiKhoan_Id(tkA.getId());
        HoaDon hdA = createGuestOrder(khA);

        // Guest B
        String emailB = "guest-b-" + System.nanoTime() + "@example.com";
        TaiKhoan tkB = createGuestAccount(emailB);
        KhachHang khB = khachHangRepository.findByTaiKhoan_Id(tkB.getId());
        HoaDon hdB = createGuestOrder(khB);

        // Guest B's session trying to access Guest A's order hdA
        MockHttpSession guestSessionB = new MockHttpSession();
        guestSessionB.setAttribute("guestCheckoutEmail", emailB);
        
        List<GuestOrderAccess> allowedAccesses = new ArrayList<>();
        // Guest B tries to manipulate their session or query directly, but even if they inject hdA.getId():
        allowedAccesses.add(new GuestOrderAccess(hdA.getId(), Instant.now().plus(30, ChronoUnit.MINUTES)));
        guestSessionB.setAttribute("allowedGuestOrderAccesses", allowedAccesses);

        mockMvc.perform(get("/user/manage-order/" + hdA.getId()).session(guestSessionB).requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void test01_AnonymousCannotUpgradeGuestByEmail() throws Exception {
        String email = "guest-attack-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);

        MvcResult result = mockMvc.perform(post("/checkout/api/set-password")
                .param("email", email)
                .param("password", "strongpassword123")
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("\"success\":false"));

        TaiKhoan unchangedTk = taiKhoanRepository.findById(tk.getId()).orElseThrow();
        assertEquals(AccountStatus.GUEST, unchangedTk.getTrangThaiTaiKhoan());
        assertNull(unchangedTk.getMatKhau());
    }

    @Test
    void test02_GuestCheckoutCreatesGuestAccountWithActivationToken() {
        String email = "guest-new-" + System.nanoTime() + "@example.com";
        String sdt = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        GuestCheckoutService.GuestRegisterResult regResult = guestCheckoutService.autoRegisterGuest("Khach Test", sdt, email);

        assertNotNull(regResult);
        assertNotNull(regResult.getTaiKhoan());
        assertEquals(AccountStatus.GUEST, regResult.getTaiKhoan().getTrangThaiTaiKhoan());
        assertNull(regResult.getTaiKhoan().getMatKhau());
        assertNotNull(regResult.getToken(), "Activation token must be generated for guest account");

        com.smashvn.shop.entity.TokenKhoiPhuc tkp = tokenRepository.findByMaXacNhan(regResult.getToken());
        assertNotNull(tkp);
        assertEquals(regResult.getTaiKhoan().getId(), tkp.getTaiKhoan().getId());
        assertFalse(tkp.isDaSuDung());
    }

    @Test
    void test03_ValidTokenSetsPasswordAndActivatesAccount() throws Exception {
        String email = "guest-token-valid-" + System.nanoTime() + "@example.com";
        String sdt = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        GuestCheckoutService.GuestRegisterResult regResult = guestCheckoutService.autoRegisterGuest("Khach Hop Le", sdt, email);
        String token = regResult.getToken();

        // GET view with valid token
        mockMvc.perform(get("/user/thiet-lap-mat-khau").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("set-password-by-token"));

        // POST submit new password
        mockMvc.perform(post("/user/thiet-lap-mat-khau")
                .param("token", token)
                .param("password", "Password123")
                .param("confirmPassword", "Password123")
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap?thanhcong=Thi%E1%BA%BFt+l%E1%BA%ADp+m%E1%BA%ADt+kh%E1%BA%A9u+th%C3%A0nh+c%C3%B4ng%21+Vui+l%C3%B2ng+%C4%91%C4%83ng+nh%E1%BA%ADp."));

        TaiKhoan activatedTk = taiKhoanRepository.findById(regResult.getTaiKhoan().getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, activatedTk.getTrangThaiTaiKhoan());
        assertNotNull(activatedTk.getMatKhau());

        com.smashvn.shop.entity.TokenKhoiPhuc usedToken = tokenRepository.findByMaXacNhan(token);
        assertTrue(usedToken.isDaSuDung());
    }

    @Test
    void test04_InvalidTokenIsRejected() throws Exception {
        mockMvc.perform(get("/user/thiet-lap-mat-khau").param("token", "invalid-token-12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("signin"));

        assertThrows(RuntimeException.class, () -> {
            guestCheckoutService.setPasswordByToken("invalid-token-12345", "Password123");
        });
    }

    @Test
    void test05_ExpiredTokenIsRejected() {
        String email = "guest-expired-token-" + System.nanoTime() + "@example.com";
        String sdt = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        GuestCheckoutService.GuestRegisterResult regResult = guestCheckoutService.autoRegisterGuest("Khach Het Han", sdt, email);
        String token = regResult.getToken();

        com.smashvn.shop.entity.TokenKhoiPhuc tkp = tokenRepository.findByMaXacNhan(token);
        tkp.setThoiGianHetHan(java.time.LocalDateTime.now().minusDays(1)); // expired
        tokenRepository.saveAndFlush(tkp);

        assertThrows(RuntimeException.class, () -> {
            guestCheckoutService.setPasswordByToken(token, "Password123");
        });
    }

    @Test
    void test06_UsedTokenCannotBeUsedTwice() {
        String email = "guest-used-token-" + System.nanoTime() + "@example.com";
        String sdt = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        GuestCheckoutService.GuestRegisterResult regResult = guestCheckoutService.autoRegisterGuest("Khach Used", sdt, email);
        String token = regResult.getToken();

        // First use succeeds
        guestCheckoutService.setPasswordByToken(token, "Password123");

        // Second use fails
        assertThrows(RuntimeException.class, () -> {
            guestCheckoutService.setPasswordByToken(token, "AnotherPass123");
        });
    }

    @Test
    void test07_AttackerKnowingEmailCannotSetPasswordWithoutToken() throws Exception {
        String email = "victim-guest-" + System.nanoTime() + "@example.com";
        TaiKhoan victimTk = createGuestAccount(email);

        // Attacker calls set-password endpoint directly
        MvcResult result = mockMvc.perform(post("/checkout/api/set-password")
                .param("email", email)
                .param("password", "HackerPass123")
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains("\"success\":false"));

        // Victim account remains unchanged
        TaiKhoan unchanged = taiKhoanRepository.findById(victimTk.getId()).orElseThrow();
        assertEquals(AccountStatus.GUEST, unchanged.getTrangThaiTaiKhoan());
        assertNull(unchanged.getMatKhau());
    }

    @Test
    void test08_GuestRemainsEligibleAfterMultiplePurchases() {
        String email = "guest-multiple-buys-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);
        tk.setSoLanMuaThanhCong(3);
        taiKhoanRepository.saveAndFlush(tk);

        String status = guestCheckoutService.checkEmailStatus(email);
        assertEquals("GUEST_NO_PASSWORD", status);
    }

    @Test
    void test09_PasswordActivationRaceProtection() {
        String email = "guest-race-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);

        // First upgrade should succeed
        guestCheckoutService.setPasswordForGuest(tk.getId(), "mypassword123");

        // Second upgrade must fail with IllegalStateException (not GUEST status anymore)
        assertThrows(IllegalStateException.class, () -> {
            guestCheckoutService.setPasswordForGuest(tk.getId(), "anotherpassword123");
        });
    }

    @Test
    void test10_PurchaseCountNoLongerExpiresGuestCheckout() throws Exception {
        String email = "guest-4th-checkout-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);
        assertEquals(0, tk.getSoLanMuaThanhCong());

        // Simulate 3 successful payments
        guestCheckoutService.incrementPurchaseCount(tk.getId());
        guestCheckoutService.incrementPurchaseCount(tk.getId());
        guestCheckoutService.incrementPurchaseCount(tk.getId());

        TaiKhoan updatedTk = taiKhoanRepository.findById(tk.getId()).orElseThrow();
        assertEquals(3, updatedTk.getSoLanMuaThanhCong());

        // Service check
        String status = guestCheckoutService.checkEmailStatus(email);
        assertEquals("GUEST_NO_PASSWORD", status);

        // API check
        MvcResult result = mockMvc.perform(post("/checkout/api/check-email")
                .param("email", email)
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        assertTrue(responseContent.contains("GUEST_NO_PASSWORD"));
        assertFalse(responseContent.contains("GUEST_EXPIRED"));
    }

    @Test
    void test11_GuestWithInSessionIdNguoiDungIsNotRejectedByFourthOrderRule() throws Exception {
        String email = "guest-insession-" + System.nanoTime() + "@example.com";
        TaiKhoan tk = createGuestAccount(email);
        tk.setSoLanMuaThanhCong(3);
        taiKhoanRepository.save(tk);

        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("idNguoiDung", tk.getId());
        session.setAttribute("nguoiDungDangNhap", email);

        // Attempt COD checkout with guest session
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                .session(session)
                .param("hoTenNhan", "Khach InSession")
                .param("sdtNhan", "0987654321")
                .param("diaChiNhan", "123 Le Loi, Da Nang")
                .param("phuongThucThanhToan", "1") // COD
                .param("phiShip", "0")
                .requestAttr("_csrf", csrfToken))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertFalse(responseJson.contains("yeucaudoimatkhau"));
        assertFalse(responseJson.contains("quá 3 lần") || responseJson.contains("3 lần"));
    }

    @Test
    void test12_ResetPasswordViaGuestCheckoutServiceUpgradesGuestToActive() {
        String email = "guest-reset-active-" + System.nanoTime() + "@example.com";
        String sdt = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        GuestCheckoutService.GuestRegisterResult regResult = guestCheckoutService.autoRegisterGuest("Khach Reset", sdt, email);
        String token = regResult.getToken();

        TaiKhoan initialTk = taiKhoanRepository.findById(regResult.getTaiKhoan().getId()).orElseThrow();
        assertEquals(AccountStatus.GUEST, initialTk.getTrangThaiTaiKhoan());

        guestCheckoutService.setPasswordByToken(token, "NewPassword123");

        TaiKhoan upgradedTk = taiKhoanRepository.findById(regResult.getTaiKhoan().getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, upgradedTk.getTrangThaiTaiKhoan());
        assertNotNull(upgradedTk.getMatKhau());
    }

    @Test
    void test13_GuestWithTemporaryPasswordLoginRemainsGuest() {
        String email = "guest-login-active-" + System.nanoTime() + "@example.com";
        TaiKhoan guestTk = new TaiKhoan();
        guestTk.setUsername(email);
        guestTk.setMatKhau(passwordEncoder.encode("ValidPass123"));
        guestTk.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        guestTk.setVaiTro("KH");
        guestTk = taiKhoanRepository.saveAndFlush(guestTk);

        TaiKhoan loggedInTk = userDangNhapService.kiemTraDangNhap(email, "ValidPass123");
        assertNotNull(loggedInTk);
        assertEquals(AccountStatus.GUEST, loggedInTk.getTrangThaiTaiKhoan());

        TaiKhoan dbTk = taiKhoanRepository.findById(guestTk.getId()).orElseThrow();
        assertEquals(AccountStatus.GUEST, dbTk.getTrangThaiTaiKhoan());
    }

    @Test
    void test14_CheckEmailStatusRecognizesGuestWithTemporaryPasswordWithoutActivation() {
        String email = "guest-check-active-" + System.nanoTime() + "@example.com";
        TaiKhoan guestTk = new TaiKhoan();
        guestTk.setUsername(email);
        guestTk.setMatKhau(passwordEncoder.encode("ValidPass123"));
        guestTk.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        guestTk.setVaiTro("KH");
        guestTk = taiKhoanRepository.saveAndFlush(guestTk);

        String status = guestCheckoutService.checkEmailStatus(email);
        assertEquals("GUEST_WITH_TEMP_PASSWORD", status);

        TaiKhoan dbTk = taiKhoanRepository.findById(guestTk.getId()).orElseThrow();
        assertEquals(AccountStatus.GUEST, dbTk.getTrangThaiTaiKhoan());
    }

    @Test
    void test15_SetupPasswordRequiresVerifiedTemporaryPasswordSession() throws Exception {
        mockMvc.perform(get("/user/setup-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));
    }

    @Test
    void test16_VerifiedGuestSetsOfficialPasswordThenBecomesActive() throws Exception {
        String email = "guest-official-" + System.nanoTime() + "@example.com";
        TaiKhoan guest = createGuestAccount(email);
        guest.setMatKhau(passwordEncoder.encode("Temporary123"));
        taiKhoanRepository.saveAndFlush(guest);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", guest.getId());
        session.setAttribute("isGuestView", true);
        session.setAttribute("temporaryPasswordVerified", true);
        session.setAttribute("pendingPasswordSetupAccountId", guest.getId());

        mockMvc.perform(get("/user/setup-password").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("setup-password"));

        mockMvc.perform(post("/user/setup-password")
                        .session(session)
                        .param("newPassword", "Official456")
                        .param("confirmPassword", "Official456")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dashboard"));

        TaiKhoan active = taiKhoanRepository.findById(guest.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, active.getTrangThaiTaiKhoan());
        assertTrue(passwordEncoder.matches("Official456", active.getMatKhau()));
        assertFalse(passwordEncoder.matches("Temporary123", active.getMatKhau()));
        assertNull(session.getAttribute("isGuestView"));
        assertNull(session.getAttribute("temporaryPasswordVerified"));
        assertNull(session.getAttribute("pendingPasswordSetupAccountId"));
        assertEquals("KH", session.getAttribute("activeRole"));
    }
}
