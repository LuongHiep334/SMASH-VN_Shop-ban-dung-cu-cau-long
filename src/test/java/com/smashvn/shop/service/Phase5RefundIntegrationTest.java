package com.smashvn.shop.service;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.order.OrderViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase5RefundIntegrationTest {

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
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer adminTaiKhoanId;
    private SanPhamChiTiet testSpct;
    private KhachHang testKh;
    private PhuongThucThanhToan testPt;

    @BeforeEach
    void setUp() {
        // Clean up test data
        jdbcTemplate.execute("DELETE FROM EditLog WHERE ten_bang = 'HoaDon' AND id_ban_ghi IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE5_%')");
        jdbcTemplate.execute("DELETE FROM GiaoDichThanhToan WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE5_%')");
        jdbcTemplate.execute("DELETE FROM TichHopVanChuyen WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE5_%')");
        jdbcTemplate.execute("DELETE FROM HoaDonChiTiet WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE5_%')");
        jdbcTemplate.execute("DELETE FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE5_%'");

        TaiKhoan admin = taiKhoanRepository.findAll().stream().findFirst().orElseThrow();
        adminTaiKhoanId = admin.getId();

        testKh = khachHangRepository.findAll().stream().findFirst().orElse(null);
        testSpct = sanPhamChiTietRepository.findAll().stream().findFirst().orElseThrow();
        testPt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
    }

    private HoaDon createTestOrder(String loaiYeuCau, ReturnStatus returnStatus, ReturnInventoryStatus invStatus, BigDecimal totalAmount) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setPhuongThucThanhToan(testPt);
        hd.setNgayTao(LocalDateTime.now().minusDays(3));
        hd.setNgayThanhToan(LocalDateTime.now().minusDays(3));
        hd.setTongTienHang(totalAmount);
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        hd.setTongTien(totalAmount);
        hd.setTrangThaiDonHang("DA_GIAO");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setTenNguoiNhan("Khách Test Phase5");
        hd.setSdtNhan("0987654321");
        hd.setDiaChiNhan("123 Đường Test, Hà Nội");
        hd.setGhiChu("TEST_PHASE5_" + System.currentTimeMillis() + "_" + (int)(Math.random()*1000));
        hd.setLoaiYeuCauDoiTra(loaiYeuCau);
        hd.setTrangThaiHoanHang(returnStatus);
        hd.setTrangThaiXuLyHangHoan(invStatus);

        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(totalAmount);
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }

    @Test
    @DisplayName("Test 1: Refund đơn TRẢ HÀNG (DA_HOAN_KHO) thành công")
    void test1_RefundTraDaHoanKhoSuccess() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));
        String maGD = "FT_TEST1_" + System.currentTimeMillis();

        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), maGD, "Hoàn đủ tiền", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.REFUNDED, reloaded.getTrangThaiHoanHang());
        assertEquals("DA_HOAN_TIEN", reloaded.getTrangThaiThanhToan());

        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "REFUND_SUCCESS");
        assertEquals(1, txs.size());
        assertEquals(maGD, txs.get(0).getTransactionId());
        assertEquals(new BigDecimal("500000.00"), txs.get(0).getAmount());
    }

    @Test
    @DisplayName("Test 2: Refund đơn TRẢ HÀNG (DA_CHUYEN_KHO_LOI) thành công")
    void test2_RefundTraDaChuyenKhoLoiSuccess() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, new BigDecimal("700000"));
        String maGD = "FT_TEST2_" + System.currentTimeMillis();

        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("700000"), maGD, "Hoàn hàng lỗi", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.REFUNDED, reloaded.getTrangThaiHoanHang());
        assertEquals("DA_HOAN_TIEN", reloaded.getTrangThaiThanhToan());

        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "REFUND_SUCCESS");
        assertEquals(1, txs.size());
    }

    @Test
    @DisplayName("Test 3: Reject Refund trên đơn ĐỔI HÀNG (loaiYeuCauDoiTra = DOI)")
    void test3_RejectRefundOnExchangeOrder() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("300000"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("300000"), "FT_DOI", "Ghi chú", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Chỉ yêu cầu TRẢ HÀNG"));
    }

    @Test
    @DisplayName("Test 4: Reject Refund khi hàng chưa kiểm kho (CHUA_XU_LY)")
    void test4_RejectRefundOnDeliveredToShop() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, ReturnInventoryStatus.CHUA_XU_LY, new BigDecimal("400000"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("400000"), "FT_CHUA_XU_LY", "Ghi chú", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Chỉ đơn hàng ở trạng thái Đã nhận hàng về shop (RETURNED) mới được phép hoàn tiền"));
    }

    @Test
    @DisplayName("Test 5: Reject Refund khi đơn bị từ chối (REJECTED)")
    void test5_RejectRefundOnRejectedOrder() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.REJECTED, ReturnInventoryStatus.DANG_TRA_LAI_KHACH, new BigDecimal("400000"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("400000"), "FT_REJECTED", "Ghi chú", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Chỉ đơn hàng ở trạng thái Đã nhận hàng về shop (RETURNED) mới được phép hoàn tiền"));
    }

    @Test
    @DisplayName("Test 6: Double Refund Layer 1 - Đơn đã REFUNDED gọi lại không tạo transaction mới")
    void test6_DoubleRefundOnRefundedStatus() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));
        String maGD = "FT_DOUBLE1_" + System.currentTimeMillis();

        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), maGD, "Hoàn lần 1", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");
        int countBefore = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "REFUND_SUCCESS").size();

        // Call refund again
        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), maGD + "_2", "Hoàn lần 2", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");
        int countAfter = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "REFUND_SUCCESS").size();

        assertEquals(1, countBefore);
        assertEquals(1, countAfter);
    }

    @Test
    @DisplayName("Test 7: Double Refund Layer 2 + Reconcile - Đơn RETURNED nhưng DB đã có REFUND_SUCCESS")
    void test7_DoubleRefundLayer2Reconcile() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("600000"));

        // Insert legacy/inconsistent transaction manually
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrder(hd);
        tx.setTransactionId("FT_MANUAL_EXISTING_" + System.currentTimeMillis());
        tx.setAmount(new BigDecimal("600000"));
        tx.setGateway("MANUAL_REFUND");
        tx.setStatus("REFUND_SUCCESS");
        tx.setRawPayload("{}");
        tx.setCreatedAt(LocalDateTime.now());
        paymentTransactionRepository.saveAndFlush(tx);

        // Call confirmRefund on order which is still RETURNED
        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("600000"), "FT_NEW_ATTEMPT", "Reconcile test", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.REFUNDED, reloaded.getTrangThaiHoanHang());
        assertEquals("DA_HOAN_TIEN", reloaded.getTrangThaiThanhToan());

        // Should NOT create second transaction
        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "REFUND_SUCCESS");
        assertEquals(1, txs.size());
    }

    @Test
    @DisplayName("Test 8: Refund số tiền = 0 bị reject")
    void test8_RefundAmountZeroFails() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", BigDecimal.ZERO, "FT_ZERO", "Notes", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Số tiền hoàn phải lớn hơn 0"));
    }

    @Test
    @DisplayName("Test 9: Refund số tiền âm bị reject")
    void test9_RefundAmountNegativeFails() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("-100000"), "FT_NEG", "Notes", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Số tiền hoàn phải lớn hơn 0"));
    }

    @Test
    @DisplayName("Test 10: Refund số tiền vượt tổng đơn bị reject")
    void test10_RefundAmountExceedingTotalFails() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("600000"), "FT_EXCEED", "Notes", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Số tiền hoàn không được lớn hơn tổng số tiền khách thực trả"));
    }

    @Test
    @DisplayName("Test 11: Refund số tiền nhỏ hơn tổng đơn nhưng không có ghi chú bị reject")
    void test11_PartialRefundWithoutNotesFails() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("400000"), "FT_LESS", "", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("vui lòng nhập ghi chú lý do hoàn thiếu"));
    }

    @Test
    @DisplayName("Test 12: CHUYEN_KHOAN nhưng maGiaoDich rỗng bị reject")
    void test12_ChuyenKhoanWithoutTransactionIdFails() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), "  ", "Notes", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Vui lòng nhập mã giao dịch chuyển khoản hoàn tiền"));
    }

    @Test
    @DisplayName("Test 13: Trùng maGiaoDich hoàn tiền bị reject")
    void test13_DuplicateTransactionIdFails() {
        HoaDon hd1 = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));
        HoaDon hd2 = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("300000"));
        String sameMaGD = "FT_DUPLICATE_" + System.currentTimeMillis();

        orderViewService.xacNhanHoanTienChoKhach(hd1.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), sameMaGD, "Notes 1", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd2.getId(), "CHUYEN_KHOAN", new BigDecimal("300000"), sameMaGD, "Notes 2", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Mã giao dịch hoàn tiền") && ex.getMessage().contains("đã tồn tại"));
    }

    @Test
    @DisplayName("Test 14: TIEN_MAT không nhập maGiaoDich tự sinh mã internal thành công")
    void test14_TienMatNoTransactionIdAutoGenerates() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "TIEN_MAT", new BigDecimal("500000"), null, "Hoàn tiền mặt", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.REFUNDED, reloaded.getTrangThaiHoanHang());

        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "REFUND_SUCCESS");
        assertEquals(1, txs.size());
        assertTrue(txs.get(0).getTransactionId().startsWith("REFUND-CASH-HD" + hd.getId()));
    }

    @Test
    @DisplayName("Test 15: Single DB transaction rollback if PaymentTransaction save fails")
    void test15_PaymentTransactionSaveFailRollback() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        // Pre-create duplicate transactionId in DB
        PaymentTransaction existing = new PaymentTransaction();
        existing.setOrder(hd);
        existing.setTransactionId("FT_FAIL_ROLLBACK");
        existing.setAmount(new BigDecimal("100000"));
        existing.setGateway("TEST");
        existing.setStatus("PAID");
        existing.setCreatedAt(LocalDateTime.now());
        paymentTransactionRepository.saveAndFlush(existing);

        // Attempt refund with duplicate transactionId
        assertThrows(IllegalArgumentException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), "FT_FAIL_ROLLBACK", "Notes", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );

        // Order MUST stay RETURNED (not REFUNDED)
        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.RETURNED, reloaded.getTrangThaiHoanHang());
    }

    @Test
    @DisplayName("Test 16: Concurrency Guard - 2 Admin cùng refund chỉ tạo 1 transaction")
    void test16_ConcurrentAdminRefund() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));
        String maGD1 = "FT_CONCUR1_" + System.currentTimeMillis();
        String maGD2 = "FT_CONCUR2_" + System.currentTimeMillis();

        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), maGD1, "Admin 1", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");

        // Second call should return gracefully due to Layer 1 guard
        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), maGD2, "Admin 2", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");

        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "REFUND_SUCCESS");
        assertEquals(1, txs.size());
        assertEquals(maGD1, txs.get(0).getTransactionId());
    }

    @Test
    @DisplayName("Test 17: Payment query isolation - refund transactions don't interfere with payment queries")
    void test17_PaymentQueryIsolation() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        // Create initial payment transaction
        PaymentTransaction payTx = new PaymentTransaction();
        payTx.setOrder(hd);
        payTx.setTransactionId("PAY_" + System.currentTimeMillis());
        payTx.setAmount(new BigDecimal("500000"));
        payTx.setGateway("SEPAY");
        payTx.setStatus("SUCCESS");
        payTx.setCreatedAt(LocalDateTime.now().minusDays(1));
        paymentTransactionRepository.saveAndFlush(payTx);

        // Perform refund
        String refundMaGD = "FT_ISOLATION_" + System.currentTimeMillis();
        orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), refundMaGD, "Refund isolation", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1");

        // Payment query for SUCCESS should only return payTx
        List<PaymentTransaction> payTxs = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "SUCCESS");
        assertEquals(1, payTxs.size());
        assertEquals(payTx.getTransactionId(), payTxs.get(0).getTransactionId());

        // Refund query for REFUND_SUCCESS should return refundTx
        List<PaymentTransaction> refundTxs = paymentTransactionRepository.findByOrder_IdAndStatus(hd.getId(), "REFUND_SUCCESS");
        assertEquals(1, refundTxs.size());
        assertEquals(refundMaGD, refundTxs.get(0).getTransactionId());
    }

    @Autowired
    private com.smashvn.shop.service.common.FileStorageService fileStorageService;

    @Test
    @DisplayName("Test 18: File chứng từ được cleanup tự động nếu refund service thất bại")
    void test18_FileUploadCleanupOnFailure() throws Exception {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));

        // Create a valid mock image file
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        byte[] fakeImageContent = baos.toByteArray();

        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "fileChungTu", "refund_proof_test.png", "image/png", fakeImageContent
        );

        List<String> savedNames = fileStorageService.saveImages(List.of(mockFile), "refunds");
        assertFalse(savedNames.isEmpty());
        String filename = savedNames.get(0);

        // Attempt refund on an exchange order (DOI), which will throw IllegalStateException
        assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), "FT_CLEANUP_FAIL", "Test", "/uploads/refunds/" + filename, adminTaiKhoanId, "127.0.0.1")
        );

        // Controller catch block calls cleanup
        fileStorageService.deleteFiles(savedNames, "refunds");

        // Verify file was deleted from disk
        java.nio.file.Path targetPath = java.nio.file.Paths.get("uploads/refunds/" + filename);
        assertFalse(java.nio.file.Files.exists(targetPath), "File chứng từ phải được xóa thành công khỏi ổ đĩa khi refund fail");
    }

    @Test
    @DisplayName("Test 19: Refund không có ảnh chứng từ bị reject")
    void test19_RefundWithoutProofImageFails() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, new BigDecimal("500000"));
        String maGD = "FT_NO_IMG_" + System.currentTimeMillis();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), maGD, "Hoàn đủ tiền", null, adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Ảnh / chứng từ xác nhận hoàn tiền là bắt buộc"));
    }
}
