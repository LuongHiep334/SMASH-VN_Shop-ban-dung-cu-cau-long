package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.DanhMucThuocTinh;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.DanhMucThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamChiTietThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DanhMucServiceTest {

    @Mock
    private DanhMucRepository danhMucRepository;
    @Mock
    private DanhMucThuocTinhRepository danhMucThuocTinhRepository;
    @Mock
    private ThuocTinhRepository thuocTinhRepository;
    @Mock
    private SanPhamRepository sanPhamRepository;
    @Mock
    private SanPhamChiTietThuocTinhRepository sanPhamChiTietThuocTinhRepository;

    @InjectMocks
    private DanhMucService service;

    @Test
    void categoryWithProductsCanBeRenamedWithinSameBusinessType() {
        DanhMuc category = categoryWithAttribute("Vợt cầu lông");
        when(danhMucRepository.findById(1)).thenReturn(Optional.of(category));
        when(sanPhamRepository.existsByDanhMucId(1)).thenReturn(true);
        when(danhMucRepository.save(any(DanhMuc.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DanhMuc updated = service.suaDanhMuc(1, "Vợt trẻ em", null, false);

        assertEquals("Vợt trẻ em", updated.getTenDanhMuc());
        assertTrue(updated.getDanhMucThuocTinhs().get(0).getTrangThai());
    }

    @Test
    void categoryWithProductsCannotBeRenamedToAnotherBusinessType() {
        DanhMuc category = categoryWithAttribute("Vợt cầu lông");
        when(danhMucRepository.findById(1)).thenReturn(Optional.of(category));
        when(sanPhamRepository.existsByDanhMucId(1)).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.suaDanhMuc(1, "Racket cao cấp", null, false));

        assertTrue(error.getMessage().contains("đang được gán cho sản phẩm"));
        assertEquals("Vợt cầu lông", category.getTenDanhMuc());
        verify(danhMucRepository, never()).save(any());
    }

    @Test
    void failedAttributeLoadPathPreservesExistingMappings() {
        DanhMuc category = categoryWithAttribute("Vợt cầu lông");
        when(danhMucRepository.findById(1)).thenReturn(Optional.of(category));
        when(sanPhamRepository.existsByDanhMucId(1)).thenReturn(false);
        when(danhMucRepository.save(any(DanhMuc.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.suaDanhMuc(1, "Vợt cầu lông", null, false);

        assertTrue(category.getDanhMucThuocTinhs().get(0).getTrangThai());
        verify(sanPhamChiTietThuocTinhRepository, never())
                .existsByThuocTinh_IdAndSanPhamChiTiet_SanPham_DanhMuc_Id(any(), any());
    }

    @Test
    void usedVariantAttributeCannotBeRemovedFromCategory() {
        DanhMuc category = categoryWithAttribute("Vợt cầu lông");
        when(danhMucRepository.findById(1)).thenReturn(Optional.of(category));
        when(sanPhamChiTietThuocTinhRepository
                .existsByThuocTinh_IdAndSanPhamChiTiet_SanPham_DanhMuc_Id(10, 1))
                .thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.suaDanhMuc(1, "Vợt cầu lông", List.of(), true));

        assertTrue(error.getMessage().contains("đang sử dụng thuộc tính này"));
        assertFalse(category.getDanhMucThuocTinhs().isEmpty());
        assertTrue(category.getDanhMucThuocTinhs().get(0).getTrangThai());
        verify(danhMucRepository, never()).save(any());
    }

    private DanhMuc categoryWithAttribute(String name) {
        ThuocTinh color = ThuocTinh.builder()
                .id(10)
                .tenThuocTinh("Màu sắc")
                .trangThai(true)
                .build();
        DanhMuc category = new DanhMuc();
        category.setId(1);
        category.setTenDanhMuc(name);
        category.setTrangThai(true);
        category.getDanhMucThuocTinhs().add(DanhMucThuocTinh.builder()
                .id(100)
                .danhMuc(category)
                .thuocTinh(color)
                .trangThai(true)
                .build());
        return category;
    }
}
