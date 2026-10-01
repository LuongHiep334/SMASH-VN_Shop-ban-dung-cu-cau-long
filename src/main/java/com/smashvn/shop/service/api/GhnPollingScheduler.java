package com.smashvn.shop.service.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.order.OrderViewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler Polling định kỳ gọi API GHN để đồng bộ trạng thái đơn hàng.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GhnPollingScheduler {

    private final HoaDonRepository hoaDonRepository;
    private final GhnService ghnService;
    private final GhnStatusMapper ghnStatusMapper;
    private final OrderViewService orderViewService;

    @Value("${ghn.polling.enabled:true}")
    private boolean pollingEnabled;

    @Value("${ghn.polling.batch-size:50}")
    private int batchSize;

    private int currentPage = 0;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(fixedDelayString = "${ghn.polling.interval-ms:600000}")
    public void pollGhnOrderStatuses() {
        if (!pollingEnabled) return;

        if (!running.compareAndSet(false, true)) {
            log.warn("[GHN_POLLING] Chu kỳ trước chưa hoàn tất, bỏ qua lần này.");
            return;
        }

        try {
            List<HoaDon> orders = hoaDonRepository
                    .findActiveShippingOrders(PageRequest.of(currentPage, batchSize));

            if (orders.size() < batchSize) {
                currentPage = 0;
            } else {
                currentPage++;
            }

            if (orders.isEmpty()) {
                log.info("[GHN_POLLING] Không có đơn hàng nào cần kiểm tra.");
                return;
            }

            log.info("[GHN_POLLING] Bắt đầu quét trang {}, {} đơn hàng",
                    currentPage == 0 ? 0 : currentPage - 1, orders.size());

            int updated = 0, skipped = 0, errors = 0;

            for (HoaDon hd : orders) {
                try {
                    String orderCode = hd.getGhnOrderCode();
                    if (orderCode != null && !orderCode.isBlank()) {
                        if (orderCode.startsWith("DEMO-GHN-")) {
                            log.debug("[GHN_POLLING] Skip polling GHN API for DEMO Fallback order #{}: {}", hd.getId(), orderCode);
                            skipped++;
                        } else {
                            Map<String, Object> trackingData = ghnService.trackOrder(orderCode);
                            if (trackingData != null) {
                                String ghnStatus = (String) trackingData.get("status");
                                String currentGhnStatus = hd.getGhnStatus();

                                if (ghnStatus != null && !ghnStatus.equalsIgnoreCase(currentGhnStatus)) {
                                    String internalStatus = ghnStatusMapper.mapToInternalStatus(ghnStatus);
                                    if (internalStatus != null) {
                                        orderViewService.applyShippingStatus(hd.getId(), internalStatus, ghnStatus);
                                        log.info("[GHN_POLLING] Cập nhật đơn #{}: {} → {} (GHN: {} → {})",
                                                hd.getId(), hd.getTrangThaiDonHang(), internalStatus,
                                                currentGhnStatus, ghnStatus);
                                        updated++;
                                    } else {
                                        skipped++;
                                    }
                                } else {
                                    skipped++;
                                }
                            }
                        }
                    } else {
                        skipped++;
                    }

                    // Check return & exchange shipments (GHN_RETURN, GHN_REJECT_RETURN, GHN_EXCHANGE)
                    try {
                        String returnCode = orderViewService.resolveGhnReturnOrderCode(hd.getId(), hd);
                        if (returnCode != null && !returnCode.isBlank()) {
                            if (returnCode.startsWith("DEMO-GHN-RETURN-")) {
                                log.debug("[GHN_RETURN_FALLBACK] Skip GHN tracking for Demo Return code {} for HoaDon #{}", returnCode, hd.getId());
                            } else {
                                Map<String, Object> rData = ghnService.trackOrder(returnCode);
                                if (rData != null) {
                                    String rStatus = (String) rData.get("status");
                                    com.smashvn.shop.entity.ReturnStatus newRetStatus = ghnStatusMapper.mapToReturnStatus(rStatus);
                                    if (newRetStatus != null) {
                                        orderViewService.updateReturnStatusFromGhn(hd.getId(), newRetStatus, rStatus, "GHN_POLLING");
                                    }
                                }
                            }
                        }

                        String exchangeCode = orderViewService.resolveGhnExchangeOrderCode(hd.getId());
                        if (exchangeCode != null && !exchangeCode.isBlank()) {
                            Map<String, Object> exData = ghnService.trackOrder(exchangeCode);
                            if (exData != null) {
                                String exStatus = (String) exData.get("status");
                                com.smashvn.shop.entity.ReturnStatus targetExchangeStatus = "delivered".equalsIgnoreCase(exStatus)
                                        ? com.smashvn.shop.entity.ReturnStatus.EXCHANGED
                                        : com.smashvn.shop.entity.ReturnStatus.EXCHANGE_SHIPPING;
                                orderViewService.updateExchangeStatusFromGhn(hd.getId(), targetExchangeStatus, exStatus, "GHN_POLLING");
                            }
                        }

                        String rejectCode = orderViewService.resolveGhnRejectReturnCode(hd.getId());
                        if (rejectCode != null && !rejectCode.isBlank()) {
                            Map<String, Object> rejData = ghnService.trackOrder(rejectCode);
                            if (rejData != null) {
                                String rejStatus = (String) rejData.get("status");
                                if ("delivered".equalsIgnoreCase(rejStatus) || "returned".equalsIgnoreCase(rejStatus) || "returned_to_sender".equalsIgnoreCase(rejStatus)) {
                                    orderViewService.handleRejectReturnDeliveryFromGhn(hd.getId(), rejStatus, "GHN_POLLING");
                                }
                            }
                        }
                    } catch (Exception rEx) {
                        log.warn("[GHN_POLLING] Lỗi kiểm tra vận đơn hoàn/trả đơn #{}: {}", hd.getId(), rEx.getMessage());
                    }
                } catch (Exception e) {
                    log.warn("[GHN_POLLING] Lỗi kiểm tra đơn #{}: {}", hd.getId(), e.getMessage());
                    errors++;
                }
            }

            log.info("[GHN_POLLING] Hoàn tất: {} cập nhật, {} bỏ qua, {} lỗi", updated, skipped, errors);

        } finally {
            running.set(false);
        }
    }
}
