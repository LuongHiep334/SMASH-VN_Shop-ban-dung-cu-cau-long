package com.smashvn.shop.controller.admin;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.admin.AdminKhachHangService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminKhachHangManagementTest {

    @Autowired
    private AdminKhachHangService adminKhachHangService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testCreateKhachHangSuccess() {
        String testEmail = "khachhangtest_" + System.currentTimeMillis() + "@smash.vn";
        String rawPass = "Password123";
        String hoTen = "Nguyễn Văn Test";
        String sdt = "0987654321";

        KhachHang kh = adminKhachHangService.createKhachHang(testEmail, rawPass, hoTen, sdt, null, "127.0.0.1");

        assertNotNull(kh);
        assertNotNull(kh.getId());
        assertEquals(hoTen, kh.getHoTenKh());
        assertEquals(sdt, kh.getSoDienThoaiKh());

        TaiKhoan tk = kh.getTaiKhoan();
        assertNotNull(tk);
        assertEquals(testEmail, tk.getUsername());
        assertEquals("KH", tk.getVaiTro());
        assertEquals(AccountStatus.ACTIVE, tk.getTrangThaiTaiKhoan());
        assertTrue(passwordEncoder.matches(rawPass, tk.getMatKhau()));
    }

    @Test
    public void testCreateKhachHangDuplicateEmailFails() {
        String testEmail = "khachhangdup_" + System.currentTimeMillis() + "@smash.vn";
        adminKhachHangService.createKhachHang(testEmail, "Password123", "Khách A", "0911223344", null, "127.0.0.1");

        assertThrows(IllegalArgumentException.class, () -> {
            adminKhachHangService.createKhachHang(testEmail, "Password123", "Khách B", "0955667788", null, "127.0.0.1");
        });
    }

    @Test
    public void testUpdateKhachHangInfoAndStatus() {
        String testEmail = "khachhangupdate_" + System.currentTimeMillis() + "@smash.vn";
        KhachHang kh = adminKhachHangService.createKhachHang(testEmail, "Password123", "Khách Cũ", "0900000001", null, "127.0.0.1");

        String newName = "Khách Hàng Mới Cập Nhật";
        String newSdt = "0900000002";
        String newTrangThai = "bi_khoa";

        KhachHang updatedKh = adminKhachHangService.updateKhachHang(kh.getId(), newName, newSdt, newTrangThai, null, "127.0.0.1");

        assertEquals(newName, updatedKh.getHoTenKh());
        assertEquals(newSdt, updatedKh.getSoDienThoaiKh());
        assertEquals("bi_khoa", updatedKh.getTaiKhoan().getTrangThai());
        assertEquals(AccountStatus.LOCKED, updatedKh.getTaiKhoan().getTrangThaiTaiKhoan());
    }
}
