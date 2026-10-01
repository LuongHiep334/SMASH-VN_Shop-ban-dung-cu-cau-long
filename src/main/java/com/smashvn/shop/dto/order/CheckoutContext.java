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
public class CheckoutContext implements Serializable {
    private static final long serialVersionUID = 1L;

    private String token;
    private CheckoutSource source;
    @Builder.Default
    private CheckoutContextStatus status = CheckoutContextStatus.READY;
    private Integer customerId;
    private String sessionId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    @lombok.Setter(lombok.AccessLevel.NONE)
    private CompletedCheckout completedCheckout;
    @Builder.Default
    private List<CheckoutItemContext> items = new ArrayList<>();

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public synchronized boolean tryClaim() {
        if (this.status != CheckoutContextStatus.READY) {
            return false;
        }
        if (isExpired()) {
            this.status = CheckoutContextStatus.EXPIRED;
            return false;
        }
        if (this.status == CheckoutContextStatus.READY) {
            this.status = CheckoutContextStatus.PROCESSING;
            return true;
        }
        return false;
    }

    public synchronized void release() {
        if (this.status == CheckoutContextStatus.PROCESSING) {
            this.status = CheckoutContextStatus.READY;
        }
    }

    public synchronized void consume() {
        this.status = CheckoutContextStatus.CONSUMED;
    }

    public synchronized void complete(CompletedCheckout result) {
        this.completedCheckout = java.util.Objects.requireNonNull(result);
        this.status = CheckoutContextStatus.CONSUMED;
    }

    public synchronized CompletedCheckout getCompletedCheckout() {
        return completedCheckout;
    }

    public synchronized CheckoutContextStatus getStatus() {
        return status;
    }
}
