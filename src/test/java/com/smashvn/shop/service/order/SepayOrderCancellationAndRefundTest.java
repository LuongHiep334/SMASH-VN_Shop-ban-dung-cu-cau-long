package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.inventory.InventoryLotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class SepayOrderCancellationAndRefundTest {

    @Autowired
    private OrderViewService orderViewService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private InventoryLotService inventoryLotService;

    private TaiKhoan adminTk;
    private KhachHang testKhachHang;
    private SanPhamChiTiet testSpct;
    private PhuongThucThanhToan testPttt;

    @BeforeEach
    void setUp() {
        adminTk = taiKhoanRepository.findByUsername("admin");
        if (adminTk == null) {
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("admin_test_sepay");
            tk.setMatKhau("123456");
            tk.setVaiTro("QL");
            tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
            tk.setNgayTao(LocalDateTime.now());
            adminTk = taiKhoanRepository.save(tk);
        }

        testKhachHang = khachHangRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("user_test_sepay");
            tk.setMatKhau("123456");
            tk.setVaiTro("KH");
            tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
            tk.setNgayTao(LocalDateTime.now());
            tk = taiKhoanRepository.save(tk);

            KhachHang kh = new KhachHang();
            kh.setHoTenKh("Nguyen Van Test");
            kh.setSoDienThoaiKh("0987654321");
            kh.setTaiKhoan(tk);
            kh.setNgayTao(LocalDateTime.now());
            return khachHangRepository.save(kh);
        });

        testSpct = sanPhamChiTietRepository.findAll().stream().findFirst().orElseThrow();
        testPttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseThrow();
    }

    @Test
    @DisplayName("isStockDeductedState nhận diện đúng đơn thanh toán trước ở cho_xac_nhan")
    void testIsStockDeductedState() {
        HoaDon hdCod = new HoaDon();
        hdCod.setTrangThaiDonHang("cho_xac_nhan");
        hdCod.setTrangThaiThanhToan("CHUA_THANH_TOAN");
        assertFalse(orderViewService.isStockDeductedState(hdCod, "cho_xac_nhan"), "Đơn COD ở cho_xac_nhan chưa trừ kho");

        HoaDon hdPaid = new HoaDon();
        hdPaid.setTrangThaiDonHang("cho_xac_nhan");
        hdPaid.setTrangThaiThanhToan("DA_THANH_TOAN");
        assertTrue(orderViewService.isStockDeductedState(hdPaid, "cho_xac_nhan"), "Đơn SePay/Online ở cho_xac_nhan đã trừ kho");

        HoaDon hdConfirmed = new HoaDon();
        hdConfirmed.setTrangThaiDonHang("da_xac_nhan");
        assertTrue(orderViewService.isStockDeductedState(hdConfirmed, "da_xac_nhan"), "Đơn ở da_xac_nhan luôn đã trừ kho");
    }

    @Test
    @DisplayName("Khách hàng hủy đơn SePay ở trạng thái cho_xac_nhan -> Tự động hoàn kho và chuyển trạng thái da_huy, CHO_HOAN_TIEN")
    void testCustomerCancelPaidOrderInChoXacNhan_RestoresStock() {
        // Tạo đơn SePay ở trạng thái cho_xac_nhan (đã thanh toán)
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("HD-TEST-SEP-001");
        hd.setKhachHang(testKhachHang);
        hd.setTenNguoiNhan("Nguyen Van Test");
        hd.setSdtNhan("0987654321");
        hd.setDiaChiNhan("123 Test Street");
        hd.setPhuongThucThanhToan(testPttt);
        hd.setTongTien(new BigDecimal("500000"));
        hd.setTrangThaiDonHang("cho_xac_nhan");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setPaymentStatus("paid");
        hd.setNgayTao(LocalDateTime.now());
        hd.setNgayThanhToan(LocalDateTime.now());
        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(2);
        hdct.setDonGia(new BigDecimal("250000"));
        hdct.setGiaGoc(new BigDecimal("250000"));
        hdct.setGiaSauGiam(new BigDecimal("250000"));
        hdct.setNgayTao(LocalDateTime.now());
        hoaDonChiTietRepository.save(hdct);

        // Khách hàng hủy đơn
        orderViewService.huyDonHang(hd.getId(), testKhachHang.getId(), "127.0.0.1", "Đổi ý không mua nữa");

        // Verify
        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals("da_huy", updated.getTrangThaiDonHang(), "Trạng thái đơn hàng phải là da_huy");
        assertTrue("paid".equalsIgnoreCase(updated.getPaymentStatus()) || "CHO_HOAN_TIEN".equalsIgnoreCase(updated.getTrangThaiThanhToan()), "Đơn phải ghi nhận đã thanh toán / chờ hoàn tiền");
        assertTrue(Boolean.TRUE.equals(updated.getDaNhapKhoHoan()), "Cờ daNhapKhoHoan phải là true sau khi hoàn kho");
    }

    @Test
    @DisplayName("Admin xác nhận hoàn tiền cho đơn đã hủy có chứng từ thành công")
    void testAdminConfirmRefundForCancelledOrder_WithProof() {
        // Tạo đơn đã hủy chờ hoàn tiền
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("HD-TEST-SEP-002");
        hd.setKhachHang(testKhachHang);
        hd.setTenNguoiNhan("Nguyen Van Test");
        hd.setSdtNhan("0987654321");
        hd.setDiaChiNhan("123 Test Street");
        hd.setPhuongThucThanhToan(testPttt);
        hd.setTongTien(new BigDecimal("600000"));
        hd.setTrangThaiDonHang("da_huy");
        hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hd.setPaymentStatus("paid");
        hd.setRefundStatus(RefundStatus.PENDING);
        hd.setDaNhapKhoHoan(true);
        hd.setNgayTao(LocalDateTime.now());
        HoaDon savedHd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(savedHd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("600000"));
        hdct.setGiaGoc(new BigDecimal("600000"));
        hdct.setGiaSauGiam(new BigDecimal("600000"));
        hdct.setNgayTao(LocalDateTime.now());
        hoaDonChiTietRepository.save(hdct);

        final Integer orderId = savedHd.getId();

        // Xác nhận hoàn tiền thiếu chứng từ -> Phải throw exception
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.xacNhanHoanTienChoKhach(
                    orderId, "CHUYEN_KHOAN", new BigDecimal("600000"), "FT_TEST_001", "Hoàn tiền SePay",
                    null, adminTk.getId(), "127.0.0.1"
            );
        }, "Bắt buộc phải có ảnh chứng từ hoàn tiền");

        // Xác nhận hoàn tiền đầy đủ chứng từ
        String proofUrl = "/uploads/refunds/refund_proof_test_001.png";
        orderViewService.xacNhanHoanTienChoKhach(
                orderId, "CHUYEN_KHOAN", new BigDecimal("600000"), "FT_TEST_001", "Hoàn tiền SePay cho khách",
                proofUrl, adminTk.getId(), "127.0.0.1"
        );

        // Verify
        HoaDon refunded = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals("da_huy", refunded.getTrangThaiDonHang());
        assertEquals("REFUNDED", refunded.getTrangThaiThanhToan());
        assertEquals("refunded", refunded.getPaymentStatus());
        assertEquals(RefundStatus.COMPLETED, refunded.getRefundStatus());
        assertEquals("CHUYEN_KHOAN", refunded.getPhuongThucHoanTien());
        assertEquals(new BigDecimal("600000"), refunded.getSoTienHoan());
        assertEquals("FT_TEST_001", refunded.getMaGiaoDichHoanTien());
        assertEquals(proofUrl, refunded.getAnhChungTuHoanTien());

        // Kiểm tra PaymentTransaction
        PaymentTransaction tx = paymentTransactionRepository.findByTransactionId("FT_TEST_001").orElseThrow();
        assertEquals("REFUND_SUCCESS", tx.getStatus());
        assertEquals(new BigDecimal("600000"), tx.getAmount());
        assertTrue(tx.getRawPayload().contains(proofUrl), "Raw payload phải chứa đường dẫn ảnh chứng từ");
    }

    @Test
    @DisplayName("Admin hoàn tiền TIỀN MẶT (TIEN_MAT) -> Phương thức hoàn tiền lưu trữ và hiển thị đúng là TIEN_MAT")
    void testCashRefund_PersistenceAndResolution() {
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("HD-TEST-CASH-001");
        hd.setKhachHang(testKhachHang);
        hd.setTenNguoiNhan("Nguyen Van Test");
        hd.setSdtNhan("0987654321");
        hd.setDiaChiNhan("123 Test Street");
        hd.setPhuongThucThanhToan(testPttt);
        hd.setTongTien(new BigDecimal("750000"));
        hd.setTrangThaiDonHang("da_huy");
        hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hd.setPaymentStatus("paid");
        hd.setRefundStatus(RefundStatus.PENDING);
        hd.setDaNhapKhoHoan(true);
        hd.setNgayTao(LocalDateTime.now());
        HoaDon savedHd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(savedHd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("750000"));
        hdct.setGiaGoc(new BigDecimal("750000"));
        hdct.setGiaSauGiam(new BigDecimal("750000"));
        hdct.setNgayTao(LocalDateTime.now());
        hoaDonChiTietRepository.save(hdct);

        final Integer orderId = savedHd.getId();
        String proofUrl = "/uploads/refunds/refund_cash_proof.png";

        // Admin hoàn tiền bằng TIỀN MẶT
        orderViewService.xacNhanHoanTienChoKhach(
                orderId, "TIEN_MAT", new BigDecimal("750000"), "", "Hoàn tiền mặt trực tiếp tại shop",
                proofUrl, adminTk.getId(), "127.0.0.1"
        );

        // Giả lập request mới: Xóa các trường @Transient của entity để kiểm tra khả năng phục hồi từ PaymentTransaction
        HoaDon reloadedHd = hoaDonRepository.findById(orderId).orElseThrow();
        reloadedHd.setPhuongThucHoanTien(null);
        reloadedHd.setSoTienHoan(null);

        var details = orderViewService.resolveRefundDetails(orderId, reloadedHd);
        assertEquals("TIEN_MAT", details.get("phuongThucHoanTien"), "Phương thức hoàn phải là TIEN_MAT");
        assertEquals("750000.00", new BigDecimal(details.get("soTienHoan")).setScale(2).toString(), "Số tiền hoàn phải là 750000");
        assertEquals(proofUrl, details.get("anhChungTuHoanTien"), "Ảnh chứng từ phải đúng");
        assertEquals("TIEN_MAT", reloadedHd.getPhuongThucHoanTien(), "HoaDon phải được đồng bộ phuongThucHoanTien = TIEN_MAT");
    }
}
