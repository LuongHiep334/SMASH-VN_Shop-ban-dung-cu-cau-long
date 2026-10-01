package com.smashvn.shop.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvalidCartItemView {
    private Integer idSanPhamChiTiet;
    private String tenSanPham;
    private Integer requestedQuantity;
    private Integer stockQuantity;
    private String reason;

    // Backward/Vietnamese compatibility getters
    public Integer getSoLuongYeuCau() {
        return requestedQuantity;
    }

    public Integer getSoLuongTon() {
        return stockQuantity;
    }

    public String getLyDo() {
        return reason;
    }
}
