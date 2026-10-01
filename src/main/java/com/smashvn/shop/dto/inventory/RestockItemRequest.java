package com.smashvn.shop.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestockItemRequest {
    private Integer idHoaDonChiTiet;
    private Integer idSanPhamChiTiet;
    private int quantityToRestock;
    private boolean conBanDuoc; // Flag từ form request/ReturnStatus UI (true = hoàn kho, false = hàng hỏng)
}
