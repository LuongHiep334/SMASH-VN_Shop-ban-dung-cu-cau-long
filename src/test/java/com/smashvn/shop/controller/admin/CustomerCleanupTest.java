package com.smashvn.shop.controller.admin;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class CustomerCleanupTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    @Rollback(false)
    public void deleteCustomersExceptTop5() {
        System.out.println("=== XÓA TẤT CẢ KHÁCH HÀNG TỪ KH6 (ID > 5) VÀ DỮ LIỆU LIÊN QUAN ===");

        // 1. Xóa các bảng con của HoaDon
        entityManager.createNativeQuery("DELETE FROM GiaoDichThanhToan WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE id_khach_hang > 5)").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM TichHopVanChuyen WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE id_khach_hang > 5)").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM HoaDonChiTiet WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE id_khach_hang > 5)").executeUpdate();

        // 2. Xóa HoaDon
        entityManager.createNativeQuery("DELETE FROM HoaDon WHERE id_khach_hang > 5").executeUpdate();

        // 3. Xóa các bảng con của KhachHang
        entityManager.createNativeQuery("DELETE FROM DanhGia WHERE id_khach_hang > 5").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM SoDiaChi WHERE id_khach_hang > 5").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM SanPhamYeuThich WHERE id_khach_hang > 5").executeUpdate();

        // 4. Xóa Giỏ hàng
        entityManager.createNativeQuery("DELETE FROM GioHangChiTiet WHERE id_gio_hang IN (SELECT id FROM GioHang WHERE id_khach_hang > 5)").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM GioHang WHERE id_khach_hang > 5").executeUpdate();

        // 5. Xóa KhachHang
        int deletedCustomers = entityManager.createNativeQuery("DELETE FROM KhachHang WHERE id > 5").executeUpdate();
        System.out.println("Đã xóa số lượng KhachHang: " + deletedCustomers);

        // 6. Xóa các dữ liệu phụ thuộc TaiKhoan của khách hàng
        entityManager.createNativeQuery("DELETE FROM MaKhoiPhuc WHERE id_tai_khoan IN (SELECT id FROM TaiKhoan WHERE vai_tro = 'KH' AND id NOT IN (SELECT id_tai_khoan FROM KhachHang WHERE id_tai_khoan IS NOT NULL))").executeUpdate();
        entityManager.createNativeQuery("UPDATE EditLog SET id_tai_khoan = NULL WHERE id_tai_khoan IN (SELECT id FROM TaiKhoan WHERE vai_tro = 'KH' AND id NOT IN (SELECT id_tai_khoan FROM KhachHang WHERE id_tai_khoan IS NOT NULL))").executeUpdate();

        // 7. Xóa TaiKhoan của các khách hàng đã bị xóa
        int deletedAccounts = entityManager.createNativeQuery("DELETE FROM TaiKhoan WHERE vai_tro = 'KH' AND id NOT IN (SELECT id_tai_khoan FROM KhachHang WHERE id_tai_khoan IS NOT NULL)").executeUpdate();
        System.out.println("Đã xóa số lượng TaiKhoan khách hàng mồ côi: " + deletedAccounts);

        System.out.println("=== THÀNH CÔNG: ĐÃ GIỮ LẠI ĐÚNG 5 KHÁCH HÀNG ĐẦU TIÊN (KH1 - KH5) ===");
    }
}
