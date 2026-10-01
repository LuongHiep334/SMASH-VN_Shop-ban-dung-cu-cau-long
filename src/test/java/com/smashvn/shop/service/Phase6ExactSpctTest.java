package com.smashvn.shop.service;

import com.smashvn.shop.dto.inventory.AllocationResult;
import com.smashvn.shop.dto.inventory.AllocationStatus;
import com.smashvn.shop.dto.inventory.OrderItemRequest;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.inventory.InventoryLotService;
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
public class Phase6ExactSpctTest {

    @Autowired
    private InventoryLotService inventoryLotService;

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
    private JdbcTemplate jdbcTemplate;

    private SanPham testSanPham;
    private SanPhamChiTiet spctRedZeroStock;
    private SanPhamChiTiet spctBlueTenStock;

    @BeforeEach
    void setUp() {
        // Clean up previous test products
        jdbcTemplate.execute("DELETE FROM SanPhamChiTietThuocTinh WHERE id_san_pham_chi_tiet IN (SELECT id FROM SanPhamChiTiet WHERE id_san_pham IN (SELECT id FROM SanPham WHERE ten_san_pham LIKE 'Vợt Test Exact SPCT%'))");
        jdbcTemplate.execute("DELETE FROM SanPhamChiTiet WHERE id_san_pham IN (SELECT id FROM SanPham WHERE ten_san_pham LIKE 'Vợt Test Exact SPCT%')");
        jdbcTemplate.execute("DELETE FROM SanPham WHERE ten_san_pham LIKE 'Vợt Test Exact SPCT%'");

        ThuocTinh ttMauSac = thuocTinhRepository.findAll().stream()
                .filter(t -> t.getTenThuocTinh() != null && ("Màu sắc".equalsIgnoreCase(t.getTenThuocTinh()) || "Mau Sac".equalsIgnoreCase(t.getTenThuocTinh())))
                .findFirst().orElseGet(() -> {
                    ThuocTinh tt = new ThuocTinh();
                    tt.setTenThuocTinh("Màu sắc");
                    return thuocTinhRepository.save(tt);
                });

        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseThrow();
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseThrow();
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseThrow();

        testSanPham = new SanPham();
        testSanPham.setTenSanPham("Vợt Test Exact SPCT " + System.currentTimeMillis());
        testSanPham.setDanhMuc(dm);
        testSanPham.setThuongHieu(th);
        testSanPham.setNhanVien(nv);
        testSanPham.setMoTa("Test exact matching");
        testSanPham.setTrangThai("dang_ban");
        testSanPham = sanPhamRepository.save(testSanPham);

        // Variant 1: Red color, stock = 0
        spctRedZeroStock = new SanPhamChiTiet();
        spctRedZeroStock.setSanPham(testSanPham);
        spctRedZeroStock.setSoLuongTon(0);
        spctRedZeroStock.setGiaBan(new BigDecimal("1000000"));
        spctRedZeroStock.setTrangThai("dang_ban");
        spctRedZeroStock = sanPhamChiTietRepository.save(spctRedZeroStock);

        SanPhamChiTietThuocTinh ttRed = new SanPhamChiTietThuocTinh();
        ttRed.setSanPhamChiTiet(spctRedZeroStock);
        ttRed.setThuocTinh(ttMauSac);
        ttRed.setGiaTri("Đỏ");
        sanPhamChiTietThuocTinhRepository.save(ttRed);

        // Variant 2: Blue color, stock = 10
        spctBlueTenStock = new SanPhamChiTiet();
        spctBlueTenStock.setSanPham(testSanPham);
        spctBlueTenStock.setSoLuongTon(10);
        spctBlueTenStock.setGiaBan(new BigDecimal("1000000"));
        spctBlueTenStock.setTrangThai("dang_ban");
        spctBlueTenStock = sanPhamChiTietRepository.save(spctBlueTenStock);

        SanPhamChiTietThuocTinh ttBlue = new SanPhamChiTietThuocTinh();
        ttBlue.setSanPhamChiTiet(spctBlueTenStock);
        ttBlue.setThuocTinh(ttMauSac);
        ttBlue.setGiaTri("Xanh");
        sanPhamChiTietThuocTinhRepository.save(ttBlue);
    }

    @Test
    @DisplayName("Verify exact SPCT matching: Allocate for Red (stock 0) fails and does NOT touch Blue (stock 10)")
    void testExactSpctMatching() {
        // Request allocation for Red variant (stock = 0)
        OrderItemRequest req = new OrderItemRequest(1, spctRedZeroStock.getId(), 1);
        AllocationResult result = inventoryLotService.allocateFifo(List.of(req));

        // EXPECTED: INSUFFICIENT_STOCK
        assertEquals(AllocationStatus.INSUFFICIENT_STOCK, result.status());

        // Verify Blue variant stock remains 10 (untouched)
        SanPhamChiTiet blueReloaded = sanPhamChiTietRepository.findById(spctBlueTenStock.getId()).orElseThrow();
        assertEquals(10, blueReloaded.getSoLuongTon(), "SPCT Xanh phải giữ nguyên tồn kho = 10, không được trừ nhầm");

        // Verify Red variant stock remains 0
        SanPhamChiTiet redReloaded = sanPhamChiTietRepository.findById(spctRedZeroStock.getId()).orElseThrow();
        assertEquals(0, redReloaded.getSoLuongTon());
    }
}
