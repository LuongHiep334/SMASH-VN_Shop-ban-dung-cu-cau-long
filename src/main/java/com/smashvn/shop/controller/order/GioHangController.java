package com.smashvn.shop.controller.order;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.smashvn.shop.dto.cart.CartItemView;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.repository.GioHangChiTietRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.order.GuestCartService;
import com.smashvn.shop.service.product.PricingService;
import com.smashvn.shop.service.product.ProductAvailabilityService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/gio-hang")
@RequiredArgsConstructor
public class GioHangController {

    private final GioHangService gioHangService;
    private final PricingService pricingService;
    private final GuestCartService guestCartService;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final com.smashvn.shop.service.order.CheckoutContextService checkoutContextService;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final ProductAvailabilityService productAvailabilityService;

    // HÀM 1: THÊM VÀO GIỎ (Dùng cho AJAX)
    @PostMapping("/them")
    @ResponseBody
    public ResponseEntity<?> xuLyThemVaoGio(
            @RequestParam(value = "idSanPhamChiTiet", required = false) Integer idSanPhamChiTiet,
            @RequestParam(value = "soLuong", required = false) Integer soLuong,
            HttpSession session) {

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(session, idNguoiDung);

        if (soLuong == null) {
            return ResponseEntity.status(400).body("Số lượng sản phẩm không được để trống.");
        }
        if (idSanPhamChiTiet == null) {
            return ResponseEntity.status(400).body("Sản phẩm không hợp lệ.");
        }

        if (!activeAccount) {
            try {
                guestCartService.addToGuestCart(session, idSanPhamChiTiet, soLuong);
                com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(idSanPhamChiTiet)
                        .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
                Map<String, Object> data = new HashMap<>();
                data.put("trangThai", "ok");
                data.put("tenSanPham", spct.getSanPham().getTenSanPham());
                String sizeOrWeight = spct.getTrongLuong() != null && !spct.getTrongLuong().isBlank() ? spct.getTrongLuong() : (spct.getKichThuoc() != null ? spct.getKichThuoc() : "");
                String phanLoaiStr = (spct.getMauSac() != null ? spct.getMauSac() : "") + (!sizeOrWeight.isEmpty() ? " | " + sizeOrWeight : "");
                data.put("phanLoai", phanLoaiStr);
                data.put("giaBan", pricingService.calculateCurrentSellingPrice(spct));
                data.put("hinhAnh", spct.getHinhAnhSanPham());
                data.put("soLuongThem", soLuong);

                // Create Quick Add CheckoutContext backend token
                com.smashvn.shop.dto.order.CheckoutContext quickAddCtx = checkoutContextService.createQuickAddContext(session, null, idSanPhamChiTiet, soLuong, null);
                data.put("checkoutToken", quickAddCtx.getToken());
                data.put("quickAddCheckoutUrl", "/checkout?token=" + quickAddCtx.getToken());

                return ResponseEntity.ok(data);
            } catch (RuntimeException e) {
                return ResponseEntity.status(400).body(e.getMessage());
            }
        }

        try {
            // Service xử lý và trả về luôn dữ liệu hiển thị Modal
            Map<String, Object> data = gioHangService.themVaoGio(idNguoiDung, idSanPhamChiTiet, soLuong);
            data.put("trangThai", "ok");

            Integer cartItemId = (Integer) data.get("cartItemId");
            com.smashvn.shop.dto.order.CheckoutContext quickAddCtx = checkoutContextService.createQuickAddContext(session, idNguoiDung, idSanPhamChiTiet, soLuong, cartItemId);
            data.put("checkoutToken", quickAddCtx.getToken());
            data.put("quickAddCheckoutUrl", "/checkout?token=" + quickAddCtx.getToken());

            return ResponseEntity.ok(data);

        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    // HÀM 2: LẤY DỮ LIỆU MINI CART (Dùng cho AJAX Header)
    @GetMapping("/api/mini-cart")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> layDuLieuMiniCart(HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(session, idNguoiDung);

        if (!activeAccount) {
            Map<String, Object> response = guestCartService.layDuLieuMiniCart(session);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = gioHangService.layDuLieuMiniCart(idNguoiDung);
        return ResponseEntity.ok(response);
    }

    // HÀM 3: HIỂN THỊ TRANG GIỎ HÀNG (cart.html)
    @GetMapping
    public String hienThiGioHang(HttpSession session, Model model) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(session, idNguoiDung);

        List<CartItemView> danhSachCartView = new java.util.ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO;

        if (!activeAccount) {
            List<GuestCartService.GuestCartItem> guestItems = guestCartService.getGuestCartItems(session);
            for (GuestCartService.GuestCartItem item : guestItems) {
                com.smashvn.shop.entity.SanPhamChiTiet spct = sanPhamChiTietRepository.findById(item.getIdSanPhamChiTiet()).orElse(null);
                CartItemView view = buildCartItemView(item.getIdSanPhamChiTiet(), spct, item.getSoLuong(), true);
                if (view.isHopLe() && view.getThanhTien() != null) {
                    tongTien = tongTien.add(view.getThanhTien());
                }
                danhSachCartView.add(view);
            }
        } else {
            List<GioHangChiTiet> dbCartItems = gioHangService.layDanhSachSanPhamTrongGio(idNguoiDung);
            for (GioHangChiTiet item : dbCartItems) {
                CartItemView view = buildCartItemView(item.getId(), item.getSanPhamChiTiet(), item.getSoLuong(), false);
                if (view.isHopLe() && view.getThanhTien() != null) {
                    tongTien = tongTien.add(view.getThanhTien());
                }
                danhSachCartView.add(view);
            }
        }

        model.addAttribute("danhSachCart", danhSachCartView);
        model.addAttribute("tongTien", tongTien);
        return "cart";
    }

    private CartItemView buildCartItemView(Integer cartItemId, com.smashvn.shop.entity.SanPhamChiTiet spct, Integer soLuong, boolean isGuest) {
        if (spct == null) {
            return CartItemView.builder()
                    .cartItemId(cartItemId)
                    .soLuong(soLuong)
                    .hopLe(false)
                    .guestItem(isGuest)
                    .build();
        }

        com.smashvn.shop.entity.SanPham sp = spct.getSanPham();
        String tenSp = sp != null ? sp.getTenSanPham() : "Sản phẩm";
        String anhSp = spct.getHinhAnhUrl() != null && !spct.getHinhAnhUrl().isBlank() ? spct.getHinhAnhUrl() : spct.getHinhAnhSanPham();
        String danhMuc = (sp != null && sp.getDanhMuc() != null) ? sp.getDanhMuc().getTenDanhMuc() : "";

        List<String> thuocTinhList = new java.util.ArrayList<>();
        if (spct.getSanPhamChiTietThuocTinhs() != null) {
            for (com.smashvn.shop.entity.SanPhamChiTietThuocTinh tt : spct.getSanPhamChiTietThuocTinhs()) {
                if (tt.getThuocTinh() != null && tt.getGiaTri() != null) {
                    thuocTinhList.add(tt.getThuocTinh().getTenThuocTinh() + ": " + tt.getGiaTri());
                }
            }
        }
        if (thuocTinhList.isEmpty()) {
            String sizeOrWeight = spct.getTrongLuong() != null && !spct.getTrongLuong().isBlank() ? spct.getTrongLuong() : (spct.getKichThuoc() != null ? spct.getKichThuoc() : "");
            if (spct.getMauSac() != null || !sizeOrWeight.isEmpty()) {
                String str = (spct.getMauSac() != null ? spct.getMauSac() : "") + (!sizeOrWeight.isEmpty() ? " | " + sizeOrWeight : "");
                thuocTinhList.add(str);
            }
        }

        int tonKho = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
        boolean hetHang = tonKho <= 0;
        boolean ngungBan = !productAvailabilityService.isVariantPublished(spct);
        boolean hopLe = !hetHang && !ngungBan && soLuong != null && soLuong > 0;

        BigDecimal donGia = pricingService.calculateCurrentSellingPrice(spct);
        if (donGia == null) {
            donGia = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;
        }
        BigDecimal thanhTien = donGia.multiply(new BigDecimal(soLuong != null ? soLuong : 0));

        return CartItemView.builder()
                .cartItemId(cartItemId)
                .idSanPhamChiTiet(spct.getId())
                .sanPhamId(sp != null ? sp.getId() : null)
                .tenSanPham(tenSp)
                .anhSanPham(anhSp)
                .danhMuc(danhMuc)
                .thuocTinh(thuocTinhList)
                .soLuong(soLuong)
                .soLuongTon(tonKho)
                .donGia(donGia)
                .thanhTien(thanhTien)
                .hetHang(hetHang)
                .ngungBan(ngungBan)
                .hopLe(hopLe)
                .guestItem(isGuest)
                .build();
    }

    // HÀM 4 MỚI: XÓA SẢN PHẨM BẰNG AJAX
    @PostMapping("/api/xoa/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> xoaSanPhamAjax(@PathVariable("id") Integer idChiTiet, HttpSession session) {
        Map<String, String> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(session, idNguoiDung);

        try {
            if (!activeAccount) {
                guestCartService.removeFromGuestCart(session, idChiTiet);
            } else {
                gioHangService.xoaSanPhamKhoiGio(idChiTiet, idNguoiDung);
            }
            response.put("trangThai", "ok");
        } catch (Exception e) {
            response.put("trangThai", "loi");
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // HÀM 4.5 MỚI: XÓA NHIỀU SẢN PHẨM BẰNG AJAX
    @PostMapping("/api/xoa-nhieu")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> xoaNhieuSanPhamAjax(
            @RequestParam(value = "selectedItemIds", required = false) List<Integer> selectedItemIds,
            HttpSession session) {

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(session, idNguoiDung);

        try {
            Map<String, Object> result;
            if (!activeAccount) {
                result = guestCartService.xoaNhieuKhoiGuestCart(session, selectedItemIds);
            } else {
                result = gioHangService.xoaNhieuSanPhamKhoiGio(selectedItemIds, idNguoiDung);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errResp = new HashMap<>();
            errResp.put("trangThai", "error");
            errResp.put("thongBao", "Không thể xóa các sản phẩm đã chọn");
            return ResponseEntity.ok(errResp);
        }
    }

    // HÀM 5: CẬP NHẬT SỐ LƯỢNG (Dùng cho AJAX trong cart.html)
    @PostMapping("/cap-nhat")
    @ResponseBody
    public ResponseEntity<?> capNhatSoLuong(
            @RequestParam(value = "idChiTiet", required = false) Integer idChiTiet,
            @RequestParam(value = "soLuong", required = false) Integer soLuong,
            HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        boolean activeAccount = isActiveAccount(session, idNguoiDung);

        if (soLuong == null) {
            return ResponseEntity.status(400).body("Số lượng sản phẩm không được để trống.");
        }
        if (idChiTiet == null) {
            return ResponseEntity.status(400).body("Chi tiết giỏ hàng không hợp lệ.");
        }

        try {
            com.smashvn.shop.entity.SanPhamChiTiet spct = null;
            Integer updatedQty = soLuong;
            Integer cartItemId = idChiTiet;

            if (!activeAccount) {
                guestCartService.updateGuestCartQuantity(session, idChiTiet, soLuong);
                spct = sanPhamChiTietRepository.findById(idChiTiet).orElse(null);
            } else {
                gioHangService.capNhatSoLuong(idChiTiet, soLuong, idNguoiDung);
                GioHangChiTiet item = gioHangChiTietRepository.findById(idChiTiet).orElse(null);
                if (item != null) {
                    spct = item.getSanPhamChiTiet();
                    updatedQty = item.getSoLuong();
                }
            }

            BigDecimal unitPrice = pricingService.calculateCurrentSellingPrice(spct);
            if (unitPrice == null && spct != null) {
                unitPrice = spct.getGiaBan();
            }
            if (unitPrice == null) unitPrice = BigDecimal.ZERO;

            BigDecimal lineTotal = unitPrice.multiply(new BigDecimal(updatedQty != null ? updatedQty : 0));

            Map<String, Object> resp = new HashMap<>();
            resp.put("trangThai", "ok");
            resp.put("cartItemId", cartItemId);
            resp.put("quantity", updatedQty);
            resp.put("unitPrice", unitPrice);
            resp.put("lineTotal", lineTotal);
            resp.put("stockQuantity", spct != null && spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 999);

            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    private boolean isActiveAccount(HttpSession session, Integer idNguoiDung) {
        if (session != null && Boolean.TRUE.equals(session.getAttribute("isGuestView"))) {
            return false;
        }
        if (idNguoiDung == null) {
            return false;
        }
        com.smashvn.shop.entity.TaiKhoan tk = taiKhoanRepository.findById(idNguoiDung).orElse(null);
        return tk != null
                && tk.getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.ACTIVE
                && "hoat_dong".equalsIgnoreCase(tk.getTrangThai());
    }
}

