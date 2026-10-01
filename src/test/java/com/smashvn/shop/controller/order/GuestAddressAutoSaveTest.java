package com.smashvn.shop.controller.order;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.order.GuestCheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GuestAddressAutoSaveTest {

    @Autowired
    private GuestCheckoutService guestCheckoutService;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private SoDiaChiRepository soDiaChiRepository;

    @Test
    public void testGuestAutoRegistrationAndAddressSave() {
        String guestEmail = "guest_address_test_" + System.currentTimeMillis() + "@smash.vn";
        String hoTen = "Nguyễn Văn Guest";
        String sdt = "0912345678";

        // Auto register guest customer
        GuestCheckoutService.GuestRegisterResult result = guestCheckoutService.autoRegisterGuest(hoTen, sdt, guestEmail);
        TaiKhoan tk = result.getTaiKhoan();
        assertNotNull(tk);
        assertEquals(AccountStatus.GUEST, tk.getTrangThaiTaiKhoan());

        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        assertNotNull(kh);

        // Manually simulate address saving to SoDiaChi for the new guest account
        SoDiaChi diaChi = new SoDiaChi();
        diaChi.setKhachHang(kh);
        diaChi.setHoNguoiNhan("Nguyễn Văn");
        diaChi.setTenNguoiNhan("Guest");
        diaChi.setSdtNguoiNhan(sdt);
        diaChi.setDiaChiCuThe("123 Đường Cầu Lông, Phường 1");
        diaChi.setTinhThanh("Hồ Chí Minh");
        diaChi.setQuanHuyen("Quận 1");
        diaChi.setDefaultShipping(true);
        diaChi.setDefaultBilling(true);

        SoDiaChi savedAddress = soDiaChiRepository.save(diaChi);

        assertNotNull(savedAddress.getId());
        List<SoDiaChi> listAddress = soDiaChiRepository.findByKhachHang_Id(kh.getId());
        assertEquals(1, listAddress.size());
        assertEquals("Hồ Chí Minh", listAddress.get(0).getTinhThanh());
        assertTrue(listAddress.get(0).isDefaultShipping());
    }
}
