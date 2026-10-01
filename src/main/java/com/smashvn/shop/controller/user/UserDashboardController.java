package com.smashvn.shop.controller.user;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.dto.user.UserProfileEditDto;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.service.order.OrderViewService;
import com.smashvn.shop.service.user.UserDashboardService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class UserDashboardController {

    @Value("${app.upload.path:uploads}")
    private String uploadPathConfig;

    private final UserDashboardService dashboardService;
    private final OrderViewService orderViewService;
    private final SanPhamYeuThichRepository wishlistRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final HoaDonRepository hoaDonRepository;
    private final com.smashvn.shop.repository.NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final com.smashvn.shop.service.common.FileStorageService fileStorageService;

    // Hàm dùng chung để kiểm tra đăng nhập và lấy KhachHang (chỉ chấp nhận ACTIVE Member đã xác thực)
    private KhachHang getLoggedInCustomer(HttpSession session) {
        if (session == null || Boolean.TRUE.equals(session.getAttribute("isGuestView"))) {
            return null;
        }
        Integer idTaiKhoan = (Integer) session.getAttribute("idNguoiDung");
        if (idTaiKhoan == null) {
            return null;
        }
        KhachHang kh = dashboardService.layThongTinKhachHang(idTaiKhoan);
        if (kh == null || kh.getTaiKhoan() == null) {
            return null;
        }
        TaiKhoan tk = kh.getTaiKhoan();
        if (tk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.ACTIVE
                || (tk.getTrangThai() != null && !"hoat_dong".equalsIgnoreCase(tk.getTrangThai()))) {
            return null;
        }
        return kh;
    }

    private boolean isGuestAllowedOrder(HttpSession session, Integer targetId, HoaDon hd) {
        if (session == null || targetId == null) {
            return false;
        }
        Object allowedAccessesAttr = session.getAttribute("allowedGuestOrderAccesses");
        java.util.List<?> allowedAccessesRaw = (allowedAccessesAttr instanceof java.util.List<?>)
                ? (java.util.List<?>) allowedAccessesAttr : null;

        if (allowedAccessesRaw == null) {
            return false;
        }

        com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess matchingAccess = null;
        synchronized (session) {
            java.util.Iterator<?> iterator = allowedAccessesRaw.iterator();
            while (iterator.hasNext()) {
                Object item = iterator.next();
                if (item instanceof com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess access) {
                    if (access.isExpired()) {
                        iterator.remove();
                    } else if (access.getOrderId().equals(targetId)) {
                        matchingAccess = access;
                    }
                }
            }
        }

        if (matchingAccess == null) {
            return false;
        }

        if (hd != null) {
            // Match the contact used when checkout/tracking issued this order-scoped grant.
            String orderEmail = (hd.getEmailNguoiNhan() != null && !hd.getEmailNguoiNhan().isBlank())
                    ? hd.getEmailNguoiNhan().trim()
                    : ((hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null)
                        ? hd.getKhachHang().getTaiKhoan().getUsername() : null);
            if (orderEmail != null) {
                if (matchingAccess.getGuestEmail() != null && !matchingAccess.getGuestEmail().isBlank()) {
                    if (!orderEmail.equalsIgnoreCase(matchingAccess.getGuestEmail().trim())) {
                        return false;
                    }
                } else {
                    String guestEmail = (String) session.getAttribute("guestCheckoutEmail");
                    if (guestEmail == null || !orderEmail.equalsIgnoreCase(guestEmail.trim())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String checkRoleAndRedirect(HttpSession session) {
        String vaiTro = (String) session.getAttribute("vaiTro");
        if ("QL".equals(vaiTro)) {
            return "redirect:/admin/all";
        }
        if ("NV".equals(vaiTro)) {
            return "redirect:/admin/don-hang";
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String hienThiDashboard(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        boolean nhanBanTin = false;
        if (kh.getTaiKhoan() != null && kh.getTaiKhoan().getUsername() != null) {
            String userEmail = kh.getTaiKhoan().getUsername().trim().toLowerCase();
            nhanBanTin = newsletterSubscriberRepository.findByEmail(userEmail)
                    .map(sub -> "hoat_dong".equalsIgnoreCase(sub.getTrangThai()))
                    .orElse(false);
        }
        model.addAttribute("nhanBanTin", nhanBanTin);

        // Lấy địa chỉ giao hàng và hóa đơn mặc định
        SoDiaChi defaultShipping = null;
        SoDiaChi defaultBilling = null;
        if (kh.getSoDiaChis() != null) {
            defaultShipping = kh.getSoDiaChis().stream()
                    .filter(SoDiaChi::isDefaultShipping)
                    .findFirst()
                    .orElse(null);
            defaultBilling = kh.getSoDiaChis().stream()
                    .filter(SoDiaChi::isDefaultBilling)
                    .findFirst()
                    .orElse(null);
        }
        model.addAttribute("defaultShipping", defaultShipping);
        model.addAttribute("defaultBilling", defaultBilling);

        // Đơn hàng gần đây (giới hạn 5 đơn)
        List<Map<String, Object>> recentOrders = ordersList.stream()
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("orders", recentOrders);

        return "dashboard"; // Trỏ đến dashboard.html
    }

    @GetMapping("/profile")
    public String hienThiHoSo(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-my-profile"; // Trỏ đến dash-my-profile.html
    }

    @GetMapping("/profile/edit")
    public String hienThiSuaHoSo(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);
        return "dash-edit-profile"; // Trỏ đến dash-edit-profile.html
    }

    @PostMapping("/profile/edit")
    public String xuLySuaHoSo(HttpSession session,
            @Valid @ModelAttribute("profileDto") UserProfileEditDto profileDto,
            BindingResult bindingResult,
            Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        Integer idTaiKhoan = kh.getTaiKhoan().getId();

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            kh.setHoKh(profileDto.getHo());
            kh.setTenKh(profileDto.getTen());
            kh.setSoDienThoaiKh(profileDto.getSdt());
            model.addAttribute("kh", kh);
            model.addAttribute("loi", errorMessage);
            return "dash-edit-profile";
        }

        try {
            dashboardService.capNhatHoSo(idTaiKhoan, profileDto);
            return "redirect:/user/profile?capNhatThanhCong";
        } catch (IllegalArgumentException e) {
            kh.setHoKh(profileDto.getHo());
            kh.setTenKh(profileDto.getTen());
            kh.setSoDienThoaiKh(profileDto.getSdt());
            model.addAttribute("kh", kh);
            model.addAttribute("loi", e.getMessage());
            return "dash-edit-profile";
        }
    }

    @GetMapping("/my-order")
    public String hienThiMyOrders(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            Object allowedAccessesAttr = session.getAttribute("allowedGuestOrderAccesses");
            if (allowedAccessesAttr instanceof java.util.List<?> list && !list.isEmpty()) {
                List<Map<String, Object>> guestOrdersList = new java.util.ArrayList<>();
                for (Object item : new java.util.ArrayList<>(list)) {
                    if (item instanceof com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess access && !access.isExpired()) {
                        HoaDon hd = hoaDonRepository.findById(access.getOrderId()).orElse(null);
                        if (hd == null || !isGuestAllowedOrder(session, access.getOrderId(), hd)) {
                            continue;
                        }
                        Map<String, Object> orderSummary = orderViewService.layChiTietDonHangChoCustomer(access.getOrderId(), null);
                        if (orderSummary != null) {
                            guestOrdersList.add(orderSummary);
                        }
                    }
                }
                if (!guestOrdersList.isEmpty()) {
                    model.addAttribute("orders", guestOrdersList);
                    model.addAttribute("orderPlaced", guestOrdersList.size());
                    model.addAttribute("cancelOrders", 0);
                    model.addAttribute("wishlist", 0);
                    model.addAttribute("isGuestView", true);
                    return "dash-my-order";
                }
            }
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        model.addAttribute("orders", ordersList);

        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);
        return "dash-my-order"; // Trỏ đến dash-my-order.html
    }

    @GetMapping("/cancellation")
    public String hienThiCancellation(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-cancellation";
    }

    @GetMapping("/payment-option")
    public String hienThiPaymentOption(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-payment-option";
    }

    @GetMapping("/track-order")
    public String hienThiTrackOrder(
            @RequestParam(value = "id", required = false) String paramId,
            HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        if (paramId != null && !paramId.trim().isEmpty()) {
            model.addAttribute("orderId", paramId.trim());
        }

        // 1. Kiểm tra phiên đăng nhập Member chính thức (AccountStatus.ACTIVE, isGuestView != true)
        KhachHang memberKh = getLoggedInCustomer(session);
        if (memberKh != null) {
            model.addAttribute("kh", memberKh);
            model.addAttribute("memberView", true);
            model.addAttribute("isGuestView", false);
            model.addAttribute("hasSidebar", true);

            List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(memberKh.getId());
            long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
            long wishlistCount = wishlistRepository.countByKhachHang_Id(memberKh.getId());

            model.addAttribute("orderPlaced", ordersList.size() - cancelled);
            model.addAttribute("cancelOrders", cancelled);
            model.addAttribute("wishlist", wishlistCount);
            return "dash-track-order";
        }

        // 2. Kiểm tra phiên Guest Account Session (idNguoiDung != null && isGuestView == true)
        Integer idNguoiDung = (session != null) ? (Integer) session.getAttribute("idNguoiDung") : null;
        boolean isGuest = (session != null) && Boolean.TRUE.equals(session.getAttribute("isGuestView"));

        if (idNguoiDung != null && isGuest) {
            KhachHang guestKh = dashboardService.layThongTinKhachHang(idNguoiDung);
            if (guestKh != null) {
                model.addAttribute("kh", guestKh);
            }
            String tenHienThi = (String) session.getAttribute("tenHienThi");
            if (tenHienThi != null) {
                model.addAttribute("tenHienThi", tenHienThi);
            }
            model.addAttribute("guestAccountView", true);
            model.addAttribute("isGuestView", true);
            model.addAttribute("hasSidebar", true);
            return "dash-track-order";
        }

        // 3. Anonymous hoặc Existing Guest Order-only (idNguoiDung == null)
        model.addAttribute("publicView", true);
        model.addAttribute("hasSidebar", false);
        return "dash-track-order";
    }

    @PostMapping("/track-order/submit")
    public String submitTrackOrder(
            @RequestParam("orderId") String orderIdStr,
            @RequestParam(value = "contactInfo", required = false) String contactInfo,
            HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (orderIdStr == null || orderIdStr.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("loi", "Mã đơn hàng không được để trống.");
            return "redirect:/user/track-order";
        }

        if (contactInfo == null || contactInfo.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("loi", "Vui lòng nhập Email hoặc Số điện thoại đặt hàng.");
            redirectAttributes.addFlashAttribute("orderId", orderIdStr.trim());
            return "redirect:/user/track-order";
        }

        // Try lookup by maDonHang
        java.util.Optional<com.smashvn.shop.entity.HoaDon> hdOpt = hoaDonRepository.findByMaDonHang(orderIdStr.trim());
        if (hdOpt.isEmpty()) {
            String normalized = orderIdStr.trim().replace("-", "").replace("_", "");
            hdOpt = hoaDonRepository.findByMaDonHangOrNormalized(orderIdStr.trim(), normalized);
        }
        if (hdOpt.isEmpty() && orderIdStr.trim().matches("\\d+")) {
            hdOpt = hoaDonRepository.findById(Integer.parseInt(orderIdStr.trim()));
        }

        if (hdOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("loi", "Không tìm thấy đơn hàng tương ứng với mã cung cấp.");
            redirectAttributes.addFlashAttribute("orderId", orderIdStr.trim());
            return "redirect:/user/track-order";
        }

        com.smashvn.shop.entity.HoaDon hd = hdOpt.get();

        String searchVal = contactInfo.trim().toLowerCase();
        String orderEmail = (hd.getEmailNguoiNhan() != null && !hd.getEmailNguoiNhan().trim().isEmpty())
                ? hd.getEmailNguoiNhan().trim()
                : ((hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null)
                    ? hd.getKhachHang().getTaiKhoan().getUsername() : "");
        String orderPhone = hd.getSdtNhan() != null ? hd.getSdtNhan().trim() : "";

        boolean matchesEmail = !orderEmail.isEmpty() && orderEmail.toLowerCase().equals(searchVal);
        boolean matchesPhone = !orderPhone.isEmpty() && (orderPhone.equals(searchVal) || com.smashvn.shop.util.PhoneUtils.normalize(orderPhone).equals(com.smashvn.shop.util.PhoneUtils.normalize(searchVal)));

        if (!matchesEmail && !matchesPhone) {
            redirectAttributes.addFlashAttribute("loi", "Thông tin email hoặc số điện thoại không khớp với đơn hàng.");
            redirectAttributes.addFlashAttribute("orderId", orderIdStr.trim());
            return "redirect:/user/track-order";
        }

        // Successfully validated: grant guest access in session
        synchronized (session) {
            List<com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess> allowedAccesses
                    = (List<com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess>) session.getAttribute("allowedGuestOrderAccesses");
            if (allowedAccesses == null) {
                allowedAccesses = new java.util.ArrayList<>();
            }
            allowedAccesses.add(new com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess(hd.getId(), orderEmail, java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.MINUTES)));
            session.setAttribute("allowedGuestOrderAccesses", allowedAccesses);
            if (!orderEmail.isEmpty()) {
                session.setAttribute("guestCheckoutEmail", orderEmail);
            }
        }

        // Redirect to detail page
        return "redirect:/user/manage-order/" + hd.getId();
    }

    @GetMapping({"/manage-order/{id}", "/manage-order"})
    public String hienThiManageOrder(@PathVariable(value = "id", required = false) String pathId,
            @RequestParam(value = "id", required = false) String paramId,
            HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        String targetStr = (pathId != null) ? pathId : paramId;
        if (targetStr == null || targetStr.trim().isEmpty()) {
            return "redirect:/user/my-order";
        }

        // Try lookup by maDonHang first
        java.util.Optional<com.smashvn.shop.entity.HoaDon> hdOpt = hoaDonRepository.findByMaDonHang(targetStr.trim());
        if (hdOpt.isEmpty()) {
            // Try normalized maDonHang
            String normalized = targetStr.trim().replace("-", "").replace("_", "");
            hdOpt = hoaDonRepository.findByMaDonHangOrNormalized(targetStr.trim(), normalized);
        }
        if (hdOpt.isEmpty() && targetStr.trim().matches("\\d+")) {
            // Fallback: try by numeric ID
            hdOpt = hoaDonRepository.findById(Integer.parseInt(targetStr.trim()));
        }

        if (hdOpt.isEmpty()) {
            return "redirect:/user/my-order?loi=donhangkhongton";
        }

        com.smashvn.shop.entity.HoaDon hd = hdOpt.get();
        Integer targetId = hd.getId();

        KhachHang kh = getLoggedInCustomer(session);
        boolean isGuestView = false;

        if (kh == null) {
            if (!isGuestAllowedOrder(session, targetId, hd)) {
                return "redirect:/user/dang-nhap";
            }
            kh = hd.getKhachHang();
            isGuestView = true;
        } else if (kh.getTaiKhoan() != null && kh.getTaiKhoan().getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST) {
            isGuestView = true;
        }

        try {
            Map<String, Object> details = orderViewService.layChiTietOrder(targetId, kh != null ? kh.getId() : null);
            if (details == null) {
                return "redirect:/user/my-order?loi=donhangkhongton";
            }

            model.addAttribute("kh", kh);
            model.addAllAttributes(details);

            long wishlistCount = (kh != null) ? wishlistRepository.countByKhachHang_Id(kh.getId()) : 0;
            model.addAttribute("wishlistCount", wishlistCount);
            model.addAttribute("isGuestView", isGuestView);

            int soLanMua = (kh != null && kh.getTaiKhoan() != null && kh.getTaiKhoan().getSoLanMuaThanhCong() != null) ? kh.getTaiKhoan().getSoLanMuaThanhCong() : 0;
            boolean isFirstGuestOrder = isGuestView && (kh != null && kh.getTaiKhoan() != null && kh.getTaiKhoan().getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST && soLanMua <= 1);
            model.addAttribute("isFirstGuestOrder", isFirstGuestOrder);

            return "dash-manage-order";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user/my-order?loi=loihethong";
        }
    }

    @PostMapping("/manage-order/cancel/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> xuLyHuyDonHang(
            @PathVariable("id") Integer idHoaDon,
            @RequestParam(value = "lyDoHuy", required = false) String lyDoHuy,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            Optional<HoaDon> hdOpt = hoaDonRepository.findById(idHoaDon);
            if (hdOpt.isEmpty() || !isGuestAllowedOrder(session, idHoaDon, hdOpt.get())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện thao tác này.");
                return org.springframework.http.ResponseEntity.ok(response);
            }
            kh = hdOpt.get().getKhachHang();
        }

        String ipAddress = request.getRemoteAddr();
        try {
            boolean success = orderViewService.huyDonHang(idHoaDon, kh.getId(), ipAddress, lyDoHuy);
            if (success) {
                response.put("success", true);
                response.put("message", "Hủy đơn hàng thành công!");
            } else {
                response.put("success", false);
                response.put("message", "Không thể hủy đơn hàng này. Đơn hàng có thể đã được giao hoặc đang được xử lý.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PostMapping("/manage-order/confirm-received/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> xuLyXacNhanDaNhanHang(
            @PathVariable("id") Integer idHoaDon,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            Optional<HoaDon> hdOpt = hoaDonRepository.findById(idHoaDon);
            if (hdOpt.isEmpty() || !isGuestAllowedOrder(session, idHoaDon, hdOpt.get())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện thao tác này.");
                return org.springframework.http.ResponseEntity.ok(response);
            }
            kh = hdOpt.get().getKhachHang();
        }

        try {
            boolean success = orderViewService.xacNhanDaNhanHang(idHoaDon, kh.getId(), request.getRemoteAddr());
            if (success) {
                response.put("success", true);
                response.put("message", "Cảm ơn bạn đã xác nhận đã nhận được hàng!");
            } else {
                response.put("success", false);
                response.put("message", "Không thể xác nhận đơn hàng này. Vui lòng kiểm tra lại trạng thái đơn.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PostMapping("/manage-order/request-return/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> xuLyYeuCauTraHang(
            @PathVariable("id") Integer idHoaDon,
            @RequestParam(value = "loaiYeuCau", required = false) String loaiYeuCau,
            @RequestParam(value = "lyDo", required = false) String lyDo,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            Optional<HoaDon> hdOpt = hoaDonRepository.findById(idHoaDon);
            if (hdOpt.isEmpty() || !isGuestAllowedOrder(session, idHoaDon, hdOpt.get())) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền thực hiện thao tác này.");
                return org.springframework.http.ResponseEntity.ok(response);
            }
            kh = hdOpt.get().getKhachHang();
        }

        List<String> bangChungPaths = new java.util.ArrayList<>();
        try {
            bangChungPaths = fileStorageService.storeReturnEvidenceVideos(files, idHoaDon);
            boolean success = orderViewService.yeuCauTraHang(idHoaDon, kh.getId(), loaiYeuCau, lyDo, bangChungPaths, request.getRemoteAddr());
            if (success) {
                response.put("success", true);
                response.put("message", "Yêu cầu Đổi/Trả hàng của bạn đã được gửi thành công! Shop sẽ sớm phản hồi.");
            } else {
                fileStorageService.deleteReturnFiles(bangChungPaths);
                response.put("success", false);
                response.put("message", "Không thể gửi yêu cầu Đổi/Trả cho đơn hàng này.");
            }
        } catch (Exception e) {
            fileStorageService.deleteReturnFiles(bangChungPaths);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @GetMapping("/notifications")
    public String hienThiThongBao(HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("kh", kh);

        List<ThongBao> listThongBao = thongBaoRepository.findByTaiKhoan_IdOrderByNgayTaoDesc(kh.getTaiKhoan().getId());
        model.addAttribute("listThongBao", listThongBao);

        List<Map<String, Object>> ordersList = orderViewService.layDanhSachOrders(kh.getId());
        long cancelled = ordersList.stream().filter(o -> "cancelled".equals(o.get("status"))).count();
        long wishlistCount = wishlistRepository.countByKhachHang_Id(kh.getId());

        model.addAttribute("orderPlaced", ordersList.size() - cancelled);
        model.addAttribute("cancelOrders", cancelled);
        model.addAttribute("wishlist", wishlistCount);

        return "dash-notifications"; // Trỏ đến dash-notifications.html
    }

    @PostMapping("/notifications/read/{id}")
    public String danhDauDaDoc(@PathVariable("id") Integer id, HttpSession session) {
        KhachHang kh = getLoggedInCustomer(session);
        if (kh == null) {
            return "redirect:/user/dang-nhap";
        }

        thongBaoRepository.findById(id).ifPresent(tb -> {
            if (tb.getTaiKhoan().getId().equals(kh.getTaiKhoan().getId())) {
                tb.setDaDoc(true);
                thongBaoRepository.save(tb);
            }
        });
        return "redirect:/user/notifications";
    }

    @GetMapping("/order/invoice/{id}")
    public String viewInvoice(@PathVariable("id") Integer id, HttpSession session, Model model) {
        String redirect = checkRoleAndRedirect(session);
        if (redirect != null) {
            return redirect;
        }

        Optional<HoaDon> hdOpt = hoaDonRepository.findById(id);
        if (hdOpt.isEmpty()) {
            return "redirect:/user/my-order?loi=donhangkhongton";
        }

        HoaDon hd = hdOpt.get();
        KhachHang kh = getLoggedInCustomer(session);

        boolean isAllowed = false;
        if (kh != null) {
            if (hd.getKhachHang() != null && hd.getKhachHang().getId().equals(kh.getId())) {
                isAllowed = true;
            }
        } else {
            isAllowed = isGuestAllowedOrder(session, id, hd);
        }

        if (!isAllowed) {
            return "redirect:/user/dang-nhap";
        }

        try {
            Map<String, Object> details = orderViewService.layChiTietOrder(id, hd.getKhachHang() != null ? hd.getKhachHang().getId() : null);
            if (details == null) {
                return "redirect:/user/my-order?loi=donhangkhongton";
            }
            model.addAllAttributes(details);
            return "invoice-print";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/user/my-order?loi=loihethong";
        }
    }
}
