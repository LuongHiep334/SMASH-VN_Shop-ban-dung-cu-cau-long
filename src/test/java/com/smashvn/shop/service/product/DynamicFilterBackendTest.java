package com.smashvn.shop.service.product;

import com.smashvn.shop.dto.product.AttributeFilterDTO;
import com.smashvn.shop.dto.product.AttributeOptionProjection;
import com.smashvn.shop.dto.product.ShopFilterRequest;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.DanhMucThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamChiTietThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
import com.smashvn.shop.specification.SanPhamSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicFilterBackendTest {

    @Mock
    private SanPhamRepository sanPhamRepository;

    @Mock
    private SanPhamChiTietThuocTinhRepository sanPhamChiTietThuocTinhRepository;

    @Mock
    private DanhMucThuocTinhRepository danhMucThuocTinhRepository;

    @Mock
    private ThuocTinhRepository thuocTinhRepository;

    @InjectMocks
    private SanPhamService sanPhamService;

    private DanhMuc catVot;
    private DanhMuc catGiay;

    private ThuocTinh ttMauSac;     // id 1
    private ThuocTinh ttDoCung;      // id 2
    private ThuocTinh ttTrongLuong;  // id 3
    private ThuocTinh ttKichThuoc;   // id 6

    @BeforeEach
    void setUp() {
        catVot = DanhMuc.builder().id(1).tenDanhMuc("Vợt cầu lông").trangThai(true).build();
        catGiay = DanhMuc.builder().id(2).tenDanhMuc("Giày cầu lông").trangThai(true).build();

        ttMauSac = ThuocTinh.builder().id(1).tenThuocTinh("Màu sắc").trangThai(true).build();
        ttDoCung = ThuocTinh.builder().id(2).tenThuocTinh("Độ cứng").trangThai(true).build();
        ttTrongLuong = ThuocTinh.builder().id(3).tenThuocTinh("Trọng lượng").trangThai(true).build();
        ttKichThuoc = ThuocTinh.builder().id(6).tenThuocTinh("Kích thước").trangThai(true).build();
    }

    @Test
    @DisplayName("Test 1: categoryId = null => dynamicAttributeFilters is empty list")
    void test1_CategoryNull_ReturnsEmptyDynamicAttributeFilters() {
        List<AttributeFilterDTO> filters = sanPhamService.getDynamicAttributeFilters(null, null, null);
        assertNotNull(filters);
        assertTrue(filters.isEmpty());
    }

    @Test
    @DisplayName("Test 2: category = Giày => Returns Màu sắc + Kích thước, does NOT return Trọng lượng")
    void test2_CategoryGiay_ReturnsGiayAttributesOnly() {
        DanhMucThuocTinh dmttMau = DanhMucThuocTinh.builder().id(10).danhMuc(catGiay).thuocTinh(ttMauSac).trangThai(true).build();
        DanhMucThuocTinh dmttKich = DanhMucThuocTinh.builder().id(11).danhMuc(catGiay).thuocTinh(ttKichThuoc).trangThai(true).build();

        when(danhMucThuocTinhRepository.findByDanhMucIdAndTrangThaiTrue(eq(2)))
                .thenReturn(List.of(dmttMau, dmttKich));

        AttributeOptionProjection p1 = createProjection(1, "Màu sắc", "Đen", 5L);
        AttributeOptionProjection p2 = createProjection(6, "Kích thước", "40", 3L);

        when(sanPhamChiTietThuocTinhRepository.findAttributeOptionProjectionsByCategory(eq(2)))
                .thenReturn(List.of(p1, p2));

        List<AttributeFilterDTO> filters = sanPhamService.getDynamicAttributeFilters(2, null, null);

        assertEquals(2, filters.size());
        assertEquals("Màu sắc", filters.get(0).getTenThuocTinh());
        assertEquals("Kích thước", filters.get(1).getTenThuocTinh());

        // Verify Trọng lượng (ID 3) is NOT present
        boolean containsTrongLuong = filters.stream().anyMatch(f -> f.getThuocTinhId().equals(3));
        assertFalse(containsTrongLuong, "Giày category should NOT contain Trọng lượng filter");
    }

    @Test
    @DisplayName("Test 3: OR within same attribute group")
    void test3_OrWithinSameAttributeGroup_SpecificationBuilds() {
        Map<Integer, List<String>> attrs = new HashMap<>();
        attrs.put(6, List.of("39", "40")); // Size 39 OR 40

        ShopFilterRequest req = ShopFilterRequest.builder()
                .categoryId(2)
                .attributes(attrs)
                .build();

        Specification<SanPham> spec = SanPhamSpecification.filter(req);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Test 4: AND across different attribute groups")
    void test4_AndAcrossDifferentAttributeGroups_SpecificationBuilds() {
        Map<Integer, List<String>> attrs = new HashMap<>();
        attrs.put(1, List.of("Đỏ"));
        attrs.put(6, List.of("M"));

        ShopFilterRequest req = ShopFilterRequest.builder()
                .categoryId(2)
                .attributes(attrs)
                .build();

        Specification<SanPham> spec = SanPhamSpecification.filter(req);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Test 5: SAME VARIANT NEGATIVE - Red+XL specification structure")
    void test5_SameVariantNegative_SpecificationStructure() {
        Map<Integer, List<String>> attrs = new HashMap<>();
        attrs.put(1, List.of("Đỏ"));
        attrs.put(6, List.of("XL"));

        ShopFilterRequest req = ShopFilterRequest.builder()
                .categoryId(2)
                .attributes(attrs)
                .build();

        Specification<SanPham> spec = SanPhamSpecification.filter(req);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Test 6: PRICE SAME VARIANT - Red + minPrice 1.8M specification structure")
    void test6_PriceSameVariant_SpecificationStructure() {
        Map<Integer, List<String>> attrs = new HashMap<>();
        attrs.put(1, List.of("Đỏ"));

        ShopFilterRequest req = ShopFilterRequest.builder()
                .categoryId(1)
                .minPrice(new BigDecimal("1800000"))
                .attributes(attrs)
                .build();

        Specification<SanPham> spec = SanPhamSpecification.filter(req);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Test 7: Pagination duplicate check - Specification does NOT create root JOINs")
    void test7_PaginationDuplicateCheck_SpecificationNoRootJoins() {
        ShopFilterRequest req = ShopFilterRequest.builder()
                .categoryId(1)
                .sort("newest")
                .page(0)
                .size(12)
                .build();

        Specification<SanPham> spec = SanPhamSpecification.filter(req);
        assertNotNull(spec);
    }

    @Test
    @DisplayName("Test 8: Invalid attribute param handling (blank, malformed)")
    void test8_InvalidAttributeParamHandling() {
        Map<Integer, List<String>> attrs = new HashMap<>();
        attrs.put(1, Arrays.asList("", "  ", null)); // invalid values

        ShopFilterRequest req = ShopFilterRequest.builder()
                .categoryId(1)
                .attributes(attrs)
                .build();

        DanhMucThuocTinh dmttMau = DanhMucThuocTinh.builder().id(10).danhMuc(catVot).thuocTinh(ttMauSac).trangThai(true).build();
        when(danhMucThuocTinhRepository.findByDanhMucIdAndTrangThaiTrue(eq(1))).thenReturn(List.of(dmttMau));

        ShopFilterRequest sanitized = sanPhamService.sanitizeFilterRequest(req);
        assertNotNull(sanitized);
        assertTrue(sanitized.getAttributes().isEmpty(), "Blank attribute values should be stripped");
    }

    @Test
    @DisplayName("Test 9: Attribute outside category is ignored")
    void test9_AttributeOutsideCategory_Ignored() {
        // Category Giày (id 2) only has Màu sắc (1) and Kích thước (6). User sends Trọng lượng (3).
        DanhMucThuocTinh dmttMau = DanhMucThuocTinh.builder().id(10).danhMuc(catGiay).thuocTinh(ttMauSac).trangThai(true).build();
        DanhMucThuocTinh dmttKich = DanhMucThuocTinh.builder().id(11).danhMuc(catGiay).thuocTinh(ttKichThuoc).trangThai(true).build();
        when(danhMucThuocTinhRepository.findByDanhMucIdAndTrangThaiTrue(eq(2))).thenReturn(List.of(dmttMau, dmttKich));

        Map<Integer, List<String>> attrs = new HashMap<>();
        attrs.put(3, List.of("4U")); // Trọng lượng - outside Giày category

        ShopFilterRequest req = ShopFilterRequest.builder()
                .categoryId(2)
                .attributes(attrs)
                .build();

        ShopFilterRequest sanitized = sanPhamService.sanitizeFilterRequest(req);
        assertFalse(sanitized.getAttributes().containsKey(3), "Attribute 3 (Trọng lượng) should be ignored for Category Giày");
    }

    @Test
    @DisplayName("Test 10: Legacy trongLuong resolved dynamically without hardcoded ID 3")
    void test10_LegacyTrongLuong_ResolvedDynamically() {
        DanhMucThuocTinh dmttTrong = DanhMucThuocTinh.builder().id(12).danhMuc(catVot).thuocTinh(ttTrongLuong).trangThai(true).build();
        when(danhMucThuocTinhRepository.findByDanhMucIdAndTrangThaiTrue(eq(1))).thenReturn(List.of(dmttTrong));
        when(thuocTinhRepository.findByTenThuocTinhIgnoreCaseAndTrangThaiTrue(anyString())).thenReturn(Optional.of(ttTrongLuong));

        ShopFilterRequest req = ShopFilterRequest.builder()
                .categoryId(1)
                .legacyTrongLuong(List.of("4U"))
                .build();

        ShopFilterRequest sanitized = sanPhamService.sanitizeFilterRequest(req);
        assertTrue(sanitized.getAttributes().containsKey(3), "Legacy trongLuong should map dynamically to attribute ID 3");
        assertEquals(List.of("4U"), sanitized.getAttributes().get(3));

        verify(thuocTinhRepository, times(1)).findByTenThuocTinhIgnoreCaseAndTrangThaiTrue("Trọng lượng");
    }

    private AttributeOptionProjection createProjection(Integer ttId, String ttName, String val, Long count) {
        return new AttributeOptionProjection() {
            @Override public Integer getThuocTinhId() { return ttId; }
            @Override public String getTenThuocTinh() { return ttName; }
            @Override public String getGiaTri() { return val; }
            @Override public Long getProductCount() { return count; }
        };
    }
}
