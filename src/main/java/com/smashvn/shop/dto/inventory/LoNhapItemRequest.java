package com.smashvn.shop.dto.inventory;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoNhapItemRequest {
    private Integer representativeSpctId;
    private Integer soLuongNhap;
    private BigDecimal giaNhap;
}
