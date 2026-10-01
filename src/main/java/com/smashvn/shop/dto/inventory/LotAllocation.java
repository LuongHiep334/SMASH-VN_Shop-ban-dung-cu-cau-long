package com.smashvn.shop.dto.inventory;

import com.smashvn.shop.entity.SanPhamChiTiet;

public record LotAllocation(
        Integer sourceLineId,
        Integer representativeSpctId,
        SanPhamChiTiet allocatedSpct,
        int quantityAllocated
) {
}
