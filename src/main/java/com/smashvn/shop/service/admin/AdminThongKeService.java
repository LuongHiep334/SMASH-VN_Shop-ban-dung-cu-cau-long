package com.smashvn.shop.service.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.stream.Collectors;

import com.smashvn.shop.dto.order.GeneralMetricsDTO;
import com.smashvn.shop.dto.order.GrowthMetricDTO;
import com.smashvn.shop.dto.order.OperationalInsightDTO;
import com.smashvn.shop.dto.product.BrandRevenueDTO;
import com.smashvn.shop.dto.product.SlowMovingProductDTO;
import com.smashvn.shop.dto.product.TopProductDTO;
import com.smashvn.shop.dto.payment.TransactionHistoryDTO;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.repository.SanPhamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminThongKeService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SanPhamRepository sanPhamRepository;

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int TRANSACTION_PREVIEW_LIMIT = 5;

    // Ngưỡng cảnh báo dashboard nội bộ để phục vụ theo dõi vận hành
    private static final double INTERNAL_HIGH_CANCEL_RATE_THRESHOLD = 15.0; // 15%
    private static final double INTERNAL_REVENUE_GROWTH_THRESHOLD = 10.0;    // 10%
    private static final double INTERNAL_REVENUE_DROP_THRESHOLD = -10.0;    // -10%

    public enum RevenueClassification {
        ACTUAL_REVENUE,
        PROJECTED_REVENUE,
        EXCLUDED
    }

    public static class OrderClassifier {

        public static RevenueClassification classify(
                String trangThaiDonHang,
                String paymentStatus,
                String trangThaiThanhToan,
                RefundStatus refundStatus,
                boolean isCod) {

            String status = normalizeLower(trangThaiDonHang);

            // Doanh thu thực tế được xác định theo đóng góp của từng đơn. Đơn đã
            // hoàn tiền bị loại, không tạo một khoản âm để trừ ở bước khác.
            if (isDelivered(status)) {
                if (isRefundCompleted(paymentStatus, trangThaiThanhToan, refundStatus)) {
                    return RevenueClassification.EXCLUDED;
                }
                return RevenueClassification.ACTUAL_REVENUE;
            }

            // Đơn đang/đã hoàn tiền không còn là doanh thu dự kiến.
            if (isRefundCompleted(paymentStatus, trangThaiThanhToan, refundStatus)
                    || isRefundPending(trangThaiThanhToan, refundStatus)) {
                return RevenueClassification.EXCLUDED;
            }

            // COD đang hoạt động được dự kiến thu khi giao; online chỉ được dự
            // kiến khi đã có bằng chứng thanh toán, kể cả chưa tới bước đang giao.
            if (isActive(status) && (isCod || hasPaymentReceived(paymentStatus, trangThaiThanhToan))) {
                return RevenueClassification.PROJECTED_REVENUE;
            }

            return RevenueClassification.EXCLUDED;
        }

        static boolean isDelivered(String status) {
            String normalized = normalizeLower(status);
            return "da_giao".equals(normalized)
                    || "hoan_thanh".equals(normalized)
                    || "delivered".equals(normalized);
        }

        static boolean isActive(String status) {
            return switch (normalizeLower(status)) {
                case "cho_thanh_toan", "cho_xac_nhan", "da_xac_nhan", "dang_chuan_bi_hang",
                        "san_sang_giao", "da_tao_van_don_ghn", "da_ban_giao_ghn",
                        "dang_lay_hang", "dang_giao", "processing", "shipping" -> true;
                default -> false;
            };
        }

        static boolean isRefundCompleted(
                String paymentStatus,
                String trangThaiThanhToan,
                RefundStatus refundStatus) {
            String pStatus = normalizeUpper(paymentStatus);
            String tStatus = normalizeUpper(trangThaiThanhToan);
            return RefundStatus.COMPLETED == refundStatus
                    || "REFUNDED".equals(pStatus)
                    || "REFUNDED".equals(tStatus)
                    || "HOAN_TIEN".equals(tStatus)
                    || "DA_HOAN_TIEN".equals(tStatus);
        }

        static boolean isRefundPending(String trangThaiThanhToan, RefundStatus refundStatus) {
            return RefundStatus.PENDING == refundStatus
                    || "CHO_HOAN_TIEN".equals(normalizeUpper(trangThaiThanhToan));
        }

        static boolean hasPaymentReceived(String paymentStatus, String trangThaiThanhToan) {
            String pStatus = normalizeUpper(paymentStatus);
            String tStatus = normalizeUpper(trangThaiThanhToan);
            return "PAID".equals(pStatus)
                    || "PAID".equals(tStatus)
                    || "DA_THANH_TOAN".equals(tStatus)
                    || "CHO_HOAN_TIEN".equals(tStatus)
                    || "REFUNDED".equals(pStatus)
                    || "REFUNDED".equals(tStatus)
                    || "HOAN_TIEN".equals(tStatus)
                    || "DA_HOAN_TIEN".equals(tStatus);
        }

        private static String normalizeLower(String value) {
            return value == null ? "" : value.trim().toLowerCase();
        }

        private static String normalizeUpper(String value) {
            return value == null ? "" : value.trim().toUpperCase();
        }
    }

    // Xử lý Lấy khoảng thời gian từ bộ lọc nhanh (Preset)
    public Map<String, LocalDateTime> getDateRange(String preset, String startDateStr, String endDateStr) {
        LocalDateTime start;
        LocalDateTime end;
        ZonedDateTime now = ZonedDateTime.now(DEFAULT_ZONE);

        switch (preset != null ? preset.toLowerCase() : "") {
            case "today":
                start = now.toLocalDate().atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "this_week":
                // Thứ 2 tuần này
                LocalDate monday = now.toLocalDate().with(DayOfWeek.MONDAY);
                start = monday.atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "this_month":
                start = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "this_year":
                start = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "last_30_days":
                start = now.toLocalDate().minusDays(29).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "all_time":
                start = LocalDate.of(2000, 1, 1).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
            case "custom":
                if (startDateStr != null && !startDateStr.isEmpty() && endDateStr != null && !endDateStr.isEmpty()) {
                    start = LocalDate.parse(startDateStr).atStartOfDay();
                    end = LocalDate.parse(endDateStr).atTime(LocalTime.MAX);
                } else {
                    // Mặc định 30 ngày qua
                    start = now.toLocalDate().minusDays(29).atStartOfDay();
                    end = now.toLocalDate().atTime(LocalTime.MAX);
                }
                break;
            default:
                // Mặc định 30 ngày qua
                start = now.toLocalDate().minusDays(29).atStartOfDay();
                end = now.toLocalDate().atTime(LocalTime.MAX);
                break;
        }

        Map<String, LocalDateTime> range = new HashMap<>();
        range.put("start", start);
        range.put("end", end);
        return range;
    }

    // Xử lý Lấy khoảng thời gian của kỳ trước tương ứng để so sánh tăng trưởng
    public Map<String, LocalDateTime> getPreviousDateRange(String preset, LocalDateTime currentStart, LocalDateTime currentEnd) {
        if (preset == null || "all_time".equalsIgnoreCase(preset)) {
            return null;
        }

        LocalDateTime prevStart;
        LocalDateTime prevEnd;

        switch (preset.toLowerCase()) {
            case "today":
                // Hôm qua
                prevStart = currentStart.minusDays(1);
                prevEnd = currentEnd.minusDays(1);
                break;
            case "this_week":
                // Cùng khoảng thời gian của tuần trước
                prevStart = currentStart.minusWeeks(1);
                prevEnd = currentEnd.minusWeeks(1);
                break;
            case "last_30_days":
                // 30 ngày ngay trước khoảng hiện tại
                prevStart = currentStart.minusDays(30);
                prevEnd = currentStart.minusNanos(1);
                break;
            case "this_month":
                // Cùng khoảng ngày tương ứng của tháng trước (an toàn khi tháng trước ngắn hơn)
                LocalDate prevMonthStart = currentStart.toLocalDate().minusMonths(1).withDayOfMonth(1);
                int maxDayPrevMonth = prevMonthStart.lengthOfMonth();
                int targetDay = Math.min(currentEnd.toLocalDate().getDayOfMonth(), maxDayPrevMonth);
                LocalDate prevMonthEnd = prevMonthStart.withDayOfMonth(targetDay);
                prevStart = prevMonthStart.atStartOfDay();
                prevEnd = prevMonthEnd.atTime(LocalTime.MAX);
                break;
            case "this_year":
                // Cùng khoảng thời gian tương ứng của năm trước (Leap year safe)
                prevStart = currentStart.minusYears(1);
                prevEnd = currentEnd.minusYears(1);
                break;
            case "custom":
            default:
                // Kỳ trước có ĐÚNG SỐ NGÀY LỊCH bằng kỳ hiện tại
                long days = java.time.temporal.ChronoUnit.DAYS.between(
                        currentStart.toLocalDate(),
                        currentEnd.toLocalDate()
                ) + 1;
                prevStart = currentStart.minusDays(days);
                prevEnd = currentStart.minusNanos(1);
                break;
        }

        Map<String, LocalDateTime> prevRange = new HashMap<>();
        prevRange.put("start", prevStart);
        prevRange.put("end", prevEnd);
        return prevRange;
    }

    // Helper tính toán tăng trưởng giữa kỳ hiện tại và kỳ trước an toàn phép chia cho 0
    public static GrowthMetricDTO calculateGrowth(Double current, Double previous) {
        if (current == null) current = 0.0;
        if (previous == null) previous = 0.0;

        if (previous == 0.0) {
            if (current == 0.0) {
                return new GrowthMetricDTO(current, previous, 0.0, "0.0%", "EQUAL", false);
            } else {
                return new GrowthMetricDTO(current, previous, null, "Mới", "UP", true);
            }
        }

        double diff = current - previous;
        double pct = (diff / previous) * 100.0;
        String dir;
        String formatted;
        if (Math.abs(pct) < 0.05) {
            dir = "EQUAL";
            formatted = "0.0%";
        } else if (pct > 0) {
            dir = "UP";
            formatted = String.format("↑ %.1f%%", pct);
        } else {
            dir = "DOWN";
            formatted = String.format("↓ %.1f%%", Math.abs(pct));
        }
        return new GrowthMetricDTO(current, previous, pct, formatted, dir, false);
    }

    // Sinh 3-5 nhận xét vận hành quan trọng dựa trên dữ liệu thực tế
    public List<OperationalInsightDTO> generateOperationalInsights(
            GrowthMetricDTO revenueGrowth,
            GrowthMetricDTO cancelGrowth,
            double cancellationRate,
            long totalOrders,
            List<BrandRevenueDTO> brandRevenues,
            List<SlowMovingProductDTO> slowMovingProducts,
            BigDecimal pendingRefund) {

        List<OperationalInsightDTO> insights = new ArrayList<>();

        // 1. Nhận xét doanh thu so với kỳ trước
        if (revenueGrowth != null && revenueGrowth.percentageChange() != null) {
            double pct = revenueGrowth.percentageChange();
            if (pct >= INTERNAL_REVENUE_GROWTH_THRESHOLD) {
                insights.add(new OperationalInsightDTO(
                        "SUCCESS",
                        "Tăng trưởng doanh thu tích cực",
                        String.format("Doanh thu thực tế kỳ này tăng %.1f%% so với kỳ trước.", pct),
                        "fas fa-arrow-trend-up"
                ));
            } else if (pct <= INTERNAL_REVENUE_DROP_THRESHOLD) {
                insights.add(new OperationalInsightDTO(
                        "WARNING",
                        "Doanh thu suy giảm",
                        String.format("Doanh thu thực tế kỳ này giảm %.1f%% so với kỳ trước.", Math.abs(pct)),
                        "fas fa-arrow-trend-down"
                ));
            } else {
                insights.add(new OperationalInsightDTO(
                        "INFO",
                        "Doanh thu ổn định",
                        String.format("Doanh thu thực tế duy trì tương đương kỳ trước (%s).", revenueGrowth.formattedChange()),
                        "fas fa-chart-line"
                ));
            }
        } else if (revenueGrowth != null && Boolean.TRUE.equals(revenueGrowth.isNew())) {
            insights.add(new OperationalInsightDTO(
                    "SUCCESS",
                    "Ghi nhận doanh thu mới",
                    "Kỳ này phát sinh doanh thu mới so với mức 0 đ ở kỳ trước.",
                    "fas fa-sparkles"
            ));
        }

        // 2. Cảnh báo tỷ lệ hủy đơn hàng
        if (totalOrders >= 5 && cancellationRate >= INTERNAL_HIGH_CANCEL_RATE_THRESHOLD) {
            insights.add(new OperationalInsightDTO(
                    "DANGER",
                    "Tỷ lệ hủy đơn cao",
                    String.format("Tỷ lệ hủy đơn đạt %.1f%% trên tổng số %d đơn hàng phát sinh trong kỳ.", cancellationRate, totalOrders),
                    "fas fa-triangle-exclamation"
            ));
        }

        // 3. Thương hiệu đóng góp doanh thu lớn nhất
        if (brandRevenues != null && !brandRevenues.isEmpty()) {
            BrandRevenueDTO topBrand = brandRevenues.get(0);
            if (topBrand.revenue() != null && topBrand.revenue().compareTo(BigDecimal.ZERO) > 0 && topBrand.percentage() != null && topBrand.percentage() > 0.0) {
                insights.add(new OperationalInsightDTO(
                        "INFO",
                        "Thương hiệu chủ lực",
                        String.format("Thương hiệu %s đóng góp tỷ trọng doanh thu hàng hóa lớn nhất: %.1f%%.",
                                topBrand.brandName(), topBrand.percentage()),
                        "fas fa-award"
                ));
            }
        }

        // 4. Hàng tồn kho không phát sinh đơn bán
        if (slowMovingProducts != null) {
            long zeroSalesCount = slowMovingProducts.stream().filter(p -> p.soldQuantity() == 0).count();
            if (zeroSalesCount > 0) {
                insights.add(new OperationalInsightDTO(
                        "WARNING",
                        "Sản phẩm tồn kho chưa bán được",
                        String.format("Có %d sản phẩm trong danh sách theo dõi đang còn tồn kho nhưng chưa phát sinh lượt bán trong kỳ.", zeroSalesCount),
                        "fas fa-boxes-stacked"
                ));
            }
        }

        // 5. Cảnh báo tiền chờ hoàn
        if (pendingRefund != null && pendingRefund.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(new OperationalInsightDTO(
                    "WARNING",
                    "Dòng tiền chờ hoàn trả",
                    String.format("Hiện đang có %s đ tiền hàng trong trạng thái chờ xử lý hoàn trả.",
                            Math.round(pendingRefund.doubleValue())),
                    "fas fa-hand-holding-dollar"
            ));
        }

        // Giới hạn tối đa 5 insights quan trọng nhất
        return insights.stream().limit(5).collect(Collectors.toList());
    }

    // Xác định kiểu gom nhóm biểu đồ
    public String getGroupingType(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        long days = duration.toDays();
        if (days <= 1) {
            return "HOUR";
        } else if (days <= 90) {
            return "DAY";
        } else {
            return "MONTH";
        }
    }

    private void addRevenueContribution(
            Map<String, BigDecimal> groupedRevenue,
            String grouping,
            LocalDateTime occurredAt,
            BigDecimal amount) {
        if (occurredAt == null || amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        String key;
        if ("HOUR".equals(grouping)) {
            key = String.valueOf(occurredAt.getHour());
        } else if ("DAY".equals(grouping)) {
            key = String.format("%02d/%02d", occurredAt.getDayOfMonth(), occurredAt.getMonthValue());
        } else {
            key = String.format("%02d/%d", occurredAt.getMonthValue(), occurredAt.getYear());
        }
        groupedRevenue.put(key, groupedRevenue.getOrDefault(key, BigDecimal.ZERO).add(amount));
    }

    private String standardizePaymentMethod(String raw) {
        if (raw == null) {
            return "CASH";
        }
        raw = raw.toUpperCase().trim();
        if (raw.contains("ZALOPAY")) {
            return "ZALOPAY";
        }
        if (raw.contains("SEPAY")) {
            return "SEPAY";
        }
        if (raw.contains("COD")) {
            return "COD";
        }
        if (raw.contains("CHUYEN_KHOAN") || raw.contains("CHUYỂN KHOẢN") || raw.contains("BANK") || raw.contains("TRANSFER") || raw.contains("BANKING")) {
            return "BANK_TRANSFER";
        }
        if (raw.contains("TIEN_MAT") || raw.contains("TIỀN MẶT") || raw.contains("CASH")) {
            return "CASH";
        }
        return "CASH";
    }

    static String standardizePaymentStatus(String status, String trangThaiDonHang) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();

        // Ưu tiên trạng thái thanh toán thực tế. Một đơn đã hủy vẫn có thể đang
        // chờ hoàn hoặc đã hoàn tiền, nên không được để trạng thái đơn ghi đè.
        if ("REFUNDED".equals(normalizedStatus)
                || "HOAN_TIEN".equals(normalizedStatus)
                || "DA_HOAN_TIEN".equals(normalizedStatus)) {
            return "REFUNDED";
        }
        if ("CHO_HOAN_TIEN".equals(normalizedStatus)) {
            return "REFUND_PENDING";
        }
        if ("PAID".equals(normalizedStatus)
                || "DA_THANH_TOAN".equals(normalizedStatus)
                || "SUCCESS".equals(normalizedStatus)
                || "SUCCESSFUL".equals(normalizedStatus)
                || "THANH_CONG".equals(normalizedStatus)) {
            return "PAID";
        }
        if ("FAILED".equals(normalizedStatus) || "THAT_BAI".equals(normalizedStatus)) {
            return "FAILED";
        }
        if ("EXPIRED".equals(normalizedStatus) || "HET_HAN".equals(normalizedStatus)) {
            return "EXPIRED";
        }
        if ("PENDING".equals(normalizedStatus)
                || "CHO_THANH_TOAN".equals(normalizedStatus)
                || "CHUA_THANH_TOAN".equals(normalizedStatus)) {
            return "PENDING";
        }
        if ("CANCELLED".equals(normalizedStatus)
                || "CANCELED".equals(normalizedStatus)
                || "DA_HUY".equals(normalizedStatus)
                || "HUY".equals(normalizedStatus)) {
            return "CANCELLED";
        }

        // Chỉ dùng trạng thái đơn làm dữ liệu dự phòng khi cột thanh toán trống
        // hoặc chứa một mã cũ không nhận diện được.
        if ("da_huy".equalsIgnoreCase(trangThaiDonHang)) {
            return "CANCELLED";
        }
        return normalizedStatus.isEmpty() ? "PENDING" : "UNKNOWN";
    }

    static String paymentStatusLabel(String paymentStatus) {
        return switch (paymentStatus != null ? paymentStatus : "UNKNOWN") {
            case "PAID" -> "Thành công";
            case "PENDING" -> "Chờ thanh toán";
            case "FAILED" -> "Thất bại";
            case "REFUNDED" -> "Đã hoàn tiền";
            case "REFUND_PENDING" -> "Chờ hoàn tiền";
            case "CANCELLED" -> "Đã hủy";
            case "EXPIRED" -> "Hết hạn thanh toán";
            default -> "Không xác định";
        };
    }

    public List<TransactionHistoryDTO> getTransactionHistory(LocalDateTime start, LocalDateTime end, Integer limit) {
        org.springframework.data.domain.Pageable pageable = limit != null ? PageRequest.of(0, limit) : org.springframework.data.domain.Pageable.unpaged();
        List<Object[]> rawList = hoaDonRepository.findRawTransactionsInPeriod(start, end, pageable);
        List<TransactionHistoryDTO> dtos = new ArrayList<>();
        for (Object[] row : rawList) {
            Integer rawId = (Integer) row[0];
            String ho = (String) row[1];
            String ten = (String) row[2];
            LocalDateTime ngayTao = (LocalDateTime) row[3];
            String pm = (String) row[4];
            String tenPttt = (String) row[5];
            String ps = (String) row[6];
            String tttt = (String) row[7];
            String ttdh = (String) row[8];
            String tid = (String) row[9];
            String appTransId = (String) row[10];
            String maGd = (String) row[11];
            BigDecimal amount = (BigDecimal) row[12];

            Long id = rawId != null ? rawId.longValue() : 0L;
            String dateStr = ngayTao != null ? ngayTao.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) : "20260805";
            String prefix = (row.length > 13 && row[13] != null) ? "HDSVN" : "DHSVN";
            String invoiceCode = prefix + dateStr + "-" + id;

            String customerName = "Khách vãng lai";
            if (ho != null || ten != null) {
                customerName = ((ho != null ? ho : "") + " " + (ten != null ? ten : "")).trim();
                if (customerName.isEmpty()) {
                    customerName = "Khách vãng lai";
                }
            }

            String rawMethod = pm != null && !pm.trim().isEmpty() ? pm : tenPttt;
            String paymentMethod = standardizePaymentMethod(rawMethod);

            String rawStatus = ps != null && !ps.trim().isEmpty() ? ps : tttt;
            String paymentStatus = standardizePaymentStatus(rawStatus, ttdh);
            String paymentStatusLabel = paymentStatusLabel(paymentStatus);

            String transactionId = tid != null && !tid.trim().isEmpty() ? tid : appTransId;
            if (transactionId == null || transactionId.trim().isEmpty()) {
                transactionId = maGd;
            }
            if (transactionId == null || transactionId.trim().isEmpty()) {
                transactionId = "-";
            }

            dtos.add(new TransactionHistoryDTO(
                    id,
                    invoiceCode,
                    customerName,
                    ngayTao,
                    paymentMethod,
                    paymentStatus,
                    paymentStatusLabel,
                    transactionId,
                    amount != null ? amount : BigDecimal.ZERO
            ));
        }
        return dtos;
    }

    // Cache kết quả thống kê 30 giây (được cấu hình bằng Caffeine TTL ở application.properties)
    @Cacheable(value = "thongke", key = "(#preset != null ? #preset : 'default') + '-' + #start.toString() + '-' + #end.toString()")
    public Map<String, Object> getStatisticsData(String preset, LocalDateTime start, LocalDateTime end) {
        List<Object[]> rawOrders = hoaDonRepository.findAllOrdersInPeriod(start, end);
        List<Object[]> refundEvents = paymentTransactionRepository.findSuccessfulRefundEventsInPeriod(start, end);

        long totalOrders = rawOrders.size();
        long successfulOrders = 0;
        long processingOrders = 0;
        long cancelledOrders = 0;
        long refundedOrders = 0;
        BigDecimal grossRevenue = BigDecimal.ZERO;
        BigDecimal actualRevenue = BigDecimal.ZERO;
        BigDecimal expectedRevenue = BigDecimal.ZERO;
        BigDecimal refundedRevenue = BigDecimal.ZERO;
        BigDecimal pendingRefund = BigDecimal.ZERO;
        Set<Integer> refundedOrderIds = new HashSet<>();

        List<BigDecimal> successfulOrderAmounts = new ArrayList<>();

        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("cho_xac_nhan", 0L);
        statusMap.put("dang_giao", 0L);
        statusMap.put("da_giao", 0L);
        statusMap.put("da_huy", 0L);

        Map<String, BigDecimal> groupedRevenue = new HashMap<>();
        String grouping = getGroupingType(start, end);

        long onlineTotal = 0;
        long onlineSuccess = 0;
        long onlineFailed = 0;
        long onlinePending = 0;
        BigDecimal onlineRevenue = BigDecimal.ZERO;

        for (Object[] row : rawOrders) {
            LocalDateTime ngayTao = (LocalDateTime) row[3];
            String paymentMethod = (String) row[4];
            String paymentStatus = (String) row[6];
            String trangThaiThanhToan = (String) row[7];
            String trangThaiDonHang = (String) row[8];
            BigDecimal tongTien = (BigDecimal) row[12];
            RefundStatus refundStatus = (RefundStatus) row[13];

            if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
                tongTien = BigDecimal.ZERO;
            }

            // Centralized classification
            String m1 = paymentMethod != null ? paymentMethod.toUpperCase().trim() : "";
            String m2 = (String) row[5] != null ? ((String) row[5]).toUpperCase().trim() : "";
            boolean isCod = m1.contains("COD") || m2.contains("COD");
            RevenueClassification classification = OrderClassifier.classify(trangThaiDonHang, paymentStatus, trangThaiThanhToan, refundStatus, isCod);

            BigDecimal revenueContribution = BigDecimal.ZERO;
            if (OrderClassifier.isDelivered(trangThaiDonHang)) {
                grossRevenue = grossRevenue.add(tongTien);
            }
            if (classification == RevenueClassification.ACTUAL_REVENUE) {
                // Mỗi đơn chỉ đóng góp một lần khi đã giao và chưa hoàn tiền.
                actualRevenue = actualRevenue.add(tongTien);
                revenueContribution = tongTien;
                successfulOrders++;
                successfulOrderAmounts.add(tongTien);
            } else if (classification == RevenueClassification.PROJECTED_REVENUE) {
                expectedRevenue = expectedRevenue.add(tongTien);
            }

            addRevenueContribution(groupedRevenue, grouping, ngayTao, revenueContribution);

            // Count order categories
            String status = trangThaiDonHang != null ? trangThaiDonHang.toLowerCase() : "";
            if ("da_huy".equals(status) || "cancelled".equals(status) || "giao_that_bai".equals(status) || "stock_conflict".equals(status)) {
                cancelledOrders++;
            } else if ("cho_xac_nhan".equals(status) || "cho_thanh_toan".equals(status) || "da_xac_nhan".equals(status) 
                    || "dang_chuan_bi_hang".equals(status) || "san_sang_giao".equals(status) || "da_tao_van_don_ghn".equals(status) 
                    || "da_ban_giao_ghn".equals(status) || "dang_lay_hang".equals(status) || "dang_giao".equals(status) 
                    || "processing".equals(status) || "shipping".equals(status)) {
                processingOrders++;
            }

            // Count pending refund
            if (OrderClassifier.isRefundPending(trangThaiThanhToan, refundStatus)
                    && OrderClassifier.hasPaymentReceived(paymentStatus, trangThaiThanhToan)
                    && !OrderClassifier.isRefundCompleted(paymentStatus, trangThaiThanhToan, refundStatus)) {
                pendingRefund = pendingRefund.add(tongTien);
            }

            // Status map distribution for chart
            String normalizedStatus = switch (status) {
                case "da_giao", "delivered", "hoan_thanh" ->
                    "da_giao";
                case "da_huy", "cancelled", "giao_that_bai", "stock_conflict" ->
                    "da_huy";
                case "dang_giao", "shipping", "dang_lay_hang", "da_ban_giao_ghn" ->
                    "dang_giao";
                case "cho_xac_nhan", "cho_thanh_toan", "processing", "da_xac_nhan", "dang_chuan_bi_hang", "san_sang_giao", "da_tao_van_don_ghn" ->
                    "cho_xac_nhan";
                default ->
                    "cho_xac_nhan";
            };
            if (statusMap.containsKey(normalizedStatus)) {
                statusMap.put(normalizedStatus, statusMap.get(normalizedStatus) + 1);
            }

            // Online stats
            if (paymentMethod != null) {
                String pm = paymentMethod.toLowerCase();
                if (pm.contains("zalopay") || pm.contains("sepay")) {
                    onlineTotal++;

                    // Online Success KPI: strictly requires gateway-confirmed payment_status='paid'.
                    // trang_thai_thanh_toan='DA_THANH_TOAN' alone is NOT sufficient because it can be set
                    // manually by admins via the override endpoint and would otherwise inflate this metric.
                    String pStatus = paymentStatus != null ? paymentStatus.toUpperCase() : "";
                    String tStatus = trangThaiThanhToan != null ? trangThaiThanhToan.toUpperCase() : "";
                    if ("PAID".equals(pStatus)) {
                        onlineSuccess++;
                        // Revenue: only count if delivered AND gateway-confirmed
                        if (OrderClassifier.isDelivered(trangThaiDonHang)
                                && !OrderClassifier.isRefundCompleted(paymentStatus, trangThaiThanhToan, refundStatus)) {
                            onlineRevenue = onlineRevenue.add(tongTien);
                        }
                    } else if ("FAILED".equals(pStatus) || "THAT_BAI".equalsIgnoreCase(pStatus) || "FAILED".equals(tStatus)) {
                        onlineFailed++;
                    } else if ("PENDING".equals(pStatus) || "PENDING".equals(tStatus) || "CHO_THANH_TOAN".equals(tStatus)) {
                        onlinePending++;
                    } else {
                        onlinePending++;
                    }
                }
            }
        }

        // Hoàn tiền là một bút toán tài chính độc lập: dùng đúng số tiền và thời
        // điểm của REFUND_SUCCESS, không dùng tổng tiền/trạng thái cuối của đơn.
        for (Object[] refundEvent : refundEvents) {
            Integer orderId = (Integer) refundEvent[0];
            BigDecimal refundAmount = (BigDecimal) refundEvent[1];
            if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            refundedRevenue = refundedRevenue.add(refundAmount);
            if (orderId != null) {
                refundedOrderIds.add(orderId);
            }
        }
        refundedOrders = refundedOrderIds.size();

        // Avg order value
        double avgOrderValue = 0.0;
        if (!successfulOrderAmounts.isEmpty()) {
            BigDecimal sum = successfulOrderAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            avgOrderValue = sum.doubleValue() / successfulOrderAmounts.size();
        }

        // Rates
        double successRate = totalOrders > 0 ? ((double) successfulOrders / totalOrders) * 100.0 : 0.0;
        double cancellationRate = totalOrders > 0 ? ((double) cancelledOrders / totalOrders) * 100.0 : 0.0;
        double processingRate = totalOrders > 0 ? ((double) processingOrders / totalOrders) * 100.0 : 0.0;

        // Khách hàng mới (Có đơn đầu tiên hoàn thành trong kỳ)
        Long newCustomersCount = hoaDonRepository.countNewCustomers(start, end);

        // Chart labels & values
        List<String> chartLabels = new ArrayList<>();
        List<BigDecimal> chartValues = new ArrayList<>();

        if ("HOUR".equals(grouping)) {
            for (int i = 0; i < 24; i++) {
                chartLabels.add(String.format("%02dh", i));
                chartValues.add(groupedRevenue.getOrDefault(String.valueOf(i), BigDecimal.ZERO));
            }
        } else if ("DAY".equals(grouping)) {
            LocalDateTime temp = start;
            while (!temp.isAfter(end)) {
                String dayKey = String.format("%02d/%02d", temp.getDayOfMonth(), temp.getMonthValue());
                chartLabels.add(dayKey);
                chartValues.add(groupedRevenue.getOrDefault(dayKey, BigDecimal.ZERO));
                temp = temp.plusDays(1);
            }
        } else { // MONTH
            LocalDateTime temp = start;
            while (temp.isBefore(end) || temp.getYear() == end.getYear() && temp.getMonthValue() == end.getMonthValue()) {
                String monthKey = String.format("%02d/%d", temp.getMonthValue(), temp.getYear());
                chartLabels.add("Tháng " + temp.getMonthValue() + "/" + temp.getYear());
                chartValues.add(groupedRevenue.getOrDefault(monthKey, BigDecimal.ZERO));
                temp = temp.plusMonths(1);
            }
        }

        // Top 5 products with revenue percentage calculated against total valid product line revenue
        List<TopProductDTO> rawTopProducts = hoaDonChiTietRepository.findBestSellingProducts(start, end, PageRequest.of(0, 5));
        Double totalValidProductRevenue = hoaDonChiTietRepository.getTotalProductLineRevenueInPeriod(start, end);
        double baseProductRevenue = (totalValidProductRevenue != null && totalValidProductRevenue > 0) ? totalValidProductRevenue : 0.0;

        List<TopProductDTO> topProducts = new ArrayList<>();
        for (TopProductDTO p : rawTopProducts) {
            double pRev = p.revenue() != null ? p.revenue().doubleValue() : 0.0;
            double pShare = baseProductRevenue > 0 ? (pRev / baseProductRevenue) * 100.0 : 0.0;
            topProducts.add(p.withPercentage(pShare));
        }

        // Brand revenue statistics & chart data
        List<BrandRevenueDTO> rawBrandRevenues = hoaDonChiTietRepository.findRevenueByBrand(start, end);
        List<BrandRevenueDTO> brandRevenues = new ArrayList<>();
        List<String> brandChartLabels = new ArrayList<>();
        List<BigDecimal> brandChartValues = new ArrayList<>();

        for (BrandRevenueDTO b : rawBrandRevenues) {
            double bRev = b.revenue() != null ? b.revenue().doubleValue() : 0.0;
            double bShare = baseProductRevenue > 0 ? (bRev / baseProductRevenue) * 100.0 : 0.0;
            BrandRevenueDTO enrichedBrand = b.withPercentage(bShare);
            brandRevenues.add(enrichedBrand);
            brandChartLabels.add(b.brandName() != null ? b.brandName() : "Khác");
            brandChartValues.add(b.revenue() != null ? b.revenue() : BigDecimal.ZERO);
        }

        // Slow moving / inventory analysis (Products with stock > 0, supporting soldQty = 0)
        List<Object[]> activeProducts = sanPhamRepository.findActiveProductsWithStock();
        List<Object[]> salesData = hoaDonChiTietRepository.findSoldQuantityByProductInPeriod(start, end);

        Map<Integer, Long> salesMap = new HashMap<>();
        for (Object[] row : salesData) {
            Integer pId = (Integer) row[0];
            Long sold = (Long) row[1];
            salesMap.put(pId, sold != null ? sold : 0L);
        }

        List<SlowMovingProductDTO> slowMovingCandidates = new ArrayList<>();
        for (Object[] row : activeProducts) {
            Integer pId = (Integer) row[0];
            String pName = (String) row[1];
            String catName = (String) row[2];
            String img = (String) row[3];
            Long stock = (Long) row[4];
            long sold = salesMap.getOrDefault(pId, 0L);
            long stockVal = stock != null ? stock : 0L;

            if (stockVal > 0) {
                String warningLevel;
                String warningBadge;
                if (sold == 0) {
                    warningLevel = "DANGER";
                    warningBadge = "Không phát sinh bán trong kỳ";
                } else {
                    warningLevel = "WARNING";
                    warningBadge = "Cần theo dõi";
                }

                slowMovingCandidates.add(new SlowMovingProductDTO(
                        pId,
                        pName,
                        catName,
                        img,
                        stockVal,
                        sold,
                        warningLevel,
                        warningBadge
                ));
            }
        }

        // Sort priority: soldQuantity ASC, stockQuantity DESC
        slowMovingCandidates.sort(
                Comparator.comparingLong(SlowMovingProductDTO::soldQuantity)
                          .thenComparing(Comparator.comparingLong(SlowMovingProductDTO::stockQuantity).reversed())
        );

        List<SlowMovingProductDTO> slowMovingProducts = slowMovingCandidates.stream()
                .limit(5)
                .collect(Collectors.toList());

        // General DTO
        GeneralMetricsDTO metrics = new GeneralMetricsDTO(
                totalOrders,
                successfulOrders,
                cancelledOrders,
                actualRevenue,
                avgOrderValue,
                hoaDonChiTietRepository.getTotalProductsSold(start, end)
        );

        Map<String, Object> data = new HashMap<>();
        data.put("metrics", metrics);
        data.put("totalOrders", totalOrders);
        data.put("successfulOrders", successfulOrders);
        data.put("processingOrders", processingOrders);
        data.put("cancelledOrders", cancelledOrders);
        data.put("refundedOrders", refundedOrders);
        data.put("successRate", successRate);
        data.put("cancellationRate", cancellationRate);
        data.put("processingRate", processingRate);
        data.put("newCustomers", newCustomersCount != null ? newCustomersCount : 0L);
        data.put("statusDistribution", statusMap);
        data.put("chartLabels", chartLabels);
        data.put("chartValues", chartValues);
        data.put("topProducts", topProducts);
        data.put("slowMovingProducts", slowMovingProducts);
        data.put("brandRevenues", brandRevenues);
        data.put("brandChartLabels", brandChartLabels);
        data.put("brandChartValues", brandChartValues);
        data.put("grouping", grouping);
        data.put("expectedRevenue", expectedRevenue);
        data.put("grossRevenue", grossRevenue);
        data.put("actualRevenue", actualRevenue);
        data.put("refundedRevenue", refundedRevenue);
        data.put("pendingRefund", pendingRefund);

        // Put Online statistics
        data.put("onlineTotal", onlineTotal);
        data.put("onlineSuccess", onlineSuccess);
        data.put("onlineFailed", onlineFailed);
        data.put("onlinePending", onlinePending);
        data.put("onlineRevenue", onlineRevenue);

        // Transaction history
        Long totalTransactions = hoaDonRepository.countTransactionsInPeriod(start, end);
        List<TransactionHistoryDTO> transactions = getTransactionHistory(start, end, TRANSACTION_PREVIEW_LIMIT);
        data.put("totalTransactions", totalTransactions != null ? totalTransactions : 0L);
        data.put("displayedTransactions", transactions.size());
        data.put("transactions", transactions);

        // Growth / Comparison with previous period
        Map<String, GrowthMetricDTO> growthMap = new HashMap<>();
        Map<String, LocalDateTime> prevRange = (preset != null && !"all_time".equalsIgnoreCase(preset))
                ? getPreviousDateRange(preset, start, end)
                : null;

        if (prevRange != null && prevRange.get("start") != null && prevRange.get("end") != null) {
            LocalDateTime prevStart = prevRange.get("start");
            LocalDateTime prevEnd = prevRange.get("end");

            List<Object[]> rawPrevOrders = hoaDonRepository.findAllOrdersInPeriod(prevStart, prevEnd);
            long prevTotalOrders = rawPrevOrders.size();
            long prevSuccessfulOrders = 0;
            long prevCancelledOrders = 0;
            BigDecimal prevActualRevenue = BigDecimal.ZERO;
            List<BigDecimal> prevSuccessfulOrderAmounts = new ArrayList<>();

            for (Object[] row : rawPrevOrders) {
                String paymentMethod = (String) row[4];
                String paymentStatus = (String) row[6];
                String trangThaiThanhToan = (String) row[7];
                String trangThaiDonHang = (String) row[8];
                BigDecimal tongTien = (BigDecimal) row[12];
                RefundStatus refundStatus = (RefundStatus) row[13];

                if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
                    tongTien = BigDecimal.ZERO;
                }

                String m1 = paymentMethod != null ? paymentMethod.toUpperCase().trim() : "";
                String m2 = (String) row[5] != null ? ((String) row[5]).toUpperCase().trim() : "";
                boolean isCod = m1.contains("COD") || m2.contains("COD");
                RevenueClassification classification = OrderClassifier.classify(trangThaiDonHang, paymentStatus, trangThaiThanhToan, refundStatus, isCod);

                if (classification == RevenueClassification.ACTUAL_REVENUE) {
                    prevActualRevenue = prevActualRevenue.add(tongTien);
                    prevSuccessfulOrders++;
                    prevSuccessfulOrderAmounts.add(tongTien);
                }

                String status = trangThaiDonHang != null ? trangThaiDonHang.toLowerCase() : "";
                if ("da_huy".equals(status) || "cancelled".equals(status) || "giao_that_bai".equals(status) || "stock_conflict".equals(status)) {
                    prevCancelledOrders++;
                }
            }
            double prevAvgOrderValue = 0.0;
            if (!prevSuccessfulOrderAmounts.isEmpty()) {
                BigDecimal sum = prevSuccessfulOrderAmounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                prevAvgOrderValue = sum.doubleValue() / prevSuccessfulOrderAmounts.size();
            }

            double prevCancellationRate = prevTotalOrders > 0 ? ((double) prevCancelledOrders / prevTotalOrders) * 100.0 : 0.0;
            Long prevNewCustomersCount = hoaDonRepository.countNewCustomers(prevStart, prevEnd);
            long prevNewCust = prevNewCustomersCount != null ? prevNewCustomersCount : 0L;

            growthMap.put("revenue", calculateGrowth(actualRevenue.doubleValue(), prevActualRevenue.doubleValue()));
            growthMap.put("totalOrders", calculateGrowth((double) totalOrders, (double) prevTotalOrders));
            growthMap.put("avgOrderValue", calculateGrowth(avgOrderValue, prevAvgOrderValue));
            growthMap.put("newCustomers", calculateGrowth((double) (newCustomersCount != null ? newCustomersCount : 0L), (double) prevNewCust));
            growthMap.put("cancellationRate", calculateGrowth(cancellationRate, prevCancellationRate));
        }

        data.put("growth", growthMap.isEmpty() ? null : growthMap);
        data.put("hasPreviousPeriod", prevRange != null);

        // Generate Operational Insights (Rule-based 3-5 key points)
        List<OperationalInsightDTO> insights = generateOperationalInsights(
                growthMap.get("revenue"),
                growthMap.get("cancellationRate"),
                cancellationRate,
                totalOrders,
                brandRevenues,
                slowMovingProducts,
                pendingRefund
        );
        data.put("insights", insights);

        return data;
    }

    public Map<String, Object> getStatisticsData(LocalDateTime start, LocalDateTime end) {
        return getStatisticsData("last_30_days", start, end);
    }

    // Xuất báo cáo thống kê ra file Excel
    public byte[] exportToExcel(LocalDateTime start, LocalDateTime end) throws IOException {
        Map<String, Object> stats = getStatisticsData(start, end);
        GeneralMetricsDTO metrics = (GeneralMetricsDTO) stats.get("metrics");
        Double cancellationRate = (Double) stats.get("cancellationRate");
        Long newCustomers = (Long) stats.get("newCustomers");
        @SuppressWarnings("unchecked")
        Map<String, Long> statusMap = (Map<String, Long>) stats.get("statusDistribution");
        @SuppressWarnings("unchecked")
        List<String> chartLabels = (List<String>) stats.get("chartLabels");
        @SuppressWarnings("unchecked")
        List<BigDecimal> chartValues = (List<BigDecimal>) stats.get("chartValues");
        @SuppressWarnings("unchecked")
        List<TopProductDTO> topProducts = (List<TopProductDTO>) stats.get("topProducts");

        try (Workbook workbook = new XSSFWorkbook()) {
            // Tạo kiểu chữ và Style cho Header
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);

            // Kiểu định dạng tiền tệ VND
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0\" đ\""));

            // ----------------------------------------------------
            // SHEET 1: CHỈ SỐ TỔNG QUAN & TRẠNG THÁI ĐƠN HÀNG
            // ----------------------------------------------------
            Sheet kpiSheet = workbook.createSheet("KPIs & Trạng Thái Đơn Hàng");
            kpiSheet.setColumnWidth(0, 8000);
            kpiSheet.setColumnWidth(1, 5000);

            Row r0 = kpiSheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("KPIs Chỉ Số Tổng Quan");
            c0.setCellStyle(headerStyle);
            kpiSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 1));

            String[] kpiNames = {
                "Tổng số đơn hàng", "Đơn hàng thành công", "Đơn hàng đã hủy",
                "Doanh thu trước hoàn", "Doanh thu thực tế", "Doanh thu dự kiến", "Tiền đã hoàn", "Đang chờ hoàn", "Giá trị trung bình đơn hàng",
                "Tổng sản phẩm đã bán", "Khách hàng mới", "Tỷ lệ hủy đơn"
            };
            Object[] kpiValues = {
                metrics.totalOrders(), metrics.successfulOrders(), metrics.cancelledOrders(),
                stats.get("grossRevenue"), stats.get("actualRevenue"), stats.get("expectedRevenue"), stats.get("refundedRevenue"), stats.get("pendingRefund"), metrics.avgOrderValue(),
                metrics.totalProductsSold(), newCustomers, cancellationRate
            };

            for (int i = 0; i < kpiNames.length; i++) {
                Row row = kpiSheet.createRow(i + 1);
                row.createCell(0).setCellValue(kpiNames[i]);
                Cell valCell = row.createCell(1);
                if (kpiValues[i] instanceof Long) {
                    valCell.setCellValue((Long) kpiValues[i]);
                } else if (kpiValues[i] instanceof Integer) {
                    valCell.setCellValue((Integer) kpiValues[i]);
                } else if (kpiValues[i] instanceof BigDecimal) {
                    valCell.setCellValue(((BigDecimal) kpiValues[i]).doubleValue());
                    valCell.setCellStyle(currencyStyle);
                } else if (kpiValues[i] instanceof Double) {
                    if (kpiNames[i].contains("trung bình")) {
                        valCell.setCellValue((Double) kpiValues[i]);
                        valCell.setCellStyle(currencyStyle);
                    } else if (kpiNames[i].contains("Tỷ lệ")) {
                        valCell.setCellValue((Double) kpiValues[i] / 100.0);
                        CellStyle pctStyle = workbook.createCellStyle();
                        pctStyle.setDataFormat(format.getFormat("0.0%"));
                        valCell.setCellStyle(pctStyle);
                    } else {
                        valCell.setCellValue((Double) kpiValues[i]);
                    }
                }
            }

            // Dòng trống và vẽ bảng Trạng thái Đơn hàng
            int startRowStatus = kpiNames.length + 3;
            Row rStatusHeader = kpiSheet.createRow(startRowStatus);
            Cell cStatusHeader = rStatusHeader.createCell(0);
            cStatusHeader.setCellValue("Trạng Thái Đơn Hàng");
            cStatusHeader.setCellStyle(headerStyle);
            kpiSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRowStatus, startRowStatus, 0, 1));

            String[] statuses = {"Chờ xác nhận", "Đang giao", "Đã giao", "Đã hủy"};
            String[] statusKeys = {"cho_xac_nhan", "dang_giao", "da_giao", "da_huy"};

            for (int i = 0; i < statuses.length; i++) {
                Row row = kpiSheet.createRow(startRowStatus + 1 + i);
                row.createCell(0).setCellValue(statuses[i]);
                row.createCell(1).setCellValue(statusMap.getOrDefault(statusKeys[i], 0L));
            }

            // Dòng trống và vẽ bảng Thống kê Online
            int startRowZp = startRowStatus + statuses.length + 3;
            Row rZpHeader = kpiSheet.createRow(startRowZp);
            Cell cZpHeader = rZpHeader.createCell(0);
            cZpHeader.setCellValue("Thống Kê Thanh Toán Online");
            cZpHeader.setCellStyle(headerStyle);
            kpiSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(startRowZp, startRowZp, 0, 1));

            String[] zpLabels = {
                "Tổng số giao dịch Online", "Giao dịch thành công",
                "Giao dịch thất bại", "Giao dịch đang chờ", "Doanh thu Online"
            };
            Object[] zpValues = {
                stats.get("onlineTotal"), stats.get("onlineSuccess"),
                stats.get("onlineFailed"), stats.get("onlinePending"), stats.get("onlineRevenue")
            };

            for (int i = 0; i < zpLabels.length; i++) {
                Row row = kpiSheet.createRow(startRowZp + 1 + i);
                row.createCell(0).setCellValue(zpLabels[i]);
                Cell valCell = row.createCell(1);
                if (zpValues[i] instanceof Long) {
                    valCell.setCellValue((Long) zpValues[i]);
                } else if (zpValues[i] instanceof BigDecimal) {
                    valCell.setCellValue(((BigDecimal) zpValues[i]).doubleValue());
                    valCell.setCellStyle(currencyStyle);
                }
            }

            // ----------------------------------------------------
            // SHEET 2: DOANH THU THEO THỜI GIAN
            // ----------------------------------------------------
            Sheet revSheet = workbook.createSheet("Doanh Thu Theo Thời Gian");
            revSheet.setColumnWidth(0, 6000);
            revSheet.setColumnWidth(1, 5000);

            Row rRevHeader = revSheet.createRow(0);
            Cell cRevTime = rRevHeader.createCell(0);
            cRevTime.setCellValue("Thời Gian");
            cRevTime.setCellStyle(headerStyle);

            Cell cRevAmount = rRevHeader.createCell(1);
            cRevAmount.setCellValue("Doanh Thu");
            cRevAmount.setCellStyle(headerStyle);

            for (int i = 0; i < chartLabels.size(); i++) {
                Row row = revSheet.createRow(i + 1);
                row.createCell(0).setCellValue(chartLabels.get(i));
                Cell cellVal = row.createCell(1);
                cellVal.setCellValue(chartValues.get(i).doubleValue());
                cellVal.setCellStyle(currencyStyle);
            }

            // Kiểu định dạng tỷ lệ %
            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.setDataFormat(format.getFormat("0.0%"));

            // ----------------------------------------------------
            // SHEET 3: TOP SẢN PHẨM BÁN CHẠY
            // ----------------------------------------------------
            Sheet prodSheet = workbook.createSheet("Top Sản Phẩm Bán Chạy");
            prodSheet.setColumnWidth(0, 2000);
            prodSheet.setColumnWidth(1, 10000);
            prodSheet.setColumnWidth(2, 5000);
            prodSheet.setColumnWidth(3, 3500);
            prodSheet.setColumnWidth(4, 5000);
            prodSheet.setColumnWidth(5, 3500);

            Row rProdHeader = prodSheet.createRow(0);
            String[] prodHeaders = {"Rank", "Tên Sản Phẩm", "Danh Mục", "Số Lượng Bán", "Doanh Thu", "Tỷ Trọng"};
            for (int i = 0; i < prodHeaders.length; i++) {
                Cell cell = rProdHeader.createCell(i);
                cell.setCellValue(prodHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < topProducts.size(); i++) {
                TopProductDTO p = topProducts.get(i);
                Row row = prodSheet.createRow(i + 1);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(p.productName());
                row.createCell(2).setCellValue(p.categoryName());
                row.createCell(3).setCellValue(p.soldQuantity());
                Cell cellRev = row.createCell(4);
                cellRev.setCellValue(p.revenue() != null ? p.revenue().doubleValue() : 0.0);
                cellRev.setCellStyle(currencyStyle);
                Cell cellPct = row.createCell(5);
                cellPct.setCellValue((p.percentage() != null ? p.percentage() : 0.0) / 100.0);
                cellPct.setCellStyle(percentStyle);
            }

            // ----------------------------------------------------
            // SHEET 4: DOANH THU THEO THƯƠNG HIỆU
            // ----------------------------------------------------
            Sheet brandSheet = workbook.createSheet("Doanh Thu Theo Thương Hiệu");
            brandSheet.setColumnWidth(0, 2000);
            brandSheet.setColumnWidth(1, 7000);
            brandSheet.setColumnWidth(2, 3500);
            brandSheet.setColumnWidth(3, 5000);
            brandSheet.setColumnWidth(4, 3500);

            Row rBrandHeader = brandSheet.createRow(0);
            String[] brandHeaders = {"Rank", "Thương Hiệu", "Số Lượng Bán", "Doanh Thu", "Tỷ Trọng"};
            for (int i = 0; i < brandHeaders.length; i++) {
                Cell cell = rBrandHeader.createCell(i);
                cell.setCellValue(brandHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            @SuppressWarnings("unchecked")
            List<BrandRevenueDTO> brandList = (List<BrandRevenueDTO>) stats.get("brandRevenues");
            if (brandList != null) {
                for (int i = 0; i < brandList.size(); i++) {
                    BrandRevenueDTO b = brandList.get(i);
                    Row row = brandSheet.createRow(i + 1);
                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(b.brandName());
                    row.createCell(2).setCellValue(b.soldQuantity() != null ? b.soldQuantity() : 0L);
                    Cell cellRev = row.createCell(3);
                    cellRev.setCellValue(b.revenue() != null ? b.revenue().doubleValue() : 0.0);
                    cellRev.setCellStyle(currencyStyle);
                    Cell cellPct = row.createCell(4);
                    cellPct.setCellValue((b.percentage() != null ? b.percentage() : 0.0) / 100.0);
                    cellPct.setCellStyle(percentStyle);
                }
            }

            // ----------------------------------------------------
            // SHEET 5: SẢN PHẨM BÁN CHẬM / TỒN KHO
            // ----------------------------------------------------
            Sheet slowSheet = workbook.createSheet("Sản Phẩm Bán Chậm - Tồn Kho");
            slowSheet.setColumnWidth(0, 2000);
            slowSheet.setColumnWidth(1, 10000);
            slowSheet.setColumnWidth(2, 5000);
            slowSheet.setColumnWidth(3, 3500);
            slowSheet.setColumnWidth(4, 3500);
            slowSheet.setColumnWidth(5, 6000);

            Row rSlowHeader = slowSheet.createRow(0);
            String[] slowHeaders = {"Rank", "Tên Sản Phẩm", "Danh Mục", "Tồn Kho Hiện Tại", "Đã Bán Trong Kỳ", "Cảnh Báo"};
            for (int i = 0; i < slowHeaders.length; i++) {
                Cell cell = rSlowHeader.createCell(i);
                cell.setCellValue(slowHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            @SuppressWarnings("unchecked")
            List<SlowMovingProductDTO> slowList = (List<SlowMovingProductDTO>) stats.get("slowMovingProducts");
            if (slowList != null) {
                for (int i = 0; i < slowList.size(); i++) {
                    SlowMovingProductDTO s = slowList.get(i);
                    Row row = slowSheet.createRow(i + 1);
                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(s.productName());
                    row.createCell(2).setCellValue(s.categoryName());
                    row.createCell(3).setCellValue(s.stockQuantity() != null ? s.stockQuantity() : 0L);
                    row.createCell(4).setCellValue(s.soldQuantity() != null ? s.soldQuantity() : 0L);
                    row.createCell(5).setCellValue(s.warningBadge());
                }
            }

            // ----------------------------------------------------
            // SHEET 6: LỊCH SỬ GIAO DỊCH
            // ----------------------------------------------------
            Sheet txSheet = workbook.createSheet("Lịch Sử Giao Dịch");
            txSheet.setColumnWidth(0, 3000);
            txSheet.setColumnWidth(1, 7000);
            txSheet.setColumnWidth(2, 6000);
            txSheet.setColumnWidth(3, 5000);
            txSheet.setColumnWidth(4, 8000);
            txSheet.setColumnWidth(5, 5000);
            txSheet.setColumnWidth(6, 5000);

            Row rTxHeader = txSheet.createRow(0);
            String[] txHeaders = {
                "Mã Hóa Đơn", "Khách Hàng", "Thời Gian",
                "Phương Thức", "Mã Giao Dịch", "Số Tiền", "Trạng Thái Thanh Toán"
            };
            for (int i = 0; i < txHeaders.length; i++) {
                Cell cell = rTxHeader.createCell(i);
                cell.setCellValue(txHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Bảng trên dashboard chỉ xem nhanh 5 dòng; file Excel phải giữ đầy
            // đủ lịch sử giao dịch trong khoảng thời gian được chọn.
            List<TransactionHistoryDTO> transactionsList = getTransactionHistory(start, end, null);
            if (transactionsList != null) {
                DateTimeFormatter excelDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                for (int i = 0; i < transactionsList.size(); i++) {
                    TransactionHistoryDTO tx = transactionsList.get(i);
                    Row row = txSheet.createRow(i + 1);

                    // Mã Hóa Đơn (String)
                    row.createCell(0).setCellValue(tx.invoiceCode());

                    // Khách Hàng (String)
                    row.createCell(1).setCellValue(tx.customerName());

                    // Thời Gian (String)
                    String formattedTime = tx.transactionTime() != null ? excelDateFormatter.format(tx.transactionTime()) : "";
                    row.createCell(2).setCellValue(formattedTime);

                    // Phương Thức (String)
                    row.createCell(3).setCellValue(tx.paymentMethod());

                    // Mã Giao Dịch (String)
                    row.createCell(4).setCellValue(tx.transactionId());

                    // Số Tiền (Numeric)
                    Cell cellAmt = row.createCell(5);
                    cellAmt.setCellValue(tx.amount() != null ? tx.amount().doubleValue() : 0.0);
                    cellAmt.setCellStyle(currencyStyle);

                    // Trạng Thái Thanh Toán (String)
                    row.createCell(6).setCellValue(tx.paymentStatusLabel());
                }
            }

            // Ghi workbook vào byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}
