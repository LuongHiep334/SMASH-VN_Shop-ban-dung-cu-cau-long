package com.smashvn.shop.dto.inventory;

import java.util.List;

public record AllocationResult(
        AllocationStatus status,
        List<LotAllocation> allocations,
        String message
) {
}
