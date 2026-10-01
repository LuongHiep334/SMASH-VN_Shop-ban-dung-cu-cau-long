package com.smashvn.shop.controller.product;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.SanPhamYeuThichRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.product.SanPhamYeuThichService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SanPhamYeuThichControllerTest {

    @Mock
    private SanPhamYeuThichService yeuThichService;

    @Mock
    private SanPhamYeuThichRepository yeuThichRepository;

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private HttpSession session;

    private SanPhamYeuThichController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SanPhamYeuThichController(yeuThichService, yeuThichRepository, taiKhoanRepository);
    }

    @Test
    void guestCannotViewWishlist() {
        TaiKhoan guest = account(10, AccountStatus.GUEST, "hoat_dong");
        when(session.getAttribute("idNguoiDung")).thenReturn(10);
        when(taiKhoanRepository.findById(10)).thenReturn(Optional.of(guest));

        Model model = new ConcurrentModel();
        String view = controller.hienThiWishlist(session, model);

        assertEquals("redirect:/user/dang-nhap", view);
        verify(yeuThichService, never()).layDanhSachWishlist(10);
    }

    @Test
    void guestCannotModifyWishlist() {
        TaiKhoan guest = account(10, AccountStatus.GUEST, "hoat_dong");
        when(session.getAttribute("idNguoiDung")).thenReturn(10);
        when(taiKhoanRepository.findById(10)).thenReturn(Optional.of(guest));

        ResponseEntity<Map<String, Object>> response = controller.themVaoWishlist(99, session);

        assertEquals(401, response.getStatusCode().value());
        assertEquals("chuadangnhap", response.getBody().get("status"));
        verify(yeuThichService, never()).themVaoWishlist(10, 99);
    }

    @Test
    void activeWishlistPageStillWorks() {
        TaiKhoan active = account(11, AccountStatus.ACTIVE, "hoat_dong");
        when(session.getAttribute("idNguoiDung")).thenReturn(11);
        when(taiKhoanRepository.findById(11)).thenReturn(Optional.of(active));
        when(yeuThichService.layDanhSachWishlist(11)).thenReturn(List.of());

        Model model = new ConcurrentModel();
        String view = controller.hienThiWishlist(session, model);

        assertEquals("wishlist", view);
        assertEquals(List.of(), model.getAttribute("listWishlist"));
        verify(yeuThichService).layDanhSachWishlist(11);
    }

    @Test
    void activeWishlistModifyStillWorks() {
        TaiKhoan active = account(11, AccountStatus.ACTIVE, "hoat_dong");
        when(session.getAttribute("idNguoiDung")).thenReturn(11);
        when(taiKhoanRepository.findById(11)).thenReturn(Optional.of(active));
        when(yeuThichService.themVaoWishlist(11, 99)).thenReturn("added");
        when(yeuThichRepository.countById_SanPhamId(99)).thenReturn(3L);

        ResponseEntity<Map<String, Object>> response = controller.themVaoWishlist(99, session);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("added", response.getBody().get("status"));
        assertEquals(3L, response.getBody().get("count"));
        verify(yeuThichService).themVaoWishlist(11, 99);
    }

    private TaiKhoan account(Integer id, AccountStatus status, String trangThai) {
        TaiKhoan tk = new TaiKhoan();
        tk.setId(id);
        tk.setUsername("user" + id + "@example.com");
        tk.setVaiTro("KH");
        tk.setTrangThai(trangThai);
        tk.setTrangThaiTaiKhoan(status);
        return tk;
    }
}
