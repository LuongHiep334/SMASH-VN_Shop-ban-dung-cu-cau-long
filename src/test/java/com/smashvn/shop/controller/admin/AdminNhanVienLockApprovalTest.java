package com.smashvn.shop.controller.admin;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.admin.AdminNhanVienService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@Transactional
public class AdminNhanVienLockApprovalTest {

    @Autowired
    private AdminNhanVienService adminNhanVienService;

    @Autowired
    private AdminNhanVienController adminNhanVienController;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private org.springframework.web.context.WebApplicationContext webApplicationContext;

    @Autowired
    private org.springframework.security.web.FilterChainProxy springSecurityFilterChain;

    private org.springframework.test.web.servlet.MockMvc secureMockMvc;

    @BeforeEach
    void setUpSecureMockMvc() {
        secureMockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    private NhanVien createSampleEmployee(String email, String phone) {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(email);
        tk.setMatKhau("12345678");
        tk.setVaiTro("NV");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk = taiKhoanRepository.save(tk);

        NhanVien nv = new NhanVien();
        nv.setHoTenNv("Nhân Viên Thử Nghiệm");
        nv.setChucVu("Nhân viên bán hàng");
        nv.setSoDienThoaiNv(phone);
        nv.setTaiKhoan(tk);
        return nhanVienRepository.save(nv);
    }

    private TaiKhoan createSampleManager(String email) {
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername(email);
        tk.setMatKhau("12345678");
        tk.setVaiTro("QL");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        return taiKhoanRepository.save(tk);
    }

    @Test
    public void testToggleStatusToPendingLock() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_toggle_" + ts + "@smash.vn", "0912" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_toggle_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        TaiKhoan updatedTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("cho_khoa", updatedTk.getTrangThai());
        assertEquals(AccountStatus.PENDING_LOCK, updatedTk.getTrangThaiTaiKhoan());
        assertNotNull(updatedTk.getTokenXacThucKhoa());
    }

