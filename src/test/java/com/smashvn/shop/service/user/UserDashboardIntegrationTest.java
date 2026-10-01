package com.smashvn.shop.service.user;

import com.smashvn.shop.dto.user.UserProfileEditDto;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class UserDashboardIntegrationTest {

    @Autowired
    private UserDashboardService userDashboardService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Test
    void testIntegration_CapNhatHoSo_Success_And_Sanitization() {
        // 1. Create a test user account
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("test_integration_user@gmail.com");
        tk.setMatKhau("testpass123");
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");

        tk = taiKhoanRepository.save(tk);

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoKh("InitHo");
        kh.setTenKh("InitTen");
        kh.setSoDienThoaiKh("0988888888");
        kh = khachHangRepository.save(kh);

        // 2. Perform profile update with XSS payload and Unicode characters
        UserProfileEditDto dto = new UserProfileEditDto(
                "<script>alert(1)</script>Nguyễn", 
                "<img src=x onerror=alert(2)>Đặng Trần Văn Ánh", 
                "0912345678"
        );
        
        userDashboardService.capNhatHoSo(tk.getId(), dto);

        // 3. Verify database updates and sanitization
        KhachHang updatedKh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        assertNotNull(updatedKh);
        assertEquals("Nguyễn", updatedKh.getHoKh());
        assertEquals("Đặng Trần Văn Ánh", updatedKh.getTenKh());
        assertEquals("0912345678", updatedKh.getSoDienThoaiKh());
    }

    @Test
    void testIntegration_CapNhatHoSo_DuplicatePhoneRejected() {
        // 1. Create User A
        TaiKhoan tkA = new TaiKhoan();
        tkA.setUsername("usera@gmail.com");
        tkA.setMatKhau("pass123");
        tkA.setVaiTro("KH");
        tkA.setTrangThai("hoat_dong");

        tkA = taiKhoanRepository.save(tkA);

        KhachHang khA = new KhachHang();
        khA.setTaiKhoan(tkA);
        khA.setHoKh("User");
        khA.setTenKh("A");
        khA.setSoDienThoaiKh("0911111111");
        khA = khachHangRepository.save(khA);

        // 2. Create User B
        TaiKhoan tkB = new TaiKhoan();
        tkB.setUsername("userb@gmail.com");
        tkB.setMatKhau("pass123");
        tkB.setVaiTro("KH");
        tkB.setTrangThai("hoat_dong");

        tkB = taiKhoanRepository.save(tkB);

        KhachHang khB = new KhachHang();
        khB.setTaiKhoan(tkB);
        khB.setHoKh("User");
        khB.setTenKh("B");
        khB.setSoDienThoaiKh("0922222222");
        khB = khachHangRepository.save(khB);

        // 3. Try to update User B's phone number to User A's phone number
        UserProfileEditDto dto = new UserProfileEditDto("User", "B", "0911111111");
        
        final Integer idB = tkB.getId();
        assertThrows(IllegalArgumentException.class, () -> {
            userDashboardService.capNhatHoSo(idB, dto);
        });
    }
}
