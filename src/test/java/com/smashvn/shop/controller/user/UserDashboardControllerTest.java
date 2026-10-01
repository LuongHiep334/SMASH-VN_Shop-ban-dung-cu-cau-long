package com.smashvn.shop.controller.user;

import com.smashvn.shop.dto.user.UserProfileEditDto;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.order.OrderViewService;
import com.smashvn.shop.service.user.UserDashboardService;
import com.smashvn.shop.repository.NewsletterSubscriberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserDashboardControllerTest {

    @Mock
    private UserDashboardService dashboardService;

    @Mock
    private OrderViewService orderViewService;

    @Mock
    private SanPhamYeuThichRepository wishlistRepository;

    @Mock
    private ThongBaoRepository thongBaoRepository;

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private NewsletterSubscriberRepository newsletterSubscriberRepository;

    @Mock
    private com.smashvn.shop.service.common.FileStorageService fileStorageService;

    @Mock
    private HttpSession session;

    @Mock
    private BindingResult bindingResult;

    private UserDashboardController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new UserDashboardController(dashboardService, orderViewService, wishlistRepository, thongBaoRepository, hoaDonRepository, newsletterSubscriberRepository, fileStorageService);
    }

    @Test
    void testHienThiMyOrders_StaleGuestTabUsesOrderSummaryModel() {
        var access = new com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess(
                100,
                "guest@example.com",
                Instant.now().plusSeconds(1800));
        when(session.getAttribute("isGuestView")).thenReturn(true);
        when(session.getAttribute("allowedGuestOrderAccesses")).thenReturn(List.of(access));

        Map<String, Object> orderSummary = new java.util.HashMap<>();
        orderSummary.put("id", 100);
        orderSummary.put("maDonHang", "DHSVN-100");
        orderSummary.put("status", "processing");
        var order = new com.smashvn.shop.entity.HoaDon();
        order.setId(100);
        order.setEmailNguoiNhan("guest@example.com");
        when(hoaDonRepository.findById(100)).thenReturn(java.util.Optional.of(order));
        when(orderViewService.layChiTietDonHangChoCustomer(100, null)).thenReturn(orderSummary);

        Model model = new ConcurrentModel();
        String view = controller.hienThiMyOrders(session, model);

        assertEquals("dash-my-order", view);
        assertEquals(List.of(orderSummary), model.getAttribute("orders"));
        assertEquals(1, model.getAttribute("orderPlaced"));
        assertEquals(Boolean.TRUE, model.getAttribute("isGuestView"));
        verify(orderViewService).layChiTietDonHangChoCustomer(100, null);
        verify(orderViewService, never()).layChiTietOrder(any(), any());
    }

    @Test
    void testXuLySuaHoSo_Success() {
        UserProfileEditDto dto = new UserProfileEditDto("Nguyen", "Van A", "0912345678");
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername("test@gmail.com");
        kh.setTaiKhoan(tk);
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        when(bindingResult.hasErrors()).thenReturn(false);

        Model model = new ConcurrentModel();
        String view = controller.xuLySuaHoSo(session, dto, bindingResult, model);

        assertEquals("redirect:/user/profile?capNhatThanhCong", view);
        verify(dashboardService).capNhatHoSo(1, dto);
    }

    @Test
    void testXuLySuaHoSo_BindingErrors() {
        UserProfileEditDto dto = new UserProfileEditDto("", "", "invalid-phone");
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(bindingResult.hasErrors()).thenReturn(true);
        
        ObjectError error = new FieldError("profileDto", "ho", "Họ không được để trống!");
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(error));

        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername("test@gmail.com");
        kh.setTaiKhoan(tk);
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        Model model = new ConcurrentModel();
        String view = controller.xuLySuaHoSo(session, dto, bindingResult, model);

        assertEquals("dash-edit-profile", view);
        assertEquals("Họ không được để trống!", model.getAttribute("loi"));
        verify(dashboardService, never()).capNhatHoSo(any(), any());
    }

    @Test
    void testXuLySuaHoSo_ServiceException() {
        UserProfileEditDto dto = new UserProfileEditDto("Nguyen", "Van A", "0912345678");
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername("test@gmail.com");
        kh.setTaiKhoan(tk);
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalArgumentException("Số điện thoại này đã được đăng ký bởi tài khoản khác!"))
                .when(dashboardService).capNhatHoSo(1, dto);

        Model model = new ConcurrentModel();
        String view = controller.xuLySuaHoSo(session, dto, bindingResult, model);

        assertEquals("dash-edit-profile", view);
        assertEquals("Số điện thoại này đã được đăng ký bởi tài khoản khác!", model.getAttribute("loi"));
    }

    @Test
    void testViewInvoice_Success() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        
        KhachHang kh = new KhachHang();
        kh.setId(10);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        kh.setTaiKhoan(tk);
        
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(100);
        hd.setKhachHang(kh);
        
        when(hoaDonRepository.findById(100)).thenReturn(java.util.Optional.of(hd));
        
        java.util.Map<String, Object> mockDetails = new java.util.HashMap<>();
        mockDetails.put("order", hd);
        when(orderViewService.layChiTietOrder(100, 10)).thenReturn(mockDetails);

        Model model = new ConcurrentModel();
        String view = controller.viewInvoice(100, session, model);

        assertEquals("invoice-print", view);
        assertNotNull(model.getAttribute("order"));
    }

    @Test
    void testViewInvoice_AccessDenied() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        
        KhachHang kh = new KhachHang();
        kh.setId(10);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        kh.setTaiKhoan(tk);
        
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        KhachHang otherKh = new KhachHang();
        otherKh.setId(99);

        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(100);
        hd.setKhachHang(otherKh);
        
        when(hoaDonRepository.findById(100)).thenReturn(java.util.Optional.of(hd));

        Model model = new ConcurrentModel();
        String view = controller.viewInvoice(100, session, model);

        assertEquals("redirect:/user/dang-nhap", view);
    }

    @Test
    void testViewInvoice_NotFound() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(hoaDonRepository.findById(100)).thenReturn(java.util.Optional.empty());

        Model model = new ConcurrentModel();
        String view = controller.viewInvoice(100, session, model);

        assertEquals("redirect:/user/my-order?loi=donhangkhongton", view);
    }

    @Test
    @DisplayName("TC-T01: Anonymous Track Order renders public view without sidebar")
    void testHienThiTrackOrder_TC_T01_Anonymous() {
        when(session.getAttribute("vaiTro")).thenReturn(null);
        when(session.getAttribute("idNguoiDung")).thenReturn(null);
        when(session.getAttribute("isGuestView")).thenReturn(null);

        Model model = new ConcurrentModel();
        String view = controller.hienThiTrackOrder(null, session, model);

        assertEquals("dash-track-order", view);
        assertEquals(Boolean.TRUE, model.getAttribute("publicView"));
        assertEquals(Boolean.FALSE, model.getAttribute("hasSidebar"));
        assertNull(model.getAttribute("kh"));
        assertNull(model.getAttribute("orderPlaced"));
    }

    @Test
    @DisplayName("TC-T02: Guest Account Session renders layout with guest sidebar")
    void testHienThiTrackOrder_TC_T02_GuestAccountSession() {
        when(session.getAttribute("vaiTro")).thenReturn("KH");
        when(session.getAttribute("idNguoiDung")).thenReturn(99);
        when(session.getAttribute("isGuestView")).thenReturn(true);
        when(session.getAttribute("tenHienThi")).thenReturn("Khach Guest");

        KhachHang guestKh = new KhachHang();
        guestKh.setHoKh("Khach");
        guestKh.setTenKh("Guest");
        when(dashboardService.layThongTinKhachHang(99)).thenReturn(guestKh);

        Model model = new ConcurrentModel();
        String view = controller.hienThiTrackOrder(null, session, model);

        assertEquals("dash-track-order", view);
        assertEquals(Boolean.TRUE, model.getAttribute("hasSidebar"));
        assertEquals(Boolean.TRUE, model.getAttribute("guestAccountView"));
        assertEquals(Boolean.TRUE, model.getAttribute("isGuestView"));
        assertEquals("Khach Guest", model.getAttribute("tenHienThi"));
        assertNull(model.getAttribute("orderPlaced")); // No member stats
    }

    @Test
    @DisplayName("TC-T03: Existing Guest Order-only Session renders public standalone view without account sidebar")
    void testHienThiTrackOrder_TC_T03_ExistingGuestOrderOnly() {
        when(session.getAttribute("vaiTro")).thenReturn(null);
        when(session.getAttribute("idNguoiDung")).thenReturn(null);
        when(session.getAttribute("isGuestView")).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = controller.hienThiTrackOrder(null, session, model);

        assertEquals("dash-track-order", view);
        assertEquals(Boolean.TRUE, model.getAttribute("publicView"));
        assertEquals(Boolean.FALSE, model.getAttribute("hasSidebar"));
        assertNull(model.getAttribute("kh"));
    }

    @Test
    @DisplayName("TC-T04: ACTIVE Member Session renders member dashboard layout with full sidebar")
    void testHienThiTrackOrder_TC_T04_MemberSession() {
        when(session.getAttribute("vaiTro")).thenReturn("KH");
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(session.getAttribute("isGuestView")).thenReturn(false);

        KhachHang memberKh = new KhachHang();
        memberKh.setId(1);
        TaiKhoan tk = new TaiKhoan();
        tk.setTrangThaiTaiKhoan(com.smashvn.shop.entity.AccountStatus.ACTIVE);
        memberKh.setTaiKhoan(tk);
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(memberKh);
        when(orderViewService.layDanhSachOrders(1)).thenReturn(java.util.Collections.emptyList());
        when(wishlistRepository.countByKhachHang_Id(1)).thenReturn(0L);

        Model model = new ConcurrentModel();
        String view = controller.hienThiTrackOrder(null, session, model);

        assertEquals("dash-track-order", view);
        assertEquals(Boolean.TRUE, model.getAttribute("hasSidebar"));
        assertEquals(Boolean.TRUE, model.getAttribute("memberView"));
        assertEquals(Boolean.FALSE, model.getAttribute("isGuestView"));
        assertEquals(memberKh, model.getAttribute("kh"));
        assertEquals(0L, model.getAttribute("orderPlaced"));
    }

    @Test
    void testHienThiTrackOrder_WithParamId_PrefillsOnly() {
        when(session.getAttribute("vaiTro")).thenReturn(null);
        when(session.getAttribute("idNguoiDung")).thenReturn(null);
        when(session.getAttribute("isGuestView")).thenReturn(null);

        Model model = new ConcurrentModel();
        String view = controller.hienThiTrackOrder("DHSVN2026-TEST", session, model);

        assertEquals("dash-track-order", view);
        assertEquals("DHSVN2026-TEST", model.getAttribute("orderId"));
        assertNull(model.getAttribute("order"));
    }

    @Test
    void testSubmitTrackOrder_CorrectOrderId_WrongContactInfo_Rejected() {
        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(50);
        hd.setMaDonHang("DHSVN2026-TEST");
        hd.setEmailNguoiNhan("customer@gmail.com");
        hd.setSdtNhan("0912345678");

        when(hoaDonRepository.findByMaDonHang("DHSVN2026-TEST")).thenReturn(java.util.Optional.of(hd));

        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        String view = controller.submitTrackOrder("DHSVN2026-TEST", "wrong@gmail.com", session, redirectAttributes);

        assertEquals("redirect:/user/track-order", view);
        assertEquals("Thông tin email hoặc số điện thoại không khớp với đơn hàng.", redirectAttributes.getFlashAttributes().get("loi"));
        assertEquals("DHSVN2026-TEST", redirectAttributes.getFlashAttributes().get("orderId"));
    }

    @Test
    void testSubmitTrackOrder_CorrectOrderId_CorrectEmail_Success() {
        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(50);
        hd.setMaDonHang("DHSVN2026-TEST");
        hd.setEmailNguoiNhan("customer@gmail.com");
        hd.setSdtNhan("0912345678");

        when(hoaDonRepository.findByMaDonHang("DHSVN2026-TEST")).thenReturn(java.util.Optional.of(hd));

        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        String view = controller.submitTrackOrder("DHSVN2026-TEST", "customer@gmail.com", session, redirectAttributes);

        assertEquals("redirect:/user/manage-order/50", view);
        verify(session).setAttribute(eq("allowedGuestOrderAccesses"), any());
        verify(session).setAttribute("guestCheckoutEmail", "customer@gmail.com");
    }

    @Test
    void testSubmitTrackOrder_CorrectOrderId_CorrectPhone_Success() {
        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(50);
        hd.setMaDonHang("DHSVN2026-TEST");
        hd.setEmailNguoiNhan("customer@gmail.com");
        hd.setSdtNhan("0912345678");

        when(hoaDonRepository.findByMaDonHang("DHSVN2026-TEST")).thenReturn(java.util.Optional.of(hd));

        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        String view = controller.submitTrackOrder("DHSVN2026-TEST", "0912345678", session, redirectAttributes);

        assertEquals("redirect:/user/manage-order/50", view);
        verify(session).setAttribute(eq("allowedGuestOrderAccesses"), any());
    }

    @Test
    void testSubmitTrackOrder_MissingContactInfo_Rejected() {
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        String view = controller.submitTrackOrder("DHSVN2026-TEST", "", session, redirectAttributes);

        assertEquals("redirect:/user/track-order", view);
        assertEquals("Vui lòng nhập Email hoặc Số điện thoại đặt hàng.", redirectAttributes.getFlashAttributes().get("loi"));
    }

    @Test
    void testOptionB_GuestAccountSession_BlockedFromMemberProfile() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(session.getAttribute("isGuestView")).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = controller.hienThiHoSo(session, model);

        assertEquals("redirect:/user/dang-nhap", view);
    }

    @Test
    void testOptionB_GuestAccountSession_BlockedFromDashboard() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(session.getAttribute("isGuestView")).thenReturn(true);

        Model model = new ConcurrentModel();
        String view = controller.hienThiDashboard(session, model);

        assertEquals("redirect:/user/dang-nhap", view);
    }

    @Test
    void testOptionB_GuestAccountSession_ActivatedInDb_StillBlockedWithoutMemberLogin() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(session.getAttribute("isGuestView")).thenReturn(true);

        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setTrangThaiTaiKhoan(com.smashvn.shop.entity.AccountStatus.ACTIVE);
        tk.setTrangThai("hoat_dong");
        kh.setTaiKhoan(tk);
        when(dashboardService.layThongTinKhachHang(1)).thenReturn(kh);

        Model model = new ConcurrentModel();
        String view = controller.hienThiHoSo(session, model);

        assertEquals("redirect:/user/dang-nhap", view, "Guest session must remain blocked even if DB account became ACTIVE elsewhere");
    }

    @Test
    void testOptionB_GuestAccountSession_AllowedManageOrderWithOrderAccess() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(session.getAttribute("isGuestView")).thenReturn(true);

        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(50);
        hd.setMaDonHang("DHSVN2026-50");
        KhachHang kh = new KhachHang();
        kh.setId(10);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        tk.setUsername("guest@gmail.com");
        tk.setTrangThaiTaiKhoan(com.smashvn.shop.entity.AccountStatus.GUEST);
        kh.setTaiKhoan(tk);
        hd.setKhachHang(kh);

        when(hoaDonRepository.findByMaDonHang("DHSVN2026-50")).thenReturn(java.util.Optional.of(hd));
        when(hoaDonRepository.findById(50)).thenReturn(java.util.Optional.of(hd));

        java.util.List<com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess> accesses = new java.util.ArrayList<>();
        accesses.add(new com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess(50, "guest@gmail.com", java.time.Instant.now().plusSeconds(1800)));
        when(session.getAttribute("allowedGuestOrderAccesses")).thenReturn(accesses);

        java.util.Map<String, Object> mockDetails = new java.util.HashMap<>();
        mockDetails.put("order", hd);
        when(orderViewService.layChiTietOrder(50, 10)).thenReturn(mockDetails);

        Model model = new ConcurrentModel();
        String view = controller.hienThiManageOrder("DHSVN2026-50", null, session, model);

        assertEquals("dash-manage-order", view);
        assertEquals(Boolean.TRUE, model.getAttribute("isGuestView"));
    }

    @Test
    void testOptionB_GuestAccountSession_BlockedFromUnrelatedOrder() {
        when(session.getAttribute("idNguoiDung")).thenReturn(1);
        when(session.getAttribute("isGuestView")).thenReturn(true);

        com.smashvn.shop.entity.HoaDon hd = new com.smashvn.shop.entity.HoaDon();
        hd.setId(99);
        hd.setMaDonHang("DHSVN2026-99");
        KhachHang kh = new KhachHang();
        kh.setId(20);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(2);
        tk.setUsername("other@gmail.com");
        kh.setTaiKhoan(tk);
        hd.setKhachHang(kh);

        when(hoaDonRepository.findByMaDonHang("DHSVN2026-99")).thenReturn(java.util.Optional.of(hd));
        when(hoaDonRepository.findById(99)).thenReturn(java.util.Optional.of(hd));

        // Guest session only has access to order 50, not 99
        java.util.List<com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess> accesses = new java.util.ArrayList<>();
        accesses.add(new com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess(50, "guest@gmail.com", java.time.Instant.now().plusSeconds(1800)));
        when(session.getAttribute("allowedGuestOrderAccesses")).thenReturn(accesses);

        Model model = new ConcurrentModel();
        String view = controller.hienThiManageOrder("DHSVN2026-99", null, session, model);

        assertEquals("redirect:/user/dang-nhap", view, "Unrelated order access must be blocked for Guest session");
    }
}
