package com.smashvn.shop.controller.admin;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dto.user.PosCustomerResponse;
import com.smashvn.shop.dto.user.PosRegisterCustomerRequest;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.admin.AdminPosService;
import com.smashvn.shop.service.product.PriceSnapshot;
import com.smashvn.shop.service.product.PricingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/pos")
@RequiredArgsConstructor
public class AdminPosController {

    private final AdminPosService adminPosService;
    private final TaiKhoanRepository taiKhoanRepository;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final com.smashvn.shop.repository.DanhMucRepository danhMucRepository;
    private final com.smashvn.shop.repository.ThuongHieuRepository thuongHieuRepository;
    private final PricingService pricingService;
    private final SepayConfig sepayConfig;
    private final com.smashvn.shop.service.order.OrderViewService orderViewService;

    // ─── Trang chính POS ────────────────────────────────────────────────────────
    @GetMapping
    public String viewPos(Model model, HttpSession session) {
        model.addAttribute("categories", danhMucRepository.findAll());
        model.addAttribute("brands", thuongHieuRepository.findAll());
        // Thông tin ngân hàng SePay để hiển thị trong modal chuyển khoản POS
        model.addAttribute("sepayBankAccount", sepayConfig.getBankAccount());
        model.addAttribute("sepayBankName", sepayConfig.getBankName());
        model.addAttribute("sepayMemoPrefix", sepayConfig.getMemoPrefix());
        return "admin/pos";
    }

