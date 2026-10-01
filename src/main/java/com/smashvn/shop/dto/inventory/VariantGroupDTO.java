package com.smashvn.shop.dto.inventory;

import java.math.BigDecimal;
import java.util.List;
import com.smashvn.shop.entity.SanPhamChiTiet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantGroupDTO {
    private String attributeKey;
    private String displayTitle;
    private Integer representativeSpctId;
    private SanPhamChiTiet representativeSpct;
    private BigDecimal giaBan;
    private String trangThai;
    private boolean isDangBan;
    private Integer tongSoLuongTon;
    private Integer soLuongLoActive;
    private String hinhAnhUrl;
    private List<SanPhamChiTiet> danhSachSpctLo;
}
