package com.smashvn.shop.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopFilterRequest {
    private String keyword;
    private Integer categoryId;
    private Integer brandId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double rating;
    
    // Map of thuocTinhId -> List of selected string values
    @Builder.Default
    private Map<Integer, List<String>> attributes = new HashMap<>();
    
    // Legacy trongLuong parameter support
    private List<String> legacyTrongLuong;
    
    @Builder.Default
    private String sort = "newest";
    
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 12;
}
