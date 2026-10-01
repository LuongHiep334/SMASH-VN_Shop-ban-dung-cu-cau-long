package com.smashvn.shop.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    private Integer sourceLineId;         // ID của HoaDonChiTiet tạm (provisional) nếu có
    private Integer representativeSpctId; // ID SPCT đại diện
    private int quantity;                  // Số lượng cần mua/trừ tồn
}
