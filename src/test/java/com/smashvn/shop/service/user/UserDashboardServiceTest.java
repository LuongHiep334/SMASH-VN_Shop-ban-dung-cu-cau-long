package com.smashvn.shop.service.user;

import com.smashvn.shop.dto.user.UserProfileEditDto;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserDashboardServiceTest {

    @Mock
    private KhachHangRepository khachHangRepository;

    private UserDashboardService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UserDashboardService(khachHangRepository);
    }

    @Test
    void testCapNhatHoSo_Success() {
        UserProfileEditDto dto = new UserProfileEditDto("Nguyễn", "Đặng Trần Văn Ánh", "0912345678");
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        kh.setTaiKhoan(tk);

        when(khachHangRepository.findBySoDienThoaiKh("0912345678")).thenReturn(null);
        when(khachHangRepository.findByTaiKhoan_Id(1)).thenReturn(kh);

        service.capNhatHoSo(1, dto);

        assertEquals("Nguyễn", kh.getHoKh());
        assertEquals("Đặng Trần Văn Ánh", kh.getTenKh());
        assertEquals("0912345678", kh.getSoDienThoaiKh());
        verify(khachHangRepository).save(kh);
    }

    @Test
    void testCapNhatHoSo_NullDto() {
        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, null));
    }

    @Test
    void testCapNhatHoSo_NullOrBlankHo() {
        UserProfileEditDto dtoNull = new UserProfileEditDto(null, "Van A", "0912345678");
        UserProfileEditDto dtoBlank = new UserProfileEditDto("   ", "Van A", "0912345678");

        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dtoNull));
        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dtoBlank));
    }

    @Test
    void testCapNhatHoSo_NullOrBlankTen() {
        UserProfileEditDto dtoNull = new UserProfileEditDto("Nguyen", null, "0912345678");
        UserProfileEditDto dtoBlank = new UserProfileEditDto("Nguyen", "   ", "0912345678");

        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dtoNull));
        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dtoBlank));
    }

    @Test
    void testCapNhatHoSo_BoundaryFname() {
        String name49 = "a".repeat(49);
        String name50 = "a".repeat(50);
        String name51 = "a".repeat(51);

        UserProfileEditDto dto49 = new UserProfileEditDto(name49, "Van A", "0912345678");
        UserProfileEditDto dto50 = new UserProfileEditDto(name50, "Van A", "0912345678");
        UserProfileEditDto dto51 = new UserProfileEditDto(name51, "Van A", "0912345678");

        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        kh.setTaiKhoan(tk);
        when(khachHangRepository.findByTaiKhoan_Id(1)).thenReturn(kh);

        // Should pass
        service.capNhatHoSo(1, dto49);
        service.capNhatHoSo(1, dto50);

        // Should throw
        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dto51));
    }

    @Test
    void testCapNhatHoSo_BoundaryLname() {
        String name49 = "a".repeat(49);
        String name50 = "a".repeat(50);
        String name51 = "a".repeat(51);

        UserProfileEditDto dto49 = new UserProfileEditDto("Nguyen", name49, "0912345678");
        UserProfileEditDto dto50 = new UserProfileEditDto("Nguyen", name50, "0912345678");
        UserProfileEditDto dto51 = new UserProfileEditDto("Nguyen", name51, "0912345678");

        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        kh.setTaiKhoan(tk);
        when(khachHangRepository.findByTaiKhoan_Id(1)).thenReturn(kh);

        // Should pass
        service.capNhatHoSo(1, dto49);
        service.capNhatHoSo(1, dto50);

        // Should throw
        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dto51));
    }

    @Test
    void testCapNhatHoSo_InvalidPhoneFormat() {
        UserProfileEditDto dto1 = new UserProfileEditDto("Nguyen", "Van A", "0123456789");
        UserProfileEditDto dto2 = new UserProfileEditDto("Nguyen", "Van A", "+84123456789");
        UserProfileEditDto dto3 = new UserProfileEditDto("Nguyen", "Van A", "abc1234567");

        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dto1));
        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dto2));
        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dto3));
    }

    @Test
    void testCapNhatHoSo_DuplicatePhone() {
        UserProfileEditDto dto = new UserProfileEditDto("Nguyen", "Van A", "0912345678");
        
        KhachHang otherKh = new KhachHang();
        TaiKhoan otherTk = new TaiKhoan();
        otherTk.setId(2); // different user
        otherKh.setTaiKhoan(otherTk);

        when(khachHangRepository.findBySoDienThoaiKh("0912345678")).thenReturn(otherKh);

        assertThrows(IllegalArgumentException.class, () -> service.capNhatHoSo(1, dto));
    }

    @Test
    void testCapNhatHoSo_XSSSanitization() {
        UserProfileEditDto dto = new UserProfileEditDto("<script>alert(1)</script>Nguyen", "<img src=x onerror=alert(1)>Van A", "<svg onload=alert(1)>0912345678");
        
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(1);
        kh.setTaiKhoan(tk);
        when(khachHangRepository.findByTaiKhoan_Id(1)).thenReturn(kh);

        service.capNhatHoSo(1, dto);

        assertEquals("Nguyen", kh.getHoKh());
        assertEquals("Van A", kh.getTenKh());
        assertEquals("0912345678", kh.getSoDienThoaiKh());
    }
}
