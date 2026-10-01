package com.smashvn.shop.controller.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;

class GlobalNotificationAdviceTest {

    private KhachHangRepository khachHangRepository;
    private SanPhamYeuThichRepository wishlistRepository;
    private HoaDonRepository hoaDonRepository;
    private ThongBaoRepository thongBaoRepository;
    private GlobalNotificationAdvice advice;

    @BeforeEach
    void setUp() {
        thongBaoRepository = mock(ThongBaoRepository.class);
        DanhMucRepository danhMucRepository = mock(DanhMucRepository.class);
        ThuongHieuRepository thuongHieuRepository = mock(ThuongHieuRepository.class);
        khachHangRepository = mock(KhachHangRepository.class);
        wishlistRepository = mock(SanPhamYeuThichRepository.class);
        hoaDonRepository = mock(HoaDonRepository.class);

        advice = new GlobalNotificationAdvice(
                thongBaoRepository,
                danhMucRepository,
                thuongHieuRepository,
                khachHangRepository,
                wishlistRepository,
                hoaDonRepository);
    }

    @Test
    void reusesCustomerAndOrderStatsWithinTheSameRequest() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", 17);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        KhachHang customer = mock(KhachHang.class);
        when(customer.getId()).thenReturn(9);
        when(khachHangRepository.findByTaiKhoan_Id(17)).thenReturn(customer);

        HoaDon activeOrder = mock(HoaDon.class);
        when(activeOrder.getTrangThaiDonHang()).thenReturn(OrderStatus.CHO_XAC_NHAN.getValue());
        HoaDon cancelledOrder = mock(HoaDon.class);
        when(cancelledOrder.getTrangThaiDonHang()).thenReturn(OrderStatus.DA_HUY.getValue());
        when(hoaDonRepository.findByKhachHang_Id(9)).thenReturn(List.of(activeOrder, cancelledOrder));
        when(wishlistRepository.countByKhachHang_Id(9)).thenReturn(3L);

        assertEquals(1, advice.getOrderPlacedCount(session, request));
        assertEquals(1, advice.getCancelOrdersCount(session, request));
        assertEquals(3, advice.getWishlistCount(session, request));

        verify(khachHangRepository, times(1)).findByTaiKhoan_Id(17);
        verify(hoaDonRepository, times(1)).findByKhachHang_Id(9);
        verify(wishlistRepository, times(1)).countByKhachHang_Id(9);
    }

    @Test
    void anonymousRequestDoesNotQueryCustomerOrOrders() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        assertEquals(0, advice.getOrderPlacedCount(session, request));
        assertEquals(0, advice.getCancelOrdersCount(session, request));
        assertEquals(0, advice.getWishlistCount(session, request));

        verify(khachHangRepository, never()).findByTaiKhoan_Id(org.mockito.ArgumentMatchers.anyInt());
        verify(hoaDonRepository, never()).findByKhachHang_Id(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void guestAccountSessionDoesNotExposeActiveMemberCounters() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", 27);
        session.setAttribute("isGuestView", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);

        assertEquals(0, advice.getOrderPlacedCount(session, request));
        assertEquals(0, advice.getCancelOrdersCount(session, request));
        assertEquals(0, advice.getWishlistCount(session, request));
        assertEquals(0, advice.getUnreadNotificationCount(session));

        verify(khachHangRepository, never()).findByTaiKhoan_Id(org.mockito.ArgumentMatchers.anyInt());
        verify(hoaDonRepository, never()).findByKhachHang_Id(org.mockito.ArgumentMatchers.anyInt());
        verify(wishlistRepository, never()).countByKhachHang_Id(org.mockito.ArgumentMatchers.anyInt());
        verify(thongBaoRepository, never()).countByTaiKhoan_IdAndDaDocFalse(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void normalizesLegacyErrorFlashWithHighestPriority() {
        ConcurrentModel model = new ConcurrentModel();
        MockHttpServletRequest request = requestWithFlash(
                "successMsg", "Lưu thành công",
                "errorMessage", "Không thể lưu dữ liệu");

        advice.normalizeFlashNotification(model, request);

        assertEquals("error", model.getAttribute("globalNotificationType"));
        assertEquals("Lỗi", model.getAttribute("globalNotificationTitle"));
        assertEquals("Không thể lưu dữ liệu", model.getAttribute("globalNotificationMessage"));
    }

    @Test
    void normalizesLegacySuccessFlash() {
        ConcurrentModel model = new ConcurrentModel();
        MockHttpServletRequest request = requestWithFlash(
                "thongBaoThanhCong", "Cập nhật đơn hàng thành công");

        advice.normalizeFlashNotification(model, request);

        assertEquals("success", model.getAttribute("globalNotificationType"));
        assertEquals("Thành công", model.getAttribute("globalNotificationTitle"));
        assertEquals("Cập nhật đơn hàng thành công", model.getAttribute("globalNotificationMessage"));
    }

    @Test
    void normalizesErrorQueryParameterUsedByLegacyRedirects() {
        ConcurrentModel model = new ConcurrentModel();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("loi", "Phiên làm việc đã hết hạn");

        advice.normalizeFlashNotification(model, request);

        assertEquals("error", model.getAttribute("globalNotificationType"));
        assertEquals("Phiên làm việc đã hết hạn", model.getAttribute("globalNotificationMessage"));
    }

    @Test
    void doesNotTurnInlineFormValidationIntoADuplicateToast() {
        ConcurrentModel model = new ConcurrentModel();
        model.addAttribute("loi", "Email không hợp lệ");

        advice.normalizeFlashNotification(model, new MockHttpServletRequest());

        assertEquals(null, model.getAttribute("globalNotificationMessage"));
    }

    @Test
    void flashSuccessTakesPrecedenceOverQueryParamLoi() {
        ConcurrentModel model = new ConcurrentModel();
        MockHttpServletRequest request = requestWithFlash("thongBaoThanhCong", "Đã thêm địa chỉ mới thành công!");
        request.setParameter("loi", "Vui lòng chọn sản phẩm để thanh toán!");

        advice.normalizeFlashNotification(model, request);

        assertEquals("success", model.getAttribute("globalNotificationType"));
        assertEquals("Thành công", model.getAttribute("globalNotificationTitle"));
        assertEquals("Đã thêm địa chỉ mới thành công!", model.getAttribute("globalNotificationMessage"));
    }

    private MockHttpServletRequest requestWithFlash(String... keyValues) {
        FlashMap flashMap = new FlashMap();
        for (int index = 0; index < keyValues.length; index += 2) {
            flashMap.put(keyValues[index], keyValues[index + 1]);
        }
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE, flashMap);
        return request;
    }
}
