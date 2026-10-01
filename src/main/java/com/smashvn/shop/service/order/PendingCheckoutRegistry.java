package com.smashvn.shop.service.order;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.smashvn.shop.dto.order.CheckoutExecutionSnapshot;
import com.smashvn.shop.dto.order.PendingCheckoutStatus;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PendingCheckoutRegistry {

    private final Map<String, CheckoutExecutionSnapshot> registry = new ConcurrentHashMap<>();

    public void registerSnapshot(CheckoutExecutionSnapshot snapshot) {
        if (snapshot == null || snapshot.getMaDonHang() == null || snapshot.getMaDonHang().isBlank()) {
            throw new IllegalArgumentException("Thông tin đơn hàng thanh toán chờ xử lý không hợp lệ hoặc mã đơn hàng trống.");
        }
        String key = snapshot.getMaDonHang().trim().toUpperCase();
        if (snapshot.getCreatedAt() == null) {
            snapshot.setCreatedAt(LocalDateTime.now());
        }
        if (snapshot.getExpiresAt() == null) {
            snapshot.setExpiresAt(snapshot.getCreatedAt().plusMinutes(30));
        }
        snapshot.setStatus(PendingCheckoutStatus.READY);
        registry.put(key, snapshot);
        log.info("[PENDING_REGISTRY] Registered pending checkout snapshot for maDonHang: {}", key);
    }

    public CheckoutExecutionSnapshot peekSnapshot(String maDonHang) {
        if (maDonHang == null || maDonHang.isBlank()) {
            return null;
        }
        String key = maDonHang.trim().toUpperCase();
        CheckoutExecutionSnapshot snapshot = registry.get(key);
        if (snapshot != null && snapshot.isExpired()) {
            snapshot.setStatus(PendingCheckoutStatus.EXPIRED);
        }
        return snapshot;
    }

    public synchronized CheckoutExecutionSnapshot claimSnapshot(String maDonHang) {
        if (maDonHang == null || maDonHang.isBlank()) {
            return null;
        }
        String key = maDonHang.trim().toUpperCase();
        CheckoutExecutionSnapshot snapshot = registry.get(key);
        if (snapshot == null) {
            return null;
        }
        if (snapshot.isExpired()) {
            snapshot.setStatus(PendingCheckoutStatus.EXPIRED);
            return null;
        }
        if (snapshot.getStatus() == PendingCheckoutStatus.READY) {
            snapshot.setStatus(PendingCheckoutStatus.PROCESSING);
            log.info("[PENDING_REGISTRY] Atomic claim successful for maDonHang: {}", key);
            return snapshot;
        }
        log.warn("[PENDING_REGISTRY] Atomic claim failed for maDonHang: {} (current status: {})", key, snapshot.getStatus());
        return null;
    }

    public synchronized void releaseSnapshot(String maDonHang) {
        if (maDonHang == null || maDonHang.isBlank()) {
            return;
        }
        String key = maDonHang.trim().toUpperCase();
        CheckoutExecutionSnapshot snapshot = registry.get(key);
        if (snapshot != null && snapshot.getStatus() == PendingCheckoutStatus.PROCESSING) {
            snapshot.setStatus(PendingCheckoutStatus.READY);
            log.info("[PENDING_REGISTRY] Released snapshot back to READY for maDonHang: {}", key);
        }
    }

    public synchronized void completeAndRemove(String maDonHang) {
        if (maDonHang == null || maDonHang.isBlank()) {
            return;
        }
        String key = maDonHang.trim().toUpperCase();
        CheckoutExecutionSnapshot snapshot = registry.remove(key);
        if (snapshot != null) {
            snapshot.setStatus(PendingCheckoutStatus.COMPLETED);
            log.info("[PENDING_REGISTRY] Completed and removed snapshot for maDonHang: {}", key);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredSnapshots() {
        registry.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired();
            if (expired) {
                log.info("[PENDING_REGISTRY] Cleaning up expired snapshot for maDonHang: {}", entry.getKey());
            }
            return expired;
        });
    }

    public void clearAllForTest() {
        registry.clear();
    }
}
