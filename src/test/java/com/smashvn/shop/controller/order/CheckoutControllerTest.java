package com.smashvn.shop.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.user.UserAddressService;
import com.smashvn.shop.service.product.ProductAvailabilityService;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CheckoutControllerTest {
    @org.junit.jupiter.api.AfterEach void clearSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Mock
    private GioHangService gioHangService;

    @Mock
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Mock
    private UserAddressService userAddressService;

    @Mock
    private HttpSession session;

    @Mock
    private com.smashvn.shop.config.SepayConfig sepayConfig;

    @Mock
    private com.smashvn.shop.repository.KhachHangRepository khachHangRepository;

    @Mock
    private com.smashvn.shop.repository.PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Mock
    private com.smashvn.shop.service.product.PricingService pricingService;

    @Mock
    private com.smashvn.shop.service.order.GuestCartService guestCartService;

    @Mock
    private com.smashvn.shop.service.order.GuestCheckoutService guestCheckoutService;

    @Mock
    private com.smashvn.shop.service.user.UserDangNhapService userDangNhapService;

    @Mock
    private com.smashvn.shop.repository.SanPhamChiTietRepository sanPhamChiTietRepository;

    @Mock
    private com.smashvn.shop.repository.TaiKhoanRepository taiKhoanRepository;

    @Mock
    private com.smashvn.shop.repository.TokenKhoiPhucRepository tokenKhoiPhucRepository;

    @Mock
    private com.smashvn.shop.repository.SoDiaChiRepository soDiaChiRepository;

    @Mock
    private com.smashvn.shop.service.order.CheckoutContextService checkoutContextService;

    @Mock
    private com.smashvn.shop.service.order.PendingCheckoutRegistry pendingCheckoutRegistry;

    @Mock
    private com.smashvn.shop.repository.GioHangChiTietRepository gioHangChiTietRepository;

    @Mock
    private ProductAvailabilityService productAvailabilityService;

    @Mock
    private com.smashvn.shop.service.user.TemporaryPasswordService temporaryPasswordService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private CheckoutController checkoutController;

    private List<GioHangChiTiet> mockCartItems;
    private List<DonViVanChuyen> mockDvvcs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        checkoutController = new CheckoutController(
                gioHangService, 
                donViVanChuyenDAO, 
                userAddressService, 
                sepayConfig, 
                khachHangRepository, 
                phieuGiamGiaRepository, 
                pricingService,
                guestCartService,
                guestCheckoutService,
                userDangNhapService,
                sanPhamChiTietRepository,
                taiKhoanRepository,
                tokenKhoiPhucRepository,
                soDiaChiRepository,
                checkoutContextService,
                pendingCheckoutRegistry,
                gioHangChiTietRepository,
                productAvailabilityService,
                temporaryPasswordService
        );

        when(productAvailabilityService.isVariantPublished(any())).thenReturn(true);

        when(pricingService.calculateCurrentSellingPrice(any())).thenAnswer(invocation -> {
            SanPhamChiTiet arg = invocation.getArgument(0);
            return arg != null && arg.getGiaBan() != null ? arg.getGiaBan() : BigDecimal.ZERO;
        });

        com.smashvn.shop.entity.KhachHang kh = new com.smashvn.shop.entity.KhachHang();
        kh.setId(123);
        when(khachHangRepository.findByTaiKhoan_Id(123)).thenReturn(kh);

        when(session.getAttribute("idNguoiDung")).thenReturn(123);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(123);
        tk.setUsername("active@example.com");
        tk.setMatKhau("password123");
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        when(taiKhoanRepository.findById(123)).thenReturn(java.util.Optional.of(tk));

        mockCartItems = new ArrayList<>();
        GioHangChiTiet item = new GioHangChiTiet();
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(99);
        spct.setSoLuongTon(10);
        spct.setGiaBan(new BigDecimal("100000"));
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Yonex");
        sp.setTrangThai("dang_ban");
        spct.setSanPham(sp);
        item.setSanPhamChiTiet(spct);
        item.setSoLuong(2);
        mockCartItems.add(item);

        mockDvvcs = new ArrayList<>();
        DonViVanChuyen dvvc = new DonViVanChuyen();
        dvvc.setId(1);
        dvvc.setTenDonVi("Giao Hàng Nhanh");
        mockDvvcs.add(dvvc);

        when(gioHangService.layDanhSachSanPhamTrongGio(123)).thenReturn(mockCartItems);
        when(donViVanChuyenDAO.findAll()).thenReturn(mockDvvcs);
        when(sanPhamChiTietRepository.findById(99)).thenReturn(java.util.Optional.of(spct));

        com.smashvn.shop.dto.order.CheckoutItemContext itemCtx = com.smashvn.shop.dto.order.CheckoutItemContext.builder()
                .cartItemId(1)
                .idSanPhamChiTiet(99)
                .soLuong(2)
                .fromCart(true)
                .build();

        com.smashvn.shop.dto.order.CheckoutContext dummyCtx = com.smashvn.shop.dto.order.CheckoutContext.builder()
                .token("valid-token")
                .source(com.smashvn.shop.dto.order.CheckoutSource.CART)
                .status(com.smashvn.shop.dto.order.CheckoutContextStatus.READY)
                .customerId(123)
                .sessionId(session.getId())
                .items(java.util.List.of(itemCtx))
                .build();

        when(checkoutContextService.getContext(any(), eq("valid-token"))).thenReturn(dummyCtx);
        when(checkoutContextService.validateOwnership(any(), any(), any())).thenReturn(true);
    }


    @Test
    void testViewCheckout_NoSavedAddresses() {
        when(userAddressService.layDanhSachDiaChi(123)).thenReturn(new ArrayList<>());

        Model model = new ConcurrentModel();
        String view = checkoutController.viewCheckout("valid-token", session, model);


        assertEquals("checkout", view);
        assertTrue(model.containsAttribute("listDiaChi"));
        assertEquals(false, model.getAttribute("hasDefaultAddress"));
        assertEquals("{}", model.getAttribute("addressMapJson"));

        List<?> listDiaChi = (List<?>) model.getAttribute("listDiaChi");
        assertTrue(listDiaChi.isEmpty());
    }

    @Test
    void testViewCheckout_OneSavedAddress_NoDefault() throws Exception {
        List<SoDiaChi> addresses = new ArrayList<>();
        SoDiaChi dc = new SoDiaChi();
        dc.setId(10);
        dc.setHoNguoiNhan("Nguyen");
        dc.setTenNguoiNhan("An");
        dc.setSdtNguoiNhan("0987654321");
        dc.setDiaChiCuThe("123 Duong ABC");
        dc.setTinhThanh("Ha Noi");
        dc.setProvinceId(244);
        dc.setDistrictId(1639);
        dc.setWardCode("120125");
        dc.setQuocGia("Viet Nam");
        dc.setDefaultShipping(false);
        addresses.add(dc);

        when(userAddressService.layDanhSachDiaChi(123)).thenReturn(addresses);

        Model model = new ConcurrentModel();
        String view = checkoutController.viewCheckout("valid-token", session, model);


        assertEquals("checkout", view);
        assertEquals(true, model.getAttribute("hasDefaultAddress"));

        String jsonStr = (String) model.getAttribute("addressMapJson");
        Map<?, ?> addressMap = objectMapper.readValue(jsonStr, Map.class);
        assertEquals(1, addressMap.size());
        
        Map<?, ?> details = (Map<?, ?>) addressMap.get("10");
        assertNotNull(details);
        assertEquals("Nguyen An", details.get("hoTen"));
        assertEquals("0987654321", details.get("sdt"));
        assertEquals("123 Duong ABC, Ha Noi, Viet Nam", details.get("diaChi"));
        assertEquals(244, details.get("ghnProvinceId"));
        assertEquals(1639, details.get("ghnDistrictId"));
        assertEquals("120125", details.get("ghnWardCode"));
        assertEquals(true, details.get("ghnReady"));
    }

    @Test
    void testViewCheckout_MultipleSavedAddresses_WithDefault() throws Exception {
        List<SoDiaChi> addresses = new ArrayList<>();
        
        SoDiaChi dc1 = new SoDiaChi();
        dc1.setId(10);
        dc1.setHoNguoiNhan("Nguyen");
        dc1.setTenNguoiNhan("An");
        dc1.setSdtNguoiNhan("0987654321");
        dc1.setDiaChiCuThe("123 Duong ABC");
        dc1.setTinhThanh("Ha Noi");
        dc1.setQuocGia("Viet Nam");
        dc1.setDefaultShipping(true);
        addresses.add(dc1);

        SoDiaChi dc2 = new SoDiaChi();
        dc2.setId(11);
        dc2.setHoNguoiNhan("Tran");
        dc2.setTenNguoiNhan("Binh");
        dc2.setSdtNguoiNhan("0912345678");
        dc2.setDiaChiCuThe("456 Duong XYZ");
        dc2.setTinhThanh("HCM");
        dc2.setQuocGia("Viet Nam");
        dc2.setDefaultShipping(false);
        addresses.add(dc2);

        when(userAddressService.layDanhSachDiaChi(123)).thenReturn(addresses);

        Model model = new ConcurrentModel();
        String view = checkoutController.viewCheckout("valid-token", session, model);


        assertEquals("checkout", view);
        assertEquals(true, model.getAttribute("hasDefaultAddress"));

        String jsonStr = (String) model.getAttribute("addressMapJson");
        Map<?, ?> addressMap = objectMapper.readValue(jsonStr, Map.class);
        assertEquals(2, addressMap.size());

        Map<?, ?> details1 = (Map<?, ?>) addressMap.get("10");
        assertEquals("Nguyen An", details1.get("hoTen"));

        Map<?, ?> details2 = (Map<?, ?>) addressMap.get("11");
        assertEquals("Tran Binh", details2.get("hoTen"));
    }

    @Test
    void guestCheckoutPageDoesNotLoadSavedAddresses() {
        Integer guestAccountId = 456;
        when(session.getAttribute("idNguoiDung")).thenReturn(guestAccountId);

        TaiKhoan guest = new TaiKhoan();
        guest.setId(guestAccountId);
        guest.setUsername("guest@example.com");
        guest.setVaiTro("KH");
        guest.setTrangThai("hoat_dong");
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        when(taiKhoanRepository.findById(guestAccountId)).thenReturn(java.util.Optional.of(guest));

        com.smashvn.shop.service.order.GuestCartService.GuestCartItem guestItem =
                new com.smashvn.shop.service.order.GuestCartService.GuestCartItem(99, 1);
        when(guestCartService.getGuestCartItems(session)).thenReturn(java.util.List.of(guestItem));

        SanPham sp = new SanPham();
        sp.setTenSanPham("Guest Product");
        sp.setTrangThai("dang_ban");
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(99);
        spct.setSoLuongTon(5);
        spct.setGiaBan(new BigDecimal("120000"));
        spct.setSanPham(sp);
        when(sanPhamChiTietRepository.findById(99)).thenReturn(java.util.Optional.of(spct));

        Model model = new ConcurrentModel();
        String view = checkoutController.viewCheckout("valid-token", session, model);


        assertEquals("checkout", view);
        assertEquals(true, model.getAttribute("isGuest"));
        assertTrue(((List<?>) model.getAttribute("listDiaChi")).isEmpty());
        assertEquals("{}", model.getAttribute("addressMapJson"));
        verify(userAddressService, never()).layDanhSachDiaChi(anyInt());
        verify(gioHangService, never()).layDanhSachSanPhamTrongGio(guestAccountId);
        verify(gioHangService, never()).cleanPendingOrders(guestAccountId);
    }

    @Test
    void setupPasswordPageRejectsSessionWithoutTemporaryPasswordVerification() {
        when(session.getAttribute("temporaryPasswordVerified")).thenReturn(null);

        String view = checkoutController.viewSetupPassword(session, new ConcurrentModel());

        assertEquals("redirect:/user/dang-nhap", view);
    }

    @Test
    void verifiedGuestCanSubmitOfficialPasswordAndReceiveMemberSession() {
        TaiKhoan guest = new TaiKhoan();
        guest.setId(15);
        guest.setUsername("guest@example.com");
        guest.setVaiTro("KH");
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        guest.setMatKhau("$2a$tempHash");

        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(session.getAttribute("temporaryPasswordVerified")).thenReturn(true);
        when(session.getAttribute("pendingPasswordSetupAccountId")).thenReturn(15);
        when(request.getSession(anyBoolean())).thenReturn(session);
        when(request.isRequestedSessionIdValid()).thenReturn(true);
        when(request.getSession()).thenReturn(session);
        when(session.getId()).thenReturn("unit-session-before");
        when(request.changeSessionId()).thenReturn("unit-session-after");
        when(taiKhoanRepository.findById(15)).thenReturn(java.util.Optional.of(guest));
        doAnswer(invocation -> { guest.setTrangThaiTaiKhoan(AccountStatus.ACTIVE); return null; })
                .when(guestCheckoutService).setPasswordForGuest(15, "Official456");

        String view = checkoutController.submitSetupPassword(
                "Official456", "Official456", request, session, new ConcurrentModel(), mock(jakarta.servlet.http.HttpServletResponse.class));

        assertEquals("redirect:/user/dashboard", view);
        verify(guestCheckoutService).setPasswordForGuest(15, "Official456");
        verify(request).changeSessionId();
        verify(session).removeAttribute("isGuestView");
        verify(session).removeAttribute("temporaryPasswordVerified");
        verify(session).removeAttribute("pendingPasswordSetupAccountId");
        verify(session).setAttribute("activeRole", "KH");
    }

    @Test
    void codCheckoutRemainsSuccessfulWhenTemporaryPasswordEmailTriggerThrows() {
        when(session.getAttribute("idNguoiDung")).thenReturn(null);

        com.smashvn.shop.dto.order.CheckoutContext context =
                com.smashvn.shop.dto.order.CheckoutContext.builder()
                        .token("cod-email-failure-token")
                        .source(com.smashvn.shop.dto.order.CheckoutSource.CART)
                        .status(com.smashvn.shop.dto.order.CheckoutContextStatus.READY)
                        .build();
        when(checkoutContextService.getContext(session, "cod-email-failure-token")).thenReturn(context);

        TaiKhoan guest = new TaiKhoan();
        guest.setId(77);
        guest.setUsername("cod-buyer@realmail.vn");
        guest.setVaiTro("KH");
        guest.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        guest.setSoLanMuaThanhCong(1);
        com.smashvn.shop.entity.KhachHang customer = new com.smashvn.shop.entity.KhachHang();
        customer.setId(78);
        customer.setTaiKhoan(guest);

        when(guestCheckoutService.checkEmailStatus("cod-buyer@realmail.vn")).thenReturn("NEW");
        when(guestCheckoutService.autoRegisterGuest(
                "Guest Buyer", "0912345678", "cod-buyer@realmail.vn"))
                .thenReturn(new com.smashvn.shop.service.order.GuestCheckoutService.GuestRegisterResult(
                        guest, "activation-token", true));
        when(khachHangRepository.findByTaiKhoan_Id(77)).thenReturn(customer);

        DonViVanChuyen carrier = new DonViVanChuyen();
        carrier.setId(1);
        carrier.setMaDonVi("GHN");
        carrier.setTenDonVi("Giao Hàng Nhanh");
        when(donViVanChuyenDAO.findAll()).thenReturn(List.of(carrier));
        when(soDiaChiRepository.findByKhachHang_Id(78)).thenReturn(List.of());
        when(soDiaChiRepository.save(any(SoDiaChi.class))).thenAnswer(invocation -> {
            SoDiaChi address = invocation.getArgument(0);
            address.setId(91);
            return address;
        });

        com.smashvn.shop.entity.HoaDon order = new com.smashvn.shop.entity.HoaDon();
        order.setId(500);
        order.setMaDonHang("HD500");
        order.setPaymentMethod("COD");
        order.setTongTien(BigDecimal.valueOf(250000));
        order.setKhachHang(customer);
        com.smashvn.shop.dto.order.OrderCreationResult orderResult =
                com.smashvn.shop.dto.order.OrderCreationResult.builder()
                        .hoaDon(order)
                        .purchasedItems(List.of())
                        .build();
        when(gioHangService.submitCodOrder(
                eq(77), same(context), same(session),
                eq("Guest Buyer"), eq("0912345678"), eq("123 Street, District, City"),
                eq(1), isNull(), eq(1442), eq("20101"), eq(201), eq(91), isNull()))
                .thenReturn(orderResult);

        var issueResult = new com.smashvn.shop.service.user.TemporaryPasswordService.TemporaryPasswordIssueResult(
                com.smashvn.shop.service.user.TemporaryPasswordService.IssueStatus.ISSUED,
                77,
                "cod-buyer@realmail.vn",
                "A7kp2Qm9Xs4L",
                "activation-token");
        when(temporaryPasswordService.recordCodOrderCreated(77, true)).thenReturn(issueResult);
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(temporaryPasswordService).sendTemporaryPasswordEmail(any(), anyString());

        jakarta.servlet.http.HttpServletRequest request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getSession(anyBoolean())).thenReturn(session);
        when(request.isRequestedSessionIdValid()).thenReturn(true);
        when(request.getSession()).thenReturn(session);
        when(session.getId()).thenReturn("unit-session-before");
        when(request.changeSessionId()).thenReturn("unit-session-after");
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("smash.vn");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("");

        org.springframework.http.ResponseEntity<Map<String, Object>> response =
                checkoutController.submitCheckout(
                        "cod-email-failure-token",
                        "Guest Buyer",
                        "0912345678",
                        "123 Street, District, City",
                        null,
                        "COD",
                        null,
                        1442,
                        "20101",
                        201,
                        null,
                        null,
                        "cod-buyer@realmail.vn",
                        "City",
                        "District",
                        "Ward",
                        "123 Street",
                        false,
                        session,
                        request);

        assertEquals("ok", response.getBody().get("trangThai"));
        assertEquals(500, response.getBody().get("orderId"));
        assertEquals(com.smashvn.shop.dto.order.CheckoutContextStatus.CONSUMED, context.getStatus());
        verify(gioHangService).submitCodOrder(
                eq(77), same(context), same(session),
                eq("Guest Buyer"), eq("0912345678"), eq("123 Street, District, City"),
                eq(1), isNull(), eq(1442), eq("20101"), eq(201), eq(91), isNull());
        verify(temporaryPasswordService).sendTemporaryPasswordEmail(eq(issueResult), anyString());
    }

    @Test
    void checkoutTemplateInitializesShippingStateBeforeSavedAddressTrigger() throws Exception {
        String template = Files.readString(
                Path.of("src/main/resources/templates/checkout.html"),
                StandardCharsets.UTF_8);

        int readyHandler = template.indexOf("$(document).ready(function() {");
        int pendingRequestDeclaration = template.indexOf(
                "let pendingShippingRequest = null;", readyHandler);
        int shippingFeeDeclaration = template.indexOf(
                "let currentShippingFee = 0;", readyHandler);
        int voucherDeclaration = template.indexOf(
                "let voucherDiscount = 0;", readyHandler);
        int savedAddressTrigger = template.indexOf(
                "$('#selectDiaChiLuu').trigger('change');", readyHandler);

        assertTrue(readyHandler >= 0);
        assertTrue(pendingRequestDeclaration > readyHandler
                        && pendingRequestDeclaration < savedAddressTrigger,
                "pendingShippingRequest must exist before the initial saved-address change event");
        assertTrue(shippingFeeDeclaration > readyHandler
                        && shippingFeeDeclaration < savedAddressTrigger,
                "currentShippingFee must exist before calculateShippingFee can run");
        assertTrue(voucherDeclaration > readyHandler
                        && voucherDeclaration < savedAddressTrigger,
                "voucherDiscount must exist before updateUITotal can run");
    }
}
