package com.smashvn.shop.dto.chatbot;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductSearchCriteria {
    private String keyword;
    private String keyword2;
    private String keyword3;
    private String brandName;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String color;
    private String weight;
    private String tension;
    private Boolean inStock = true;
    private Boolean active = true;
    private Integer maxResults = 5;
}