    @Test
    public void testApproveLockViaAjax() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_ajax_" + ts + "@smash.vn", "0913" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_ajax_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", manager.getId());
        session.setAttribute("vaiTro", "QL");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object response = adminNhanVienController.xuLyPheDuyetKhoa(
                nv.getId(), null, true, false, session, request, model, redirectAttributes);

        assertNotNull(response);
        assertTrue(response instanceof org.springframework.http.ResponseEntity);
        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        assertEquals(200, entity.getStatusCode().value());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));

        TaiKhoan lockedTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("bi_khoa", lockedTk.getTrangThai());
        assertEquals(AccountStatus.LOCKED, lockedTk.getTrangThaiTaiKhoan());
        assertNull(lockedTk.getTokenXacThucKhoa());
    }

    @Test
    public void testTokenLinkOnlyShowsConfirmationBeforePost() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_token_" + ts + "@smash.vn", "0914" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_token_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        TaiKhoan pendingTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        String token = pendingTk.getTokenXacThucKhoa();
        assertNotNull(token);

        MockHttpSession unauthSession = new MockHttpSession(); // No logged in user
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String resultView = adminNhanVienController.hienThiXacNhanPheDuyetKhoa(
                nv.getId(), token, unauthSession, model);

        assertEquals("admin/lock-confirm", resultView);

        TaiKhoan stillPendingTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("cho_khoa", stillPendingTk.getTrangThai());
        assertEquals(token, stillPendingTk.getTokenXacThucKhoa());
    }

    @Test
    public void testRejectLockViaAjax() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_rej_" + ts + "@smash.vn", "0915" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_rej_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", manager.getId());
        session.setAttribute("vaiTro", "QL");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object response = adminNhanVienController.xuLyTuChoiKhoa(
                nv.getId(), null, true, false, session, request, model, redirectAttributes);

        assertNotNull(response);
        assertTrue(response instanceof org.springframework.http.ResponseEntity);
        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        assertEquals(200, entity.getStatusCode().value());

        TaiKhoan activeTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("hoat_dong", activeTk.getTrangThai());
        assertEquals(AccountStatus.ACTIVE, activeTk.getTrangThaiTaiKhoan());
        assertNull(activeTk.getTokenXacThucKhoa());
    }

    @Test
    public void testApproveLockReturnsOnlyToFixedEmployeeList() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_redir_" + ts + "@smash.vn", "0916" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_redir_" + ts + "@smash.vn");

        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", manager.getId());
        session.setAttribute("vaiTro", "QL");

        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ConcurrentModel();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object result = adminNhanVienController.xuLyPheDuyetKhoa(
                nv.getId(), null, false, true, session, request, model, redirectAttributes);

        assertEquals("redirect:/admin/nhan-vien", result);
        assertNotNull(redirectAttributes.getFlashAttributes().get("success"));

        TaiKhoan lockedTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        assertEquals("bi_khoa", lockedTk.getTrangThai());
    }

    @Test
    public void testExpiredTokenIsRejected() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_expired_" + ts + "@smash.vn", "0917" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_expired_" + ts + "@smash.vn");
        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        TaiKhoan pendingTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        String expiredToken = (java.time.Instant.now().minusSeconds(25 * 60 * 60).getEpochSecond())
                + "." + java.util.UUID.randomUUID();
        pendingTk.setTokenXacThucKhoa(expiredToken);
        taiKhoanRepository.saveAndFlush(pendingTk);

        MockHttpSession unauthSession = new MockHttpSession();
        Model model = new ConcurrentModel();
        String view = adminNhanVienController.hienThiXacNhanPheDuyetKhoa(
                nv.getId(), expiredToken, unauthSession, model);

        assertEquals("admin/confirm-result", view);
        assertEquals(false, model.getAttribute("success"));
        assertEquals("cho_khoa", pendingTk.getTrangThai());
    }

    @Test
    public void testInvalidAjaxTokenReturnsForbidden() {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_forbidden_" + ts + "@smash.vn", "0918" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_forbidden_" + ts + "@smash.vn");
        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        Object response = adminNhanVienController.xuLyPheDuyetKhoa(
                nv.getId(), "invalid-token", true, false,
                new MockHttpSession(), new MockHttpServletRequest(), new ConcurrentModel(),
                new RedirectAttributesModelMap());

        assertInstanceOf(org.springframework.http.ResponseEntity.class, response);
        org.springframework.http.ResponseEntity<?> entity = (org.springframework.http.ResponseEntity<?>) response;
        assertEquals(403, entity.getStatusCode().value());
        assertEquals("cho_khoa", taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow().getTrangThai());
    }

    @Test
    public void testGetOnlyShowsConfirmationAndPostRequiresCsrf() throws Exception {
        long ts = System.currentTimeMillis();
        NhanVien nv = createSampleEmployee("nv_http_" + ts + "@smash.vn", "0919" + (ts % 1000000));
        TaiKhoan manager = createSampleManager("ql_http_" + ts + "@smash.vn");
        adminNhanVienService.toggleStatus(nv.getId(), manager.getId(), "127.0.0.1", "http://localhost:8080");

        TaiKhoan pendingTk = taiKhoanRepository.findById(nv.getTaiKhoan().getId()).orElseThrow();
        String token = pendingTk.getTokenXacThucKhoa();

        org.springframework.test.web.servlet.MvcResult confirmationResult = secureMockMvc
                .perform(get("/admin/nhan-vien/approve-lock/{id}", nv.getId())
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/lock-confirm"))
                .andReturn();
        assertEquals("cho_khoa", taiKhoanRepository.findById(pendingTk.getId()).orElseThrow().getTrangThai());

        secureMockMvc.perform(post("/admin/nhan-vien/approve-lock/{id}", nv.getId())
                        .param("token", token)
                        .param("ajax", "true")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Accept", "application/json"))
                .andExpect(status().isForbidden());
        assertEquals("cho_khoa", taiKhoanRepository.findById(pendingTk.getId()).orElseThrow().getTrangThai());

        String confirmationHtml = confirmationResult.getResponse().getContentAsString();
        java.util.regex.Matcher csrfMatcher = java.util.regex.Pattern
                .compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                .matcher(confirmationHtml);
        assertTrue(csrfMatcher.find(), "Trang xác nhận phải chứa CSRF token");
        org.springframework.mock.web.MockHttpSession csrfSession =
                (org.springframework.mock.web.MockHttpSession) confirmationResult.getRequest().getSession(false);

        secureMockMvc.perform(post("/admin/nhan-vien/approve-lock/{id}", nv.getId())
                        .session(csrfSession)
                        .param("_csrf", csrfMatcher.group(1))
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/confirm-result"));
        assertEquals("bi_khoa", taiKhoanRepository.findById(pendingTk.getId()).orElseThrow().getTrangThai());
    }

    @Test
    public void testRedirectUrlParameterCanNoLongerRedirectExternally() throws Exception {
        secureMockMvc.perform(get("/admin/nhan-vien/approve-lock/{id}", 999999)
                        .param("redirectUrl", "https://example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/confirm-result"))
                .andExpect(header().doesNotExist("Location"));
    }
}
