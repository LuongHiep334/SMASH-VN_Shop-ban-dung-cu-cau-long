package com.smashvn.shop.controller.api;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerSearchApiTest {
    private final SanPhamRepository products = mock(SanPhamRepository.class);
    private final SanPhamChiTietRepository variants = mock(SanPhamChiTietRepository.class);
    private final DanhMucRepository categories = mock(DanhMucRepository.class);
    private final ThuongHieuRepository brands = mock(ThuongHieuRepository.class);
    private final SearchApiController controller = new SearchApiController(products, variants, categories, brands);

    @Test void shortKeywordDoesNotQueryCatalog() {
        assertTrue(controller.searchProducts(" a ").getBody().isEmpty());
        verifyNoInteractions(products, variants);
    }

    @Test void textOnlyResultsHaveRepresentativePriceAndNoSkuOrImage() {
        SanPham product = SanPham.builder().id(27).tenSanPham("Vợt Yonex 700").build();
        when(products.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(product)));
        when(variants.findActiveBySanPham_Id(27)).thenReturn(List.of(
                SanPhamChiTiet.builder().giaBan(BigDecimal.valueOf(100)).soLuongTon(0).build(),
                SanPhamChiTiet.builder().giaBan(BigDecimal.valueOf(200)).soLuongTon(2).build()));
        var body = controller.searchProducts("yonex 700").getBody();
        assertEquals(1, body.size());
        assertEquals(BigDecimal.valueOf(200), body.get(0).get("giaSauGiam"));
        assertEquals(true, body.get(0).get("conHang"));
        assertFalse(body.get(0).containsKey("sku"));
        assertFalse(body.get(0).containsKey("maSanPham"));
        assertFalse(body.get(0).containsKey("hinhAnh"));
        verify(products).findAll(any(Specification.class), eq(PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"))));
    }

    @Test void popularKeepsExistingSourceAndReturnsSoldOutState() {
        SanPham product = SanPham.builder().id(12).tenSanPham("Vợt Victor").build();
        when(categories.findByTrangThaiTrue()).thenReturn(List.of());
        when(brands.findByTrangThaiTrue()).thenReturn(List.of());
        when(products.findBestSellers(PageRequest.of(0, 4))).thenReturn(List.of(product));
        when(variants.findActiveBySanPham_Id(12)).thenReturn(List.of());
        var entries = (List<?>) controller.getPopularKeywords().getBody().get("sanPhamNoiBat");
        var entry = (java.util.Map<?, ?>) entries.get(0);
        assertEquals(false, entry.get("conHang"));
        assertNull(entry.get("giaSauGiam"));
        assertFalse(entry.containsKey("hinhAnh"));
    }
}
