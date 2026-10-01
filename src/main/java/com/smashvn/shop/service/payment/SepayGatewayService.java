package com.smashvn.shop.service.payment;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.api.GhnService;

import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dto.payment.SepayIpnRequest;
import com.smashvn.shop.dto.payment.SepayTransactionDto;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.exception.InvalidPaymentException;
import com.smashvn.shop.exception.OrderNotFoundException;
import com.smashvn.shop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayGatewayService implements PaymentGatewayService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final SepayConfig sepayConfig;
    private final HoaDonRepository hoaDonRepository;
    private final HoaDonChiTietRepository hoaDonChiTietRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditService auditService;
    private final GhnService ghnService;
    private final com.smashvn.shop.service.order.GuestCheckoutService guestCheckoutService;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final SepayOrderPaymentService sepayOrderPaymentService;

    @Override

    @Transactional
    public Map<String, Object> handleIpn(SepayIpnRequest request, String rawPayload) throws Exception {
        SepayTransactionDto transaction = request.getTransactionData();
        String transactionId = transaction.getTransactionId();
        BigDecimal transferAmount = transaction.getTransferAmount();
        String gateway = transaction.getGateway();

        // 1. Fast duplicate check (Idempotency)
        Optional<PaymentTransaction> existingTx = paymentTransactionRepository.findByTransactionId(transactionId);
        if (existingTx.isPresent()) {
            String existingStatus = existingTx.get().getStatus();
            if (isRecordedReceipt(existingTx.get())) {
                log.info("SePay IPN: Transaction ID {} already processed (status: {}). Duplicate IPN callback ignored.", transactionId, existingStatus);
                auditService.log(null, "PaymentTransaction", null, "UPDATE", 
                        null, "PAID", "127.0.0.1", "[PAYMENT_DUPLICATE] Duplicate IPN callback ignored for status " + existingStatus + ".", "SYSTEM");
                return createSuccessResponse("Already processed");
            } else {
                log.info("SePay IPN: Deleting previously failed transaction record {} (status: {}) to re-process.", 
                        transactionId, existingStatus);
                paymentTransactionRepository.delete(existingTx.get());
                paymentTransactionRepository.flush();
            }
        }

        // 2. Locate order by maDonHang
        String orderCode = extractOrderCode(transaction.getCode(), transaction.getContent());
        if (orderCode == null) {
            log.warn("SePay IPN: Could not extract order code from transaction Code: {} or Content: {}", 
                    transaction.getCode(), transaction.getContent());
            throw new OrderNotFoundException("Could not extract order code from transaction info.");
        }

        log.info("SePay IPN: Processing transaction {} for order code {}", transactionId, orderCode);

        String normalizedCode = orderCode.replace("-", "").replace("_", "").trim().toUpperCase();
        HoaDon order = hoaDonRepository.findByMaDonHangOrNormalized(orderCode, normalizedCode)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with code: " + orderCode));

        // Cancellation and timeout use the same order lock. Refresh also replaces a snapshot
        // loaded before waiting, which a locking query alone does not evict from JPA's context.
        order = hoaDonRepository.findByIdWithLock(order.getId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found with code: " + orderCode));
        entityManager.refresh(order, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (paymentTransactionRepository.findByTransactionId(transactionId).filter(this::isRecordedReceipt).isPresent()) {
            return createSuccessResponse("Already processed");
        }

        // 3. Validation
        // 3.1 Reject negative/zero transfer amounts
        if (transferAmount == null || transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentException("Transferred amount must be positive. Received: " + transferAmount);
        }

        // 3.2 Verify transfer amount matches order total (compare by numeric value, ignoring scale/decimal notation)
        BigDecimal normalizedTransfer = transferAmount.stripTrailingZeros();
        BigDecimal normalizedOrder = order.getTongTien().stripTrailingZeros();
        if (normalizedTransfer.compareTo(normalizedOrder) != 0) {
            throw new InvalidPaymentException("Transferred amount " + transferAmount + " does not match order total " + order.getTongTien());
        }

        // 3.3 Validate that the order's paymentStatus is not already PAID
        if (isOrderAlreadyPaid(order)) {
            log.info("SePay IPN: Order {} already paid. Returning success.", orderCode);
            return createSuccessResponse("Already processed");
        }

        // Cancelled/expired orders include POS: record money for refund handling before
        // any branch can mark the sale complete. Replays recognize this receipt status.
        if (OrderStatus.DA_HUY.getValue().equals(order.getTrangThaiDonHang()) || "expired".equalsIgnoreCase(order.getPaymentStatus())) {
            log.warn("SePay IPN: Late payment received for already cancelled/expired order: {}", orderCode);

            saveTransactionRecord(transaction, order, PaymentStatus.PAID_RECEIVED_AFTER_CANCEL.getValue(), rawPayload);

            // Keep the cancelled order; no inventory, voucher, cart or success email effects.
            order.setPaymentStatus(PaymentStatus.PAID_RECEIVED_AFTER_CANCEL.getValue());
            order.setTrangThaiThanhToan("HUY");
            hoaDonRepository.save(order);

            auditService.log(null, "HoaDon", Long.valueOf(order.getId()), "UPDATE",
                    "da_huy", "da_huy", "127.0.0.1",
                    "[PAYMENT_RECEIVED_AFTER_CANCEL] CRITICAL: Payment received after order cancellation/expiration. Ref: " + transactionId + ", Amt: " + transferAmount,
                    "SYSTEM");

            return createSuccessResponse("Processed");
        }

        // 3.4 Handle POS Order (Custom flow)
        boolean isPosOrder = order.getMaDonHang() != null && order.getMaDonHang().startsWith("HDSVN");
        if (isPosOrder) {
            order.setPaymentStatus(PaymentStatus.PAID.getValue());
            order.setTrangThaiThanhToan("DA_THANH_TOAN");
            order.setTransactionId(transactionId);
            order.setMaGiaoDich(transactionId);
            order.setPaidAt(LocalDateTime.now());
            order.setThoiGianXacNhan(LocalDateTime.now());
            order.setNguoiXacNhanThanhToan("SePay Gateway");
            order.setTrangThaiDonHang(OrderStatus.DA_GIAO.getValue()); // POS -> hoan thanh luon
            
            if (sepayConfig.isDebug()) {
                order.setGatewayResponse("SePay: Successful POS payment processed. Ref: " + transactionId);
            }
            hoaDonRepository.save(order);

            // Save transaction record in db
            saveTransactionRecord(transaction, order, "success", rawPayload);

            // Log PAYMENT_CONFIRMED
            auditService.log(null, "HoaDon", Long.valueOf(order.getId()), "UPDATE",
                    OrderStatus.CHO_THANH_TOAN.getValue(), OrderStatus.DA_GIAO.getValue(), "127.0.0.1",
                    "[PAYMENT_CONFIRMED_POS] POS payment success callback handled.", "SYSTEM");

            log.info("SePay IPN POS Success: Applied to order {}", orderCode);
            return createSuccessResponse("Processed");
        }

        // 4.5 Handle STOCK_CONFLICT Order (Block IPN payment processing)
        if (OrderStatus.STOCK_CONFLICT.getValue().equalsIgnoreCase(order.getTrangThaiDonHang())) {
            log.warn("SePay IPN ignored/blocked because order is in STOCK_CONFLICT. maDonHang: {}, transactionId: {}, trangThaiDonHang: {}",
                    orderCode, transactionId, order.getTrangThaiDonHang());

            saveTransactionRecord(transaction, order, "stock_conflict_blocked", rawPayload);

            auditService.log(null, "HoaDon", Long.valueOf(order.getId()), "UPDATE",
                    OrderStatus.STOCK_CONFLICT.getValue(), OrderStatus.STOCK_CONFLICT.getValue(), "127.0.0.1",
                    "[PAYMENT_BLOCKED_STOCK_CONFLICT] SePay IPN ignored/blocked because order is in STOCK_CONFLICT. Ref: " + transactionId + ", Amt: " + transferAmount,
                    "SYSTEM");

            return createSuccessResponse("Processed");
        }

        // 5. Normal Payment Processing via SepayOrderPaymentService
        boolean success = sepayOrderPaymentService.xuLyThanhToanSePay(order.getId(), transactionId, transferAmount, rawPayload);
        if (success) {
            log.info("SePay IPN Success: Payment processed via SepayOrderPaymentService for order {}", orderCode);
            return createSuccessResponse("Processed");
        } else {
            return createErrorResponse("Failed to process payment for order: " + orderCode);
        }
    }


    private PaymentTransaction saveTransactionRecord(SepayTransactionDto transactionDto, HoaDon order, String status, String rawPayload) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setTransactionId(transactionDto.getTransactionId());
        tx.setOrder(order);
        tx.setAmount(transactionDto.getTransferAmount());
        tx.setGateway(transactionDto.getGateway());
        tx.setStatus(status);
        tx.setCreatedAt(LocalDateTime.now());
        
        if (sepayConfig.isDebug()) {
            tx.setRawPayload(rawPayload);
        } else {
            tx.setRawPayload(null);
        }

        try {
            return paymentTransactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException ex) {
            log.warn("SePay IPN: Database unique constraint hit on transaction_id: {}", transactionDto.getTransactionId());
            // Rethrow a duplicate check or custom handler depending on callers, here we just allow transaction rollback or let caller return duplicate response
            throw ex;
        }
    }

    private void clearCustomerCart(HoaDon order, List<HoaDonChiTiet> orderItems) {
        try {
            GioHang gioHang = gioHangRepository.findByKhachHang_Id(order.getKhachHang().getId());
            if (gioHang != null) {
                for (HoaDonChiTiet orderItem : orderItems) {
                    GioHangChiTiet cartItem = gioHangChiTietRepository.findByGioHang_IdAndSanPhamChiTiet_Id(
                            gioHang.getId(), orderItem.getSanPhamChiTiet().getId());
                    if (cartItem != null) {
                        gioHangChiTietRepository.delete(cartItem);
                    }
                }
            }
        } catch (Exception e) {
            log.error("SePay IPN: Error clearing cart for order {}: ", order.getMaDonHang(), e);
        }
    }

    private String extractOrderCode(String code, String content) {
        String parsedCode = parseCode(code);
        String parsedContent = parseCode(content);

        if (parsedCode != null && parsedContent != null) {
            // Prefer the longer match, which represents the full order code including date and UUID suffix
            return parsedCode.length() >= parsedContent.length() ? parsedCode : parsedContent;
        }
        return parsedCode != null ? parsedCode : parsedContent;
    }

    private String parseCode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        // 1. Explicitly match HDSVN (POS orders) or DHSVN (Online orders) with digits and hyphens
        Pattern specificPattern = Pattern.compile("(HDSVN|DHSVN)[-_\\s]*\\d+([-_\\s]*\\d+)?", Pattern.CASE_INSENSITIVE);
        Matcher specificMatcher = specificPattern.matcher(text);
        if (specificMatcher.find()) {
            return specificMatcher.group(0).replaceAll("\\s+", "").toUpperCase();
        }

        // 2. Match HDSVN or DHSVN with alphanumeric codes anywhere in the text
        Pattern hsvPattern = Pattern.compile("(HDSVN|DHSVN)[-_\\s]*[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
        Matcher hsvMatcher = hsvPattern.matcher(text);
        if (hsvMatcher.find()) {
            return hsvMatcher.group(0).replaceAll("\\s+", "").toUpperCase();
        }

        // 3. Fallback match for legacy DH prefix with word boundary (to avoid matching inside bank refs like 220D)
        Pattern dhPattern = Pattern.compile("\\bDH[-_\\s]*\\d+[-_\\s]*[A-Z0-9]+", Pattern.CASE_INSENSITIVE);
        Matcher dhMatcher = dhPattern.matcher(text);
        if (dhMatcher.find()) {
            return dhMatcher.group(0).replaceAll("\\s+", "").toUpperCase();
        }

        return null;
    }

    private boolean isRecordedReceipt(PaymentTransaction transaction) {
        String status = transaction.getStatus();
        return "success".equalsIgnoreCase(status) || "stock_conflict_blocked".equalsIgnoreCase(status)
                || "PAID_INSUFFICIENT_STOCK".equalsIgnoreCase(status)
                || PaymentStatus.PAID_RECEIVED_AFTER_CANCEL.getValue().equalsIgnoreCase(status);
    }

    private boolean isOrderAlreadyPaid(HoaDon order) {
        return order != null
                && "DA_THANH_TOAN".equalsIgnoreCase(order.getTrangThaiThanhToan());
    }

    private String maskAccountNumber(String acc) {
        if (acc == null || acc.length() < 4) {
            return "****";
        }
        return "*".repeat(acc.length() - 4) + acc.substring(acc.length() - 4);
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        return resp;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        return resp;
    }
}