    // ─── API tìm kiếm sản phẩm ──────────────────────────────────────────────────
    @GetMapping("/search-products")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchProducts(
            @RequestParam(value = "q", required = false) String query,
            @RequestParam(value = "danhMucId", required = false) Integer danhMucId,
            @RequestParam(value = "thuongHieuId", required = false) Integer thuongHieuId) {

        List<Map<String, Object>> results = adminPosService.searchActiveVariants(query, danhMucId, thuongHieuId).stream().map(v -> {
            Map<String, Object> map = new HashMap<>();
            // Dùng PriceSnapshot duy nhất để lấy giá thực sau DotGiamGia
            PriceSnapshot snap;
            try {
                snap = pricingService.buildPriceSnapshot(v);
            } catch (Exception ex) {
                BigDecimal giaGoc = v.getGiaBan() != null ? v.getGiaBan() : BigDecimal.ZERO;
                snap = new PriceSnapshot(giaGoc, giaGoc, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
            }
            map.put("id", v.getId());
            map.put("sku", v.getSku() != null ? v.getSku() : "");
            map.put("maSanPham", v.getSanPham() != null && v.getSanPham().getMaSanPham() != null ? v.getSanPham().getMaSanPham() : "");
            map.put("tenSanPham", v.getSanPham() != null && v.getSanPham().getTenSanPham() != null ? v.getSanPham().getTenSanPham() : "Sản phẩm");
            map.put("phanLoai", v.getPhanLoaiHienThi());
            map.put("mauSac", v.getMauSac() != null ? v.getMauSac() : "");
            map.put("kichThuoc", v.getKichThuoc() != null ? v.getKichThuoc() : "");
            map.put("trongLuong", v.getTrongLuong() != null ? v.getTrongLuong() : "");
            map.put("mucCang", v.getMucCang() != null ? v.getMucCang() : "");
            // Giá bán thực sau khi áp dụng đợt giảm giá (nếu có)
            map.put("giaBan", snap.giaBanSauGiam() != null ? snap.giaBanSauGiam() : BigDecimal.ZERO);
            // Giá niêm yết gốc để gạch ngang trên UI
            map.put("giaNiemYet", snap.giaNiemYet() != null ? snap.giaNiemYet() : BigDecimal.ZERO);
            // % giảm (0 nếu không có đợt giảm)
            map.put("phanTramGiam", snap.phanTramGiam() != null ? snap.phanTramGiam() : BigDecimal.ZERO);
            // Tên chiến dịch (null nếu không có)
            map.put("tenDotGiamGia", snap.tenDotGiamGia());
            map.put("soLuongTon", v.getSoLuongTon() != null ? v.getSoLuongTon() : 0);
            map.put("hinhAnh", v.getHinhAnhSanPham() != null ? v.getHinhAnhSanPham() : "product9.jpg");
            return map;
        }).toList();
        return ResponseEntity.ok(results);
    }

    // ─── API tìm kiếm khách hàng ────────────────────────────────────────────────
    @GetMapping("/search-customers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> searchCustomers(@RequestParam(value = "q", required = false) String query) {
        List<Map<String, Object>> results = adminPosService.searchCustomers(query).stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            String ho = c.getHoKh() != null ? c.getHoKh() : "";
            String ten = c.getTenKh() != null ? c.getTenKh() : "";
            map.put("hoTen", (ho + " " + ten).trim());
            map.put("sdt", c.getSoDienThoaiKh() != null ? c.getSoDienThoaiKh() : "");
            map.put("email", c.getTaiKhoan() != null && c.getTaiKhoan().getUsername() != null ? c.getTaiKhoan().getUsername() : "");
            return map;
        }).toList();
        return ResponseEntity.ok(results);
    }

    // ─── API kiểm tra voucher ────────────────────────────────────────────────────
    @GetMapping("/check-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkVoucher(@RequestParam("code") String code, @RequestParam("total") BigDecimal total) {
        Map<String, Object> response = new HashMap<>();
        try {
            var voucher = adminPosService.checkVoucher(code, total);
            if (voucher != null) {
                response.put("success", true);
                response.put("maPhieu", voucher.getMaPhieu());
                response.put("giaTri", voucher.getGiaTri());
                response.put("donVi", voucher.getDonVi());
                response.put("giaTriDonHangToiThieu", voucher.getGiaTriDonHangToiThieu());
                response.put("giaTriGiamToiDa", voucher.getGiaTriGiamToiDa());
            } else {
                response.put("success", false);
                response.put("message", "Voucher không hợp lệ hoặc đã hết hạn.");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    // ─── DTO request checkout ────────────────────────────────────────────────────
    public static class PosCheckoutRequest {

        public Integer idKhachHang;
        public String maVoucher;
        public List<AdminPosService.PosItem> items;
        /**
         * TIEN_MAT | CHUYEN_KHOAN
         */
        public String phuongThucPos;
        public String maGiaoDich;
        public String ghiChu;
    }

    // ─── Xử lý thanh toán POS ────────────────────────────────────────────────────
    @PostMapping("/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody PosCheckoutRequest req,
            HttpServletRequest request,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        // Xác định nhân viên đang thao tác
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            org.springframework.security.core.Authentication auth
                    = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                TaiKhoan tk = taiKhoanRepository.findByUsername(auth.getName());
                if (tk != null) {
                    idNguoiDung = tk.getId();
                    session.setAttribute("idNguoiDung", idNguoiDung);
                    session.setAttribute("vaiTro", tk.getVaiTro());
                }
            }
        }

        if (idNguoiDung == null) {
            response.put("success", false);
            response.put("message", "Phiên làm việc đã hết hạn hoặc chưa đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        // Chống trùng lặp submit (Double Submit Protection)
        synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute("pos_processing"))) {
                response.put("success", false);
                response.put("message", "Hệ thống đang xử lý giao dịch trước đó. Vui lòng không click liên tục!");
                return ResponseEntity.badRequest().body(response);
            }
            session.setAttribute("pos_processing", true);
        }

        String ipAddress = request.getRemoteAddr();

        // Validate request payload
        if (req.items == null || req.items.isEmpty()) {
            response.put("success", false);
            response.put("message", "Đơn hàng không có sản phẩm nào!");
            return ResponseEntity.badRequest().body(response);
        }
        for (com.smashvn.shop.service.admin.AdminPosService.PosItem item : req.items) {
            if (item.idSanPhamChiTiet == null) {
                response.put("success", false);
                response.put("message", "ID sản phẩm chi tiết không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }
            if (item.soLuong == null || item.soLuong <= 0) {
                response.put("success", false);
                response.put("message", "Số lượng sản phẩm không hợp lệ!");
                return ResponseEntity.badRequest().body(response);
            }
        }
        if (req.phuongThucPos == null || (!"TIEN_MAT".equalsIgnoreCase(req.phuongThucPos) && !"CHUYEN_KHOAN".equalsIgnoreCase(req.phuongThucPos))) {
            response.put("success", false);
            response.put("message", "Phương thức thanh toán POS không hợp lệ!");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            HoaDon hd = adminPosService.thanhToanPos(
                    req.idKhachHang,
                    req.maVoucher,
                    req.items,
                    req.phuongThucPos,
                    req.maGiaoDich,
                    req.ghiChu,
                    idNguoiDung,
                    ipAddress
            );

            response.put("success", true);
            boolean isPending = "CHUYEN_KHOAN".equalsIgnoreCase(req.phuongThucPos);
            response.put("message", isPending ? "Đã khởi tạo đơn hàng chờ thanh toán!" : "Thanh toán thành công!");
            response.put("hoaDonId", hd.getId());
            response.put("maHoaDon", hd.getMaDonHang());
            response.put("paymentMethod", req.phuongThucPos);
            response.put("tongTien", hd.getTongTien());
            response.put("isPendingPayment", isPending);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } finally {
            session.removeAttribute("pos_processing");
        }
    }

    // ─── In hóa đơn nhiệt ────────────────────────────────────────────────────────
    @GetMapping("/print/{id}")
    public String printInvoice(@PathVariable("id") Integer id, Model model) {
        HoaDon hd = hoaDonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + id));
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(id);

        BigDecimal tongTienTruocGiam = items.stream()
                .map(item -> item.getDonGia().multiply(new BigDecimal(item.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal tienGiam = tongTienTruocGiam.subtract(hd.getTongTien());
        if (tienGiam.compareTo(BigDecimal.ZERO) < 0) {
            tienGiam = BigDecimal.ZERO;
        }

        // Label phương thức thanh toán
        String phuongThucLabel = hd.getPhuongThucThanhToan() != null
                ? hd.getPhuongThucThanhToan().getTenPhuongThuc()
                : "Tiền mặt";

        var paymentInfo = orderViewService.getPaymentStatusInfo(hd.getTrangThaiThanhToan());
        String trangThaiLabel = paymentInfo.label();

        if (hd.getMaVoucherApDung() == null || hd.getMaVoucherApDung().isEmpty()) {
            if (hd.getPhieuGiamGia() != null) {
                hd.setMaVoucherApDung(hd.getPhieuGiamGia().getMaPhieu());
            } else if (hd.getSoTienGiamVoucher().compareTo(BigDecimal.ZERO) > 0) {
                hd.setMaVoucherApDung("Voucher");
            } else {
                hd.setMaVoucherApDung("Không áp dụng voucher");
            }
        }

        if (hd.getThoiGianXacNhan() == null && hd.getPaidAt() != null) {
            hd.setThoiGianXacNhan(hd.getPaidAt());
        }
        if (hd.getNguoiXacNhanThanhToan() == null && hd.getNhanVien() != null) {
            hd.setNguoiXacNhanThanhToan(hd.getNhanVien().getHoTenNv());
        }

        model.addAttribute("hoaDon", hd);
        model.addAttribute("maHoaDon", hd.getMaDonHang());
        model.addAttribute("items", items);
        model.addAttribute("tongTienTruocGiam", tongTienTruocGiam);
        model.addAttribute("tienGiam", tienGiam);
        model.addAttribute("phuongThucLabel", phuongThucLabel);
        model.addAttribute("trangThaiLabel", trangThaiLabel);
        return "admin/pos-print";
    }

    @PostMapping("/confirm-payment/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> confirmPayment(@PathVariable("id") Integer id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            response.put("success", false);
            response.put("message", "Phiên làm việc hết hạn.");
            return ResponseEntity.status(401).body(response);
        }
        try {
            adminPosService.confirmPaymentPos(id, idNguoiDung);
            response.put("success", true);
            response.put("message", "Xác nhận thanh toán thủ công thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/confirm-payment-ui")
    public String confirmPaymentUi(
            @RequestParam("idHoaDon") Integer idHoaDon,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return "redirect:/admin/dang-nhap";
        }
        try {
            adminPosService.confirmPaymentPos(idHoaDon, idNguoiDung);
            redirectAttributes.addFlashAttribute("successMsg", "Xác nhận thanh toán thành công cho đơn POS #" + idHoaDon + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Lỗi xác nhận thanh toán: " + e.getMessage());
        }
        return "redirect:/admin/don-hang";
    }

    @PostMapping("/cancel-order/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable("id") Integer id, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            response.put("success", false);
            response.put("message", "Phiên làm việc hết hạn.");
            return ResponseEntity.status(401).body(response);
        }
        try {
            adminPosService.cancelOrderPos(id, idNguoiDung);
            response.put("success", true);
            response.put("message", "Hủy hóa đơn thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/customers/register")
    @ResponseBody
    public ResponseEntity<PosCustomerResponse> registerCustomer(
            @jakarta.validation.Valid @RequestBody PosRegisterCustomerRequest req,
            org.springframework.validation.BindingResult result,
            HttpSession session) {

        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.status(401).body(PosCustomerResponse.builder()
                    .success(false)
                    .message("Phiên làm việc hết hạn hoặc chưa đăng nhập.")
                    .build());
        }

        if (result.hasErrors()) {
            String errorMsg = result.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(PosCustomerResponse.builder()
                    .success(false)
                    .message(errorMsg)
                    .build());
        }

        try {
            PosCustomerResponse resp = adminPosService.registerCustomerAtPos(req);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(PosCustomerResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}
