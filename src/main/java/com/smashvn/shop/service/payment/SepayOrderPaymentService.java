package com.smashvn.shop.service.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.smashvn.shop.dto.inventory.AllocationResult;
import com.smashvn.shop.dto.inventory.AllocationStatus;
import com.smashvn.shop.dto.inventory.LotAllocation;
import com.smashvn.shop.dto.inventory.OrderItemRequest;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.inventory.InventoryLotService;
import com.smashvn.shop.service.user.TemporaryPasswordService;
import com.smashvn.shop.service.user.TemporaryPasswordService.TemporaryPasswordIssueResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SepayOrderPaymentService {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InventoryLotService inventoryLotService;
    private final AuditService auditService;
    private final com.smashvn.shop.service.order.GuestCheckoutService guestCheckoutService;
    private final TemporaryPasswordService temporaryPasswordService;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;

    @org.springframework.beans.factory.annotation.Value("${app.base-url:}")
    private String configuredBaseUrl;

    /**
     * Điều phối xử lý IPN Webhook từ SePay trong 1 Transaction kín.
     * Kiểm tra Idempotency dựa trên maGiaoDich SePay.
     * Nếu mã giao dịch mới trên đơn YEU_CAU_HUY ➔ Vẫn lưu vết nhận tiền và giữ luồng chờ hoàn tiền.
     * Nếu đủ kho ➔ Phân bổ FIFO, thay HDCT tạm bằng HDCT phân bổ thực tế, chuyển DA_THANH_TOAN.
     * Nếu thiếu kho ➔ INSUFFICIENT_STOCK: Commit lưu vết nhận tiền, chuyển YEU_CAU_HUY, gán RefundStatus = PENDING, KHÔNG rollback.
     */
    @Transactional
    public boolean xuLyThanhToanSePay(Integer idHoaDon, String maGiaoDich, BigDecimal soTien, String rawPayload) {
        log.info("[SepayOrderPaymentService] Đang xử lý IPN cho Đơn hàng #{}, mã giao dịch: {}", idHoaDon, maGiaoDich);

        // Step 1: Tra cứu và khóa bản ghi HoaDon
        HoaDon order = hoaDonRepository.findById(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        // Fail-safe guard: STOCK_CONFLICT order status block
        if (OrderStatus.STOCK_CONFLICT.getValue().equalsIgnoreCase(order.getTrangThaiDonHang())) {
            log.warn("SePay IPN ignored/blocked because order is in STOCK_CONFLICT. maDonHang: {}, transactionId: {}, trangThaiDonHang: {}",
                    order.getMaDonHang(), maGiaoDich, order.getTrangThaiDonHang());
            return false;
        }

        // Step 2: Idempotency Check dựa trên maGiaoDich SePay
        Optional<PaymentTransaction> existingTx = paymentTransactionRepository.findByTransactionId(maGiaoDich);
        if (existingTx.isPresent()) {
            log.info("[SepayOrderPaymentService] Mã giao dịch SePay '{}' đã được xử lý trước đó. Bỏ qua duplicate IPN.", maGiaoDich);
            return true;
        }

        // Step 3: Đọc danh sách HoaDonChiTiet tạm thời (provisional)
        List<HoaDonChiTiet> provisionalItems = hoaDonChiTietRepository.findByHoaDon_Id(idHoaDon);
        if (provisionalItems.isEmpty()) {
            log.warn("[SepayOrderPaymentService] Đơn hàng #{} không có dòng sản phẩm nào", idHoaDon);
            return false;
        }

        // Xây dựng danh sách OrderItemRequest để gọi InventoryLotService.allocateFifo
        List<OrderItemRequest> itemRequests = new ArrayList<>();
        for (HoaDonChiTiet hdct : provisionalItems) {
            itemRequests.add(OrderItemRequest.builder()
                    .sourceLineId(hdct.getId())
                    .representativeSpctId(hdct.getSanPhamChiTiet().getId())
                    .quantity(hdct.getSoLuong())
                    .build());
        }

        // Step 4: Gọi InventoryLotService.allocateFifo (đã khóa các sản phẩm cha theo ID ASC)
        AllocationResult allocationResult = inventoryLotService.allocateFifo(itemRequests);

        LocalDateTime now = LocalDateTime.now();

        if (allocationResult.status() == AllocationStatus.SUCCESS) {
            // Trường hợp 1: TỒN KHO ĐỦ ➔ Phân bổ FIFO thành công
            // Lưu vết PaymentTransaction SUCCESS
            PaymentTransaction tx = new PaymentTransaction();
            tx.setOrder(order);
            tx.setTransactionId(maGiaoDich);
            tx.setAmount(soTien);
            tx.setStatus("SUCCESS");
            tx.setGateway("SePay");
            tx.setRawPayload(rawPayload);
            tx.setCreatedAt(now);
            paymentTransactionRepository.save(tx);

            // Tạo các HoaDonChiTiet thực tế theo từng LotAllocation
            Map<Integer, HoaDonChiTiet> provisionalMap = new HashMap<>();
            for (HoaDonChiTiet pItem : provisionalItems) {
                provisionalMap.put(pItem.getId(), pItem);
            }

            for (LotAllocation alloc : allocationResult.allocations()) {
                HoaDonChiTiet sourceItem = provisionalMap.get(alloc.sourceLineId());
                if (sourceItem == null) {
                    sourceItem = provisionalItems.get(0);
                }

                HoaDonChiTiet allocatedHdct = new HoaDonChiTiet();
                allocatedHdct.setHoaDon(order);
                allocatedHdct.setSanPhamChiTiet(alloc.allocatedSpct());
                allocatedHdct.setSoLuong(alloc.quantityAllocated());
                allocatedHdct.setDonGia(sourceItem.getDonGia());
                allocatedHdct.setGiaGoc(sourceItem.getGiaGoc());
                allocatedHdct.setGiaSauGiam(sourceItem.getGiaSauGiam());
                allocatedHdct.setTenSanPhamSnapshot(sourceItem.getTenSanPhamSnapshot());
                allocatedHdct.setSkuSnapshot(sourceItem.getSkuSnapshot());
                allocatedHdct.setTenDotGiamGiaSnapshot(sourceItem.getTenDotGiamGiaSnapshot());
                allocatedHdct.setThuocTinhSnapshot(sourceItem.getThuocTinhSnapshot());
                allocatedHdct.setNgayTao(now);

                hoaDonChiTietRepository.save(allocatedHdct);
            }

            // Xóa các dòng HoaDonChiTiet tạm thời
            hoaDonChiTietRepository.deleteAll(provisionalItems);

            // Cập nhật trạng thái đơn hàng
            order.setTrangThaiThanhToan("DA_THANH_TOAN");
            order.setPaymentStatus("paid");
            order.setPaidAt(now);
            order.setNgayThanhToan(now);
            order.setMaGiaoDich(maGiaoDich);
            order.setTransactionId(maGiaoDich);
            if ("cho_thanh_toan".equalsIgnoreCase(order.getTrangThaiDonHang()) || "CHO_THANH_TOAN".equalsIgnoreCase(order.getTrangThaiDonHang())) {
                boolean isPosOrder = order.getMaDonHang() != null && order.getMaDonHang().startsWith("HDSVN");
                if (isPosOrder) {
                    order.setTrangThaiDonHang(OrderStatus.DA_GIAO.getValue()); // Bán tại quầy -> Hoàn thành (Đã giao) ngay khi nhận tiền
                } else {
                    order.setTrangThaiDonHang(OrderStatus.CHO_XAC_NHAN.getValue()); // Đơn Online -> Chờ xác nhận đóng gói
                }
            }
            hoaDonRepository.save(order);

            // Deduct applied Voucher quantity with Pessimistic Write Lock
            if (order.getPhieuGiamGia() != null && order.getPhieuGiamGia().getMaPhieu() != null) {
                String maPhieu = order.getPhieuGiamGia().getMaPhieu();
                try {
                    phieuGiamGiaRepository.findByMaPhieuWithLock(maPhieu).ifPresent(voucher -> {
                        Integer remaining = voucher.getSoLuongConLai();
                        if (remaining != null && remaining > 0) {
                            voucher.setSoLuongConLai(remaining - 1);
                            phieuGiamGiaRepository.save(voucher);
                            log.info("[SepayOrderPaymentService] Đã trừ số lượng voucher '{}' sau thanh toán SePay thành công (Đơn #{}): {} -> {}",
                                    maPhieu, idHoaDon, remaining, remaining - 1);
                        } else {
                            log.warn("[SepayOrderPaymentService] Voucher '{}' của đơn #{} đã hết lượt hoặc mang giá trị null ({}) tại thời điểm thanh toán SePay.",
                                    maPhieu, idHoaDon, remaining);
                        }
                    });
                } catch (Exception vEx) {
                    log.error("[SepayOrderPaymentService] Lỗi trừ số lượng voucher '{}' cho đơn #{}: {}", maPhieu, idHoaDon, vEx.getMessage(), vEx);
                }
            }

            // Counter và xét cấp mật khẩu tạm cùng nằm dưới khóa account trong transaction này.
            TemporaryPasswordIssueResult temporaryPasswordResult = null;
            if (order.getKhachHang() != null && order.getKhachHang().getTaiKhoan() != null) {
                temporaryPasswordResult = temporaryPasswordService
                        .recordSepayPaymentSuccess(order.getKhachHang().getTaiKhoan().getId());
            }
            final TemporaryPasswordIssueResult passwordEmailResult = temporaryPasswordResult;

            // Cleanup cart items for the customer if any match
            if (order.getKhachHang() != null && order.getKhachHang().getId() != null) {
                try {
                    GioHang gh = gioHangRepository.findByKhachHang_Id(order.getKhachHang().getId());
                    if (gh != null) {
                        List<GioHangChiTiet> currentCartItems = gioHangChiTietRepository.findByGioHang_Id(gh.getId());
                        if (currentCartItems != null && !currentCartItems.isEmpty()) {
                            for (HoaDonChiTiet pItem : provisionalItems) {
                                if (pItem.getSanPhamChiTiet() != null) {
                                    Integer spctId = pItem.getSanPhamChiTiet().getId();
                                    currentCartItems.stream()
                                            .filter(ci -> ci.getSanPhamChiTiet() != null && ci.getSanPhamChiTiet().getId().equals(spctId))
                                            .forEach(gioHangChiTietRepository::delete);
                                }
                            }
                        }
                    }
                } catch (Exception cEx) {
                    log.warn("[SepayOrderPaymentService] Could not cleanup cart items for order #{}: {}", idHoaDon, cEx.getMessage());
                }
            }

            // Gửi email hóa đơn xác nhận thanh toán cho đơn hàng Online SAU KHI TRANSACTION COMMIT
            boolean isPosOrder = order.getMaDonHang() != null && order.getMaDonHang().startsWith("HDSVN");
            if (!isPosOrder) {
                try {
                    String userEmail = order.getEmailNguoiNhan();
                    if (userEmail == null || userEmail.isBlank()) {
                        userEmail = (order.getKhachHang() != null && order.getKhachHang().getTaiKhoan() != null)
                                ? order.getKhachHang().getTaiKhoan().getUsername()
                                : null;
                    }
                    if (userEmail != null && userEmail.contains("@")) {
                        final String recipient = userEmail.trim();
                        final HoaDon orderSnapshot = order;

                        Runnable emailTask = () -> {
                            try {
                                String baseUrl = (configuredBaseUrl != null && !configuredBaseUrl.trim().isEmpty())
                                        ? configuredBaseUrl.trim().replaceAll("/+$", "")
                                        : "http://localhost:8080";

                                log.info("[SepayOrderPaymentService] Transaction committed, triggering order confirmation email for order #{}", orderSnapshot.getMaDonHang());
                                guestCheckoutService.sendOrderConfirmationEmail(recipient, orderSnapshot, baseUrl);

                                if (passwordEmailResult != null && passwordEmailResult.isIssued()) {
                                    log.info("[SepayOrderPaymentService] Triggering initial temporary password email for account ID {} (Order #{})",
                                            passwordEmailResult.accountId(), orderSnapshot.getMaDonHang());
                                    temporaryPasswordService.sendTemporaryPasswordEmail(passwordEmailResult, baseUrl);
                                }
                            } catch (Exception ex) {
                                log.error("[SepayOrderPaymentService] Lỗi gửi email xác nhận cho đơn {}: {}", orderSnapshot.getMaDonHang(), ex.getMessage());
                            }
                        };

                        if (TransactionSynchronizationManager.isSynchronizationActive()) {
                            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    emailTask.run();
                                }
                            });
                            log.info("[SepayOrderPaymentService] Registered afterCommit email notification for order #{}", orderSnapshot.getMaDonHang());
                        } else {
                            log.error("[SepayOrderPaymentService] No active transaction synchronization; skip confirmation email to avoid pre-commit send. order={}", orderSnapshot.getMaDonHang());
                        }
                    }
                } catch (Exception e) {
                    log.error("[SepayOrderPaymentService] Lỗi khởi chạy async email: {}", e.getMessage());
                }
            }

            String note = String.format("Thanh toán SePay thành công cho đơn #%d, mã GD: %s, số tiền: %s", idHoaDon, maGiaoDich, soTien);
            auditService.log(null, "HoaDon", idHoaDon.longValue(), "SEPAY_PAID_SUCCESS", "", "DA_THANH_TOAN", "127.0.0.1", note, "SYSTEM");

            log.info("[SepayOrderPaymentService] Đã xử lý thanh toán thành công và trừ kho FIFO cho Đơn #{}", idHoaDon);
            return true;

        } else {
            // Trường hợp 2: TỒN KHO THIẾU ➔ INSUFFICIENT_STOCK
            // KHÔNG ném Exception, COMMIT TRANSACTION để lưu giao dịch đã nhận tiền
            PaymentTransaction tx = new PaymentTransaction();
            tx.setOrder(order);
            tx.setTransactionId(maGiaoDich);
            tx.setAmount(soTien);
            tx.setStatus("PAID_INSUFFICIENT_STOCK");
            tx.setGateway("SePay");
            tx.setRawPayload(rawPayload);
            tx.setCreatedAt(now);
            paymentTransactionRepository.save(tx);

            // Giữ nguyên HDCT tạm, KHÔNG trừ kho, chuyển trạng thái bị chặn giao hàng
            order.setTrangThaiDonHang("YEU_CAU_HUY");
            order.setTrangThaiThanhToan("CHO_HOAN_TIEN");
            order.setRefundStatus(RefundStatus.PENDING);
            order.setMaGiaoDich(maGiaoDich);
            order.setLyDoHoanTien("Đã nhận tiền chuyển khoản SePay nhưng kho không đủ tồn tự động trừ. Chuyển hoàn tiền thủ công.");
            hoaDonRepository.save(order);

            String note = String.format("Đã nhận tiền SePay (%s) cho đơn #%d nhưng thiếu tồn kho tự động. Đơn chuyển YEU_CAU_HUY chờ hoàn tiền.", maGiaoDich, idHoaDon);
            auditService.log(null, "HoaDon", idHoaDon.longValue(), "SEPAY_PAID_INSUFFICIENT_STOCK", "", "YEU_CAU_HUY", "127.0.0.1", note, "SYSTEM");


            log.warn("[SepayOrderPaymentService] Đơn #{} nhận tiền thành công nhưng thiếu tồn kho. Chuyển YEU_CAU_HUY & CHO_HOAN_TIEN. Lý do: {}", idHoaDon, allocationResult.message());
            return true; // Đã xử lý ghi nhận xong
        }
    }

    /**
     * Hoàn tiền cho đơn hàng SePay thiếu kho sau khi đã hoàn tiền thủ công cho khách:
     * Chuyển trạng thái sang DA_HUY & REFUNDED mà TUYỆT ĐỐI KHÔNG gọi hoanKhoHangLoat (vì đơn chưa từng trừ kho).
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "thongke", allEntries = true)
    public void finalizeRefundWithoutRestock(Integer idHoaDon, Integer actingUserId) {
        HoaDon order = hoaDonRepository.findById(idHoaDon)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng ID: " + idHoaDon));

        // 1. Idempotency Guard: Nếu đơn đã ở trạng thái REFUNDED / RefundStatus.COMPLETED thì bỏ qua
        if ("REFUNDED".equalsIgnoreCase(order.getTrangThaiThanhToan()) || RefundStatus.COMPLETED.equals(order.getRefundStatus())) {
            log.info("[SepayOrderPaymentService] Đơn #{}: Đã ở trạng thái REFUNDED/COMPLETED trước đó. Bỏ qua hoàn tiền lặp lại.", idHoaDon);
            return;
        }

        // 2. Strict Transaction Guard: Chỉ chấp nhận transaction có status PAID_INSUFFICIENT_STOCK
        List<PaymentTransaction> txs = paymentTransactionRepository.findByOrder_Id(idHoaDon);
        PaymentTransaction insufficientStockPayment = txs != null
                ? txs.stream()
                        .filter(t -> "PAID_INSUFFICIENT_STOCK".equalsIgnoreCase(t.getStatus()))
                        .findFirst()
                        .orElse(null)
                : null;

        if (insufficientStockPayment == null) {
            String txStatuses = (txs != null && !txs.isEmpty()) 
                    ? txs.stream().map(PaymentTransaction::getStatus).reduce((a, b) -> a + ", " + b).orElse("NONE")
                    : "NONE";
            log.warn("[SepayOrderPaymentService] Reject finalizeRefundWithoutRestock cho Đơn #{}, mã đơn: {}. Không có transaction PAID_INSUFFICIENT_STOCK. Transaction status hiện có: {}",
                    idHoaDon, order.getMaDonHang(), txStatuses);
            throw new IllegalStateException("Đơn hàng không thuộc trường hợp đã thanh toán nhưng thiếu tồn kho.");
        }

        // 3. Strict Order Status Guard: Trạng thái đơn hàng phải là YEU_CAU_HUY
        if (!"YEU_CAU_HUY".equalsIgnoreCase(order.getTrangThaiDonHang())) {
            log.warn("[SepayOrderPaymentService] Reject finalizeRefundWithoutRestock cho Đơn #{}, mã đơn: {}. Trạng thái đơn hàng hiện tại '{}' không phải YEU_CAU_HUY.",
                    idHoaDon, order.getMaDonHang(), order.getTrangThaiDonHang());
            throw new IllegalStateException("Trạng thái đơn hàng không hợp lệ để hoàn tiền do thiếu kho.");
        }

        // 4. Ghi bút toán hoàn tiền riêng để thống kê dùng đúng số tiền và
        // thời điểm hoàn, đồng thời giữ nguyên giao dịch nhận tiền ban đầu.
        if (!paymentTransactionRepository.existsByOrder_IdAndStatus(idHoaDon, "REFUND_SUCCESS")) {
            BigDecimal refundAmount = insufficientStockPayment.getAmount() != null
                    ? insufficientStockPayment.getAmount()
                    : order.getTongTien();
            PaymentTransaction refundTx = new PaymentTransaction();
            refundTx.setOrder(order);
            refundTx.setTransactionId("REFUND-STOCK-" + idHoaDon + "-" + System.currentTimeMillis());
            refundTx.setAmount(refundAmount);
            refundTx.setGateway("MANUAL_REFUND");
            refundTx.setStatus("REFUND_SUCCESS");
            refundTx.setRawPayload("{\"transactionType\":\"INSUFFICIENT_STOCK_REFUND\"}");
            refundTx.setCreatedAt(LocalDateTime.now());
            paymentTransactionRepository.saveAndFlush(refundTx);
        }

        // 5. Finalize refund mà KHÔNG tác động tồn kho
        order.setTrangThaiDonHang("DA_HUY");
        order.setTrangThaiThanhToan("REFUNDED");
        order.setRefundStatus(RefundStatus.COMPLETED);
        order.setLyDoHuy("Đã hoàn tiền cho khách thành công (Đơn nhận tiền nhưng thiếu kho)");
        hoaDonRepository.save(order);

        String note = "Đã hoàn tiền thành công cho đơn SePay thiếu kho. Chuyển DA_HUY mà KHÔNG cộng tồn kho.";
        auditService.log(actingUserId, "HoaDon", idHoaDon.longValue(), "FINALIZE_REFUND_NO_RESTOCK",
                "YEU_CAU_HUY", "DA_HUY", "127.0.0.1", note, "ADMIN");

        log.info("[SepayOrderPaymentService] Đã chuyển Đơn #{} sang DA_HUY & REFUNDED mà KHÔNG cộng tồn kho.", idHoaDon);
    }
}
