package com.smashvn.shop.dto.order;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FullCartCheckoutResult {
    private String trangThai; // "ok" or "error"
    private String thongBao;  // Error message or success message
    private String message;   // Alias for thongBao
    private String checkoutUrl;
    private String checkoutToken;
    private Integer itemCount;
    private Integer totalQuantity;
    private List<InvalidCartItemView> invalidItems;

    public boolean isSuccess() {
        return "ok".equalsIgnoreCase(trangThai);
    }
}
