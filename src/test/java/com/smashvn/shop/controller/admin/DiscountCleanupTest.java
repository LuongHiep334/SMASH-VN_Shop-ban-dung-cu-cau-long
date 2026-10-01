package com.smashvn.shop.controller.admin;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class DiscountCleanupTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    @Rollback(false)
    public void deleteDiscountsExceptSpecified() {
        System.out.println("=== THỰC HIỆN XÓA PHIẾU GIẢM GIÁ VÀ ĐỢT GIẢM GIÁ (GIỮ LẠI HIEPGA005 & Hè Vui Vẻ) ===");

        // 1. Gỡ bỏ tham chiếu id_phieu_giam_gia trong HoaDon nếu thuộc về phiếu sắp bị xóa
        int unlinkedOrders = entityManager.createNativeQuery(
            "UPDATE HoaDon SET id_phieu_giam_gia = NULL WHERE id_phieu_giam_gia IN (SELECT id FROM PhieuGiamGia WHERE ma_phieu != 'HIEPGA005')"
        ).executeUpdate();
        System.out.println("Đã gỡ liên kết phiếu giảm giá từ số hóa đơn: " + unlinkedOrders);

        // 2. Xóa các phiếu giảm giá ngoại trừ 'HIEPGA005'
        int deletedVouchers = entityManager.createNativeQuery(
            "DELETE FROM PhieuGiamGia WHERE ma_phieu != 'HIEPGA005'"
        ).executeUpdate();
        System.out.println("Đã xóa số phiếu giảm giá: " + deletedVouchers);

        // 3. Xóa liên kết sản phẩm - đợt giảm giá trong bảng trung gian SanPham_DotGiamGia
        int unlinkedCampaignProducts = entityManager.createNativeQuery(
            "DELETE FROM SanPham_DotGiamGia WHERE id_dot_giam_gia IN (SELECT id FROM DotGiamGia WHERE ten_chien_dich != N'Hè Vui Vẻ')"
        ).executeUpdate();
        System.out.println("Đã xóa số liên kết sản phẩm trong đợt giảm giá bị xóa: " + unlinkedCampaignProducts);

        // 4. Xóa các đợt giảm giá ngoại trừ 'Hè Vui Vẻ'
        int deletedCampaigns = entityManager.createNativeQuery(
            "DELETE FROM DotGiamGia WHERE ten_chien_dich != N'Hè Vui Vẻ'"
        ).executeUpdate();
        System.out.println("Đã xóa số đợt giảm giá: " + deletedCampaigns);

        System.out.println("=== THÀNH CÔNG: CHỈ GIỮ LẠI PHIẾU HIEPGA005 VÀ ĐỢT GIẢM GIÁ Hè Vui Vẻ ===");
    }
}
