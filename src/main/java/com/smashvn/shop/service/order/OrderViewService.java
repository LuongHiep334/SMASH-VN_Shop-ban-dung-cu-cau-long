package com.smashvn.shop.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.dto.inventory.OrderItemRequest;
import com.smashvn.shop.dto.inventory.RestockItemRequest;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.entity.ReturnInventoryStatus;
import com.smashvn.shop.entity.ReturnStatus;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AuditService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class OrderViewService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final AuditService auditService;
    private final com.smashvn.shop.repository.EditLogRepository editLogRepository;
    private final JavaMailSender mailSender;
    private final NhanVienRepository nhanVienRepository;
    private final com.smashvn.shop.repository.ThongBaoRepository thongBaoRepository;
    private final com.smashvn.shop.service.api.GhnService ghnService;
    private final com.smashvn.shop.service.api.GhnStatusMapper ghnStatusMapper;
    private final com.smashvn.shop.service.inventory.InventoryLotService inventoryLotService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.smashvn.shop.service.api.GhnShipmentPersistenceService ghnShipmentPersistenceService;
    private final com.smashvn.shop.repository.PaymentTransactionRepository paymentTransactionRepository;
    private final com.smashvn.shop.repository.PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final ExchangeStockReservationService exchangeStockReservationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.admin.emails}")
    private String adminEmailsConfig;

    // Helper to format dates for dash-my-order.html
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss", Locale.US);

    /**
     * Lấy danh sách đơn hàng cho dash-my-order.html. Nếu khách hàng chưa có đơn
     * hàng thật nào, trả về danh sách đơn hàng giả lập (Mock Orders).
     */
    public List<Map<String, Object>> layDanhSachOrders(Integer idKhachHang) {
        List<HoaDon> realOrders = hoaDonRepository.findByKhachHang_IdOrderByIdDesc(idKhachHang);
        List<Map<String, Object>> resultList = new ArrayList<>();

        if (realOrders != null && !realOrders.isEmpty()) {
            for (HoaDon hd : realOrders) {
                if (!isVisibleInCustomerOrderHistory(hd)) {
                    continue;
                }
                resultList.add(mapSingleOrderToMap(hd));
            }
        }

        return resultList;
    }

    /**
     * Đơn SePay mới chỉ được khởi tạo để chờ quét QR chưa phải là một lần đặt
     * hàng thành công. Vì vậy không hiển thị các bản ghi chờ thanh toán và các
     * bản ghi đã tự hết hạn trong lịch sử mua hàng của khách. Đơn do khách hoặc
     * nhân viên chủ động hủy vẫn được giữ lại để khách có thể tra cứu.
     */
    private boolean isVisibleInCustomerOrderHistory(HoaDon hoaDon) {
        if (hoaDon == null) {
            return false;
        }

        String orderStatus = hoaDon.getTrangThaiDonHang();
        if (OrderStatus.CHO_THANH_TOAN.getValue().equalsIgnoreCase(orderStatus)) {
            return false;
        }

        return !(OrderStatus.DA_HUY.getValue().equalsIgnoreCase(orderStatus)
                && "expired".equalsIgnoreCase(hoaDon.getPaymentStatus()));
    }

    public Map<String, Object> layChiTietDonHangChoCustomer(Integer idHoaDon, Integer idKhachHang) {
        Optional<HoaDon> hdOpt = hoaDonRepository.findById(idHoaDon);
        if (hdOpt.isEmpty()) {
            return null;
        }
        HoaDon hd = hdOpt.get();
        if (idKhachHang != null && (hd.getKhachHang() == null || !hd.getKhachHang().getId().equals(idKhachHang))) {
            return null;
        }
        return mapSingleOrderToMap(hd);
    }

    private Map<String, Object> mapSingleOrderToMap(HoaDon hd) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("id", hd.getId());
        orderMap.put("date", hd.getNgayTao() != null ? hd.getNgayTao().format(formatter) : "");

        String statusText = getFrontendStatusLabel(hd.getTrangThaiDonHang());
        orderMap.put("status", statusText);
        orderMap.put("rawStatus", hd.getTrangThaiDonHang());
        orderMap.put("total", hd.getTongTien());
        orderMap.put("paymentMethod", hd.getPaymentMethod());
        orderMap.put("maDonHang", hd.getMaDonHang());
        orderMap.put("ghnOrderCode", hd.getGhnOrderCode());
        orderMap.put("ghnReturnOrderCode", resolveGhnReturnOrderCode(hd.getId(), hd));

        ReturnStatus returnStatus = resolveReturnStatus(hd.getId(), hd);
        orderMap.put("trangThaiHoanHang", returnStatus != null ? returnStatus.name() : null);
        orderMap.put("trangThaiHoanHangLabel", returnStatus != null ? returnStatus.getLabel() : "");

        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(hd.getId());
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (HoaDonChiTiet ct : items) {
            Map<String, Object> itemMap = new HashMap<>();
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();

            String imgUrl = (spct != null && spct.getHinhAnhUrl() != null) ? spct.getHinhAnhUrl() : "/images/placeholder.png";
            String imgName = "product9.jpg";
            if (spct != null && spct.getHinhAnhSanPham() != null && !spct.getHinhAnhSanPham().isEmpty()) {
                imgName = spct.getHinhAnhSanPham();
            }

            String title = ct.getTenSanPhamSnapshot();
            if (title == null || title.isBlank()) {
                title = (spct != null && spct.getSanPham() != null) ? spct.getSanPham().getTenSanPham() : "Sản phẩm";
            }
            String attr = (spct != null) ? spct.getMauSac() : null;
            if (attr != null && !attr.isBlank() && !"N/A".equalsIgnoreCase(attr.trim())) {
                title += " [" + attr.trim() + "]";
            }

            itemMap.put("imageUrl", imgUrl);
            itemMap.put("image", "../uploads/product/" + imgName);
            itemMap.put("title", title);
            itemMap.put("quantity", ct.getSoLuong() != null ? ct.getSoLuong() : 1);
            itemMap.put("total", (ct.getDonGia() != null ? ct.getDonGia() : BigDecimal.ZERO).multiply(new BigDecimal(ct.getSoLuong() != null ? ct.getSoLuong() : 1)));
            itemMaps.add(itemMap);
        }
        orderMap.put("items", itemMaps);
        return orderMap;
    }

    /**
     * Lấy chi tiết đơn hàng cho dash-manage-order.html. Trả về một Map chứa:
     * "order" (HoaDon hoặc Mock HoaDon), "orderItems" (List<HoaDonChiTiet> hoặc
     * Mock), v.v.
     */
    public Map<String, Object> layChiTietOrder(Integer idHoaDon, Integer idKhachHang) {
        Map<String, Object> modelMap = new HashMap<>();

        // 1. Tìm đơn hàng thật trong CSDL
        Optional<HoaDon> hdOpt = hoaDonRepository.findById(idHoaDon);
        if (hdOpt.isPresent()) {
            HoaDon hd = hdOpt.get();
            // Đảm bảo đơn hàng này đúng của khách hàng đang đăng nhập (hoặc view không ràng buộc idKhachHang)
            if (idKhachHang == null || (hd.getKhachHang() != null && hd.getKhachHang().getId() != null && hd.getKhachHang().getId().equals(idKhachHang))) {
                modelMap.put("order", adaptRealOrder(hd));

                List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
                // Fix path ảnh cho item trước khi ném ra giao diện
                if (items != null) {
                    for (HoaDonChiTiet ct : items) {
                        if (ct.getSanPhamChiTiet() != null && ct.getSanPhamChiTiet().getSanPham() != null) {
                            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
                            if (spct.getHinhAnhSanPham() != null && !spct.getHinhAnhSanPham().isEmpty()) {
                                spct.getSanPham().setMoTa(spct.getHinhAnhSanPham());
                            } else {
                                spct.getSanPham().setMoTa("product9.jpg");
                            }
                        }
                    }
                }

                modelMap.put("orderItems", items != null ? items : new ArrayList<>());

                // Lấy thống kê thật (dùng giá trị DB: da_huy, da_giao)
                List<HoaDon> allUserOrders = (idKhachHang != null) ? hoaDonRepository.findByKhachHang_Id(idKhachHang) : new ArrayList<>();
                long cancelCount = allUserOrders.stream().filter(o -> OrderStatus.DA_HUY.getValue().equals(o.getTrangThaiDonHang())).count();
                long orderCount = allUserOrders.size() - cancelCount;

                modelMap.put("orderCount", orderCount);
                modelMap.put("cancelCount", cancelCount);
                modelMap.put("wishlistCount", 0);

                return modelMap;
            }
        }

        // Không tìm thấy -> Trả về null để Controller xử lý redirect
        return null;
    }

    private String getFrontendStatusLabel(String dbStatus) {
        if (dbStatus == null) {
            return "processing";
        }
        return switch (dbStatus.toLowerCase()) {
            case "da_giao", "delivered", "hoan_thanh" ->
                "delivered";
            case "da_huy", "cancelled" ->
                "cancelled";
            default ->
                "processing";
        };
    }

    // Adapt Real HoaDon to match the dynamic properties expected in template without throwing exceptions
    private Map<String, Object> adaptRealOrder(HoaDon hd) {
        Map<String, Object> map = new HashMap<>();
        LocalDateTime ngayTao = hd.getNgayTao() != null ? hd.getNgayTao() : LocalDateTime.now();
        map.put("id", hd.getId());
        map.put("ngayDat", ngayTao);
        map.put("ngayGiao", ngayTao.plusDays(3)); // Giả sử giao sau 3 ngày
        map.put("phuongThucVanChuyen", hd.getDonViVanChuyen() != null ? hd.getDonViVanChuyen().getTenDonVi() : "Standard Delivery");
        map.put("phiVanChuyen", hd.getPhiVanChuyen() != null ? hd.getPhiVanChuyen() : BigDecimal.ZERO);
        map.put("phuongThucThanhToan", hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan().getTenPhuongThuc() : "COD");
        map.put("trangThaiThanhToan", hd.getTrangThaiThanhToan() != null ? hd.getTrangThaiThanhToan() : "PENDING");
        map.put("tongTien", hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO);
        map.put("trangThaiDonHang", hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang() : "cho_xac_nhan");
        map.put("trangThaiDonHangLabel", getStatusLabel(hd.getTrangThaiDonHang()));
        map.put("status", getFrontendStatusLabel(hd.getTrangThaiDonHang()));
        map.put("ghiChu", hd.getGhiChu() != null ? hd.getGhiChu() : "");
        map.put("paymentMethod", hd.getPaymentMethod() != null ? hd.getPaymentMethod() : "");
        map.put("maDonHang", hd.getMaDonHang() != null ? hd.getMaDonHang() : "HD-" + hd.getId());
        map.put("maGiaoDich", hd.getMaGiaoDich() != null ? hd.getMaGiaoDich() : "");
        map.put("transactionId", hd.getTransactionId() != null ? hd.getTransactionId() : "");
        map.put("ghnOrderCode", hd.getGhnOrderCode() != null ? hd.getGhnOrderCode() : "");
        map.put("ghnStatus", hd.getGhnStatus() != null ? hd.getGhnStatus() : "");
        map.put("ghnStatusLabel", ghnStatusMapper != null ? ghnStatusMapper.getGhnStatusLabel(hd.getGhnStatus()) : "");

        map.put("soTienGiamVoucher", hd.getSoTienGiamVoucher() != null ? hd.getSoTienGiamVoucher() : BigDecimal.ZERO);
        map.put("maVoucherApDung", hd.getMaVoucherApDung() != null ? hd.getMaVoucherApDung() : "");
        map.put("tenVoucherApDung", hd.getTenVoucherApDung() != null ? hd.getTenVoucherApDung() : "");
        map.put("moTaVoucherSnapshot", hd.getMoTaVoucherSnapshot() != null ? hd.getMoTaVoucherSnapshot() : "");

        Map<String, Object> adr = new HashMap<>();
        String hoTen = hd.getTenNguoiNhan();
        if (hoTen == null || hoTen.isBlank()) {
            if (hd.getKhachHang() != null) {
                String ho = hd.getKhachHang().getHoKh() != null ? hd.getKhachHang().getHoKh() : "";
                String ten = hd.getKhachHang().getTenKh() != null ? hd.getKhachHang().getTenKh() : "";
                hoTen = (ho + " " + ten).trim();
            }
            if (hoTen == null || hoTen.isBlank()) {
                hoTen = "Khách Lẻ";
            }
        }
        adr.put("hoTen", hoTen);
        adr.put("diaChiDayDu", hd.getDiaChiNhan() != null ? hd.getDiaChiNhan() : "Tại cửa hàng");
        adr.put("soDienThoai", hd.getSdtNhan() != null ? hd.getSdtNhan() : (hd.getKhachHang() != null && hd.getKhachHang().getSoDienThoaiKh() != null ? hd.getKhachHang().getSoDienThoaiKh() : "N/A"));

        map.put("diaChiGiao", adr);
        map.put("diaChiThanhToan", adr);

        map.put("ngayXacNhan", hd.getThoiGianXacNhan());
        map.put("ngayThanhToan", hd.getPaidAt());

        Map<String, LocalDateTime> transitions = getStatusTransitionTimes(hd.getId());
        map.put("ngayGiaoDVVC", transitions.get("dang_giao") != null ? transitions.get("dang_giao") : (transitions.get("dang_lay_hang") != null ? transitions.get("dang_lay_hang") : null));
        map.put("ngayGiaoThanhCong", transitions.get("da_giao"));
        map.put("ngayHuy", transitions.get("da_huy"));

        ReturnStatus returnStatus = resolveReturnStatus(hd.getId(), hd);
        map.put("trangThaiHoanHang", returnStatus != null ? returnStatus.name() : null);
        map.put("trangThaiHoanHangLabel", returnStatus != null ? returnStatus.getLabel() : "");
        map.put("lyDoHoanTien", hd.getLyDoHoanTien() != null ? hd.getLyDoHoanTien() : "");
        map.put("ghnReturnOrderCode", resolveGhnReturnOrderCode(hd.getId(), hd));

        Map<String, String> refundDetails = resolveRefundDetails(hd.getId(), hd);
        map.put("phuongThucHoanTien", refundDetails.getOrDefault("phuongThucHoanTien", hd.getPhuongThucHoanTien() != null ? hd.getPhuongThucHoanTien() : ""));
        map.put("soTienHoan", refundDetails.getOrDefault("soTienHoan", hd.getSoTienHoan() != null ? hd.getSoTienHoan().toString() : (hd.getTongTien() != null ? hd.getTongTien().toString() : "0")));
        map.put("maGiaoDichHoanTien", refundDetails.getOrDefault("maGiaoDichHoanTien", hd.getMaGiaoDichHoanTien() != null ? hd.getMaGiaoDichHoanTien() : ""));
        map.put("ghiChuHoanTien", refundDetails.getOrDefault("ghiChuHoanTien", hd.getGhiChuHoanTien() != null ? hd.getGhiChuHoanTien() : ""));
        map.put("anhChungTuHoanTien", refundDetails.getOrDefault("anhChungTuHoanTien", hd.getAnhChungTuHoanTien() != null ? hd.getAnhChungTuHoanTien() : ""));
        map.put("thoiGianHoanTien", refundDetails.getOrDefault("thoiGianHoanTien", hd.getThoiGianHoanTien() != null ? hd.getThoiGianHoanTien().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")) : ""));
        map.put("nguoiThucHienHoanTien", refundDetails.getOrDefault("nguoiThucHienHoanTien", hd.getNguoiThucHienHoanTien() != null ? hd.getNguoiThucHienHoanTien() : ""));
        map.put("daNhapKhoHoan", isDaNhapKhoHoan(hd.getId(), hd));
        map.put("loaiYeuCauDoiTra", hd.getLoaiYeuCauDoiTra() != null ? hd.getLoaiYeuCauDoiTra() : "");
        map.put("bangChungHoanTra", hd.getBangChungHoanTra() != null ? hd.getBangChungHoanTra() : "");
        map.put("isCod", hd.isCod());
        map.put("isPrepaid", hd.isPrepaid());

        return map;
    }

    @Transactional
    public boolean huyDonHang(Integer idHoaDon, Integer idKhachHang, String clientIp) {
        return huyDonHang(idHoaDon, idKhachHang, clientIp, null);
    }

    @Transactional
    public boolean huyDonHang(Integer idHoaDon, Integer idKhachHang, String clientIp, String lyDoHuy) {
        Optional<HoaDon> hdOpt = hoaDonRepository.findByIdWithLock(idHoaDon);
        if (hdOpt.isPresent()) {
            HoaDon hd = hdOpt.get();
            // Xác thực đơn hàng thuộc về đúng khách hàng đang đăng nhập
            if (!hd.getKhachHang().getId().equals(idKhachHang)) {
                return false;
            }

            // Chỉ cho phép hủy đơn ở trạng thái cho_thanh_toan, cho_xac_nhan hoặc da_xac_nhan
            String currentStatus = hd.getTrangThaiDonHang();
            if (OrderStatus.CHO_THANH_TOAN.getValue().equals(currentStatus)
                    || OrderStatus.CHO_XAC_NHAN.getValue().equals(currentStatus)
                    || OrderStatus.DA_XAC_NHAN.getValue().equals(currentStatus)) {

                // Khôi phục lại kho cho các biến thể sản phẩm nếu đã từng bị trừ kho (chỉ khi đơn đã xác nhận trở đi hoặc đơn đã thanh toán trước ở cho_xac_nhan)
                boolean stockWasDeducted = isStockDeductedState(hd, currentStatus);

                if (stockWasDeducted && (hd.getDaNhapKhoHoan() == null || !hd.getDaNhapKhoHoan())) {
                    List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
                    List<RestockItemRequest> restockReqs = new ArrayList<>();
                    for (HoaDonChiTiet item : items) {
                        if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                            restockReqs.add(RestockItemRequest.builder()
                                    .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                                    .quantityToRestock(item.getSoLuong())
                                    .conBanDuoc(true)
                                    .build());
                        }
                    }
                    if (!restockReqs.isEmpty()) {
                        inventoryLotService.hoanKho(restockReqs);
                        log.info("[OrderViewService] Đã hoàn kho FIFO cho {} sản phẩm của đơn #{} khi khách hàng hủy", restockReqs.size(), idHoaDon);
                    }
                    hd.setDaNhapKhoHoan(true);
                }

                // COD consumes at checkout; prepaid orders consume only after successful payment.
                if ((hd.isCod() || isOrderPaid(hd))
                        && hd.getPhieuGiamGia() != null && hd.getPhieuGiamGia().getMaPhieu() != null) {
                    String maPhieu = hd.getPhieuGiamGia().getMaPhieu();
                    try {
                        phieuGiamGiaRepository.findByMaPhieuWithLock(maPhieu).ifPresent(voucher -> {
                            Integer remaining = voucher.getSoLuongConLai();
                            if (remaining != null) {
                                voucher.setSoLuongConLai(remaining + 1);
                                phieuGiamGiaRepository.save(voucher);
                                log.info("[OrderViewService] Đã hoàn lại 1 lượt cho voucher '{}' khi hủy đơn #{}: {} -> {}",
                                        maPhieu, idHoaDon, remaining, remaining + 1);
                            }
                        });
                    } catch (Exception vEx) {
                        log.error("[OrderViewService] Lỗi hoàn lại voucher '{}' khi hủy đơn #{}: {}", maPhieu, idHoaDon, vEx.getMessage());
                    }
                }

                // Cập nhật trạng thái đơn hàng
                hd.setTrangThaiDonHang(OrderStatus.DA_HUY.getValue()); // "da_huy"
                String refundLogNote = "";

                String standardizedReason = "Khách hàng yêu cầu hủy";
                if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                    String trimmed = lyDoHuy.trim();
                    String sanitized = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
                    if (sanitized.length() > 500) {
                        throw new IllegalArgumentException("Lý do hủy không được vượt quá 500 ký tự.");
                    }
                    if (!sanitized.isEmpty()) {
                        standardizedReason = sanitized;
                    }
                }

                String addition = "Lý do hủy: " + standardizedReason;
                String currentGhiChu = hd.getGhiChu();
                if (currentGhiChu == null || currentGhiChu.trim().isEmpty()) {
                    hd.setGhiChu(addition.length() > 500 ? addition.substring(0, 500) : addition);
                } else {
                    String newGhiChu = currentGhiChu + "\n" + addition;
                    hd.setGhiChu(newGhiChu.length() > 500 ? newGhiChu.substring(0, 500) : newGhiChu);
                }
                hd.setLyDoHuy(standardizedReason);

                if (isOrderPaid(hd)) {
                    if (!"DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan()) && !"REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan())) {
                        hd.setPaymentStatus("paid");
                        hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                    }
                    String pm = hd.getPaymentMethod();
                    boolean isPrepaid = (pm != null && !pm.equalsIgnoreCase("COD") && !pm.equalsIgnoreCase("cod"))
                            || (hd.getPhuongThucThanhToan() != null && !"COD".equalsIgnoreCase(hd.getPhuongThucThanhToan().getTenPhuongThuc()));
                    if (isPrepaid || isOrderPaid(hd)) {
                        hd.setRefundStatus(RefundStatus.PENDING);
                    }
                    refundLogNote = String.format(" [REFUND_REQUIRED] orderId=%d, paymentMethod=%s, paidAmount=%s, cancellationTime=%s, customerId=%d",
                            hd.getId(), hd.getPaymentMethod(), hd.getTongTien().toString(), LocalDateTime.now().toString(), idKhachHang);
                    hoaDonRepository.save(hd);
                    try {
                        guiEmailYeuCauHoanTien(hd, standardizedReason);
                    } catch (Exception e) {
                        log.error("Lỗi gửi email yêu cầu hoàn tiền khi khách hàng hủy đơn: {}", e.getMessage());
                    }
                } else {
                    hd.setPaymentStatus("CANCELLED");
                    hd.setTrangThaiThanhToan("HUY");
                    hoaDonRepository.save(hd);
                }

                // Ghi nhận Audit Log
                auditService.log(
                        hd.getKhachHang().getTaiKhoan() != null ? hd.getKhachHang().getTaiKhoan().getId() : null,
                        "HoaDon",
                        Long.valueOf(hd.getId()),
                        "UPDATE",
                        currentStatus,
                        "da_huy",
                        clientIp,
                        "[CUSTOMER_CANCEL] Khách hàng tự hủy đơn hàng từ trang chi tiết." + refundLogNote,
                        "CUSTOMER"
                );

                return true;
            }
        }
        return false;
    }

    public List<String> getValidNextStatuses(String currentStatus) {
        if (currentStatus == null) {
            return List.of();
        }
        return switch (currentStatus.toLowerCase()) {
            case "cho_thanh_toan" ->
                List.of("cho_xac_nhan", "da_huy");
            case "cho_xac_nhan" ->
                List.of("da_xac_nhan", "da_huy");
            case "da_xac_nhan" ->
                List.of("dang_chuan_bi_hang", "dang_lay_hang", "dang_giao", "da_huy");
            case "dang_chuan_bi_hang" ->
                List.of("san_sang_giao", "da_huy");
            case "san_sang_giao" ->
                List.of("da_tao_van_don_ghn", "da_huy");
            case "da_tao_van_don_ghn" ->
                List.of("da_ban_giao_ghn", "da_huy");
            case "da_ban_giao_ghn" ->
                List.of();
            case "dang_lay_hang" ->
                List.of("dang_giao", "da_huy");
            case "dang_giao" ->
                List.of("da_giao", "giao_that_bai", "da_huy");
            case "giao_that_bai" ->
                List.of("dang_giao", "da_giao", "da_huy");
            case "stock_conflict" ->
                List.of("cho_xac_nhan", "da_huy");
            default ->
                List.of();
        };
    }

    public String getStatusLabel(String status) {
        if (status == null) {
            return "N/A";
        }
        return switch (status.toLowerCase()) {
            case "cho_thanh_toan" ->
                "Chờ thanh toán";
            case "cho_xac_nhan" ->
                "Chờ xác nhận";
            case "da_xac_nhan" ->
                "Đã xác nhận";
            case "dang_chuan_bi_hang" ->
                "Đang chuẩn bị hàng";
            case "san_sang_giao" ->
                "Sẵn sàng giao";
            case "da_tao_van_don_ghn" ->
                "Đã tạo đơn vận chuyển";
            case "da_ban_giao_ghn" ->
                "Đã lấy hàng";
            case "dang_lay_hang" ->
                "Đang lấy hàng";
            case "dang_giao" ->
                "Đang giao hàng";
            case "giao_that_bai" ->
                "Giao hàng thất bại (Khách hẹn giao lại/Chờ chuyển hoàn)";
            case "da_giao" ->
                "Đã giao";
            case "da_huy" ->
                "Đã hủy";
            case "stock_conflict" ->
                "Trùng kho";
            default ->
                status;
        };
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void updateOrderStatusByAdmin(Integer idHoaDon, String newStatus, String expectedStatus, Integer actingTaiKhoanId, String clientIp) {
        updateOrderStatusByAdmin(idHoaDon, newStatus, expectedStatus, actingTaiKhoanId, clientIp, null);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void updateOrderStatusByAdmin(Integer idHoaDon, String newStatus, String expectedStatus, Integer actingTaiKhoanId, String clientIp, String lyDoHuy) {
        // 1. Service-Level Authorization
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái đơn hàng.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));

        String role = actingUser.getVaiTro();
        if (!"NV".equals(role) && !"QL".equals(role)) {
            throw new AccessDeniedException("Bạn không có quyền cập nhật trạng thái đơn hàng.");
        }

        // 2. Load order
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        String currentStatus = hd.getTrangThaiDonHang();
        String currentPaymentStatus = hd.getPaymentStatus();
        String currentTrangThaiThanhToan = hd.getTrangThaiThanhToan();

        // 3. Lost Update Protection
        if (!Objects.equals(currentStatus, expectedStatus)) {
            throw new IllegalStateException("Đơn hàng đã được người khác cập nhật. Vui lòng tải lại trang.");
        }

        boolean hasGhnCode = (hd.getGhnOrderCode() != null && !hd.getGhnOrderCode().trim().isEmpty())
                || (hd.getGhnReturnOrderCode() != null && !hd.getGhnReturnOrderCode().trim().isEmpty());

        // 4. Delivered, Completed, Handed over to GHN, and Cancelled orders are immutable for manual admin update
        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(currentStatus)
                || OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)
                || OrderStatus.DA_BAN_GIAO_GHN.getValue().equalsIgnoreCase(currentStatus)
                || "dang_giao".equalsIgnoreCase(currentStatus)
                || "hoan_thanh".equalsIgnoreCase(currentStatus)
                || (hasGhnCode && ("dang_lay_hang".equalsIgnoreCase(currentStatus)
                || "da_tao_van_don_ghn".equalsIgnoreCase(currentStatus)
                || "san_sang_giao".equalsIgnoreCase(currentStatus)
                || "da_ban_giao_ghn".equalsIgnoreCase(currentStatus)
                || "giao_that_bai".equalsIgnoreCase(currentStatus)))) {
            throw new IllegalArgumentException("Đơn hàng đã được giao cho đơn vị vận chuyển GHN (Mã: "
                    + (hd.getGhnOrderCode() != null ? hd.getGhnOrderCode() : "")
                    + "). Trạng thái vận chuyển được đồng bộ tự động từ GHN và không thể chuyển thủ công trên hệ thống!");
        }

        // 5. If status is the same, no transition is needed
        if (Objects.equals(currentStatus, newStatus)) {
            return;
        }

        // 6. Transition Matrix Validation
        List<String> validNext = getValidNextStatuses(currentStatus);
        if (!validNext.contains(newStatus)) {
            throw new IllegalArgumentException(String.format("Không thể chuyển trạng thái từ '%s' sang '%s'.", getStatusLabel(currentStatus), getStatusLabel(newStatus)));
        }

        // 7. Inventory State tracking
        boolean oldIsDeducted = isStockDeductedState(hd, currentStatus);
        boolean newIsDeducted = isStockDeductedState(hd, newStatus);

        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);

        if (!oldIsDeducted && newIsDeducted) {
            List<OrderItemRequest> reqs = new ArrayList<>();
            for (HoaDonChiTiet item : items) {
                if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                    reqs.add(OrderItemRequest.builder()
                            .representativeSpctId(item.getSanPhamChiTiet().getId())
                            .quantity(item.getSoLuong())
                            .build());
                }
            }
            if (!reqs.isEmpty()) {
                com.smashvn.shop.dto.inventory.AllocationResult allocResult = inventoryLotService.allocateFifo(reqs);
                if (allocResult != null && allocResult.status() == com.smashvn.shop.dto.inventory.AllocationStatus.INSUFFICIENT_STOCK) {
                    throw new IllegalArgumentException(allocResult.message() != null ? allocResult.message() : "Sản phẩm không đủ hàng tồn kho để kích hoạt đơn hàng!");
                }
            }
        } else if (oldIsDeducted && !newIsDeducted) {
            boolean restoreStock = true;
            if ("dang_giao".equalsIgnoreCase(currentStatus) || "dang_lay_hang".equalsIgnoreCase(currentStatus) || "da_ban_giao_ghn".equalsIgnoreCase(currentStatus)) {
                restoreStock = false;
                hd.setTrangThaiHoanHang(ReturnStatus.PENDING_RETURN);
            }
            if (restoreStock && (hd.getDaNhapKhoHoan() == null || !hd.getDaNhapKhoHoan())) {
                List<RestockItemRequest> restockReqs = new ArrayList<>();
                for (HoaDonChiTiet item : items) {
                    if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                        restockReqs.add(RestockItemRequest.builder()
                                .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                                .quantityToRestock(item.getSoLuong())
                                .conBanDuoc(true)
                                .build());
                    }
                }
                if (!restockReqs.isEmpty()) {
                    inventoryLotService.hoanKho(restockReqs);
                    log.info("[OrderViewService] Đã hoàn kho FIFO cho {} sản phẩm của đơn #{} khi admin đổi trạng thái", restockReqs.size(), idHoaDon);
                }
                hd.setDaNhapKhoHoan(true);
            }
        }

        // 8. Payment Method Logic & Cancellation Rules
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";
        String refundLogNote = "";

        if (OrderStatus.DA_XAC_NHAN.getValue().equalsIgnoreCase(newStatus)) {
            if (hd.getThoiGianXacNhan() == null) {
                hd.setThoiGianXacNhan(LocalDateTime.now());
            }
        }

        if (OrderStatus.DA_TAO_VAN_DON_GHN.getValue().equalsIgnoreCase(newStatus)) {
            if (hd.getGhnOrderCode() == null || hd.getGhnOrderCode().isBlank()) {
                throw new IllegalStateException("Đơn hàng chưa có mã vận đơn GHN. Vui lòng bấm 'Gửi đơn sang GHN' để tạo vận đơn trước khi chuyển sang trạng thái này.");
            }
        }

        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(newStatus)) {
            // Explicit check: COD payments set paid when delivered
            if ("COD".equalsIgnoreCase(hd.getPaymentMethod())) {
                hd.setPaymentStatus(PaymentStatus.PAID.getValue());
                hd.setTrangThaiThanhToan("DA_THANH_TOAN");
                if (hd.getPaidAt() == null) {
                    hd.setPaidAt(LocalDateTime.now());
                }
                if (hd.getThoiGianXacNhan() == null) {
                    hd.setThoiGianXacNhan(LocalDateTime.now());
                }
            }
        } else if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus)) {
            String standardizedReason = "Khách hàng yêu cầu hủy";
            if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                String trimmed = lyDoHuy.trim();
                String sanitized = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
                if (sanitized.length() > 500) {
                    throw new IllegalArgumentException("Lý do hủy không được vượt quá 500 ký tự.");
                }
                if (!sanitized.isEmpty()) {
                    standardizedReason = sanitized;
                }
            }

            String pm = hd.getPaymentMethod();
            boolean isPrepaid = (pm != null && !pm.equalsIgnoreCase("COD") && !pm.equalsIgnoreCase("cod"))
                    || (hd.getPhuongThucThanhToan() != null && !"COD".equalsIgnoreCase(hd.getPhuongThucThanhToan().getTenPhuongThuc()));

            if (isOrderPaid(hd)) {
                if (!"DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan()) && !"REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan())) {
                    hd.setPaymentStatus("paid");
                    hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                }
                if (isPrepaid || isOrderPaid(hd)) {
                    hd.setRefundStatus(RefundStatus.PENDING);
                }
                refundLogNote = String.format(" [REFUND_REQUIRED] orderId=%d, paymentMethod=%s, paidAmount=%s, cancellationTime=%s, actingUserId=%d",
                        hd.getId(), hd.getPaymentMethod(), hd.getTongTien().toString(), LocalDateTime.now().toString(), actingTaiKhoanId);
            } else {
                hd.setPaymentStatus("CANCELLED");
                hd.setTrangThaiThanhToan("HUY");
            }

            String addition = "Lý do hủy: " + standardizedReason;
            String currentGhiChu = hd.getGhiChu();
            if (currentGhiChu == null || currentGhiChu.trim().isEmpty()) {
                hd.setGhiChu(addition.length() > 500 ? addition.substring(0, 500) : addition);
            } else {
                String newGhiChu = currentGhiChu + "\n" + addition;
                hd.setGhiChu(newGhiChu.length() > 500 ? newGhiChu.substring(0, 500) : newGhiChu);
            }
            hd.setLyDoHuy(standardizedReason);

            // Hoàn lại số lượng voucher nếu đơn hàng có áp dụng voucher
            if (hd.getPhieuGiamGia() != null && hd.getPhieuGiamGia().getMaPhieu() != null && !OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
                String maPhieu = hd.getPhieuGiamGia().getMaPhieu();
                try {
                    phieuGiamGiaRepository.findByMaPhieuWithLock(maPhieu).ifPresent(voucher -> {
                        Integer remaining = voucher.getSoLuongConLai();
                        if (remaining != null) {
                            voucher.setSoLuongConLai(remaining + 1);
                            phieuGiamGiaRepository.save(voucher);
                            log.info("[OrderViewService] Admin hủy đơn #{}, đã hoàn lại 1 lượt cho voucher '{}': {} -> {}",
                                    idHoaDon, maPhieu, remaining, remaining + 1);
                        }
                    });
                } catch (Exception vEx) {
                    log.error("[OrderViewService] Lỗi hoàn lại voucher '{}' khi admin hủy đơn #{}: {}", maPhieu, idHoaDon, vEx.getMessage());
                }
            }
        }

        // 9. Update Order
        hd.setTrangThaiDonHang(newStatus);
        hd = hoaDonRepository.save(hd);

        // Generate notification for customer (only when status actually changes)
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null && !Objects.equals(currentStatus, newStatus)) {
            try {
                String maDon = hd.getMaDonHang() != null ? hd.getMaDonHang() : "SMASH-" + hd.getId();
                String title = getCustomerOrderTitle(newStatus);
                String msgContent = buildCustomerOrderNotificationContent(maDon, currentStatus, newStatus);

                ThongBao thongBao = ThongBao.builder()
                        .taiKhoan(hd.getKhachHang().getTaiKhoan())
                        .tieuDe(title)
                        .noiDung(msgContent)
                        .daDoc(false)
                        .loaiThongBao("don_hang")
                        .ngayTao(LocalDateTime.now())
                        .build();
                thongBaoRepository.save(thongBao);
            } catch (Exception e) {
                log.error("Lỗi tạo thông báo trạng thái đơn hàng cho khách: {}", e.getMessage());
            }
        }

        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus) && "CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            String standardizedReason = "Khách hàng yêu cầu hủy";
            if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                standardizedReason = lyDoHuy.trim();
            }
            try {
                guiEmailYeuCauHoanTien(hd, standardizedReason);
            } catch (Exception e) {
                log.error("Lỗi gửi email yêu cầu hoàn tiền khi admin hủy đơn: {}", e.getMessage());
            }
        }

        // 10. Audit Log Enhancement
        String giaTriCu = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s, trangThaiHoanHang=%s",
                currentStatus, currentPaymentStatus, currentTrangThaiThanhToan,
                "NULL", "NULL");
        String giaTriMoi = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s, trangThaiHoanHang=%s",
                newStatus, hd.getPaymentStatus(), hd.getTrangThaiThanhToan(),
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL",
                hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "NULL");
        String ghiChu = String.format("[ADMIN_UPDATE] Trạng thái đơn hàng được cập nhật bởi %s.%s", roleStr, refundLogNote);

        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                giaTriCu,
                giaTriMoi,
                clientIp,
                ghiChu,
                actingUser.getVaiTro() != null ? actingUser.getVaiTro() : roleStr
        );
    }

    /**
     * Mô phỏng chuyển trạng thái kế tiếp cho vận đơn GHN Fallback (Demo
     * Simulator). Chỉ áp dụng cho đơn hàng có bản ghi provider = 'GHN_FALLBACK'
     * trong DB TichHopVanChuyen.
     */
    @Transactional
    public Map<String, Object> advanceDemoShippingStatus(Integer idHoaDon, Integer actorAccountId, String actorName, String clientIp) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        // 1. Kiểm tra trong DB TichHopVanChuyen xem có đúng provider GHN_FALLBACK hay không
        String sql = "SELECT TOP 1 nha_cung_cap, ma_van_don, trang_thai FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_FALLBACK' ORDER BY id DESC";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, idHoaDon);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng #" + idHoaDon + " không có vận đơn Demo Fallback (GHN_FALLBACK) để mô phỏng.");
        }

        Map<String, Object> record = rows.get(0);
        String provider = (String) record.get("nha_cung_cap");
        String orderCode = (String) record.get("ma_van_don");
        String dbGhnStatus = (String) record.get("trang_thai");

        if (!"GHN_FALLBACK".equalsIgnoreCase(provider) || orderCode == null || !orderCode.startsWith("DEMO-GHN-")) {
            throw new IllegalArgumentException("Đơn hàng #" + idHoaDon + " không phải là vận đơn Demo Fallback hợp lệ.");
        }

        String currentGhnStatus = dbGhnStatus != null ? dbGhnStatus : (hd.getGhnStatus() != null ? hd.getGhnStatus() : "ready_to_pick");

        // 2. State Machine xác định trạng thái kế tiếp trong luồng Demo (Forward-only)
        String nextGhnStatus;
        switch (currentGhnStatus.toLowerCase()) {
            case "ready_to_pick":
                nextGhnStatus = "picking";
                break;
            case "picking":
            case "money_collect_picking":
            case "picked":
                nextGhnStatus = "transporting";
                break;
            case "storing":
            case "sorting":
            case "transporting":
                nextGhnStatus = "delivering";
                break;
            case "delivering":
            case "money_collect_delivering":
                nextGhnStatus = "delivered";
                break;
            case "delivered":
                throw new IllegalStateException("Vận đơn Demo (" + orderCode + ") đã ở trạng thái Giao hàng thành công (delivered), không thể chuyển tiếp.");
            default:
                if (ghnStatusMapper.isTerminalGhnStatus(currentGhnStatus)) {
                    throw new IllegalStateException("Vận đơn Demo (" + orderCode + ") đã ở trạng thái kết thúc (" + currentGhnStatus + "), không thể chuyển tiếp.");
                } else {
                    throw new IllegalArgumentException("Trạng thái vận đơn hiện tại (" + currentGhnStatus + ") không nằm trong luồng mô phỏng Demo.");
                }
        }

        // 3. Map sang trạng thái đơn hàng nội bộ qua GhnStatusMapper
        String internalStatus = ghnStatusMapper.mapToInternalStatus(nextGhnStatus);
        if (internalStatus == null) {
            internalStatus = hd.getTrangThaiDonHang();
        }

        // 4. Compare-and-Set SQL update để chống double-click / concurrent transitions
        String updateSql = "UPDATE TichHopVanChuyen SET trang_thai = ? WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_FALLBACK' AND ma_van_don = ? AND (trang_thai = ? OR (trang_thai IS NULL AND ? = 'ready_to_pick'))";
        int rowsUpdated = jdbcTemplate.update(updateSql, nextGhnStatus, idHoaDon, orderCode, currentGhnStatus, currentGhnStatus);
        if (rowsUpdated != 1) {
            throw new IllegalStateException("Trạng thái vận đơn đã bị thay đổi bởi một thao tác khác hoặc không còn hợp lệ. Vui lòng làm mới trang.");
        }

        // 5. Cập nhật HoaDon và thực thi các side-effects (Stock, Payment, ThongBao, EditLog duy nhất)
        String actor = actorName != null ? actorName : "Admin";
        String demoLogNote = String.format("[GHN_DEMO_SIMULATOR] Admin %s chuyển trạng thái vận chuyển Demo: %s -> %s. Mã vận đơn: %s",
                actor, currentGhnStatus, nextGhnStatus, orderCode);

        applyShippingStatus(idHoaDon, internalStatus, nextGhnStatus, actorAccountId, actor, demoLogNote, clientIp);

        log.info("[GHN_DEMO_SIMULATOR] Đơn #{}: Chuyển trạng thái Demo thành công: GHN ({}) -> ({}), Nội bộ ({}) -> ({})",
                idHoaDon, currentGhnStatus, nextGhnStatus, hd.getTrangThaiDonHang(), internalStatus);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("orderId", idHoaDon);
        response.put("ghnOrderCode", orderCode);
        response.put("ghnStatus", nextGhnStatus);
        response.put("ghnStatusLabel", ghnStatusMapper.getGhnStatusLabel(nextGhnStatus));
        response.put("trangThaiDonHang", internalStatus);
        response.put("message", "Đã chuyển trạng thái Demo thành công: " + ghnStatusMapper.getGhnStatusLabel(nextGhnStatus));
        return response;
    }

    @Transactional
    public void applyShippingStatus(Integer idHoaDon, String newStatus, String ghnStatus) {
        applyShippingStatus(idHoaDon, newStatus, ghnStatus, null, "SYSTEM", null, "127.0.0.1");
    }

    @Transactional
    public void applyShippingStatus(Integer idHoaDon, String newStatus, String ghnStatus, Integer actorAccountId, String actorName, String customLogNote, String clientIp) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        String currentStatus = hd.getTrangThaiDonHang();
        String currentPaymentStatus = hd.getPaymentStatus();
        String currentTrangThaiThanhToan = hd.getTrangThaiThanhToan();

        // Ngăn chặn việc đảo ngược trạng thái đơn hàng từ các trạng thái cuối (Terminal States)
        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
            log.info("[GHN_WEBHOOK] Bỏ qua cập nhật trạng thái cho đơn hàng đã HỦY #{}. Trạng thái mục tiêu: {}", idHoaDon, newStatus);
            if (!Objects.equals(hd.getGhnStatus(), ghnStatus)) {
                hd.setGhnStatus(ghnStatus);
                hoaDonRepository.save(hd);
            }
            return;
        }

        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(currentStatus)) {
            if (!OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(newStatus) && !OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus)) {
                log.info("[GHN_WEBHOOK] Bỏ qua việc đảo ngược trạng thái từ ĐÃ GIAO về active cho đơn #{}. Trạng thái mục tiêu: {}", idHoaDon, newStatus);
                if (!Objects.equals(hd.getGhnStatus(), ghnStatus)) {
                    hd.setGhnStatus(ghnStatus);
                    hoaDonRepository.save(hd);
                }
                return;
            }
        }

        // Determine target return status if transitioning to da_huy (or already da_huy)
        ReturnStatus targetReturnStatus = hd.getTrangThaiHoanHang();
        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus)) {
            if ("dang_giao".equalsIgnoreCase(currentStatus) || "dang_lay_hang".equalsIgnoreCase(currentStatus) || OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
                if ("lost".equalsIgnoreCase(ghnStatus)) {
                    targetReturnStatus = ReturnStatus.LOST;
                } else if ("damage".equalsIgnoreCase(ghnStatus)) {
                    targetReturnStatus = ReturnStatus.DAMAGED;
                } else if ("cancel".equalsIgnoreCase(ghnStatus) || "return".equalsIgnoreCase(ghnStatus) || "exception".equalsIgnoreCase(ghnStatus)) {
                    targetReturnStatus = ReturnStatus.PENDING_RETURN;
                } else {
                    if (targetReturnStatus == null) {
                        targetReturnStatus = ReturnStatus.PENDING_RETURN;
                    }
                }
            }
        }

        if (Objects.equals(currentStatus, newStatus)) {
            // Duplicate webhook / same status check
            if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
                if (Objects.equals(hd.getTrangThaiHoanHang(), targetReturnStatus)) {
                    return; // Duplicate webhook, skip completely
                } else {
                    // Update return status only
                    hd.setTrangThaiHoanHang(targetReturnStatus);
                    hd.setGhnStatus(ghnStatus);
                    hd = hoaDonRepository.save(hd);

                    // Audit Log for return status update
                    String giaTriCu = String.format("status=%s, trangThaiHoanHang=%s", currentStatus, hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "NULL");
                    String giaTriMoi = String.format("status=%s, trangThaiHoanHang=%s", newStatus, targetReturnStatus != null ? targetReturnStatus.name() : "NULL");
                    String ghiChuLog = String.format("[GHN_WEBHOOK] Cập nhật trạng thái hoàn hàng từ webhook GHN. Mã vận đơn: %s, Trạng thái GHN: %s, Trạng thái hoàn hàng mới: %s", hd.getGhnOrderCode(), ghnStatus, targetReturnStatus != null ? targetReturnStatus.name() : "NULL");
                    auditService.log(
                            actorAccountId,
                            "HoaDon",
                            Long.valueOf(hd.getId()),
                            "UPDATE",
                            giaTriCu,
                            giaTriMoi,
                            clientIp != null ? clientIp : "127.0.0.1",
                            ghiChuLog,
                            actorName != null ? actorName : "SYSTEM"
                    );
                }
            } else {
                // Cập nhật trạng thái GHN nếu có thay đổi mà không thay đổi trạng thái đơn hàng
                if (!Objects.equals(hd.getGhnStatus(), ghnStatus)) {
                    hd.setGhnStatus(ghnStatus);
                    hoaDonRepository.save(hd);
                }
            }
            return;
        }

        // Cập nhật trạng thái GHN
        hd.setGhnStatus(ghnStatus);

        // Inventory State tracking
        boolean oldIsDeducted = isStockDeductedState(hd, currentStatus);
        boolean newIsDeducted = isStockDeductedState(hd, newStatus);

        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);

        boolean restoreStock = false;
        if (!oldIsDeducted && newIsDeducted) {
            List<OrderItemRequest> reqs = new ArrayList<>();
            for (HoaDonChiTiet item : items) {
                if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                    reqs.add(OrderItemRequest.builder()
                            .representativeSpctId(item.getSanPhamChiTiet().getId())
                            .quantity(item.getSoLuong())
                            .build());
                }
            }
            if (!reqs.isEmpty()) {
                com.smashvn.shop.dto.inventory.AllocationResult allocResult = inventoryLotService.allocateFifo(reqs);
                if (allocResult != null && allocResult.status() == com.smashvn.shop.dto.inventory.AllocationStatus.INSUFFICIENT_STOCK) {
                    throw new IllegalArgumentException(allocResult.message() != null ? allocResult.message() : "Sản phẩm không đủ hàng tồn kho để kích hoạt đơn hàng!");
                }
            }
        } else if (oldIsDeducted && !newIsDeducted) {
            if ("dang_giao".equalsIgnoreCase(currentStatus) || "dang_lay_hang".equalsIgnoreCase(currentStatus) || "da_ban_giao_ghn".equalsIgnoreCase(currentStatus)) {
                restoreStock = false;
                hd.setTrangThaiHoanHang(targetReturnStatus);
            } else {
                restoreStock = true;
            }
        }

        if (restoreStock && (hd.getDaNhapKhoHoan() == null || !hd.getDaNhapKhoHoan())) {
            List<RestockItemRequest> restockReqs = new ArrayList<>();
            for (HoaDonChiTiet item : items) {
                if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                    restockReqs.add(RestockItemRequest.builder()
                            .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                            .quantityToRestock(item.getSoLuong())
                            .conBanDuoc(true)
                            .build());
                }
            }
            if (!restockReqs.isEmpty()) {
                inventoryLotService.hoanKho(restockReqs);
                log.info("[OrderViewService] Đã hoàn kho FIFO cho {} sản phẩm của đơn #{} khi chuyển trạng thái kế tiếp", restockReqs.size(), idHoaDon);
            }
            hd.setDaNhapKhoHoan(true);
        }

        // Cập nhật trạng thái thanh toán và refund status
        String pm = hd.getPaymentMethod();
        boolean isPrepaid = (pm != null && !pm.equalsIgnoreCase("COD") && !pm.equalsIgnoreCase("cod"))
                || (hd.getPhuongThucThanhToan() != null && !"COD".equalsIgnoreCase(hd.getPhuongThucThanhToan().getTenPhuongThuc()));

        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(newStatus)) {
            hd.setPaymentStatus(PaymentStatus.PAID.getValue());
            hd.setTrangThaiThanhToan("DA_THANH_TOAN");
            if (hd.getPaidAt() == null) {
                hd.setPaidAt(LocalDateTime.now());
            }
            if (hd.getThoiGianXacNhan() == null) {
                hd.setThoiGianXacNhan(LocalDateTime.now());
            }
        } else if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus)) {
            if (isOrderPaid(hd)) {
                if (!"DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan()) && !"REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan())) {
                    hd.setPaymentStatus("paid");
                    hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
                }
                if (isPrepaid || isOrderPaid(hd)) {
                    hd.setRefundStatus(RefundStatus.PENDING);
                }
            } else {
                hd.setPaymentStatus("CANCELLED");
                hd.setTrangThaiThanhToan("HUY");
            }
        }

        hd.setTrangThaiDonHang(newStatus);
        hd = hoaDonRepository.save(hd);

        // Generate notification for customer from GHN Webhook update (only when status actually changes)
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null && !Objects.equals(currentStatus, newStatus)) {
            try {
                String maDon = hd.getMaDonHang() != null ? hd.getMaDonHang() : "SMASH-" + hd.getId();
                String title = getCustomerOrderTitle(newStatus);
                String msgContent = buildCustomerOrderNotificationContent(maDon, currentStatus, newStatus);

                ThongBao thongBao = ThongBao.builder()
                        .taiKhoan(hd.getKhachHang().getTaiKhoan())
                        .tieuDe(title)
                        .noiDung(msgContent)
                        .daDoc(false)
                        .loaiThongBao("don_hang")
                        .ngayTao(LocalDateTime.now())
                        .build();
                thongBaoRepository.save(thongBao);
            } catch (Exception e) {
                log.error("Lỗi tạo thông báo trạng thái giao hàng GHN cho khách: {}", e.getMessage());
            }
        }

        // Gửi email nếu hủy đơn và trạng thái là chờ hoàn tiền
        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(newStatus) && "CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            try {
                guiEmailYeuCauHoanTien(hd, "Hủy tự động qua webhook GHN do lỗi vận chuyển hoặc khách hàng từ chối nhận (Trạng thái GHN: " + ghnStatus + ")");
            } catch (Exception e) {
                // log error but do not rollback
            }
        }

        // Audit Log
        String giaTriCu = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s, trangThaiHoanHang=%s",
                currentStatus, currentPaymentStatus, currentTrangThaiThanhToan,
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL",
                hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "NULL");

        String giaTriMoi = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s, trangThaiHoanHang=%s",
                newStatus, hd.getPaymentStatus(), hd.getTrangThaiThanhToan(),
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL",
                hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().name() : "NULL");

        String ghiChuLog = customLogNote != null ? customLogNote : String.format("[GHN_WEBHOOK] Cập nhật trạng thái tự động từ webhook GHN. Mã vận đơn: %s, Trạng thái GHN: %s", hd.getGhnOrderCode(), ghnStatus);
        String actor = actorName != null ? actorName : "SYSTEM";
        String ip = clientIp != null ? clientIp : "127.0.0.1";

        auditService.log(
                actorAccountId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                giaTriCu,
                giaTriMoi,
                ip,
                ghiChuLog,
                actor
        );
    }

    public boolean isStockDeductedState(String status) {
        return isStockDeductedState(null, status);
    }

    public boolean isStockDeductedState(HoaDon hd, String status) {
        if (status == null) {
            return false;
        }
        String lower = status.toLowerCase();
        if ("da_xac_nhan".equals(lower)
                || "dang_chuan_bi_hang".equals(lower) || "san_sang_giao".equals(lower)
                || "da_tao_van_don_ghn".equals(lower) || "da_ban_giao_ghn".equals(lower)
                || "dang_lay_hang".equals(lower) || "dang_giao".equals(lower)
                || "da_giao".equals(lower) || "hoan_thanh".equals(lower)) {
            return true;
        }
        // Đơn hàng thanh toán trước (SePay, Chuyển khoản...) đã được trừ kho FIFO ngay khi thanh toán thành công (ở trạng thái cho_xac_nhan)
        if ("cho_xac_nhan".equals(lower) && hd != null && isOrderPaid(hd)) {
            return true;
        }
        return false;
    }

    public String getNextStatus(HoaDon hd) {
        if (hd == null || hd.getTrangThaiDonHang() == null) {
            return null;
        }
        boolean hasGhnCode = (hd.getGhnOrderCode() != null && !hd.getGhnOrderCode().trim().isEmpty())
                || (hd.getGhnReturnOrderCode() != null && !hd.getGhnReturnOrderCode().trim().isEmpty());
        String currentStatus = hd.getTrangThaiDonHang().toLowerCase();

        // 1. Nếu ở trạng thái san_sang_giao mà chưa có mã GHN -> KHÔNG cho phép chuyển tiếp bằng nút Next (phải bấm Gửi đơn sang GHN)
        if ("san_sang_giao".equals(currentStatus) && !hasGhnCode) {
            return null;
        }

        // 2. Đơn hàng đã có mã GHN hoặc ở các trạng thái vận chuyển GHN / hoàn thành / đã hủy -> Quản lý hoàn toàn qua GHN, ngăn chặn chuyển tiếp thủ công
        if (hasGhnCode
                || "da_tao_van_don_ghn".equals(currentStatus)
                || "da_ban_giao_ghn".equals(currentStatus)
                || "dang_lay_hang".equals(currentStatus)
                || "dang_giao".equals(currentStatus)
                || "da_giao".equals(currentStatus)
                || "giao_that_bai".equals(currentStatus)
                || "hoan_thanh".equals(currentStatus)
                || "da_huy".equals(currentStatus)) {
            return null;
        }

        return getNextStatus(currentStatus);
    }

    public String getNextStatus(String currentStatus) {
        if (currentStatus == null) {
            return null;
        }
        return switch (currentStatus.toLowerCase()) {
            case "cho_thanh_toan" ->
                "cho_xac_nhan";
            case "cho_xac_nhan" ->
                "da_xac_nhan";
            case "da_xac_nhan" ->
                "dang_chuan_bi_hang";
            case "dang_chuan_bi_hang" ->
                "san_sang_giao";
            case "san_sang_giao" ->
                "da_tao_van_don_ghn";
            case "da_tao_van_don_ghn" ->
                "da_ban_giao_ghn";
            case "da_ban_giao_ghn" ->
                null;
            case "dang_lay_hang" ->
                null; // Tự động cập nhật bởi GHN Webhook
            case "dang_giao" ->
                null; // Tự động cập nhật bởi GHN Webhook
            case "stock_conflict" ->
                "cho_xac_nhan";
            default ->
                null;
        };
    }

    @Transactional
    public void moveOrderToNextStatus(Integer orderId, Integer actingUserId, String clientIp) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        String currentStatus = hd.getTrangThaiDonHang();
        String nextStatus = getNextStatus(hd);
        if (nextStatus == null) {
            throw new IllegalStateException("Đơn hàng đã nhận mã vận đơn GHN / ở trạng thái cuối cùng và không thể chuyển trạng thái thủ công.");
        }

        updateOrderStatusByAdmin(orderId, nextStatus, currentStatus, actingUserId, clientIp);
    }

    public Map<String, LocalDateTime> getStatusTransitionTimes(Integer idHoaDon) {
        Map<String, LocalDateTime> times = new HashMap<>();
        try {
            List<com.smashvn.shop.entity.EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", idHoaDon);
            for (com.smashvn.shop.entity.EditLog log : logs) {
                String giaTriMoi = log.getGiaTriMoi();
                if (giaTriMoi != null) {
                    String lower = giaTriMoi.toLowerCase();
                    if (lower.contains("da_xac_nhan")) {
                        times.putIfAbsent("da_xac_nhan", log.getThoiGian());
                    }
                    if (lower.contains("dang_lay_hang")) {
                        times.putIfAbsent("dang_lay_hang", log.getThoiGian());
                    }
                    if (lower.contains("dang_giao")) {
                        times.putIfAbsent("dang_giao", log.getThoiGian());
                    }
                    if (lower.contains("da_giao") || lower.contains("delivered")) {
                        times.putIfAbsent("da_giao", log.getThoiGian());
                    }
                    if (lower.contains("hoan_thanh")) {
                        times.putIfAbsent("hoan_thanh", log.getThoiGian());
                    }
                    if (lower.contains("da_huy") || lower.contains("cancelled")) {
                        times.putIfAbsent("da_huy", log.getThoiGian());
                    }
                }
            }
        } catch (Exception e) {
            // Fallback silent
        }
        return times;
    }

    private boolean isTestEnvironment() {
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().startsWith("org.junit.") || element.getClassName().startsWith("org.spockframework.")) {
                return true;
            }
        }
        return false;
    }

    private void guiEmailYeuCauHoanTien(HoaDon hd, String lyDoHuy) {
        String token = java.util.UUID.randomUUID().toString();
        // Store token in gatewayResponse
        String oldResponse = hd.getGatewayResponse() != null ? hd.getGatewayResponse() : "";
        hd.setGatewayResponse("REFUND_TOKEN:" + token + ";" + oldResponse);
        hoaDonRepository.save(hd);

        if (adminEmailsConfig == null || adminEmailsConfig.trim().isEmpty()) {
            log.error("Không có email quản trị nào được cấu hình trong app.admin.emails!");
            return;
        }

        String maDonHang = hd.getMaDonHang() != null ? hd.getMaDonHang() : "POS#" + hd.getId();

        // Tránh spam gửi email thật khi chạy test cases
        if (isTestEnvironment() || maDonHang.startsWith("TEST-")) {
            System.out.println("[TEST] Bỏ qua gửi email xác nhận hoàn tiền cho đơn hàng test: " + maDonHang);
            return;
        }

        String appUrl = resolveAppUrl();
        String approveLink = appUrl + "/admin/don-hang/approve-refund?id=" + hd.getId() + "&token=" + token;
        String rejectLink = appUrl + "/admin/don-hang/reject-refund?id=" + hd.getId() + "&token=" + token;

        String tenKhachHang = hd.getKhachHang() != null ? (hd.getKhachHang().getHoKh() + " " + hd.getKhachHang().getTenKh()) : "Khách lẻ";
        String sdt = hd.getSdtNhan() != null ? hd.getSdtNhan() : "N/A";
        String phuongThuc = hd.getPaymentMethod() != null ? hd.getPaymentMethod() : (hd.getPhuongThucThanhToan() != null ? hd.getPhuongThucThanhToan().getTenPhuongThuc() : "N/A");
        String formattedTongTien = hd.getTongTien() != null ? String.format("%,.0f", hd.getTongTien()) : "0";
        String hienThiLyDo = (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) ? lyDoHuy.trim() : "Không cung cấp lý do";

        String htmlMsg = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "    <meta charset=\"utf-8\">"
                + "    <title>Yêu cầu xác nhận hoàn tiền</title>"
                + "</head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Inter', system-ui, -apple-system, sans-serif;\">"
                + "    <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" width=\"100%\" style=\"max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e9ecef;\">"
                + "        <tr>"
                + "            <td style=\"padding: 32px 40px; background-color: #212529; text-align: center;\">"
                + "                <h2 style=\"margin: 0; color: #ffffff; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;\">SMASH VN</h2>"
                + "                <p style=\"margin: 4px 0 0 0; color: #adb5bd; font-size: 14px;\">Yêu cầu xác nhận hoàn tiền đơn hàng</p>"
                + "            </td>"
                + "        </tr>"
                + "        <tr>"
                + "            <td style=\"padding: 40px;\">"
                + "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 16px; line-height: 1.6;\">Chào Quản lý hệ thống,</p>"
                + "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 16px; line-height: 1.6;\">Một yêu cầu hoàn tiền cho đơn hàng đã thanh toán trực tuyến vừa được tạo và đang chờ bạn phê duyệt:</p>"
                + "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" width=\"100%\" style=\"background-color: #f8f9fa; border-radius: 12px; padding: 20px; margin-bottom: 32px; border: 1px solid #e9ecef;\">"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px; width: 40%;\">Mã đơn hàng:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; font-family: monospace;\">" + maDonHang + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Khách hàng:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600;\">" + tenKhachHang + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Số điện thoại:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px;\">" + sdt + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Phương thức:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; color: #0d6efd;\">" + phuongThuc + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Yêu cầu hoàn tiền:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600; color: #dc3545;\">" + (hd.getRefundStatus() != null ? hd.getRefundStatus().getLabel() : "Không yêu cầu") + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 6px 0; color: #6c757d; font-size: 14px;\">Trạng thái hoàn kho:</td>"
                + "                        <td style=\"padding: 6px 0; color: #212529; font-size: 14px; font-weight: 600;\">" + (hd.getTrangThaiHoanHang() != null ? hd.getTrangThaiHoanHang().getLabel() : "Đã hoàn kho lập tức (Chưa xuất kho)") + "</td>"
                + "                    </tr>"
                + "                    <tr style=\"background-color: #fff5f5;\">"
                + "                        <td style=\"padding: 8px 10px; color: #dc3545; font-size: 14px; font-weight: bold;\">Lý do hủy:</td>"
                + "                        <td style=\"padding: 8px 10px; color: #dc3545; font-size: 14px; font-weight: bold;\">" + hienThiLyDo + "</td>"
                + "                    </tr>"
                + "                    <tr>"
                + "                        <td style=\"padding: 8px 0 6px 0; color: #6c757d; font-size: 14px; border-top: 1px dashed #dee2e6;\">Số tiền cần hoàn:</td>"
                + "                        <td style=\"padding: 8px 0 6px 0; color: #dc3545; font-size: 16px; font-weight: 700; border-top: 1px dashed #dee2e6;\">" + formattedTongTien + " đ</td>"
                + "                    </tr>"
                + "                </table>"
                + "                <p style=\"margin: 0 0 24px 0; color: #495057; font-size: 15px; line-height: 1.6; text-align: center; font-weight: 600;\">"
                + "                    Vui lòng chọn một trong các thao tác bên dưới để xử lý yêu cầu:"
                + "                </p>"
                + "                <table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" align=\"center\" style=\"margin: 0 auto 32px auto;\">"
                + "                    <tr>"
                + "                        <td align=\"center\" style=\"padding: 0 10px;\">"
                + "                            <a href=\"" + approveLink + "\" style=\"background-color: #198754; color: #ffffff; text-decoration: none; padding: 14px 24px; font-size: 14px; font-weight: bold; border-radius: 8px; display: inline-block; box-shadow: 0 4px 6px rgba(25, 135, 84, 0.2);\">"
                + "                                Phê duyệt hoàn tiền"
                + "                            </a>"
                + "                        </td>"
                + "                        <td align=\"center\" style=\"padding: 0 10px;\">"
                + "                            <a href=\"" + rejectLink + "\" style=\"background-color: #dc3545; color: #ffffff; text-decoration: none; padding: 14px 24px; font-size: 14px; font-weight: bold; border-radius: 8px; display: inline-block; box-shadow: 0 4px 6px rgba(220, 53, 69, 0.2);\">"
                + "                                Từ chối hoàn tiền"
                + "                            </a>"
                + "                        </td>"
                + "                    </tr>"
                + "                </table>"
                + "                <p style=\"margin: 16px 0 0 0; font-size: 13px; color: #6c757d; text-align: center;\">"
                + "                   Hoặc bạn cũng có thể duyệt/từ chối trực tiếp trên Dashboard Smash VN."
                + "                </p>"
                + "                <hr style=\"border: 0; border-top: 1px solid #e9ecef; margin: 32px 0;\">"
                + "                <p style=\"margin: 0; color: #868e96; font-size: 12px; line-height: 1.5; text-align: center;\">"
                + "                    * Lưu ý: Khi nhấp <strong>Phê duyệt hoàn tiền</strong>, số tiền này sẽ bị trừ khỏi thống kê doanh thu.<br>"
                + "                    Nếu nhấp <strong>Từ chối hoàn tiền</strong>, trạng thái thanh toán sẽ được khôi phục và doanh thu được giữ nguyên."
                + "                </p>"
                + "            </td>"
                + "        </tr>"
                + "        <tr>"
                + "            <td style=\"padding: 24px; background-color: #f8f9fa; text-align: center; border-top: 1px solid #e9ecef;\">"
                + "                <p style=\"margin: 0; color: #adb5bd; font-size: 12px;\">Hệ thống Quản trị Smash VN &copy; 2026</p>"
                + "            </td>"
                + "        </tr>"
                + "    </table>"
                + "</body>"
                + "</html>";

        String[] admins = adminEmailsConfig.split(",");
        for (String email : admins) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(email.trim());
                helper.setSubject("[Smash VN] Yêu cầu xác nhận hoàn tiền - Đơn hàng " + maDonHang);
                helper.setText(htmlMsg, true);
                mailSender.send(message);
            } catch (Exception e) {
                log.error("Lỗi gửi mail yêu cầu hoàn tiền đến {}: {}", com.smashvn.shop.util.ValidationUtils.maskEmail(email), e.getMessage());
            }
        }
    }

    private String resolveAppUrl() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs
                    = (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                jakarta.servlet.http.HttpServletRequest request = attrs.getRequest();

                // 1. Resolve scheme (taking X-Forwarded-Proto into account)
                String scheme = request.getHeader("X-Forwarded-Proto");
                if (scheme == null || scheme.isEmpty()) {
                    scheme = request.getScheme();
                }

                // 2. Resolve host (taking X-Forwarded-Host or Host header into account)
                String host = request.getHeader("X-Forwarded-Host");
                if (host == null || host.isEmpty()) {
                    host = request.getHeader("Host");
                }
                if (host == null || host.isEmpty()) {
                    String serverName = request.getServerName();
                    int serverPort = request.getServerPort();
                    if (serverPort == 80 || serverPort == 443) {
                        host = serverName;
                    } else {
                        host = serverName + ":" + serverPort;
                    }
                }

                // Thêm ":8080" nếu host là localhost hoặc 127.0.0.1 (không kèm cổng)
                if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equalsIgnoreCase(host)) {
                    host = host + ":8080";
                }

                String contextPath = request.getContextPath();
                return scheme + "://" + host + contextPath;
            }
        } catch (Exception e) {
            // fallback silent
        }
        return "http://localhost:8080";
    }

    @Transactional
    public void approveRefund(Integer orderId, String token, Integer actingUserId, String clientIp) {
        if (actingUserId == null) {
            throw new AccessDeniedException("Tài khoản người thực hiện không tồn tại.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingUserId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        if (!"QL".equals(actingUser.getVaiTro())) {
            throw new AccessDeniedException("Chỉ Quản lý mới có thể phê duyệt.");
        }

        HoaDon hd = hoaDonRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        String response = hd.getGatewayResponse();
        if (token != null && !token.trim().isEmpty()) {
            if (response == null || !response.contains("REFUND_TOKEN:" + token)) {
                throw new IllegalArgumentException("Token xác nhận hoàn tiền không hợp lệ hoặc đã hết hiệu lực!");
            }
        }

        if (!"CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            throw new IllegalStateException("Đơn hàng này không ở trạng thái chờ hoàn tiền!");
        }

        String oldState = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s",
                hd.getTrangThaiDonHang(), hd.getPaymentStatus(), hd.getTrangThaiThanhToan(),
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL");

        // Update payment status to REFUNDED
        hd.setPaymentStatus("REFUNDED");
        hd.setTrangThaiThanhToan("REFUNDED");
        hd.setRefundStatus(RefundStatus.COMPLETED);
        hd.setRefundTime(LocalDateTime.now());

        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingUserId);
        if (nv != null) {
            hd.setRefundConfirmedBy(nv);
        }

        // Remove token from response
        if (response != null && token != null && !token.trim().isEmpty()) {
            String newToken = response.replaceAll("REFUND_TOKEN:" + token + ";?", "");
            hd.setGatewayResponse(newToken);
        } else if (response != null && response.contains("REFUND_TOKEN:")) {
            String newToken = response.replaceAll("REFUND_TOKEN:[^;]+;?", "");
            hd.setGatewayResponse(newToken);
        }

        hoaDonRepository.save(hd);

        // Audit log
        String newState = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s",
                hd.getTrangThaiDonHang(), hd.getPaymentStatus(), hd.getTrangThaiThanhToan(), hd.getRefundStatus().name());

        String actingUserRole = "QUAN_LY";
        if (actingUserId != null) {
            TaiKhoan tk = taiKhoanRepository.findById(actingUserId).orElse(null);
            if (tk != null && tk.getVaiTro() != null) {
                actingUserRole = tk.getVaiTro();
            }
        }

        auditService.log(
                actingUserId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                oldState,
                newState,
                clientIp,
                "[REFUND_APPROVED] Quản lý phê duyệt hoàn tiền thành công. Trạng thái hoàn tiền: COMPLETED. Đơn hàng hiện đã chính thức trừ khỏi thống kê doanh thu.",
                actingUserRole
        );
    }

    @Transactional
    public void rejectRefund(Integer orderId, String token, Integer actingUserId, String clientIp) {
        if (actingUserId == null) {
            throw new AccessDeniedException("Tài khoản người thực hiện không tồn tại.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingUserId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        if (!"QL".equals(actingUser.getVaiTro())) {
            throw new AccessDeniedException("Chỉ Quản lý mới có thể phê duyệt.");
        }

        HoaDon hd = hoaDonRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        String response = hd.getGatewayResponse();
        if (token != null && !token.trim().isEmpty()) {
            if (response == null || !response.contains("REFUND_TOKEN:" + token)) {
                throw new IllegalArgumentException("Token xác nhận hoàn tiền không hợp lệ hoặc đã hết hiệu lực!");
            }
        }

        if (!"CHO_HOAN_TIEN".equals(hd.getTrangThaiThanhToan())) {
            throw new IllegalStateException("Đơn hàng này không ở trạng thái chờ hoàn tiền!");
        }

        String oldState = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s",
                hd.getTrangThaiDonHang(), hd.getPaymentStatus(), hd.getTrangThaiThanhToan(),
                hd.getRefundStatus() != null ? hd.getRefundStatus().name() : "NULL");

        // Revert to PAID and DA_THANH_TOAN
        hd.setPaymentStatus("paid");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setRefundStatus(null);
        hd.setRefundTime(null);
        hd.setRefundConfirmedBy(null);

        // Remove token from response
        if (response != null && token != null && !token.trim().isEmpty()) {
            String newToken = response.replaceAll("REFUND_TOKEN:" + token + ";?", "");
            hd.setGatewayResponse(newToken);
        } else if (response != null && response.contains("REFUND_TOKEN:")) {
            String newToken = response.replaceAll("REFUND_TOKEN:[^;]+;?", "");
            hd.setGatewayResponse(newToken);
        }

        hoaDonRepository.save(hd);

        // Audit log
        String newState = String.format("status=%s, paymentStatus=%s, trangThaiThanhToan=%s, refundStatus=%s",
                hd.getTrangThaiDonHang(), hd.getPaymentStatus(), hd.getTrangThaiThanhToan(), "NULL");

        String actingUserRole = "QUAN_LY";
        if (actingUserId != null) {
            TaiKhoan tk = taiKhoanRepository.findById(actingUserId).orElse(null);
            if (tk != null && tk.getVaiTro() != null) {
                actingUserRole = tk.getVaiTro();
            }
        }

        auditService.log(
                actingUserId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                oldState,
                newState,
                clientIp,
                "[REFUND_REJECTED] Quản lý từ chối yêu cầu hoàn tiền. Đơn hàng được giữ nguyên trạng thái thanh toán và thống kê doanh thu.",
                actingUserRole
        );
    }

    @Transactional
    public void updateReturnStatusByAdmin(Integer idHoaDon, String newReturnStatusStr, Integer actingTaiKhoanId, String clientIp) {
        // 1. Authorization Check
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String role = actingUser.getVaiTro();
        if (!"NV".equals(role) && !"QL".equals(role)) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }

        // 2. Lock HoaDon
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturnStatus = hd.getTrangThaiHoanHang();
        ReturnStatus newReturnStatus = ReturnStatus.valueOf(newReturnStatusStr.toUpperCase());

        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        // 3. Validation Of Allowed Return Status Transitions
        // Only allow transition when order is cancelled (da_huy) and return status is PENDING_RETURN
        if (!OrderStatus.DA_HUY.getValue().equalsIgnoreCase(hd.getTrangThaiDonHang()) || currentReturnStatus != ReturnStatus.PENDING_RETURN) {
            String warningMsg = String.format("[WARNING] Yêu cầu chuyển đổi trạng thái hoàn hàng không hợp lệ từ %s sang %s cho hóa đơn #%d",
                    currentReturnStatus != null ? currentReturnStatus.name() : "NULL",
                    newReturnStatus.name(),
                    hd.getId());

            auditService.log(
                    actingTaiKhoanId,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    String.format("trangThaiHoanHang=%s", currentReturnStatus != null ? currentReturnStatus.name() : "NULL"),
                    String.format("trangThaiHoanHang=%s", newReturnStatus.name()),
                    clientIp,
                    warningMsg,
                    roleStr
            );
            throw new IllegalStateException("Chỉ cho phép chuyển trạng thái hoàn hàng từ PENDING_RETURN sang RETURNED, LOST hoặc DAMAGED.");
        }

        // 4. Perform stock adjustment if new status is RETURNED
        String adjustmentDetails = "Không có điều chỉnh kho";
        if (newReturnStatus == ReturnStatus.RETURNED) {
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
            List<RestockItemRequest> restockReqs = new ArrayList<>();
            StringBuilder detailsBuilder = new StringBuilder("Chi tiết điều chỉnh tồn kho:\n");
            for (HoaDonChiTiet item : items) {
                if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                    restockReqs.add(RestockItemRequest.builder()
                            .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                            .quantityToRestock(item.getSoLuong())
                            .conBanDuoc(true)
                            .build());
                    detailsBuilder.append(String.format("SPCT-%d : +%d\n", item.getSanPhamChiTiet().getId(), item.getSoLuong()));
                }
            }
            if (!restockReqs.isEmpty()) {
                inventoryLotService.hoanKho(restockReqs);
            }
            adjustmentDetails = detailsBuilder.toString().trim();
        } else if (newReturnStatus == ReturnStatus.LOST || newReturnStatus == ReturnStatus.DAMAGED) {
            adjustmentDetails = "Không hoàn trả tồn kho (Hàng bị mất/hỏng)";
        }

        // 5. Update Audit Information
        hd.setTrangThaiHoanHang(newReturnStatus);
        hd.setNgayXacNhanHoanHang(LocalDateTime.now());

        NhanVien nv = nhanVienRepository.findByTaiKhoanId(actingTaiKhoanId);
        if (nv != null) {
            hd.setNhanVienXacNhan(nv);
        }

        hoaDonRepository.save(hd);

        // Generate notification for customer on return status update (only when return status actually changes)
        if (hd.getKhachHang() != null && hd.getKhachHang().getTaiKhoan() != null && currentReturnStatus != newReturnStatus) {
            try {
                String maDon = hd.getMaDonHang() != null ? hd.getMaDonHang() : "SMASH-" + hd.getId();
                String loaiYeuCau = hd.getLoaiYeuCauDoiTra();
                String title = getCustomerReturnTitle(newReturnStatus, loaiYeuCau);
                String msgContent = buildCustomerReturnNotificationContent(maDon, newReturnStatus, loaiYeuCau);

                ThongBao thongBao = ThongBao.builder()
                        .taiKhoan(hd.getKhachHang().getTaiKhoan())
                        .tieuDe(title)
                        .noiDung(msgContent)
                        .daDoc(false)
                        .loaiThongBao("don_hang")
                        .ngayTao(LocalDateTime.now())
                        .build();
                thongBaoRepository.save(thongBao);
            } catch (Exception e) {
                log.error("Lỗi tạo thông báo trạng thái hoàn hàng cho khách: {}", e.getMessage());
            }
        }

        // 6. Log Audit
        String logNote = String.format("[WAREHOUSE_RETURN_%s] Xác nhận trạng thái hoàn hàng: %s. %s",
                newReturnStatus.name(), newReturnStatus.getLabel(), adjustmentDetails);

        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                String.format("trangThaiHoanHang=PENDING_RETURN"),
                String.format("trangThaiHoanHang=%s", newReturnStatus.name()),
                clientIp,
                logNote,
                roleStr
        );
    }

    public record PaymentStatusInfo(String code, String label, String badgeClass) {

    }

    public PaymentStatusInfo getPaymentStatusInfo(String status) {
        if (status == null || status.trim().isEmpty()) {
            return new PaymentStatusInfo("UNKNOWN", "Không xác định", "bg-secondary");
        }
        String upperStatus = status.trim().toUpperCase();
        return switch (upperStatus) {
            case "PAID", "DA_THANH_TOAN" ->
                new PaymentStatusInfo("PAID", "Đã thanh toán", "bg-success");
            case "PENDING", "CHO_THANH_TOAN" ->
                new PaymentStatusInfo("PENDING", "Chờ thanh toán", "bg-warning text-dark");
            case "CANCELLED", "CANCELED", "HUY", "DA_HUY", "FAILED" ->
                new PaymentStatusInfo("CANCELLED", "Đã hủy", "bg-danger");
            case "REFUNDED", "DA_HOAN_TIEN" ->
                new PaymentStatusInfo("REFUNDED", "Đã hoàn tiền", "bg-danger");
            case "CHO_HOAN_TIEN", "HOAN_TIEN" ->
                new PaymentStatusInfo("CHO_HOAN_TIEN", "Chờ hoàn tiền", "bg-warning text-dark");
            case "SAI LỆCH SỐ TIỀN", "AMOUNT_MISMATCH" ->
                new PaymentStatusInfo("AMOUNT_MISMATCH", "Sai lệch số tiền", "bg-danger");
            case "PAID_RECEIVED_AFTER_CANCEL" ->
                new PaymentStatusInfo("PAID_RECEIVED_AFTER_CANCEL", "Nhận thanh toán sau hủy", "bg-info");
            default ->
                new PaymentStatusInfo("UNKNOWN", upperStatus, "bg-secondary");
        };
    }

    public boolean isOrderPaid(HoaDon hd) {
        if (hd == null) {
            return false;
        }
        String tt = hd.getTrangThaiThanhToan();
        String ps = hd.getPaymentStatus();

        if (tt != null) {
            String t = tt.trim().toUpperCase();
            if ("DA_THANH_TOAN".equals(t) || "PAID".equals(t) || "CHO_HOAN_TIEN".equals(t) || "DA_HOAN_TIEN".equals(t) || "REFUNDED".equals(t)) {
                return true;
            }
        }
        if (ps != null) {
            String p = ps.trim().toUpperCase();
            if ("DA_THANH_TOAN".equals(p) || "PAID".equals(p) || "CHO_HOAN_TIEN".equals(p) || "DA_HOAN_TIEN".equals(p) || "REFUNDED".equals(p)) {
                return true;
            }
        }
        return hd.getPaidAt() != null || hd.getNgayThanhToan() != null;
    }

    public ReturnStatus resolveReturnStatus(Integer idHoaDon, HoaDon hd) {
        if (hd != null && hd.getTrangThaiHoanHang() != null) {
            return hd.getTrangThaiHoanHang();
        }
        if (idHoaDon == null || hd == null) {
            return null;
        }
        // Normal non-returned orders will never have return status in EditLog
        String ttRaw = hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang().toLowerCase() : "";
        String psRaw = hd.getPaymentStatus() != null ? hd.getPaymentStatus().toUpperCase() : "";
        if (!"da_huy".equals(ttRaw) && !"cho_hoan_tien".equalsIgnoreCase(ttRaw) && !"CHO_HOAN_TIEN".equals(psRaw) && !"DA_HOAN_TIEN".equals(psRaw)) {
            return null;
        }
        try {
            List<com.smashvn.shop.entity.EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", idHoaDon);
            for (int i = logs.size() - 1; i >= 0; i--) {
                String giaTriMoi = logs.get(i).getGiaTriMoi();
                if (giaTriMoi != null && giaTriMoi.contains("trangThaiHoanHang=")) {
                    String statusStr = giaTriMoi.substring(giaTriMoi.indexOf("trangThaiHoanHang=") + 18);
                    if (statusStr.contains(",")) {
                        statusStr = statusStr.substring(0, statusStr.indexOf(","));
                    }
                    statusStr = statusStr.trim();
                    if (!"NULL".equalsIgnoreCase(statusStr) && !statusStr.isEmpty()) {
                        try {
                            return ReturnStatus.valueOf(statusStr.toUpperCase());
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return null;
    }

    public String resolveGhnReturnOrderCode(Integer idHoaDon, HoaDon hd) {
        if (idHoaDon == null || hd == null) {
            return null;
        }
        // Only query TichHopVanChuyen if return status is present or order is cancelled/returned
        if (hd.getTrangThaiHoanHang() == null && hd.getGhnReturnOrderCode() == null) {
            return null;
        }
        if (hd.getGhnReturnOrderCode() != null && !hd.getGhnReturnOrderCode().isBlank()
                && !hd.getGhnReturnOrderCode().startsWith("GHN-RETURN-SIMULATED-")
                && !hd.getGhnReturnOrderCode().startsWith("GHNRET")
                && (hd.getGhnOrderCode() == null || !hd.getGhnReturnOrderCode().equals(hd.getGhnOrderCode()))) {
            return hd.getGhnReturnOrderCode().trim();
        }
        try {
            List<String> codes = jdbcTemplate.queryForList(
                    "SELECT ma_van_don FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap IN ('GHN_RETURN', 'GHN_RETURN_FALLBACK') ORDER BY id DESC",
                    String.class,
                    idHoaDon
            );
            if (codes != null && !codes.isEmpty()) {
                for (String code : codes) {
                    if (code != null && !code.isBlank() && !code.startsWith("GHN-RETURN-SIMULATED-") && !code.startsWith("GHNRET")) {
                        return code.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean isDaNhapKhoHoan(Integer idHoaDon, HoaDon hd) {
        if (hd == null || idHoaDon == null) {
            return false;
        }
        if (Boolean.TRUE.equals(hd.getDaNhapKhoHoan())) {
            return true;
        }
        ReturnStatus st = hd.getTrangThaiHoanHang();
        if (st == ReturnStatus.RETURNED || st == ReturnStatus.REFUNDED) {
            return true;
        }
        String ttRaw = hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang().toLowerCase() : "";
        if (!"da_huy".equals(ttRaw) && !"cho_hoan_tien".equals(ttRaw)) {
            return false;
        }
        try {
            List<com.smashvn.shop.entity.EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", idHoaDon);
            for (int i = logs.size() - 1; i >= 0; i--) {
                String giaTriMoi = logs.get(i).getGiaTriMoi();
                if (giaTriMoi != null && giaTriMoi.contains("daNhapKhoHoan=true")) {
                    return true;
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    public Map<String, String> resolveRefundDetails(Integer idHoaDon, HoaDon hd) {
        Map<String, String> res = new HashMap<>();
        if (hd != null) {
            if (hd.getPhuongThucHoanTien() != null && !hd.getPhuongThucHoanTien().isBlank()) {
                res.put("phuongThucHoanTien", hd.getPhuongThucHoanTien());
            }
            if (hd.getSoTienHoan() != null) {
                res.put("soTienHoan", hd.getSoTienHoan().toString());
            }
            if (hd.getMaGiaoDichHoanTien() != null && !hd.getMaGiaoDichHoanTien().isBlank()) {
                res.put("maGiaoDichHoanTien", hd.getMaGiaoDichHoanTien());
            }
            if (hd.getGhiChuHoanTien() != null && !hd.getGhiChuHoanTien().isBlank()) {
                res.put("ghiChuHoanTien", hd.getGhiChuHoanTien());
            }
            if (hd.getAnhChungTuHoanTien() != null && !hd.getAnhChungTuHoanTien().isBlank()) {
                res.put("anhChungTuHoanTien", hd.getAnhChungTuHoanTien());
            }
            if (hd.getThoiGianHoanTien() != null) {
                res.put("thoiGianHoanTien", hd.getThoiGianHoanTien().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
            }
            if (hd.getNguoiThucHienHoanTien() != null && !hd.getNguoiThucHienHoanTien().isBlank()) {
                res.put("nguoiThucHienHoanTien", hd.getNguoiThucHienHoanTien());
            }
        }

        // Nguồn chân lý bền vững: PaymentTransaction lưu trữ đầy đủ payload hoàn tiền
        if (idHoaDon != null) {
            try {
                List<com.smashvn.shop.entity.PaymentTransaction> refundTxs = paymentTransactionRepository.findByOrder_IdAndStatus(idHoaDon, "REFUND_SUCCESS");
                if (refundTxs != null && !refundTxs.isEmpty()) {
                    com.smashvn.shop.entity.PaymentTransaction latestTx = refundTxs.get(refundTxs.size() - 1);
                    if (!res.containsKey("soTienHoan") && latestTx.getAmount() != null) {
                        res.put("soTienHoan", latestTx.getAmount().toString());
                    }
                    if (!res.containsKey("maGiaoDichHoanTien") && latestTx.getTransactionId() != null) {
                        res.put("maGiaoDichHoanTien", latestTx.getTransactionId());
                    }
                    if (!res.containsKey("thoiGianHoanTien") && latestTx.getCreatedAt() != null) {
                        res.put("thoiGianHoanTien", latestTx.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                    }
                    String raw = latestTx.getRawPayload();
                    if (raw != null && !raw.isBlank()) {
                        try {
                            Map<String, Object> payload = objectMapper.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                            if (!res.containsKey("phuongThucHoanTien") && payload.get("phuongThucHoanTien") != null) {
                                res.put("phuongThucHoanTien", String.valueOf(payload.get("phuongThucHoanTien")));
                            }
                            if (!res.containsKey("ghiChuHoanTien") && payload.get("ghiChu") != null) {
                                res.put("ghiChuHoanTien", String.valueOf(payload.get("ghiChu")));
                            }
                            if (!res.containsKey("nguoiThucHienHoanTien") && payload.get("nguoiThucHien") != null) {
                                res.put("nguoiThucHienHoanTien", String.valueOf(payload.get("nguoiThucHien")));
                            }
                            if (!res.containsKey("anhChungTuHoanTien") && payload.get("anhChungTu") != null) {
                                Object act = payload.get("anhChungTu");
                                if (act instanceof List<?> list && !list.isEmpty()) {
                                    res.put("anhChungTuHoanTien", String.valueOf(list.get(0)));
                                } else if (act instanceof String str && !str.isBlank()) {
                                    res.put("anhChungTuHoanTien", str);
                                }
                            }
                        } catch (Exception parseEx) {
                            log.warn("Error parsing refund raw payload for order {}: {}", idHoaDon, parseEx.getMessage());
                        }
                    }
                }
            } catch (Exception txEx) {
                log.warn("Error retrieving refund transaction for order {}: {}", idHoaDon, txEx.getMessage());
            }
        }

        try {
            List<com.smashvn.shop.entity.EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", idHoaDon);
            for (int i = logs.size() - 1; i >= 0; i--) {
                String gtm = logs.get(i).getGiaTriMoi();
                if (gtm != null && gtm.contains("trangThaiHoanHang=REFUNDED")) {
                    String[] parts = gtm.split(",");
                    for (String part : parts) {
                        String[] kv = part.split("=", 2);
                        if (kv.length == 2) {
                            String key = kv[0].trim();
                            String val = kv[1].trim();
                            if (!res.containsKey(key) && !"NULL".equalsIgnoreCase(val)) {
                                res.put(key, val);
                            }
                        }
                    }
                    if (!res.containsKey("thoiGianHoanTien") && logs.get(i).getThoiGian() != null) {
                        res.put("thoiGianHoanTien", logs.get(i).getThoiGian().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")));
                    }
                    break;
                }
            }
        } catch (Exception e) {
        }

        // Đồng bộ ngược lại vào đối tượng HoaDon trong bộ nhớ nếu hd != null
        if (hd != null) {
            if (hd.getPhuongThucHoanTien() == null && res.containsKey("phuongThucHoanTien")) {
                hd.setPhuongThucHoanTien(res.get("phuongThucHoanTien"));
            }
            if (hd.getSoTienHoan() == null && res.containsKey("soTienHoan")) {
                try {
                    hd.setSoTienHoan(new BigDecimal(res.get("soTienHoan")));
                } catch (Exception ignored) {}
            }
            if (hd.getMaGiaoDichHoanTien() == null && res.containsKey("maGiaoDichHoanTien")) {
                hd.setMaGiaoDichHoanTien(res.get("maGiaoDichHoanTien"));
            }
            if (hd.getGhiChuHoanTien() == null && res.containsKey("ghiChuHoanTien")) {
                hd.setGhiChuHoanTien(res.get("ghiChuHoanTien"));
            }
            if (hd.getAnhChungTuHoanTien() == null && res.containsKey("anhChungTuHoanTien")) {
                hd.setAnhChungTuHoanTien(res.get("anhChungTuHoanTien"));
            }
        }

        return res;
    }

    @Transactional
    public boolean xacNhanDaNhanHang(Integer idHoaDon, Integer idKhachHang, String clientIp) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon).orElse(null);
        if (hd == null || hd.getKhachHang() == null || !hd.getKhachHang().getId().equals(idKhachHang)) {
            return false;
        }

        String st = hd.getTrangThaiDonHang();
        if (!"dang_giao".equalsIgnoreCase(st) && !"da_giao".equalsIgnoreCase(st) && !"hoan_thanh".equalsIgnoreCase(st)) {
            return false;
        }

        hd.setTrangThaiDonHang("hoan_thanh");

        if ("COD".equalsIgnoreCase(hd.getPaymentMethod()) || (hd.getPhuongThucThanhToan() != null && "COD".equalsIgnoreCase(hd.getPhuongThucThanhToan().getTenPhuongThuc()))) {
            hd.setPaymentStatus("PAID");
            hd.setTrangThaiThanhToan("DA_THANH_TOAN");
            if (hd.getPaidAt() == null) {
                hd.setPaidAt(LocalDateTime.now());
            }
        }

        hoaDonRepository.save(hd);

        auditService.log(
                hd.getKhachHang().getTaiKhoan() != null ? hd.getKhachHang().getTaiKhoan().getId() : null,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiDonHang=" + st,
                "trangThaiDonHang=hoan_thanh",
                clientIp,
                "Khách hàng xác nhận đã nhận được hàng.",
                "KHACH_HANG"
        );
        return true;
    }

    public LocalDateTime getDeliveredTimestamp(HoaDon hd) {
        if (hd == null || hd.getId() == null) {
            return null;
        }

        // 1. Đối với đơn hàng bán tại quầy (POS - có nhân viên hoặc mã HDSVN):
        // Hàng được giao ngay tại quầy khi thanh toán thành công
        boolean isPosOrder = (hd.getNhanVien() != null) || (hd.getMaDonHang() != null && hd.getMaDonHang().startsWith("HDSVN"));
        if (isPosOrder) {
            String st = hd.getTrangThaiDonHang() != null ? hd.getTrangThaiDonHang().toLowerCase() : "";
            if ("da_giao".equals(st) || "hoan_thanh".equals(st) || "delivered".equals(st) || "da_thanh_toan".equals(st)) {
                if (hd.getThoiGianXacNhan() != null) {
                    return hd.getThoiGianXacNhan();
                }
                if (hd.getPaidAt() != null) {
                    return hd.getPaidAt();
                }
                if (hd.getNgayThanhToan() != null) {
                    return hd.getNgayThanhToan();
                }
                if (hd.getNgayTao() != null) {
                    return hd.getNgayTao();
                }
            }
        }

        // 2. Authoritative EditLog transition to da_giao or customer-confirmed hoan_thanh
        Map<String, LocalDateTime> times = getStatusTransitionTimes(hd.getId());
        if (times != null) {
            if (times.get("da_giao") != null) {
                return times.get("da_giao");
            }
            if (times.get("hoan_thanh") != null && isConfirmedByCustomerInEditLog(hd.getId())) {
                return times.get("hoan_thanh");
            }
        }

        // Đơn hàng Online chưa có bản ghi xác nhận giao hàng thực tế trong EditLog -> không tính được hạn
        return null;
    }

    private boolean isConfirmedByCustomerInEditLog(Integer idHoaDon) {
        try {
            List<com.smashvn.shop.entity.EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", idHoaDon);
            for (com.smashvn.shop.entity.EditLog log : logs) {
                if ("KHACH_HANG".equalsIgnoreCase(log.getVaiTroThucHien()) && log.getGiaTriMoi() != null
                        && (log.getGiaTriMoi().contains("status=hoan_thanh") || log.getGiaTriMoi().contains("trangThaiDonHang=hoan_thanh"))) {
                    return true;
                }
                if (log.getGhiChu() != null && log.getGhiChu().contains("[KHACH_XAC_NHAN_DA_NHAN_HANG]")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    public LocalDateTime getReturnDeadline(HoaDon hd) {
        LocalDateTime deliveredAt = getDeliveredTimestamp(hd);
        if (deliveredAt == null) {
            return null;
        }
        return deliveredAt.plusDays(7);
    }

    public boolean isWithinReturnWindow(HoaDon hd) {
        LocalDateTime deadline = getReturnDeadline(hd);
        if (deadline == null) {
            return false;
        }
        return !LocalDateTime.now().isAfter(deadline);
    }

    @Deprecated
    @Transactional
    public boolean yeuCauTraHang(Integer idHoaDon, Integer idKhachHang, String lyDo, String clientIp) {
        return yeuCauTraHang(idHoaDon, idKhachHang, "TRA", lyDo, null, clientIp);
    }

    @Transactional
    public boolean yeuCauTraHang(Integer idHoaDon, Integer idKhachHang, String loaiYeuCauInput, String lyDoInput, List<String> bangChungPaths, String clientIp) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon).orElse(null);
        if (hd == null || hd.getKhachHang() == null || !hd.getKhachHang().getId().equals(idKhachHang)) {
            throw new IllegalArgumentException("Đơn hàng không tồn tại hoặc không thuộc về tài khoản này.");
        }

        String st = hd.getTrangThaiDonHang();
        if (!OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(st) && !"hoan_thanh".equalsIgnoreCase(st)) {
            throw new IllegalStateException("Chỉ đơn hàng đã giao thành công mới có thể gửi yêu cầu Đổi/Trả.");
        }

        // Rule 7 ngày: Lấy deliveredAt từ timestamp transition da_giao (hoặc hoan_thanh khi khách xác nhận). KHÔNG fallback ngayTao/ngayThanhToan
        LocalDateTime deliveredAt = getDeliveredTimestamp(hd);
        if (deliveredAt == null) {
            throw new IllegalStateException("Không xác định được thời điểm giao hàng thành công để tính hạn 7 ngày, vui lòng liên hệ Admin.");
        }

        if (!isWithinReturnWindow(hd)) {
            throw new IllegalStateException("Đã hết thời hạn 7 ngày đổi/trả kể từ khi giao hàng thành công.");
        }

        // Validate loaiYeuCau: Mặc định là 'TRA' nếu trống (Trả hàng / Hoàn tiền)
        if (loaiYeuCauInput == null || loaiYeuCauInput.trim().isEmpty()) {
            loaiYeuCauInput = "TRA";
        }
        String normalizedLoaiYeuCau = loaiYeuCauInput.trim().toUpperCase();
        if (!"DOI".equals(normalizedLoaiYeuCau) && !"TRA".equals(normalizedLoaiYeuCau)) {
            throw new IllegalArgumentException("Loại yêu cầu đổi/trả không hợp lệ. Chỉ chấp nhận ĐỔI hoặc TRẢ.");
        }

        // Validate lyDoHoanTra: Không null, không blank
        if (lyDoInput == null || lyDoInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do đổi/trả không được để trống.");
        }
        String sanitizedLyDo = org.jsoup.Jsoup.clean(lyDoInput.trim(), org.jsoup.safety.Safelist.none());
        if (sanitizedLyDo.isBlank()) {
            throw new IllegalArgumentException("Lý do đổi/trả không được để trống.");
        }
        if (sanitizedLyDo.length() > 500) {
            sanitizedLyDo = sanitizedLyDo.substring(0, 500);
        }

        // Chống duplicate request nâng cao: Kết hợp trangThaiHoanHang + trangThaiXuLyHangHoan
        ReturnStatus currentReturn = resolveReturnStatus(idHoaDon, hd);
        if (currentReturn != null) {
            if (currentReturn == ReturnStatus.REJECTED) {
                // Trường hợp B: Bị từ chối sau khi hàng về shop và sản phẩm đang được gửi trả lại khách -> CHẶN
                ReturnInventoryStatus inventoryStatus = hd.getTrangThaiXuLyHangHoan();
                if (inventoryStatus == ReturnInventoryStatus.DANG_TRA_LAI_KHACH) {
                    throw new IllegalStateException("Sản phẩm của yêu cầu trước đó đang được gửi trả lại cho bạn. Vui lòng chờ nhận lại sản phẩm.");
                }
            } else {
                throw new IllegalStateException("Đơn hàng này đã có yêu cầu đổi/trả đang được xử lý.");
            }
        }

        // JSON Serialization danh sách bangChungPaths
        String bangChungJson = null;
        if (bangChungPaths != null && !bangChungPaths.isEmpty()) {
            try {
                bangChungJson = objectMapper.writeValueAsString(bangChungPaths);
            } catch (Exception e) {
                log.error("Lỗi serialize danh sách bằng chứng JSON: {}", e.getMessage());
            }
        }

        hd.setLoaiYeuCauDoiTra(normalizedLoaiYeuCau);
        hd.setLyDoHoanTra(sanitizedLyDo);
        hd.setBangChungHoanTra(bangChungJson);
        hd.setTrangThaiHoanHang(ReturnStatus.PENDING_APPROVAL);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.CHUA_XU_LY);
        hoaDonRepository.save(hd);

        auditService.log(
                hd.getKhachHang().getTaiKhoan() != null ? hd.getKhachHang().getTaiKhoan().getId() : null,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiHoanHang=" + (currentReturn != null ? currentReturn.name() : "NULL"),
                "trangThaiHoanHang=PENDING_APPROVAL, loaiYeuCau=" + normalizedLoaiYeuCau,
                clientIp,
                "[KHACH_YEU_CAU_DOI_TRA] Loại: " + normalizedLoaiYeuCau + ", Lý do: " + sanitizedLyDo,
                "KHACH_HANG"
        );
        return true;
    }

    @Transactional
    public String duyetYeuCauTraHangVaTaoDonGhn(Integer idHoaDon, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturn = resolveReturnStatus(idHoaDon, hd);
        if (currentReturn != ReturnStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Chỉ yêu cầu đang ở trạng thái Chờ duyệt (PENDING_APPROVAL) mới có thể thực hiện thao tác duyệt.");
        }

        // 1. Check tổng tồn kho nếu là yêu cầu ĐỔI HÀNG (DOI) - Gom tổng số lượng theo từng biến thể SPCT
        if ("DOI".equalsIgnoreCase(hd.getLoaiYeuCauDoiTra())) {
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
            Map<Integer, Integer> requiredQtyMap = new HashMap<>();
            Map<Integer, SanPhamChiTiet> variantMap = new HashMap<>();

            for (HoaDonChiTiet item : items) {
                if (item.getSanPhamChiTiet() != null) {
                    Integer spctId = item.getSanPhamChiTiet().getId();
                    int qty = item.getSoLuong() != null ? item.getSoLuong() : 0;
                    requiredQtyMap.put(spctId, requiredQtyMap.getOrDefault(spctId, 0) + qty);
                    variantMap.put(spctId, item.getSanPhamChiTiet());
                }
            }

            for (Map.Entry<Integer, Integer> entry : requiredQtyMap.entrySet()) {
                Integer spctId = entry.getKey();
                int totalRequired = entry.getValue();
                SanPhamChiTiet spct = variantMap.get(spctId);
                int tonKho = (spct != null && spct.getSoLuongTon() != null) ? spct.getSoLuongTon() : 0;
                if (tonKho < totalRequired) {
                    String tenSp = (spct != null && spct.getSanPham() != null && spct.getSanPham().getTenSanPham() != null)
                            ? spct.getSanPham().getTenSanPham()
                            : ("biến thể #" + spctId);
                    throw new IllegalStateException("Sản phẩm [" + tenSp + "] không đủ tồn kho để thực hiện đổi hàng. Tồn kho hiện tại: " + tonKho + ", Yêu cầu: " + totalRequired);
                }
            }
        }

        // 2. Reconcile / Chống Double GHN Order: Tra cứu xem đã có vận đơn hoàn trong TichHopVanChuyen / EditLog chưa
        String existingReturnCode = resolveGhnReturnOrderCode(idHoaDon, hd);
        String ghnReturnCode;
        boolean isFallback = false;
        if (existingReturnCode != null && !existingReturnCode.trim().isEmpty() && !existingReturnCode.startsWith("GHN-RETURN-SIMULATED-") && !existingReturnCode.startsWith("GHNRET")) {
            // Trường hợp Reconcile: Vận đơn đã được tạo thành công ở lần thử trước, tái sử dụng mã cũ mà không gọi GHN API lại
            log.info("Reconciling existing return shipment code {} for HoaDon #{}", existingReturnCode, idHoaDon);
            ghnReturnCode = existingReturnCode;
            if (existingReturnCode.startsWith("DEMO-GHN-RETURN-")) {
                isFallback = true;
            }
        } else {
            // Tạo vận đơn GHN thu hồi mới
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
            try {
                ghnReturnCode = ghnService.createReturnShippingOrder(hd, items);
                // Lưu GHN_RETURN vào TichHopVanChuyen trong CÙNG transaction để đảm bảo nguyên tố
                String mergeSql = "MERGE INTO TichHopVanChuyen WITH (HOLDLOCK) AS target "
                        + "USING (SELECT ? AS id_hoa_don, ? AS ma_van_don, ? AS nha_cung_cap, ? AS trang_thai) AS source "
                        + "ON target.id_hoa_don = source.id_hoa_don AND target.nha_cung_cap = source.nha_cung_cap "
                        + "WHEN MATCHED THEN UPDATE SET ma_van_don = source.ma_van_don, ma_don_hang_ngoai = source.ma_van_don, trang_thai = source.trang_thai "
                        + "WHEN NOT MATCHED THEN INSERT (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) "
                        + "VALUES (source.id_hoa_don, source.nha_cung_cap, source.ma_van_don, source.ma_van_don, source.trang_thai, GETDATE());";
                jdbcTemplate.update(mergeSql, idHoaDon, ghnReturnCode, "GHN_RETURN", "waiting_to_return");
            } catch (Exception e) {
                if (ghnService.isEligibleForSandboxFallback(e)) {
                    String timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                    String uniqueId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
                    ghnReturnCode = String.format("DEMO-GHN-RETURN-%s-%d-%s", timestampStr, idHoaDon, uniqueId);

                    // Lưu GHN_RETURN_FALLBACK vào TichHopVanChuyen trong CÙNG transaction để đảm bảo nguyên tố
                    String mergeSql = "MERGE INTO TichHopVanChuyen WITH (HOLDLOCK) AS target "
                            + "USING (SELECT ? AS id_hoa_don, ? AS ma_van_don, ? AS nha_cung_cap, ? AS trang_thai) AS source "
                            + "ON target.id_hoa_don = source.id_hoa_don AND target.nha_cung_cap = source.nha_cung_cap "
                            + "WHEN MATCHED THEN UPDATE SET ma_van_don = source.ma_van_don, ma_don_hang_ngoai = source.ma_van_don, trang_thai = source.trang_thai "
                            + "WHEN NOT MATCHED THEN INSERT (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) "
                            + "VALUES (source.id_hoa_don, source.nha_cung_cap, source.ma_van_don, source.ma_van_don, source.trang_thai, GETDATE());";
                    jdbcTemplate.update(mergeSql, idHoaDon, ghnReturnCode, "GHN_RETURN_FALLBACK", "ready_to_pick");

                    isFallback = true;
                    log.info("[GHN_RETURN_FALLBACK] Đã sinh mã Smart Fallback thu hồi: {} cho HoaDon #{}", ghnReturnCode, idHoaDon);
                } else {
                    throw e;
                }
            }
        }

        // 3. Cập nhật WAITING_FOR_PICKUP
        hd.setGhnReturnOrderCode(ghnReturnCode);
        hd.setTrangThaiHoanHang(ReturnStatus.WAITING_FOR_PICKUP);
        hoaDonRepository.save(hd);

        String auditNote = isFallback
                ? "[ADMIN_DUYET_TRA_HANG] Đã duyệt yêu cầu trả hàng. GHN Sandbox không tạo được đơn thu hồi. Hệ thống tạo Demo Return Fallback: " + ghnReturnCode
                : "[ADMIN_DUYET_TRA_HANG] Đã duyệt yêu cầu và tạo vận đơn GHN thu hồi: " + ghnReturnCode;

        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiHoanHang=" + (currentReturn != null ? currentReturn.name() : "PENDING_APPROVAL"),
                "trangThaiHoanHang=WAITING_FOR_PICKUP, ghnReturnOrderCode=" + ghnReturnCode,
                clientIp,
                auditNote,
                roleStr
        );

        return ghnReturnCode;
    }

    @Transactional
    public void updateReturnStatusFromGhn(Integer idHoaDon, ReturnStatus newReturnStatus, String ghnStatus, String source) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturn = resolveReturnStatus(idHoaDon, hd);
        if (currentReturn == newReturnStatus) {
            return;
        }

        if (currentReturn == ReturnStatus.RETURNED || currentReturn == ReturnStatus.REFUNDED || currentReturn == ReturnStatus.REJECTED) {
            log.info("[{}] Bỏ qua cập nhật ReturnStatus cho đơn hàng #{} (trạng thái hiện tại {} là terminal)", source, idHoaDon, currentReturn);
            return;
        }

        hd.setTrangThaiHoanHang(newReturnStatus);
        hoaDonRepository.save(hd);

        auditService.log(
                null,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiHoanHang=" + (currentReturn != null ? currentReturn.name() : "NULL"),
                "trangThaiHoanHang=" + newReturnStatus.name() + ", ghnStatus=" + ghnStatus,
                "127.0.0.1",
                "[" + source + "] Cập nhật trạng thái hoàn hàng từ GHN (" + ghnStatus + ") -> " + newReturnStatus.name(),
                "SYSTEM"
        );
    }

    @Transactional
    public void updateExchangeStatusFromGhn(Integer idHoaDon, ReturnStatus newReturnStatus, String ghnStatus, String source) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturn = hd.getTrangThaiHoanHang();
        if (currentReturn == ReturnStatus.EXCHANGED) {
            log.info("[{}] Idempotent guard: Đơn hàng #{} đã ở trạng thái EXCHANGED terminal trước đó.", source, idHoaDon);
            return;
        }

        hd.setTrangThaiHoanHang(newReturnStatus);
        hoaDonRepository.save(hd);

        auditService.log(
                null,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiHoanHang=" + (currentReturn != null ? currentReturn.name() : "NULL"),
                "trangThaiHoanHang=" + newReturnStatus.name() + ", ghnStatus=" + ghnStatus,
                "127.0.0.1",
                "[" + source + "] Cập nhật trạng thái giao hàng đổi từ GHN (" + ghnStatus + ") -> " + newReturnStatus.name(),
                "SYSTEM"
        );
    }

    @Transactional
    public void handleRejectReturnDeliveryFromGhn(Integer idHoaDon, String ghnStatus, String source) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        if (hd.getTrangThaiXuLyHangHoan() == ReturnInventoryStatus.DA_TRA_LAI_KHACH) {
            log.info("[{}] Idempotent guard: Đơn hàng #{} đã ở trạng thái DA_TRA_LAI_KHACH", source, idHoaDon);
            return;
        }

        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.DA_TRA_LAI_KHACH);
        if (hd.getTrangThaiHoanHang() == null) {
            hd.setTrangThaiHoanHang(ReturnStatus.REJECTED);
        }
        hoaDonRepository.save(hd);

        auditService.log(
                null,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiXuLyHangHoan=" + (hd.getTrangThaiXuLyHangHoan() != null ? hd.getTrangThaiXuLyHangHoan().name() : "NULL"),
                "trangThaiXuLyHangHoan=DA_TRA_LAI_KHACH, ghnStatus=" + ghnStatus,
                "127.0.0.1",
                "[" + source + "] GHN xác nhận đã giao sản phẩm bị từ chối thành công về lại khách hàng.",
                "SYSTEM"
        );
    }

    @Transactional
    public void tuChoiYeuCauTraHang(Integer idHoaDon, String lyDoTuChoi, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturn = resolveReturnStatus(idHoaDon, hd);
        if (currentReturn != ReturnStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Chỉ yêu cầu đang ở trạng thái Chờ duyệt (PENDING_APPROVAL) mới có thể từ chối.");
        }

        if (lyDoTuChoi == null || lyDoTuChoi.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do từ chối không được để trống.");
        }
        String sanitizedReason = org.jsoup.Jsoup.clean(lyDoTuChoi.trim(), org.jsoup.safety.Safelist.none());
        if (sanitizedReason.isBlank()) {
            throw new IllegalArgumentException("Lý do từ chối không được để trống.");
        }
        if (sanitizedReason.length() > 500) {
            sanitizedReason = sanitizedReason.substring(0, 500);
        }

        // Tuyệt đối không overwrite lyDoHoanTra của khách hàng
        hd.setTrangThaiHoanHang(ReturnStatus.REJECTED);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.CHUA_XU_LY);
        hoaDonRepository.save(hd);

        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiHoanHang=" + (currentReturn != null ? currentReturn.name() : "PENDING_APPROVAL"),
                "trangThaiHoanHang=REJECTED",
                clientIp,
                "[ADMIN_TU_CHOI_YEU_CAU_DOI_TRA] Lý do từ chối ban đầu: " + sanitizedReason,
                roleStr
        );
    }

    @Transactional
    public void huyDonThuHoiGhn(Integer idHoaDon, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        ReturnStatus currentReturn = resolveReturnStatus(idHoaDon, hd);
        if (currentReturn == ReturnStatus.PICKED_UP || currentReturn == ReturnStatus.RETURNING || currentReturn == ReturnStatus.DELIVERED_TO_SHOP || currentReturn == ReturnStatus.RETURNED || currentReturn == ReturnStatus.REFUNDED) {
            throw new IllegalStateException("GHN đã lấy hàng từ khách hoặc đơn đang vận chuyển/đã giao tới shop, không thể hủy đơn thu hồi.");
        }

        if (currentReturn != ReturnStatus.WAITING_FOR_PICKUP) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chờ GHN lấy hàng.");
        }

        hd.setTrangThaiHoanHang(ReturnStatus.REJECTED);
        hoaDonRepository.save(hd);

        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiHoanHang=WAITING_FOR_PICKUP",
                "trangThaiHoanHang=REJECTED",
                clientIp,
                "[ADMIN_HUY_DON_THU_HOI] Hủy vận đơn GHN thu hồi khi chưa lấy hàng.",
                roleStr
        );
    }

    public String resolveGhnRejectReturnCode(Integer idHoaDon) {
        if (idHoaDon != null) {
            try {
                List<String> codes = jdbcTemplate.queryForList(
                        "SELECT ma_van_don FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_REJECT_RETURN'",
                        String.class,
                        idHoaDon
                );
                if (codes != null && !codes.isEmpty() && codes.get(0) != null && !codes.get(0).isBlank()) {
                    return codes.get(0);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public String resolveGhnExchangeOrderCode(Integer idHoaDon) {
        if (idHoaDon != null) {
            try {
                List<String> codes = jdbcTemplate.queryForList(
                        "SELECT ma_van_don FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_EXCHANGE' ORDER BY id DESC",
                        String.class,
                        idHoaDon
                );
                if (codes != null && !codes.isEmpty() && codes.get(0) != null && !codes.get(0).isBlank()) {
                    return codes.get(0);
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void xacNhanKiemKhoVaNhapKho(Integer idHoaDon, Integer actingTaiKhoanId, String clientIp) {
        xacNhanKiemKhoVaNhapKho(idHoaDon, "BAN_LAI", null, actingTaiKhoanId, clientIp);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void xacNhanKiemKhoVaNhapKho(Integer idHoaDon, String ketQuaInput, String lyDoTuChoiInput, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        // 1. Lock HoaDon
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        // 2. State Guard: Chỉ được xử lý khi trangThaiHoanHang == DELIVERED_TO_SHOP và trangThaiXuLyHangHoan == CHUA_XU_LY (hoặc NULL)
        ReturnStatus currentReturn = resolveReturnStatus(idHoaDon, hd);
        if (currentReturn != ReturnStatus.DELIVERED_TO_SHOP) {
            throw new IllegalStateException("Chỉ hàng đổi/trả đã giao về shop (DELIVERED_TO_SHOP) mới có thể thực hiện kiểm hàng.");
        }

        ReturnInventoryStatus currentInvStatus = hd.getTrangThaiXuLyHangHoan();
        if (currentInvStatus != null && currentInvStatus != ReturnInventoryStatus.CHUA_XU_LY) {
            throw new IllegalStateException("Hàng hoàn cho đơn hàng này đã được kiểm định và xử lý trước đó (" + currentInvStatus.getLabel() + ").");
        }

        if (ketQuaInput == null || ketQuaInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn kết quả kiểm định hàng hoàn (BAN_LAI, HANG_LOI, hoặc TU_CHOI).");
        }
        String ketQua = ketQuaInput.trim().toUpperCase();

        // 3. Gom tổng số lượng theo SPCT ID
        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
        Map<Integer, Integer> groupedQtyMap = new HashMap<>();
        for (HoaDonChiTiet item : items) {
            if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                Integer spctId = item.getSanPhamChiTiet().getId();
                groupedQtyMap.put(spctId, groupedQtyMap.getOrDefault(spctId, 0) + item.getSoLuong());
            }
        }

        if ("BAN_LAI".equals(ketQua)) {
            // Outcome A: HÀNG CÓ THỂ BÁN LẠI ➔ Hoàn tồn kho bán (so_luong_ton += quantity)
            List<RestockItemRequest> restockReqs = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : groupedQtyMap.entrySet()) {
                restockReqs.add(RestockItemRequest.builder()
                        .idSanPhamChiTiet(entry.getKey())
                        .quantityToRestock(entry.getValue())
                        .conBanDuoc(true)
                        .build());
            }
            if (!restockReqs.isEmpty()) {
                inventoryLotService.hoanKho(restockReqs);
            }

            hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.DA_HOAN_KHO);
            hd.setTrangThaiHoanHang(ReturnStatus.RETURNED);
            hd.setNgayXacNhanHoanHang(LocalDateTime.now());
            hd.setDaNhapKhoHoan(true);
            markRefundPendingAfterInspection(hd);
            hoaDonRepository.save(hd);

            auditService.log(
                    actingTaiKhoanId,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    "trangThaiHoanHang=DELIVERED_TO_SHOP, trangThaiXuLyHangHoan=CHUA_XU_LY",
                    "trangThaiHoanHang=RETURNED, trangThaiXuLyHangHoan=DA_HOAN_KHO, trangThaiThanhToan=" + hd.getTrangThaiThanhToan(),
                    clientIp,
                    "[KIEM_HANG_BAN_LAI] Kiểm hàng hoàn thành công: Sản phẩm đủ điều kiện bán lại ➔ Đã hoàn kho bán (so_luong_ton).",
                    roleStr
            );

        } else if ("HANG_LOI".equals(ketQua)) {
            // Outcome B: HÀNG LỖI NHƯNG ĐỦ ĐIỀU KIỆN ĐỔI/TRẢ ➔ Cộng kho hàng lỗi (so_luong_sp_loi += quantity, KHÔNG cộng so_luong_ton)
            List<Integer> sortedSpctIds = new ArrayList<>(groupedQtyMap.keySet());
            Collections.sort(sortedSpctIds);

            for (Integer spctId : sortedSpctIds) {
                SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdWithLock(spctId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy SPCT ID: " + spctId));
                int qtyToFaulty = groupedQtyMap.get(spctId);
                int currentFaulty = spct.getSoLuongSpLoi() != null ? spct.getSoLuongSpLoi() : 0;
                spct.setSoLuongSpLoi(currentFaulty + qtyToFaulty);
                sanPhamChiTietRepository.save(spct);
                log.info("[Phase 4] Chuyển kho lỗi SPCT #{}: {} -> {}", spct.getId(), currentFaulty, spct.getSoLuongSpLoi());
            }

            hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI);
            hd.setTrangThaiHoanHang(ReturnStatus.RETURNED);
            hd.setNgayXacNhanHoanHang(LocalDateTime.now());
            hd.setDaNhapKhoHoan(true);
            markRefundPendingAfterInspection(hd);
            hoaDonRepository.save(hd);

            auditService.log(
                    actingTaiKhoanId,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    "trangThaiHoanHang=DELIVERED_TO_SHOP, trangThaiXuLyHangHoan=CHUA_XU_LY",
                    "trangThaiHoanHang=RETURNED, trangThaiXuLyHangHoan=DA_CHUYEN_KHO_LOI, trangThaiThanhToan=" + hd.getTrangThaiThanhToan(),
                    clientIp,
                    "[KIEM_HANG_HANG_LOI] Kiểm hàng hoàn thành công: Sản phẩm bị lỗi ➔ Đã chuyển vào kho hàng lỗi (so_luong_sp_loi).",
                    roleStr
            );

        } else if ("TU_CHOI".equals(ketQua)) {
            // Outcome C: HÀNG KHÔNG ĐẠT ĐIỀU KIỆN ĐỔI/TRẢ ➔ KHÔNG cộng kho bán, KHÔNG cộng kho lỗi, KHÔNG overwrite lyDoHoanTra
            if (lyDoTuChoiInput == null || lyDoTuChoiInput.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối sau khi kiểm tra hàng.");
            }
            String sanitizedReason = org.jsoup.Jsoup.clean(lyDoTuChoiInput.trim(), org.jsoup.safety.Safelist.none());
            if (sanitizedReason.isBlank()) {
                throw new IllegalArgumentException("Lý do từ chối sau khi kiểm tra hàng không được để trống.");
            }
            if (sanitizedReason.length() > 500) {
                sanitizedReason = sanitizedReason.substring(0, 500);
            }

            hd.setTrangThaiHoanHang(ReturnStatus.REJECTED);
            hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.DANG_TRA_LAI_KHACH);
            hoaDonRepository.save(hd);

            // Tạo vận đơn GHN_REJECT_RETURN gửi hàng từ shop về lại cho khách
            String rejectShipmentCode = resolveGhnRejectReturnCode(idHoaDon);
            if (rejectShipmentCode == null) {
                try {
                    rejectShipmentCode = ghnService.createReturnShippingOrder(hd, items);
                    ghnShipmentPersistenceService.saveShipment(idHoaDon, rejectShipmentCode, "GHN_REJECT_RETURN", "waiting_to_return");
                } catch (Exception e) {
                    log.warn("GHN createRejectReturnShippingOrder API failed/simulated: {}", e.getMessage());
                }
            }

            auditService.log(
                    actingTaiKhoanId,
                    "HoaDon",
                    Long.valueOf(hd.getId()),
                    "UPDATE",
                    "trangThaiHoanHang=DELIVERED_TO_SHOP, trangThaiXuLyHangHoan=CHUA_XU_LY",
                    "trangThaiHoanHang=REJECTED, trangThaiXuLyHangHoan=DANG_TRA_LAI_KHACH",
                    clientIp,
                    "[KIEM_HANG_TU_CHOI] Kiểm hàng hoàn KHÔNG ĐẠT. Lý do từ chối: " + sanitizedReason + ". Đang gửi trả lại sản phẩm cho khách.",
                    roleStr
            );

        } else {
            throw new IllegalArgumentException("Kết quả kiểm định không hợp lệ. Chỉ chấp nhận BAN_LAI, HANG_LOI, hoặc TU_CHOI.");
        }
    }

    /**
     * Sau khi yêu cầu TRẢ đã kiểm hàng đạt, khoản tiền chính thức đi vào hàng
     * đợi hoàn. Yêu cầu ĐỔI không được làm thay đổi trạng thái thanh toán.
     */
    private void markRefundPendingAfterInspection(HoaDon hd) {
        if (!"TRA".equalsIgnoreCase(hd.getLoaiYeuCauDoiTra())) {
            return;
        }
        if ("REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan())
                || "DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan())) {
            return;
        }
        hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hd.setRefundStatus(RefundStatus.PENDING);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void xacNhanHoanTienChoKhach(Integer idHoaDon, Integer actingTaiKhoanId, String clientIp) {
        xacNhanHoanTienChoKhach(idHoaDon, "CHUYEN_KHOAN", null, null, null, null, actingTaiKhoanId, clientIp);
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void xacNhanHoanTienChoKhach(Integer idHoaDon, String phuongThucHoanTienInput, BigDecimal soTienHoanInput, String maGiaoDichHoanTienInput, String ghiChuHoanTienInput, String anhChungTuHoanTienInput, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        // 1. Lock HoaDon bằng Pessimistic Write Lock
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        boolean isReturnOrder = "TRA".equalsIgnoreCase(hd.getLoaiYeuCauDoiTra());
        boolean isCancelledPaidOrder = (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(hd.getTrangThaiDonHang()) || "YEU_CAU_HUY".equalsIgnoreCase(hd.getTrangThaiDonHang()))
                && ("CHO_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan()) || RefundStatus.PENDING.equals(hd.getRefundStatus()) || isOrderPaid(hd));

        // 2. Guard: Chỉ cho phép refund với đơn TRẢ HÀNG hoặc đơn ĐÃ HỦY CHỜ HOÀN TIỀN
        if (hd.getLoaiYeuCauDoiTra() != null && !"TRA".equalsIgnoreCase(hd.getLoaiYeuCauDoiTra())) {
            throw new IllegalStateException("Chỉ yêu cầu TRẢ HÀNG mới có thể thực hiện hoàn tiền.");
        }
        if (!isReturnOrder && !isCancelledPaidOrder) {
            throw new IllegalStateException("Chỉ yêu cầu TRẢ HÀNG hoặc đơn hàng đã hủy chờ hoàn tiền mới có thể thực hiện hoàn tiền.");
        }

        if (isReturnOrder) {
            // 3. Double Refund Layer 1: Check ReturnStatus == REFUNDED
            ReturnStatus currentReturn = resolveReturnStatus(idHoaDon, hd);
            if (currentReturn == ReturnStatus.REFUNDED) {
                log.warn("[DOUBLE_REFUND_GUARD_LAYER1] Đơn hàng #{} đã ở trạng thái REFUNDED trước đó.", idHoaDon);
                return;
            }

            // 4. Guard: Phải đang ở trạng thái RETURNED
            if (currentReturn != ReturnStatus.RETURNED) {
                throw new IllegalStateException("Chỉ đơn hàng ở trạng thái Đã nhận hàng về shop (RETURNED) mới được phép hoàn tiền. Trạng thái hiện tại: " + (currentReturn != null ? currentReturn.name() : "NULL"));
            }

            // 5. Guard: Trạng thái xử lý kho phải là DA_HOAN_KHO hoặc DA_CHUYEN_KHO_LOI
            ReturnInventoryStatus invStatus = hd.getTrangThaiXuLyHangHoan();
            if (invStatus != ReturnInventoryStatus.DA_HOAN_KHO && invStatus != ReturnInventoryStatus.DA_CHUYEN_KHO_LOI) {
                throw new IllegalStateException("Hàng hoàn phải được kiểm định và xử lý kho trước khi hoàn tiền. Trạng thái kho hiện tại: " + (invStatus != null ? invStatus.getLabel() : "CHUA_XU_LY"));
            }
        } else {
            // Cancelled paid order: Double Refund Guard
            if (RefundStatus.COMPLETED.equals(hd.getRefundStatus()) && ("REFUNDED".equalsIgnoreCase(hd.getTrangThaiThanhToan()) || "DA_HOAN_TIEN".equalsIgnoreCase(hd.getTrangThaiThanhToan()))) {
                log.warn("[DOUBLE_REFUND_GUARD_CANCEL] Đơn hàng #{} đã ở trạng thái REFUNDED trước đó.", idHoaDon);
                return;
            }

            // Fail-safe restock nếu đơn từng trừ kho mà chưa hoàn
            boolean stockWasDeducted = isStockDeductedState(hd, hd.getTrangThaiDonHang());
            if (stockWasDeducted && (hd.getDaNhapKhoHoan() == null || !hd.getDaNhapKhoHoan())) {
                List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
                List<RestockItemRequest> restockReqs = new ArrayList<>();
                for (HoaDonChiTiet item : items) {
                    if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                        restockReqs.add(RestockItemRequest.builder()
                                .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                                .quantityToRestock(item.getSoLuong())
                                .conBanDuoc(true)
                                .build());
                    }
                }
                if (!restockReqs.isEmpty()) {
                    inventoryLotService.hoanKho(restockReqs);
                    log.info("[OrderViewService] Hoàn kho fail-safe cho {} sản phẩm của đơn #{} khi xác nhận hoàn tiền", restockReqs.size(), idHoaDon);
                }
                hd.setDaNhapKhoHoan(true);
            }

            // Hủy vận đơn GHN nếu có
            if (hd.getGhnOrderCode() != null && !hd.getGhnOrderCode().isBlank()) {
                try {
                    ghnService.cancelOrder(hd.getGhnOrderCode());
                } catch (Exception ghnEx) {
                    log.warn("[OrderViewService] Lỗi khi hủy vận đơn GHN '{}' cho đơn #{}: {}", hd.getGhnOrderCode(), idHoaDon, ghnEx.getMessage());
                }
            }
        }

        // 6. Double Refund Layer 2 + Reconcile: Check GiaoDichThanhToan xem đã có transaction REFUND_SUCCESS chưa
        boolean existingRefundTx = paymentTransactionRepository.existsByOrder_IdAndStatus(idHoaDon, "REFUND_SUCCESS");
        if (existingRefundTx) {
            log.info("[REFUND_RECONCILE] HoaDon #{} đã có bản ghi REFUND_SUCCESS trong CSDL. Thực hiện Reconcile trạng thái.", idHoaDon);
            if (isReturnOrder) {
                hd.setTrangThaiHoanHang(ReturnStatus.REFUNDED);
                hd.setTrangThaiThanhToan("DA_HOAN_TIEN");
            } else {
                hd.setTrangThaiDonHang(OrderStatus.DA_HUY.getValue());
                hd.setPaymentStatus(PaymentStatus.REFUNDED.getValue());
                hd.setTrangThaiThanhToan("REFUNDED");
            }
            hd.setRefundStatus(RefundStatus.COMPLETED);
            hd.setRefundTime(LocalDateTime.now());
            hoaDonRepository.save(hd);

            auditService.log(actingTaiKhoanId, "HoaDon", (long) hd.getId(), "UPDATE",
                    "refundStatus=PENDING", "refundStatus=COMPLETED",
                    clientIp, "[REFUND_RECONCILE] Reconcile trạng thái đơn hàng do đã tồn tại bản ghi hoàn tiền trong CSDL.", roleStr);
            return;
        }

        // 7. Validate & Normalize Phương Thức Hoàn Tiền (CHUYEN_KHOAN / TIEN_MAT)
        if (phuongThucHoanTienInput == null || phuongThucHoanTienInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức hoàn tiền (CHUYEN_KHOAN hoặc TIEN_MAT).");
        }
        String phuongThuc = phuongThucHoanTienInput.trim().toUpperCase();
        if (!"CHUYEN_KHOAN".equals(phuongThuc) && !"TIEN_MAT".equals(phuongThuc)) {
            throw new IllegalArgumentException("Phương thức hoàn tiền không hợp lệ. Chỉ chấp nhận CHUYEN_KHOAN hoặc TIEN_MAT.");
        }

        // 8. Validate Số Tiền Hoàn
        BigDecimal totalOrderAmount = hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO;
        BigDecimal finalSoTienHoan = (soTienHoanInput != null) ? soTienHoanInput : totalOrderAmount;

        if (finalSoTienHoan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0.");
        }

        if (finalSoTienHoan.compareTo(totalOrderAmount) > 0) {
            throw new IllegalArgumentException("Số tiền hoàn không được lớn hơn tổng số tiền khách thực trả (Tối đa: " + totalOrderAmount + " VNĐ).");
        }

        String finalGhiChu = (ghiChuHoanTienInput != null) ? ghiChuHoanTienInput.trim() : "";
        if (finalSoTienHoan.compareTo(totalOrderAmount) < 0 && finalGhiChu.isBlank()) {
            throw new IllegalArgumentException("Khi hoàn số tiền nhỏ hơn tổng tiền đơn hàng, vui lòng nhập ghi chú lý do hoàn thiếu.");
        }

        // 9. Validate Mã Giao Dịch
        String finalMaGD;
        if ("CHUYEN_KHOAN".equals(phuongThuc)) {
            if (maGiaoDichHoanTienInput == null || maGiaoDichHoanTienInput.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập mã giao dịch chuyển khoản hoàn tiền.");
            }
            finalMaGD = maGiaoDichHoanTienInput.trim();
            if (paymentTransactionRepository.findByTransactionId(finalMaGD).isPresent()) {
                throw new IllegalArgumentException("Mã giao dịch hoàn tiền [" + finalMaGD + "] đã tồn tại trên hệ thống.");
            }
        } else {
            // TIEN_MAT: Tự sinh mã internal nếu Admin để trống
            if (maGiaoDichHoanTienInput != null && !maGiaoDichHoanTienInput.trim().isEmpty()) {
                finalMaGD = maGiaoDichHoanTienInput.trim();
                if (paymentTransactionRepository.findByTransactionId(finalMaGD).isPresent()) {
                    throw new IllegalArgumentException("Mã giao dịch hoàn tiền [" + finalMaGD + "] đã tồn tại trên hệ thống.");
                }
            } else {
                finalMaGD = "REFUND-CASH-HD" + idHoaDon + "-" + System.currentTimeMillis();
            }
        }

        // 9.5 Validate Ảnh / Chứng Từ Hoàn Tiền Bắt Buộc
        if (anhChungTuHoanTienInput == null || anhChungTuHoanTienInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Ảnh / chứng từ xác nhận hoàn tiền là bắt buộc.");
        }

        // 10. Tạo chuỗi JSON du_lieu_tho
        Map<String, Object> payloadMap = new java.util.LinkedHashMap<>();
        payloadMap.put("transactionType", isReturnOrder ? "REFUND" : "ORDER_CANCEL_REFUND");
        payloadMap.put("phuongThucHoanTien", phuongThuc);
        payloadMap.put("soTien", finalSoTienHoan);
        payloadMap.put("maGiaoDich", finalMaGD);
        payloadMap.put("ghiChu", finalGhiChu);
        payloadMap.put("anhChungTu", List.of(anhChungTuHoanTienInput.trim()));
        payloadMap.put("nguoiThucHien", actingUser.getUsername());
        payloadMap.put("thoiGianHoanTien", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        String rawPayloadJson = "";
        try {
            rawPayloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            log.warn("Failed to serialize refund rawPayload JSON: {}", e.getMessage());
        }

        // 11. Tạo và Save PaymentTransaction
        com.smashvn.shop.entity.PaymentTransaction tx = new com.smashvn.shop.entity.PaymentTransaction();
        tx.setOrder(hd);
        tx.setTransactionId(finalMaGD);
        tx.setAmount(finalSoTienHoan);
        tx.setGateway("MANUAL_REFUND");
        tx.setStatus("REFUND_SUCCESS");
        tx.setRawPayload(rawPayloadJson);
        tx.setCreatedAt(LocalDateTime.now());

        try {
            paymentTransactionRepository.saveAndFlush(tx);
        } catch (org.springframework.dao.DataIntegrityViolationException dbEx) {
            log.error("Failed to save PaymentTransaction due to constraint: {}", dbEx.getMessage());
            throw new IllegalArgumentException("Mã giao dịch hoàn tiền đã tồn tại trên hệ thống.");
        }

        // 12. Cập nhật HoaDon sang REFUNDED / DA_HOAN_TIEN (CHỈ SAU KHI PaymentTransaction save THÀNH CÔNG!)
        hd.setPhuongThucHoanTien(phuongThuc);
        hd.setSoTienHoan(finalSoTienHoan);
        hd.setMaGiaoDichHoanTien(finalMaGD);
        hd.setGhiChuHoanTien(finalGhiChu);
        hd.setAnhChungTuHoanTien(anhChungTuHoanTienInput.trim());
        hd.setNguoiXacNhanThanhToan(actingUser.getUsername());
        hd.setThoiGianHoanTien(LocalDateTime.now());
        hd.setRefundTime(LocalDateTime.now());
        hd.setRefundStatus(RefundStatus.COMPLETED);

        if (isReturnOrder) {
            hd.setTrangThaiHoanHang(ReturnStatus.REFUNDED);
            hd.setTrangThaiThanhToan("DA_HOAN_TIEN");
        } else {
            hd.setTrangThaiDonHang(OrderStatus.DA_HUY.getValue());
            hd.setPaymentStatus(PaymentStatus.REFUNDED.getValue());
            hd.setTrangThaiThanhToan("REFUNDED");
        }
        hoaDonRepository.save(hd);

        // 13. Audit log
        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "UPDATE",
                "trangThaiThanhToan=CHO_HOAN_TIEN",
                "trangThaiThanhToan=" + hd.getTrangThaiThanhToan() + ", maGiaoDich=" + finalMaGD + ", soTienHoan=" + finalSoTienHoan,
                clientIp,
                "[HOAN_TIEN_KHACH_HANG] Admin đã hoàn tiền thành công cho khách hàng. Phương thức: " + phuongThuc + ", Số tiền: " + finalSoTienHoan + " VNĐ, Mã GD: " + finalMaGD,
                roleStr
        );
    }

    /**
     * Hủy đơn hàng CHƯA THANH TOÁN (COD hoặc Online chưa nhận tiền). Chỉ hoàn
     * kho nếu đơn thực tế đã từng trừ kho (e.g. đã được xác nhận).
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void cancelOrderUnpaid(Integer idHoaDon, String lyDoHuy, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền hủy đơn hàng.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        // 1. Lock HoaDon
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        String currentStatus = hd.getTrangThaiDonHang();

        // 2. Guard: Chặn nếu đơn đã ở trạng thái hủy
        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)) {
            log.warn("[CANCEL_UNPAID_GUARD] Đơn hàng #{} đã ở trạng thái hủy trước đó.", idHoaDon);
            return;
        }

        // 3. Backend Enforce: Chặn nếu đơn đã thanh toán
        if (isOrderPaid(hd)) {
            throw new IllegalStateException("Đơn hàng này đã được thanh toán (" + (hd.getPaymentMethod() != null ? hd.getPaymentMethod().toUpperCase() : "Online") + "). Vui lòng sử dụng chức năng Xác nhận hoàn tiền khi hủy đơn!");
        }

        // 4. Guard: Không được hủy đơn đã giao hoặc đã bàn giao GHN / đang giao
        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(currentStatus)
                || OrderStatus.DA_BAN_GIAO_GHN.getValue().equalsIgnoreCase(currentStatus)
                || "dang_giao".equalsIgnoreCase(currentStatus)
                || "hoan_thanh".equalsIgnoreCase(currentStatus)) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đã bàn giao vận chuyển hoặc đã giao thành công!");
        }

        // 5. Chuẩn hóa lý do hủy
        String standardizedReason = "Khách hàng yêu cầu hủy";
        if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
            String trimmed = lyDoHuy.trim();
            String sanitized = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
            if (sanitized.length() > 500) {
                throw new IllegalArgumentException("Lý do hủy không được vượt quá 500 ký tự.");
            }
            if (!sanitized.isEmpty()) {
                standardizedReason = sanitized;
            }
        }

        // 6. Xử lý tồn kho: CHỈ hoàn kho nếu đơn thực tế đã từng bị trừ kho (e.g. đã được xác nhận)
        boolean stockWasDeducted = isStockDeductedState(hd, currentStatus);
        if (stockWasDeducted && (hd.getDaNhapKhoHoan() == null || !hd.getDaNhapKhoHoan())) {
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
            List<RestockItemRequest> restockReqs = new ArrayList<>();
            for (HoaDonChiTiet item : items) {
                if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                    restockReqs.add(RestockItemRequest.builder()
                            .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                            .quantityToRestock(item.getSoLuong())
                            .conBanDuoc(true)
                            .build());
                }
            }
            if (!restockReqs.isEmpty()) {
                inventoryLotService.hoanKho(restockReqs);
                log.info("[OrderViewService] Đã hoàn kho FIFO cho {} sản phẩm của đơn #{} khi hủy", restockReqs.size(), idHoaDon);
            }
            hd.setDaNhapKhoHoan(true);
        }

        // 7. An unpaid SePay order has not consumed a voucher use.
        if ((hd.isCod() || isOrderPaid(hd))
                && hd.getPhieuGiamGia() != null && hd.getPhieuGiamGia().getMaPhieu() != null) {
            String maPhieu = hd.getPhieuGiamGia().getMaPhieu();
            try {
                phieuGiamGiaRepository.findByMaPhieuWithLock(maPhieu).ifPresent(voucher -> {
                    Integer remaining = voucher.getSoLuongConLai();
                    if (remaining != null) {
                        voucher.setSoLuongConLai(remaining + 1);
                        phieuGiamGiaRepository.save(voucher);
                        log.info("[OrderViewService] Đã hoàn lại 1 lượt cho voucher '{}' khi hủy đơn chưa thanh toán #{}: {} -> {}",
                                maPhieu, idHoaDon, remaining, remaining + 1);
                    }
                });
            } catch (Exception vEx) {
                log.error("[OrderViewService] Lỗi hoàn lại voucher '{}' khi hủy đơn #{}: {}", maPhieu, idHoaDon, vEx.getMessage());
            }
        }

        // 8. Hủy vận đơn GHN nếu có
        if (hd.getGhnOrderCode() != null && !hd.getGhnOrderCode().isBlank()) {
            try {
                ghnService.cancelOrder(hd.getGhnOrderCode());
            } catch (Exception ghnEx) {
                log.warn("[OrderViewService] Lỗi khi gọi hủy vận đơn GHN '{}' cho đơn #{}: {}", hd.getGhnOrderCode(), idHoaDon, ghnEx.getMessage());
            }
        }

        // 9. Cập nhật trạng thái đơn hàng
        hd.setTrangThaiDonHang(OrderStatus.DA_HUY.getValue());
        hd.setPaymentStatus("CANCELLED");
        hd.setTrangThaiThanhToan("HUY");
        hd.setLyDoHuy(standardizedReason);

        String addition = "Lý do hủy: " + standardizedReason;
        String currentGhiChu = hd.getGhiChu();
        if (currentGhiChu == null || currentGhiChu.trim().isEmpty()) {
            hd.setGhiChu(addition.length() > 500 ? addition.substring(0, 500) : addition);
        } else {
            String newGhiChu = currentGhiChu + "\n" + addition;
            hd.setGhiChu(newGhiChu.length() > 500 ? newGhiChu.substring(0, 500) : newGhiChu);
        }

        hoaDonRepository.save(hd);

        // 10. Audit log
        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "CANCEL",
                currentStatus,
                OrderStatus.DA_HUY.getValue(),
                clientIp,
                "[CANCEL_UNPAID] Nhân viên hủy đơn hàng chưa thanh toán. Lý do: " + standardizedReason,
                roleStr
        );
    }

    /**
     * Hủy đơn hàng ONLINE ĐÃ THANH TOÁN (SePay / ZaloPay / chuyển khoản) kèm
     * XÁC NHẬN HOÀN TIỀN. Hoàn kho FIFO chính xác, hoàn voucher, hủy GHN, tạo
     * PaymentTransaction REFUND_SUCCESS riêng biệt.
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void cancelOrderPaidWithRefund(
            Integer idHoaDon,
            String lyDoHuy,
            String phuongThucHoanTienInput,
            BigDecimal soTienHoanInput,
            String maGiaoDichHoanTienInput,
            String ghiChuHoanTienInput,
            String anhChungTuHoanTienInput,
            Integer actingTaiKhoanId,
            String clientIp) {

        if (actingTaiKhoanId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện hủy đơn và hoàn tiền.");
        }
        TaiKhoan actingUser = taiKhoanRepository.findById(actingTaiKhoanId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Tài khoản người thực hiện không tồn tại."));
        String roleStr = "QL".equals(actingUser.getVaiTro()) ? "QUAN_LY" : "NHAN_VIEN";

        // 1. Lock HoaDon bằng Pessimistic Write Lock
        HoaDon hd = hoaDonRepository.findByIdWithLock(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        String currentStatus = hd.getTrangThaiDonHang();

        // 2. Double Cancel & Double Refund Idempotency Guard (Layer 1)
        if (OrderStatus.DA_HUY.getValue().equalsIgnoreCase(currentStatus)
                && paymentTransactionRepository.existsByOrder_IdAndStatus(idHoaDon, "REFUND_SUCCESS")) {
            log.warn("[DOUBLE_REFUND_CANCEL_GUARD] Đơn hàng #{} đã được hủy và hoàn tiền trước đó.", idHoaDon);
            return;
        }

        // 3. Backend Guard: Phải là đơn đã thanh toán
        if (!isOrderPaid(hd)) {
            throw new IllegalStateException("Đơn hàng chưa thanh toán. Vui lòng sử dụng chức năng hủy đơn thông thường.");
        }

        // 4. Guard: Không được hủy đơn đã giao hoặc đã bàn giao GHN / đang giao
        if (OrderStatus.DA_GIAO.getValue().equalsIgnoreCase(currentStatus)
                || OrderStatus.DA_BAN_GIAO_GHN.getValue().equalsIgnoreCase(currentStatus)
                || "dang_giao".equalsIgnoreCase(currentStatus)
                || "hoan_thanh".equalsIgnoreCase(currentStatus)) {
            throw new IllegalArgumentException("Không thể hủy đơn hàng đã bàn giao vận chuyển hoặc đã giao thành công!");
        }

        // 5. Validate Phương thức hoàn tiền
        if (phuongThucHoanTienInput == null || phuongThucHoanTienInput.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn phương thức hoàn tiền (CHUYEN_KHOAN hoặc TIEN_MAT).");
        }
        String phuongThuc = phuongThucHoanTienInput.trim().toUpperCase();
        if (!"CHUYEN_KHOAN".equals(phuongThuc) && !"TIEN_MAT".equals(phuongThuc)) {
            throw new IllegalArgumentException("Phương thức hoàn tiền không hợp lệ. Chỉ chấp nhận CHUYEN_KHOAN hoặc TIEN_MAT.");
        }

        // 6. Validate Số tiền hoàn (tính dựa trên số tiền khách thực tế đã thanh toán)
        BigDecimal totalOrderAmount = hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO;
        BigDecimal finalSoTienHoan = (soTienHoanInput != null) ? soTienHoanInput : totalOrderAmount;

        if (finalSoTienHoan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn phải lớn hơn 0.");
        }
        if (finalSoTienHoan.compareTo(totalOrderAmount) > 0) {
            throw new IllegalArgumentException("Số tiền hoàn không được lớn hơn tổng số tiền khách đã thanh toán (Tối đa: " + totalOrderAmount + " VNĐ).");
        }

        // 7. Validate Lý do hủy & Ghi chú hoàn
        String standardizedReason = "Khách hàng yêu cầu hủy";
        if (lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
            String trimmed = lyDoHuy.trim();
            String sanitized = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
            if (sanitized.length() > 500) {
                throw new IllegalArgumentException("Lý do hủy không được vượt quá 500 ký tự.");
            }
            if (!sanitized.isEmpty()) {
                standardizedReason = sanitized;
            }
        }
        String finalGhiChu = (ghiChuHoanTienInput != null) ? ghiChuHoanTienInput.trim() : "";

        // 8. Validate / Sinh Mã Giao Dịch Hoàn Tiền
        String finalMaGD;
        if ("CHUYEN_KHOAN".equals(phuongThuc)) {
            if (maGiaoDichHoanTienInput == null || maGiaoDichHoanTienInput.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập mã giao dịch chuyển khoản hoàn tiền.");
            }
            finalMaGD = maGiaoDichHoanTienInput.trim();
            if (paymentTransactionRepository.findByTransactionId(finalMaGD).isPresent()) {
                throw new IllegalArgumentException("Mã giao dịch hoàn tiền [" + finalMaGD + "] đã tồn tại trên hệ thống.");
            }
        } else {
            if (maGiaoDichHoanTienInput != null && !maGiaoDichHoanTienInput.trim().isEmpty()) {
                finalMaGD = maGiaoDichHoanTienInput.trim();
                if (paymentTransactionRepository.findByTransactionId(finalMaGD).isPresent()) {
                    throw new IllegalArgumentException("Mã giao dịch hoàn tiền [" + finalMaGD + "] đã tồn tại trên hệ thống.");
                }
            } else {
                finalMaGD = "REFUND-CANCEL-HD" + idHoaDon + "-" + System.currentTimeMillis();
            }
        }

        // 9. Hoàn kho FIFO (Chống hoàn kho 2 lần bằng daNhapKhoHoan)
        if (hd.getDaNhapKhoHoan() == null || !hd.getDaNhapKhoHoan()) {
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
            List<RestockItemRequest> restockReqs = new ArrayList<>();
            for (HoaDonChiTiet item : items) {
                if (item.getSanPhamChiTiet() != null && item.getSoLuong() != null && item.getSoLuong() > 0) {
                    restockReqs.add(RestockItemRequest.builder()
                            .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                            .quantityToRestock(item.getSoLuong())
                            .conBanDuoc(true)
                            .build());
                }
            }
            if (!restockReqs.isEmpty()) {
                inventoryLotService.hoanKho(restockReqs);
                log.info("[OrderViewService] Đã hoàn kho FIFO cho {} sản phẩm của đơn Online Paid bị hủy #{}", restockReqs.size(), idHoaDon);
            }
            hd.setDaNhapKhoHoan(true);
        }

        // 10. Hoàn lại lượt Voucher nếu có
        if (hd.getPhieuGiamGia() != null && hd.getPhieuGiamGia().getMaPhieu() != null) {
            String maPhieu = hd.getPhieuGiamGia().getMaPhieu();
            try {
                phieuGiamGiaRepository.findByMaPhieuWithLock(maPhieu).ifPresent(voucher -> {
                    Integer remaining = voucher.getSoLuongConLai();
                    if (remaining != null) {
                        voucher.setSoLuongConLai(remaining + 1);
                        phieuGiamGiaRepository.save(voucher);
                        log.info("[OrderViewService] Đã hoàn lại 1 lượt cho voucher '{}' khi hủy đơn Online Paid #{}: {} -> {}",
                                maPhieu, idHoaDon, remaining, remaining + 1);
                    }
                });
            } catch (Exception vEx) {
                log.error("[OrderViewService] Lỗi hoàn lại voucher '{}' khi hủy đơn #{}: {}", maPhieu, idHoaDon, vEx.getMessage());
            }
        }

        // 11. Hủy vận đơn GHN nếu có
        if (hd.getGhnOrderCode() != null && !hd.getGhnOrderCode().isBlank()) {
            try {
                ghnService.cancelOrder(hd.getGhnOrderCode());
            } catch (Exception ghnEx) {
                log.warn("[OrderViewService] Lỗi khi hủy vận đơn GHN '{}' cho đơn #{}: {}", hd.getGhnOrderCode(), idHoaDon, ghnEx.getMessage());
            }
        }

        // 12. Tạo và Lưu PaymentTransaction REFUND_SUCCESS riêng biệt (giữ nguyên giao dịch thanh toán gốc!)
        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setOrder(hd);
        refundTx.setTransactionId(finalMaGD);
        refundTx.setAmount(finalSoTienHoan);
        refundTx.setGateway("ORDER_CANCEL_REFUND");
        refundTx.setStatus("REFUND_SUCCESS");
        refundTx.setCreatedAt(LocalDateTime.now());

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("transactionType", "ORDER_CANCEL_REFUND");
        payloadMap.put("phuongThucHoanTien", phuongThuc);
        payloadMap.put("lyDoHuy", standardizedReason);
        payloadMap.put("ghiChu", finalGhiChu);
        if (anhChungTuHoanTienInput != null && !anhChungTuHoanTienInput.trim().isEmpty()) {
            payloadMap.put("anhChungTu", List.of(anhChungTuHoanTienInput.trim()));
        }
        payloadMap.put("nguoiThucHien", actingUser.getUsername());
        payloadMap.put("thoiGianHoanTien", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        try {
            refundTx.setRawPayload(objectMapper.writeValueAsString(payloadMap));
        } catch (Exception e) {
            log.warn("Failed to serialize refund rawPayload JSON: {}", e.getMessage());
        }

        paymentTransactionRepository.saveAndFlush(refundTx);

        // 13. Cập nhật trạng thái HoaDon chuẩn các Enum hiện có
        hd.setTrangThaiDonHang(OrderStatus.DA_HUY.getValue());
        hd.setPaymentStatus(PaymentStatus.REFUNDED.getValue());
        hd.setTrangThaiThanhToan("REFUNDED");
        hd.setRefundStatus(RefundStatus.COMPLETED);
        hd.setRefundTime(LocalDateTime.now());
        hd.setThoiGianHoanTien(LocalDateTime.now());
        hd.setLyDoHuy(standardizedReason);
        hd.setPhuongThucHoanTien(phuongThuc);
        hd.setSoTienHoan(finalSoTienHoan);
        hd.setMaGiaoDichHoanTien(finalMaGD);
        hd.setGhiChuHoanTien(finalGhiChu);
        if (anhChungTuHoanTienInput != null && !anhChungTuHoanTienInput.trim().isEmpty()) {
            hd.setAnhChungTuHoanTien(anhChungTuHoanTienInput.trim());
        }
        hd.setNguoiXacNhanThanhToan(actingUser.getUsername());

        String addition = "Lý do hủy: " + standardizedReason + " | Đã hoàn tiền: " + finalSoTienHoan + "đ (" + phuongThuc + " - Mã GD: " + finalMaGD + ")";
        String currentGhiChu = hd.getGhiChu();
        if (currentGhiChu == null || currentGhiChu.trim().isEmpty()) {
            hd.setGhiChu(addition.length() > 500 ? addition.substring(0, 500) : addition);
        } else {
            String newGhiChu = currentGhiChu + "\n" + addition;
            hd.setGhiChu(newGhiChu.length() > 500 ? newGhiChu.substring(0, 500) : newGhiChu);
        }

        hoaDonRepository.save(hd);

        // 14. Audit log
        auditService.log(
                actingTaiKhoanId,
                "HoaDon",
                Long.valueOf(hd.getId()),
                "CANCEL_WITH_REFUND",
                currentStatus,
                OrderStatus.DA_HUY.getValue(),
                clientIp,
                String.format("[CANCEL_WITH_REFUND] Hủy đơn Online đã thanh toán & hoàn tiền: Số tiền hoàn=%s VNĐ, PTTT=%s, Mã GD=%s, Lý do hủy=%s",
                        finalSoTienHoan, phuongThuc, finalMaGD, standardizedReason),
                roleStr
        );
    }

    /**
     * Non-transactional Orchestrator cho Phase 6: Giao sản phẩm đổi mới cho
     * khách hàng. Bước 1: Gọi
     * exchangeStockReservationService.reserveReplacementStock (Transaction A -
     * REQUIRES_NEW) ➔ Commit EXCHANGE_STOCK_ALLOCATED. Bước 2: Kiểm tra nếu
     * chưa có mã GHN_EXCHANGE ➔ Gọi GHN API với COD = 0. Bước 3: Lưu mã
     * GHN_EXCHANGE qua ghnShipmentPersistenceService.saveShipment
     * (REQUIRES_NEW). Bước 4: Gọi
     * exchangeStockReservationService.completeExchangeShipping (Transaction B -
     * REQUIRES_NEW) ➔ Commit EXCHANGE_SHIPPING.
     */
    public void xacNhanGiaoHangDoiMoiChoKhach(Integer idHoaDon, Integer actingTaiKhoanId, String clientIp) {
        if (actingTaiKhoanId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
        }

        // Bước 1: Transaction A - Phân bổ tồn kho exact SPCT & lưu trạng thái EXCHANGE_STOCK_ALLOCATED (REQUIRES_NEW commit DB)
        exchangeStockReservationService.reserveReplacementStock(idHoaDon, actingTaiKhoanId, clientIp);

        // Đọc lại thông tin đơn hàng sau khi Transaction A đã commit
        HoaDon hd = hoaDonRepository.findById(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        // Kiểm tra xem đã có mã GHN_EXCHANGE trong TichHopVanChuyen chưa
        String existingOrderCode = jdbcTemplate.query(
                "SELECT ma_van_don FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_EXCHANGE'",
                rs -> rs.next() ? rs.getString("ma_van_don") : null,
                idHoaDon
        );

        String finalOrderCode = existingOrderCode;
        if (finalOrderCode == null || finalOrderCode.isBlank()) {
            List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
            try {
                // Gọi GHN API với COD = 0 và KHÔNG tự MERGE CSDL bên trong GhnService
                finalOrderCode = ghnService.createExchangeShippingOrderOrThrow(hd, items, null, null);
            } catch (Exception e) {
                log.error("GHN exchange shipment API call failed for order #{}: {}", idHoaDon, e.getMessage(), e);
                throw new RuntimeException("Tạo vận đơn GHN cho đơn đổi hàng thất bại: " + e.getMessage(), e);
            }

            // Persist shipment local qua service REQUIRES_NEW riêng
            ghnShipmentPersistenceService.saveShipment(idHoaDon, finalOrderCode, "GHN_EXCHANGE", "ready_to_pick");
        }

        // Bước 4: Transaction B - Chuyển sang EXCHANGE_SHIPPING (REQUIRES_NEW commit DB)
        exchangeStockReservationService.completeExchangeShipping(idHoaDon, actingTaiKhoanId, clientIp);
    }

    private String getCustomerOrderTitle(String newStatus) {
        if (newStatus == null) {
            return "Cập nhật đơn hàng";
        }
        switch (newStatus.toLowerCase()) {
            case "cho_thanh_toan":
                return "Đơn hàng chờ thanh toán";
            case "cho_xac_nhan":
                return "Đơn hàng đã được ghi nhận";
            case "da_xac_nhan":
                return "Đơn hàng đã được xác nhận";
            case "dang_chuan_bi_hang":
                return "Đơn hàng đang được chuẩn bị";
            case "san_sang_giao":
                return "Đơn hàng đã sẵn sàng giao";
            case "da_tao_van_don_ghn":
                return "Đã tạo mã vận chuyển";
            case "da_ban_giao_ghn", "dang_lay_hang":
                return "Đã giao cho đơn vị vận chuyển";
            case "dang_giao":
                return "Đơn hàng đang trên đường giao";
            case "da_giao":
                return "Đơn hàng đã giao thành công";
            case "da_huy":
                return "Đơn hàng đã được hủy";
            case "yeu_cau_huy":
                return "Yêu cầu hủy đơn đã ghi nhận";
            case "cho_hoan_tien":
                return "Yêu cầu hoàn tiền đang xử lý";
            case "refunded":
                return "Hoàn tiền thành công";
            default:
                return "Cập nhật đơn hàng";
        }
    }

    private String buildCustomerOrderNotificationContent(String maDon, String currentStatus, String newStatus) {
        if (newStatus == null) {
            return "Đơn hàng " + maDon + " của bạn vừa được cập nhật.";
        }
        switch (newStatus.toLowerCase()) {
            case "cho_thanh_toan":
                return "Đơn hàng " + maDon + " của bạn đang chờ hoàn tất thanh toán.";
            case "cho_xac_nhan":
                return "Đơn hàng " + maDon + " của bạn đã được ghi nhận và đang chờ cửa hàng xác nhận.";
            case "da_xac_nhan":
                return "Đơn hàng " + maDon + " của bạn đã được cửa hàng xác nhận.";
            case "dang_chuan_bi_hang":
                return "Đơn hàng " + maDon + " của bạn đang được chuẩn bị trong kho.";
            case "san_sang_giao":
                return "Đơn hàng " + maDon + " của bạn đã sẵn sàng và chờ đơn vị vận chuyển tới lấy.";
            case "da_tao_van_don_ghn":
                return "Đơn hàng " + maDon + " của bạn đã được tạo mã vận chuyển.";
            case "da_ban_giao_ghn", "dang_lay_hang":
                return "Đơn hàng " + maDon + " của bạn đã được bàn giao cho đơn vị vận chuyển.";
            case "dang_giao":
                return "Đơn hàng " + maDon + " của bạn đang trên đường giao tới bạn!";
            case "da_giao":
                return "Đơn hàng " + maDon + " của bạn đã được giao thành công. Cảm ơn bạn đã mua sắm tại SMASH VN!";
            case "da_huy":
                return "Đơn hàng " + maDon + " của bạn đã được hủy.";
            case "yeu_cau_huy":
                return "Yêu cầu hủy cho đơn hàng " + maDon + " của bạn đã được ghi nhận.";
            case "cho_hoan_tien":
                return "Khoản hoàn tiền cho đơn hàng " + maDon + " của bạn đã được tiếp nhận và đang xử lý.";
            case "refunded":
                return "Khoản hoàn tiền cho đơn hàng " + maDon + " của bạn đã được hoàn tất.";
            default:
                String labelMoi = getStatusLabel(newStatus);
                return "Đơn hàng " + maDon + " hiện ở trạng thái: " + labelMoi + ".";
        }
    }

    private String getCustomerReturnTitle(ReturnStatus returnStatus, String loaiYeuCau) {
        boolean isDoi = "DOI".equalsIgnoreCase(loaiYeuCau);
        String tenNghiepVu = isDoi ? "đổi hàng" : "trả hàng";
        if (returnStatus == null) {
            return "Yêu cầu " + tenNghiepVu;
        }
        switch (returnStatus) {
            case PENDING_RETURN:
                return "Yêu cầu " + tenNghiepVu + " đã tiếp nhận";
            case RETURNED:
                return "Đã nhận sản phẩm " + tenNghiepVu;
            case EXCHANGE_STOCK_ALLOCATED:
                return "Sản phẩm đổi mới đã sẵn sàng";
            case REFUNDED:
            case EXCHANGED:
                return "Yêu cầu " + tenNghiepVu + " đã hoàn tất";
            case REJECTED:
                return "Yêu cầu " + tenNghiepVu + " đã bị hủy";
            default:
                return "Yêu cầu " + tenNghiepVu;
        }
    }

    private String buildCustomerReturnNotificationContent(String maDon, ReturnStatus returnStatus, String loaiYeuCau) {
        boolean isDoi = "DOI".equalsIgnoreCase(loaiYeuCau);
        String tenNghiepVu = isDoi ? "đổi hàng" : "trả hàng";
        if (returnStatus == null) {
            return "Yêu cầu " + tenNghiepVu + " cho đơn hàng " + maDon + " của bạn vừa được cập nhật.";
        }
        switch (returnStatus) {
            case PENDING_RETURN:
                return "Yêu cầu " + tenNghiepVu + " cho đơn hàng " + maDon + " của bạn đã được tiếp nhận.";
            case RETURNED:
                return "Cửa hàng đã nhận được sản phẩm " + tenNghiepVu + " của đơn hàng " + maDon + ".";
            case EXCHANGE_STOCK_ALLOCATED:
                return "Sản phẩm đổi mới cho đơn hàng " + maDon + " của bạn đã được chuẩn bị xong.";
            case REFUNDED:
            case EXCHANGED:
                return "Yêu cầu " + tenNghiepVu + " cho đơn hàng " + maDon + " của bạn đã hoàn tất. Cảm ơn bạn!";
            case REJECTED:
                return "Yêu cầu " + tenNghiepVu + " cho đơn hàng " + maDon + " của bạn đã bị từ chối hoặc hủy.";
            default:
                return "Yêu cầu " + tenNghiepVu + " cho đơn hàng " + maDon + " hiện ở trạng thái: " + returnStatus.getLabel() + ".";
        }
    }
}
