package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CategoryHideIntegrationTest {

    @Autowired
    private DanhMucService danhMucService;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuService thuongHieuService;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Test
    @DisplayName("Ẩn danh mục khỏi giao diện người dùng và cho phép hiện lại")
    void testHideAndShowCategory() {
        // 1. Tạo mới danh mục
        DanhMuc category = danhMucService.themDanhMuc("Danh Mục Thử Nghiệm Ẩn");
        assertThat(category.getTrangThai()).isTrue();

        // Check findByTrangThaiTrue includes it
        List<DanhMuc> activeListBefore = danhMucRepository.findByTrangThaiTrue();
        assertThat(activeListBefore).extracting(DanhMuc::getId).contains(category.getId());

        // 2. Thực hiện ẩn danh mục
        DanhMuc hiddenCategory = danhMucService.anHoacHienDanhMuc(category.getId());
        assertThat(hiddenCategory.getTrangThai()).isFalse();

        // Check findByTrangThaiTrue excludes hidden category
        List<DanhMuc> activeListAfterHide = danhMucRepository.findByTrangThaiTrue();
        assertThat(activeListAfterHide).extracting(DanhMuc::getId).doesNotContain(category.getId());

        // But findAll still returns it for admin
        List<DanhMuc> allList = danhMucRepository.findAll();
        assertThat(allList).extracting(DanhMuc::getId).contains(category.getId());

        // 3. Thực hiện hiển thị lại danh mục
        DanhMuc restoredCategory = danhMucService.anHoacHienDanhMuc(category.getId());
        assertThat(restoredCategory.getTrangThai()).isTrue();

        List<DanhMuc> activeListAfterRestore = danhMucRepository.findByTrangThaiTrue();
        assertThat(activeListAfterRestore).extracting(DanhMuc::getId).contains(category.getId());
    }

    @Test
    @DisplayName("Ẩn hãng/thương hiệu khỏi giao diện người dùng và cho phép hiện lại")
    void testHideAndShowBrand() {
        // 1. Tạo mới thương hiệu
        ThuongHieu brand = thuongHieuService.themThuongHieu("Hãng Thử Nghiệm Ẩn");
        assertThat(brand.getTrangThai()).isTrue();

        // Check findByTrangThaiTrue includes it
        List<ThuongHieu> activeListBefore = thuongHieuRepository.findByTrangThaiTrue();
        assertThat(activeListBefore).extracting(ThuongHieu::getId).contains(brand.getId());

        // 2. Thực hiện ẩn thương hiệu
        ThuongHieu hiddenBrand = thuongHieuService.anHoacHienThuongHieu(brand.getId());
        assertThat(hiddenBrand.getTrangThai()).isFalse();

        // Check findByTrangThaiTrue excludes hidden brand
        List<ThuongHieu> activeListAfterHide = thuongHieuRepository.findByTrangThaiTrue();
        assertThat(activeListAfterHide).extracting(ThuongHieu::getId).doesNotContain(brand.getId());

        // 3. Thực hiện hiển thị lại thương hiệu
        ThuongHieu restoredBrand = thuongHieuService.anHoacHienThuongHieu(brand.getId());
        assertThat(restoredBrand.getTrangThai()).isTrue();

        List<ThuongHieu> activeListAfterRestore = thuongHieuRepository.findByTrangThaiTrue();
        assertThat(activeListAfterRestore).extracting(ThuongHieu::getId).contains(brand.getId());
    }
}
