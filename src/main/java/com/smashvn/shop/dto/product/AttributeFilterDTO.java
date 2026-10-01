package com.smashvn.shop.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeFilterDTO {
    private Integer thuocTinhId;
    private String tenThuocTinh;
    
    @Builder.Default
    private List<AttributeValueDTO> options = new ArrayList<>();
}
