package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.GioHang;
import com.smashvn.shop.entity.GioHangChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.repository.GioHangChiTietRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.service.product.ProductAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GioHangServiceAvailabilityTest {

    @Mock
    private KhachHangRepository khachHangRepository;
    @Mock
    private GioHangChiTietRepository gioHangChiTietRepository;
    @Mock
    private ProductAvailabilityService productAvailabilityService;

    @InjectMocks
    private GioHangService service;

    private GioHangChiTiet cartItem;

    @BeforeEach
    void setUp() {
        KhachHang customer = new KhachHang();
        customer.setId(11);

        GioHang cart = new GioHang();
        cart.setKhachHang(customer);

        cartItem = new GioHangChiTiet();
        cartItem.setId(101);
        cartItem.setGioHang(cart);
        cartItem.setSanPhamChiTiet(new SanPhamChiTiet());

        when(khachHangRepository.findByTaiKhoan_Id(7)).thenReturn(customer);
        when(gioHangChiTietRepository.findById(101)).thenReturn(Optional.of(cartItem));
    }

    @Test
    void unavailableItemCanStillBeRemovedFromCart() {
        service.xoaSanPhamKhoiGio(101, 7);

        verify(gioHangChiTietRepository).delete(cartItem);
        verify(productAvailabilityService, never()).isVariantPublished(cartItem.getSanPhamChiTiet());
    }

    @Test
    void unavailableItemQuantityCannotBeUpdated() {
        when(productAvailabilityService.isVariantPublished(cartItem.getSanPhamChiTiet())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.capNhatSoLuong(101, 2, 7));

        verify(gioHangChiTietRepository, never()).save(cartItem);
    }
}
