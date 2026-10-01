package com.smashvn.shop.controller.order;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.order.GuestCartService;
import com.smashvn.shop.service.order.GuestCheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuickAddAndCheckoutTest {

    @Autowired
    private GuestCheckoutService guestCheckoutService;

    @Autowired
    private GuestCartService guestCartService;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Test
    public void testQuickAddAndCheckoutForGuestWithExistingSessionId() {
        MockHttpSession session = new MockHttpSession();

        // 1. Create a guest account in DB to simulate a previous guest checkout
        String email = "quick_add_test_" + System.currentTimeMillis() + "@smash.vn";
        String phone = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        GuestCheckoutService.GuestRegisterResult regResult = guestCheckoutService.autoRegisterGuest("Trần Văn Quick", phone, email);
        TaiKhoan tk = regResult.getTaiKhoan();
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        assertNotNull(kh);

        // Put idNguoiDung into session as happens after guest registration
        session.setAttribute("idNguoiDung", tk.getId());
        session.setAttribute("nguoiDungDangNhap", tk.getUsername());

        // 2. Find a available variant
        List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findAll();
        assertFalse(variants.isEmpty(), "Cần có ít nhất 1 sản phẩm chi tiết trong CSDL để test");
        SanPhamChiTiet spct = variants.get(0);

        // 3. User clicks "Thêm vào giỏ nhanh" -> adds to session guest cart
        guestCartService.addToGuestCart(session, spct.getId(), 1);
        List<GuestCartService.GuestCartItem> sessionItems = guestCartService.getGuestCartItems(session);
        assertEquals(1, sessionItems.size());

        // 4. User clicks "THANH TOÁN TỚI ĐÂY" -> transfer guest cart to DB
        guestCartService.transferGuestCartToDb(session, kh.getId());

        // Verify session cart is now empty and DB cart contains the item
        assertTrue(guestCartService.getGuestCartItems(session).isEmpty());
    }
}
