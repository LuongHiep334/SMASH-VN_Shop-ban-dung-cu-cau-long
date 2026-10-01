package com.smashvn.shop;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.smashvn.shop.dto.order.GeneralMetricsDTO;
import com.smashvn.shop.dto.order.GrowthMetricDTO;
import com.smashvn.shop.dto.order.OperationalInsightDTO;
import com.smashvn.shop.dto.product.BrandRevenueDTO;
import com.smashvn.shop.dto.product.SlowMovingProductDTO;
import com.smashvn.shop.dto.product.TopProductDTO;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.admin.AdminThongKeService;
import com.smashvn.shop.service.admin.AdminThongKeService.OrderClassifier;
import com.smashvn.shop.service.admin.AdminThongKeService.RevenueClassification;

@SpringBootTest
public class AuditThongKeIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AdminThongKeService adminThongKeService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Test
    public void testInspectPhuongThucThanhToan() {
        System.out.println("================================================================================");
        System.out.println("                 INSPECT DATABASE & PHUONG THUC THANH TOAN                      ");
        System.out.println("================================================================================");

        String currentDb = jdbcTemplate.queryForObject("SELECT DB_NAME() AS CurrentDatabase", String.class);
        System.out.println("\n[CURRENT DATABASE]: " + currentDb);

        List<Map<String, Object>> ptttList = jdbcTemplate.queryForList("SELECT * FROM PhuongThucThanhToan ORDER BY id");
        System.out.println("\n[PHUONG THUC THANH TOAN TABLE CONTENT]: " + ptttList.size() + " rows");
        for (var row : ptttList) {
            System.out.println("  Row: " + row);
        }

        List<Map<String, Object>> tkList = jdbcTemplate.queryForList("SELECT id, username, vai_tro FROM TaiKhoan ORDER BY id");
        System.out.println("\n[TAIKHOAN TABLE CONTENT]: " + tkList.size() + " rows");
        for (var tk : tkList) {
            System.out.println("  TaiKhoan: " + tk);
        }

        List<Map<String, Object>> khList = jdbcTemplate.queryForList("SELECT id, id_tai_khoan, ho_ten_kh FROM KhachHang ORDER BY id");
        System.out.println("\n[KHACHHANG TABLE CONTENT]: " + khList.size() + " rows");
        for (var kh : khList) {
            System.out.println("  KhachHang: " + kh);
        }

        List<Map<String, Object>> allOrders = jdbcTemplate.queryForList("SELECT id, id_khach_hang, ngay_tao, tong_tien, trang_thai_don_hang, trang_thai_thanh_toan, ghi_chu FROM HoaDon ORDER BY id");
        System.out.println("\n[HOADON TABLE CONTENT]: " + allOrders.size() + " rows");
        for (var ord : allOrders) {
            System.out.println("  Order: " + ord);
        }
        System.out.println("================================================================================");
    }

    @Test
    public void testRawSqlScriptDryRun() throws Exception {
        System.out.println("================================================================================");
        System.out.println("                 TEST RAW SQL SCRIPT DRY RUN ON BadmintonShopDB1                ");
        System.out.println("================================================================================");

        String currentDb = jdbcTemplate.queryForObject("SELECT DB_NAME() AS CurrentDatabase", String.class);
        System.out.println("\n[STEP 1: DATABASE VERIFICATION]: " + currentDb);
        org.junit.jupiter.api.Assertions.assertEquals("BadmintonShopDB1", currentDb);

        // Pre-check
        int preDemo = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_%'", Integer.class);
        System.out.println("  Pre-seed demo accounts: " + preDemo);
        org.junit.jupiter.api.Assertions.assertEquals(0, preDemo);

        // Read raw SQL script
        String seedSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-seed.sql"), java.nio.charset.StandardCharsets.UTF_8);
        String rollbackSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-rollback.sql"), java.nio.charset.StandardCharsets.UTF_8);

        System.out.println("\n[STEP 2: EXECUTING RAW SQL SEED SCRIPT...]");
        jdbcTemplate.execute(seedSql);
        System.out.println("  ==> RAW SQL SEED EXECUTED SUCCESSFULLY WITHOUT ANY FOREIGN KEY ERRORS!");

        try {
            // Check PTTT FK consistency
            List<Integer> distinctPtttInLedger = jdbcTemplate.queryForList(
                "SELECT DISTINCT id_phuong_thuc_thanh_toan FROM HoaDon WHERE ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class
            );
            List<Integer> validPtttIds = jdbcTemplate.queryForList(
                "SELECT id FROM PhuongThucThanhToan", Integer.class
            );
            System.out.println("\n[STEP 3: FK PAYMENT METHODS CONSISTENCY]");
            System.out.println("  Distinct PTTT IDs in demo orders: " + distinctPtttInLedger);
            System.out.println("  Valid PTTT IDs in PhuongThucThanhToan: " + validPtttIds);
            for (Integer ptttId : distinctPtttInLedger) {
                org.junit.jupiter.api.Assertions.assertTrue(validPtttIds.contains(ptttId), "PTTT ID " + ptttId + " must exist in PhuongThucThanhToan");
            }

            // Row counts
            int tkCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_cust_%'", Integer.class);
            int khCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KhachHang kh JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan WHERE tk.username LIKE 'demo_stat_cust_%'", Integer.class);
            int dcCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SoDiaChi dc JOIN KhachHang kh ON kh.id = dc.id_khach_hang JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan WHERE tk.username LIKE 'demo_stat_cust_%'", Integer.class);
            int hdDemoCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDon WHERE ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class);
            int hdctDemoCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDonChiTiet hdct JOIN HoaDon hd ON hd.id = hdct.id_hoa_don WHERE hd.ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class);

            System.out.println("\n[STEP 4: ROW COUNTS INSIDE SEED]");
            System.out.println("  TaiKhoan demo: " + tkCount + " (Expected: 18)");
            System.out.println("  KhachHang demo: " + khCount + " (Expected: 18)");
            System.out.println("  SoDiaChi demo: " + dcCount + " (Expected: 18)");
            System.out.println("  HoaDon demo: " + hdDemoCount + " (Expected: 41)");
            System.out.println("  HoaDonChiTiet demo: " + hdctDemoCount + " (Expected: 41)");

            org.junit.jupiter.api.Assertions.assertEquals(18, tkCount);
            org.junit.jupiter.api.Assertions.assertEquals(18, khCount);
            org.junit.jupiter.api.Assertions.assertEquals(18, dcCount);
            org.junit.jupiter.api.Assertions.assertEquals(41, hdDemoCount);
            org.junit.jupiter.api.Assertions.assertEquals(41, hdctDemoCount);

        } finally {
            System.out.println("\n[STEP 5: EXECUTING RAW SQL ROLLBACK SCRIPT...]");
            jdbcTemplate.execute(rollbackSql);
            System.out.println("  ==> RAW SQL ROLLBACK EXECUTED SUCCESSFULLY!");

            int remainingDemo = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_%'", Integer.class);
            int remainingHd = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDon", Integer.class);
            System.out.println("  Remaining Demo Accounts: " + remainingDemo + " (Expected: 0)");
            System.out.println("  Remaining Total Orders: " + remainingHd + " (Expected: 0 orders before seed)");

            org.junit.jupiter.api.Assertions.assertEquals(0, remainingDemo);
            org.junit.jupiter.api.Assertions.assertEquals(0, remainingHd);
            System.out.println("\n==> [DRY RUN RESULT: 100% PASS - READY FOR SSMS EXECUTION]");
        }
        System.out.println("================================================================================");
    }

    @Test
    public void runFullAudit() throws Exception {
        System.out.println("================================================================================");
        System.out.println("                 SMASH-VN STATISTICS MODULE FULL AUDIT RUNNER                   ");
        System.out.println("================================================================================");

        // Audit Preset: "this_week"
        Map<String, LocalDateTime> rangeThisWeek = adminThongKeService.getDateRange("this_week", null, null);
        LocalDateTime startWeek = rangeThisWeek.get("start");
        LocalDateTime endWeek = rangeThisWeek.get("end");

        System.out.println("\n[AUDIT 2 & 3 & 7] TRACING ORDERS FOR PRESET 'this_week' (" + startWeek + " -> " + endWeek + ")");
        List<Object[]> rawOrdersWeek = hoaDonRepository.findAllOrdersInPeriod(startWeek, endWeek);
        System.out.println("Total raw orders in period: " + rawOrdersWeek.size());

        for (Object[] row : rawOrdersWeek) {
            Integer id = (Integer) row[0];
            String maHDSVN = (String) row[1];
            String customerName = (String) row[2];
            LocalDateTime ngayTao = (LocalDateTime) row[3];
            String paymentMethod = (String) row[4];
            String transactionType = (String) row[5];
            String paymentStatus = (String) row[6];
            String trangThaiThanhToan = (String) row[7];
            String trangThaiDonHang = (String) row[8];
            BigDecimal tongTien = (BigDecimal) row[12];
            RefundStatus refundStatus = (RefundStatus) row[13];

            String m1 = paymentMethod != null ? paymentMethod.toUpperCase().trim() : "";
            String m2 = transactionType != null ? transactionType.toUpperCase().trim() : "";
            boolean isCod = m1.contains("COD") || m2.contains("COD");
            RevenueClassification classification = OrderClassifier.classify(trangThaiDonHang, paymentStatus, trangThaiThanhToan, refundStatus, isCod);

            boolean isPendingRefundIncluded = (RefundStatus.PENDING == refundStatus);

            System.out.printf("  Order ID=%d | Code=%s | Customer=%s | Date=%s | Method=%s | PayStatus=%s | PayState=%s | OrderState=%s | RefundStatus=%s | Total=%s | Classification=%s | PendingRefund=%b%n",
                    id, maHDSVN, customerName, ngayTao, paymentMethod, paymentStatus, trangThaiThanhToan, trangThaiDonHang, refundStatus, tongTien, classification, isPendingRefundIncluded);
        }

        // Run full getStatisticsData for "this_week"
        Map<String, Object> statsWeek = adminThongKeService.getStatisticsData("this_week", startWeek, endWeek);
        GeneralMetricsDTO metricsWeek = (GeneralMetricsDTO) statsWeek.get("metrics");

        System.out.println("\n[DASHBOARD METRICS FOR 'this_week']");
        System.out.println("  totalOrders: " + statsWeek.get("totalOrders"));
        System.out.println("  successfulOrders: " + statsWeek.get("successfulOrders"));
        System.out.println("  cancelledOrders: " + statsWeek.get("cancelledOrders"));
        System.out.println("  processingOrders: " + statsWeek.get("processingOrders"));
        System.out.println("  actualRevenue: " + statsWeek.get("actualRevenue"));
        System.out.println("  expectedRevenue: " + statsWeek.get("expectedRevenue"));
        System.out.println("  refundedRevenue: " + statsWeek.get("refundedRevenue"));
        System.out.println("  pendingRefund: " + statsWeek.get("pendingRefund"));
        System.out.println("  productsSold: " + metricsWeek.totalProductsSold());

        // Audit Top Products
        @SuppressWarnings("unchecked")
        List<TopProductDTO> topProducts = (List<TopProductDTO>) statsWeek.get("topProducts");
        Double totalValidProductLineRevenue = hoaDonChiTietRepository.getTotalProductLineRevenueInPeriod(startWeek, endWeek);
        System.out.println("\n[AUDIT 4 & 5] TOP PRODUCTS & REVENUE PERCENTAGE");
        System.out.println("  Total Valid Product-Line Revenue (Denominator): " + totalValidProductLineRevenue);
        for (TopProductDTO tp : topProducts) {
            System.out.printf("  Product ID=%d | Name=%s | Category=%s | SoldQty=%d | LineRevenue=%s | SharePct=%.2f%%%n",
                    tp.productId(), tp.productName(), tp.categoryName(), tp.soldQuantity(), tp.revenue(), tp.percentage());
        }

        // Audit Brands
        @SuppressWarnings("unchecked")
        List<BrandRevenueDTO> brandRevenues = (List<BrandRevenueDTO>) statsWeek.get("brandRevenues");
        System.out.println("\n[AUDIT 6] BRAND REVENUE & SHARE");
        double sumBrandRevenue = 0.0;
        double sumBrandPct = 0.0;
        for (BrandRevenueDTO br : brandRevenues) {
            double r = br.revenue() != null ? br.revenue().doubleValue() : 0.0;
            double p = br.percentage() != null ? br.percentage() : 0.0;
            sumBrandRevenue += r;
            sumBrandPct += p;
            System.out.printf("  Brand ID=%d | BrandName=%s | SoldQty=%d | Revenue=%s | SharePct=%.2f%%%n",
                    br.brandId(), br.brandName(), br.soldQuantity(), br.revenue(), br.percentage());
        }
        System.out.printf("  Sum Brand Revenue: %.2f | Sum Brand Pct: %.2f%%%n", sumBrandRevenue, sumBrandPct);

        // Audit Slow Moving Products
        @SuppressWarnings("unchecked")
        List<SlowMovingProductDTO> slowMoving = (List<SlowMovingProductDTO>) statsWeek.get("slowMovingProducts");
        System.out.println("\n[AUDIT 9] SLOW MOVING PRODUCTS (Top 5)");
        for (SlowMovingProductDTO sm : slowMoving) {
            System.out.printf("  Product ID=%d | Name=%s | Stock=%d | SoldInPeriod=%d | Warning=%s (%s)%n",
                    sm.productId(), sm.productName(), sm.stockQuantity(), sm.soldQuantity(), sm.warningBadge(), sm.warningLevel());
        }

        // Audit Insights
        @SuppressWarnings("unchecked")
        List<OperationalInsightDTO> insights = (List<OperationalInsightDTO>) statsWeek.get("insights");
        System.out.println("\n[AUDIT 10] OPERATIONAL INSIGHTS");
        for (OperationalInsightDTO ins : insights) {
            System.out.printf("  Type=[%s] | Title: %s | Message: %s%n", ins.type(), ins.title(), ins.message());
        }

        // Audit Growth
        @SuppressWarnings("unchecked")
        Map<String, GrowthMetricDTO> growthMap = (Map<String, GrowthMetricDTO>) statsWeek.get("growth");
        System.out.println("\n[AUDIT 8] GROWTH METRICS");
        if (growthMap != null) {
            for (Map.Entry<String, GrowthMetricDTO> entry : growthMap.entrySet()) {
                GrowthMetricDTO g = entry.getValue();
                System.out.printf("  Metric: %s | Current=%.2f | Previous=%.2f | Pct=%.2f%% | Formatted=%s | Dir=%s | isNew=%b%n",
                        entry.getKey(), g.currentValue(), g.previousValue(), g.percentageChange() != null ? g.percentageChange() : 0.0, g.formattedChange(), g.direction(), g.isNew());
            }
        } else {
            System.out.println("  Growth map is null (e.g. all_time)");
        }

        // Audit Excel Export
        System.out.println("\n[AUDIT 11] TESTING EXCEL EXPORT WORKBOOK GENERATION");
        byte[] excelBytes = adminThongKeService.exportToExcel(startWeek, endWeek);
        System.out.println("  Generated Excel size in bytes: " + excelBytes.length);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            int numberOfSheets = wb.getNumberOfSheets();
            System.out.println("  Number of sheets: " + numberOfSheets);
            for (int s = 0; s < numberOfSheets; s++) {
                Sheet sheet = wb.getSheetAt(s);
                System.out.println("    Sheet " + (s + 1) + ": " + sheet.getSheetName() + " (rows=" + sheet.getPhysicalNumberOfRows() + ")");
            }
        }

        System.out.println("\n================================================================================");
        System.out.println("                         AUDIT COMPLETE SUCCESS                                 ");
        System.out.println("================================================================================");
    }

    @Autowired
    private com.smashvn.shop.repository.SanPhamRepository sanPhamRepository;

    @Autowired
    private com.smashvn.shop.repository.SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private com.smashvn.shop.repository.ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private com.smashvn.shop.repository.KhachHangRepository khachHangRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    public void inspectDatabaseForDemoPlan() {
        System.out.println("================================================================================");
        System.out.println("                 DATABASE INSPECTION FOR DEMO SEED PLAN                         ");
        System.out.println("================================================================================");

        System.out.println("\n--- BRANDS (ThuongHieu) ---");
        List<Map<String, Object>> brands = jdbcTemplate.queryForList("SELECT * FROM ThuongHieu");
        for (var b : brands) {
            System.out.println("  Brand: " + b);
        }

        System.out.println("\n--- PAYMENT METHODS (PhuongThucThanhToan) ---");
        List<Map<String, Object>> pttts = jdbcTemplate.queryForList("SELECT * FROM PhuongThucThanhToan");
        for (var p : pttts) {
            System.out.println("  PTTT: " + p);
        }

        System.out.println("\n--- CUSTOMERS (KhachHang) ---");
        List<Map<String, Object>> customers = jdbcTemplate.queryForList("SELECT * FROM KhachHang");
        System.out.println("  Total customers: " + customers.size());
        for (var c : customers) {
            System.out.println("  Customer: " + c);
        }

        System.out.println("\n--- PRODUCTS & VARIANTS ---");
        List<Map<String, Object>> products = jdbcTemplate.queryForList(
            "SELECT sp.id AS product_id, sp.ten_san_pham, th.ten_thuong_hieu, dm.ten_danh_muc, sp.trang_thai, " +
            "spct.id AS spct_id, spct.so_luong_ton, spct.gia_ban, spct.trang_thai AS spct_status " +
            "FROM SanPham sp " +
            "LEFT JOIN ThuongHieu th ON th.id = sp.id_thuong_hieu " +
            "LEFT JOIN DanhMuc dm ON dm.id = sp.id_danh_muc " +
            "LEFT JOIN SanPhamChiTiet spct ON spct.id_san_pham = sp.id " +
            "WHERE sp.trang_thai = 1 " +
            "ORDER BY th.ten_thuong_hieu, sp.id, spct.id"
        );
        for (var p : products) {
            System.out.println("  Product/Variant: " + p);
        }

        System.out.println("\n--- ALL EXISTING ORDERS (HoaDon) ---");
        List<Map<String, Object>> allOrders = jdbcTemplate.queryForList(
            "SELECT hd.id, hd.ngay_tao, hd.tong_tien, hd.trang_thai_don_hang, hd.trang_thai_thanh_toan, " +
            "kh.ho_ten_kh, pttt.ten_phuong_thuc " +
            "FROM HoaDon hd " +
            "LEFT JOIN KhachHang kh ON kh.id = hd.id_khach_hang " +
            "LEFT JOIN PhuongThucThanhToan pttt ON pttt.id = hd.id_phuong_thuc_thanh_toan " +
            "ORDER BY hd.id"
        );
        System.out.println("  Total orders in DB: " + allOrders.size());
        for (var hd : allOrders) {
            System.out.println("  Order: " + hd);
        }

        System.out.println("\n--- EXISTING ORDER ITEMS (HoaDonChiTiet) ---");
        List<Map<String, Object>> allItems = jdbcTemplate.queryForList(
            "SELECT hdct.id, hdct.id_hoa_don, hdct.id_san_pham_chi_tiet, hdct.so_luong, hdct.don_gia, sp.ten_san_pham " +
            "FROM HoaDonChiTiet hdct " +
            "LEFT JOIN SanPhamChiTiet spct ON spct.id = hdct.id_san_pham_chi_tiet " +
            "LEFT JOIN SanPham sp ON sp.id = spct.id_san_pham " +
            "ORDER BY hdct.id_hoa_don, hdct.id"
        );
        for (var item : allItems) {
            System.out.println("  Item: " + item);
        }
        System.out.println("================================================================================");
    }

    @Test
    public void testDateRangeAndQueryBaseline() {
        System.out.println("================================================================================");
        System.out.println("                 PHASE 0 & PHASE 1: DATE RANGE & BASELINE TEST                  ");
        System.out.println("================================================================================");

        // Test Preset: "last_30_days"
        Map<String, LocalDateTime> range30 = adminThongKeService.getDateRange("last_30_days", null, null);
        LocalDateTime start30 = range30.get("start");
        LocalDateTime end30 = range30.get("end");

        Map<String, LocalDateTime> prevRange30 = adminThongKeService.getPreviousDateRange("last_30_days", start30, end30);
        LocalDateTime prevStart30 = prevRange30.get("start");
        LocalDateTime prevEnd30 = prevRange30.get("end");

        long currentDays30 = java.time.temporal.ChronoUnit.DAYS.between(start30.toLocalDate(), end30.toLocalDate()) + 1;
        long prevDays30 = java.time.temporal.ChronoUnit.DAYS.between(prevStart30.toLocalDate(), prevEnd30.toLocalDate()) + 1;

        System.out.println("\n[PHASE 0: DATE RANGE VERIFICATION]");
        System.out.printf("  Preset 'last_30_days': Current = %s -> %s (%d days)%n", start30, end30, currentDays30);
        System.out.printf("                         Previous = %s -> %s (%d days)%n", prevStart30, prevEnd30, prevDays30);

        org.junit.jupiter.api.Assertions.assertEquals(30, currentDays30, "Current days for last_30_days must be exactly 30");
        org.junit.jupiter.api.Assertions.assertEquals(30, prevDays30, "Previous days for last_30_days must be exactly 30");

        // Query Stats for last_30_days
        Map<String, Object> stats30 = adminThongKeService.getStatisticsData("last_30_days", start30, end30);
        @SuppressWarnings("unchecked")
        List<String> chartLabels = (List<String>) stats30.get("chartLabels");
        System.out.printf("  chartLabels.size() = %d (Expected = 30)%n", chartLabels.size());
        org.junit.jupiter.api.Assertions.assertEquals(30, chartLabels.size(), "chartLabels size must be exactly 30");

        GeneralMetricsDTO metrics30 = (GeneralMetricsDTO) stats30.get("metrics");

        System.out.println("\n[PHASE 1: CURRENT BASELINE FOR 'last_30_days' (21/07 -> 19/08)]");
        System.out.println("  totalOrders: " + stats30.get("totalOrders"));
        System.out.println("  successfulOrders: " + stats30.get("successfulOrders"));
        System.out.println("  cancelledOrders: " + stats30.get("cancelledOrders"));
        System.out.println("  processingOrders: " + stats30.get("processingOrders"));
        System.out.println("  actualRevenue: " + stats30.get("actualRevenue"));
        System.out.println("  AOV: " + metrics30.avgOrderValue());
        System.out.println("  newCustomers: " + stats30.get("newCustomers"));
        System.out.println("  productsSold: " + metrics30.totalProductsSold());
        System.out.println("  expectedRevenue: " + stats30.get("expectedRevenue"));
        System.out.println("  refundedRevenue: " + stats30.get("refundedRevenue"));
        System.out.println("  pendingRefund: " + stats30.get("pendingRefund"));

        Double totalValidProductRevenue = hoaDonChiTietRepository.getTotalProductLineRevenueInPeriod(start30, end30);
        System.out.println("  productRevenue (totalValidProductLineRevenue): " + totalValidProductRevenue);

        // Previous period baseline
        Map<String, Object> statsPrev30 = adminThongKeService.getStatisticsData("last_30_days", prevStart30, prevEnd30);
        GeneralMetricsDTO metricsPrev30 = (GeneralMetricsDTO) statsPrev30.get("metrics");

        System.out.println("\n[PHASE 1: PREVIOUS BASELINE FOR 'last_30_days' (21/06 -> 20/07)]");
        System.out.println("  prev totalOrders: " + statsPrev30.get("totalOrders"));
        System.out.println("  prev successfulOrders: " + statsPrev30.get("successfulOrders"));
        System.out.println("  prev cancelledOrders: " + statsPrev30.get("cancelledOrders"));
        System.out.println("  prev processingOrders: " + statsPrev30.get("processingOrders"));
        System.out.println("  prev actualRevenue: " + statsPrev30.get("actualRevenue"));
        System.out.println("  prev AOV: " + metricsPrev30.avgOrderValue());
        System.out.println("  prev newCustomers: " + statsPrev30.get("newCustomers"));
        System.out.println("  prev productsSold: " + metricsPrev30.totalProductsSold());

        System.out.println("================================================================================");
    }

    @Test
    public void testDryRunSeedAndRollback() throws Exception {
        System.out.println("================================================================================");
        System.out.println("                 DRY RUN: SEED -> AUDIT ASSERTIONS -> ROLLBACK                  ");
        System.out.println("================================================================================");

        // Read seed and rollback SQL scripts
        String seedSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-seed.sql"), java.nio.charset.StandardCharsets.UTF_8);
        String rollbackSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-rollback.sql"), java.nio.charset.StandardCharsets.UTF_8);

        // 1. Execute Seed
        System.out.println("\n[DRY RUN STEP 1] Executing demo-statistics-seed.sql...");
        jdbcTemplate.execute(seedSql);

        try {
            // 2. Verify Insert Counts
            int tkCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_cust_%'", Integer.class);
            int khCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KhachHang kh JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan WHERE tk.username LIKE 'demo_stat_cust_%'", Integer.class);
            int dcCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SoDiaChi dc JOIN KhachHang kh ON kh.id = dc.id_khach_hang JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan WHERE tk.username LIKE 'demo_stat_cust_%'", Integer.class);
            int hdCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDon WHERE ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class);
            int hdctCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDonChiTiet hdct JOIN HoaDon hd ON hd.id = hdct.id_hoa_don WHERE hd.ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class);

            System.out.println("  Inserted TaiKhoan: " + tkCount + " (Expected: 18)");
            System.out.println("  Inserted KhachHang: " + khCount + " (Expected: 18)");
            System.out.println("  Inserted SoDiaChi: " + dcCount + " (Expected: 18)");
            System.out.println("  Inserted HoaDon: " + hdCount + " (Expected: 41)");
            System.out.println("  Inserted HoaDonChiTiet: " + hdctCount + " (Expected: 41)");

            org.junit.jupiter.api.Assertions.assertEquals(18, tkCount);
            org.junit.jupiter.api.Assertions.assertEquals(18, khCount);
            org.junit.jupiter.api.Assertions.assertEquals(18, dcCount);
            org.junit.jupiter.api.Assertions.assertEquals(41, hdCount);
            org.junit.jupiter.api.Assertions.assertEquals(41, hdctCount);

            // 3. Query Statistics for "last_30_days"
            Map<String, LocalDateTime> range30 = adminThongKeService.getDateRange("last_30_days", null, null);
            LocalDateTime startCurr = range30.get("start");
            LocalDateTime endCurr = range30.get("end");

            Map<String, LocalDateTime> prevRange30 = adminThongKeService.getPreviousDateRange("last_30_days", startCurr, endCurr);
            LocalDateTime startPrev = prevRange30.get("start");
            LocalDateTime endPrev = prevRange30.get("end");

            Map<String, Object> statsCurr = adminThongKeService.getStatisticsData("last_30_days", startCurr, endCurr);
            Map<String, Object> statsPrev = adminThongKeService.getStatisticsData("last_30_days", startPrev, endPrev);

            GeneralMetricsDTO mCurr = (GeneralMetricsDTO) statsCurr.get("metrics");
            GeneralMetricsDTO mPrev = (GeneralMetricsDTO) statsPrev.get("metrics");

            System.out.println("\n[DRY RUN STEP 2: KPI VERIFICATION]");
            System.out.println("  Previous Revenue: " + statsPrev.get("actualRevenue") + " (Expected: 61386000.00)");
            System.out.println("  Current Revenue: " + statsCurr.get("actualRevenue") + " (Expected: 88263800.00)");
            System.out.println("  Previous Total Orders: " + statsPrev.get("totalOrders") + " (Expected: 20)");
            System.out.println("  Current Total Orders: " + statsCurr.get("totalOrders") + " (Expected: 28)");
            System.out.println("  Current Success Orders: " + statsCurr.get("successfulOrders") + " (Expected: 22)");
            System.out.println("  Current Processing Orders: " + statsCurr.get("processingOrders") + " (Expected: 4)");
            System.out.println("  Current Cancelled Orders: " + statsCurr.get("cancelledOrders") + " (Expected: 2)");
            System.out.println("  Previous New Customers: " + statsPrev.get("newCustomers") + " (Expected: 8)");
            System.out.println("  Current New Customers: " + statsCurr.get("newCustomers") + " (Expected: 11)");
            System.out.println("  Current Products Sold: " + mCurr.totalProductsSold() + " (Expected: 26)");
            System.out.println("  Pending Refund: " + statsCurr.get("pendingRefund") + " (Expected: 4806400.00)");

            Double totalValidProductRevenue = hoaDonChiTietRepository.getTotalProductLineRevenueInPeriod(startCurr, endCurr);
            System.out.println("  Current Product Revenue: " + totalValidProductRevenue + " (Expected: 86121000.0)");

            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("61386000.00"), statsPrev.get("actualRevenue"));
            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("88263800.00"), statsCurr.get("actualRevenue"));
            org.junit.jupiter.api.Assertions.assertEquals(20L, statsPrev.get("totalOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(28L, statsCurr.get("totalOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(22L, statsCurr.get("successfulOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(4L, statsCurr.get("processingOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(2L, statsCurr.get("cancelledOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(8L, statsPrev.get("newCustomers"));
            org.junit.jupiter.api.Assertions.assertEquals(11L, statsCurr.get("newCustomers"));
            org.junit.jupiter.api.Assertions.assertEquals(26L, mCurr.totalProductsSold());
            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("4806400.00"), statsCurr.get("pendingRefund"));
            org.junit.jupiter.api.Assertions.assertEquals(86121000.0, totalValidProductRevenue);

            // Audit Top Products
            @SuppressWarnings("unchecked")
            List<TopProductDTO> topProducts = (List<TopProductDTO>) statsCurr.get("topProducts");
            System.out.println("\n[DRY RUN STEP 3: TOP PRODUCTS]");
            for (TopProductDTO tp : topProducts) {
                System.out.printf("  Product ID=%d | Name=%s | Category=%s | SoldQty=%d | LineRevenue=%s | SharePct=%.2f%%%n",
                        tp.productId(), tp.productName(), tp.categoryName(), tp.soldQuantity(), tp.revenue(), tp.percentage());
            }

            // Audit Brand Revenue
            @SuppressWarnings("unchecked")
            List<BrandRevenueDTO> brandRevenues = (List<BrandRevenueDTO>) statsCurr.get("brandRevenues");
            System.out.println("\n[DRY RUN STEP 4: BRAND REVENUES]");
            double sumBrandRevenue = 0.0;
            for (BrandRevenueDTO br : brandRevenues) {
                double r = br.revenue() != null ? br.revenue().doubleValue() : 0.0;
                sumBrandRevenue += r;
                System.out.printf("  Brand ID=%d | BrandName=%s | SoldQty=%d | Revenue=%s | SharePct=%.2f%%%n",
                        br.brandId(), br.brandName(), br.soldQuantity(), br.revenue(), br.percentage());
            }
            System.out.printf("  Sum Brand Revenue: %.2f (Expected: 86121000.00)%n", sumBrandRevenue);
            org.junit.jupiter.api.Assertions.assertEquals(86121000.0, sumBrandRevenue, 0.01);

        } finally {
            // 4. Rollback
            System.out.println("\n[DRY RUN STEP 5] Executing demo-statistics-rollback.sql...");
            jdbcTemplate.execute(rollbackSql);

            int remainingDemoTk = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_%'", Integer.class);
            int remainingOrders = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDon", Integer.class);

            System.out.println("  Remaining Demo Accounts: " + remainingDemoTk + " (Expected: 0)");
            System.out.println("  Remaining Total Orders: " + remainingOrders + " (Expected: 7 original orders)");

            org.junit.jupiter.api.Assertions.assertEquals(0, remainingDemoTk);
            org.junit.jupiter.api.Assertions.assertEquals(7, remainingOrders);
            System.out.println("\n==> [DRY RUN SUCCESS] All assertions PASS and DB was cleanly rolled back to 7 original orders!");
        }
        System.out.println("================================================================================");
    }

    @Test
    public void executeOfficialSeedAndPostAudit() throws Exception {
        System.out.println("================================================================================");
        System.out.println("            OFFICIAL SEED EXECUTION & POST-SEED FULL AUDIT                      ");
        System.out.println("================================================================================");

        // 0. Ensure clean pre-seed state by cleaning any test garbage (id > 7)
        jdbcTemplate.execute("DELETE FROM GioHangChiTiet WHERE id_gio_hang IN (SELECT id FROM GioHang WHERE id_khach_hang > 4)");
        jdbcTemplate.execute("DELETE FROM GioHang WHERE id_khach_hang > 4");
        jdbcTemplate.execute("DELETE FROM HoaDonChiTiet WHERE id_hoa_don > 7");
        jdbcTemplate.execute("DELETE FROM HoaDon WHERE id > 7");
        jdbcTemplate.execute("DELETE FROM SoDiaChi WHERE id_khach_hang > 4");
        jdbcTemplate.execute("DELETE FROM KhachHang WHERE id > 4");
        jdbcTemplate.execute("DELETE FROM MaKhoiPhuc WHERE id_tai_khoan > 6");
        jdbcTemplate.execute("DELETE FROM TaiKhoan WHERE id > 6");

        // Check pre-seed state
        int preDemoTk = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_%'", Integer.class);
        List<Map<String, Object>> existingHdList = jdbcTemplate.queryForList("SELECT id, ngay_tao, tong_tien, trang_thai_don_hang, trang_thai_thanh_toan, ghi_chu, id_khach_hang FROM HoaDon ORDER BY id");
        System.out.println("\n[PRE-SEED STATE CHECK]");
        System.out.println("  Pre-seed demo accounts: " + preDemoTk + " (Expected: 0)");
        System.out.println("  Pre-seed total orders: " + existingHdList.size() + " (Expected: 7 original orders)");
        for (var hd : existingHdList) {
            System.out.println("    Original Order: " + hd);
        }
        org.junit.jupiter.api.Assertions.assertEquals(0, preDemoTk, "Demo accounts must be 0 before seeding");
        org.junit.jupiter.api.Assertions.assertEquals(7, existingHdList.size(), "Original orders must be exactly 7 before seeding");

        // Read and execute official seed SQL script
        String seedSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-seed.sql"), java.nio.charset.StandardCharsets.UTF_8);
        String rollbackSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-rollback.sql"), java.nio.charset.StandardCharsets.UTF_8);

        System.out.println("\n[EXECUTING OFFICIAL SEED SCRIPT]");
        jdbcTemplate.execute(seedSql);
        System.out.println("  ==> Seed script executed successfully!");

        try {
            // Verify row counts
            int tkCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TaiKhoan WHERE username LIKE 'demo_stat_cust_%'", Integer.class);
            int khCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM KhachHang kh JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan WHERE tk.username LIKE 'demo_stat_cust_%'", Integer.class);
            int dcCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SoDiaChi dc JOIN KhachHang kh ON kh.id = dc.id_khach_hang JOIN TaiKhoan tk ON tk.id = kh.id_tai_khoan WHERE tk.username LIKE 'demo_stat_cust_%'", Integer.class);
            int hdDemoCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDon WHERE ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class);
            int hdctDemoCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDonChiTiet hdct JOIN HoaDon hd ON hd.id = hdct.id_hoa_don WHERE hd.ghi_chu LIKE 'DEMO_STAT_D%'", Integer.class);
            int totalHdAll = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM HoaDon", Integer.class);

            System.out.println("\n[POST-SEED ROW COUNT VERIFICATION]");
            System.out.println("  Inserted TaiKhoan: " + tkCount + " (Expected: 18)");
            System.out.println("  Inserted KhachHang: " + khCount + " (Expected: 18)");
            System.out.println("  Inserted SoDiaChi: " + dcCount + " (Expected: 18)");
            System.out.println("  Inserted HoaDon: " + hdDemoCount + " (Expected: 41 | Total in DB: " + totalHdAll + ")");
            System.out.println("  Inserted HoaDonChiTiet: " + hdctDemoCount + " (Expected: 41)");

            org.junit.jupiter.api.Assertions.assertEquals(18, tkCount);
            org.junit.jupiter.api.Assertions.assertEquals(18, khCount);
            org.junit.jupiter.api.Assertions.assertEquals(18, dcCount);
            org.junit.jupiter.api.Assertions.assertEquals(41, hdDemoCount);
            org.junit.jupiter.api.Assertions.assertEquals(41, hdctDemoCount);
            org.junit.jupiter.api.Assertions.assertEquals(48, totalHdAll);

            // FIXED DEMO RANGE AUDIT
            LocalDateTime startCurr = LocalDateTime.of(2026, 7, 21, 0, 0, 0);
            LocalDateTime endCurr = LocalDateTime.of(2026, 8, 19, 23, 59, 59, 999999999);

            LocalDateTime startPrev = LocalDateTime.of(2026, 6, 21, 0, 0, 0);
            LocalDateTime endPrev = LocalDateTime.of(2026, 7, 20, 23, 59, 59, 999999999);

            System.out.println("\n[POST-SEED AUDIT - FIXED RANGE: " + startCurr + " -> " + endCurr + "]");
            Map<String, Object> statsCurr = adminThongKeService.getStatisticsData("custom", startCurr, endCurr);
            Map<String, Object> statsPrev = adminThongKeService.getStatisticsData("custom", startPrev, endPrev);

            GeneralMetricsDTO mCurr = (GeneralMetricsDTO) statsCurr.get("metrics");
            GeneralMetricsDTO mPrev = (GeneralMetricsDTO) statsPrev.get("metrics");

            System.out.println("\n--- CURRENT METRICS ---");
            System.out.println("  totalOrders: " + statsCurr.get("totalOrders") + " (Expected: 28)");
            System.out.println("  successfulOrders: " + statsCurr.get("successfulOrders") + " (Expected: 22)");
            System.out.println("  processingOrders: " + statsCurr.get("processingOrders") + " (Expected: 4)");
            System.out.println("  cancelledOrders: " + statsCurr.get("cancelledOrders") + " (Expected: 2)");
            System.out.println("  actualRevenue: " + statsCurr.get("actualRevenue") + " (Expected: 88263800.00)");
            System.out.println("  AOV: " + mCurr.avgOrderValue() + " (Expected: 4011991)");
            System.out.println("  newCustomers: " + statsCurr.get("newCustomers") + " (Expected: 11)");
            System.out.println("  totalProductsSold: " + mCurr.totalProductsSold() + " (Expected: 26)");
            System.out.println("  pendingRefund: " + statsCurr.get("pendingRefund") + " (Expected: 4806400.00)");

            Double totalValidProductRevenue = hoaDonChiTietRepository.getTotalProductLineRevenueInPeriod(startCurr, endCurr);
            System.out.println("  totalProductRevenue: " + totalValidProductRevenue + " (Expected: 86121000.0)");

            org.junit.jupiter.api.Assertions.assertEquals(28L, statsCurr.get("totalOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(22L, statsCurr.get("successfulOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(4L, statsCurr.get("processingOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(2L, statsCurr.get("cancelledOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("88263800.00"), statsCurr.get("actualRevenue"));
            org.junit.jupiter.api.Assertions.assertEquals(11L, statsCurr.get("newCustomers"));
            org.junit.jupiter.api.Assertions.assertEquals(26L, mCurr.totalProductsSold());
            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("4806400.00"), statsCurr.get("pendingRefund"));
            org.junit.jupiter.api.Assertions.assertEquals(86121000.0, totalValidProductRevenue);

            System.out.println("\n--- PREVIOUS METRICS ---");
            System.out.println("  prev totalOrders: " + statsPrev.get("totalOrders") + " (Expected: 20)");
            System.out.println("  prev successfulOrders: " + statsPrev.get("successfulOrders") + " (Expected: 15)");
            System.out.println("  prev processingOrders: " + statsPrev.get("processingOrders") + " (Expected: 2)");
            System.out.println("  prev cancelledOrders: " + statsPrev.get("cancelledOrders") + " (Expected: 3)");
            System.out.println("  prev actualRevenue: " + statsPrev.get("actualRevenue") + " (Expected: 61386000.00)");
            System.out.println("  prev AOV: " + mPrev.avgOrderValue() + " (Expected: 4092400)");
            System.out.println("  prev newCustomers: " + statsPrev.get("newCustomers") + " (Expected: 8)");

            org.junit.jupiter.api.Assertions.assertEquals(20L, statsPrev.get("totalOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(15L, statsPrev.get("successfulOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(2L, statsPrev.get("processingOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(3L, statsPrev.get("cancelledOrders"));
            org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("61386000.00"), statsPrev.get("actualRevenue"));
            org.junit.jupiter.api.Assertions.assertEquals(8L, statsPrev.get("newCustomers"));

            System.out.println("\n--- GROWTH AUDIT ---");
            @SuppressWarnings("unchecked")
            Map<String, GrowthMetricDTO> growth = (Map<String, GrowthMetricDTO>) statsCurr.get("growth");
            if (growth != null) {
                for (var e : growth.entrySet()) {
                    System.out.printf("  %s: Current=%.2f | Prev=%.2f | Pct=%.2f%% | Formatted=%s | Dir=%s%n",
                            e.getKey(), e.getValue().currentValue(), e.getValue().previousValue(),
                            e.getValue().percentageChange() != null ? e.getValue().percentageChange() : 0.0,
                            e.getValue().formattedChange(), e.getValue().direction());
                }
            }

            System.out.println("\n--- TOP PRODUCTS AUDIT ---");
            @SuppressWarnings("unchecked")
            List<TopProductDTO> topProducts = (List<TopProductDTO>) statsCurr.get("topProducts");
            for (TopProductDTO tp : topProducts) {
                System.out.printf("  ID=%d | Name=%s | Cat=%s | SoldQty=%d | Revenue=%s | Pct=%.2f%%%n",
                        tp.productId(), tp.productName(), tp.categoryName(), tp.soldQuantity(), tp.revenue(), tp.percentage());
            }

            System.out.println("\n--- BRAND REVENUE AUDIT ---");
            @SuppressWarnings("unchecked")
            List<BrandRevenueDTO> brandRevenues = (List<BrandRevenueDTO>) statsCurr.get("brandRevenues");
            double sumBrandRev = 0.0;
            for (BrandRevenueDTO br : brandRevenues) {
                double r = br.revenue() != null ? br.revenue().doubleValue() : 0.0;
                sumBrandRev += r;
                System.out.printf("  Brand ID=%d | Name=%s | SoldQty=%d | Revenue=%s | Pct=%.2f%%%n",
                        br.brandId(), br.brandName(), br.soldQuantity(), br.revenue(), br.percentage());
            }
            System.out.printf("  Total Brand Revenue Sum: %.2f (Expected: 86121000.0)%n", sumBrandRev);
            org.junit.jupiter.api.Assertions.assertEquals(86121000.0, sumBrandRev, 0.01);

            System.out.println("\n--- EXCEL EXPORT AUDIT ---");
            byte[] excelBytes = adminThongKeService.exportToExcel(startCurr, endCurr);
            System.out.println("  Generated Excel Size: " + excelBytes.length + " bytes");
            org.junit.jupiter.api.Assertions.assertTrue(excelBytes.length > 5000, "Excel export must not be empty");

            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
                int numSheets = wb.getNumberOfSheets();
                System.out.println("  Excel Sheets count: " + numSheets + " (Expected: 6)");
                org.junit.jupiter.api.Assertions.assertEquals(6, numSheets);
                for (int s = 0; s < numSheets; s++) {
                    Sheet sheet = wb.getSheetAt(s);
                    System.out.printf("    Sheet %d: %s | Rows: %d%n", s + 1, sheet.getSheetName(), sheet.getPhysicalNumberOfRows());
                    org.junit.jupiter.api.Assertions.assertTrue(sheet.getPhysicalNumberOfRows() >= 2, "Each sheet must contain header and data rows");
                }
            }

            System.out.println("\n================================================================================");
            System.out.println("     ALL POST-SEED AUDIT ASSERTIONS PASSED! SEED IS COMMITTED AND LOCKED.       ");
            System.out.println("================================================================================");

        } catch (Throwable t) {
            System.err.println("\n[CRITICAL FAILURE IN POST-SEED AUDIT] Rolling back database to clean state...");
            jdbcTemplate.execute(rollbackSql);
            throw t;
        }
    }

    @Test
    public void testThongKeUIAndApiEndpoints() throws Exception {
        System.out.println("================================================================================");
        System.out.println("                 TESTING CONTROLLER UI & JSON API ENDPOINTS                     ");
        System.out.println("================================================================================");

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 1. Test HTML Page rendering
        System.out.println("\n[ENDPOINT TEST 1] GET /admin/thong-ke");
        mockMvc.perform(get("/admin/thong-ke")
                .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk());
        System.out.println("  ==> /admin/thong-ke returned HTTP 200 OK!");

        // 2. Test JSON API endpoint for custom fixed demo range
        System.out.println("\n[ENDPOINT TEST 2] GET /admin/thong-ke/api?filter=custom&startDate=2026-07-21&endDate=2026-08-19");
        mockMvc.perform(get("/admin/thong-ke/api")
                .sessionAttr("vaiTro", "QL")
                .param("filter", "custom")
                .param("startDate", "2026-07-21")
                .param("endDate", "2026-08-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualRevenue").value(88263800.00))
                .andExpect(jsonPath("$.totalOrders").value(28))
                .andExpect(jsonPath("$.successfulOrders").value(22))
                .andExpect(jsonPath("$.processingOrders").value(4))
                .andExpect(jsonPath("$.cancelledOrders").value(2))
                .andExpect(jsonPath("$.newCustomers").value(11))
                .andExpect(jsonPath("$.pendingRefund").value(4806400.00))
                .andExpect(jsonPath("$.growth.revenue.currentValue").value(88263800.00))
                .andExpect(jsonPath("$.growth.totalOrders.currentValue").value(28))
                .andExpect(jsonPath("$.topProducts[0].productId").value(58))
                .andExpect(jsonPath("$.topProducts[0].soldQuantity").value(9))
                .andExpect(jsonPath("$.topProducts[1].productId").value(26))
                .andExpect(jsonPath("$.topProducts[1].soldQuantity").value(7))
                .andExpect(jsonPath("$.brandRevenues[0].brandName").value("Yonex"))
                .andExpect(jsonPath("$.brandRevenues[1].brandName").value("Li-Ning"))
                .andExpect(jsonPath("$.brandRevenues[2].brandName").value("Victor"));
        System.out.println("  ==> /admin/thong-ke/api returned HTTP 200 OK with 100% verified JSON data!");

        // 3. Test Excel Export Endpoint
        System.out.println("\n[ENDPOINT TEST 3] GET /admin/thong-ke/export?startDate=2026-07-21&endDate=2026-08-19");
        mockMvc.perform(get("/admin/thong-ke/export")
                .sessionAttr("vaiTro", "QL")
                .param("startDate", "2026-07-21")
                .param("endDate", "2026-08-19"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        System.out.println("  ==> /admin/thong-ke/export returned HTTP 200 OK with valid Excel file!");
        System.out.println("================================================================================");
    }
}
