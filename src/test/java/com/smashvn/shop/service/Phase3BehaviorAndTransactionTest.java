package com.smashvn.shop.service;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.api.GhnShipmentPersistenceService;
import com.smashvn.shop.service.order.OrderViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase3BehaviorAndTransactionTest {

    @Autowired
    private OrderViewService orderViewService;

    @Autowired
    private GhnShipmentPersistenceService ghnShipmentPersistenceService;

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
    private JdbcTemplate jdbcTemplate;

    private Integer adminTaiKhoanId;
    private KhachHang testKhachHang;
    private PhuongThucThanhToan testPttt;
    private SanPhamChiTiet testSpct;

    @BeforeEach
    void setUp() {
        TaiKhoan adminUser = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equalsIgnoreCase(t.getVaiTro()) || "NV".equalsIgnoreCase(t.getVaiTro()))
                .findFirst().orElse(null);
        assertNotNull(adminUser, "Phải có ít nhất 1 tài khoản Admin/NV trong DB để chạy test");
        adminTaiKhoanId = adminUser.getId();

        testKhachHang = khachHangRepository.findAll().stream().findFirst().orElseGet(() -> {
            KhachHang kh = new KhachHang();
            kh.setHoTenKh("Khách Test");
            kh.setSoDienThoaiKh("0901234567");
            return khachHangRepository.save(kh);
        });

        testPttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan pttt = new PhuongThucThanhToan();
            pttt.setTenPhuongThuc("Thanh toán khi nhận hàng");
            return phuongThucThanhToanDAO.save(pttt);
        });
    }

    @Test
    @DisplayName("Test 1: Admin reject với lý do rỗng phải bị từ chối với IllegalArgumentException")
    void testRejectWithoutReasonFails() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.PENDING_APPROVAL);
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.tuChoiYeuCauTraHang(hd.getId(), "   ", adminTaiKhoanId, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Test 2: Admin reject hợp lệ phải chuyển REJECTED và CHUA_XU_LY")
    void testRejectWithReasonSuccess() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.PENDING_APPROVAL);
        orderViewService.tuChoiYeuCauTraHang(hd.getId(), "Sản phẩm bị rách do khách tự gây ra", adminTaiKhoanId, "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.REJECTED, updated.getTrangThaiHoanHang());
        assertEquals(ReturnInventoryStatus.CHUA_XU_LY, updated.getTrangThaiXuLyHangHoan());
    }

    @Test
    @DisplayName("Test 3: Admin reject khi đơn không ở PENDING_APPROVAL phải bị chặn với IllegalStateException")
    void testRejectNonPendingFails() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.WAITING_FOR_PICKUP);
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.tuChoiYeuCauTraHang(hd.getId(), "Lý do từ chối", adminTaiKhoanId, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Test 4: Admin duyệt DOI khi thiếu tồn kho phải bị chặn và giữ PENDING_APPROVAL")
    void testApproveExchangeInsufficientStockFails() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.PENDING_APPROVAL);
        testSpct.setSoLuongTon(0);
        sanPhamChiTietRepository.save(testSpct);

        assertThrows(IllegalStateException.class, () -> {
            orderViewService.duyetYeuCauTraHangVaTaoDonGhn(hd.getId(), adminTaiKhoanId, "127.0.0.1");
        });

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.PENDING_APPROVAL, updated.getTrangThaiHoanHang(), "Đơn vẫn giữ PENDING_APPROVAL khi thiếu kho");
    }

    @Test
    @DisplayName("Test 5: Verification of REQUIRES_NEW persistence for TichHopVanChuyen record")
    void testRequiresNewShipmentPersistence() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.PENDING_APPROVAL);
        String testReturnCode = "GHN_TEST_REQ_NEW_" + System.currentTimeMillis();

        // 1. Executed in REQUIRES_NEW transaction
        ghnShipmentPersistenceService.saveReturnShipment(hd.getId(), testReturnCode, "waiting_to_return");

        // 2. Verify record remains committed in TichHopVanChuyen table
        List<String> codes = jdbcTemplate.queryForList(
            "SELECT ma_van_don FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_RETURN'",
            String.class,
            hd.getId()
        );
        assertFalse(codes.isEmpty(), "TichHopVanChuyen record must be committed");
        assertEquals(testReturnCode, codes.get(0));

        // Cleanup test row
        jdbcTemplate.update("DELETE FROM TichHopVanChuyen WHERE id_hoa_don = ?", hd.getId());
    }

    @Test
    @DisplayName("Test 6: Reconcile existing shipment when order is PENDING_APPROVAL")
    void testReconcileExistingShipment() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.PENDING_APPROVAL);
        String mockShipmentCode = "GHN_MOCK_RECONCILE_" + hd.getId();

        // Save existing return shipment to TichHopVanChuyen
        ghnShipmentPersistenceService.saveReturnShipment(hd.getId(), mockShipmentCode, "waiting_to_return");

        // Now approve order - should reconcile and reuse mockShipmentCode without calling GHN create order
        String code = orderViewService.duyetYeuCauTraHangVaTaoDonGhn(hd.getId(), adminTaiKhoanId, "127.0.0.1");
        assertEquals(mockShipmentCode, code);

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.WAITING_FOR_PICKUP, updated.getTrangThaiHoanHang());

        // Clean up
        jdbcTemplate.update("DELETE FROM TichHopVanChuyen WHERE id_hoa_don = ?", hd.getId());
    }

    private HoaDon createTestOrder(String loaiYeuCau, ReturnStatus returnStatus) {
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("HD_TEST_" + System.currentTimeMillis() + "_" + (int)(Math.random()*1000));
        hd.setDiaChiNhan("123 Đường Test, Quận 1, TP.HCM");
        hd.setTenNguoiNhan("Khách Hàng Test");
        hd.setSdtNhan("0901234567");
        hd.setNgayTao(java.time.LocalDateTime.now());
        hd.setTrangThaiDonHang("da_giao");
        hd.setLoaiYeuCauDoiTra(loaiYeuCau);
        hd.setLyDoHoanTra("Lý do test " + loaiYeuCau);
        hd.setTrangThaiHoanHang(returnStatus);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.CHUA_XU_LY);
        hd.setKhachHang(testKhachHang);
        hd.setPhuongThucThanhToan(testPttt);
        hd.setTongTien(new BigDecimal("500000"));
        hd = hoaDonRepository.save(hd);

        List<SanPhamChiTiet> spcts = sanPhamChiTietRepository.findAll();
        if (!spcts.isEmpty()) {
            testSpct = spcts.get(0);
        } else {
            testSpct = new SanPhamChiTiet();
            testSpct.setSoLuongTon(10);
            testSpct = sanPhamChiTietRepository.save(testSpct);
        }

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("500000"));
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }
}
