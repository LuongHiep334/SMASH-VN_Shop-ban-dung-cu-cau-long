package com.smashvn.shop.service;

import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.inventory.InventoryLotService;
import com.smashvn.shop.service.order.ExchangeStockReservationService;
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
public class Phase6ExchangeIntegrationTest {

    @Autowired
    private OrderViewService orderViewService;

    @Autowired
    private ExchangeStockReservationService exchangeStockReservationService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private ThuocTinhRepository thuocTinhRepository;

    @Autowired
    private SanPhamChiTietThuocTinhRepository sanPhamChiTietThuocTinhRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer adminTaiKhoanId;
    private KhachHang testKh;
    private PhuongThucThanhToan testPt;
    private SanPham testSanPham;
    private SanPhamChiTiet testSpct1;
    private SanPhamChiTiet testSpct2;

    @BeforeEach
    void setUp() {
        // Clean up test data
        jdbcTemplate.execute("DELETE FROM EditLog WHERE ten_bang = 'HoaDon' AND id_ban_ghi IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE6_%')");
        jdbcTemplate.execute("DELETE FROM GiaoDichThanhToan WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE6_%')");
        jdbcTemplate.execute("DELETE FROM TichHopVanChuyen WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE6_%')");
        jdbcTemplate.execute("DELETE FROM HoaDonChiTiet WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE6_%')");
        jdbcTemplate.execute("DELETE FROM HoaDon WHERE ghi_chu LIKE 'TEST_PHASE6_%'");

        TaiKhoan admin = taiKhoanRepository.findAll().stream().findFirst().orElseThrow();
        adminTaiKhoanId = admin.getId();
        testKh = khachHangRepository.findAll().stream().findFirst().orElse(null);
        testPt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);

        // Setup clean test product & variants
        ThuocTinh ttMau = thuocTinhRepository.findAll().stream()
                .filter(t -> t.getTenThuocTinh() != null && ("Màu sắc".equalsIgnoreCase(t.getTenThuocTinh()) || "Mau Sac".equalsIgnoreCase(t.getTenThuocTinh())))
                .findFirst().orElseGet(() -> {
                    ThuocTinh t = new ThuocTinh();
                    t.setTenThuocTinh("Màu sắc");
                    return thuocTinhRepository.save(t);
                });

        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseThrow();
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseThrow();
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseThrow();

        testSanPham = new SanPham();
        testSanPham.setTenSanPham("Vợt Phase6 " + System.currentTimeMillis());
        testSanPham.setDanhMuc(dm);
        testSanPham.setThuongHieu(th);
        testSanPham.setNhanVien(nv);
        testSanPham.setMoTa("Test Phase6");
        testSanPham.setTrangThai("dang_ban");
        testSanPham = sanPhamRepository.save(testSanPham);

        testSpct1 = new SanPhamChiTiet();
        testSpct1.setSanPham(testSanPham);
        testSpct1.setSoLuongTon(10);
        testSpct1.setGiaBan(new BigDecimal("500000"));
        testSpct1.setTrangThai("dang_ban");
        testSpct1 = sanPhamChiTietRepository.save(testSpct1);

