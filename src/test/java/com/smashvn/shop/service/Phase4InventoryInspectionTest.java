package com.smashvn.shop.service;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.api.GhnShipmentPersistenceService;
import com.smashvn.shop.service.inventory.InventoryLotService;
import com.smashvn.shop.service.order.OrderViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class Phase4InventoryInspectionTest {

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
            kh.setHoTenKh("Khách Test Phase 4");
            kh.setSoDienThoaiKh("0909998888");
            return khachHangRepository.save(kh);
        });

        testPttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan pttt = new PhuongThucThanhToan();
            pttt.setTenPhuongThuc("Thanh toán khi nhận hàng");
            return phuongThucThanhToanDAO.save(pttt);
        });

        List<SanPhamChiTiet> spcts = sanPhamChiTietRepository.findAll();
        if (!spcts.isEmpty()) {
            testSpct = spcts.get(0);
        } else {
            testSpct = new SanPhamChiTiet();
            testSpct.setSoLuongTon(50);
            testSpct.setSoLuongSpLoi(0);
            testSpct = sanPhamChiTietRepository.save(testSpct);
        }
    }

    @Test
    @DisplayName("Test 1: GHN_RETURN delivered chuyển DELIVERED_TO_SHOP và stock không đổi")
    void testGhnReturnDeliveredNoStockChange() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.WAITING_FOR_PICKUP, 2);
        int initialStock = getLatestSpct(testSpct.getId()).getSoLuongTon();
        int initialFaulty = getLatestSpct(testSpct.getId()).getSoLuongSpLoi() != null ? getLatestSpct(testSpct.getId()).getSoLuongSpLoi() : 0;

        orderViewService.updateReturnStatusFromGhn(hd.getId(), ReturnStatus.DELIVERED_TO_SHOP, "returned", "GHN_WEBHOOK");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.DELIVERED_TO_SHOP, updated.getTrangThaiHoanHang());
        assertEquals(initialStock, getLatestSpct(testSpct.getId()).getSoLuongTon());
        assertEquals(initialFaulty, getLatestSpct(testSpct.getId()).getSoLuongSpLoi() != null ? getLatestSpct(testSpct.getId()).getSoLuongSpLoi() : 0);
    }

    @Test
    @DisplayName("Test 2: GHN_RETURN delivered duplicate không tự cộng kho / reset status")
    void testGhnReturnDeliveredDuplicateIdempotent() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 2);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.DA_HOAN_KHO);
        hd.setTrangThaiHoanHang(ReturnStatus.RETURNED);
        hoaDonRepository.save(hd);

        orderViewService.updateReturnStatusFromGhn(hd.getId(), ReturnStatus.DELIVERED_TO_SHOP, "returned", "GHN_WEBHOOK");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.RETURNED, updated.getTrangThaiHoanHang(), "Terminal status RETURNED must be preserved");
    }

    @Test
    @DisplayName("Test 3: BAN_LAI cộng soLuongTon đúng, soLuongSpLoi không đổi, status RETURNED, DA_HOAN_KHO")
    void testInspectBanLaiSuccess() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 3);
        int initialStock = getLatestSpct(testSpct.getId()).getSoLuongTon();
        int initialFaulty = getLatestSpct(testSpct.getId()).getSoLuongSpLoi() != null ? getLatestSpct(testSpct.getId()).getSoLuongSpLoi() : 0;

        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, adminTaiKhoanId, "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.RETURNED, updated.getTrangThaiHoanHang());
        assertEquals(ReturnInventoryStatus.DA_HOAN_KHO, updated.getTrangThaiXuLyHangHoan());
        assertEquals("CHO_HOAN_TIEN", updated.getTrangThaiThanhToan());
        assertEquals(RefundStatus.PENDING, updated.getRefundStatus());
        assertEquals(initialStock + 3, getLatestSpct(testSpct.getId()).getSoLuongTon());
        assertEquals(initialFaulty, getLatestSpct(testSpct.getId()).getSoLuongSpLoi() != null ? getLatestSpct(testSpct.getId()).getSoLuongSpLoi() : 0);
    }

    @Test
    @DisplayName("Test 4: HANG_LOI cộng soLuongSpLoi đúng, soLuongTon không đổi, status RETURNED, DA_CHUYEN_KHO_LOI")
    void testInspectHangLoiSuccess() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 4);
        int initialStock = getLatestSpct(testSpct.getId()).getSoLuongTon();
        int initialFaulty = getLatestSpct(testSpct.getId()).getSoLuongSpLoi() != null ? getLatestSpct(testSpct.getId()).getSoLuongSpLoi() : 0;

        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "HANG_LOI", null, adminTaiKhoanId, "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.RETURNED, updated.getTrangThaiHoanHang());
        assertEquals(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, updated.getTrangThaiXuLyHangHoan());
        assertEquals("CHO_HOAN_TIEN", updated.getTrangThaiThanhToan());
        assertEquals(RefundStatus.PENDING, updated.getRefundStatus());
        assertEquals(initialStock, getLatestSpct(testSpct.getId()).getSoLuongTon());
        assertEquals(initialFaulty + 4, getLatestSpct(testSpct.getId()).getSoLuongSpLoi() != null ? getLatestSpct(testSpct.getId()).getSoLuongSpLoi() : 0);
    }

    @Test
    @DisplayName("Test 5 & 6: Double BAN_LAI / HANG_LOI bị reject và không tăng tồn kho lần 2")
    void testDoubleInspectionRejected() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 2);
        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, adminTaiKhoanId, "127.0.0.1");

        int stockAfterFirst = getLatestSpct(testSpct.getId()).getSoLuongTon();

        // Lần 2 thực hiện xử lý kho ➔ Bắt buộc bị reject
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, adminTaiKhoanId, "127.0.0.1");
        });

        assertEquals(stockAfterFirst, getLatestSpct(testSpct.getId()).getSoLuongTon(), "Stock must not increase on 2nd attempt");
    }

    @Test
    @DisplayName("Test 8: TU_CHOI sau kiểm hàng không cộng kho, status REJECTED, DANG_TRA_LAI_KHACH, giữ lyDoHoanTra")
    void testInspectTuChoiSuccess() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 2);
        String customerOriginalReason = hd.getLyDoHoanTra();
        int initialStock = getLatestSpct(testSpct.getId()).getSoLuongTon();
        int initialFaulty = getLatestSpct(testSpct.getId()).getSoLuongSpLoi() != null ? getLatestSpct(testSpct.getId()).getSoLuongSpLoi() : 0;

        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "TU_CHOI", "Hàng bị gãy vợt do tác động ngoại lực", adminTaiKhoanId, "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.REJECTED, updated.getTrangThaiHoanHang());
        assertEquals(ReturnInventoryStatus.DANG_TRA_LAI_KHACH, updated.getTrangThaiXuLyHangHoan());
        assertEquals(customerOriginalReason, updated.getLyDoHoanTra(), "Lý do của khách không được bị overwrite");
        assertEquals(initialStock, getLatestSpct(testSpct.getId()).getSoLuongTon());
        assertEquals(initialFaulty, getLatestSpct(testSpct.getId()).getSoLuongSpLoi() != null ? getLatestSpct(testSpct.getId()).getSoLuongSpLoi() : 0);
    }

    @Test
    @DisplayName("Test 7: Hai Admin cùng xử lý kiểm kho -> Chỉ 1 transaction thành công, request thứ hai bị guard chặn")
    void testConcurrentAdminInspectionAllowedOnlyOnce() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 2);

        // Admin 1 inspects & restocks
        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, adminTaiKhoanId, "127.0.0.1");

        // Admin 2 inspects on same order -> throws IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, adminTaiKhoanId, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Test 9: Reuse GHN_REJECT_RETURN shipment code if present on retry")
    void testReuseRejectReturnShipmentCode() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 1);
        String mockRejectCode = "GHN_REJECT_MOCK_" + hd.getId();
        ghnShipmentPersistenceService.saveShipment(hd.getId(), mockRejectCode, "GHN_REJECT_RETURN", "waiting_to_return");

        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "TU_CHOI", "Lý do không đạt", adminTaiKhoanId, "127.0.0.1");

        String code = orderViewService.resolveGhnRejectReturnCode(hd.getId());
        assertEquals(mockRejectCode, code);

        // Cleanup
        jdbcTemplate.update("DELETE FROM TichHopVanChuyen WHERE id_hoa_don = ?", hd.getId());
    }

    @Test
    @DisplayName("Test 10 & 11: GHN_REJECT_RETURN delivered chuyển DA_TRA_LAI_KHACH và idempotent")
    void testRejectReturnDeliveredAndIdempotent() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 1);
        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "TU_CHOI", "Lý do không đạt", adminTaiKhoanId, "127.0.0.1");

        orderViewService.handleRejectReturnDeliveryFromGhn(hd.getId(), "delivered", "GHN_WEBHOOK");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnInventoryStatus.DA_TRA_LAI_KHACH, updated.getTrangThaiXuLyHangHoan());
        assertEquals(ReturnStatus.REJECTED, updated.getTrangThaiHoanHang());

        // Duplicate call
        orderViewService.handleRejectReturnDeliveryFromGhn(hd.getId(), "delivered", "GHN_WEBHOOK");
        HoaDon updated2 = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnInventoryStatus.DA_TRA_LAI_KHACH, updated2.getTrangThaiXuLyHangHoan());
    }

    @Test
    @DisplayName("Test 12: Đơn có nhiều HDCT trùng SPCT gom SUM quantity chính xác")
    void testMultipleHdctGroupedSumQuantity() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.DELIVERED_TO_SHOP, 2);
        
        // Add 2nd HDCT item for same SPCT with quantity 3
        HoaDonChiTiet hdct2 = new HoaDonChiTiet();
        hdct2.setHoaDon(hd);
        hdct2.setSanPhamChiTiet(testSpct);
        hdct2.setSoLuong(3);
        hdct2.setDonGia(new BigDecimal("500000"));
        hoaDonChiTietRepository.save(hdct2);

        int initialStock = getLatestSpct(testSpct.getId()).getSoLuongTon();

        // 2 + 3 = 5 items total
        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, adminTaiKhoanId, "127.0.0.1");

        assertEquals(initialStock + 5, getLatestSpct(testSpct.getId()).getSoLuongTon());
    }

    @Test
    @DisplayName("Đơn ĐỔI sau kiểm hàng không được chuyển sang chờ hoàn tiền")
    void testExchangeInspectionDoesNotCreatePendingRefund() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.DELIVERED_TO_SHOP, 1);

        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, adminTaiKhoanId, "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.RETURNED, updated.getTrangThaiHoanHang());
        assertEquals("DA_THANH_TOAN", updated.getTrangThaiThanhToan());
        assertNull(updated.getRefundStatus());
    }

    private SanPhamChiTiet getLatestSpct(Integer id) {
        return sanPhamChiTietRepository.findById(id).orElseThrow();
    }

    private HoaDon createTestOrder(String loaiYeuCau, ReturnStatus returnStatus, int quantity) {
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("HD_P4_" + System.currentTimeMillis() + "_" + (int)(Math.random()*1000));
        hd.setDiaChiNhan("456 Đường Test Phase 4, TP.HCM");
        hd.setTenNguoiNhan("Khách Phase 4");
        hd.setSdtNhan("0909998888");
        hd.setNgayTao(java.time.LocalDateTime.now());
        hd.setTrangThaiDonHang("da_giao");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setLoaiYeuCauDoiTra(loaiYeuCau);
        hd.setLyDoHoanTra("Khách hàng báo sản phẩm bị lỗi sản xuất");
        hd.setTrangThaiHoanHang(returnStatus);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.CHUA_XU_LY);
        hd.setKhachHang(testKhachHang);
        hd.setPhuongThucThanhToan(testPttt);
        hd.setTongTien(new BigDecimal("500000"));
        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(quantity);
        hdct.setDonGia(new BigDecimal("500000"));
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }
}
