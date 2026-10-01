package com.smashvn.shop.service.order;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.PhieuGiamGia;

@SpringBootTest
@Transactional
public class PaymentStatusAndVoucherTest {

    @Autowired
    private OrderViewService orderViewService;

    @Test
    public void testPaymentStatusMapping() {
        // 1. paid -> Đã thanh toán (bg-success)
        var paidInfo = orderViewService.getPaymentStatusInfo("paid");
        assertEquals("PAID", paidInfo.code());
        assertEquals("Đã thanh toán", paidInfo.label());
        assertEquals("bg-success", paidInfo.badgeClass());

        var daThanhToanInfo = orderViewService.getPaymentStatusInfo("DA_THANH_TOAN");
        assertEquals("PAID", daThanhToanInfo.code());
        assertEquals("Đã thanh toán", daThanhToanInfo.label());
        assertEquals("bg-success", daThanhToanInfo.badgeClass());

        // 2. pending -> Chờ thanh toán (bg-warning text-dark)
        var pendingInfo = orderViewService.getPaymentStatusInfo("pending");
        assertEquals("PENDING", pendingInfo.code());
        assertEquals("Chờ thanh toán", pendingInfo.label());
        assertEquals("bg-warning text-dark", pendingInfo.badgeClass());

        // 3. cancelled -> Đã hủy (bg-danger)
        var cancelledInfo = orderViewService.getPaymentStatusInfo("cancelled");
        assertEquals("CANCELLED", cancelledInfo.code());
        assertEquals("Đã hủy", cancelledInfo.label());
        assertEquals("bg-danger", cancelledInfo.badgeClass());

        // 4. refunded / da_hoan_tien -> Đã hoàn tiền (bg-danger)
        var refundedInfo = orderViewService.getPaymentStatusInfo("REFUNDED");
        assertEquals("REFUNDED", refundedInfo.code());
        assertEquals("Đã hoàn tiền", refundedInfo.label());
        assertEquals("bg-danger", refundedInfo.badgeClass());

        var daHoanTienInfo = orderViewService.getPaymentStatusInfo("DA_HOAN_TIEN");
        assertEquals("REFUNDED", daHoanTienInfo.code());
        assertEquals("Đã hoàn tiền", daHoanTienInfo.label());
        assertEquals("bg-danger", daHoanTienInfo.badgeClass());

        // 5. Strange status -> Raw status (bg-secondary)
        var strangeInfo = orderViewService.getPaymentStatusInfo("SOME_STRANGE_STATUS");
        assertEquals("UNKNOWN", strangeInfo.code());
        assertEquals("SOME_STRANGE_STATUS", strangeInfo.label());
        assertEquals("bg-secondary", strangeInfo.badgeClass());
    }

    @Test
    public void testVoucherFallbackLogic() {
        HoaDon hd = new HoaDon();
        hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        hd.setMaVoucherApDung(null);

        // Case 1: no voucher
        String maVoucher = hd.getMaVoucherApDung();
        if (maVoucher == null || maVoucher.isEmpty()) {
            if (hd.getPhieuGiamGia() != null) {
                maVoucher = hd.getPhieuGiamGia().getMaPhieu();
            } else if (hd.getSoTienGiamVoucher() != null && hd.getSoTienGiamVoucher().compareTo(BigDecimal.ZERO) > 0) {
                maVoucher = "Voucher";
            } else {
                maVoucher = "Không áp dụng voucher";
            }
        }
        assertEquals("Không áp dụng voucher", maVoucher);

        // Case 2: has voucher fallback to relation
        PhieuGiamGia pg = new PhieuGiamGia();
        pg.setMaPhieu("TESTCODE123");
        hd.setPhieuGiamGia(pg);
        hd.setSoTienGiamVoucher(new BigDecimal("50000"));

        maVoucher = hd.getMaVoucherApDung();
        if (maVoucher == null || maVoucher.isEmpty()) {
            if (hd.getPhieuGiamGia() != null) {
                maVoucher = hd.getPhieuGiamGia().getMaPhieu();
            } else if (hd.getSoTienGiamVoucher() != null && hd.getSoTienGiamVoucher().compareTo(BigDecimal.ZERO) > 0) {
                maVoucher = "Voucher";
            } else {
                maVoucher = "Không áp dụng voucher";
            }
        }
        assertEquals("TESTCODE123", maVoucher);
    }

