package com.smashvn.shop.service.inventory;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.admin.AdminPosService;
import com.smashvn.shop.service.admin.AdminPosService.PosItem;
import com.smashvn.shop.service.order.ExchangeStockReservationService;
import com.smashvn.shop.service.order.OrderViewService;

@SpringBootTest
@Transactional
public class InventoryFlowFullVerificationTest {

    @Autowired
    private AdminPosService adminPosService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private OrderViewService orderViewService;

    @Autowired
    private ExchangeStockReservationService exchangeStockReservationService;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    private SanPham testSanPham;
    private SanPhamChiTiet testSpct1;
    private SanPhamChiTiet testSpct2;
    private NhanVien testNhanVien;
    private TaiKhoan testAdminAccount;

    @BeforeEach
    void setUp() {
        // Admin account & Employee first (so SanPham has non-null id_nhan_vien)
        testAdminAccount = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equalsIgnoreCase(t.getVaiTro()))
                .findFirst().orElseGet(() -> {
                    TaiKhoan tk = new TaiKhoan();
                    tk.setUsername("admin_inv_test_" + System.currentTimeMillis());
                    tk.setVaiTro("QL");
                    tk.setTrangThai("hoat_dong");
                    return taiKhoanRepository.save(tk);
                });

        testNhanVien = nhanVienRepository.findByTaiKhoanId(testAdminAccount.getId());
        if (testNhanVien == null) {
            testNhanVien = new NhanVien();
            testNhanVien.setHoTen("Nhân Viên Inv Test");
            testNhanVien.setTaiKhoan(testAdminAccount);
            testNhanVien = nhanVienRepository.save(testNhanVien);
        }

        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("Vợt cầu lông Test");
            newDm.setTrangThai(true);
            return danhMucRepository.save(newDm);
        });

        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu newTh = new ThuongHieu();
            newTh.setTenThuongHieu("Yonex Test");
            newTh.setTrangThai(true);
            return thuongHieuRepository.save(newTh);
        });

        testSanPham = new SanPham();
        testSanPham.setTenSanPham("Vợt Yonex Inventory Test");
        testSanPham.setDanhMuc(dm);
        testSanPham.setThuongHieu(th);
        testSanPham.setNhanVien(testNhanVien);
        testSanPham.setTrangThai("dang_ban");
        testSanPham = sanPhamRepository.save(testSanPham);

        // Variant 1: Initial stock = 10
        testSpct1 = new SanPhamChiTiet();
        testSpct1.setSanPham(testSanPham);
        testSpct1.setGiaBan(new BigDecimal("1000000"));
        testSpct1.setGiaNhap(new BigDecimal("700000"));
        testSpct1.setSoLuongTon(10);
        testSpct1.setSoLuongSpLoi(0);
        testSpct1.setTrangThai("hoat_dong");
        testSpct1 = sanPhamChiTietRepository.save(testSpct1);

        inventoryLotService.nhapLoMoi(testSpct1.getId(), 10, new BigDecimal("700000"), null);

        // Variant 2: Initial stock = 5
        testSpct2 = new SanPhamChiTiet();
        testSpct2.setSanPham(testSanPham);
        testSpct2.setGiaBan(new BigDecimal("1500000"));
        testSpct2.setGiaNhap(new BigDecimal("1000000"));
        testSpct2.setSoLuongTon(5);
        testSpct2.setSoLuongSpLoi(0);
        testSpct2.setTrangThai("hoat_dong");
        testSpct2 = sanPhamChiTietRepository.save(testSpct2);

        inventoryLotService.nhapLoMoi(testSpct2.getId(), 5, new BigDecimal("1000000"), null);
    }

    @Test
    @DisplayName("TC-INV-01: Đặt hàng thành công làm giảm đúng số lượng tồn kho")
    void test01_successfulCheckoutStockDeduction() {
        int initialStock = testSpct1.getSoLuongTon(); // 10
        int buyQty = 2;

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = testSpct1.getId();
        item.soLuong = buyQty;

        HoaDon hd = adminPosService.thanhToanPos(
                null, null, List.of(item), "TIEN_MAT", null, "Ghi chú test 1", testAdminAccount.getId(), "127.0.0.1"
        );

        Assertions.assertNotNull(hd);
        Assertions.assertEquals("DA_THANH_TOAN", hd.getTrangThaiThanhToan());

        SanPhamChiTiet reloadedSpct = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(initialStock - buyQty, reloadedSpct.getSoLuongTon()); // 8
    }

    @Test
    @DisplayName("TC-INV-02: Đặt quá số lượng tồn kho bị từ chối và giữ nguyên tồn kho")
    void test02_overQuantityOrderRejection() {
        int initialStock = testSpct1.getSoLuongTon(); // 10
        int buyQty = 15; // Exceeds 10

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = testSpct1.getId();
        item.soLuong = buyQty;

        Assertions.assertThrows(Exception.class, () -> {
            adminPosService.thanhToanPos(
                    null, null, List.of(item), "TIEN_MAT", null, "Ghi chú test 2", testAdminAccount.getId(), "127.0.0.1"
            );
        });

        SanPhamChiTiet reloadedSpct = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(initialStock, reloadedSpct.getSoLuongTon());
    }

    @Test
    @DisplayName("TC-INV-03: Mua hết toàn bộ tồn kho (tồn = 0 hợp lệ) và từ chối mua tiếp")
    void test03_buyingOutTotalStock() {
        int buyQty = testSpct1.getSoLuongTon(); // 10

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = testSpct1.getId();
        item.soLuong = buyQty;

        HoaDon hd = adminPosService.thanhToanPos(
                null, null, List.of(item), "TIEN_MAT", null, "Ghi chú test 3", testAdminAccount.getId(), "127.0.0.1"
        );
        Assertions.assertNotNull(hd);

        SanPhamChiTiet reloadedSpct = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(0, reloadedSpct.getSoLuongTon());

        // Try buying 1 more item when stock = 0
        PosItem extraItem = new PosItem();
        extraItem.idSanPhamChiTiet = testSpct1.getId();
        extraItem.soLuong = 1;

        Assertions.assertThrows(Exception.class, () -> {
            adminPosService.thanhToanPos(
                    null, null, List.of(extraItem), "TIEN_MAT", null, "Ghi chú test 3b", testAdminAccount.getId(), "127.0.0.1"
            );
        });
    }

    @Test
    @DisplayName("TC-INV-04: Hủy đơn POS hoàn tồn kho và chống cộng kho trùng lần 2")
    void test04_orderCancellationStockRestorationAndIdempotency() {
        int initialStock = testSpct1.getSoLuongTon(); // 10
        int buyQty = 3;

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = testSpct1.getId();
        item.soLuong = buyQty;

        HoaDon hd = adminPosService.thanhToanPos(
                null, null, List.of(item), "TIEN_MAT", null, "Ghi chú test 4", testAdminAccount.getId(), "127.0.0.1"
        );

        SanPhamChiTiet afterCheckoutSpct = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(initialStock - buyQty, afterCheckoutSpct.getSoLuongTon()); // 7

        // First cancellation -> Restores stock back to 10
        adminPosService.cancelOrderPos(hd.getId(), testAdminAccount.getId());

        SanPhamChiTiet afterCancelSpct = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(initialStock, afterCancelSpct.getSoLuongTon()); // 10

        // Second cancellation attempt -> Idempotency guard throws exception, stock stays 10
        Assertions.assertThrows(Exception.class, () -> {
            adminPosService.cancelOrderPos(hd.getId(), testAdminAccount.getId());
        });

        SanPhamChiTiet afterSecondCancelSpct = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(initialStock, afterSecondCancelSpct.getSoLuongTon()); // Still 10
    }

    @Test
    @DisplayName("TC-INV-07: Kiểm định hàng hoàn có thể bán lại (BAN_LAI) -> Hoàn tồn kho bán đúng 1 lần")
    void test07_returnSellableStockRestoration() {
        int initialStock = testSpct1.getSoLuongTon(); // 10
        int buyQty = 2;

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = testSpct1.getId();
        item.soLuong = buyQty;

        HoaDon hd = adminPosService.thanhToanPos(
                null, null, List.of(item), "TIEN_MAT", null, "Ghi chú test 7", testAdminAccount.getId(), "127.0.0.1"
        );

        // Mark return request DELIVERED_TO_SHOP
        hd.setTrangThaiHoanHang(ReturnStatus.DELIVERED_TO_SHOP);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.CHUA_XU_LY);
        hd.setLoaiYeuCauDoiTra("TRA");
        hoaDonRepository.save(hd);

        // Confirm inspection: BAN_LAI
        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, testAdminAccount.getId(), "127.0.0.1");

        SanPhamChiTiet reloadedSpct = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(initialStock, reloadedSpct.getSoLuongTon()); // Restored from 8 -> 10
    }

    @Test
    @DisplayName("TC-INV-08: Kiểm định hàng hoàn lỗi (HANG_LOI) -> Tăng soLuongSpLoi, KHÔNG tăng soLuongTon")
    void test08_returnDefectiveStockHandling() {
        int initialStock = testSpct1.getSoLuongTon(); // 10
        int buyQty = 2;

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = testSpct1.getId();
        item.soLuong = buyQty;

        HoaDon hd = adminPosService.thanhToanPos(
                null, null, List.of(item), "TIEN_MAT", null, "Ghi chú test 8", testAdminAccount.getId(), "127.0.0.1"
        );

        SanPhamChiTiet afterCheckout = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(initialStock - buyQty, afterCheckout.getSoLuongTon()); // 8
        Assertions.assertEquals(0, afterCheckout.getSoLuongSpLoi());

        // Mark return request DELIVERED_TO_SHOP
        hd.setTrangThaiHoanHang(ReturnStatus.DELIVERED_TO_SHOP);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.CHUA_XU_LY);
        hd.setLoaiYeuCauDoiTra("TRA");
        hoaDonRepository.save(hd);

        // Confirm inspection: HANG_LOI
        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "HANG_LOI", null, testAdminAccount.getId(), "127.0.0.1");

        SanPhamChiTiet reloadedSpct = sanPhamChiTietRepository.findById(testSpct1.getId()).orElseThrow();
        Assertions.assertEquals(initialStock - buyQty, reloadedSpct.getSoLuongTon()); // Remains 8!
        Assertions.assertEquals(buyQty, reloadedSpct.getSoLuongSpLoi()); // Increased to 2!
    }

    @Test
    @DisplayName("TC-INV-09: Chống nhập kho hoàn 2 lần (Idempotency Guard)")
    void test09_duplicateRestockIdempotencyProtection() {
        PosItem item = new PosItem();
        item.idSanPhamChiTiet = testSpct1.getId();
        item.soLuong = 2;

        HoaDon hd = adminPosService.thanhToanPos(
                null, null, List.of(item), "TIEN_MAT", null, "Ghi chú test 9", testAdminAccount.getId(), "127.0.0.1"
        );

        hd.setTrangThaiHoanHang(ReturnStatus.DELIVERED_TO_SHOP);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.CHUA_XU_LY);
        hd.setLoaiYeuCauDoiTra("TRA");
        hoaDonRepository.save(hd);

        // First inspection -> SUCCESS
        orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, testAdminAccount.getId(), "127.0.0.1");

        // Second inspection attempt -> IllegalStateException thrown, prevents double restock
        Assertions.assertThrows(IllegalStateException.class, () -> {
            orderViewService.xacNhanKiemKhoVaNhapKho(hd.getId(), "BAN_LAI", null, testAdminAccount.getId(), "127.0.0.1");
        });
    }

    @Test
    @DisplayName("TC-INV-10: Đổi hàng - Phân bổ kho sản phẩm đổi (EXCHANGE_STOCK_ALLOCATED)")
    void test10_exchangeStockReservationAndCompletion() {
        int initialStockSpct2 = testSpct2.getSoLuongTon(); // 5
        int exchangeQty = 2;

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = testSpct2.getId();
        item.soLuong = exchangeQty;

        HoaDon hd = adminPosService.thanhToanPos(
                null, null, List.of(item), "TIEN_MAT", null, "Ghi chú test 10", testAdminAccount.getId(), "127.0.0.1"
        );

        // Set up exchange request
        hd.setLoaiYeuCauDoiTra("DOI");
        hd.setTrangThaiHoanHang(ReturnStatus.RETURNED);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.DA_HOAN_KHO);
        hoaDonRepository.save(hd);

        // Reserve replacement stock
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), testAdminAccount.getId(), "127.0.0.1");

        SanPhamChiTiet afterReserveSpct2 = sanPhamChiTietRepository.findById(testSpct2.getId()).orElseThrow();
        Assertions.assertEquals(initialStockSpct2 - exchangeQty - exchangeQty, afterReserveSpct2.getSoLuongTon()); // 5 - 2 - 2 = 1

        // Duplicate reserve call is ignored
        exchangeStockReservationService.reserveReplacementStock(hd.getId(), testAdminAccount.getId(), "127.0.0.1");
        SanPhamChiTiet afterSecondReserve = sanPhamChiTietRepository.findById(testSpct2.getId()).orElseThrow();
        Assertions.assertEquals(1, afterSecondReserve.getSoLuongTon());
    }

    @Test
    @DisplayName("TC-INV-13: Khóa bi quan (Pessimistic Write Lock) ngăn chặn đọc ghi ghi đè dữ liệu")
    void test13_pessimisticLockingConcurrencyProtection() {
        Assertions.assertDoesNotThrow(() -> {
            sanPhamRepository.findByIdWithLock(testSanPham.getId());
            sanPhamChiTietRepository.findByIdWithLock(testSpct1.getId());
        });
    }
}
