package com.smashvn.shop.service.admin;

import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminNhanVienServiceTest {

    @Mock
    private NhanVienRepository nhanVienRepository;

    @Mock
    private TaiKhoanRepository taiKhoanRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private KhachHangRepository khachHangRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private org.springframework.cache.CacheManager cacheManager;

    @Mock
    private org.springframework.cache.Cache cache;

    private AdminNhanVienService adminNhanVienService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cacheManager.getCache("taiKhoanStatus")).thenReturn(cache);
        adminNhanVienService = new AdminNhanVienService(
                nhanVienRepository,
                taiKhoanRepository,
                auditService,
                mailSender,
                khachHangRepository,
                passwordEncoder,
                cacheManager);
    }

    @Test
    void createNhanVien_duplicatePhone_showsFriendlyMessageAndDoesNotInsert() {
        when(nhanVienRepository.existsBySoDienThoai("0357059078")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                adminNhanVienService.createNhanVien(
                        "staff@example.com",
                        "SecurePass123",
                        "Nguyen Van A",
                        "Nhan vien ban hang",
                        "0357059078",
                        1,
                        "127.0.0.1"));

        assertEquals("Số điện thoại nhân viên đã tồn tại. Vui lòng nhập số khác.", ex.getMessage());
        verify(taiKhoanRepository, never()).saveAndFlush(any());
        verify(nhanVienRepository, never()).saveAndFlush(any());
    }

    @Test
    void createNhanVien_invalidVietnamPhone_showsValidationMessageAndDoesNotInsert() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                adminNhanVienService.createNhanVien(
                        "staff@example.com",
                        "SecurePass123",
                        "Nguyen Van A",
                        "Nhan vien ban hang",
                        "012345",
                        1,
                        "127.0.0.1"));

        assertEquals("Số điện thoại không đúng định dạng Việt Nam. Vui lòng nhập số bắt đầu bằng 03, 05, 07, 08, 09 và đủ 10 số.", ex.getMessage());
        verify(taiKhoanRepository, never()).saveAndFlush(any());
        verify(nhanVienRepository, never()).saveAndFlush(any());
    }

    @Test
    void approveLock_validManager_locksAccountAndEvictsCache() {
        com.smashvn.shop.entity.TaiKhoan tk = new com.smashvn.shop.entity.TaiKhoan();
        tk.setId(10);
        tk.setUsername("nv1@smash.vn");
        tk.setTrangThai("cho_khoa");
        tk.setVaiTro("NV");
        tk.setTokenXacThucKhoa("valid-token");

        com.smashvn.shop.entity.NhanVien nv = new com.smashvn.shop.entity.NhanVien();
        nv.setId(5);
        nv.setHoTenNv("Nguyen Staff");
        nv.setChucVu("Sales");
        nv.setSoDienThoaiNv("0987654321");
        nv.setTaiKhoan(tk);

        com.smashvn.shop.entity.TaiKhoan actingManager = new com.smashvn.shop.entity.TaiKhoan();
        actingManager.setId(1);
        actingManager.setVaiTro("QL");
        actingManager.setUsername("manager@smash.vn");

        when(nhanVienRepository.findById(5)).thenReturn(java.util.Optional.of(nv));
        when(taiKhoanRepository.findByIdForUpdate(10)).thenReturn(java.util.Optional.of(tk));
        when(taiKhoanRepository.findById(1)).thenReturn(java.util.Optional.of(actingManager));

        adminNhanVienService.approveLock(5, null, 1, "127.0.0.1");

        assertEquals("bi_khoa", tk.getTrangThai());
        org.junit.jupiter.api.Assertions.assertNull(tk.getTokenXacThucKhoa());
        verify(taiKhoanRepository).save(tk);
        verify(cache).evict(10);
    }

    @Test
    void approveLock_validTokenWithoutSession_locksAccountAndEvictsCache() {
        com.smashvn.shop.entity.TaiKhoan tk = new com.smashvn.shop.entity.TaiKhoan();
        tk.setId(10);
        tk.setUsername("nv1@smash.vn");
        tk.setTrangThai("cho_khoa");
        tk.setVaiTro("NV");
        String token = java.time.Instant.now().getEpochSecond() + ".secret-email-token";
        tk.setTokenXacThucKhoa(token);

        com.smashvn.shop.entity.NhanVien nv = new com.smashvn.shop.entity.NhanVien();
        nv.setId(5);
        nv.setHoTenNv("Nguyen Staff");
        nv.setChucVu("Sales");
        nv.setSoDienThoaiNv("0987654321");
        nv.setTaiKhoan(tk);

        when(nhanVienRepository.findById(5)).thenReturn(java.util.Optional.of(nv));
        when(taiKhoanRepository.findByIdForUpdate(10)).thenReturn(java.util.Optional.of(tk));

        adminNhanVienService.approveLock(5, token, null, "127.0.0.1");

        assertEquals("bi_khoa", tk.getTrangThai());
        org.junit.jupiter.api.Assertions.assertNull(tk.getTokenXacThucKhoa());
        verify(taiKhoanRepository).save(tk);
        verify(cache).evict(10);
    }

    @Test
    void rejectLock_validManager_revertsToActiveAndEvictsCache() {
        com.smashvn.shop.entity.TaiKhoan tk = new com.smashvn.shop.entity.TaiKhoan();
        tk.setId(10);
        tk.setUsername("nv1@smash.vn");
        tk.setTrangThai("cho_khoa");
        tk.setVaiTro("NV");
        tk.setTokenXacThucKhoa("valid-token");

        com.smashvn.shop.entity.NhanVien nv = new com.smashvn.shop.entity.NhanVien();
        nv.setId(5);
        nv.setHoTenNv("Nguyen Staff");
        nv.setChucVu("Sales");
        nv.setSoDienThoaiNv("0987654321");
        nv.setTaiKhoan(tk);

        com.smashvn.shop.entity.TaiKhoan actingManager = new com.smashvn.shop.entity.TaiKhoan();
        actingManager.setId(1);
        actingManager.setVaiTro("QL");
        actingManager.setUsername("manager@smash.vn");

        when(nhanVienRepository.findById(5)).thenReturn(java.util.Optional.of(nv));
        when(taiKhoanRepository.findByIdForUpdate(10)).thenReturn(java.util.Optional.of(tk));
        when(taiKhoanRepository.findById(1)).thenReturn(java.util.Optional.of(actingManager));

        adminNhanVienService.rejectLock(5, null, 1, "127.0.0.1");

        assertEquals("hoat_dong", tk.getTrangThai());
        org.junit.jupiter.api.Assertions.assertNull(tk.getTokenXacThucKhoa());
        verify(taiKhoanRepository).save(tk);
        verify(cache).evict(10);
    }
}