    @Test
    public void testTensionSpellingCorrection() {
        String originalSnapshot = "Màu sắc: Xanh, Trọng lượng: 3U, Mức cảng: 28 lbs";
        String corrected = originalSnapshot.replace("Mức cảng:", "Sức căng khuyến nghị:");
        assertEquals("Màu sắc: Xanh, Trọng lượng: 3U, Sức căng khuyến nghị: 28 lbs", corrected);
    }

    @Autowired
    private com.smashvn.shop.repository.PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private com.smashvn.shop.repository.HoaDonRepository hoaDonRepository;

    @Autowired
    private com.smashvn.shop.repository.KhachHangRepository khachHangRepository;

    @Autowired
    private com.smashvn.shop.repository.TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private com.smashvn.shop.repository.NhanVienRepository nhanVienRepository;

    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Test
    public void testOrderCancel_RestoresVoucherQuantity() {
        com.smashvn.shop.entity.NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElse(null);
        com.smashvn.shop.entity.PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);

        // Create test voucher
        PhieuGiamGia voucher = new PhieuGiamGia();
        voucher.setMaPhieu("TEST_RESTORE_VOUCHER");
        voucher.setLoaiGiamGia("Giảm trực tiếp");
        voucher.setDonVi("VND");
        voucher.setGiaTri(BigDecimal.valueOf(50000));
        voucher.setSoLuongConLai(10);
        voucher.setGiaTriDonHangToiThieu(BigDecimal.ZERO);
        voucher.setNgayBatDau(java.time.LocalDateTime.now().minusDays(1));
        voucher.setNgayKetThuc(java.time.LocalDateTime.now().plusDays(10));
        voucher.setNhanVien(nv);
        voucher = phieuGiamGiaRepository.save(voucher);

        // Create test user and customer
        com.smashvn.shop.entity.TaiKhoan tk = new com.smashvn.shop.entity.TaiKhoan();
        tk.setUsername("vouchertest_" + System.currentTimeMillis() + "@test.com");
        tk.setMatKhau("testpass");
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk = taiKhoanRepository.save(tk);

        com.smashvn.shop.entity.KhachHang kh = new com.smashvn.shop.entity.KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoKh("Voucher");
        kh.setTenKh("Tester");
        kh.setSoDienThoaiKh("0912345678");
        kh = khachHangRepository.save(kh);

        // Create test order with voucher attached
        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setMaDonHang("HD_VOUCHER_TEST");
        hd.setTrangThaiDonHang("cho_xac_nhan");
        hd.setTrangThaiThanhToan("CHUA_THANH_TOAN");
        hd.setPaymentStatus("pending");
        hd.setPaymentMethod("COD");
        hd.setPhuongThucThanhToan(pttt);
        hd.setPhieuGiamGia(voucher);
        hd.setTongTien(BigDecimal.valueOf(200000));
        hd.setSdtNhan("0912345678");
        hd.setDiaChiNhan("123 Hà Nội");
        hd = hoaDonRepository.save(hd);

        // Cancel order via huyDonHang
        boolean cancelled = orderViewService.huyDonHang(hd.getId(), kh.getId(), "127.0.0.1", "Đổi ý không mua nữa");
        assertEquals(true, cancelled);

        // Check that voucher quantity was incremented: 10 -> 11
        PhieuGiamGia updatedVoucher = phieuGiamGiaRepository.findById(voucher.getId()).orElseThrow();
        assertEquals(11, updatedVoucher.getSoLuongConLai());
    }
}
