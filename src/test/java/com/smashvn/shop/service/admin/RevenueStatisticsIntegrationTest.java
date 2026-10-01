package com.smashvn.shop.service.admin;
import com.smashvn.shop.service.admin.AdminThongKeService;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class RevenueStatisticsIntegrationTest {

    @Autowired
    private AdminThongKeService adminThongKeService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private KhachHang testKhachHang;
    private PhuongThucThanhToan ptttCOD;
    private PhuongThucThanhToan ptttOnline;
    private DonViVanChuyen testDvvc;
    private SanPhamChiTiet testSpct;

    @BeforeEach
    void setUp() {
        // Find / Create customer
        List<KhachHang> khs = khachHangRepository.findAll();
        if (khs.isEmpty()) {
            TaiKhoan customerUser = new TaiKhoan();
            customerUser.setUsername("customer-stats-test-" + System.nanoTime() + "@smashvn.com");
            customerUser.setMatKhau("123");
            customerUser.setVaiTro("KH");

            customerUser = taiKhoanRepository.save(customerUser);

            KhachHang newKh = new KhachHang();
            newKh.setHoKh("Test");
            newKh.setTenKh("Customer");
            newKh.setSoDienThoaiKh("0912345678");
            newKh.setLaTaiKhoanNoiBo(false);
            newKh.setTaiKhoan(customerUser);
            testKhachHang = khachHangRepository.save(newKh);
        } else {
            testKhachHang = khs.get(0);
        }

        // Find / Create payment methods
        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        ptttCOD = ptts.stream()
                .filter(p -> "COD".equalsIgnoreCase(p.getTenPhuongThuc()))
                .findFirst()
                .orElseGet(() -> {
                    PhuongThucThanhToan p = new PhuongThucThanhToan();
                    p.setTenPhuongThuc("COD");
                    return phuongThucThanhToanDAO.save(p);
                });

        ptttOnline = ptts.stream()
                .filter(p -> !"COD".equalsIgnoreCase(p.getTenPhuongThuc()))
                .findFirst()
                .orElseGet(() -> {
                    PhuongThucThanhToan p = new PhuongThucThanhToan();
                    p.setTenPhuongThuc("SEPAY");
                    return phuongThucThanhToanDAO.save(p);
                });

        // Find / Create shipping carrier
        List<DonViVanChuyen> dvvcs = donViVanChuyenDAO.findAll();
        if (dvvcs.isEmpty()) {
            DonViVanChuyen d = new DonViVanChuyen();
            d.setTenDonVi("GHTK");
            d.setHotline("19001000");
            d.setWebsite("ghtk.vn");
            testDvvc = donViVanChuyenDAO.save(d);
        } else {
            testDvvc = dvvcs.get(0);
        }

        // Seed NhanVien for product
        TaiKhoan nvUser = new TaiKhoan();
        nvUser.setUsername("staff-stats-" + System.nanoTime() + "@smashvn.com");
        nvUser.setMatKhau("123");
        nvUser.setVaiTro("NV");

        nvUser = taiKhoanRepository.save(nvUser);
        NhanVien nv = new NhanVien();
        nv.setTaiKhoan(nvUser);
        nv.setHoTenNv("Stats Staff");
        nv.setChucVu("Nhân viên");
        nv.setSoDienThoaiNv("0888999111");
        nv = nhanVienRepository.save(nv);

        // Seed DanhMuc / ThuongHieu / SanPham / SanPhamChiTiet
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc d = new DanhMuc();
            d.setTenDanhMuc("Mặc định");
            return danhMucRepository.save(d);
        });
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu t = new ThuongHieu();
            t.setTenThuongHieu("Mặc định");
            return thuongHieuRepository.save(t);
        });
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Stats " + System.nanoTime());
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(nv);
        sp = sanPhamRepository.save(sp);
        testSpct = new SanPhamChiTiet();
        testSpct.setSanPham(sp);
        testSpct.setMauSac("Đen");
        testSpct.setSoLuongTon(50);
        testSpct.setGiaBan(BigDecimal.valueOf(300000));
        testSpct = sanPhamChiTietRepository.save(testSpct);

        // Clear statistics cache to ensure clean test environment
        if (cacheManager != null && cacheManager.getCache("thongke") != null) {
            cacheManager.getCache("thongke").clear();
        }
    }

    private HoaDon createTestOrder(String status, String paymentMethod, String paymentStatus, PhuongThucThanhToan pttt, BigDecimal amount) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(testDvvc);
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(amount);
        hd.setMaDonHang("TEST-STATS-" + System.nanoTime());
        hd.setTrangThaiDonHang(status);
        hd.setPaymentMethod(paymentMethod);
        hd.setPaymentStatus(paymentStatus);
        
        String tStatus = "CHO_THANH_TOAN";
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            tStatus = "DA_THANH_TOAN";
        } else if ("refunded".equalsIgnoreCase(paymentStatus)) {
            tStatus = "REFUNDED";
        } else if ("cancelled".equalsIgnoreCase(paymentStatus)) {
            tStatus = "HUY";
        }
        hd.setTrangThaiThanhToan(tStatus);
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(amount);
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }

    private void createSuccessfulRefund(HoaDon order, BigDecimal amount) {
        PaymentTransaction refund = new PaymentTransaction();
        refund.setOrder(order);
        refund.setTransactionId("REFUND_STATS_" + order.getId() + "_" + System.nanoTime());
        refund.setAmount(amount);
        refund.setGateway("MANUAL_REFUND");
        refund.setStatus("REFUND_SUCCESS");
        refund.setRawPayload("{}");
        refund.setCreatedAt(LocalDateTime.now());
        paymentTransactionRepository.save(refund);
    }

    private record KpiSnapshot(
            BigDecimal actual,
            BigDecimal expected,
            BigDecimal refunded,
            BigDecimal pendingRefund) {
    }

    private void clearStatisticsCache() {
        if (cacheManager != null && cacheManager.getCache("thongke") != null) {
            cacheManager.getCache("thongke").clear();
        }
    }

    private KpiSnapshot snapshot(LocalDateTime start, LocalDateTime end) {
        clearStatisticsCache();
        Map<String, Object> stats = adminThongKeService.getStatisticsData(start, end);
        BigDecimal actual = (BigDecimal) stats.get("actualRevenue");
        assertNotNull(actual);
        assertTrue(actual.compareTo(BigDecimal.ZERO) >= 0, "actualRevenue must never be negative");
        return new KpiSnapshot(
                actual,
                (BigDecimal) stats.get("expectedRevenue"),
                (BigDecimal) stats.get("refundedRevenue"),
                (BigDecimal) stats.get("pendingRefund"));
    }

    private void assertDelta(
            KpiSnapshot before,
            KpiSnapshot after,
            long actual,
            long expected,
            long refunded,
            long pendingRefund) {
        assertEquals(0, BigDecimal.valueOf(actual).compareTo(after.actual().subtract(before.actual())), "actualRevenue delta");
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(after.expected().subtract(before.expected())), "expectedRevenue delta");
        assertEquals(0, BigDecimal.valueOf(refunded).compareTo(after.refunded().subtract(before.refunded())), "refundedRevenue delta");
        assertEquals(0, BigDecimal.valueOf(pendingRefund).compareTo(after.pendingRefund().subtract(before.pendingRefund())), "pendingRefund delta");
    }

    private LocalDateTime[] shortRange() {
        return new LocalDateTime[] { LocalDateTime.now().minusMinutes(2), LocalDateTime.now().plusMinutes(2) };
    }

    @Test
    void case1_codProcessingUnpaidContributesNothing() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, BigDecimal.valueOf(5_000_000));
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 0, 5_000_000, 0, 0);
    }

    @Test
    void case2_onlinePaidProcessingIsExpectedButNotActual() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        HoaDon order = createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(5_000_000));
        // Dữ liệu production cũ có thể lưu trực tiếp alias lowercase "paid".
        order.setTrangThaiThanhToan("paid");
        hoaDonRepository.save(order);
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 0, 5_000_000, 0, 0);
    }

    @Test
    void case3_onlinePaidDeliveredContributesActualRevenue() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        createTestOrder("da_giao", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(5_000_000));
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 5_000_000, 0, 0, 0);
    }

    @Test
    void case4_onlinePaidCancelledPendingRefundOnlyContributesPendingRefund() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        HoaDon order = createTestOrder("da_huy", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(5_000_000));
        order.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hoaDonRepository.save(order);
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 0, 0, 0, 5_000_000);
    }

    @Test
    void case5_onlinePaidCancelledCompletedRefundNeverMakesActualNegative() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        HoaDon order = createTestOrder("da_huy", "SEPAY", "refunded", ptttOnline, BigDecimal.valueOf(5_000_000));
        createSuccessfulRefund(order, BigDecimal.valueOf(5_000_000));
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 0, 0, 5_000_000, 0);
    }

    @Test
    void case6_onlinePaidDeliveredThenCompletedRefundIsExcludedFromActual() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        HoaDon order = createTestOrder("da_giao", "SEPAY", "refunded", ptttOnline, BigDecimal.valueOf(5_000_000));
        createSuccessfulRefund(order, BigDecimal.valueOf(5_000_000));
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 0, 0, 5_000_000, 0);
    }

    @Test
    void case7_largerPredeliveryRefundCannotOffsetAnotherDeliveredOrder() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        createTestOrder("da_giao", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(3_000_000));
        HoaDon refundedBeforeDelivery = createTestOrder("da_huy", "SEPAY", "refunded", ptttOnline, BigDecimal.valueOf(5_000_000));
        createSuccessfulRefund(refundedBeforeDelivery, BigDecimal.valueOf(5_000_000));
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 3_000_000, 0, 5_000_000, 0);
        assertNotEquals(0, BigDecimal.valueOf(-2_000_000).compareTo(after.actual().subtract(before.actual())));
    }

    @Test
    void codUnpaidCancelledReturnCannotCreatePendingRefund() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        HoaDon order = createTestOrder("da_huy", "COD", "pending", ptttCOD, BigDecimal.valueOf(5_000_000));
        order.setLoaiYeuCauDoiTra("TRA");
        order.setTrangThaiHoanHang(ReturnStatus.RETURNED);
        hoaDonRepository.save(order);
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 0, 0, 0, 0);
    }

    @Test
    void refundEventAtPeriodStartForOlderOrderCannotMakeActualNegative() {
        LocalDateTime[] range = shortRange();
        KpiSnapshot before = snapshot(range[0], range[1]);

        HoaDon olderOrder = createTestOrder("da_giao", "SEPAY", "refunded", ptttOnline, BigDecimal.valueOf(5_000_000));
        olderOrder.setNgayTao(range[0].minusDays(1));
        hoaDonRepository.save(olderOrder);
        createSuccessfulRefund(olderOrder, BigDecimal.valueOf(5_000_000));
        entityManager.flush();

        KpiSnapshot after = snapshot(range[0], range[1]);
        assertDelta(before, after, 0, 0, 5_000_000, 0);
    }

    @Test
    void testRevenueClassificationRules() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(2);
        LocalDateTime end = LocalDateTime.now().plusMinutes(2);

        // Fetch initial statistics to isolate from database pollution
        Map<String, Object> initialStats = adminThongKeService.getStatisticsData(start, end);
        BigDecimal initialGrossRevenue = (BigDecimal) initialStats.get("grossRevenue");
        BigDecimal initialActualRevenue = (BigDecimal) initialStats.get("actualRevenue");
        BigDecimal initialExpectedRevenue = (BigDecimal) initialStats.get("expectedRevenue");
        BigDecimal initialRefundedRevenue = (BigDecimal) initialStats.get("refundedRevenue");
        BigDecimal initialPendingRefund = (BigDecimal) initialStats.get("pendingRefund");

        // 1. Projected Revenue Inclusions
        // Online đã thanh toán và còn hoạt động => Projected Revenue.
        createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(100000));
        createTestOrder("da_xac_nhan", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(110000));
        createTestOrder("dang_lay_hang", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(120000));
        createTestOrder("dang_giao", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(130000));

        // COD + active states => Projected Revenue (COD orders are counted for all active states)
        createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, BigDecimal.valueOf(50000));
        createTestOrder("da_xac_nhan", "COD", "pending", ptttCOD, BigDecimal.valueOf(60000));
        createTestOrder("dang_lay_hang", "COD", "pending", ptttCOD, BigDecimal.valueOf(70000));
        createTestOrder("dang_giao", "COD", "pending", ptttCOD, BigDecimal.valueOf(80000));

        // 2. Actual Revenue Inclusions
        // DA_GIAO => Actual Revenue
        createTestOrder("da_giao", "COD", "paid", ptttCOD, BigDecimal.valueOf(200000));
        // HOAN_THANH => Actual Revenue
        createTestOrder("hoan_thanh", "COD", "paid", ptttCOD, BigDecimal.valueOf(250000));

        // 3. Cancel before delivery
        // PAID + DA_HUY => removed from Projected Revenue, does not affect Actual Revenue
        createTestOrder("da_huy", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(300000));

        // 4. Delivered then Refunded toàn phần: loại khỏi actual, refund là KPI riêng.
        HoaDon refundedOrder = createTestOrder("da_giao", "SEPAY", "refunded", ptttOnline, BigDecimal.valueOf(400000));
        refundedOrder.setRefundStatus(RefundStatus.COMPLETED);
        hoaDonRepository.save(refundedOrder);
        createSuccessfulRefund(refundedOrder, BigDecimal.valueOf(400000));

        // 5. Theo mô hình order-level hiện tại, refund completed loại cả order khỏi actual.
        HoaDon partialRefundOrder = createTestOrder("da_giao", "SEPAY", "refunded", ptttOnline, BigDecimal.valueOf(500000));
        partialRefundOrder.setRefundStatus(RefundStatus.COMPLETED);
        hoaDonRepository.save(partialRefundOrder);
        createSuccessfulRefund(partialRefundOrder, BigDecimal.valueOf(300000));

        // 6. Hàng trả đã nhập kho nhưng chưa hoàn tiền phải hiện ở KPI chờ hoàn.
        HoaDon pendingRefundOrder = createTestOrder("da_giao", "SEPAY", "paid", ptttOnline, BigDecimal.valueOf(275000));
        pendingRefundOrder.setLoaiYeuCauDoiTra("TRA");
        pendingRefundOrder.setTrangThaiHoanHang(ReturnStatus.RETURNED);
        pendingRefundOrder.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hoaDonRepository.save(pendingRefundOrder);

        // Clear statistics cache before second fetch to bypass @Cacheable
        if (cacheManager != null && cacheManager.getCache("thongke") != null) {
            cacheManager.getCache("thongke").clear();
        }

        // Calculate statistics
        Map<String, Object> stats = adminThongKeService.getStatisticsData(start, end);

        BigDecimal grossRevenue = (BigDecimal) stats.get("grossRevenue");
        BigDecimal actualRevenue = (BigDecimal) stats.get("actualRevenue");
        BigDecimal expectedRevenue = (BigDecimal) stats.get("expectedRevenue");
        BigDecimal refundedRevenue = (BigDecimal) stats.get("refundedRevenue");
        BigDecimal pendingRefund = (BigDecimal) stats.get("pendingRefund");

        BigDecimal diffGross = grossRevenue.subtract(initialGrossRevenue != null ? initialGrossRevenue : BigDecimal.ZERO);
        BigDecimal diffActual = actualRevenue.subtract(initialActualRevenue != null ? initialActualRevenue : BigDecimal.ZERO);
        BigDecimal diffExpected = expectedRevenue.subtract(initialExpectedRevenue != null ? initialExpectedRevenue : BigDecimal.ZERO);
        BigDecimal diffRefunded = refundedRevenue.subtract(initialRefundedRevenue != null ? initialRefundedRevenue : BigDecimal.ZERO);
        BigDecimal diffPending = pendingRefund.subtract(initialPendingRefund != null ? initialPendingRefund : BigDecimal.ZERO);

        // Online paid active 460k + active COD 260k = 720k.
        assertEquals(0, BigDecimal.valueOf(720000).compareTo(diffExpected), "Projected revenue does not match expected");

        // Gross = 200k + 250k + 400k + 500k + 275k = 1.625k.
        assertEquals(0, BigDecimal.valueOf(1625000).compareTo(diffGross), "Gross revenue does not match expected");

        // Actual = 200k + 250k + 275k pending refund (chưa completed) = 725k.
        // Hai order completed refund bị loại, không bị trừ thêm 700k.
        assertEquals(0, BigDecimal.valueOf(725000).compareTo(diffActual), "Actual revenue does not match expected");

        // Refunded Revenue must use actual REFUND_SUCCESS amounts, not order totals.
        assertEquals(0, BigDecimal.valueOf(700000).compareTo(diffRefunded), "Refunded revenue does not match expected");

        assertEquals(0, BigDecimal.valueOf(275000).compareTo(diffPending), "Pending refund does not match expected");

        Map<?, ?> growth = (Map<?, ?>) stats.get("growth");
        assertNotNull(growth, "Previous-period comparison must be available");
        assertTrue(growth.containsKey("revenue"), "Actual revenue must retain previous-period comparison");

        // Verify chart values sum matches Actual Revenue
        List<BigDecimal> chartValues = (List<BigDecimal>) stats.get("chartValues");
        BigDecimal chartSum = chartValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, actualRevenue.compareTo(chartSum), "Chart sum does not match actual revenue");
    }
}
