package com.smashvn.shop.dto.order;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchasedItemSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer cartItemId;
    private Integer idSanPhamChiTiet;
    private Integer soLuongDaMua;
    private boolean fromCart;
}
