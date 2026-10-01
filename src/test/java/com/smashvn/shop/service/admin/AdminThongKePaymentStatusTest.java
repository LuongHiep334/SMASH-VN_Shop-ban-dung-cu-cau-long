package com.smashvn.shop.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AdminThongKePaymentStatusTest {

    @Test
    void cancelledOrderWithCompletedRefundKeepsRefundedPaymentStatus() {
        assertStatus("REFUNDED", "da_huy", "REFUNDED", "Đã hoàn tiền");
        assertStatus("DA_HOAN_TIEN", "da_huy", "REFUNDED", "Đã hoàn tiền");
        assertStatus("HOAN_TIEN", "da_huy", "REFUNDED", "Đã hoàn tiền");
    }

    @Test
    void cancelledOrderWaitingForRefundHasItsOwnVietnameseStatus() {
        assertStatus("CHO_HOAN_TIEN", "da_huy", "REFUND_PENDING", "Chờ hoàn tiền");
    }

    @Test
    void cancelledAndOtherPaymentStatesAreTranslatedWithoutLeakingRawCodes() {
        assertStatus("CANCELLED", "da_huy", "CANCELLED", "Đã hủy");
        assertStatus("FAILED", "da_xac_nhan", "FAILED", "Thất bại");
        assertStatus("EXPIRED", "cho_thanh_toan", "EXPIRED", "Hết hạn thanh toán");
        assertStatus("SOME_LEGACY_STATUS", "da_xac_nhan", "UNKNOWN", "Không xác định");
    }

    @Test
    void paymentStateTakesPriorityOverOrderState() {
        assertStatus("DA_THANH_TOAN", "da_huy", "PAID", "Thành công");
        assertStatus(null, "da_huy", "CANCELLED", "Đã hủy");
        assertStatus(null, "cho_xac_nhan", "PENDING", "Chờ thanh toán");
    }

    private void assertStatus(String rawStatus, String orderStatus, String expectedCode, String expectedLabel) {
        String actualCode = AdminThongKeService.standardizePaymentStatus(rawStatus, orderStatus);
        assertEquals(expectedCode, actualCode);
        assertEquals(expectedLabel, AdminThongKeService.paymentStatusLabel(actualCode));
    }
}
