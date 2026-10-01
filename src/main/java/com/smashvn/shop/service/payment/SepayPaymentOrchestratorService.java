package com.smashvn.shop.service.payment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.smashvn.shop.dto.payment.SepayIpnRequest;
import com.smashvn.shop.dto.payment.SepayTransactionDto;
import com.smashvn.shop.dto.order.CheckoutExecutionSnapshot;
import com.smashvn.shop.dto.order.CheckoutSource;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.order.GuestCartService;
import com.smashvn.shop.service.order.PendingCheckoutRegistry;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayPaymentOrchestratorService {

    private final PendingCheckoutRegistry pendingCheckoutRegistry;
    private final SepayGatewayService sepayGatewayService;
    private final GioHangService gioHangService;
    private final GuestCartService guestCartService;
    private final HoaDonRepository hoaDonRepository;
    private final com.smashvn.shop.repository.HoaDonChiTietRepository hoaDonChiTietRepository;

    public Map<String, Object> orchestrateSimulatedPayment(
            String maDonHang,
            BigDecimal amount,
            SepayIpnRequest ipnRequest,
            String rawJson,
            HttpSession session) throws Exception {

        if (maDonHang == null || maDonHang.isBlank()) {
            throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");
        }

        String normalizedCode = maDonHang.replace("-", "").replace("_", "").trim().toUpperCase();
        HoaDon order = hoaDonRepository.findByMaDonHang(maDonHang)
                .orElseGet(() -> hoaDonRepository.findByMaDonHangOrNormalized(maDonHang, normalizedCode).orElse(null));


        // 1. Try atomic claim on snapshot
        CheckoutExecutionSnapshot snapshot = pendingCheckoutRegistry.claimSnapshot(maDonHang);

        if (snapshot == null && order != null && ("CHO_THANH_TOAN".equalsIgnoreCase(order.getTrangThaiThanhToan()) || "cho_thanh_toan".equalsIgnoreCase(order.getTrangThaiDonHang()))) {
            // In debug/test mode, create dynamic fallback snapshot from saved HoaDonChiTiet
            List<com.smashvn.shop.dto.order.PurchasedItemSnapshot> fallbackItems = new java.util.ArrayList<>();
            List<com.smashvn.shop.entity.HoaDonChiTiet> hdcts = hoaDonChiTietRepository.findByHoaDon_Id(order.getId());
            if (hdcts != null) {
                for (com.smashvn.shop.entity.HoaDonChiTiet item : hdcts) {
                    if (item.getSanPhamChiTiet() != null) {
                        fallbackItems.add(com.smashvn.shop.dto.order.PurchasedItemSnapshot.builder()
                                .idSanPhamChiTiet(item.getSanPhamChiTiet().getId())
                                .soLuongDaMua(item.getSoLuong())
                                .fromCart(true)
                                .build());
                    }
                }
            }
            snapshot = CheckoutExecutionSnapshot.builder()
                    .orderId(order.getId())
                    .maDonHang(order.getMaDonHang())
                    .source(CheckoutSource.CART)
                    .status(com.smashvn.shop.dto.order.PendingCheckoutStatus.READY)
                    .customerId(order.getKhachHang() != null && order.getKhachHang().getTaiKhoan() != null ? order.getKhachHang().getTaiKhoan().getId() : null)
                    .sessionId(session != null ? session.getId() : null)
                    .items(fallbackItems)
                    .build();
            pendingCheckoutRegistry.registerSnapshot(snapshot);
            snapshot = pendingCheckoutRegistry.claimSnapshot(maDonHang);
        }

        if (snapshot == null) {

            // Check if order was already paid
            if (order != null && ("DA_THANH_TOAN".equalsIgnoreCase(order.getTrangThaiThanhToan()) || "paid".equalsIgnoreCase(order.getPaymentStatus()))) {
                log.info("[ORCHESTRATOR] Order {} already paid and snapshot missing. Returning idempotent success.", maDonHang);
                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("message", "Đơn hàng đã được thanh toán trước đó.");
                return resp;
            }
            // Pending order but snapshot lost/expired
            log.warn("[ORCHESTRATOR] Snapshot for pending order {} lost or expired.", maDonHang);
            throw new IllegalStateException("Phiên thanh toán đã hết hạn hoặc bị mất do máy chủ khởi động lại. Vui lòng thực hiện lại đơn hàng.");
        }

        try {
            // 2. Guest session validation if applicable
            if (snapshot.getCustomerId() == null) {
                if (session == null || snapshot.getSessionId() == null || !snapshot.getSessionId().equals(session.getId())) {
                    log.warn("[ORCHESTRATOR] Guest session mismatch for order {}. Expected {}, actual {}",
                            maDonHang, snapshot.getSessionId(), (session != null ? session.getId() : "null"));
                    throw new AccessDeniedException("Phiên làm việc khách vãng lai không khớp với đơn hàng thanh toán.");
                }
            }

            // 3. Process IPN & payment (deduct stock, set payment status)
            Map<String, Object> result = sepayGatewayService.handleIpn(ipnRequest, rawJson);

            // 4. Clean up cart based on CheckoutSource
            if (snapshot.getSource() == CheckoutSource.CART || snapshot.getSource() == CheckoutSource.QUICK_ADD) {
                if (snapshot.getCustomerId() != null) {
                    gioHangService.removePurchasedItemsFromCart(snapshot.getCustomerId(), snapshot.getItems());
                } else if (session != null) {
                    guestCartService.removePurchasedItemsFromGuestCart(session, snapshot.getItems());
                }
            }

            // 5. Complete and remove snapshot
            pendingCheckoutRegistry.completeAndRemove(maDonHang);
            log.info("[ORCHESTRATOR] Payment orchestration completed successfully for order {}", maDonHang);
            return result;

        } catch (Exception e) {
            log.error("[ORCHESTRATOR] Exception during payment orchestration for order {}: {}. Releasing snapshot to READY.", maDonHang, e.getMessage());
            pendingCheckoutRegistry.releaseSnapshot(maDonHang);
            throw e;
        }
    }
}
