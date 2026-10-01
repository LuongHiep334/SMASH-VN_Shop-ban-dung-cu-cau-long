package com.smashvn.shop.dto.chatbot;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatProductResponse {
    private Integer id;
    private String name;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String brand;
    private String shortDescription;
    private String imageUrl;
    private String productUrl;
    private String detailUrl;

    public String getDetailUrl() {
        if (detailUrl != null) {
            return detailUrl;
        }
        if (productUrl != null) {
            return productUrl;
        }
        return id != null ? "/san-pham/" + id : null;
    }

    public String getProductUrl() {
        if (productUrl != null) {
            return productUrl;
        }
        if (detailUrl != null) {
            return detailUrl;
        }
        return id != null ? "/san-pham/" + id : null;
    }
}
