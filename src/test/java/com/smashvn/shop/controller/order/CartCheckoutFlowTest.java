package com.smashvn.shop.controller.order;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.smashvn.shop.entity.GioHangChiTiet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;

import com.smashvn.shop.dto.order.CheckoutContext;
import com.smashvn.shop.dto.order.CheckoutContextStatus;
import com.smashvn.shop.dto.order.CheckoutExecutionSnapshot;
import com.smashvn.shop.dto.order.CheckoutSource;
import com.smashvn.shop.dto.order.PendingCheckoutStatus;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.order.CheckoutContextService;
import com.smashvn.shop.service.order.PendingCheckoutRegistry;
import com.smashvn.shop.service.payment.SepayPaymentOrchestratorService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;

@SpringBootTest
@org.springframework.transaction.annotation.Transactional
public class CartCheckoutFlowTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private CheckoutContextService checkoutContextService;

    @Autowired
    private com.smashvn.shop.service.order.GioHangService gioHangService;

    @Autowired
    private PendingCheckoutRegistry pendingCheckoutRegistry;


    @Autowired
    private SepayPaymentOrchestratorService sepayPaymentOrchestratorService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private com.smashvn.shop.repository.KhachHangRepository khachHangRepository;

    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    private MockHttpSession session;

    @BeforeEach
    public void setUp() {
        session = new MockHttpSession();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }


    @Test
    public void testCheckoutContextAtomicTryClaimAndRelease() {
        CheckoutContext context = CheckoutContext.builder()
                .token("test-token-1")
                .status(CheckoutContextStatus.READY)
                .build();

        assertTrue(context.tryClaim(), "First claim should succeed");
        assertFalse(context.tryClaim(), "Second claim while processing should fail");

        context.release();
        assertEquals(CheckoutContextStatus.READY, context.getStatus(), "Release should revert status to READY");

        assertTrue(context.tryClaim(), "Claim after release should succeed");
        context.consume();
        assertEquals(CheckoutContextStatus.CONSUMED, context.getStatus());
        assertFalse(context.tryClaim(), "Claim after consume should fail");
    }

    @Test
    public void testPendingCheckoutRegistryAtomicClaimAndComplete() {
        CheckoutExecutionSnapshot snapshot = CheckoutExecutionSnapshot.builder()
                .maDonHang("DON-TEST-001")
                .source(CheckoutSource.CART)
                .status(PendingCheckoutStatus.READY)
                .customerId(1)
                .sessionId(session.getId())
                .items(new ArrayList<>())
                .build();

        pendingCheckoutRegistry.registerSnapshot(snapshot);

        CheckoutExecutionSnapshot claimed = pendingCheckoutRegistry.claimSnapshot("DON-TEST-001");
        assertNotNull(claimed, "Claiming READY snapshot should return object");
        assertEquals(PendingCheckoutStatus.PROCESSING, claimed.getStatus());

        CheckoutExecutionSnapshot secondClaim = pendingCheckoutRegistry.claimSnapshot("DON-TEST-001");
        assertNull(secondClaim, "Second claim while PROCESSING should return null");

        pendingCheckoutRegistry.completeAndRemove("DON-TEST-001");
        assertNull(pendingCheckoutRegistry.peekSnapshot("DON-TEST-001"), "Snapshot should be removed after complete");
    }

    @Test
    public void testSePaySimulatedGuestWrongSessionRejected() {
        CheckoutExecutionSnapshot snapshot = CheckoutExecutionSnapshot.builder()
                .maDonHang("DON-GUEST-001")
                .source(CheckoutSource.CART)
                .status(PendingCheckoutStatus.READY)
                .customerId(null) // Guest
                .sessionId("SESSION-ORIGINAL")
                .items(new ArrayList<>())
                .build();

        pendingCheckoutRegistry.registerSnapshot(snapshot);

        MockHttpSession wrongSession = new MockHttpSession();

        Exception exception = assertThrows(Exception.class, () -> {
            sepayPaymentOrchestratorService.orchestrateSimulatedPayment(
                    "DON-GUEST-001", new BigDecimal("100000"), null, "{}", wrongSession);
        });

        assertTrue(exception.getMessage().contains("khách vãng lai") || exception.getMessage().contains("mismatch"),
                "Wrong guest session must throw access denied exception");

        CheckoutExecutionSnapshot released = pendingCheckoutRegistry.peekSnapshot("DON-GUEST-001");
        assertNotNull(released);
        assertEquals(PendingCheckoutStatus.READY, released.getStatus(), "Snapshot must revert to READY on error");
    }

    @Test
    public void testMissingSnapshotPendingOrderRejected() {
        com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElse(null);
        com.smashvn.shop.entity.PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
        HoaDon hd = new HoaDon();
        hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
        hd.setPaymentStatus("pending");
        hd.setTongTien(new BigDecimal("200000"));
        hd.setDiaChiNhan("123 Street");
        hd.setSdtNhan("0912345678");
        hd.setTenNguoiNhan("Test User");
        hd.setKhachHang(kh);
        hd.setPhuongThucThanhToan(pttt);
        hd = hoaDonRepository.saveAndFlush(hd);
        hd.setMaDonHang("DON-" + hd.getId());
        hd = hoaDonRepository.saveAndFlush(hd);

        final String code = hd.getMaDonHang();

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            sepayPaymentOrchestratorService.orchestrateSimulatedPayment(
                    code, new BigDecimal("200000"), null, "{}", session);
        });

        assertTrue(exception.getMessage().contains("hết hạn hoặc bị mất"), "Missing snapshot on pending order must reject simulated payment");
    }

    @Test
    public void testMissingSnapshotPaidOrderReturnsIdempotent() throws Exception {
        com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElse(null);
        com.smashvn.shop.entity.PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
        HoaDon hd = new HoaDon();
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setPaymentStatus("paid");
        hd.setTongTien(new BigDecimal("300000"));
        hd.setDiaChiNhan("123 Street");
        hd.setSdtNhan("0912345678");
        hd.setTenNguoiNhan("Test User");
        hd.setKhachHang(kh);
        hd.setPhuongThucThanhToan(pttt);
        hd = hoaDonRepository.saveAndFlush(hd);
        hd.setMaDonHang("DON-" + hd.getId());
        hd = hoaDonRepository.saveAndFlush(hd);

        final String code = hd.getMaDonHang();

        var result = sepayPaymentOrchestratorService.orchestrateSimulatedPayment(
                code, new BigDecimal("300000"), null, "{}", session);

        assertNotNull(result);
        assertEquals(true, result.get("success"), "Already paid order must return idempotent success even if snapshot is missing");
    }

    @Test
    public void testQuickAddQuantityTamperProof() {
        CheckoutContext context = checkoutContextService.createQuickAddContext(session, 1, 10, 2, 5);

        assertNotNull(context.getToken());
        assertEquals(CheckoutSource.QUICK_ADD, context.getSource());
        assertEquals(1, context.getItems().size());
        assertEquals(2, context.getItems().get(0).getSoLuong(), "Quick add quantity must be locked to addedQuantity (2)");
    }

    @Test
    public void testCheckoutContextSurvivesSessionIdChange() {
        org.springframework.mock.web.MockHttpSession oldSession = new org.springframework.mock.web.MockHttpSession();
        CheckoutContext context = checkoutContextService.createBuyNowContext(oldSession, null, 1, 2);
        String token = context.getToken();

        org.springframework.mock.web.MockHttpSession newSession = new org.springframework.mock.web.MockHttpSession();
        CheckoutContext promoted = checkoutContextService.promoteGuestContextToAuthenticatedUser(token, oldSession, newSession, 99);

        assertNotNull(promoted);
        assertNotNull(checkoutContextService.getContext(newSession, token));
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;


    @Test
    public void testExistingAccountPasswordLoginKeepsCheckoutToken() throws Exception {
        String testEmail = "user_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@smash.vn";
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(testEmail);
        tk.setMatKhau(passwordEncoder.encode("Password123"));
        tk.setVaiTro("KH");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.saveAndFlush(tk);

        com.smashvn.shop.entity.SanPhamChiTiet validSpct = sanPhamChiTietRepository.findAll().stream()
                .filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() > 0)
                .findFirst().orElse(null);
        Integer spctId = (validSpct != null) ? validSpct.getId() : 1;

        CheckoutContext context = checkoutContextService.createBuyNowContext(session, null, spctId, 1);
        String token = context.getToken();

        org.springframework.test.web.servlet.MvcResult mvcResult = mockMvc.perform(post("/checkout/api/verify-password")
                .param("email", tk.getUsername())
                .param("password", "Password123")
                .param("checkoutToken", token)
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value("/checkout?token=" + token))
                .andReturn();

        org.springframework.mock.web.MockHttpSession activeSession = (org.springframework.mock.web.MockHttpSession) mvcResult.getRequest().getSession();

        mockMvc.perform(get("/checkout")
                .param("token", token)
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(activeSession != null ? activeSession : session))
                .andExpect(status().isOk());
    }

    @Test
    public void testWrongPasswordDoesNotConsumeContext() throws Exception {
        CheckoutContext context = checkoutContextService.createBuyNowContext(session, null, 1, 1);
        String token = context.getToken();

        mockMvc.perform(post("/checkout/api/verify-password")
                .param("email", "nonexistent_email_test@smash.vn")
                .param("password", "wrong_password_xyz")
                .param("checkoutToken", token)
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        CheckoutContext fetched = checkoutContextService.getContext(session, token);
        assertNotNull(fetched);
        assertEquals(CheckoutContextStatus.READY, fetched.getStatus());
    }

    @Test
    public void testContextPromotedToCorrectCustomer() {
        org.springframework.mock.web.MockHttpSession oldSession = new org.springframework.mock.web.MockHttpSession();
        CheckoutContext context = checkoutContextService.createCartContext(oldSession, null, new ArrayList<>());
        String token = context.getToken();

        org.springframework.mock.web.MockHttpSession newSession = new org.springframework.mock.web.MockHttpSession();
        CheckoutContext promoted = checkoutContextService.promoteGuestContextToAuthenticatedUser(token, oldSession, newSession, 555);

        assertNotNull(promoted);
        assertEquals(Integer.valueOf(555), promoted.getCustomerId());
        assertEquals(newSession.getId(), promoted.getSessionId());
    }

    @Test
    public void testContextCannotBePromotedFromAnotherSession() {
        org.springframework.mock.web.MockHttpSession sessionA = new org.springframework.mock.web.MockHttpSession();
        CheckoutContext context = checkoutContextService.createBuyNowContext(sessionA, null, 1, 1);
        String token = context.getToken();

        org.springframework.mock.web.MockHttpSession sessionB = new org.springframework.mock.web.MockHttpSession();
        org.springframework.mock.web.MockHttpSession sessionC = new org.springframework.mock.web.MockHttpSession();

        CheckoutContext result = checkoutContextService.promoteGuestContextToAuthenticatedUser(token, sessionB, sessionC, 100);
        assertNull(result, "Session B cannot promote context created in Session A");
    }

    @Test
    public void testSetPasswordAfterGuestOrderLimitKeepsCheckout() throws Exception {
        String testEmail = "guest_limit_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@smash.vn";
        com.smashvn.shop.entity.TaiKhoan guestTk = new com.smashvn.shop.entity.TaiKhoan();
        guestTk.setUsername(testEmail);
        guestTk.setMatKhau(null);
        guestTk.setVaiTro("KH");
        guestTk.setTrangThaiTaiKhoan(com.smashvn.shop.entity.AccountStatus.GUEST);
        guestTk = taiKhoanRepository.saveAndFlush(guestTk);

        session.setAttribute("guestCheckoutEmail", guestTk.getUsername());

        CheckoutContext context = checkoutContextService.createBuyNowContext(session, null, 1, 1);
        String token = context.getToken();

        org.springframework.test.web.servlet.MvcResult mvcResult = mockMvc.perform(post("/checkout/api/set-password")
                .param("email", guestTk.getUsername())
                .param("password", "NewPass123")
                .param("checkoutToken", token)
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andReturn();

        com.smashvn.shop.entity.TaiKhoan updated = taiKhoanRepository.findById(guestTk.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(com.smashvn.shop.entity.AccountStatus.GUEST, updated.getTrangThaiTaiKhoan());
        assertNull(updated.getMatKhau());

    }

    @Test
    public void testSavedAddressesLoadedAfterCheckoutLogin() throws Exception {
        String testEmail = "user_address_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@smash.vn";
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(testEmail);
        tk.setMatKhau(passwordEncoder.encode("Password123"));
        tk.setVaiTro("KH");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.saveAndFlush(tk);

        com.smashvn.shop.entity.KhachHang kh = new com.smashvn.shop.entity.KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoTenKh("Test User");
        kh.setSoDienThoaiKh("0987654321");
        kh = khachHangRepository.saveAndFlush(kh);

        com.smashvn.shop.entity.SanPhamChiTiet validSpct = sanPhamChiTietRepository.findAll().stream()
                .filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() > 0)
                .findFirst().orElse(null);
        Integer spctId = (validSpct != null) ? validSpct.getId() : 1;

        CheckoutContext context = checkoutContextService.createBuyNowContext(session, null, spctId, 1);
        String token = context.getToken();

        org.springframework.test.web.servlet.MvcResult mvcResult = mockMvc.perform(post("/checkout/api/verify-password")
                .param("email", tk.getUsername())
                .param("password", "Password123")
                .param("checkoutToken", token)
                .session(session))
                .andExpect(status().isOk())
                .andReturn();

        org.springframework.mock.web.MockHttpSession activeSession = (org.springframework.mock.web.MockHttpSession) mvcResult.getRequest().getSession();

        mockMvc.perform(get("/checkout")
                .param("token", token)
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(activeSession != null ? activeSession : session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("listDiaChi"));
    }

    @Test
    public void testCheckoutLoginDoesNotAddWholeCart() throws Exception {
        String testEmail = "user_nocart_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@smash.vn";
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(testEmail);
        tk.setMatKhau(passwordEncoder.encode("Password123"));
        tk.setVaiTro("KH");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.saveAndFlush(tk);

        CheckoutContext context = checkoutContextService.createBuyNowContext(session, null, 1, 3);
        String token = context.getToken();

        mockMvc.perform(post("/checkout/api/verify-password")
                .param("email", tk.getUsername())
                .param("password", "Password123")
                .param("checkoutToken", token)
                .session(session))
                .andExpect(status().isOk());

        CheckoutContext currentCtx = checkoutContextService.getContext(session, token);
        assertNotNull(currentCtx);
        assertEquals(1, currentCtx.getItems().size(), "Checkout items must remain exactly the 1 item from initial context");
        assertEquals(3, currentCtx.getItems().get(0).getSoLuong());
    }

    @Test
    public void testVerifyPasswordDoesNotClaimContext() throws Exception {
        String testEmail = "user_noclaim_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@smash.vn";
        CheckoutContext context = checkoutContextService.createBuyNowContext(session, null, 1, 1);
        String token = context.getToken();

        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(testEmail);
        tk.setMatKhau(passwordEncoder.encode("Password123"));
        tk.setVaiTro("KH");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.saveAndFlush(tk);

        mockMvc.perform(post("/checkout/api/verify-password")
                .param("email", tk.getUsername())
                .param("password", "Password123")
                .param("checkoutToken", token)
                .session(session))
                .andExpect(status().isOk());

        CheckoutContext currentCtx = checkoutContextService.getContext(session, token);
        assertNotNull(currentCtx);
        assertEquals(CheckoutContextStatus.READY, currentCtx.getStatus(), "verifyPassword must NOT claim or consume the context");
    }

    @Autowired
    private com.smashvn.shop.repository.GioHangChiTietRepository gioHangChiTietRepository;
    @Autowired
    private com.smashvn.shop.repository.GioHangRepository gioHangRepository;
    @Autowired
    private com.smashvn.shop.repository.SanPhamChiTietRepository sanPhamChiTietRepository;

    @Test
    public void testRenderLoggedInCartWithoutThymeleafException() throws Exception {
        String testEmail = "user_cartrender_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@smash.vn";
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(testEmail);
        tk.setMatKhau(passwordEncoder.encode("Password123"));
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.saveAndFlush(tk);

        session.setAttribute("idNguoiDung", tk.getId());

        mockMvc.perform(get("/gio-hang")
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("danhSachCart"))
                .andExpect(model().attributeExists("tongTien"));
    }

    @Test
    public void testRenderGuestCartWithoutThymeleafException() throws Exception {
        org.springframework.mock.web.MockHttpSession guestSession = new org.springframework.mock.web.MockHttpSession();
        mockMvc.perform(get("/gio-hang")
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(guestSession))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("danhSachCart"));
    }

    @Test
    public void testCartViewModelContainsCalculatedPrice() throws Exception {
        org.springframework.mock.web.MockHttpSession guestSession = new org.springframework.mock.web.MockHttpSession();
        var spct = sanPhamChiTietRepository.findAll().stream().filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() > 0).findFirst().orElse(null);
        assertNotNull(spct);

        mockMvc.perform(post("/gio-hang/them")
                .param("idSanPhamChiTiet", spct.getId().toString())
                .param("soLuong", "2")
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(guestSession))
                .andExpect(status().isOk());

        org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(get("/gio-hang")
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(guestSession))
                .andExpect(status().isOk())
                .andReturn();

        List<?> danhSachCart = (List<?>) res.getModelAndView().getModel().get("danhSachCart");
        assertNotNull(danhSachCart);
        assertFalse(danhSachCart.isEmpty());

        Object itemObj = danhSachCart.get(0);
        assertTrue(itemObj instanceof com.smashvn.shop.dto.cart.CartItemView);
        com.smashvn.shop.dto.cart.CartItemView viewItem = (com.smashvn.shop.dto.cart.CartItemView) itemObj;
        assertNotNull(viewItem.getDonGia());
        assertNotNull(viewItem.getThanhTien());
    }

    @Test
    public void testCheckoutStartUsesLatestDatabaseQuantity() throws Exception {
        String testEmail = "user_dbqty_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@smash.vn";
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(testEmail);
        tk.setMatKhau(passwordEncoder.encode("Password123"));
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.saveAndFlush(tk);

        var spct = sanPhamChiTietRepository.findAll().stream().filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() >= 5).findFirst().orElse(null);
        assertNotNull(spct);

        Map<String, Object> data = gioHangService.themVaoGio(tk.getId(), spct.getId(), 1);
        Integer cartItemId = (Integer) data.get("cartItemId");
        assertNotNull(cartItemId);

        session.setAttribute("idNguoiDung", tk.getId());

        // Update quantity in DB to 3
        gioHangService.capNhatSoLuong(cartItemId, 3, tk.getId());

        org.springframework.test.web.servlet.MvcResult mvcResult = mockMvc.perform(post("/checkout/start")
                .param("selectedItemIds", cartItemId.toString())
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();


        String token = (String) ((Map<?, ?>) new com.fasterxml.jackson.databind.ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), Map.class)).get("checkoutToken");
        assertNotNull(token);

        CheckoutContext context = checkoutContextService.getContext(session, token);
        assertNotNull(context);
        assertEquals(1, context.getItems().size());
        assertEquals(3, context.getItems().get(0).getSoLuong());
    }

    @Test
    public void testGuestCheckoutStartUsesLatestSessionQuantity() throws Exception {
        org.springframework.mock.web.MockHttpSession guestSession = new org.springframework.mock.web.MockHttpSession();
        var spct = sanPhamChiTietRepository.findAll().stream().filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() >= 5).findFirst().orElse(null);
        assertNotNull(spct);

        mockMvc.perform(post("/gio-hang/them")
                .param("idSanPhamChiTiet", spct.getId().toString())
                .param("soLuong", "1")
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(guestSession))
                .andExpect(status().isOk());

        // Update quantity to 3
        mockMvc.perform(post("/gio-hang/cap-nhat")
                .param("idChiTiet", spct.getId().toString())
                .param("soLuong", "3")
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(guestSession))
                .andExpect(status().isOk());

        org.springframework.test.web.servlet.MvcResult mvcResult = mockMvc.perform(post("/checkout/start")
                .param("selectedItemIds", spct.getId().toString())
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(guestSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String token = (String) ((Map<?, ?>) new com.fasterxml.jackson.databind.ObjectMapper().readValue(mvcResult.getResponse().getContentAsString(), Map.class)).get("checkoutToken");
        assertNotNull(token);

        CheckoutContext context = checkoutContextService.getContext(guestSession, token);
        assertNotNull(context);
        assertEquals(1, context.getItems().size());
        assertEquals(3, context.getItems().get(0).getSoLuong());
    }

    @Test
    public void testCheckoutStartRejectsForeignCartItem() throws Exception {
        String testEmail = "user_foreign_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@smash.vn";
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(testEmail);
        tk.setMatKhau(passwordEncoder.encode("Password123"));
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.saveAndFlush(tk);

        session.setAttribute("idNguoiDung", tk.getId());

        // Call checkout with invalid/foreign item ID 999999
        mockMvc.perform(post("/checkout/start")
                .param("selectedItemIds", "999999")
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"));
    }

    @Test
    public void testCheckoutStartRejectsDeletedItem() throws Exception {
        org.springframework.mock.web.MockHttpSession guestSession = new org.springframework.mock.web.MockHttpSession();
        var spct = sanPhamChiTietRepository.findAll().stream().filter(s -> s.getSoLuongTon() != null && s.getSoLuongTon() >= 1).findFirst().orElse(null);
        assertNotNull(spct);

        // Try start checkout without adding item to guest session cart
        mockMvc.perform(post("/checkout/start")
                .param("selectedItemIds", spct.getId().toString())
                .requestAttr("_csrf", new org.springframework.security.web.csrf.DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-123"))
                .session(guestSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"));
    }
}



