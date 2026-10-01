package com.smashvn.shop.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.ui.Model;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.OrderStatus;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalNotificationAdvice {

    private static final String CURRENT_CUSTOMER_ATTRIBUTE = GlobalNotificationAdvice.class.getName() + ".currentCustomer";
    private static final String NO_CURRENT_CUSTOMER_ATTRIBUTE = GlobalNotificationAdvice.class.getName() + ".noCurrentCustomer";
    private static final String ORDER_STATS_ATTRIBUTE = GlobalNotificationAdvice.class.getName() + ".orderStats";

    private final ThongBaoRepository thongBaoRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamYeuThichRepository wishlistRepository;
    private final HoaDonRepository hoaDonRepository;

    @ModelAttribute
    public void normalizeFlashNotification(Model model, HttpServletRequest request) {
        Map<String, ?> flashAttributes = RequestContextUtils.getInputFlashMap(request);
        NotificationMessage notification = firstNotification(flashAttributes, request);
        if (notification == null) {
            return;
        }

        model.addAttribute("globalNotificationType", notification.type());
        model.addAttribute("globalNotificationTitle", notification.title());
        model.addAttribute("globalNotificationMessage", notification.message());
    }

    @ModelAttribute("orderPlaced")
    public long getOrderPlacedCount(HttpSession session, HttpServletRequest request) {
        return getOrderStats(session, request).placed();
    }

    @ModelAttribute("cancelOrders")
    public long getCancelOrdersCount(HttpSession session, HttpServletRequest request) {
        return getOrderStats(session, request).cancelled();
    }

    @ModelAttribute("wishlist")
    public long getWishlistCount(HttpSession session, HttpServletRequest request) {
        try {
            KhachHang kh = getCurrentCustomer(session, request);
            if (kh == null) return 0;
            return wishlistRepository.countByKhachHang_Id(kh.getId());
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("unreadNotificationCount")
    public long getUnreadNotificationCount(HttpSession session) {
        if (session == null || Boolean.TRUE.equals(session.getAttribute("isGuestView"))) {
            return 0;
        }
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return 0;
        }
        try {
            return thongBaoRepository.countByTaiKhoan_IdAndDaDocFalse(idNguoiDung);
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("globalCategories")
    @Cacheable("globalCategories")
    public List<DanhMuc> getGlobalCategories() {
        try {
            return danhMucRepository.findByTrangThaiTrue();
        } catch (Exception e) {
            return List.of();
        }
    }

    @ModelAttribute("globalBrands")
    @Cacheable("globalBrands")
    public List<ThuongHieu> getGlobalBrands() {
        try {
            return thuongHieuRepository.findByTrangThaiTrue();
        } catch (Exception e) {
            return List.of();
        }
    }

    private OrderStats getOrderStats(HttpSession session, HttpServletRequest request) {
        Object cached = request.getAttribute(ORDER_STATS_ATTRIBUTE);
        if (cached instanceof OrderStats orderStats) {
            return orderStats;
        }

        OrderStats orderStats = new OrderStats(0, 0);
        try {
            KhachHang kh = getCurrentCustomer(session, request);
            if (kh != null) {
                List<HoaDon> orders = hoaDonRepository.findByKhachHang_Id(kh.getId());
                if (orders != null) {
                    long cancelled = orders.stream()
                            .filter(o -> OrderStatus.DA_HUY.getValue().equalsIgnoreCase(o.getTrangThaiDonHang()))
                            .count();
                    orderStats = new OrderStats(Math.max(0, orders.size() - cancelled), cancelled);
                }
            }
        } catch (Exception ignored) {
            // Header counters are optional and must never prevent the page from rendering.
        }

        request.setAttribute(ORDER_STATS_ATTRIBUTE, orderStats);
        return orderStats;
    }

    private KhachHang getCurrentCustomer(HttpSession session, HttpServletRequest request) {
        Object cached = request.getAttribute(CURRENT_CUSTOMER_ATTRIBUTE);
        if (cached instanceof KhachHang khachHang) {
            return khachHang;
        }
        if (Boolean.TRUE.equals(request.getAttribute(NO_CURRENT_CUSTOMER_ATTRIBUTE))) {
            return null;
        }

        // A guest account has an idNguoiDung so its order can be displayed after
        // checkout, but it is not an authenticated ACTIVE member account.
        if (session == null || Boolean.TRUE.equals(session.getAttribute("isGuestView"))) {
            request.setAttribute(NO_CURRENT_CUSTOMER_ATTRIBUTE, Boolean.TRUE);
            return null;
        }

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            request.setAttribute(NO_CURRENT_CUSTOMER_ATTRIBUTE, Boolean.TRUE);
            return null;
        }

        KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
        if (khachHang == null) {
            request.setAttribute(NO_CURRENT_CUSTOMER_ATTRIBUTE, Boolean.TRUE);
            return null;
        }

        request.setAttribute(CURRENT_CUSTOMER_ATTRIBUTE, khachHang);
        return khachHang;
    }

    private NotificationMessage firstNotification(Map<String, ?> flashAttributes, HttpServletRequest request) {
        String message = firstNonBlank(flashAttributes,
                "errorMsg", "error", "errorMessage", "thongBaoLoi", "loi");
        if (message != null) {
            return new NotificationMessage("error", "Lỗi", message);
        }

        message = firstNonBlank(flashAttributes, "warningMsg");
        if (message != null) {
            return new NotificationMessage("warning", "Cảnh báo", message);
        }

        message = firstNonBlank(flashAttributes,
                "successMsg", "success", "successMessage", "thongBaoThanhCong", "thongBaoGuiLai");
        if (message != null) {
            return new NotificationMessage("success", "Thành công", message);
        }

        message = trimToNull(request.getParameter("loi"));
        if (message != null) {
            return new NotificationMessage("error", "Lỗi", message);
        }

        return null;
    }

    private String firstNonBlank(Map<String, ?> attributes, String... keys) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            Object value = attributes.get(key);
            if (value != null) {
                String message = trimToNull(String.valueOf(value));
                if (message != null) {
                    return message;
                }
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record OrderStats(long placed, long cancelled) {
    }

    private record NotificationMessage(String type, String title, String message) {
    }
}