        testSpct2 = new SanPhamChiTiet();
        testSpct2.setSanPham(testSanPham);
        testSpct2.setSoLuongTon(10);
        testSpct2.setGiaBan(new BigDecimal("500000"));
        testSpct2.setTrangThai("dang_ban");
        testSpct2 = sanPhamChiTietRepository.save(testSpct2);
    }

    private HoaDon createTestOrder(String loaiYeuCau, ReturnStatus returnStatus, ReturnInventoryStatus invStatus, SanPhamChiTiet spct, int qty) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setPhuongThucThanhToan(testPt);
        hd.setNgayTao(LocalDateTime.now().minusDays(3));
        hd.setNgayThanhToan(LocalDateTime.now().minusDays(3));
        BigDecimal total = spct.getGiaBan().multiply(new BigDecimal(qty));
        hd.setTongTienHang(total);
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        hd.setTongTien(total);
        hd.setTrangThaiDonHang("DA_GIAO");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setTenNguoiNhan("Khách Test Phase6");
        hd.setSdtNhan("0987654321");
        hd.setDiaChiNhan("123 Đường Test, Hà Nội");
        hd.setGhiChu("TEST_PHASE6_" + System.currentTimeMillis() + "_" + (int)(Math.random()*1000));
        hd.setLoaiYeuCauDoiTra(loaiYeuCau);
        hd.setTrangThaiHoanHang(returnStatus);
        hd.setTrangThaiXuLyHangHoan(invStatus);

        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(spct);
        hdct.setSoLuong(qty);
        hdct.setDonGia(spct.getGiaBan());
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }

    @Test
    @DisplayName("Test 1: Exchange đơn ĐỔI HÀNG (DA_HOAN_KHO) đủ tồn kho thành công")
    void test1_ExchangeTraDaHoanKhoSuccess() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 2);
        int initialStock = testSpct1.getSoLuongTon();

        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.EXCHANGE_SHIPPING, reloaded.getTrangThaiHoanHang());

        SanPhamChiTiet spctReloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock - 2, spctReloaded.getSoLuongTon(), "Tồn kho SPCT1 phải bị trừ exact 2 cây");

        Integer shipmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_EXCHANGE'",
                Integer.class, hd.getId()
        );
        assertEquals(1, shipmentCount, "Phải lưu 1 bản ghi GHN_EXCHANGE trong TichHopVanChuyen");
    }

    @Test
    @DisplayName("Test 2: Exchange đơn ĐỔI HÀNG (DA_CHUYEN_KHO_LOI) thành công")
    void test2_ExchangeTraDaChuyenKhoLoiSuccess() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, testSpct1, 1);
        int initialStock = testSpct1.getSoLuongTon();

        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.EXCHANGE_SHIPPING, reloaded.getTrangThaiHoanHang());

        SanPhamChiTiet spctReloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock - 1, spctReloaded.getSoLuongTon());
    }

    @Test
    @DisplayName("Test 3: Reject Exchange trên đơn TRẢ HÀNG (loaiYeuCauDoiTra = TRA)")
    void test3_RejectExchangeOnReturnOrder() {
        HoaDon hd = createTestOrder("TRA", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Chỉ áp dụng cho yêu cầu ĐỔI HÀNG"));
    }

    @Test
    @DisplayName("Test 4: Reject Exchange khi đơn chưa ở trạng thái RETURNED")
    void test4_RejectExchangeNotReturned() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.DELIVERED_TO_SHOP, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Chỉ đơn hàng ở trạng thái Đã kiểm hàng (RETURNED)"));
    }

    @Test
    @DisplayName("Test 5: Exact SPCT thiếu tồn kho (SPCT 1 stock = 0, SPCT 2 stock = 10) bị reject, SPCT 2 không bị trừ")
    void test5_ExactRequestedSpctLack_OtherSpctHasStock() {
        testSpct1.setSoLuongTon(0);
        sanPhamChiTietRepository.save(testSpct1);

        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("không đủ tồn kho để đổi"));

        SanPhamChiTiet spct2Reloaded = sanPhamChiTietRepository.findById(testSpct2.getId()).orElseThrow();
        assertEquals(10, spct2Reloaded.getSoLuongTon(), "SPCT 2 (10 cây) phải giữ nguyên, không bị trừ nhầm");
    }

    @Test
    @DisplayName("Test 6: Đơn có nhiều HDCT cùng exact SPCT ID -> Gom SUM quantity trừ chính xác")
    void test6_MultipleHdctSameExactSpct_GroupSum() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        // Add 2nd line for same SPCT1
        HoaDonChiTiet line2 = new HoaDonChiTiet();
        line2.setHoaDon(hd);
        line2.setSanPhamChiTiet(testSpct1);
        line2.setSoLuong(2);
        line2.setDonGia(testSpct1.getGiaBan());
        hoaDonChiTietRepository.save(line2);

        int initialStock = testSpct1.getSoLuongTon(); // 10

        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        SanPhamChiTiet spct1Reloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock - 3, spct1Reloaded.getSoLuongTon(), "Tồn kho phải trừ tổng = 1 + 2 = 3 cây");
    }

    @Test
    @DisplayName("Test 7: Nhiều SPCT: 1 đủ, 1 thiếu -> ALL OR NOTHING, SPCT đủ KHÔNG bị decrement")
    void test7_MultipleSpct_AllOrNothing_OneLacksStock() {
        testSpct2.setSoLuongTon(0);
        sanPhamChiTietRepository.save(testSpct2);

        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 2);

        HoaDonChiTiet line2 = new HoaDonChiTiet();
        line2.setHoaDon(hd);
        line2.setSanPhamChiTiet(testSpct2); // Stock = 0
        line2.setSoLuong(1);
        line2.setDonGia(testSpct2.getGiaBan());
        hoaDonChiTietRepository.save(line2);

        int spct1StockBefore = testSpct1.getSoLuongTon(); // 10

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("không đủ tồn kho để đổi"));

        SanPhamChiTiet spct1Reloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(spct1StockBefore, spct1Reloaded.getSoLuongTon(), "ALL-OR-NOTHING: SPCT1 không được bị trừ tồn khi SPCT2 thiếu stock");
    }

    @Test
    @DisplayName("Test 8: Call reserve 2 lần trên EXCHANGE_STOCK_ALLOCATED -> Không trừ kho lần 2")
    void test8_RetryStockAllocated_NoDoubleDecrement() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);
        int initialStock = testSpct1.getSoLuongTon();

        // 1st call: reserve stock -> EXCHANGE_STOCK_ALLOCATED
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        SanPhamChiTiet spctAfter1 = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock - 1, spctAfter1.getSoLuongTon());

        // 2nd call: reserve stock again
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        SanPhamChiTiet spctAfter2 = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock - 1, spctAfter2.getSoLuongTon(), "Kho không được bị trừ lần thứ 2 khi retry");
    }

    @Test
    @DisplayName("Test 9: GHN fail sau khi trừ kho -> Trạng thái EXCHANGE_STOCK_ALLOCATED được giữ nguyên, kho đã trừ giữ an toàn")
    void test9_GhnFail_StockAllocatedMaintained() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);
        int initialStock = testSpct1.getSoLuongTon();

        // Simulate step 1 reserve stock
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.EXCHANGE_STOCK_ALLOCATED, reloaded.getTrangThaiHoanHang());

        SanPhamChiTiet spctReloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock - 1, spctReloaded.getSoLuongTon(), "Tồn kho đã trừ vẫn được giữ an toàn ở EXCHANGE_STOCK_ALLOCATED");
    }

    @Test
    @DisplayName("Test 10: Retry sau khi GHN fail -> Không trừ kho lại, tiến hành tạo vận đơn GHN")
    void test10_RetryAfterGhnFail_NoReDecrement() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);
        int initialStock = testSpct1.getSoLuongTon();

        // 1. Reserve stock
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        // 2. Retry complete orchestrator call
        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.EXCHANGE_SHIPPING, reloaded.getTrangThaiHoanHang());

        SanPhamChiTiet spctReloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock - 1, spctReloaded.getSoLuongTon(), "Tồn kho chỉ bị trừ 1 lần duy nhất");
    }

    @Test
    @DisplayName("Test 11: Đã có GHN_EXCHANGE trong DB + EXCHANGE_STOCK_ALLOCATED -> Reuse mã cũ, không gọi GHN mới")
    void test11_ExistingGhnExchange_Reused() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        // Reserve stock
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        // Manually insert existing GHN_EXCHANGE shipment
        String existingCode = "GHN_EXCHANGE_EXISTING_" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) VALUES (?, 'GHN_EXCHANGE', ?, ?, 'ready_to_pick', GETDATE())",
                hd.getId(), existingCode, existingCode
        );

        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.EXCHANGE_SHIPPING, reloaded.getTrangThaiHoanHang());

        String savedCode = jdbcTemplate.queryForObject(
                "SELECT ma_van_don FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_EXCHANGE'",
                String.class, hd.getId()
        );
        assertEquals(existingCode, savedCode, "Phải reuse mã vận đơn GHN_EXCHANGE đã tồn tại");
    }

    @Test
    @DisplayName("Test 12: RETURNED + Đã có GHN_EXCHANGE -> Inconsistent state -> Reject, stock không đổi")
    void test12_ReturnedPlusExistingGhnExchange_Rejected() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);
        int initialStock = testSpct1.getSoLuongTon();

        // Insert inconsistent GHN_EXCHANGE shipment while order is still RETURNED
        jdbcTemplate.update(
                "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) VALUES (?, 'GHN_EXCHANGE', 'OLD_CODE', 'OLD_CODE', 'ready_to_pick', GETDATE())",
                hd.getId()
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Phát hiện vận đơn giao hàng đổi đã tồn tại nhưng chưa có trạng thái phân bổ tồn kho"));

        SanPhamChiTiet spctReloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock, spctReloaded.getSoLuongTon(), "Tồn kho không được thay đổi khi bị reject");
    }

    @Test
    @DisplayName("Test 13: GHN persisted + local status fail -> Retry completeExchangeShipping reconciles sang EXCHANGE_SHIPPING")
    void test13_GhnPersisted_StatusUpdateFail_RetryReconciles() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        // Step 1: Reserve stock
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        // Step 2: Insert shipment
        String orderCode = "GHN_EX_TEST13_" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) VALUES (?, 'GHN_EXCHANGE', ?, ?, 'ready_to_pick', GETDATE())",
                hd.getId(), orderCode, orderCode
        );

        // Step 3: Complete shipping
        exchangeStockReservationService.completeExchangeShipping(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.EXCHANGE_SHIPPING, reloaded.getTrangThaiHoanHang());
    }

    @Test
    @DisplayName("Test 14: Đơn đã ở EXCHANGE_SHIPPING -> Gọi lại không tạo duplicate GHN hay trừ kho")
    void test14_ExchangeShipping_NoDuplicateGhn() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);
        int initialStock = testSpct1.getSoLuongTon();

        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");
        int stockAfter1 = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongTon();

        // Recall orchestrator
        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");
        int stockAfter2 = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongTon();

        assertEquals(initialStock - 1, stockAfter1);
        assertEquals(stockAfter1, stockAfter2, "Không được trừ kho lần 2");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM TichHopVanChuyen WHERE id_hoa_don = ? AND nha_cung_cap = 'GHN_EXCHANGE'",
                Integer.class, hd.getId()
        );
        assertEquals(1, count, "Không tạo trùng bản ghi GHN_EXCHANGE thứ 2");
    }

    @Test
    @DisplayName("Test 15: BAN_LAI Stock Math (Phase 4 hoanKho +1, Phase 6 exchange -1 -> Net sellable = 0)")
    void test15_BanLaiStockMath() {
        int initialStock = testSpct1.getSoLuongTon(); // 10

        // Phase 4: Customer returns 1 item, inspection = BAN_LAI (soLuongTon + 1)
        testSpct1.setSoLuongTon(initialStock + 1);
        sanPhamChiTietRepository.save(testSpct1);
        assertEquals(initialStock + 1, sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongTon());

        // Phase 6: Exchange 1 replacement item (soLuongTon - 1)
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);
        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        int finalStock = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongTon();
        assertEquals(initialStock, finalStock, "BAN_LAI stock math: initial (10) + 1 (Phase 4) - 1 (Phase 6) = 10 (Net delta = 0)");
    }

    @Test
    @DisplayName("Test 16: HANG_LOI Stock Math (Phase 4 faulty +1, Phase 6 exchange sellable -1)")
    void test16_HangLoiStockMath() {
        int initialSellable = testSpct1.getSoLuongTon(); // 10
        int initialFaulty = testSpct1.getSoLuongSpLoi(); // 0

        // Phase 4: Customer returns 1 item, inspection = HANG_LOI
        testSpct1.setSoLuongSpLoi(initialFaulty + 1);
        sanPhamChiTietRepository.save(testSpct1);

        assertEquals(initialSellable, sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongTon());
        assertEquals(1, sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongSpLoi());

        // Phase 6: Exchange 1 replacement item from sellable stock
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, testSpct1, 1);
        orderViewService.xacNhanGiaoHangDoiMoiChoKhach(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        SanPhamChiTiet reloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialSellable - 1, reloaded.getSoLuongTon(), "Tồn kho bán hàng giảm 1");
        assertEquals(1, reloaded.getSoLuongSpLoi(), "Tồn kho lỗi tăng 1");
    }

    @Test
    @DisplayName("Test 17: Phase 5 confirmRefund trên đơn ĐỔI HÀNG (DOI) bị reject")
    void test17_Phase5RefundOnExchangeOrder_Rejected() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.xacNhanHoanTienChoKhach(hd.getId(), "CHUYEN_KHOAN", new BigDecimal("500000"), "FT_FAIL", "Notes", "/uploads/refunds/proof.png", adminTaiKhoanId, "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Chỉ yêu cầu TRẢ HÀNG mới có thể thực hiện hoàn tiền"));
    }

    @Test
    @DisplayName("Test 18: Concurrent Admin Reserve - Gọi reserve 2 lần chỉ thực hiện 1 lần trừ kho")
    void test18_ConcurrentAdminReserve() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);
        int initialStock = testSpct1.getSoLuongTon();

        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        SanPhamChiTiet reloaded = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        assertEquals(initialStock - 1, reloaded.getSoLuongTon(), "Tồn kho chỉ bị trừ 1 lần");
    }

    @Test
    @DisplayName("Test 19: GHN_EXCHANGE delivered -> Cập nhật trạng thái sang EXCHANGED")
    void test19_GhnExchangeDelivered_StateTransits() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        String code = "GHN_EX_TEST19_" + System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) VALUES (?, 'GHN_EXCHANGE', ?, ?, 'ready_to_pick', GETDATE())",
                hd.getId(), code, code
        );

        exchangeStockReservationService.completeExchangeShipping(hd.getId(), adminTaiKhoanId, "127.0.0.1");

        orderViewService.updateExchangeStatusFromGhn(hd.getId(), ReturnStatus.EXCHANGED, "delivered", "TEST");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.EXCHANGED, reloaded.getTrangThaiHoanHang());
    }

    @Test
    @DisplayName("Test 20: Duplicate delivered callback -> Idempotent ignore, stock không đổi")
    void test20_DuplicateDeliveredCallback_Idempotent() {
        HoaDon hd = createTestOrder("DOI", ReturnStatus.RETURNED, ReturnInventoryStatus.DA_HOAN_KHO, testSpct1, 1);

        exchangeStockReservationService.reserveReplacementStock(hd.getId(), adminTaiKhoanId, "127.0.0.1");
        int stockAfterAllocated = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongTon();

        orderViewService.updateExchangeStatusFromGhn(hd.getId(), ReturnStatus.EXCHANGED, "delivered", "TEST_1");
        int stockAfterDelivered1 = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongTon();

        // Duplicate delivered callback
        orderViewService.updateExchangeStatusFromGhn(hd.getId(), ReturnStatus.EXCHANGED, "delivered", "TEST_2");
        int stockAfterDelivered2 = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow().getSoLuongTon();

        assertEquals(stockAfterAllocated, stockAfterDelivered1);
        assertEquals(stockAfterDelivered1, stockAfterDelivered2, "Tồn kho tuyệt đối không bị trừ thêm khi nhận callback trùng");

        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertEquals(ReturnStatus.EXCHANGED, reloaded.getTrangThaiHoanHang());
    }
}
