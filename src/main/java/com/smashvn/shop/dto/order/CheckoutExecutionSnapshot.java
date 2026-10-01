package com.smashvn.shop.dto.order;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutExecutionSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer orderId;
    private String maDonHang;
    private CheckoutSource source;
    @Builder.Default
    private PendingCheckoutStatus status = PendingCheckoutStatus.READY;
    private Integer customerId;
    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    @Builder.Default
    private List<PurchasedItemSnapshot> items = new ArrayList<>();

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
