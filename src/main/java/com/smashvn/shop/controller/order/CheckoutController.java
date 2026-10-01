package com.smashvn.shop.controller.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.PaymentMethod;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.order.GuestCartService;
import com.smashvn.shop.service.order.GuestCheckoutService;
import com.smashvn.shop.service.product.PricingService;
import com.smashvn.shop.service.product.ProductAvailabilityService;
import com.smashvn.shop.service.user.UserAddressService;
import com.smashvn.shop.service.user.UserDangNhapService;
import com.smashvn.shop.service.user.TemporaryPasswordService;
import com.smashvn.shop.service.user.TemporaryPasswordService.TemporaryPasswordIssueResult;
import com.smashvn.shop.dto.order.*;
import com.smashvn.shop.repository.GioHangChiTietRepository;
import com.smashvn.shop.service.order.CheckoutContextService;
import com.smashvn.shop.service.order.PendingCheckoutRegistry;
import com.smashvn.shop.util.VoucherCalculator;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.smashvn.shop.security.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final GioHangService gioHangService;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final UserAddressService userAddressService;
    private final SepayConfig sepayConfig;
    private final com.smashvn.shop.repository.KhachHangRepository khachHangRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final PricingService pricingService;
    private final GuestCartService guestCartService;
    private final GuestCheckoutService guestCheckoutService;
    private final UserDangNhapService userDangNhapService;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final SoDiaChiRepository soDiaChiRepository;
    private final CheckoutContextService checkoutContextService;
    private final PendingCheckoutRegistry pendingCheckoutRegistry;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final ProductAvailabilityService productAvailabilityService;
    private final TemporaryPasswordService temporaryPasswordService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Value("${app.base-url:}")
    private String configuredBaseUrl;

    private String resolveBaseUrl(HttpServletRequest request) {
        if (configuredBaseUrl != null && !configuredBaseUrl.trim().isEmpty()) {
            return configuredBaseUrl.trim().replaceAll("/+$", "");
        }
        try {
            return org.springframework.web.servlet.support.ServletUriComponentsBuilder
                    .fromContextPath(request)
                    .build()
                    .toUriString();
        } catch (Exception e) {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();
            if ((scheme.equalsIgnoreCase("http") && serverPort == 80) || (scheme.equalsIgnoreCase("https") && serverPort == 443)) {
                return scheme + "://" + serverName + contextPath;
            }
            return scheme + "://" + serverName + ":" + serverPort + contextPath;
        }
    }

    @PostMapping("/checkout/start-all")
    @ResponseBody
    public ResponseEntity<FullCartCheckoutResult> startAllCheckout(HttpSession session) {
        try {
            Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
            FullCartCheckoutResult result = checkoutContextService.createFullCartContext(session, idNguoiDung);

            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (Exception e) {
            log.error("Error in startAllCheckout", e);
            FullCartCheckoutResult errorResult = FullCartCheckoutResult.builder()
                    .trangThai("error")
                    .thongBao("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.")
                    .message("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.")
                    .build();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @PostMapping("/checkout/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startCheckout(
            @RequestParam(value = "selectedItemIds", required = false) List<Integer> selectedItemIds,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        if (selectedItemIds == null || selectedItemIds.isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Vui lòng chọn ít nhất một sản phẩm để thanh toán!");
            return ResponseEntity.ok(response);
        }

        List<Integer> distinctIds = selectedItemIds.stream().distinct().collect(java.util.stream.Collectors.toList());
        if (distinctIds.size() > 100) {
            response.put("trangThai", "loi");
            response.put("message", "Số lượng sản phẩm thanh toán không được vượt quá 100 sản phẩm.");
            return ResponseEntity.ok(response);
        }

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(idNguoiDung);

        List<CheckoutItemContext> contextItems = new java.util.ArrayList<>();

        if (!activeAccount) {
            List<GuestCartService.GuestCartItem> guestCart = guestCartService.getGuestCartItems(session);
            for (Integer spctId : distinctIds) {
                GuestCartService.GuestCartItem match = guestCart.stream()
                        .filter(item -> item.getIdSanPhamChiTiet().equals(spctId))
                        .findFirst()
                        .orElse(null);
                if (match == null || match.getSoLuong() == null || match.getSoLuong() <= 0) {
                    response.put("trangThai", "loi");
                    response.put("message", "Một số sản phẩm đã chọn không còn tồn tại trong giỏ hàng. Vui lòng tải lại giỏ hàng!");
                    return ResponseEntity.ok(response);
                }

                com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(spctId).orElse(null);
                if (spct == null) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm đã chọn không tồn tại trong hệ thống.");
                    return ResponseEntity.ok(response);
                }
                boolean dangBan = productAvailabilityService.isVariantPublished(spct);
                if (!dangBan) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm \"" + (spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "") + "\" đã ngừng kinh doanh.");
                    return ResponseEntity.ok(response);
                }
                int tonKho = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
                if (match.getSoLuong() > tonKho) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm \"" + spct.getSanPham().getTenSanPham() + "\" số lượng (" + match.getSoLuong() + ") vượt quá tồn kho (" + tonKho + "). Vui lòng điều chỉnh lại!");
                    return ResponseEntity.ok(response);
                }

                contextItems.add(CheckoutItemContext.builder()
                        .cartItemId(null)
                        .idSanPhamChiTiet(spctId)
                        .soLuong(match.getSoLuong())
                        .fromCart(true)
                        .build());
            }
        } else {
            com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
            if (kh == null) {
                response.put("trangThai", "loi");
                response.put("message", "Thông tin tài khoản không hợp lệ.");
                return ResponseEntity.ok(response);
            }
            List<GioHangChiTiet> dbCartItems = gioHangChiTietRepository.findAllByIdInAndGioHang_KhachHang_Id(distinctIds, kh.getId());
            if (dbCartItems.size() != distinctIds.size()) {
                response.put("trangThai", "loi");
                response.put("message", "Một số sản phẩm đã chọn không còn tồn tại trong giỏ hàng. Vui lòng tải lại giỏ hàng!");
                return ResponseEntity.ok(response);
            }

            for (GioHangChiTiet item : dbCartItems) {
                com.smashvn.shop.entity.SanPhamChiTiet spct = item.getSanPhamChiTiet();
                if (spct == null) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm trong giỏ không hợp lệ.");
                    return ResponseEntity.ok(response);
                }
                boolean dangBan = productAvailabilityService.isVariantPublished(spct);
                if (!dangBan) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm \"" + (spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "") + "\" đã ngừng kinh doanh.");
                    return ResponseEntity.ok(response);
                }
                int tonKho = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
                if (item.getSoLuong() == null || item.getSoLuong() <= 0) {
                    response.put("trangThai", "loi");
                    response.put("message", "Số lượng sản phẩm trong giỏ hàng không hợp lệ.");
                    return ResponseEntity.ok(response);
                }
                if (item.getSoLuong() > tonKho) {
                    response.put("trangThai", "loi");
                    response.put("message", "Sản phẩm \"" + spct.getSanPham().getTenSanPham() + "\" số lượng (" + item.getSoLuong() + ") vượt quá tồn kho (" + tonKho + "). Vui lòng điều chỉnh lại!");
                    return ResponseEntity.ok(response);
                }

                contextItems.add(CheckoutItemContext.builder()
                        .cartItemId(item.getId())
                        .idSanPhamChiTiet(spct.getId())
                        .soLuong(item.getSoLuong())
                        .fromCart(true)
                        .build());
            }
        }


        if (contextItems.isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Không tìm thấy sản phẩm hợp lệ trong giỏ hàng để thanh toán!");
            return ResponseEntity.ok(response);
        }

        CheckoutContext context = checkoutContextService.createCartContext(session, idNguoiDung, contextItems);
        response.put("trangThai", "ok");
        response.put("checkoutToken", context.getToken());
        response.put("checkoutUrl", "/checkout?token=" + context.getToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/buy-now")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buyNow(
            @RequestParam(value = "idSanPhamChiTiet", required = false) Integer idSanPhamChiTiet,
            @RequestParam(value = "soLuong", required = false, defaultValue = "1") Integer soLuong,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        if (idSanPhamChiTiet == null) {
            response.put("trangThai", "loi");
            response.put("message", "Sản phẩm không hợp lệ.");
            return ResponseEntity.ok(response);
        }
        if (soLuong == null || soLuong <= 0) {
            response.put("trangThai", "loi");
            response.put("message", "Số lượng mua không hợp lệ.");
            return ResponseEntity.ok(response);
        }

        com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idSanPhamChiTiet).orElse(null);
        if (!productAvailabilityService.isVariantPublished(spct)) {
            response.put("trangThai", "loi");
            response.put("message", "Sản phẩm hoặc phân loại đã ngưng kinh doanh.");
            return ResponseEntity.ok(response);
        }
        if (spct.getSoLuongTon() < soLuong) {
            response.put("trangThai", "loi");
            response.put("message", "Số lượng tồn kho không đủ (Còn lại: " + spct.getSoLuongTon() + ").");
            return ResponseEntity.ok(response);
        }

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        CheckoutContext context = checkoutContextService.createBuyNowContext(session, idNguoiDung, idSanPhamChiTiet, soLuong);

        response.put("trangThai", "ok");
        response.put("checkoutToken", context.getToken());
        response.put("checkoutUrl", "/checkout?token=" + context.getToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/checkout", "/checkout.html"})
    public String viewCheckout(
            @RequestParam(value = "token", required = false) String token,
            HttpSession session,
            Model model) {

        if (token == null || token.isBlank()) {
            return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Vui lòng chọn sản phẩm để thanh toán!", java.nio.charset.StandardCharsets.UTF_8);
        }

        CheckoutContext context = checkoutContextService.getContext(session, token);
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(idNguoiDung);


        if (context == null || !checkoutContextService.validateOwnership(context, idNguoiDung, session.getId())) {
            return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Phiên thanh toán không hợp lệ hoặc đã hết hạn!", java.nio.charset.StandardCharsets.UTF_8);
        }

        if (context.getCompletedCheckout() != null) {
            restoreGuestOrderAccess(context.getCompletedCheckout(), session);
            return "redirect:" + context.getCompletedCheckout().redirectUrl();
        }

        List<GioHangChiTiet> danhSachChiTiet = new java.util.ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO;

        for (CheckoutItemContext itemCtx : context.getItems()) {
            com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(itemCtx.getIdSanPhamChiTiet()).orElse(null);
            if (spct == null) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Giỏ hàng chứa sản phẩm không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
            }

            SanPham sp = spct.getSanPham();
            int tonKho = spct.getSoLuongTon();
            int reqQty = itemCtx.getSoLuong() != null ? itemCtx.getSoLuong() : 0;

            if (reqQty <= 0) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Số lượng sản phẩm không hợp lệ!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (!productAvailabilityService.isVariantPublished(spct)) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Phân loại sản phẩm '" + sp.getTenSanPham() + "' đã ngưng kinh doanh!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (tonKho <= 0) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' đã hết hàng!", java.nio.charset.StandardCharsets.UTF_8);
            }
            if (reqQty > tonKho) {
                return "redirect:/gio-hang?loi=" + java.net.URLEncoder.encode("Sản phẩm '" + sp.getTenSanPham() + "' không đủ số lượng tồn kho (Còn lại: " + tonKho + ")!", java.nio.charset.StandardCharsets.UTF_8);
            }

            GioHangChiTiet detail = new GioHangChiTiet();
            detail.setId(itemCtx.getCartItemId() != null ? itemCtx.getCartItemId() : spct.getId());
            detail.setSanPhamChiTiet(spct);
            detail.setSoLuong(reqQty);

            BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(spct);
            tongTien = tongTien.add(giaBanSauGiam.multiply(new BigDecimal(reqQty)));
            danhSachChiTiet.add(detail);
        }

        model.addAttribute("checkoutToken", token);


        List<DonViVanChuyen> listDvvc = donViVanChuyenDAO.findAll().stream()
                .filter(dv -> DonViVanChuyen.isGhnCarrier(dv))
                .collect(java.util.stream.Collectors.toList());

        if (listDvvc.isEmpty()) {
            listDvvc = donViVanChuyenDAO.findAll().stream()
                    .filter(dv -> !DonViVanChuyen.isCounterCarrier(dv))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (listDvvc.isEmpty()) {
            DonViVanChuyen defaultGhn = new DonViVanChuyen();
            defaultGhn.setMaDonVi("GHN");
            defaultGhn.setTenDonVi("Giao Hàng Nhanh (GHN)");
            defaultGhn.setHotline("1900 636677");
            defaultGhn.setWebsite("https://ghn.vn");
            defaultGhn.setPhiLocal(new java.math.BigDecimal("25000"));
            defaultGhn.setPhiNationwide(new java.math.BigDecimal("38000"));
            listDvvc = java.util.List.of(donViVanChuyenDAO.save(defaultGhn));
        }

        List<SoDiaChi> listDiaChi = new java.util.ArrayList<>();
        boolean hasDefaultAddress = false;
        if (idNguoiDung != null) {
            com.smashvn.shop.entity.KhachHang khachHang = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
            if (khachHang == null) {
                khachHang = khachHangRepository.findById(idNguoiDung).orElse(null);
            }
            if (khachHang != null) {
                listDiaChi = userAddressService.layDanhSachDiaChi(khachHang.getId());
                if (listDiaChi != null && !listDiaChi.isEmpty()) {
                    hasDefaultAddress = listDiaChi.stream().anyMatch(SoDiaChi::isDefaultShipping);
                    if (!hasDefaultAddress) {
                        listDiaChi.get(0).setDefaultShipping(true);
                        hasDefaultAddress = true;
                    }
                }
            }
        }

        Map<Integer, Map<String, Object>> addressMap = new java.util.HashMap<>();
        for (SoDiaChi dc : listDiaChi) {
            Map<String, Object> details = new java.util.HashMap<>();
            String hoTenNguoiNhan = (dc.getHoNguoiNhan() != null && !dc.getHoNguoiNhan().isBlank() ? dc.getHoNguoiNhan().trim() + " " : "")
                    + (dc.getTenNguoiNhan() != null ? dc.getTenNguoiNhan().trim() : "");
            details.put("hoTen", hoTenNguoiNhan.trim());
            details.put("sdt", dc.getSdtNguoiNhan());
            details.put("diaChiCuThe", dc.getDiaChiCuThe());
            details.put("tinhThanh", dc.getProvinceName());
            details.put("thanhPho", dc.getDistrictName());
            details.put("quocGia", dc.getQuocGia());
            details.put("latitude", dc.getLatitude());
            details.put("longitude", dc.getLongitude());
            details.put("wardName", dc.getWardName());
            // Checkout must reuse the exact GHN master-data mapping saved in the
            // address book. Re-parsing the display text can select a different
            // district/ward and is the reason saved addresses behaved differently
            // from manually selected checkout addresses.
            details.put("ghnProvinceId", dc.getProvinceId());
            details.put("ghnDistrictId", dc.getDistrictId());
            details.put("ghnWardCode", dc.getWardCode());
            details.put("ghnReady", dc.getProvinceId() != null
                    && dc.getDistrictId() != null
                    && dc.getWardCode() != null
                    && !dc.getWardCode().isBlank());
            String fullAddress = dc.getDiaChiCuThe();
            if (dc.getWardName() != null && !dc.getWardName().trim().isEmpty() && !fullAddress.contains(dc.getWardName())) {
                fullAddress += ", " + dc.getWardName().trim();
            }
            if (dc.getDistrictName() != null && !dc.getDistrictName().trim().isEmpty() && !fullAddress.contains(dc.getDistrictName())) {
                fullAddress += ", " + dc.getDistrictName().trim();
            }
            if (dc.getProvinceName() != null && !dc.getProvinceName().trim().isEmpty() && !fullAddress.contains(dc.getProvinceName())) {
                fullAddress += ", " + dc.getProvinceName().trim();
            }
            if (dc.getQuocGia() != null && !dc.getQuocGia().trim().isEmpty() && !fullAddress.contains(dc.getQuocGia())) {
                fullAddress += ", " + dc.getQuocGia().trim();
            }
            details.put("diaChi", fullAddress);
            addressMap.put(dc.getId(), details);
        }

        String addressMapJson = "{}";
        try {
            addressMapJson = objectMapper.writeValueAsString(addressMap);
        } catch (Exception e) {
            log.error("Failed to serialize address map for checkout", e);
        }

        model.addAttribute("danhSachCart", danhSachChiTiet);
        model.addAttribute("tongTien", tongTien);
        model.addAttribute("listDvvc", listDvvc);
        model.addAttribute("listDiaChi", listDiaChi);
        model.addAttribute("hasDefaultAddress", hasDefaultAddress);
        model.addAttribute("addressMapJson", addressMapJson);
        model.addAttribute("sepayBankAccount", sepayConfig.getBankAccount());
        model.addAttribute("sepayBankName", sepayConfig.getBankName());
        model.addAttribute("sepayMemoPrefix", sepayConfig.getMemoPrefix());

        boolean isGuest = true;
        if (activeAccount) {
            isGuest = false;
        }
        model.addAttribute("isGuest", isGuest);

        if (idNguoiDung != null && !activeAccount) {
            TaiKhoan sessionTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
            if (sessionTk != null) {
                model.addAttribute("guestEmail", sessionTk.getUsername());
            }
        }

        return "checkout";
    }

    private boolean isActiveAccount(Integer idNguoiDung) {
        if (idNguoiDung == null) {
            return false;
        }
        TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        return tk != null
                && tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.ACTIVE
                && tk.getMatKhau() != null && !tk.getMatKhau().trim().isEmpty()
                && "hoat_dong".equalsIgnoreCase(tk.getTrangThai());
    }

    @PostMapping("/checkout/submit")


    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitCheckout(
            @RequestParam(value = "checkoutToken", required = false) String checkoutToken,
            @RequestParam(value = "hoTenNhan", required = false) String hoTenNhan,
            @RequestParam(value = "sdtNhan", required = false) String sdtNhan,
            @RequestParam(value = "diaChiNhan", required = false) String diaChiNhan,
            @RequestParam(value = "idDonViVanChuyen", required = false) Integer idDonViVanChuyen,
            @RequestParam(value = "phuongThucThanhToan", required = false) String phuongThucThanhToan,
            @RequestParam(value = "ghiChu", required = false) String ghiChu,
            @RequestParam(value = "ghnToDistrictId", required = false) Integer ghnToDistrictId,
            @RequestParam(value = "ghnToWardCode", required = false) String ghnToWardCode,
            @RequestParam(value = "ghnProvinceId", required = false) Integer ghnProvinceId,
            @RequestParam(value = "idDiaChiLuu", required = false) Integer idDiaChiLuu,
            @RequestParam(value = "voucherCode", required = false) String voucherCode,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "tinhThanhText", required = false) String tinhThanhText,
            @RequestParam(value = "thanhPhoText", required = false) String thanhPhoText,
            @RequestParam(value = "phuongXaText", required = false) String phuongXaText,
            @RequestParam(value = "diaChiCuThe", required = false) String diaChiCuTheParam,
            @RequestParam(value = "saveAddress", required = false) Boolean saveAddress,
            HttpSession session,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");

        CheckoutContext context = null;
        if (checkoutToken != null && !checkoutToken.isBlank()) {
            context = checkoutContextService.getContext(session, checkoutToken);
            if (context == null || !checkoutContextService.validateOwnership(context, idNguoiDung, session.getId())) {
                return ResponseEntity.ok(invalidCheckoutResponse());
            }
        }

        if (context == null) {
            List<CheckoutItemContext> contextItems = new java.util.ArrayList<>();
            if (idNguoiDung != null) {
                com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
                if (kh != null) {
                    List<GioHangChiTiet> dbCartItems = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
                    for (GioHangChiTiet item : dbCartItems) {
                        if (item.getSoLuong() != null && item.getSoLuong() > 0) {
                            contextItems.add(CheckoutItemContext.builder()
                                    .cartItemId(item.getId())
                                    .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                                    .soLuong(item.getSoLuong())
                                    .fromCart(true)
                                    .build());
                        }
                    }
                }
            } else {
                List<GuestCartService.GuestCartItem> guestCart = guestCartService.getGuestCartItems(session);
                for (GuestCartService.GuestCartItem item : guestCart) {
                    if (item.getSoLuong() != null && item.getSoLuong() > 0) {
                        contextItems.add(CheckoutItemContext.builder()
                                .cartItemId(null)
                                .idSanPhamChiTiet(item.getIdSanPhamChiTiet())
                                .soLuong(item.getSoLuong())
                                .fromCart(true)
                                .build());
                    }
                }
            }
            context = checkoutContextService.createCartContext(session, idNguoiDung, contextItems);
        }

        if (!context.tryClaim()) {
            return ResponseEntity.ok(checkoutStateResponse(context, session));
        }



        boolean startedAsAnonymousGuest = (idNguoiDung == null);
        String emailStatus = "NEW";
        boolean newGuestAccount = false;

        long startPipeline = System.currentTimeMillis();
        try {
            log.info("[GuestCheckout] Starting guest checkout pipeline execution.");

            // Sanitize text inputs
            hoTenNhan = sanitizeInput(hoTenNhan);
            sdtNhan = sanitizeInput(sdtNhan);
            diaChiNhan = sanitizeInput(diaChiNhan);
            ghiChu = sanitizeInput(ghiChu);
            tinhThanhText = sanitizeInput(tinhThanhText);
            thanhPhoText = sanitizeInput(thanhPhoText);
            phuongXaText = sanitizeInput(phuongXaText);
            diaChiCuTheParam = sanitizeInput(diaChiCuTheParam);

            // Validate inputs if user is not using a saved address
            if (idDiaChiLuu == null) {
                // Validate presence first
                if (hoTenNhan == null || hoTenNhan.trim().isEmpty()) {
                    response.put("trangThai", "loi");
                    response.put("message", "Họ và tên người nhận không được để trống.");
                    return ResponseEntity.ok(response);
                }
                if (sdtNhan == null || sdtNhan.trim().isEmpty()) {
                    response.put("trangThai", "loi");
                    response.put("message", "Số điện thoại không được để trống.");
                    return ResponseEntity.ok(response);
                }
                if (diaChiNhan == null || diaChiNhan.trim().isEmpty()) {
                    response.put("trangThai", "loi");
                    response.put("message", "Địa chỉ nhận hàng không được để trống.");
                    return ResponseEntity.ok(response);
                }

                // Validate length & format next
                if (hoTenNhan.trim().length() < 2 || hoTenNhan.trim().length() > 100) {
                    response.put("trangThai", "loi");
                    response.put("message", "Họ và tên người nhận phải từ 2 đến 100 ký tự.");
                    return ResponseEntity.ok(response);
                }

                String normalizedPhone = com.smashvn.shop.util.PhoneUtils.normalize(sdtNhan);
                if (!com.smashvn.shop.util.PhoneUtils.isValid(normalizedPhone)) {
                    response.put("trangThai", "loi");
                    response.put("message", "Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
                    return ResponseEntity.ok(response);
                }
            }

            TaiKhoan checkoutTk = null;
            if (idNguoiDung != null) {
                checkoutTk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
            }
            boolean isGuestCheckout = (idNguoiDung == null) || (checkoutTk != null && checkoutTk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST);

            if (isGuestCheckout) {
                String guestEmail = (email != null && !email.trim().isEmpty())
                        ? email.trim()
                        : ((checkoutTk != null) ? checkoutTk.getUsername() : null);

                if (guestEmail == null || guestEmail.trim().isEmpty()) {
                    response.put("trangThai", "loi");
                    response.put("message", "Email không được để trống.");
                    return ResponseEntity.ok(response);
                }

                emailStatus = guestCheckoutService.checkEmailStatus(guestEmail);
                if ("LOCKED".equals(emailStatus)) {
                    response.put("trangThai", "loi");
                    response.put("message", "Tài khoản liên kết với email này đã bị khóa hoặc đang chờ khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ!");
                    return ResponseEntity.ok(response);
                } else if ("ACTIVE".equals(emailStatus)) {
                    if (checkoutTk == null || !checkoutTk.getUsername().equalsIgnoreCase(guestEmail)) {
                        response.put("trangThai", "yeucaudangnhap");
                        response.put("email", guestEmail);
                        response.put("message", "Tài khoản thành viên đã tồn tại. Vui lòng đăng nhập mật khẩu để đặt hàng.");
                        return ResponseEntity.ok(response);
                    }
                }

                if (idNguoiDung == null) {
                    long startAccount = System.currentTimeMillis();
                    try {
                        com.smashvn.shop.service.order.GuestCheckoutService.GuestRegisterResult regResult = guestCheckoutService.autoRegisterGuest(hoTenNhan, sdtNhan, guestEmail);
                        TaiKhoan tk = regResult.getTaiKhoan();
                        newGuestAccount = regResult.isNewAccount();
                        com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());

                        guestCartService.transferGuestCartToDb(session, kh.getId());

                        boolean isNewlyCreatedGuest = regResult.isNewAccount();
                        Integer currentSessionUserId = (Integer) session.getAttribute("idNguoiDung");
                        boolean alreadyOwnsGuestSession = (currentSessionUserId != null && currentSessionUserId.equals(tk.getId()) && Boolean.TRUE.equals(session.getAttribute("isGuestView")));

                        request.changeSessionId();
                        session = request.getSession(true);
                        context.setSessionId(session.getId());

                        if (isNewlyCreatedGuest || alreadyOwnsGuestSession) {
                            // SAFE GUEST AUTO-LOGIN: Tạo / duy trì Guest Account Session cho email mới hoặc chính session sở hữu
                            session.setAttribute("idNguoiDung", tk.getId());
                            session.setAttribute("isGuestView", true);
                            session.setAttribute("vaiTro", tk.getVaiTro());
                            session.setAttribute("nguoiDungDangNhap", tk.getUsername());
                            session.setAttribute("tenHienThi", hoTenNhan);
                            session.setAttribute("guestCheckoutEmail", tk.getUsername());
                        } else {
                            // EXISTING GUEST TỪ BROWSER KHÁC (Chưa xác thực ownership):
                            // TUYỆT ĐỐI KHÔNG gán idNguoiDung cũ để bảo vệ tài khoản nạn nhân
                            session.setAttribute("isGuestView", true);
                            session.setAttribute("guestCheckoutEmail", guestEmail);
                            session.setAttribute("tenHienThi", hoTenNhan);
                        }

                        checkoutTk = tk;
                        long endAccount = System.currentTimeMillis();
                        log.info("[GuestCheckout] Auto-register guest account: {}ms - SUCCESS (Safe Guest Auto-login: {})", (endAccount - startAccount), isNewlyCreatedGuest || alreadyOwnsGuestSession);
                    } catch (Exception e) {
                        long endAccount = System.currentTimeMillis();
                        log.error("[GuestCheckout] Create inactive account: {}ms - FAILED. Exception: {}", (endAccount - startAccount), e.getMessage(), e);
                        response.put("trangThai", "loi");
                        if (isPhoneUniqueConstraintViolation(e)) {
                            response.put("message", "Số điện thoại này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng số điện thoại khác.");
                        } else if (e instanceof org.springframework.dao.DataIntegrityViolationException || isDatabaseException(e)) {
                            response.put("message", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
                        } else {
                            response.put("message", e.getMessage());
                        }
                        return ResponseEntity.ok(response);
                    }
                }
            } else {
                log.info("[GuestCheckout] Inactive account exists or already registered: Skip autoRegisterGuest.");
                if (idNguoiDung != null) {
                    com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idNguoiDung);
                    if (kh != null) {
                        guestCartService.transferGuestCartToDb(session, kh.getId());
                    }
                }
            }

            Integer accountIdForOrder = (checkoutTk != null) ? checkoutTk.getId() : idNguoiDung;
            com.smashvn.shop.entity.KhachHang khachHang = (accountIdForOrder != null) ? khachHangRepository.findByTaiKhoan_Id(accountIdForOrder) : null;
            Integer idKhachHang = (khachHang != null) ? khachHang.getId() : accountIdForOrder;

            if (idDiaChiLuu != null) {
                try {
                    userAddressService.layDiaChiTheoId(idDiaChiLuu, idKhachHang);
                } catch (Exception e) {
                    response.put("trangThai", "loi");
                    response.put("message", "Địa chỉ đã lưu không tồn tại hoặc không thuộc về tài khoản của bạn. Vui lòng chọn địa chỉ khác hoặc nhập địa chỉ mới.");
                    return ResponseEntity.ok(response);
                }
            } else {
                // Mandatory check for GHN IDs on new address checkout
                if (ghnProvinceId == null || ghnToDistrictId == null || ghnToWardCode == null || ghnToWardCode.trim().isEmpty()) {
                    response.put("trangThai", "loi");
                    response.put("message", "Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã để sử dụng địa chỉ nhận hàng.");
                    return ResponseEntity.ok(response);
                }
            }

            // Resolve Giao Hàng Nhanh (GHN) carrier for online orders
            DonViVanChuyen dvvc = null;
            if (idDonViVanChuyen != null) {
                dvvc = donViVanChuyenDAO.findById(idDonViVanChuyen).orElse(null);
            }
            if (dvvc == null || DonViVanChuyen.isCounterCarrier(dvvc)) {
                dvvc = donViVanChuyenDAO.findAll().stream()
                        .filter(c -> DonViVanChuyen.isGhnCarrier(c))
                        .findFirst()
                        .orElse(null);
            }
            if (dvvc == null) {
                dvvc = donViVanChuyenDAO.findAll().stream()
                        .filter(c -> !DonViVanChuyen.isCounterCarrier(c))
                        .findFirst()
                        .orElse(null);
            }
            if (dvvc == null) {
                DonViVanChuyen defaultGhn = new DonViVanChuyen();
                defaultGhn.setMaDonVi("GHN");
                defaultGhn.setTenDonVi("Giao Hàng Nhanh (GHN)");
                defaultGhn.setHotline("1900 636677");
                defaultGhn.setWebsite("https://ghn.vn");
                defaultGhn.setPhiLocal(new java.math.BigDecimal("25000"));
                defaultGhn.setPhiNationwide(new java.math.BigDecimal("38000"));
                dvvc = donViVanChuyenDAO.save(defaultGhn);
            }
            Integer idDonViVanChuyenResolved = dvvc.getId();

            if (phuongThucThanhToan == null || phuongThucThanhToan.trim().isEmpty()) {
                response.put("trangThai", "loi");
                response.put("message", "Vui lòng chọn phương thức thanh toán.");
                return ResponseEntity.ok(response);
            }

            Integer resolvedDiaChiLuuId = idDiaChiLuu;

            // Save address to SoDiaChi first if applicable (so we can pass the ID to createOrder)
            if (idDiaChiLuu == null && khachHang != null) {
                TaiKhoan currentTk = (idNguoiDung != null) ? taiKhoanRepository.findById(idNguoiDung).orElse(null) : checkoutTk;
                boolean shouldSave = false;
                boolean setAsDefault = false;

                if (currentTk != null) {
                    long existingCount = soDiaChiRepository.findByKhachHang_Id(khachHang.getId()).size();
                    if (currentTk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST
                            || startedAsAnonymousGuest
                            || existingCount == 0
                            || Boolean.TRUE.equals(saveAddress)) {
                        // Guest account / Auto-registered guest / First address: automatically save to SoDiaChi
                        shouldSave = true;
                        if (existingCount == 0) {
                            setAsDefault = true;
                        }
                    }
                }

                if (shouldSave) {
                    String tinhThanhVal = tinhThanhText != null ? tinhThanhText.trim() : "";
                    String thanhPhoVal = thanhPhoText != null ? thanhPhoText.trim() : "";
                    String diaChiCuTheVal = diaChiCuTheParam != null ? diaChiCuTheParam.trim() : "";
                    String sdtVal = sdtNhan != null ? sdtNhan.trim() : "";

                    if (tinhThanhVal.isEmpty() || tinhThanhVal.contains("--")) {
                        if (diaChiNhan != null && !diaChiNhan.trim().isEmpty()) {
                            String[] parts = diaChiNhan.split(",");
                            if (parts.length >= 3) {
                                tinhThanhVal = parts[parts.length - 1].trim();
                                thanhPhoVal = parts[parts.length - 2].trim();
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < parts.length - 2; i++) {
                                    if (i > 0) {
                                        sb.append(", ");
                                    }
                                    sb.append(parts[i].trim());
                                }
                                diaChiCuTheVal = sb.toString();
                            } else if (parts.length == 2) {
                                tinhThanhVal = parts[1].trim();
                                thanhPhoVal = parts[1].trim();
                                diaChiCuTheVal = parts[0].trim();
                            } else {
                                tinhThanhVal = diaChiNhan.trim();
                                thanhPhoVal = diaChiNhan.trim();
                                diaChiCuTheVal = diaChiNhan.trim();
                            }
                        }
                    }

                    // Check duplicate
                    SoDiaChi existingAddress = null;
                    List<SoDiaChi> existingAddresses = soDiaChiRepository.findByKhachHang_Id(khachHang.getId());
                    for (SoDiaChi dc : existingAddresses) {
                        String existingDiaChiCuThe = dc.getDiaChiCuThe() != null ? dc.getDiaChiCuThe().trim() : "";
                        String existingTinhThanh = dc.getTinhThanh() != null ? dc.getTinhThanh().trim() : "";
                        String existingThanhPho = dc.getThanhPho() != null ? dc.getThanhPho().trim() : "";
                        String existingSdt = dc.getSdtNguoiNhan() != null ? dc.getSdtNguoiNhan().trim() : "";

                        if (existingDiaChiCuThe.equalsIgnoreCase(diaChiCuTheVal)
                                && existingTinhThanh.equalsIgnoreCase(tinhThanhVal)
                                && existingThanhPho.equalsIgnoreCase(thanhPhoVal)
                                && existingSdt.equalsIgnoreCase(sdtVal)) {
                            existingAddress = dc;
                            break;
                        }
                    }

                    if (existingAddress == null) {
                        SoDiaChi newAddress = new SoDiaChi();
                        newAddress.setKhachHang(khachHang);

                        // Split name
                        String ho = "Khách";
                        String ten = "Vãng Lai";
                        if (hoTenNhan != null && !hoTenNhan.trim().isEmpty()) {
                            String name = hoTenNhan.trim();
                            int lastSpace = name.lastIndexOf(' ');
                            if (lastSpace >= 0) {
                                ho = name.substring(0, lastSpace).trim();
                                ten = name.substring(lastSpace + 1).trim();
                            } else {
                                ho = "";
                                ten = name;
                            }
                        }
                        newAddress.setHoNguoiNhan(ho);
                        newAddress.setTenNguoiNhan(ten);
                        newAddress.setSdtNguoiNhan(sdtVal);
                        newAddress.setDiaChiCuThe(diaChiCuTheVal);
                        newAddress.setTinhThanh(tinhThanhVal);
                        newAddress.setThanhPho(thanhPhoVal);
                        newAddress.setQuocGia("Việt Nam");
                        newAddress.setMaBuuDien("700000");
                        newAddress.setProvinceId(ghnProvinceId);
                        newAddress.setDistrictId(ghnToDistrictId);
                        newAddress.setWardCode(ghnToWardCode);
                        newAddress.setProvinceName(tinhThanhVal);
                        newAddress.setDistrictName(thanhPhoVal);
                        newAddress.setWardName(phuongXaText != null ? phuongXaText.trim() : null);

                        if (setAsDefault) {
                            newAddress.setDefaultShipping(true);
                            newAddress.setDefaultBilling(true);
                        } else {
                            newAddress.setDefaultShipping(false);
                            newAddress.setDefaultBilling(false);
                        }

                        newAddress = soDiaChiRepository.save(newAddress);
                        resolvedDiaChiLuuId = newAddress.getId();
                        log.info("[Checkout] Saved new address to SoDiaChi: id={}, isDefault={}", newAddress.getId(), setAsDefault);
                    } else {
                        // Update existing address with GHN IDs if they are missing
                        if (existingAddress.getProvinceId() == null || existingAddress.getDistrictId() == null || existingAddress.getWardCode() == null) {
                            existingAddress.setProvinceId(ghnProvinceId);
                            existingAddress.setDistrictId(ghnToDistrictId);
                            existingAddress.setWardCode(ghnToWardCode);
                            soDiaChiRepository.save(existingAddress);
                            log.info("[Checkout] Updated existing address ID {} with GHN details.", existingAddress.getId());
                        }
                        resolvedDiaChiLuuId = existingAddress.getId();
                        log.info("[Checkout] Address already exists in SoDiaChi: id={}, skipped saving.", resolvedDiaChiLuuId);
                    }
                }
            }

            long startOrder = System.currentTimeMillis();
            OrderCreationResult orderResult;
            boolean isCod = "COD".equalsIgnoreCase(phuongThucThanhToan);

            if (isCod) {
                orderResult = gioHangService.submitCodOrder(
                        accountIdForOrder, context, session,
                        hoTenNhan, sdtNhan, diaChiNhan, idDonViVanChuyenResolved,
                        ghiChu, ghnToDistrictId, ghnToWardCode, ghnProvinceId,
                        resolvedDiaChiLuuId, voucherCode);
            } else {
                orderResult = gioHangService.createSepayPendingOrder(
                        accountIdForOrder, context, session,
                        hoTenNhan, sdtNhan, diaChiNhan, idDonViVanChuyenResolved,
                        ghiChu, ghnToDistrictId, ghnToWardCode, ghnProvinceId,
                        resolvedDiaChiLuuId, voucherCode);
            }

            // Both service methods are transactional. Retain the committed result before
            // any registry, session, password or email work can fail.
            HoaDon hd = orderResult.getHoaDon();
            boolean isGuest = checkoutTk != null
                    && checkoutTk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST;
            String guestEmailToRecord = email != null && !email.isBlank()
                    ? email.trim() : (checkoutTk != null ? checkoutTk.getUsername() : null);
            context.complete(new CompletedCheckout(hd.getId(), hd.getMaDonHang(), hd.getPaymentMethod(),
                    hd.getTongTien(), isGuest, newGuestAccount, startedAsAnonymousGuest || isGuest,
                    guestEmailToRecord, hd.getGhnToDistrictId(), hd.getGhnToWardCode()));
            restoreGuestOrderAccess(context.getCompletedCheckout(), session);

            if (!isCod) {
                HoaDon hdPending = orderResult.getHoaDon();
                CheckoutExecutionSnapshot snapshot = CheckoutExecutionSnapshot.builder()
                        .orderId(hdPending.getId())
                        .maDonHang(hdPending.getMaDonHang())
                        .source(context.getSource())
                        .status(PendingCheckoutStatus.READY)
                        .customerId(accountIdForOrder)
                        .sessionId(session.getId())
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusMinutes(30))
                        .items(orderResult.getPurchasedItems())
                        .build();

                pendingCheckoutRegistry.registerSnapshot(snapshot);
            }

            long endOrder = System.currentTimeMillis();
            log.info("[GuestCheckout] Create order: {}ms - SUCCESS", (endOrder - startOrder));

            if (checkoutTk == null && accountIdForOrder != null) {
                checkoutTk = taiKhoanRepository.findById(accountIdForOrder).orElse(null);
            }
            isGuestCheckout = (checkoutTk != null && checkoutTk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST);

            TemporaryPasswordIssueResult temporaryPasswordResult = null;
            if (isCod && accountIdForOrder != null) {
                temporaryPasswordResult = temporaryPasswordService
                        .recordCodOrderCreated(accountIdForOrder, newGuestAccount);
            }

            TaiKhoan tk = (checkoutTk != null) ? checkoutTk : (accountIdForOrder != null ? taiKhoanRepository.findById(accountIdForOrder).orElse(null) : null);
            if (tk != null) {
                response.put("isGuest", isGuest);
                response.put("soLanMuaThanhCong", tk.getSoLanMuaThanhCong());

                if (isCod && temporaryPasswordResult != null && temporaryPasswordResult.isIssued()) {
                    long startEmail = System.currentTimeMillis();
                    try {
                        String appUrl = resolveBaseUrl(request);
                        temporaryPasswordService.sendTemporaryPasswordEmail(temporaryPasswordResult, appUrl);
                        long endEmail = System.currentTimeMillis();
                        log.info("[GuestCheckout] Temporary password email triggered: {}ms - SUCCESS (Asynchronous)", (endEmail - startEmail));
                    } catch (Exception e) {
                        long endEmail = System.currentTimeMillis();
                        log.error("[GuestCheckout] Temporary password email trigger: {}ms - FAILED. Exception: {}", (endEmail - startEmail), e.getMessage(), e);
                    }
                }
            } else {
                response.put("isGuest", false);
                response.put("soLanMuaThanhCong", 0);
            }

            // Trigger order confirmation email for COD orders (SePay orders trigger on payment confirmation)
            if (isCod) {
                String userEmail = null;
                if (tk != null && tk.getUsername() != null && tk.getUsername().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                    userEmail = tk.getUsername();
                } else if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null) {
                    String un = hd.getKhachHang().getTaiKhoan().getUsername();
                    if (un != null && un.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                        userEmail = un;
                    }
                } else if (email != null && !email.trim().isEmpty()) {
                    userEmail = email.trim();
                }
                if (userEmail != null && !userEmail.trim().isEmpty()) {
                    try {
                        String appUrl = resolveBaseUrl(request);
                        guestCheckoutService.sendOrderConfirmationEmail(userEmail, hd, appUrl);
                    } catch (Exception e) {
                        log.error("Failed to trigger order confirmation email", e);
                    }
                }
            }

            // Payment initialization & validation
            long startPayment = System.currentTimeMillis();
            if ("SePay".equalsIgnoreCase(phuongThucThanhToan)) {
                if (hd.getMaDonHang() == null || hd.getMaDonHang().isEmpty()
                        || hd.getTongTien() == null || hd.getTongTien().compareTo(BigDecimal.ZERO) <= 0
                        || !PaymentMethod.SEPAY.getValue().equalsIgnoreCase(hd.getPaymentMethod())) {

                    long endPayment = System.currentTimeMillis();
                    log.error("[GuestCheckout] Create payment request: {}ms - FAILED (Validation error: maDonHang={}, amount={}, method={})",
                            (endPayment - startPayment), hd.getMaDonHang(), hd.getTongTien(), hd.getPaymentMethod());

                    Map<String, Object> savedOrder = checkoutStateResponse(context, session);
                    savedOrder.put("message", "Đơn hàng đã được ghi nhận. Vui lòng mở đơn hàng để kiểm tra thanh toán.");
                    return ResponseEntity.ok(savedOrder);
                }
                long endPayment = System.currentTimeMillis();
                log.info("[GuestCheckout] Create payment request: {}ms - SUCCESS", (endPayment - startPayment));
            }

            long endPipeline = System.currentTimeMillis();
            log.info("[GuestCheckout] Guest checkout pipeline completed successfully in {}ms.", (endPipeline - startPipeline));

            response.put("trangThai", "ok");
            response.put("orderId", hd.getId());
            response.put("paymentMethod", hd.getPaymentMethod());
            response.put("tongTien", hd.getTongTien());
            response.put("maDonHang", hd.getMaDonHang());
            response.put("isGuest", isGuest);
            response.put("isNewAccount", newGuestAccount);
            response.put("ghnToDistrictId", hd.getGhnToDistrictId());
            response.put("ghnToWardCode", hd.getGhnToWardCode());
            response.put("checkoutStatus", "COMPLETED");
            response.put("redirectUrl", context.getCompletedCheckout().redirectUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            long endPipeline = System.currentTimeMillis();

            log.error("[GuestCheckout] Guest checkout pipeline failed in {}ms. Exception: {}", (endPipeline - startPipeline), e.getMessage(), e);
            if (context.getCompletedCheckout() != null) {
                return ResponseEntity.ok(checkoutStateResponse(context, session));
            }
            response.put("trangThai", "loi");
            if (isPhoneUniqueConstraintViolation(e)) {
                response.put("message", "Số điện thoại này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng số điện thoại khác.");
            } else if (e instanceof org.springframework.dao.DataIntegrityViolationException || isDatabaseException(e)) {
                response.put("message", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
            } else {
                response.put("message", e.getMessage());
            }
            return ResponseEntity.ok(response);
        } finally {
            // Only the request that acquired the lock releases it. CONSUMED is never reopened.
            context.release();
        }
    }

    @GetMapping("/checkout/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkoutStatus(
            @RequestParam("checkoutToken") String checkoutToken, HttpSession session) {
        CheckoutContext context = checkoutContextService.getContext(session, checkoutToken);
        Integer userId = (Integer) session.getAttribute("idNguoiDung");
        if (context == null || !checkoutContextService.validateOwnership(context, userId, session.getId())) {
            return ResponseEntity.ok(invalidCheckoutResponse());
        }
        return ResponseEntity.ok(checkoutStateResponse(context, session));
    }

    private Map<String, Object> invalidCheckoutResponse() {
        return new HashMap<>(Map.of("trangThai", "loi", "checkoutStatus", "EXPIRED",
                "errorCode", "CHECKOUT_EXPIRED", "retryable", false,
                "message", "Phiên thanh toán không hợp lệ hoặc đã hết hạn. Vui lòng kiểm tra đơn hàng trước khi bắt đầu thanh toán mới."));
    }

    private Map<String, Object> checkoutStateResponse(CheckoutContext context, HttpSession session) {
        CompletedCheckout saved;
        CheckoutContextStatus state;
        synchronized (context) {
            saved = context.getCompletedCheckout();
            state = context.getStatus();
        }
        Map<String, Object> result = new HashMap<>();
        if (saved != null) {
            restoreGuestOrderAccess(saved, session);
            result.put("trangThai", "ok");
            result.put("checkoutStatus", "COMPLETED");
            result.put("replayed", true);
            result.put("orderId", saved.orderId());
            result.put("maDonHang", saved.maDonHang());
            result.put("paymentMethod", saved.paymentMethod());
            result.put("tongTien", saved.tongTien());
            result.put("isGuest", saved.guest());
            result.put("isNewAccount", saved.newAccount());
            result.put("ghnToDistrictId", saved.ghnToDistrictId());
            result.put("ghnToWardCode", saved.ghnToWardCode());
            result.put("redirectUrl", saved.redirectUrl());
            result.put("message", "Đơn hàng đã được ghi nhận. Đang mở lại đơn hàng của bạn.");
        } else if (state == CheckoutContextStatus.PROCESSING) {
            result.put("trangThai", "processing");
            result.put("checkoutStatus", "PROCESSING");
            result.put("message", "Yêu cầu đặt hàng đang được xử lý. Hệ thống sẽ kiểm tra kết quả, vui lòng chờ.");
        } else if (state == CheckoutContextStatus.READY && !context.isExpired()) {
            result.put("trangThai", "ready");
            result.put("checkoutStatus", "READY");
            result.put("retryable", true);
            result.put("message", "Chưa tạo đơn hàng. Bạn có thể kiểm tra thông tin và đặt hàng lại.");
        } else if (state == CheckoutContextStatus.CONSUMED) {
            result.put("trangThai", "loi");
            result.put("checkoutStatus", "COMPLETED");
            result.put("retryable", false);
            result.put("message", "Phiên này đã được sử dụng. Vui lòng kiểm tra đơn hàng đã đặt.");
        } else {
            return invalidCheckoutResponse();
        }
        return result;
    }

    private void restoreGuestOrderAccess(CompletedCheckout saved, HttpSession session) {
        if (!saved.grantGuestAccess()) {
            return;
        }
        synchronized (session) {
            List<GuestOrderAccess> accesses = new java.util.ArrayList<>();
            Object attr = session.getAttribute("allowedGuestOrderAccesses");
            if (attr instanceof List<?> previous) {
                for (Object item : previous) {
                    if (item instanceof GuestOrderAccess access && !saved.orderId().equals(access.getOrderId())) {
                        accesses.add(access);
                    }
                }
            }
            accesses.add(new GuestOrderAccess(saved.orderId(), saved.guestEmail(),
                    java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.MINUTES)));
            session.setAttribute("allowedGuestOrderAccesses", accesses);
        }
    }

    @PostMapping("/api/voucher/apply")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyVoucher(
            @RequestParam("voucherCode") String voucherCode,
            @RequestParam(value = "checkoutToken", required = false) String checkoutToken,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");

        if (voucherCode == null || voucherCode.trim().isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Vui lòng nhập mã giảm giá.");
            return ResponseEntity.ok(response);
        }

        String uppercaseCode = voucherCode.trim().toUpperCase();

        // Calculate server-side tamTinh from CheckoutContext if token exists, otherwise fall back to DB/guest cart
        BigDecimal tamTinh = BigDecimal.ZERO;
        boolean hasItems = false;

        if (checkoutToken != null && !checkoutToken.trim().isEmpty()) {
            CheckoutContext context = checkoutContextService.getContext(session, checkoutToken.trim());
            if (context != null && context.getItems() != null && !context.getItems().isEmpty()) {
                for (CheckoutItemContext itemCtx : context.getItems()) {
                    com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(itemCtx.getIdSanPhamChiTiet()).orElse(null);
                    if (spct != null && itemCtx.getSoLuong() != null && itemCtx.getSoLuong() > 0) {
                        BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(spct);
                        tamTinh = tamTinh.add(giaBanSauGiam.multiply(new BigDecimal(itemCtx.getSoLuong())));
                        hasItems = true;
                    }
                }
            }
        }

        if (!hasItems) {
            if (idNguoiDung != null) {
                List<GioHangChiTiet> cartItems = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
                for (GioHangChiTiet item : cartItems) {
                    if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                        BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(item.getSanPhamChiTiet());
                        tamTinh = tamTinh.add(giaBanSauGiam.multiply(new BigDecimal(item.getSoLuong())));
                        hasItems = true;
                    }
                }
            } else {
                List<GuestCartService.GuestCartItem> guestCart = guestCartService.getGuestCartItems(session);
                for (GuestCartService.GuestCartItem item : guestCart) {
                    com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(item.getIdSanPhamChiTiet()).orElse(null);
                    if (spct != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                        BigDecimal giaBanSauGiam = pricingService.calculateCurrentSellingPrice(spct);
                        tamTinh = tamTinh.add(giaBanSauGiam.multiply(new BigDecimal(item.getSoLuong())));
                        hasItems = true;
                    }
                }
            }
        }

        if (!hasItems || tamTinh.compareTo(BigDecimal.ZERO) <= 0) {
            response.put("trangThai", "loi");
            response.put("message", "Giỏ hàng trống hoặc không tìm thấy sản phẩm hợp lệ để áp dụng mã giảm giá.");
            return ResponseEntity.ok(response);
        }

        Optional<PhieuGiamGia> optVoucher = phieuGiamGiaRepository.findByMaPhieu(uppercaseCode);
        if (optVoucher.isEmpty()) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá '" + uppercaseCode + "' không tồn tại.");
            return ResponseEntity.ok(response);
        }

        PhieuGiamGia voucher = optVoucher.get();

        if (!voucher.getActive()) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá này đã ngưng hoạt động.");
            return ResponseEntity.ok(response);
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getNgayBatDau())) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá chưa đến thời gian áp dụng.");
            return ResponseEntity.ok(response);
        }

        if (now.isAfter(voucher.getNgayKetThuc())) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá đã hết hạn sử dụng.");
            return ResponseEntity.ok(response);
        }

        if (voucher.getSoLuongConLai() <= 0) {
            response.put("trangThai", "loi");
            response.put("message", "Mã giảm giá đã hết lượt sử dụng.");
            return ResponseEntity.ok(response);
        }

        BigDecimal minOrder = voucher.getGiaTriDonHangToiThieu();
        if (minOrder != null && minOrder.compareTo(BigDecimal.ZERO) > 0
                && tamTinh.compareTo(minOrder) < 0) {
            java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
            response.put("trangThai", "loi");
            response.put("message", "Hạn mức tối thiểu để sử dụng mã này là " + df.format(voucher.getGiaTriDonHangToiThieu()) + " đ (Đơn của bạn: " + df.format(tamTinh) + " đ).");
            return ResponseEntity.ok(response);
        }

        BigDecimal giamGia = VoucherCalculator.calculateVoucherDiscount(tamTinh, voucher);

        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
        response.put("trangThai", "ok");
        response.put("maPhieu", voucher.getMaPhieu());
        response.put("giamGia", giamGia);
        response.put("donVi", voucher.getDonVi());
        response.put("giaTri", voucher.getGiaTri());
        response.put("giaTriGiamToiDa", voucher.getGiaTriGiamToiDa());
        response.put("giamGiaFormatted", df.format(giamGia) + " đ");
        response.put("message", "Áp dụng thành công: Giảm " + df.format(voucher.getGiaTri()) + ("%".equals(voucher.getDonVi()) ? "%" : " đ"));
        if (voucher.getGiaTriGiamToiDa() != null) {
            response.put("message", "Áp dụng thành công: Giảm " + df.format(voucher.getGiaTri()) + "%" + " (Tối đa " + df.format(voucher.getGiaTriGiamToiDa()) + " đ)");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/api/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkEmail(@RequestParam("email") String email, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        String status = guestCheckoutService.checkEmailStatus(email);
        if ("GUEST_NO_PASSWORD".equals(status) || "GUEST_WITH_TEMP_PASSWORD".equals(status)) {
            if (email != null && !email.trim().isEmpty()) {
                session.setAttribute("guestCheckoutEmail", email.trim());
            }
        }
        response.put("status", status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout/api/verify-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "checkoutToken", required = false) String checkoutToken,
            HttpSession session,
            HttpServletRequest request, HttpServletResponse httpResponse) {
        Map<String, Object> response = new HashMap<>();
        try {
            com.smashvn.shop.entity.TaiKhoan tk = userDangNhapService.kiemTraDangNhap(email, password);
            if (!"KH".equals(tk.getVaiTro())) {
                throw new IllegalArgumentException("Tài khoản này không được phép đăng nhập tại trang khách hàng.");
            }

            HttpSession oldSession = session;
            LoginSession.establish(request, httpResponse, tk);
            HttpSession newSession = request.getSession(true);

            if (tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST) {
                newSession.setAttribute("pendingPasswordSetupAccountId", tk.getId());
                newSession.setAttribute("temporaryPasswordVerified", true);
                newSession.setAttribute("isGuestView", true);
                newSession.setAttribute("guestCheckoutEmail", tk.getUsername());
                newSession.setAttribute("nguoiDungDangNhap", tk.getUsername());
                newSession.setAttribute("idNguoiDung", tk.getId());
                newSession.setAttribute("vaiTro", "KH");
                newSession.removeAttribute("activeRole");

                response.put("redirectUrl", "/user/setup-password");
                response.put("trangThai", "require_password_setup");
                response.put("authenticated", false);
                response.put("requiresPasswordSetup", true);
                response.put("success", true);
                return ResponseEntity.ok(response);
            }


            newSession.setAttribute("nguoiDungDangNhap", tk.getUsername());
            newSession.setAttribute("idNguoiDung", tk.getId());
            newSession.setAttribute("vaiTro", "KH");
            newSession.setAttribute("activeRole", "KH");


            com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
            if (kh != null) {
                newSession.setAttribute("tenHienThi", kh.getHoKh() + " " + kh.getTenKh());
                guestCartService.transferGuestCartToDb(newSession, kh.getId());
            } else {
                newSession.setAttribute("tenHienThi", "Khách hàng");
            }

            if (checkoutToken != null && !checkoutToken.isBlank()) {
                checkoutContextService.promoteGuestContextToAuthenticatedUser(
                        checkoutToken, oldSession, newSession, tk.getId());
                response.put("redirectUrl", "/checkout?token=" + java.net.URLEncoder.encode(checkoutToken.trim(), java.nio.charset.StandardCharsets.UTF_8));
            } else {
                response.put("redirectUrl", "/checkout");
            }

            response.put("trangThai", "ok");
            response.put("authenticated", true);
            response.put("reloadAddresses", true);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("trangThai", "loi");
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/checkout/api/set-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> setPassword() {
        Map<String, Object> response = new HashMap<>();
        response.put("trangThai", "loi");
        response.put("success", false);
        response.put("message", "Tính năng đặt mật khẩu trực tiếp tại checkout đã bị vô hiệu hóa. Mật khẩu ban đầu chỉ được thiết lập qua đường link gửi về email của bạn.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/setup-password")
    public String viewSetupPassword(HttpSession session, Model model) {
        Integer accountId = pendingPasswordSetupAccountId(session);
        if (accountId == null) {
            return "redirect:/user/dang-nhap";
        }

        TaiKhoan account = taiKhoanRepository.findById(accountId).orElse(null);
        if (account == null
                || account.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.GUEST
                || account.getMatKhau() == null
                || account.getMatKhau().isBlank()) {
            clearTemporaryPasswordVerification(session);
            return "redirect:/user/dang-nhap";
        }

        model.addAttribute("guestEmail", account.getUsername());
        return "setup-password";
    }

    @PostMapping("/user/setup-password")
    public String submitSetupPassword(
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            HttpServletRequest request,
            HttpSession session,
            Model model, HttpServletResponse httpResponse) {
        Integer accountId = pendingPasswordSetupAccountId(session);
        if (accountId == null) {
            return "redirect:/user/dang-nhap";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("loi", "Mật khẩu xác nhận không trùng khớp!");
            TaiKhoan account = taiKhoanRepository.findById(accountId).orElse(null);
            model.addAttribute("guestEmail", account != null ? account.getUsername() : "");
            return "setup-password";
        }

        try {
            guestCheckoutService.setPasswordForGuest(accountId, newPassword);

            TaiKhoan activeAccount = taiKhoanRepository.findById(accountId)
                    .orElseThrow(() -> new IllegalStateException("Tài khoản không tồn tại"));
            LoginSession.establish(request, httpResponse, activeAccount);
            HttpSession memberSession = request.getSession(true);

            memberSession.setAttribute("nguoiDungDangNhap", activeAccount.getUsername());
            memberSession.setAttribute("idNguoiDung", activeAccount.getId());
            memberSession.setAttribute("vaiTro", activeAccount.getVaiTro());
            memberSession.setAttribute("activeRole", "KH");

            com.smashvn.shop.entity.KhachHang customer =
                    khachHangRepository.findByTaiKhoan_Id(activeAccount.getId());
            memberSession.setAttribute("tenHienThi", customer != null
                    ? customer.getHoKh() + " " + customer.getTenKh()
                    : "Khách hàng");
            return "redirect:/user/dashboard";
        } catch (Exception exception) {
            model.addAttribute("loi", exception.getMessage());
            TaiKhoan account = taiKhoanRepository.findById(accountId).orElse(null);
            model.addAttribute("guestEmail", account != null ? account.getUsername() : "");
            return "setup-password";
        }
    }

    private Integer pendingPasswordSetupAccountId(HttpSession session) {
        if (session == null || !Boolean.TRUE.equals(session.getAttribute("temporaryPasswordVerified"))) {
            return null;
        }
        Object accountId = session.getAttribute("pendingPasswordSetupAccountId");
        return accountId instanceof Integer ? (Integer) accountId : null;
    }

    private void clearTemporaryPasswordVerification(HttpSession session) {
        if (session != null) {
            session.removeAttribute("temporaryPasswordVerified");
            session.removeAttribute("pendingPasswordSetupAccountId");
        }
    }


    @GetMapping("/user/thiet-lap-mat-khau")
    public String viewThietLapMatKhau(
            @RequestParam(value = "token", required = false) String token,
            HttpSession session,
            Model model) {
        if (token == null || token.trim().isEmpty()) {
            Integer idNguoiDung = (session != null) ? (Integer) session.getAttribute("idNguoiDung") : null;
            boolean isGuest = (session != null) && Boolean.TRUE.equals(session.getAttribute("isGuestView"));
            if (idNguoiDung != null && isGuest) {
                TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
                if (tk != null && tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST) {
                    model.addAttribute("guestEmail", tk.getUsername());
                    model.addAttribute("token", null);
                    return "set-password-by-token";
                }
            }
            return "redirect:/user/dang-nhap";
        }
        try {
            com.smashvn.shop.entity.TokenKhoiPhuc tkp = tokenRepository.findByMaXacNhan(token);
            if (tkp == null || !"GUEST_ACTIVATION".equals(tkp.getLoaiXacNhan()) || tkp.isDaSuDung() || tkp.getThoiGianHetHan().isBefore(java.time.LocalDateTime.now())) {
                model.addAttribute("loi", "Đường link thiết lập mật khẩu không hợp lệ hoặc đã hết hạn!");
                return "signin";
            }
            TaiKhoan tk = tkp.getTaiKhoan();
            if (tk == null || tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.LOCKED || tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.PENDING_LOCK) {
                model.addAttribute("loi", "Tài khoản liên kết với đường link này đã bị khóa!");
                return "signin";
            }
            if (tk.getTrangThaiTaiKhoan() != com.smashvn.shop.entity.AccountStatus.GUEST) {
                model.addAttribute("loi", "Tài khoản đã được kích hoạt trước đó!");
                return "signin";
            }
            model.addAttribute("token", token);
            return "set-password-by-token";
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            return "signin";
        }
    }

    @PostMapping("/user/thiet-lap-mat-khau/gui-lai")
    public String resendThietLapMatKhau(
            HttpSession session,
            jakarta.servlet.http.HttpServletRequest request,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Integer idNguoiDung = (session != null) ? (Integer) session.getAttribute("idNguoiDung") : null;
        boolean isGuest = (session != null) && Boolean.TRUE.equals(session.getAttribute("isGuestView"));

        if (idNguoiDung == null || !isGuest) {
            return "redirect:/user/dang-nhap";
        }

        try {
            String appUrl = resolveBaseUrl(request);
            guestCheckoutService.resendGuestActivationEmail(idNguoiDung, appUrl);
            redirectAttributes.addFlashAttribute("thongBaoGuiLai", "Hệ thống đã gửi lại email kích hoạt mới! Vui lòng kiểm tra hộp thư của bạn.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("loi", e.getMessage());
        }

        return "redirect:/user/thiet-lap-mat-khau";
    }

    @PostMapping("/user/thiet-lap-mat-khau")
    public String submitThietLapMatKhau(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("loi", "Mật khẩu xác nhận không trùng khớp!");
            model.addAttribute("token", token);
            return "set-password-by-token";
        }

        try {
            guestCheckoutService.setPasswordByToken(token, password);
            return "redirect:/user/dang-nhap?thanhcong=" + java.net.URLEncoder.encode("Thiết lập mật khẩu thành công! Vui lòng đăng nhập.", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            model.addAttribute("loi", e.getMessage());
            model.addAttribute("token", token);
            return "set-password-by-token";
        }
    }

    public static class GuestOrderAccess implements java.io.Serializable {

        private static final long serialVersionUID = 2L;
        private final Integer orderId;
        private final String guestEmail;
        private final java.time.Instant expiresAt;

        public GuestOrderAccess(Integer orderId, java.time.Instant expiresAt) {
            this(orderId, null, expiresAt);
        }

        public GuestOrderAccess(Integer orderId, String guestEmail, java.time.Instant expiresAt) {
            this.orderId = orderId;
            this.guestEmail = guestEmail;
            this.expiresAt = expiresAt;
        }

        public Integer getOrderId() {
            return orderId;
        }

        public String getGuestEmail() {
            return guestEmail;
        }

        public java.time.Instant getExpiresAt() {
            return expiresAt;
        }

        public boolean isExpired() {
            return java.time.Instant.now().isAfter(expiresAt);
        }
    }

    private boolean isPhoneUniqueConstraintViolation(Throwable e) {
        if (e == null) {
            return false;
        }
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && msg.contains("UX_KhachHang_SoDienThoaiKh")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isDatabaseException(Throwable e) {
        if (e == null) {
            return false;
        }
        Throwable current = e;
        while (current != null) {
            if (current instanceof org.springframework.dao.DataAccessException
                    || current instanceof java.sql.SQLException
                    || current instanceof org.hibernate.JDBCException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return org.jsoup.Jsoup.clean(input, org.jsoup.safety.Safelist.none()).trim();
    }
}
