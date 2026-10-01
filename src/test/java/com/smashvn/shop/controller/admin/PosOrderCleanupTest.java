package com.smashvn.shop.controller.admin;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class PosOrderCleanupTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    @Rollback(false)
    public void deletePosOrdersExceptTop5() {
        System.out.println("=== TIẾN HÀNH XÓA ĐƠN HÀNG POS (ID > 5) CHỈ GIỮ LẠI 5 ĐƠN ĐẦU TIÊN ===");

        String posOrderSubQuery = "SELECT id FROM HoaDon WHERE (id_nhan_vien IS NOT NULL OR dia_chi_nhan LIKE N'%tại quầy%' OR dia_chi_nhan LIKE '%tai quay%') AND id > 5";

        // 1. Xóa các bảng con phụ thuộc HoaDon
        entityManager.createNativeQuery("DELETE FROM GiaoDichThanhToan WHERE id_hoa_don IN (" + posOrderSubQuery + ")").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM TichHopVanChuyen WHERE id_hoa_don IN (" + posOrderSubQuery + ")").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM HoaDonChiTiet WHERE id_hoa_don IN (" + posOrderSubQuery + ")").executeUpdate();

        // 2. Xóa các hóa đơn POS từ ID > 5
        int deletedOrders = entityManager.createNativeQuery("DELETE FROM HoaDon WHERE (id_nhan_vien IS NOT NULL OR dia_chi_nhan LIKE N'%tại quầy%' OR dia_chi_nhan LIKE '%tai quay%') AND id > 5").executeUpdate();
        System.out.println("Đã xóa thành công số đơn hàng POS: " + deletedOrders);

        System.out.println("=== THÀNH CÔNG: ĐÃ GIỮ LẠI ĐÚNG 5 ĐƠN HÀNG BÁN TẠI QUẦY (ID 1 -> 5) ===");
    }
}
