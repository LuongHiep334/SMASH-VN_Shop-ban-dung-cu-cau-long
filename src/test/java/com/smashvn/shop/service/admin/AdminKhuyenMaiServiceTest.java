package com.smashvn.shop.service.admin;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.exception.PromotionValidationException;
import com.smashvn.shop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminKhuyenMaiServiceTest {

    @Autowired
    private AdminKhuyenMaiService adminKhuyenMaiService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    private TaiKhoan testTaiKhoan;
    private SanPham testSanPham;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.now().plusDays(1);
        endTime = LocalDateTime.now().plusDays(5);

        // Find or create Category
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc d = new DanhMuc();
            d.setTenDanhMuc("Test Category");
            return danhMucRepository.save(d);
        });

        // Find or create Brand
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu t = new ThuongHieu();
            t.setTenThuongHieu("Test Brand");
            return thuongHieuRepository.save(t);
        });

        // Find or create staff / employee
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan staffTk = new TaiKhoan();
            staffTk.setUsername("staff_promo_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com");
            staffTk.setMatKhau("testpass123");
            staffTk.setVaiTro("NV");
            staffTk.setTrangThai("hoat_dong");

            staffTk = taiKhoanRepository.save(staffTk);

            NhanVien n = new NhanVien();
            n.setTaiKhoan(staffTk);
            n.setHoTenNv("Staff Promo Test");
            n.setChucVu("Staff");
            n.setSoDienThoaiNv("0981112224");
            return nhanVienRepository.save(n);
        });
        testTaiKhoan = nv.getTaiKhoan();

        // Create SanPham
        SanPham sp = new SanPham();
        sp.setTenSanPham("Test San Pham Promo " + UUID.randomUUID().toString().substring(0, 8));
        sp.setTrangThai("dang_ban");
        sp.setMoTa("Test description");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        testSanPham = sanPhamRepository.save(sp);
    }

    // =========================================================================
    // 1. CAMPAIGN (ĐỢT GIẢM GIÁ) VALIDATION TESTS
    // =========================================================================

    @Test
    void testCreateDotGiamGia_ValidBoundaries() {
        // Boundary 1: phanTramGiam = 1
        assertNotNull(adminKhuyenMaiService.createDotGiamGia(
                "Super Sale 1", startTime, endTime, 1, "Theo Phần Trăm",
                List.of(testSanPham.getId()), testTaiKhoan.getId(), "127.0.0.1"
        ));

        // Create second product so there is no conflict on the same product
        SanPham sp2 = new SanPham();
        sp2.setTenSanPham("Test San Pham Promo 2 " + UUID.randomUUID().toString().substring(0, 8));
        sp2.setTrangThai("dang_ban");
        sp2.setMoTa("Test description 2");
        sp2.setDanhMuc(testSanPham.getDanhMuc());
        sp2.setThuongHieu(testSanPham.getThuongHieu());
        sp2.setNhanVien(testSanPham.getNhanVien());
        sp2 = sanPhamRepository.save(sp2);

        // Boundary 40: phanTramGiam = 40
        assertNotNull(adminKhuyenMaiService.createDotGiamGia(
                "Super Sale 40", startTime.plusDays(10), endTime.plusDays(10), 40, "Theo Phần Trăm",
                List.of(sp2.getId()), testTaiKhoan.getId(), "127.0.0.1"
        ));
    }

    @Test
    void testCreateDotGiamGia_InvalidBoundaries() {
        // Boundary 0: phanTramGiam = 0 -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createDotGiamGia(
                    "Super Sale 0", startTime, endTime, 0, "Theo Phần Trăm",
                    List.of(testSanPham.getId()), testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Boundary 41: phanTramGiam = 41 -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createDotGiamGia(
                    "Super Sale 41", startTime, endTime, 41, "Theo Phần Trăm",
                    List.of(testSanPham.getId()), testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Negative: phanTramGiam = -5 -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createDotGiamGia(
                    "Super Sale Neg", startTime, endTime, -5, "Theo Phần Trăm",
                    List.of(testSanPham.getId()), testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    // =========================================================================
    // 2. VOUCHER VALUE (giaTri) VALIDATION TESTS
    // =========================================================================

    @Test
    void testCreatePhieuGiamGia_PercentageBoundaries() {
        // Valid boundaries: 1% and 100%
        assertNotNull(adminKhuyenMaiService.createPhieuGiamGia(
                "VOUCHER_PCT_1", BigDecimal.ONE, "%", startTime, endTime, 10,
                BigDecimal.ZERO, "Giảm phần trăm", BigDecimal.TEN, testTaiKhoan.getId(), "127.0.0.1"
        ));

        assertNotNull(adminKhuyenMaiService.createPhieuGiamGia(
                "VOUCHER_PCT_100", new BigDecimal("100"), "%", startTime, endTime, 10,
                BigDecimal.ZERO, "Giảm phần trăm", BigDecimal.TEN, testTaiKhoan.getId(), "127.0.0.1"
        ));

        // Invalid boundary: 0% -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VOUCHER_PCT_0", BigDecimal.ZERO, "%", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm phần trăm", BigDecimal.TEN, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Invalid boundary: 101% -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VOUCHER_PCT_101", new BigDecimal("101"), "%", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm phần trăm", BigDecimal.TEN, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Decimal percentage: 10.5% -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VOUCHER_PCT_DEC", new BigDecimal("10.5"), "%", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm phần trăm", BigDecimal.TEN, testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    @Test
    void testCreatePhieuGiamGia_VndBoundaries() {
        // Valid boundaries: 1 VND and 100,000,000 VND
        assertNotNull(adminKhuyenMaiService.createPhieuGiamGia(
                "VOUCHER_VND_1", BigDecimal.ONE, "VND", startTime, endTime, 10,
                BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
        ));

        assertNotNull(adminKhuyenMaiService.createPhieuGiamGia(
                "VOUCHER_VND_MAX", new BigDecimal("100000000"), "VND", startTime, endTime, 10,
                BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
        ));

        // Invalid boundary: 0 VND -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VOUCHER_VND_0", BigDecimal.ZERO, "VND", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Invalid boundary: 100,000,001 VND -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VOUCHER_VND_OVER", new BigDecimal("100000001"), "VND", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Decimal VND: 1000.5 VND -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VOUCHER_VND_DEC", new BigDecimal("1000.5"), "VND", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    // =========================================================================
    // 3. VOUCHER QUANTITY (soLuongConLai) VALIDATION TESTS
    // =========================================================================

    @Test
    void testCreatePhieuGiamGia_QuantityBoundaries() {
        // Valid boundaries: 1 and 1,000,000
        assertNotNull(adminKhuyenMaiService.createPhieuGiamGia(
                "V_QTY_1", BigDecimal.TEN, "VND", startTime, endTime, 1,
                BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
        ));

        assertNotNull(adminKhuyenMaiService.createPhieuGiamGia(
                "V_QTY_MAX", BigDecimal.TEN, "VND", startTime, endTime, 1000000,
                BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
        ));

        // Invalid boundary (create): 0 -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "V_QTY_0", BigDecimal.TEN, "VND", startTime, endTime, 0,
                    BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Invalid boundary (create): 1,000,001 -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "V_QTY_OVER", BigDecimal.TEN, "VND", startTime, endTime, 1000001,
                    BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    @Test
    void testUpdatePhieuGiamGia_QuantityBoundaries() {
        // Save first with quantity = 10
        PhieuGiamGia pgg = adminKhuyenMaiService.createPhieuGiamGia(
                "V_QTY_EDIT", BigDecimal.TEN, "VND", startTime, endTime, 10,
                BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
        );

        // Edit boundary: quantity = 0 -> allowed in edit
        assertNotNull(adminKhuyenMaiService.updatePhieuGiamGia(
                pgg.getId(), "V_QTY_EDIT", BigDecimal.TEN, "VND", startTime, endTime, 0,
                BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
        ));

        // Edit boundary: quantity = -1 -> not allowed
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.updatePhieuGiamGia(
                    pgg.getId(), "V_QTY_EDIT", BigDecimal.TEN, "VND", startTime, endTime, -1,
                    BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    // =========================================================================
    // 4. CROSS-FIELD TYPE AND UNIT VALIDATION TESTS
    // =========================================================================

    @Test
    void testCreatePhieuGiamGia_TypeMismatches() {
        // Mismatch 1: donVi = '%' but loaiGiamGia = 'Giảm trực tiếp' -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "MISMATCH_1", BigDecimal.TEN, "%", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm trực tiếp", BigDecimal.TEN, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Mismatch 2: donVi = 'VND' but loaiGiamGia = 'Giảm phần trăm' -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "MISMATCH_2", BigDecimal.TEN, "VND", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm phần trăm", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    @Test
    void testCreatePhieuGiamGia_CapValidations() {
        // Percentage voucher without max discount cap -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "NO_CAP", BigDecimal.TEN, "%", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm phần trăm", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Percentage voucher with max discount cap = 0 -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "ZERO_CAP", BigDecimal.TEN, "%", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm phần trăm", BigDecimal.ZERO, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Fixed amount voucher with non-null max discount cap -> should throw
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VND_WITH_CAP", BigDecimal.TEN, "VND", startTime, endTime, 10,
                    BigDecimal.ZERO, "Giảm trực tiếp", BigDecimal.TEN, testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    // =========================================================================
    // 5. VOUCHER TYPE CONVERSION TESTS
    // =========================================================================

    @Test
    void testVoucherTypeConversions() {
        // 1. Create a percentage voucher (cap is required)
        PhieuGiamGia voucher = adminKhuyenMaiService.createPhieuGiamGia(
                "CONV_TEST", new BigDecimal("20"), "%", startTime, endTime, 50,
                BigDecimal.ZERO, "Giảm phần trăm", new BigDecimal("50000"), testTaiKhoan.getId(), "127.0.0.1"
        );
        assertNotNull(voucher.getGiaTriGiamToiDa());

        // 2. Convert to VND voucher:
        // Service layer should enforce cross-field check (VND cannot have a cap, resolves to null).
        // Since we pass resolves cap null, it passes successfully.
        PhieuGiamGia updatedToVnd = adminKhuyenMaiService.updatePhieuGiamGia(
                voucher.getId(), "CONV_TEST", new BigDecimal("100000"), "VND", startTime, endTime, 50,
                BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
        );
        assertEquals(new BigDecimal("100000"), updatedToVnd.getGiaTri());
        assertEquals("VND", updatedToVnd.getDonVi());
        assertNull(updatedToVnd.getGiaTriGiamToiDa());

        // 3. Convert back to percentage voucher:
        // It must require a max cap (fails if null)
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.updatePhieuGiamGia(
                    updatedToVnd.getId(), "CONV_TEST", new BigDecimal("20"), "%", startTime, endTime, 50,
                    BigDecimal.ZERO, "Giảm phần trăm", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });

        // Convert successfully with valid cap
        PhieuGiamGia updatedToPct = adminKhuyenMaiService.updatePhieuGiamGia(
                updatedToVnd.getId(), "CONV_TEST", new BigDecimal("20"), "%", startTime, endTime, 50,
                BigDecimal.ZERO, "Giảm phần trăm", new BigDecimal("60000"), testTaiKhoan.getId(), "127.0.0.1"
        );
        assertEquals(new BigDecimal("20"), updatedToPct.getGiaTri());
        assertEquals("%", updatedToPct.getDonVi());
        assertEquals(new BigDecimal("60000"), updatedToPct.getGiaTriGiamToiDa());
    }

    @Test
    void testUpdateDotGiamGia_Dates() {
        // 1. Create a campaign
        DotGiamGia dgg = adminKhuyenMaiService.createDotGiamGia(
                "Test Campaign Dates", startTime, endTime, 15, "Theo Phần Trăm",
                List.of(testSanPham.getId()), testTaiKhoan.getId(), "127.0.0.1"
        );
        assertNotNull(dgg);
        assertEquals(startTime, dgg.getNgayBatDau());
        assertEquals(endTime, dgg.getNgayKetThuc());

        // 2. Update with new dates
        LocalDateTime newStart = startTime.plusHours(1);
        LocalDateTime newEnd = endTime.plusHours(2);

        DotGiamGia updated = adminKhuyenMaiService.updateDotGiamGia(
                dgg.getId(), "Test Campaign Dates", newStart, newEnd, 15, "Theo Phần Trăm",
                List.of(testSanPham.getId()), testTaiKhoan.getId(), "127.0.0.1"
        );

        assertNotNull(updated);
        assertEquals(newStart, updated.getNgayBatDau());
        assertEquals(newEnd, updated.getNgayKetThuc());
    }

    @Test
    void testParseVndCurrency() {
        // Valid tests
        assertEquals(new BigDecimal("500000"), adminKhuyenMaiService.parseVndCurrency("500000", "Field", false));
        assertEquals(new BigDecimal("500000"), adminKhuyenMaiService.parseVndCurrency("500.000", "Field", false));
        assertEquals(new BigDecimal("1500000"), adminKhuyenMaiService.parseVndCurrency("1,500,000", "Field", false));
        assertEquals(new BigDecimal("500000"), adminKhuyenMaiService.parseVndCurrency("  500.000  ", "Field", false));
        assertNull(adminKhuyenMaiService.parseVndCurrency("", "Field", true));
        assertNull(adminKhuyenMaiService.parseVndCurrency("   ", "Field", true));
        assertNull(adminKhuyenMaiService.parseVndCurrency(null, "Field", true));

        // Invalid tests
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.parseVndCurrency("", "Field", false);
        });
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.parseVndCurrency("-100000", "Field", false);
        });
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.parseVndCurrency("abc", "Field", false);
        });
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.parseVndCurrency("500,5", "Field", false);
        });
    }

    @Test
    void testFindProductsByPriceRange_InvalidRange() {
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.findProductsByPriceRange(null, BigDecimal.TEN);
        });
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.findProductsByPriceRange(BigDecimal.ZERO, BigDecimal.TEN);
        });
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.findProductsByPriceRange(new BigDecimal("-10"), BigDecimal.TEN);
        });
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.findProductsByPriceRange(new BigDecimal("100"), new BigDecimal("50"));
        });
        // Test no matched products
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.findProductsByPriceRange(new BigDecimal("999999999"), new BigDecimal("9999999999"));
        });
    }

    @Test
    void testCreateDotGiamGia_PriceRange_Valid() {
        // Create variant for testSanPham so it matches the price range query
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(testSanPham);
        spct.setMauSac("Red");
        spct.setMucCang("22 lbs");
        spct.setTrongLuong("3U");
        spct.setGiaBan(new BigDecimal("900000"));
        spct.setSoLuongTon(10);
        spct.setTrangThai("dang_ban");
        sanPhamChiTietRepository.save(spct);

        DotGiamGia dgg = adminKhuyenMaiService.createDotGiamGia(
                "Price Range Sale", startTime, endTime, 15, "Theo Phần Trăm",
                "PRICE_RANGE", null, new BigDecimal("800000"), new BigDecimal("1000000"),
                testTaiKhoan.getId(), "127.0.0.1"
        );
        assertNotNull(dgg);
        assertTrue(dgg.getSanPhams().stream().anyMatch(sp -> sp.getId().equals(testSanPham.getId())));
    }

    @Test
    void testCreateDotGiamGia_Manual_DiscontinuedProduct() {
        // Discontinue the test product
        testSanPham.setTrangThai("ngung_ban");
        sanPhamRepository.save(testSanPham);

        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createDotGiamGia(
                    "Manual Sale Discontinued", startTime, endTime, 15, "Theo Phần Trăm",
                    "MANUAL", List.of(testSanPham.getId()), null, null,
                    testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    // =========================================================================
    // 7. NEW TIMING RULE TESTS ("TẠO LÀ BẮT ĐẦU NGAY")
    // =========================================================================

    // =========================================================================
    // 7. TIMING RULE TESTS ("KHÔNG CHO CHỌN THỜI GIAN TRONG QUÁ KHỨ")
    // =========================================================================

    @Test
    void testCampaignTiming_RejectPastStartDate() {
        // Start in past -> REJECT
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createDotGiamGia(
                    "Past Start Campaign", LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2), 20, "Theo Phần Trăm",
                    List.of(testSanPham.getId()), testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    @Test
    void testCampaignTiming_RejectEndDateBeforeStartDate() {
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createDotGiamGia(
                    "End Before Start Campaign", LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(1), 20, "Theo Phần Trăm",
                    List.of(testSanPham.getId()), testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    @Test
    void testVoucherTiming_RejectPastStartDate() {
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VOUCHER_PAST_START", new BigDecimal("50000"), "VND",
                    LocalDateTime.now().minusDays(2), LocalDateTime.now().plusDays(3), 20,
                    BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    @Test
    void testVoucherTiming_RejectEndDateBeforeStartDate() {
        assertThrows(PromotionValidationException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(
                    "VOUCHER_END_BEFORE_START", new BigDecimal("50000"), "VND",
                    LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(1), 20,
                    BigDecimal.ZERO, "Giảm trực tiếp", null, testTaiKhoan.getId(), "127.0.0.1"
            );
        });
    }

    @Test
    void testVoucherTiming_ValidFutureDates() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(5);
        PhieuGiamGia voucher = adminKhuyenMaiService.createPhieuGiamGia(
                "VOUCHER_VALID_DATES", new BigDecimal("20"), "%",
                start, end, 20,
                BigDecimal.ZERO, "Giảm phần trăm", new BigDecimal("50000"), testTaiKhoan.getId(), "127.0.0.1"
        );
        assertNotNull(voucher);
        assertEquals(start, voucher.getNgayBatDau());
        assertEquals(end, voucher.getNgayKetThuc());
    }
}
