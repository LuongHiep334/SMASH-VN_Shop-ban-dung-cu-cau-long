package com.smashvn.shop.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smashvn.shop.dto.inventory.KhoSanPhamLoiDetailView;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.admin.AdminKhuyenMaiService;
import com.smashvn.shop.service.inventory.InventoryLotService;
import com.smashvn.shop.service.order.OrderViewService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final TaiKhoanRepository taiKhoanRepository;
    private final SanPhamRepository sanPhamRepository;
    private final HoaDonRepository hoaDonRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final AdminKhuyenMaiService adminKhuyenMaiService;
    private final OrderViewService orderViewService;
    private final com.smashvn.shop.repository.HoaDonChiTietRepository hoaDonChiTietRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final com.smashvn.shop.repository.SoDiaChiRepository soDiaChiRepository;
    private final com.smashvn.shop.service.admin.AdminKhachHangService adminKhachHangService;
    private final com.smashvn.shop.service.common.FileStorageService fileStorageService;
    /**
     * Phase 1 – Kho San Pham Loi
     */
    private final InventoryLotService inventoryLotService;

    @GetMapping("/all")
    public String hienThiDashboard(Model model) {
        java.util.List<String> employeeRoles = java.util.List.of("NV", "QL");
        java.util.List<TaiKhoan> recentEmployeeAccounts = taiKhoanRepository
                .findTop10ByVaiTroInOrderByIdDesc(employeeRoles);
        java.util.List<TaiKhoan> recentCustomerAccounts = taiKhoanRepository
                .findTop10ByVaiTroOrderByIdDesc("KH");
        java.util.List<com.smashvn.shop.entity.SanPham> recentProducts = sanPhamRepository
                .findTop10ByOrderByIdDesc();

        long employeeCount = nhanVienRepository.count();
        long staffAccountCount = taiKhoanRepository.countByVaiTro("NV");
        long managerAccountCount = taiKhoanRepository.countByVaiTro("QL");
        long employeeAccountCount = taiKhoanRepository.countByVaiTroIn(employeeRoles);
        long customerAccountCount = taiKhoanRepository.countByVaiTro("KH");
        long productCount = sanPhamRepository.count();
        long inStockProductCount = sanPhamRepository.countActiveProductsWithStock();

        model.addAttribute("danhSachTaiKhoanNhanVien", recentEmployeeAccounts);
        model.addAttribute("danhSachTaiKhoanKhachHang", recentCustomerAccounts);
        model.addAttribute("soLuongNhanVien", employeeCount);
        model.addAttribute("soLuongTaiKhoanNhanVienOnly", staffAccountCount);
        model.addAttribute("soLuongTaiKhoanQuanLy", managerAccountCount);
        model.addAttribute("soLuongTaiKhoanNhanVien", employeeAccountCount);
        model.addAttribute("soLuongTaiKhoanKhachHang", customerAccountCount);
        model.addAttribute("soLuongSanPham", productCount);
        model.addAttribute("soLuongSanPhamConHang", inStockProductCount);

        model.addAttribute("danhSachSanPham", recentProducts);
        model.addAttribute("danhSachChoKhoa", nhanVienRepository.findPendingLockEmployees());
        return "admin/admin-dashboard";
    }

    @GetMapping("/don-hang")
    public String hienThiDanhSachDonHang(
            @RequestParam(value = "orderCode", required = false) String orderCode,
            @RequestParam(value = "transactionId", required = false) String transactionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            Model model) {

        String role = (String) session.getAttribute("vaiTro");
        if (role == null) {
            return "redirect:/admin/dang-nhap";
        }

        java.util.List<com.smashvn.shop.entity.HoaDon> orders = hoaDonRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        model.addAttribute("danhSachDonHang", orders);

        java.util.List<com.smashvn.shop.entity.HoaDon> onlineOrders = new java.util.ArrayList<>();
        java.util.List<com.smashvn.shop.entity.HoaDon> posOrders = new java.util.ArrayList<>();
        java.util.List<com.smashvn.shop.entity.HoaDon> returnOrders = new java.util.ArrayList<>();
        for (com.smashvn.shop.entity.HoaDon hd : orders) {
            com.smashvn.shop.entity.ReturnStatus resolvedReturn = orderViewService.resolveReturnStatus(hd.getId(), hd);
            if (resolvedReturn != null) {
                hd.setTrangThaiHoanHang(resolvedReturn);
                hd.setGhnReturnOrderCode(orderViewService.resolveGhnReturnOrderCode(hd.getId(), hd));
                returnOrders.add(hd);
            }
            if ("REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan())
                    || "DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan())
                    || hd.getTrangThaiHoanHang() == com.smashvn.shop.entity.ReturnStatus.REFUNDED
                    || hd.getRefundStatus() == com.smashvn.shop.entity.RefundStatus.COMPLETED) {
                orderViewService.resolveRefundDetails(hd.getId(), hd);
            }
            if (hd.getMaDonHang() != null && hd.getMaDonHang().startsWith("HDSVN")) {
                posOrders.add(hd);
            } else {
                onlineOrders.add(hd);
            }
        }
        model.addAttribute("danhSachDonHangOnline", onlineOrders);
        model.addAttribute("danhSachDonHangPos", posOrders);
        model.addAttribute("danhSachDonHangReturn", returnOrders);

        java.util.Map<Integer, String> currentStatusLabels = new java.util.HashMap<>();
        java.util.Map<Integer, String> nextStatusLabels = new java.util.HashMap<>();
        for (com.smashvn.shop.entity.HoaDon hd : orders) {
            if (hd.getTrangThaiDonHang() != null) {
                currentStatusLabels.put(hd.getId(), orderViewService.getStatusLabel(hd.getTrangThaiDonHang()));
                String nextStatus = orderViewService.getNextStatus(hd);
                if (nextStatus != null) {
                    nextStatusLabels.put(hd.getId(), orderViewService.getStatusLabel(nextStatus));
                }
            }
        }
        model.addAttribute("currentStatusLabels", currentStatusLabels);
        model.addAttribute("nextStatusLabels", nextStatusLabels);
        model.addAttribute("orderViewService", orderViewService);

        // Parse dates
        java.time.LocalDateTime start = parseStartDate(startDate);
        java.time.LocalDateTime end = parseEndDate(endDate);

        // Filter transactions (clean, dynamic JPQL query)
        java.util.List<PaymentTransaction> list = paymentTransactionRepository.filterTransactions(
                cleanParam(orderCode),
                cleanParam(transactionId),
                cleanParam(status),
                start,
                end
        );

        boolean isManager = "QL".equals(role);

        model.addAttribute("danhSachGiaoDich", list);
        model.addAttribute("orderCode", orderCode);
        model.addAttribute("transactionId", transactionId);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("isManager", isManager);

        String resolvedTab = activeTab;
        if (resolvedTab == null) {
            if ((orderCode != null && !orderCode.trim().isEmpty())
                    || (transactionId != null && !transactionId.trim().isEmpty())
                    || (status != null && !status.trim().isEmpty())
                    || (startDate != null && !startDate.trim().isEmpty())
                    || (endDate != null && !endDate.trim().isEmpty())) {
                resolvedTab = "transactions";
            } else {
                resolvedTab = "orders";
            }
        }
        model.addAttribute("activeTab", resolvedTab);

        return "admin/donhang-list";
    }

    private java.time.LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(dateStr).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private java.time.LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(dateStr).atTime(java.time.LocalTime.MAX);
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanParam(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        return val.trim();
    }

    private String friendlyErrorMessage(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause.getMessage() != null) {
                message += " " + cause.getMessage();
            }
            cause = cause.getCause();
        }

        String lowerMessage = message.toLowerCase();
        if (lowerMessage.contains("uk_username") || lowerMessage.contains("username") || lowerMessage.contains("email")) {
            return "Email/Tên đăng nhập đã được sử dụng trong hệ thống.";
        }
        if (lowerMessage.contains("so_dien_thoai") || lowerMessage.contains("sodienthoai")) {
            return "Số điện thoại đã được sử dụng.";
        }
        if (lowerMessage.contains("duplicate") || lowerMessage.contains("unique") || lowerMessage.contains("constraint")) {
            return "Dữ liệu đã tồn tại hoặc không hợp lệ. Vui lòng kiểm tra lại.";
        }
        return message.isBlank() ? "Không thể thực hiện thao tác. Vui lòng kiểm tra lại." : ex.getMessage();
    }

    @GetMapping("/khach-hang")
    public String hienThiDanhSachKhachHang(Model model) {
        model.addAttribute("danhSachKhachHang", khachHangRepository.findByTaiKhoan_VaiTro("KH"));
        return "admin/khachhang-list";
    }

    @PostMapping("/khach-hang/them")
    public String xuLyThemKhachHang(
            @RequestParam(value = "email", required = false) String email,
            @RequestParam("matKhau") String matKhau,
            @RequestParam("hoTenKh") String hoTenKh,
            @RequestParam(value = "soDienThoaiKh", required = false) String soDienThoaiKh,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminKhachHangService.createKhachHang(email, matKhau, hoTenKh, soDienThoaiKh, actingTaiKhoanId, ipAddress);
            redirectAttributes.addFlashAttribute("success", "Tạo mới tài khoản khách hàng thành công!");
            return "redirect:/admin/khach-hang?themThanhCong";
        } catch (Exception e) {
            String errorMessage = friendlyErrorMessage(e);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            redirectAttributes.addFlashAttribute("loi", errorMessage);
            return "redirect:/admin/khach-hang";
        }
    }

    @PostMapping("/khach-hang/sua/{id}")
    public String xuLySuaKhachHang(
            @PathVariable("id") Integer id,
            @RequestParam("hoTenKh") String hoTenKh,
            @RequestParam(value = "soDienThoaiKh", required = false) String soDienThoaiKh,
            @RequestParam("trangThai") String trangThai,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
            String ipAddress = request.getRemoteAddr();

            adminKhachHangService.updateKhachHang(id, hoTenKh, soDienThoaiKh, trangThai, actingTaiKhoanId, ipAddress);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin khách hàng thành công!");
            return "redirect:/admin/khach-hang?suaThanhCong";
        } catch (Exception e) {
            String errorMessage = friendlyErrorMessage(e);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            redirectAttributes.addFlashAttribute("loi", errorMessage);
            return "redirect:/admin/khach-hang";
        }
    }

    @GetMapping("/khach-hang/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getChiTietKhachHangApi(@PathVariable("id") Integer idKhachHang) {
        com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findById(idKhachHang).orElse(null);
        if (kh == null) {
            return ResponseEntity.notFound().build();
        }

        java.util.List<com.smashvn.shop.entity.HoaDon> hoaDons = hoaDonRepository.findByKhachHang_IdOrderByIdDesc(idKhachHang);

        long tongDonHoanThanh = hoaDons.stream()
                .filter(hd -> {
                    String st = hd.getTrangThaiDonHang();
                    return st != null && ("da_giao".equalsIgnoreCase(st) || "hoan_thanh".equalsIgnoreCase(st));
                })
                .count();

        java.math.BigDecimal tongChiTieu = hoaDons.stream()
                .filter(hd -> {
                    String st = hd.getTrangThaiDonHang();
                    return st != null && ("da_giao".equalsIgnoreCase(st) || "hoan_thanh".equalsIgnoreCase(st));
                })
                .map(hd -> hd.getTongTien() != null ? hd.getTongTien() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.util.List<com.smashvn.shop.entity.SoDiaChi> diaChis = soDiaChiRepository.findByKhachHang_Id(idKhachHang);

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", kh.getId());
        response.put("maKhachHang", "KH" + kh.getId());
        response.put("hoTen", kh.getHoTenKh() != null ? kh.getHoTenKh() : "Khách Hàng");
        response.put("username", kh.getTaiKhoan() != null ? kh.getTaiKhoan().getUsername() : "Khách vãng lai / POS");
        response.put("soDienThoai", (kh.getSoDienThoaiKh() != null && !kh.getSoDienThoaiKh().isBlank()) ? kh.getSoDienThoaiKh() : "Chưa cập nhật");

        String trangThai = "Không có tài khoản";
        if (kh.getTaiKhoan() != null) {
            trangThai = "hoat_dong".equalsIgnoreCase(kh.getTaiKhoan().getTrangThai()) ? "Hoạt động" : "Ngừng hoạt động";
        }
        response.put("trangThaiTaiKhoan", trangThai);

        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        response.put("ngayTaoFormatted", kh.getNgayTao() != null ? kh.getNgayTao().format(dtf) : "N/A");

        response.put("tongDonHoanThanh", tongDonHoanThanh);
        response.put("tongChiTieuRaw", tongChiTieu);
        response.put("tongChiTieuFormatted", String.format("%,d ₫", tongChiTieu.longValue()));
        response.put("tongSoDonHang", hoaDons.size());

        java.util.List<java.util.Map<String, Object>> listDiaChiMap = diaChis.stream().map(dc -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("hoTenNguoiNhan", dc.getHoVaTenNguoiNhan());
            item.put("sdtNguoiNhan", dc.getSdtNguoiNhan());
            StringBuilder fullAddr = new StringBuilder(dc.getDiaChiCuThe() != null ? dc.getDiaChiCuThe() : "");
            if (dc.getPhuongXa() != null && !dc.getPhuongXa().isBlank()) {
                fullAddr.append(", ").append(dc.getPhuongXa());
            }
            if (dc.getQuanHuyen() != null && !dc.getQuanHuyen().isBlank()) {
                fullAddr.append(", ").append(dc.getQuanHuyen());
            }
            if (dc.getTinhThanh() != null && !dc.getTinhThanh().isBlank()) {
                fullAddr.append(", ").append(dc.getTinhThanh());
            }
            item.put("diaChiFull", fullAddr.toString());
            item.put("laMacDinh", dc.isDiaChiMacDinh());
            return item;
        }).collect(java.util.stream.Collectors.toList());
        response.put("danhSachDiaChi", listDiaChiMap);

        java.util.List<java.util.Map<String, Object>> listDonHangMap = hoaDons.stream().limit(10).map(hd -> {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", hd.getId());
            item.put("maDonHang", hd.getMaDonHang());
            item.put("ngayTaoFormatted", hd.getNgayTao() != null ? hd.getNgayTao().format(dtf) : "N/A");
            item.put("tongTienFormatted", hd.getTongTien() != null ? String.format("%,d ₫", hd.getTongTien().longValue()) : "0 ₫");
            item.put("trangThaiDonHang", hd.getTrangThaiDonHang());
            item.put("trangThaiThanhToan", hd.getTrangThaiThanhToan());
            return item;
        }).collect(java.util.stream.Collectors.toList());
        response.put("danhSachDonHang", listDonHangMap);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/khuyen-mai")
    public String hienThiDanhSachKhuyenMai(Model model) {
        model.addAttribute("danhSachDotGiamGia", adminKhuyenMaiService.getAllDotGiamGia());
        model.addAttribute("danhSachPhieuGiamGia", adminKhuyenMaiService.getAllPhieuGiamGia());
        return "admin/khuyenmai-list";
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }

    private String redirectWithTab(String activeTab) {
        if (activeTab != null && !activeTab.trim().isEmpty()) {
            return "redirect:/admin/don-hang?activeTab=" + activeTab.trim();
        }
        return "redirect:/admin/don-hang";
    }

    @PostMapping("/don-hang/update-status")
    public Object capNhatTrangThaiDonHang(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam("trangThai") String trangThai,
            @RequestParam("expectedStatus") String expectedStatus,
            @RequestParam(value = "lyDoHuy", required = false) String lyDoHuy,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.updateOrderStatusByAdmin(idHoaDon, trangThai, expectedStatus, actingTaiKhoanId, request.getRemoteAddr(), lyDoHuy);
            String msg = "Cập nhật trạng thái đơn hàng #" + idHoaDon + " thành công!";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            String errorMsg = "Lỗi: Bạn không có quyền thực hiện chức năng này.";
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        } catch (Exception e) {
            String errorMsg = "Lỗi: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }

        return redirectWithTab(activeTab);
    }

    @PostMapping("/don-hang/next-status")
    public Object moveOrderToNextStatus(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.moveOrderToNextStatus(idHoaDon, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Cập nhật trạng thái đơn hàng #" + idHoaDon + " thành công!";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            String errorMsg = "Lỗi: Bạn không có quyền thực hiện chức năng này.";
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        } catch (Exception e) {
            String errorMsg = "Lỗi: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }

        return redirectWithTab(activeTab);
    }

    @PostMapping("/don-hang/cancel-unpaid")
    public Object cancelOrderUnpaid(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "lyDoHuy", required = false) String lyDoHuy,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.cancelOrderUnpaid(idHoaDon, lyDoHuy, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Hủy đơn hàng #" + idHoaDon + " thành công!";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            String errorMsg = "Lỗi: Bạn không có quyền thực hiện chức năng này.";
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        } catch (Exception e) {
            String errorMsg = "Lỗi hủy đơn: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }

        return redirectWithTab(activeTab);
    }

    @PostMapping("/don-hang/cancel-paid-refund")
    public Object cancelOrderPaidWithRefund(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "lyDoHuy", required = false) String lyDoHuy,
            @RequestParam(value = "phuongThucHoanTien", required = false) String phuongThucHoanTien,
            @RequestParam(value = "soTienHoan", required = false) java.math.BigDecimal soTienHoan,
            @RequestParam(value = "maGiaoDichHoanTien", required = false) String maGiaoDichHoanTien,
            @RequestParam(value = "ghiChuHoanTien", required = false) String ghiChuHoanTien,
            @RequestParam(value = "fileChungTu", required = false) org.springframework.web.multipart.MultipartFile fileChungTu,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        java.util.List<String> savedNames = null;
        String anhChungTuUrl = null;
        try {
            if (fileChungTu != null && !fileChungTu.isEmpty()) {
                savedNames = fileStorageService.saveImages(java.util.List.of(fileChungTu), "refunds");
                if (savedNames != null && !savedNames.isEmpty()) {
                    anhChungTuUrl = "/uploads/refunds/" + savedNames.get(0);
                }
            }

            orderViewService.cancelOrderPaidWithRefund(
                    idHoaDon,
                    lyDoHuy,
                    phuongThucHoanTien,
                    soTienHoan,
                    maGiaoDichHoanTien,
                    ghiChuHoanTien,
                    anhChungTuUrl,
                    actingTaiKhoanId,
                    request.getRemoteAddr()
            );
            String msg = "Đã xác nhận hoàn tiền và hủy đơn hàng #" + idHoaDon + " thành công!";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            if (savedNames != null && !savedNames.isEmpty()) {
                try {
                    fileStorageService.deleteFiles(savedNames, "refunds");
                } catch (Exception cleanupEx) {
                    log.warn("Không thể xóa file ảnh chứng từ hoàn tiền tạm thời: {}", cleanupEx.getMessage());
                }
            }
            String errorMsg = "Lỗi hoàn tiền & hủy đơn: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }

        return redirectWithTab(activeTab);
    }

    @GetMapping("/don-hang/approve-refund")
    public String approveRefundEmail(
            @RequestParam("id") Integer id,
            @RequestParam("token") String token,
            HttpServletRequest request,
            Model model) {
        try {
            model.addAttribute("success", true);
            model.addAttribute("title", "Phê Duyệt Hoàn Tiền Thành Công");
            model.addAttribute("message", "Đơn hàng #" + id + " đã được phê duyệt hoàn tiền. Số tiền đã được chính thức trừ khỏi thống kê doanh thu.");
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("title", "Lỗi Phê Duyệt Hoàn Tiền");
            model.addAttribute("message", e.getMessage());
        }
        return "admin/confirm-result";
    }

    @GetMapping("/don-hang/reject-refund")
    public String rejectRefundEmail(
            @RequestParam("id") Integer id,
            @RequestParam("token") String token,
            HttpServletRequest request,
            Model model) {
        try {
            model.addAttribute("success", true);
            model.addAttribute("title", "Từ Chối Hoàn Tiền Thành Công");
            model.addAttribute("message", "Yêu cầu hoàn tiền cho đơn hàng #" + id + " đã bị từ chối. Trạng thái thanh toán được giữ nguyên.");
        } catch (Exception e) {
            model.addAttribute("success", false);
            model.addAttribute("title", "Lỗi Thao Tác");
            model.addAttribute("message", e.getMessage());
        }
        return "admin/confirm-result";
    }

    @PostMapping("/don-hang/approve-refund-ui")
    public Object approveRefundUi(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }
        try {
            com.smashvn.shop.entity.HoaDon hd = hoaDonRepository.findById(idHoaDon)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
            String response = hd.getGatewayResponse();
            String token = "";
            if (response != null && response.contains("REFUND_TOKEN:")) {
                int start = response.indexOf("REFUND_TOKEN:") + 13;
                int end = response.indexOf(";", start);
                if (end == -1) {
                    end = response.length();
                }
                token = response.substring(start, end);
            }

            orderViewService.approveRefund(idHoaDon, token, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Đã phê duyệt hoàn tiền thành công cho đơn hàng #" + idHoaDon;
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            String errorMsg = "Lỗi phê duyệt hoàn tiền: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }

        return redirectWithTab(activeTab);
    }

    @PostMapping("/don-hang/reject-refund-ui")
    public Object rejectRefundUi(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            com.smashvn.shop.entity.HoaDon hd = hoaDonRepository.findById(idHoaDon)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
            String response = hd.getGatewayResponse();
            String token = "";
            if (response != null && response.contains("REFUND_TOKEN:")) {
                int start = response.indexOf("REFUND_TOKEN:") + 13;
                int end = response.indexOf(";", start);
                if (end == -1) {
                    end = response.length();
                }
                token = response.substring(start, end);
            }

            orderViewService.rejectRefund(idHoaDon, token, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Đã từ chối hoàn tiền cho đơn hàng #" + idHoaDon;
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            String errorMsg = "Lỗi từ chối hoàn tiền: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }

        return redirectWithTab(activeTab);
    }

    @GetMapping("/don-hang/detail-json")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> getOrderDetailJson(
            @RequestParam("id") Integer id,
            HttpSession session) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        String role = (String) session.getAttribute("vaiTro");
        String activeRole = (String) session.getAttribute("activeRole");
        var secAuth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSpringAuth = secAuth != null && secAuth.isAuthenticated() && !(secAuth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);

        if (actingTaiKhoanId == null && !isSpringAuth) {
            return org.springframework.http.ResponseEntity.status(401).build();
        }
        if (role != null && !"QL".equalsIgnoreCase(role) && !"NV".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role) && !"STAFF".equalsIgnoreCase(role)
                && !"QL".equalsIgnoreCase(activeRole) && !"NV".equalsIgnoreCase(activeRole) && !isSpringAuth) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }

        java.util.Optional<com.smashvn.shop.entity.HoaDon> opt = hoaDonRepository.findById(id);
        if (opt.isEmpty()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        try {
            com.smashvn.shop.entity.HoaDon hd = opt.get();
            java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", hd.getId());
            map.put("maDonHang", hd.getMaDonHang() != null ? hd.getMaDonHang() : "#" + hd.getId());
            map.put("ngayTao", hd.getNgayTao() != null ? hd.getNgayTao().toString() : "");

            // Thông tin khách hàng — null-safe
            String tenKH = "";
            if (hd.getKhachHang() != null) {
                String ho = hd.getKhachHang().getHoKh() != null ? hd.getKhachHang().getHoKh() : "";
                String ten = hd.getKhachHang().getTenKh() != null ? hd.getKhachHang().getTenKh() : "";
                tenKH = (ho + " " + ten).trim();
            }
            map.put("khachHang", tenKH);
            map.put("sdt", hd.getSdtNhan() != null ? hd.getSdtNhan() : "");
            map.put("diaChi", hd.getDiaChiNhan() != null ? hd.getDiaChiNhan() : "");
            map.put("donViVanChuyen", hd.getDonViVanChuyen() != null ? hd.getDonViVanChuyen().getTenDonVi() : "N/A");
            map.put("tongTien", hd.getTongTien() != null ? hd.getTongTien() : java.math.BigDecimal.ZERO);
            map.put("phiVanChuyen", hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : java.math.BigDecimal.ZERO);
            map.put("soTienGiamVoucher", hd.getSoTienGiamVoucher() != null ? hd.getSoTienGiamVoucher() : java.math.BigDecimal.ZERO);

            String maVoucher = hd.getMaVoucherApDung();
            String tenVoucher = hd.getTenVoucherApDung();
            String moTaVoucher = hd.getMoTaVoucherSnapshot();

            if (maVoucher == null || maVoucher.isEmpty()) {
                if (hd.getPhieuGiamGia() != null) {
                    maVoucher = hd.getPhieuGiamGia().getMaPhieu();
                } else if (hd.getSoTienGiamVoucher() != null && hd.getSoTienGiamVoucher().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    maVoucher = "Voucher";
                } else {
                    maVoucher = "Không áp dụng voucher";
                }
            }
            if (tenVoucher == null || tenVoucher.isEmpty()) {
                if (hd.getPhieuGiamGia() != null) {
                    tenVoucher = hd.getPhieuGiamGia().getTenPhieu();
                } else if (hd.getSoTienGiamVoucher() != null && hd.getSoTienGiamVoucher().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    tenVoucher = "Voucher giảm giá";
                } else {
                    tenVoucher = "Không áp dụng voucher";
                }
            }
            if (moTaVoucher == null || moTaVoucher.isEmpty()) {
                if (hd.getPhieuGiamGia() != null) {
                    var pg = hd.getPhieuGiamGia();
                    String limitDesc = pg.getGiaTriGiamToiDa() != null ? " (Giảm tối đa " + pg.getGiaTriGiamToiDa() + "đ)" : "";
                    moTaVoucher = "Giảm " + pg.getGiaTri() + ("%".equals(pg.getDonVi()) ? "%" : "đ") + limitDesc + " cho đơn hàng từ " + pg.getGiaTriDonHangToiThieu() + "đ";
                } else {
                    moTaVoucher = "Không áp dụng voucher";
                }
            }

            map.put("maVoucherApDung", maVoucher);
            map.put("tenVoucherApDung", tenVoucher);
            map.put("moTaVoucherSnapshot", moTaVoucher);
            map.put("trangThai", orderViewService.getStatusLabel(hd.getTrangThaiDonHang()));
            map.put("trangThaiRaw", hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang() : "");
            String nextStatus = orderViewService.getNextStatus(hd);
            String nextStatusLabel = nextStatus != null ? orderViewService.getStatusLabel(nextStatus) : "";
            map.put("nextStatus", nextStatus != null ? nextStatus : "");
            map.put("nextStatusLabel", nextStatusLabel);
            boolean isPos = (hd.getMaDonHang() != null && hd.getMaDonHang().startsWith("HDSVN"))
                    || hd.getNhanVien() != null
                    || "Bán tại quầy".equalsIgnoreCase(hd.getDiaChiNhan())
                    || (hd.getDiaChiNhan() != null && (hd.getDiaChiNhan().toLowerCase().contains("quầy") || hd.getDiaChiNhan().toLowerCase().contains("quay")))
                    || (hd.getDonViVanChuyen() != null && hd.getDonViVanChuyen().getTenDonVi() != null && (hd.getDonViVanChuyen().getTenDonVi().toLowerCase().contains("quầy") || hd.getDonViVanChuyen().getTenDonVi().toLowerCase().contains("quay")));
            map.put("isPosOrder", isPos);
            String returnCode = orderViewService.resolveGhnReturnOrderCode(hd.getId(), hd);
            com.smashvn.shop.entity.ReturnStatus resolvedReturnStatus = orderViewService.resolveReturnStatus(hd.getId(), hd);

            map.put("ghnOrderCode", hd.getGhnOrderCode() != null ? hd.getGhnOrderCode() : "");
            map.put("ghnReturnOrderCode", returnCode != null ? returnCode : "");
            map.put("ghnExchangeOrderCode", orderViewService.resolveGhnExchangeOrderCode(hd.getId()) != null ? orderViewService.resolveGhnExchangeOrderCode(hd.getId()) : "");

            // Phương thức thanh toán — null-safe
            String tenPhuongThuc = "N/A";
            if (hd.getPhuongThucThanhToan() != null && hd.getPhuongThucThanhToan().getTenPhuongThuc() != null) {
                tenPhuongThuc = hd.getPhuongThucThanhToan().getTenPhuongThuc();
            } else if (hd.getPaymentMethod() != null) {
                tenPhuongThuc = hd.getPaymentMethod().toUpperCase();
            }
            map.put("paymentMethod", tenPhuongThuc);
            map.put("isCod", hd.isCod());
            map.put("isPrepaid", hd.isPrepaid());

            var paymentInfo = orderViewService.getPaymentStatusInfo(hd.getTrangThaiThanhToan());
            map.put("paymentStatus", hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan() : "N/A");
            map.put("paymentStatusCode", paymentInfo.code());
            map.put("paymentStatusLabel", paymentInfo.label());
            map.put("paymentStatusBadgeClass", paymentInfo.badgeClass());

            map.put("maGiaoDich", hd.getMaGiaoDich() != null ? hd.getMaGiaoDich() : "");
            String nvName = hd.getNguoiXacNhanThanhToan();
            if (nvName == null || nvName.isBlank()) {
                if (hd.getNhanVien() != null && hd.getNhanVien().getHoTenNv() != null) {
                    nvName = hd.getNhanVien().getHoTenNv();
                } else {
                    nvName = "Nhân viên hệ thống";
                }
            }
            map.put("nguoiXacNhan", nvName);

            java.time.LocalDateTime xnTime = hd.getThoiGianXacNhan() != null 
                    ? hd.getThoiGianXacNhan() 
                    : (hd.getPaidAt() != null ? hd.getPaidAt() : (hd.getNgayThanhToan() != null ? hd.getNgayThanhToan() : hd.getNgayTao()));
            String formattedXacNhanAt = "";
            if (xnTime != null) {
                formattedXacNhanAt = xnTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            }
            map.put("thoiGianXacNhan", formattedXacNhanAt);

            // Additional payment transaction fields
            java.time.LocalDateTime paidTime = hd.getPaidAt() != null 
                    ? hd.getPaidAt() 
                    : (hd.getNgayThanhToan() != null ? hd.getNgayThanhToan() : ("DA_THANH_TOAN".equalsIgnoreCase(hd.getTrangThaiThanhToan()) ? hd.getNgayTao() : null));
            String formattedPaidAt = "";
            if (paidTime != null) {
                formattedPaidAt = paidTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            }
            map.put("thoiGianThanhToan", formattedPaidAt);
            map.put("transactionId", hd.getTransactionId() != null ? hd.getTransactionId() : "");
            map.put("gatewayResponse", hd.getGatewayResponse() != null ? hd.getGatewayResponse() : "");
            map.put("ghiChu", hd.getGhiChu() != null ? hd.getGhiChu() : "");

            // Return status details
            map.put("trangThaiHoanHang", resolvedReturnStatus != null ? resolvedReturnStatus.name() : (hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : null));
            map.put("trangThaiHoanHangLabel", resolvedReturnStatus != null ? resolvedReturnStatus.getLabel() : (hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().getLabel() : ""));
            map.put("loaiYeuCauDoiTra", hd.getLoaiYeuCauDoiTra() != null ? hd.getLoaiYeuCauDoiTra() : "");
            map.put("lyDoHoanTra", hd.getLyDoHoanTra() != null ? hd.getLyDoHoanTra() : "");
            map.put("bangChungHoanTra", hd.getBangChungHoanTra() != null ? hd.getBangChungHoanTra() : "");
            map.put("ngayXacNhanHoanHang", hd.getNgayXacNhanHoanHang() != null ? hd.getNgayXacNhanHoanHang().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "");
            map.put("nhanVienXacNhan", hd.getNhanVienXacNhan() != null ? hd.getNhanVienXacNhan().getHoTenNv() : "");

            java.util.Map<String, String> refundDetails = orderViewService.resolveRefundDetails(hd.getId(), hd);

            // Refund status details
            map.put("refundStatus", hd.getRefundStatus() != null ? hd.getRefundStatus().name() : (refundDetails.containsKey("phuongThucHoanTien") ? "COMPLETED" : ""));
            map.put("refundStatusLabel", hd.getRefundStatus() != null ? hd.getRefundStatus().getLabel() : (refundDetails.containsKey("phuongThucHoanTien") ? "Đã hoàn tiền" : ""));
            String refTime = refundDetails.getOrDefault("thoiGianHoanTien", "");
            if (refTime.isEmpty() && hd.getRefundTime() != null) {
                refTime = hd.getRefundTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            }
            map.put("refundTime", refTime);
            map.put("refundConfirmedBy", refundDetails.getOrDefault("nguoiThucHienHoanTien", hd.getRefundConfirmedBy() != null ? hd.getRefundConfirmedBy().getHoTenNv() : ""));

            // Cancellation & Refund meta
            map.put("isOrderPaid", orderViewService.isOrderPaid(hd));
            map.put("isStockDeducted", orderViewService.isStockDeductedState(hd, hd.getTrangThaiDonHang()));
            map.put("lyDoHuy", hd.getLyDoHuy() != null ? hd.getLyDoHuy() : "");
            
            String ptht = refundDetails.getOrDefault("phuongThucHoanTien", hd.getPhuongThucHoanTien() != null ? hd.getPhuongThucHoanTien() : "");
            map.put("phuongThucHoanTien", ptht);
            
            java.math.BigDecimal sth = hd.getSoTienHoan();
            if (sth == null && refundDetails.containsKey("soTienHoan")) {
                try {
                    sth = new java.math.BigDecimal(refundDetails.get("soTienHoan"));
                } catch (Exception ignored) {}
            }
            map.put("soTienHoan", sth != null ? sth : java.math.BigDecimal.ZERO);
            map.put("maGiaoDichHoanTien", refundDetails.getOrDefault("maGiaoDichHoanTien", hd.getMaGiaoDichHoanTien() != null ? hd.getMaGiaoDichHoanTien() : ""));
            map.put("ghiChuHoanTien", refundDetails.getOrDefault("ghiChuHoanTien", hd.getGhiChuHoanTien() != null ? hd.getGhiChuHoanTien() : ""));
            map.put("anhChungTuHoanTien", refundDetails.getOrDefault("anhChungTuHoanTien", hd.getAnhChungTuHoanTien() != null ? hd.getAnhChungTuHoanTien() : ""));
            boolean canCancel = !OrderStatus.DA_HUY.getValue().equalsIgnoreCase(hd.getTrangThaiDonHang())
                    && !OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(hd.getTrangThaiDonHang())
                    && !OrderStatus.DA_BAN_GIAO_GHN.getValue().equalsIgnoreCase(hd.getTrangThaiDonHang())
                    && !"dang_giao".equalsIgnoreCase(hd.getTrangThaiDonHang())
                    && !"hoan_thanh".equalsIgnoreCase(hd.getTrangThaiDonHang());
            map.put("canCancel", canCancel);

            // Linked transactions from PaymentTransaction
            java.util.List<com.smashvn.shop.entity.PaymentTransaction> txs = paymentTransactionRepository.findByOrder_Id(id);
            java.util.List<java.util.Map<String, Object>> txsList = new java.util.ArrayList<>();
            for (com.smashvn.shop.entity.PaymentTransaction tx : txs) {
                java.util.Map<String, Object> txMap = new java.util.LinkedHashMap<>();
                txMap.put("id", tx.getId());
                txMap.put("transactionId", tx.getTransactionId() != null ? tx.getTransactionId() : "");
                txMap.put("amount", tx.getAmount() != null ? tx.getAmount() : java.math.BigDecimal.ZERO);
                txMap.put("gateway", tx.getGateway() != null ? tx.getGateway() : "");
                txMap.put("status", tx.getStatus() != null ? tx.getStatus() : "");
                String txDate = "";
                if (tx.getCreatedAt() != null) {
                    txDate = tx.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                }
                txMap.put("createdAt", txDate);
                txsList.add(txMap);
            }
            map.put("transactions", txsList);

            // Danh sách sản phẩm — null-safe
            java.util.List<com.smashvn.shop.entity.HoaDonChiTiet> items
                    = hoaDonChiTietRepository.findByHoaDon_Id(id);
            java.util.List<java.util.Map<String, Object>> itemsList = new java.util.ArrayList<>();
            for (com.smashvn.shop.entity.HoaDonChiTiet item : items) {
                java.util.Map<String, Object> itemMap = new java.util.LinkedHashMap<>();
                String tenSP = item.getTenSanPhamSnapshot();
                if (tenSP == null || tenSP.isBlank()) {
                    if (item.getSanPhamChiTiet() != null && item.getSanPhamChiTiet().getSanPham() != null) {
                        tenSP = item.getSanPhamChiTiet().getSanPham().getTenSanPham();
                    }
                }
                if (tenSP == null) {
                    tenSP = "";
                }

                String thuocTinh = "";
                if (item.getThuocTinhSnapshot() != null && !item.getThuocTinhSnapshot().isBlank()) {
                    thuocTinh = item.getThuocTinhSnapshot().replace("Mức cảng:", "Sức căng khuyến nghị:");
                } else if (item.getSanPhamChiTiet() != null) {
                    thuocTinh = item.getSanPhamChiTiet().getPhanLoaiHienThi();
                }

                java.math.BigDecimal giaNiemYet = item.getGiaGoc();
                if (giaNiemYet == null) {
                    if (item.getSanPhamChiTiet() != null) {
                        giaNiemYet = item.getSanPhamChiTiet().getGiaBan();
                    }
                }
                if (giaNiemYet == null) {
                    giaNiemYet = java.math.BigDecimal.ZERO;
                }

                java.math.BigDecimal donGia = item.getDonGia() != null ? item.getDonGia() : java.math.BigDecimal.ZERO;
                java.math.BigDecimal soTienGiamSanPham = giaNiemYet.subtract(donGia);
                if (soTienGiamSanPham.compareTo(java.math.BigDecimal.ZERO) < 0) {
                    soTienGiamSanPham = java.math.BigDecimal.ZERO;
                }
                java.math.BigDecimal phanTramGiam = java.math.BigDecimal.ZERO;
                if (giaNiemYet.compareTo(java.math.BigDecimal.ZERO) > 0 && soTienGiamSanPham.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    try {
                        phanTramGiam = soTienGiamSanPham.multiply(java.math.BigDecimal.valueOf(100))
                                .divide(giaNiemYet, 0, java.math.RoundingMode.HALF_UP);
                    } catch (Exception e) {
                        log.warn("Lỗi tính toán phần trăm giảm giá cho sản phẩm: {}", e.getMessage());
                        phanTramGiam = java.math.BigDecimal.ZERO;
                    }
                }
                String tenDotGiamGia = item.getTenDotGiamGiaSnapshot() != null ? item.getTenDotGiamGiaSnapshot() : "";
                String skuSnapshot = item.getSkuSnapshot();
                if ((skuSnapshot == null || skuSnapshot.isBlank()) && item.getSanPhamChiTiet() != null) {
                    skuSnapshot = item.getSanPhamChiTiet().getSku();
                }
                if (skuSnapshot == null) {
                    skuSnapshot = "";
                }

                String hinhAnh = "";
                if (item.getSanPhamChiTiet() != null) {
                    hinhAnh = item.getSanPhamChiTiet().getHinhAnhSanPham() != null
                            ? item.getSanPhamChiTiet().getHinhAnhSanPham() : "";
                }

                itemMap.put("tenSanPham", tenSP);
                itemMap.put("thuocTinh", thuocTinh);
                itemMap.put("sku", skuSnapshot);
                itemMap.put("soLuong", item.getSoLuong());
                itemMap.put("giaNiemYet", giaNiemYet);
                itemMap.put("giaBan", donGia); // Keep 'giaBan' representing purchase price (donGia) for backward compatibility
                itemMap.put("phanTramGiam", phanTramGiam);
                itemMap.put("soTienGiamSanPham", soTienGiamSanPham);
                itemMap.put("tenDotGiamGia", tenDotGiamGia);
                itemMap.put("hinhAnh", hinhAnh);

                itemsList.add(itemMap);
            }
            map.put("items", itemsList);

            return org.springframework.http.ResponseEntity.ok(map);
        } catch (Exception e) {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("error", "Lỗi khi tải chi tiết đơn hàng: " + e.getMessage());
            return org.springframework.http.ResponseEntity.status(500).body(err);
        }
    }

    @PostMapping("/don-hang/update-return-status")
    public Object updateReturnStatus(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam("trangThaiHoanHang") String trangThaiHoanHang,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.updateReturnStatusByAdmin(idHoaDon, trangThaiHoanHang, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Cập nhật trạng thái hoàn hàng thành công!";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            String errorMsg = "Lỗi: Bạn không có quyền thực hiện chức năng này.";
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                        .body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        } catch (Exception e) {
            String errorMsg = "Lỗi: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }

        return redirectWithTab(activeTab != null ? activeTab : "return");
    }

    @PostMapping("/don-hang/approve-return")
    public Object approveReturn(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            String ghnCode = orderViewService.duyetYeuCauTraHangVaTaoDonGhn(idHoaDon, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Đã duyệt yêu cầu trả hàng và tạo vận đơn GHN thu hồi thành công: " + ghnCode;
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon, "ghnReturnOrderCode", ghnCode != null ? ghnCode : ""));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            String errorMsg = "Lỗi: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }
        return redirectWithTab(activeTab != null ? activeTab : "return");
    }

    @PostMapping("/don-hang/reject-return")
    public Object rejectReturn(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "lyDoTuChoi", required = false) String lyDoTuChoi,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.tuChoiYeuCauTraHang(idHoaDon, lyDoTuChoi, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Đã từ chối yêu cầu trả hàng.";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            String errorMsg = "Lỗi: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }
        return redirectWithTab(activeTab != null ? activeTab : "return");
    }

    @PostMapping("/don-hang/cancel-return-pickup")
    public Object cancelReturnPickup(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.huyDonThuHoiGhn(idHoaDon, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Đã hủy vận đơn thu hồi GHN thành công.";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            String errorMsg = "Lỗi: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }
        return redirectWithTab(activeTab != null ? activeTab : "return");
    }

    @PostMapping("/don-hang/confirm-restock")
    public Object confirmRestock(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "ketQua", required = false, defaultValue = "BAN_LAI") String ketQua,
            @RequestParam(value = "lyDoTuChoi", required = false) String lyDoTuChoi,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.xacNhanKiemKhoVaNhapKho(idHoaDon, ketQua, lyDoTuChoi, actingTaiKhoanId, request.getRemoteAddr());
            String msg;
            if ("TU_CHOI".equalsIgnoreCase(ketQua)) {
                msg = "Đã từ chối nhận hàng hoàn và tạo vận đơn gửi trả lại cho khách hàng thành công!";
            } else if ("HANG_LOI".equalsIgnoreCase(ketQua)) {
                msg = "Đã kiểm hàng thành công và chuyển sản phẩm vào kho hàng lỗi!";
            } else {
                msg = "Đã kiểm hàng thành công và hoàn lại tồn kho bán!";
            }
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            String errorMsg = "Lỗi: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }
        return redirectWithTab(activeTab != null ? activeTab : "return");
    }

    @PostMapping("/don-hang/confirm-refund")
    public Object confirmRefund(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "phuongThucHoanTien", required = false) String phuongThucHoanTien,
            @RequestParam(value = "soTienHoan", required = false) java.math.BigDecimal soTienHoan,
            @RequestParam(value = "maGiaoDichHoanTien", required = false) String maGiaoDichHoanTien,
            @RequestParam(value = "ghiChuHoanTien", required = false) String ghiChuHoanTien,
            @RequestParam(value = "fileChungTu", required = false) org.springframework.web.multipart.MultipartFile fileChungTu,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        if (fileChungTu == null || fileChungTu.isEmpty()) {
            String errorMsg = "Vui lòng tải lên ảnh / chứng từ xác nhận đã hoàn tiền cho khách hàng.";
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
            return redirectWithTab(activeTab != null ? activeTab : "return");
        }

        java.util.List<String> savedNames = null;
        try {
            savedNames = fileStorageService.saveImages(java.util.List.of(fileChungTu), "refunds");
            if (savedNames == null || savedNames.isEmpty()) {
                throw new IllegalArgumentException("Không thể lưu ảnh chứng từ hoàn tiền.");
            }
            String anhChungTuUrl = "/uploads/refunds/" + savedNames.get(0);

            orderViewService.xacNhanHoanTienChoKhach(
                    idHoaDon,
                    phuongThucHoanTien,
                    soTienHoan,
                    maGiaoDichHoanTien,
                    ghiChuHoanTien,
                    anhChungTuUrl,
                    actingTaiKhoanId,
                    request.getRemoteAddr()
            );
            String msg = "Đã xác nhận hoàn tiền thành công cho khách hàng!";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            if (savedNames != null && !savedNames.isEmpty()) {
                try {
                    fileStorageService.deleteFiles(savedNames, "refunds");
                } catch (Exception cleanupEx) {
                    log.warn("Không thể xóa file ảnh chứng từ hoàn tiền tạm thời: {}", cleanupEx.getMessage());
                }
            }
            String errorMsg = "Lỗi hoàn tiền: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }
        return redirectWithTab(activeTab != null ? activeTab : "return");
    }

    /**
     * Endpoint Admin xác nhận phân bổ kho và tạo vận đơn giao sản phẩm đổi mới
     * cho khách hàng (Phase 6)
     */
    @PostMapping("/don-hang/confirm-exchange-shipment")
    public Object confirmExchangeShipment(
            @RequestParam("idHoaDon") Integer idHoaDon,
            @RequestParam(value = "activeTab", required = false) String activeTab,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            if (isAjaxRequest(request)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(java.util.Map.of("success", false, "message", "Phiên làm việc đã hết hạn."));
            }
            return "redirect:/admin/dang-nhap";
        }

        try {
            orderViewService.xacNhanGiaoHangDoiMoiChoKhach(idHoaDon, actingTaiKhoanId, request.getRemoteAddr());
            String msg = "Đã phân bổ kho và khởi tạo vận đơn giao hàng đổi thành công!";
            if (isAjaxRequest(request)) {
                return ResponseEntity.ok(java.util.Map.of("success", true, "message", msg, "idHoaDon", idHoaDon));
            }
            redirectAttributes.addFlashAttribute("successMsg", msg);
        } catch (Exception e) {
            log.error("Lỗi chuẩn bị/giao hàng đổi cho đơn #{}: {}", idHoaDon, e.getMessage(), e);
            String errorMsg = "Lỗi xử lý giao hàng đổi: " + e.getMessage();
            if (isAjaxRequest(request)) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", errorMsg));
            }
            redirectAttributes.addFlashAttribute("errorMsg", errorMsg);
        }
        return redirectWithTab(activeTab != null ? activeTab : "return");
    }

    // ── PHASE 1: KHO SAN PHAM LOI ────────────────────────────────────────────
    /**
     * GET /admin/kho-san-pham-loi Hien thi danh sach bien the san pham co
     * soLuongSpLoi > 0. Chi doc – khong chinh sua du lieu. Phan quyen: QL va NV
     * (dam bao boi SecurityConfig).
     */
    @GetMapping("/kho-san-pham-loi")
    public String hienThiKhoSanPhamLoi(HttpSession session, Model model) {
        String role = (String) session.getAttribute("vaiTro");
        if (role == null) {
            return "redirect:/admin/dang-nhap";
        }

        java.util.List<com.smashvn.shop.dto.inventory.KhoSanPhamLoiView> danhSach
                = inventoryLotService.layDanhSachKhoSanPhamLoi();

        int tongSoLuongSanPhamLoi = danhSach.stream()
                .mapToInt(v -> v.getSoLuongSpLoi() != null ? v.getSoLuongSpLoi() : 0)
                .sum();

        model.addAttribute("danhSachKhoLoi", danhSach);
        model.addAttribute("tongSoLuongSanPhamLoi", tongSoLuongSanPhamLoi);
        model.addAttribute("soBienTheCoLoi", danhSach.size());

        return "admin/kho-san-pham-loi";
    }

    /**
     * Phase 2 – Chi Tiet Kho San Pham Loi GET
     * /admin/kho-san-pham-loi/{idSanPhamChiTiet} Hien thi thong tin bien the va
     * lich su cac don hang da tung chuyen bien the nay vao kho loi.
     */
    @GetMapping("/kho-san-pham-loi/{idSanPhamChiTiet}")
    public String hienThiChiTietKhoSanPhamLoi(
            @PathVariable("idSanPhamChiTiet") Integer idSanPhamChiTiet,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("vaiTro");
        if (role == null) {
            return "redirect:/admin/dang-nhap";
        }

        KhoSanPhamLoiDetailView detail = inventoryLotService.layChiTietKhoSanPhamLoi(idSanPhamChiTiet);
        if (detail == null) {
            redirectAttributes.addFlashAttribute("errorMsg", "Không tìm thấy sản phẩm hoặc biến thể.");
            return "redirect:/admin/kho-san-pham-loi";
        }

        model.addAttribute("detail", detail);
        return "admin/kho-san-pham-loi-detail";
    }

    /**
     * Phase 3 – Xử lý sản phẩm đang nằm trong kho lỗi: POST
     * /admin/kho-san-pham-loi/{idSanPhamChiTiet}/xu-ly 1. Sửa xong -> Nhập lại
     * kho bán (SUA_XONG_NHAP_LAI_KHO) 2. Tiêu hủy (TIEU_HUY - QL ONLY) 3. Trả
     * nhà cung cấp (TRA_NHA_CUNG_CAP)
     */
    @PostMapping("/kho-san-pham-loi/{idSanPhamChiTiet}/xu-ly")
    public String xuLySanPhamLoi(
            @PathVariable("idSanPhamChiTiet") Integer idSanPhamChiTiet,
            @RequestParam("hanhDong") String hanhDong,
            @RequestParam(value = "soLuong", required = false) Integer soLuong,
            @RequestParam(value = "ghiChu", required = false) String ghiChu,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String role = (String) session.getAttribute("vaiTro");
        if (role == null) {
            return "redirect:/admin/dang-nhap";
        }

        Integer actingTaiKhoanId = (Integer) session.getAttribute("idNguoiDung");
        if (actingTaiKhoanId == null) {
            TaiKhoan tk = (TaiKhoan) session.getAttribute("taiKhoan");
            if (tk != null) {
                actingTaiKhoanId = tk.getId();
            }
        }

        String clientIp = request.getRemoteAddr();

        try {
            inventoryLotService.xuLySanPhamLoi(
                    idSanPhamChiTiet,
                    hanhDong,
                    soLuong,
                    ghiChu,
                    actingTaiKhoanId,
                    role,
                    clientIp
            );

            String successMsg = "Đã xử lý sản phẩm lỗi thành công.";
            if ("SUA_XONG_NHAP_LAI_KHO".equalsIgnoreCase(hanhDong)) {
                successMsg = "Đã chuyển " + soLuong + " sản phẩm đã sửa chữa trở lại kho bán.";
            } else if ("TIEU_HUY".equalsIgnoreCase(hanhDong)) {
                successMsg = "Đã tiêu hủy " + soLuong + " sản phẩm lỗi.";
            } else if ("TRA_NHA_CUNG_CAP".equalsIgnoreCase(hanhDong)) {
                successMsg = "Đã ghi nhận trả " + soLuong + " sản phẩm lỗi cho nhà cung cấp.";
            }

            redirectAttributes.addFlashAttribute("successMsg", successMsg);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi khi xử lý sản phẩm lỗi SPCT #{}: ", idSanPhamChiTiet, e);
            redirectAttributes.addFlashAttribute("errorMsg", "Có lỗi xảy ra trong quá trình xử lý: " + e.getMessage());
        }

        return "redirect:/admin/kho-san-pham-loi/" + idSanPhamChiTiet;
    }
}
