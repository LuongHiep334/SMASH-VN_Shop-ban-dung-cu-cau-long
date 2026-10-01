package com.smashvn.shop.controller.order;

import com.smashvn.shop.dto.order.FullCartCheckoutResult;
import com.smashvn.shop.dto.order.InvalidCartItemView;
import com.smashvn.shop.dto.order.CheckoutContext;
import com.smashvn.shop.dto.order.CheckoutSource;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.order.CheckoutContextService;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.order.GuestCartService;
import com.smashvn.shop.service.product.ProductAvailabilityService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StartAllCheckoutTest {

    @Mock
    private GuestCartService guestCartService;

    @Mock
    private GioHangService gioHangService;

    @Mock
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private HttpSession session;

    @Mock
    private ProductAvailabilityService productAvailabilityService;

    private CheckoutContextService checkoutContextService;
    private CheckoutController checkoutController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(session.getId()).thenReturn("mock-session-123");

        checkoutContextService = new CheckoutContextService(
                guestCartService,
                gioHangService,
                sanPhamChiTietRepository,
                taiKhoanRepository,
                productAvailabilityService
        );

        checkoutController = new CheckoutController(
                gioHangService,
                null,
                null,
                null,
                null,
                null,
                null,
                guestCartService,
                null,
                null,
                sanPhamChiTietRepository,
                taiKhoanRepository,
                null,
                null,
                checkoutContextService,
                null,
                null,
                productAvailabilityService,
                null
        );

        when(productAvailabilityService.isVariantPublished(any())).thenReturn(true);
    }

    // 1. testStartAllOneLineQuantityFive
    @Test
    void testStartAllOneLineQuantityFive() {
        Integer userId = 10;
        when(session.getAttribute("idNguoiDung")).thenReturn(userId);

        TaiKhoan tk = new TaiKhoan();
        tk.setId(userId);
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk.setMatKhau("pass");
        tk.setTrangThai("hoat_dong");
        when(taiKhoanRepository.findById(userId)).thenReturn(Optional.of(tk));

        SanPham sp = new SanPham();
        sp.setId(1);
        sp.setTenSanPham("Yonex Nanoflare 700 Tour");
        sp.setTrangThai("dang_ban");

        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(101);
        spct.setSanPham(sp);
        spct.setTrangThai("dang_ban");
        spct.setSoLuongTon(20);

        GioHangChiTiet ghct = new GioHangChiTiet();
        ghct.setId(501);
        ghct.setSanPhamChiTiet(spct);
        ghct.setSoLuong(5);

        when(gioHangService.layDanhSachSanPhamTrongGio(userId)).thenReturn(List.of(ghct));

        ResponseEntity<FullCartCheckoutResult> response = checkoutController.startAllCheckout(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FullCartCheckoutResult body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals(1, body.getItemCount());
        assertEquals(5, body.getTotalQuantity());
    }

    // 2. testStartAllMultipleDistinctItems
    @Test
    void testStartAllMultipleDistinctItems() {
        Integer userId = 10;
        when(session.getAttribute("idNguoiDung")).thenReturn(userId);

        TaiKhoan tk = new TaiKhoan();
        tk.setId(userId);
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk.setMatKhau("pass");
        tk.setTrangThai("hoat_dong");
        when(taiKhoanRepository.findById(userId)).thenReturn(Optional.of(tk));

        SanPham spA = new SanPham(); spA.setId(1); spA.setTenSanPham("Vợt A"); spA.setTrangThai("dang_ban");
        SanPhamChiTiet spctA = new SanPhamChiTiet(); spctA.setId(101); spctA.setSanPham(spA); spctA.setTrangThai("dang_ban"); spctA.setSoLuongTon(10);
        GioHangChiTiet itemA = new GioHangChiTiet(); itemA.setId(501); itemA.setSanPhamChiTiet(spctA); itemA.setSoLuong(2);

        SanPham spB = new SanPham(); spB.setId(2); spB.setTenSanPham("Giày B"); spB.setTrangThai("dang_ban");
        SanPhamChiTiet spctB = new SanPhamChiTiet(); spctB.setId(102); spctB.setSanPham(spB); spctB.setTrangThai("dang_ban"); spctB.setSoLuongTon(10);
        GioHangChiTiet itemB = new GioHangChiTiet(); itemB.setId(502); itemB.setSanPhamChiTiet(spctB); itemB.setSoLuong(3);

        when(gioHangService.layDanhSachSanPhamTrongGio(userId)).thenReturn(List.of(itemA, itemB));

        ResponseEntity<FullCartCheckoutResult> response = checkoutController.startAllCheckout(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FullCartCheckoutResult body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals(2, body.getItemCount());
        assertEquals(5, body.getTotalQuantity());
    }

    // 3. testStartAllUsesAccountIdCorrectly
    @Test
    void testStartAllUsesAccountIdCorrectly() {
        Integer idTaiKhoan = 10;
        when(session.getAttribute("idNguoiDung")).thenReturn(idTaiKhoan);

        TaiKhoan tk = new TaiKhoan();
        tk.setId(idTaiKhoan);
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk.setMatKhau("pass");
        tk.setTrangThai("hoat_dong");
        when(taiKhoanRepository.findById(idTaiKhoan)).thenReturn(Optional.of(tk));

        SanPham sp = new SanPham(); sp.setId(1); sp.setTenSanPham("Sản phẩm 1"); sp.setTrangThai("dang_ban");
        SanPhamChiTiet spct = new SanPhamChiTiet(); spct.setId(101); spct.setSanPham(sp); spct.setTrangThai("dang_ban"); spct.setSoLuongTon(10);
        GioHangChiTiet item = new GioHangChiTiet(); item.setId(501); item.setSanPhamChiTiet(spct); item.setSoLuong(1);

        when(gioHangService.layDanhSachSanPhamTrongGio(idTaiKhoan)).thenReturn(List.of(item));

        ResponseEntity<FullCartCheckoutResult> response = checkoutController.startAllCheckout(session);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(gioHangService).layDanhSachSanPhamTrongGio(eq(idTaiKhoan));
    }

    // 4. testStartAllInvalidQuantityReturnsDetails
    @Test
    void testStartAllInvalidQuantityReturnsDetails() {
        Integer userId = 10;
        when(session.getAttribute("idNguoiDung")).thenReturn(userId);

        TaiKhoan tk = new TaiKhoan();
        tk.setId(userId);
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk.setMatKhau("pass");
        tk.setTrangThai("hoat_dong");
        when(taiKhoanRepository.findById(userId)).thenReturn(Optional.of(tk));

        SanPham sp = new SanPham(); sp.setId(1); sp.setTenSanPham("Yonex Nanoflare 700 Tour"); sp.setTrangThai("dang_ban");
        SanPhamChiTiet spct = new SanPhamChiTiet(); spct.setId(101); spct.setSanPham(sp); spct.setTrangThai("dang_ban"); spct.setSoLuongTon(4); // Ton kho = 4

        GioHangChiTiet item = new GioHangChiTiet(); item.setId(501); item.setSanPhamChiTiet(spct); item.setSoLuong(5); // Yeu cau = 5

        when(gioHangService.layDanhSachSanPhamTrongGio(userId)).thenReturn(List.of(item));

        ResponseEntity<FullCartCheckoutResult> response = checkoutController.startAllCheckout(session);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        FullCartCheckoutResult body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("error", body.getTrangThai());
        assertNotNull(body.getInvalidItems());
        assertEquals(1, body.getInvalidItems().size());

        InvalidCartItemView invalid = body.getInvalidItems().get(0);
        assertEquals("Yonex Nanoflare 700 Tour", invalid.getTenSanPham());
        assertEquals(5, invalid.getRequestedQuantity());
        assertEquals(4, invalid.getStockQuantity());
        assertTrue(invalid.getReason().contains("vượt tồn kho"));
    }

    // 5. testStartAllSuccessReturnsJson
    @Test
    void testStartAllSuccessReturnsJson() {
        when(session.getAttribute("idNguoiDung")).thenReturn(null);

        GuestCartService.GuestCartItem guestItem = new GuestCartService.GuestCartItem(101, 1);
        when(guestCartService.getGuestCartItems(session)).thenReturn(List.of(guestItem));

        SanPham sp = new SanPham(); sp.setId(1); sp.setTenSanPham("A"); sp.setTrangThai("dang_ban");
        SanPhamChiTiet spct = new SanPhamChiTiet(); spct.setId(101); spct.setSanPham(sp); spct.setTrangThai("dang_ban"); spct.setSoLuongTon(10);
        when(sanPhamChiTietRepository.findById(101)).thenReturn(Optional.of(spct));

        ResponseEntity<FullCartCheckoutResult> response = checkoutController.startAllCheckout(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FullCartCheckoutResult body = response.getBody();
        assertNotNull(body);
        assertEquals("ok", body.getTrangThai());
        assertNotNull(body.getCheckoutUrl());
        assertTrue(body.getCheckoutUrl().startsWith("/checkout?token="));
    }

    // 6. testStartAllDoesNotRedirectHtml
    @Test
    void testStartAllDoesNotRedirectHtml() {
        when(session.getAttribute("idNguoiDung")).thenReturn(null);
        when(guestCartService.getGuestCartItems(session)).thenReturn(Collections.emptyList());

        ResponseEntity<FullCartCheckoutResult> response = checkoutController.startAllCheckout(session);

        // Responsive JSON 400 error, NOT HTML redirect
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        FullCartCheckoutResult body = response.getBody();
        assertNotNull(body);
        assertEquals("error", body.getTrangThai());
    }

    // 7. testStartAllGuestMultipleItems
    @Test
    void testStartAllGuestMultipleItems() {
        when(session.getAttribute("idNguoiDung")).thenReturn(null);

        GuestCartService.GuestCartItem item1 = new GuestCartService.GuestCartItem(101, 2);
        GuestCartService.GuestCartItem item2 = new GuestCartService.GuestCartItem(102, 1);
        when(guestCartService.getGuestCartItems(session)).thenReturn(List.of(item1, item2));

        SanPham sp1 = new SanPham(); sp1.setId(1); sp1.setTenSanPham("A"); sp1.setTrangThai("dang_ban");
        SanPhamChiTiet spct1 = new SanPhamChiTiet(); spct1.setId(101); spct1.setSanPham(sp1); spct1.setTrangThai("dang_ban"); spct1.setSoLuongTon(10);

        SanPham sp2 = new SanPham(); sp2.setId(2); sp2.setTenSanPham("B"); sp2.setTrangThai("dang_ban");
        SanPhamChiTiet spct2 = new SanPhamChiTiet(); spct2.setId(102); spct2.setSanPham(sp2); spct2.setTrangThai("dang_ban"); spct2.setSoLuongTon(10);

        when(sanPhamChiTietRepository.findById(101)).thenReturn(Optional.of(spct1));
        when(sanPhamChiTietRepository.findById(102)).thenReturn(Optional.of(spct2));

        ResponseEntity<FullCartCheckoutResult> response = checkoutController.startAllCheckout(session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        FullCartCheckoutResult body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.getItemCount());
        assertEquals(3, body.getTotalQuantity());
    }
}
