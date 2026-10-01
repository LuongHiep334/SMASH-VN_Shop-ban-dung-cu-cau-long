package com.smashvn.shop.dto.order;

import java.util.List;
import com.smashvn.shop.entity.HoaDon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreationResult {
    private HoaDon hoaDon;
    private List<PurchasedItemSnapshot> purchasedItems;
}
