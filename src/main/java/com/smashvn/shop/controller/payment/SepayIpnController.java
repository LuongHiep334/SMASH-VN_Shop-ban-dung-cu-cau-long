package com.smashvn.shop.controller.payment;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dto.payment.SepayIpnRequest;
import com.smashvn.shop.dto.payment.SepayTransactionDto;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.exception.InvalidPaymentException;
import com.smashvn.shop.exception.OrderNotFoundException;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.payment.PaymentGatewayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SepayIpnController {

    private final SepayConfig sepayConfig;
    private final PaymentGatewayService paymentGatewayService;
    private final HoaDonRepository hoaDonRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditService auditService;
    private final com.smashvn.shop.service.order.GioHangService gioHangService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/api/payment/sepay/ipn")
    public ResponseEntity<Map<String, Object>> handleSepayIpn(
            @RequestBody String rawPayload,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("SePay IPN: Received callback webhook request");

        try {
            // 1. Verify Request IP Whitelist
            if (sepayConfig.isIpVerification()) {
                String clientIp = getClientIp(request);
                if (!isValidIp(clientIp)) {
                    log.warn("SePay IPN: Request rejected. Unauthorized IP: {}", clientIp);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Unauthorized IP address: " + clientIp + ". Please whitelist this IP or disable IP verification in your configuration."));
                }
            }

            // 2. Verify Authorization Header (Apikey ipnSecret)
            boolean isAuthValid = false;
            if (authHeader != null) {
                String trimHeader = authHeader.trim();
                String expectedSecret = sepayConfig.getIpnSecret();
                if (trimHeader.length() > 7 && trimHeader.substring(0, 7).equalsIgnoreCase("apikey ")) {
                    String providedSecret = trimHeader.substring(7).trim();
                    if (expectedSecret != null && !expectedSecret.isBlank()
                            && providedSecret.equals(expectedSecret)) {
                        isAuthValid = true;
                    }
                }
            }
            if (!isAuthValid) {
                log.warn("SePay IPN: Request rejected. Invalid Authorization Header.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("Invalid API credentials."));
            }

            // Deserialize payload
            SepayIpnRequest ipnRequest = objectMapper.readValue(rawPayload, SepayIpnRequest.class);

            // 3. Process IPN payload (handles duplicate transactions internally and returns proper message)
            Map<String, Object> result;
            try {
                result = paymentGatewayService.handleIpn(ipnRequest, rawPayload);
            } catch (DataIntegrityViolationException e) {
                log.info("SePay IPN: Database unique constraint triggered on concurrent duplicate.");
                return ResponseEntity.ok(createSuccessResponse("Already processed", ipnRequest.getTransactionData().getTransactionId()));
            }

            return ResponseEntity.ok(result);

        } catch (InvalidPaymentException e) {
            log.error("SePay IPN Validation Error: {}", e.getMessage());
            try {
                SepayIpnRequest ipnRequest = objectMapper.readValue(rawPayload, SepayIpnRequest.class);
                String orderCode = ipnRequest.getTransactionData().getCode();
                if (orderCode != null) {
                    String normalizedCode = orderCode.replace("-", "").replace("_", "").trim().toUpperCase();
                    Optional<HoaDon> orderOpt = hoaDonRepository.findByMaDonHangOrNormalized(orderCode, normalizedCode);
                    if (orderOpt.isPresent()) {
                        HoaDon order = orderOpt.get();
                        saveFailedTransaction(ipnRequest, order, "amount_mismatch", rawPayload);
                        order.setPaymentStatus(PaymentStatus.AMOUNT_MISMATCH.getValue());
                        order.setTrangThaiThanhToan("Sai lệch số tiền");
                        order.setTransactionId(ipnRequest.getTransactionData().getTransactionId());
                        order.setMaGiaoDich(ipnRequest.getTransactionData().getTransactionId());
                        order.setGatewayResponse("SePay: " + e.getMessage());
                        hoaDonRepository.save(order);

                        auditService.log(null, "HoaDon", Long.valueOf(order.getId()), "UPDATE",
                                order.getTrangThaiDonHang(), order.getTrangThaiDonHang(), "127.0.0.1",
                                "[PAYMENT_AMOUNT_MISMATCH] SePay payment amount mismatch: " + e.getMessage(), "SYSTEM");
                    }
                }
            } catch (Exception ex) {
                log.error("Error logging mismatched payment in controller: ", ex);
            }
            return ResponseEntity.ok(createSuccessResponse(e.getMessage()));
        } catch (OrderNotFoundException e) {
            log.error("SePay IPN Order Error: {}", e.getMessage());
            try {
                SepayIpnRequest ipnRequest = objectMapper.readValue(rawPayload, SepayIpnRequest.class);
                saveFailedTransaction(ipnRequest, null, "order_not_found", rawPayload);
            } catch (Exception ex) {
                log.error("Error logging order not found payment in controller: ", ex);
            }
            return ResponseEntity.ok(createSuccessResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("SePay IPN Unhandled Exception: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Internal server error."));
        }
    }

    @GetMapping("/api/payment/sepay/query/{maDonHang}")
    public ResponseEntity<Map<String, Object>> queryTransaction(
            @PathVariable("maDonHang") String maDonHang,
            HttpSession session) {

        // 1. Retrieve order first so the query can fall back safely for guest orders
        Optional<HoaDon> orderOpt = hoaDonRepository.findByMaDonHang(maDonHang);
        if (!orderOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Order not found."));
        }

        HoaDon order = orderOpt.get();

        // 2. Ownership check: signed-in member needs to match the order owner, while guest orders are allowed to poll by order code.
        boolean isDebug = sepayConfig.isDebug();
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        String sessionRole = (String) session.getAttribute("vaiTro");
        boolean isStaff = "NV".equals(sessionRole) || "QL".equals(sessionRole);
        boolean isGuestOrder = order.getKhachHang() == null 
                || order.getKhachHang().getTaiKhoan() == null
                || order.getKhachHang().getTaiKhoan().getTrangThaiTaiKhoan() == com.smashvn.shop.entity.AccountStatus.GUEST;

        Object allowedAccessesAttr = session.getAttribute("allowedGuestOrderAccesses");
        if (allowedAccessesAttr instanceof java.util.List<?>) {
            for (Object item : (java.util.List<?>) allowedAccessesAttr) {
                if (item instanceof com.smashvn.shop.controller.order.CheckoutController.GuestOrderAccess access) {
                    if (access.getOrderId().equals(order.getId()) && !access.isExpired()) {
                        isGuestOrder = true;
                        break;
                    }
                }
            }
        }

        if (!isDebug && !isStaff && idNguoiDung == null && !isGuestOrder) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("Unauthorized session. Please login."));
        }

        if (!isDebug && !isStaff && idNguoiDung != null && !isGuestOrder) {
            if (order.getKhachHang() == null || order.getKhachHang().getTaiKhoan() == null
                    || !order.getKhachHang().getTaiKhoan().getId().equals(idNguoiDung)) {
                log.warn("SePay Query: Ownership validation failed for user #{} trying to query order code {}", idNguoiDung, maDonHang);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access Denied."));
            }
        }

        // 3. Return standardized query API response
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("orderCode", order.getMaDonHang());

        String paymentStatus = order.getPaymentStatus();
        String trangThaiThanhToan = order.getTrangThaiThanhToan();

        // Check 180s expiration for pending orders (Skip if already paid)
        boolean isAlreadyPaid = "DA_THANH_TOAN".equalsIgnoreCase(trangThaiThanhToan) || "paid".equalsIgnoreCase(paymentStatus);
        if (!isAlreadyPaid && ("cho_thanh_toan".equalsIgnoreCase(order.getTrangThaiDonHang()) || "pending".equalsIgnoreCase(paymentStatus))) {
            if (order.getNgayTao() != null && LocalDateTime.now().isAfter(order.getNgayTao().plusSeconds(180))) {
                gioHangService.expirePendingOrder(order);
                paymentStatus = "expired";
                resp.put("paymentStatus", "expired");
                resp.put("orderStatus", "da_huy");
                resp.put("trangThaiThanhToan", "HỦY");
                resp.put("message", "Đã hết thời gian chờ thanh toán. Phiên thanh toán đã bị hủy.");
                return ResponseEntity.ok(resp);
            }
        }

        if ("DA_THANH_TOAN".equalsIgnoreCase(trangThaiThanhToan) || "DA_THANH_TOAN".equalsIgnoreCase(paymentStatus) || "paid".equalsIgnoreCase(paymentStatus)) {
            paymentStatus = "paid";
        } else if ("expired".equalsIgnoreCase(paymentStatus) || "da_huy".equalsIgnoreCase(order.getTrangThaiDonHang())) {
            paymentStatus = "expired";
        } else if ("CHO_THANH_TOAN".equalsIgnoreCase(trangThaiThanhToan) || "CHO_THANH_TOAN".equalsIgnoreCase(paymentStatus) || "pending".equalsIgnoreCase(paymentStatus) || paymentStatus == null) {
            paymentStatus = "pending";
        }

        resp.put("paymentStatus", paymentStatus);
        resp.put("orderStatus", order.getTrangThaiDonHang());
        resp.put("trangThaiThanhToan", trangThaiThanhToan);
        resp.put("isGuest", isGuestOrder);
        resp.put("orderId", order.getId());
        return ResponseEntity.ok(resp);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        String ip;
        if (xff != null && !xff.isEmpty()) {
            ip = xff.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        if (ip != null) {
            ip = ip.trim();
            if (ip.startsWith("::ffff:")) {
                ip = ip.substring(7);
            }
        }
        return ip;
    }

    private boolean isValidIp(String clientIp) {
        if (clientIp == null) {
            return false;
        }
        // Allow loopbacks in development
        if ("127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp) || "localhost".equals(clientIp)) {
            return true;
        }
        String allowedIpsStr = sepayConfig.getIpRanges();
        if (allowedIpsStr == null || allowedIpsStr.trim().isEmpty()) {
            return true;
        }
        String[] allowedIps = allowedIpsStr.split(",");
        for (String ip : allowedIps) {
            if (ip.trim().equals(clientIp)) {
                return true;
            }
        }
        return false;
    }

    private void saveFailedTransaction(SepayIpnRequest ipnRequest, HoaDon order, String status, String rawPayload) {
        try {
            SepayTransactionDto transactionDto = ipnRequest.getTransactionData();
            if (transactionDto == null || transactionDto.getTransactionId() == null) {
                return;
            }
            Optional<PaymentTransaction> existingTx = paymentTransactionRepository.findByTransactionId(transactionDto.getTransactionId());
            if (existingTx.isPresent()) {
                return;
            }
            PaymentTransaction tx = new PaymentTransaction();
            tx.setTransactionId(transactionDto.getTransactionId());
            tx.setOrder(order);
            tx.setAmount(transactionDto.getTransferAmount() != null ? transactionDto.getTransferAmount() : java.math.BigDecimal.ZERO);
            tx.setGateway(transactionDto.getGateway() != null ? transactionDto.getGateway() : "N/A");
            tx.setStatus(status);
            tx.setCreatedAt(LocalDateTime.now());
            if (sepayConfig.isDebug()) {
                tx.setRawPayload(rawPayload);
            }
            paymentTransactionRepository.saveAndFlush(tx);
        } catch (Exception e) {
            log.error("Failed to save failed transaction record: ", e);
        }
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        return resp;
    }

    private Map<String, Object> createSuccessResponse(String message, String transactionId) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        resp.put("transactionId", transactionId);
        return resp;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        return resp;
    }
}
