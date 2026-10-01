package com.smashvn.shop.service.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.product.PriceSnapshot;
import com.smashvn.shop.service.product.PricingService;
import com.smashvn.shop.util.VoucherCalculator;
import com.smashvn.shop.util.PhoneUtils;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.dto.user.PosRegisterCustomerRequest;
import com.smashvn.shop.dto.user.PosCustomerResponse;
import com.smashvn.shop.dto.inventory.RestockItemRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminPosService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final KhachHangRepository khachHangRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    private final DonViVanChuyenDAO donViVanChuyenDAO;
    private final AuditService auditService;
    private final PricingService pricingService;
    private final PasswordEncoder passwordEncoder;
    private final com.smashvn.shop.service.inventory.InventoryLotService inventoryLotService;



    private boolean isDangBan(String trangThai) {
        if (trangThai == null || trangThai.isBlank()) {
            return true;
        }
        String normalized = trangThai.trim().toLowerCase();
        return !"ngung_kinh_doanh".equals(normalized)
                && !"ngung_ban".equals(normalized)
                && !"inactive".equals(normalized)
                && !"disabled".equals(normalized);
    }

    // Tìm kiếm biến thể sản phẩm đang bán kèm lọc danh mục & thương hiệu
    @Transactional(readOnly = true)
    public List<SanPhamChiTiet> searchActiveVariants(String query, Integer idDanhMuc, Integer idThuongHieu) {
        String keyword = query == null ? "" : query.trim();
        return sanPhamChiTietRepository.searchActiveVariantsForPos(keyword, idDanhMuc, idThuongHieu);
    }

    /**
     * Tìm kiếm khách hàng — tương thích multi-role (dùng flag la_khach_hang =
     * true). Loại trừ tài khoản Khách Lẻ nội bộ (guest@smashvn.com).
     */
    public List<KhachHang> searchCustomers(String query) {
        List<KhachHang> customers = khachHangRepository.findByTaiKhoan_VaiTro("KH")
                .stream()
                .filter(java.util.Objects::nonNull)
                .filter(c -> c.getTaiKhoan() != null)
                .filter(c -> !"guest@smashvn.com".equalsIgnoreCase(nullToEmpty(c.getTaiKhoan().getUsername())))
                .collect(Collectors.toList());

        if (query == null || query.trim().isEmpty()) {
            return customers;
        }
        String lowerQuery = query.toLowerCase().trim();
        return customers.stream()
                .filter(c -> containsIgnoreCase(c.getHoKh(), lowerQuery)
                || containsIgnoreCase(c.getTenKh(), lowerQuery)
                || containsIgnoreCase(c.getSoDienThoaiKh(), lowerQuery)
                || containsIgnoreCase(c.getTaiKhoan().getUsername(), lowerQuery))
                .collect(Collectors.toList());
    }

    private boolean containsIgnoreCase(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    // Lấy thông tin voucher và xác thực
    public PhieuGiamGia checkVoucher(String maVoucher, BigDecimal tongTien) {
        if (maVoucher == null || maVoucher.trim().isEmpty()) {
            return null;
        }
        PhieuGiamGia voucher = phieuGiamGiaRepository.findByMaPhieu(maVoucher.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher: " + maVoucher));

        if (!voucher.getActive()) {
            throw new RuntimeException("Voucher đã ngưng hoạt động.");
        }
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getNgayBatDau() != null && now.isBefore(voucher.getNgayBatDau())) {
            throw new RuntimeException("Voucher chưa bắt đầu sử dụng.");
        }
        if (voucher.getNgayKetThuc() != null && now.isAfter(voucher.getNgayKetThuc())) {
            throw new RuntimeException("Voucher đã hết hạn sử dụng.");
        }
        if (voucher.getSoLuongConLai() != null && voucher.getSoLuongConLai() <= 0) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng.");
        }
        if (tongTien.compareTo(voucher.getGiaTriDonHangToiThieu()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu để áp dụng voucher: " + voucher.getGiaTriDonHangToiThieu() + " đ");
        }
        return voucher;
    }

    // DTO cho POS item
    public static class PosItem {

        public Integer idSanPhamChiTiet;
        public Integer soLuong;
    }

    /**
     * Hàm thanh toán POS trong transaction an toàn.
     *
     * @param phuongThucPos TIEN_MAT | CHUYEN_KHOAN (phương thức thanh toán POS)
     * @param ghiChu Ghi chú hóa đơn (nullable)
     */
    @Transactional
    public HoaDon thanhToanPos(Integer idKhachHang, String maVoucher, List<PosItem> items,
            String phuongThucPos, String maGiaoDich, String ghiChu,
            Integer idNhanVienTaiKhoan, String clientIp) {
        String sanitizedGiaoDich = null;
        if (maGiaoDich != null) {
            String trimmed = maGiaoDich.trim();
            sanitizedGiaoDich = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
            if (sanitizedGiaoDich.length() > 100) {
                throw new RuntimeException("Mã giao dịch không được vượt quá 100 ký tự!");
            }
        }

        String sanitizedGhiChu = null;
        if (ghiChu != null) {
            String trimmed = ghiChu.trim();
            sanitizedGhiChu = org.jsoup.Jsoup.clean(trimmed, org.jsoup.safety.Safelist.none());
            if (sanitizedGhiChu.length() > 500) {
                throw new RuntimeException("Ghi chú không được vượt quá 500 ký tự!");
            }
        }

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Đơn hàng không có sản phẩm nào!");
        }

        // 1. Xác định nhân viên thực hiện
        TaiKhoan nvTk = taiKhoanRepository.findById(idNhanVienTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Nhân viên thực hiện giao dịch không hợp lệ!"));

        NhanVien nhanVien = nhanVienRepository.findByTaiKhoanId(nvTk.getId());

        // 2. Xác định khách hàng (Nếu không có, dùng tài khoản Khách Lẻ mặc định)
        KhachHang khachHang;
        if (idKhachHang == null || idKhachHang == -1) {
            TaiKhoan guestTk = taiKhoanRepository.findByUsername("guest@smashvn.com");
            if (guestTk == null) {
                TaiKhoan tk = new TaiKhoan();
                tk.setUsername("guest@smashvn.com");
                tk.setMatKhau("GUEST_NO_PASSWORD");
                tk.setVaiTro("KH");
                tk.setTrangThai("hoat_dong");
                guestTk = taiKhoanRepository.save(tk);
            }
            final TaiKhoan guestTkFinal = guestTk;
            khachHang = khachHangRepository.findByTaiKhoan_Username("guest@smashvn.com").orElseGet(() -> {
                KhachHang kh = new KhachHang();
                kh.setTaiKhoan(guestTkFinal);
                kh.setHoKh("Khách");
                kh.setTenKh("Lẻ");
                kh.setSoDienThoaiKh("0000000000");
                kh.setNhanBanTin(false);
                kh.setLaTaiKhoanNoiBo(false);
                return khachHangRepository.save(kh);
            });
        } else {
            khachHang = khachHangRepository.findById(idKhachHang)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng trong hệ thống!"));
        }

        // 3. Ánh xạ phương thức thanh toán POS → PhuongThucThanhToan (giữ FK cho tương thích)
        final String targetCode = "CHUYEN_KHOAN".equalsIgnoreCase(phuongThucPos) ? "CHUYEN_KHOAN" : "TIEN_MAT";
        final String tenPhuongThucCan = "CHUYEN_KHOAN".equalsIgnoreCase(phuongThucPos) ? "chuyển khoản" : "tiền mặt";
        List<PhuongThucThanhToan> allPttt = phuongThucThanhToanDAO.findAll();
        PhuongThucThanhToan pttt = allPttt.stream()
                .filter(p -> (p.getMaPhuongThuc() != null && targetCode.equalsIgnoreCase(p.getMaPhuongThuc()))
                          || ("TIEN_MAT".equalsIgnoreCase(targetCode) && (p.getId() != null && p.getId() == 3))
                          || ("CHUYEN_KHOAN".equalsIgnoreCase(targetCode) && (p.getId() != null && p.getId() == 4)))
                .findFirst()
                .orElseGet(() -> {
                    Integer targetId = "TIEN_MAT".equalsIgnoreCase(targetCode) ? 3 : 4;
                    return phuongThucThanhToanDAO.findById(targetId).orElseGet(() -> {
                        PhuongThucThanhToan newP = new PhuongThucThanhToan();
                        newP.setMaPhuongThuc(targetCode);
                        newP.setTenPhuongThuc("TIEN_MAT".equalsIgnoreCase(targetCode) ? "Tiền mặt tại quầy" : "Chuyển khoản tại quầy");
                        return phuongThucThanhToanDAO.save(newP);
                    });
                });

        // 4. Lấy đơn vị vận chuyển (Bán tại quầy)
        List<DonViVanChuyen> allDvvc = donViVanChuyenDAO.findAll();
        DonViVanChuyen dvvc;
        if (allDvvc.isEmpty()) {
            DonViVanChuyen defaultDvvc = new DonViVanChuyen();
            defaultDvvc.setTenDonVi("Mua tại quầy");
            defaultDvvc.setHotline("000000");
            dvvc = donViVanChuyenDAO.save(defaultDvvc);
        } else {
            dvvc = allDvvc.stream()
                    .filter(d -> d.getTenDonVi().toLowerCase().contains("quầy") || d.getTenDonVi().toLowerCase().contains("chỗ"))
                    .findFirst()
                    .orElseGet(() -> {
                        DonViVanChuyen defaultDvvc = new DonViVanChuyen();
                        defaultDvvc.setTenDonVi("Mua tại quầy");
                        defaultDvvc.setHotline("000000");
                        return donViVanChuyenDAO.save(defaultDvvc);
                    });
        }

        // 5. Phân bổ tồn kho FIFO bằng InventoryLotService (Đã khóa idSanPham theo thứ tự ASC)
        List<com.smashvn.shop.dto.inventory.OrderItemRequest> itemRequests = new java.util.ArrayList<>();
        for (PosItem item : items) {
            if (item.soLuong == null || item.soLuong <= 0) {
                throw new RuntimeException("Số lượng sản phẩm không hợp lệ!");
            }
            itemRequests.add(com.smashvn.shop.dto.inventory.OrderItemRequest.builder()
                    .representativeSpctId(item.idSanPhamChiTiet)
                    .quantity(item.soLuong)
                    .build());
        }

        com.smashvn.shop.dto.inventory.AllocationResult allocResult = inventoryLotService.allocateFifo(itemRequests);
        if (allocResult.status() != com.smashvn.shop.dto.inventory.AllocationStatus.SUCCESS) {
            throw new RuntimeException(allocResult.message());
        }

        BigDecimal tongTienHang = BigDecimal.ZERO;
        List<HoaDonChiTiet> listCt = new java.util.ArrayList<>();

        for (com.smashvn.shop.dto.inventory.LotAllocation alloc : allocResult.allocations()) {
            SanPhamChiTiet spct = alloc.allocatedSpct();
            PriceSnapshot priceSnapshot = pricingService.buildPriceSnapshot(spct);
            BigDecimal sellingPrice = priceSnapshot.giaBanSauGiam();
            BigDecimal itemTotal = sellingPrice.multiply(new BigDecimal(alloc.quantityAllocated()));
            tongTienHang = tongTienHang.add(itemTotal);

            HoaDonChiTiet hdct = new HoaDonChiTiet();
            hdct.setSanPhamChiTiet(spct);
            hdct.setSoLuong(alloc.quantityAllocated());
            hdct.setDonGia(sellingPrice);
            hdct.setGiaGoc(priceSnapshot.giaNiemYet());
            hdct.setGiaSauGiam(sellingPrice);
            hdct.setTenSanPhamSnapshot(spct.getSanPham().getTenSanPham());

            String sku = spct.getSku();
            if (sku == null || sku.isBlank()) {
                String maSp = (spct.getSanPham() != null && spct.getSanPham().getMaSanPham() != null)
                        ? spct.getSanPham().getMaSanPham()
                        : com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateProductCode(spct.getSanPham().getId());
                sku = com.smashvn.shop.util.ProductCodeAndSkuGenerator.generateVariantSku(maSp, spct.getId());
            }
            hdct.setSkuSnapshot(sku);
            hdct.setTenDotGiamGiaSnapshot(priceSnapshot.tenDotGiamGia());
            hdct.setThuocTinhSnapshot(inventoryLotService.getDisplayTitle(spct));
            listCt.add(hdct);
        }


        // 6. Xử lý Voucher và Khóa Pessimistic Write voucher
        PhieuGiamGia phieu = null;
        BigDecimal giamGia = BigDecimal.ZERO;

        if (maVoucher != null && !maVoucher.trim().isEmpty()) {
            phieu = phieuGiamGiaRepository.findByMaPhieuWithLock(maVoucher.trim())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher: " + maVoucher));

            if (!phieu.getActive()) {
                throw new RuntimeException("Voucher đã ngưng hoạt động.");
            }
            LocalDateTime now = LocalDateTime.now();
            if (phieu.getNgayBatDau() != null && now.isBefore(phieu.getNgayBatDau())) {
                throw new RuntimeException("Voucher chưa bắt đầu.");
            }
            if (phieu.getNgayKetThuc() != null && now.isAfter(phieu.getNgayKetThuc())) {
                throw new RuntimeException("Voucher đã hết hạn.");
            }
            if (phieu.getSoLuongConLai() != null && phieu.getSoLuongConLai() <= 0) {
                throw new RuntimeException("Voucher đã hết lượt sử dụng.");
            }
            if (tongTienHang.compareTo(phieu.getGiaTriDonHangToiThieu()) < 0) {
                throw new RuntimeException("Đơn hàng chưa đạt giá trị tối thiểu của voucher (" + phieu.getGiaTriDonHangToiThieu() + " đ)");
            }

            // Tính tiền giảm qua VoucherCalculator (bao gồm cả giới hạn giảm tối đa)
            giamGia = VoucherCalculator.calculateVoucherDiscount(tongTienHang, phieu);

            // Trừ lượt sử dụng voucher
            phieu.setSoLuongConLai(phieu.getSoLuongConLai() - 1);
            phieuGiamGiaRepository.save(phieu);
        }

        BigDecimal tongTienCuoi = tongTienHang.subtract(giamGia);
        if (tongTienCuoi.compareTo(BigDecimal.ZERO) < 0) {
            tongTienCuoi = BigDecimal.ZERO;
        }

        if ("TIEN_MAT".equalsIgnoreCase(phuongThucPos)) {
            tongTienCuoi = roundCashAmount(tongTienCuoi);
        }

        // 7. Tạo hóa đơn
        HoaDon hd = new HoaDon();
        hd.setKhachHang(khachHang);
        hd.setNhanVien(nhanVien);
        hd.setPhuongThucThanhToan(pttt);
        hd.setPhieuGiamGia(phieu);
        hd.setDonViVanChuyen(dvvc);
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(tongTienCuoi);
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setDiaChiNhan("Bán tại quầy");
        hd.setSdtNhan(khachHang.getSoDienThoaiKh() != null && !khachHang.getSoDienThoaiKh().trim().isEmpty()
                ? khachHang.getSoDienThoaiKh()
                : "0000000000");
        hd.setTenNguoiNhan(
                khachHang.getTenKh() != null
                && !khachHang.getTenKh().trim().isEmpty()
                && !"Lẻ".equalsIgnoreCase(khachHang.getTenKh().trim())
                ? khachHang.getTenKh().trim()
                : "Khách lẻ"
        );
        if (khachHang != null && khachHang.getTaiKhoan() != null && khachHang.getTaiKhoan().getUsername() != null) {
            String username = khachHang.getTaiKhoan().getUsername().trim();
            if (!username.isEmpty() && username.contains("@") && !"khachle@smashvn.local".equalsIgnoreCase(username)) {
                hd.setEmailNguoiNhan(username);
            } else {
                hd.setEmailNguoiNhan(null);
            }
        } else {
            hd.setEmailNguoiNhan(null);
        }
        hd.setGhiChu(sanitizedGhiChu);
        hd.setMaGiaoDich(sanitizedGiaoDich);

        // Bán tại quầy: Tiền mặt -> Hoàn thành ngay; Chuyển khoản -> Tạo đơn chờ thanh toán để nhận Webhook SePay
        if ("CHUYEN_KHOAN".equalsIgnoreCase(phuongThucPos)) {
            hd.setTrangThaiDonHang(OrderStatus.CHO_THANH_TOAN.getValue());
            hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
            hd.setPaymentStatus(PaymentStatus.PENDING.getValue());
            hd.setNguoiXacNhanThanhToan(null);
            hd.setThoiGianXacNhan(null);
            hd.setPaidAt(null);
        } else {
            hd.setTrangThaiDonHang("da_giao");
            hd.setTrangThaiThanhToan("DA_THANH_TOAN");
            hd.setPaymentStatus(PaymentStatus.PAID.getValue());
            hd.setNguoiXacNhanThanhToan(nhanVien != null ? nhanVien.getHoTenNv() : "Nhân viên hệ thống");
            hd.setThoiGianXacNhan(LocalDateTime.now());
            hd.setPaidAt(LocalDateTime.now());
        }

        hd.setSoTienGiamVoucher(giamGia);
        if (phieu != null) {
            hd.setMaVoucherApDung(phieu.getMaPhieu());
            hd.setTenVoucherApDung("Voucher " + phieu.getMaPhieu());
            String limitDesc = phieu.getGiaTriGiamToiDa() != null ? " (Giảm tối đa " + phieu.getGiaTriGiamToiDa() + "đ)" : "";
            hd.setMoTaVoucherSnapshot("Giảm " + phieu.getGiaTri() + ("%".equals(phieu.getDonVi()) ? "%" : "đ") + limitDesc + " cho đơn hàng từ " + phieu.getGiaTriDonHangToiThieu() + "đ");
        } else {
            hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        }

        hd = hoaDonRepository.save(hd);

        // 8. Lưu các chi tiết hóa đơn
        for (HoaDonChiTiet ct : listCt) {
            ct.setHoaDon(hd);
            hoaDonChiTietRepository.save(ct);
        }

        // 9. Ghi nhật ký kiểm toán
        String ptLabel = "CHUYEN_KHOAN".equalsIgnoreCase(phuongThucPos) ? "Chuyển khoản" : "Tiền mặt";
        String initialStatus = "CHUYEN_KHOAN".equalsIgnoreCase(phuongThucPos) ? OrderStatus.CHO_THANH_TOAN.getValue() : OrderStatus.DA_GIAO.getValue();
        auditService.log(nvTk.getId(), "HoaDon", Long.valueOf(hd.getId()), "INSERT", null,
                "status=" + initialStatus + ", paymentStatus=" + hd.getPaymentStatus() + ", trangThaiThanhToan=" + hd.getTrangThaiThanhToan(), clientIp,
                "Thanh toán POS - " + ptLabel + " - Tổng tiền: " + tongTienCuoi + " đ (Mã: " + hd.getMaDonHang() + ")", nvTk.getVaiTro());

        return hd;
    }

    @Transactional
    public void confirmPaymentPos(Integer hoaDonId, Integer idNhanVienTaiKhoan) {
        HoaDon hd = hoaDonRepository.findById(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + hoaDonId));

        if (!OrderStatus.CHO_THANH_TOAN.getValue().equals(hd.getTrangThaiDonHang())) {
            throw new RuntimeException("Hóa đơn này không ở trạng thái chờ thanh toán!");
        }

        TaiKhoan nvTk = taiKhoanRepository.findById(idNhanVienTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Nhân viên thực hiện giao dịch không hợp lệ!"));
        NhanVien nhanVien = nhanVienRepository.findByTaiKhoanId(nvTk.getId());

        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setPaymentStatus(PaymentStatus.PAID.getValue());
        hd.setTrangThaiDonHang(OrderStatus.DA_GIAO.getValue());
        hd.setNguoiXacNhanThanhToan(nhanVien != null ? nhanVien.getHoTenNv() : "Nhân viên hệ thống");
        hd.setThoiGianXacNhan(LocalDateTime.now());
        hd.setPaidAt(LocalDateTime.now());
        hoaDonRepository.save(hd);

        auditService.log(nvTk.getId(), "HoaDon", Long.valueOf(hd.getId()), "UPDATE",
                "status=" + OrderStatus.CHO_THANH_TOAN.getValue(), "status=" + OrderStatus.DA_GIAO.getValue(), "127.0.0.1",
                "Nhân viên xác nhận thanh toán chuyển khoản thủ công cho đơn POS. Mã: " + hd.getMaDonHang(), nvTk.getVaiTro());
    }

    @Transactional
    public void cancelOrderPos(Integer hoaDonId, Integer idNhanVienTaiKhoan) {
        HoaDon hd = hoaDonRepository.findByIdWithLock(hoaDonId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + hoaDonId));

        // Share the webhook's order lock. A caller may already hold a stale managed
        // snapshot; flush its own changes before replacing that snapshot under the lock.
        entityManager.flush();
        entityManager.refresh(hd, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        if (OrderStatus.DA_HUY.getValue().equals(hd.getTrangThaiDonHang())) {
            throw new RuntimeException("Hóa đơn này đã được hủy trước đó!");
        }

        boolean isPosOrder = hd.getMaDonHang() != null && hd.getMaDonHang().startsWith("HDSVN");
        if (!isPosOrder && !OrderStatus.CHO_THANH_TOAN.getValue().equals(hd.getTrangThaiDonHang())) {
            throw new RuntimeException("Chỉ có thể hủy hóa đơn ở trạng thái chờ thanh toán!");
        }

        TaiKhoan nvTk = taiKhoanRepository.findById(idNhanVienTaiKhoan)
                .orElseThrow(() -> new RuntimeException("Nhân viên thực hiện không hợp lệ!"));

        // 1. Hoàn lại tồn kho sản phẩm chi tiết bằng cách reverse chính xác các lô (SPCT) đã được allocate trong HoaDonChiTiet
        List<HoaDonChiTiet> details = hoaDonChiTietRepository.findByHoaDon_Id(hoaDonId);
        if (details == null || details.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin sản phẩm chi tiết (allocation) của đơn hàng POS ID: " + hoaDonId);
        }

        List<RestockItemRequest> restockReqs = new java.util.ArrayList<>();
        for (HoaDonChiTiet ct : details) {
            if (ct.getSanPhamChiTiet() == null || ct.getSoLuong() == null || ct.getSoLuong() <= 0) {
                throw new RuntimeException("Chi tiết đơn hàng không hợp lệ cho sản phẩm ID: " + (ct.getSanPhamChiTiet() != null ? ct.getSanPhamChiTiet().getId() : "null"));
            }
            restockReqs.add(RestockItemRequest.builder()
                    .idSanPhamChiTiet(ct.getSanPhamChiTiet().getId())
                    .quantityToRestock(ct.getSoLuong())
                    .conBanDuoc(true)
                    .build());
        }

        // Thực hiện hoàn kho qua InventoryLotService (Đã thực hiện lock kho theo ID sản phẩm ASC chống deadlock và cập nhật chính xác tồn kho từng SPCT lô đã bán)
        inventoryLotService.hoanKho(restockReqs);

        // 2. Hoàn lại lượt sử dụng Voucher (nếu có)
        if (hd.getPhieuGiamGia() != null) {
            PhieuGiamGia phieu = phieuGiamGiaRepository.findByMaPhieuWithLock(hd.getPhieuGiamGia().getMaPhieu())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher: " + hd.getPhieuGiamGia().getMaPhieu()));
            phieu.setSoLuongConLai(phieu.getSoLuongConLai() + 1);
            phieuGiamGiaRepository.save(phieu);
        }

        // 3. Cập nhật trạng thái hóa đơn sang đã hủy
        hd.setTrangThaiDonHang(OrderStatus.DA_HUY.getValue());
        hd.setTrangThaiThanhToan("HUY");
        hd.setPaymentStatus(PaymentStatus.FAILED.getValue());
        hoaDonRepository.save(hd);

        auditService.log(nvTk.getId(), "HoaDon", Long.valueOf(hd.getId()), "UPDATE",
                OrderStatus.CHO_THANH_TOAN.getValue(), OrderStatus.DA_HUY.getValue(), "127.0.0.1",
                "Nhân viên hủy đơn hàng chờ thanh toán POS. Mã: " + hd.getMaDonHang(), nvTk.getVaiTro());
    }

    @Transactional
    public PosCustomerResponse registerCustomerAtPos(PosRegisterCustomerRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu yêu cầu không hợp lệ!");
        }
        if (request.getHoTen() == null || request.getHoTen().trim().isEmpty()) {
            throw new RuntimeException("Họ tên không được để trống!");
        }
        if (request.getSoDienThoai() == null || request.getSoDienThoai().trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống!");
        }

        String normalizedPhone = PhoneUtils.normalize(request.getSoDienThoai());
        if (!PhoneUtils.isValid(normalizedPhone)) {
            throw new RuntimeException("Số điện thoại không đúng định dạng Việt Nam!");
        }

        String username = normalizedPhone;
        String name = request.getHoTen().trim();

        // 1. Kiểm tra nếu KhachHang đã tồn tại theo số điện thoại
        KhachHang existingKh = khachHangRepository.findBySoDienThoaiKh(normalizedPhone);
        if (existingKh != null) {
            return PosCustomerResponse.builder()
                    .success(true)
                    .created(false)
                    .requiresConfirmation(true)
                    .message("Khách hàng đã tồn tại trong hệ thống.")
                    .customer(PosCustomerResponse.CustomerDto.builder()
                            .id(existingKh.getId())
                            .hoTen(existingKh.getHoTenKh() != null ? existingKh.getHoTenKh() : "")
                            .sdt(existingKh.getSoDienThoaiKh())
                            .build())
                    .build();
        }

        // 2. Kiểm tra nếu Username (SĐT) đã tồn tại trong bảng TaiKhoan
        if (taiKhoanRepository.existsByUsername(username)) {
            throw new RuntimeException("Số điện thoại này đã được sử dụng!");
        }

        // 3. Tạo mới cả TaiKhoan và KhachHang trong cùng transaction
        TaiKhoan newTk = new TaiKhoan();
        newTk.setUsername(username);
        newTk.setMatKhau(passwordEncoder.encode("12345678"));
        newTk.setVaiTro("KH");
        newTk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        newTk.setNgayTao(LocalDateTime.now());
        TaiKhoan savedTk = taiKhoanRepository.save(newTk);

        KhachHang newKh = new KhachHang();
        newKh.setTaiKhoan(savedTk);
        newKh.setHoKh("");
        newKh.setTenKh(name);
        newKh.setHoTenKh(name);
        newKh.setSoDienThoaiKh(normalizedPhone);
        newKh.setNgayTao(LocalDateTime.now());
        KhachHang savedKh = khachHangRepository.save(newKh);

        return PosCustomerResponse.builder()
                .success(true)
                .created(true)
                .requiresConfirmation(false)
                .message("Đăng ký tài khoản khách hàng thành công.")
                .customer(PosCustomerResponse.CustomerDto.builder()
                        .id(savedKh.getId())
                        .hoTen(savedKh.getHoTenKh())
                        .sdt(savedKh.getSoDienThoaiKh())
                        .build())
                .build();
    }

    /**
     * Quy tắc làm tròn tiền mặt tại quầy POS:
     * - Phần lẻ dưới 500đ (< 500) -> Làm tròn xuống nghìn gần nhất.
     * - Phần lẻ từ 500đ trở lên (>= 500) -> Làm tròn lên nghìn gần nhất.
     */
    public static BigDecimal roundCashAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        long rawAmount = amount.longValue();
        long remainder = rawAmount % 1000;
        long roundedAmount;
        if (remainder < 500) {
            roundedAmount = (rawAmount / 1000) * 1000;
        } else {
            roundedAmount = ((rawAmount / 1000) + 1) * 1000;
        }
        return BigDecimal.valueOf(Math.max(0, roundedAmount));
    }
}

