package com.smashvn.shop.controller.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.ConcurrentModel;

import com.smashvn.shop.dao.DotGiamGiaDAO;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.blog.BlogService;
import com.smashvn.shop.service.product.SanPhamService;

class HomeControllerPerformanceTest {

    @Test
    void homepageCapsBestSellerCardsToTwentyFour() {
        SanPhamRepository sanPhamRepository = mock(SanPhamRepository.class);
        SanPhamChiTietRepository sanPhamChiTietRepository = mock(SanPhamChiTietRepository.class);
        ThuongHieuRepository thuongHieuRepository = mock(ThuongHieuRepository.class);
        DanhMucRepository danhMucRepository = mock(DanhMucRepository.class);
        BlogService blogService = mock(BlogService.class);
        DotGiamGiaDAO dotGiamGiaDAO = mock(DotGiamGiaDAO.class);
        SanPhamService sanPhamService = mock(SanPhamService.class);

        when(sanPhamRepository.findAll()).thenReturn(new ArrayList<>());
        when(sanPhamRepository.findNewProducts(any(Pageable.class))).thenReturn(List.of());
        when(sanPhamRepository.findBestSellers(any(Pageable.class))).thenReturn(List.of());
        when(sanPhamRepository.findFeaturedProducts(any(Pageable.class))).thenReturn(List.of());
        when(thuongHieuRepository.findByTrangThaiTrue()).thenReturn(List.of());
        when(dotGiamGiaDAO.findAll()).thenReturn(List.of());
        when(blogService.getRecentBlogs(3)).thenReturn(List.of());

        HomeController controller = new HomeController(
                sanPhamRepository,
                sanPhamChiTietRepository,
                thuongHieuRepository,
                danhMucRepository,
                blogService,
                dotGiamGiaDAO,
                sanPhamService);

        assertEquals("index", controller.hienThiTrangChu(new ConcurrentModel()));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(sanPhamRepository).findBestSellers(pageableCaptor.capture());
        assertEquals(24, pageableCaptor.getValue().getPageSize());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
    }
}
