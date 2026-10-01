package com.smashvn.shop.controller.admin;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
public class CatalogCleanupTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    @Rollback(false)
    public void cleanupCategoriesAndBrands() {
        System.out.println("=== BẮT ĐẦU DỌN DẸP DANH MỤC, THƯƠNG HIỆU VÀ SẢN PHẨM ===");

        // 1. Chuyển sản phẩm cước (nếu có) về đúng Danh mục Cước (ID = 164)
        int updatedCuoc = entityManager.createNativeQuery("UPDATE SanPham SET id_danh_muc = 164 WHERE ten_san_pham LIKE '%Cước%' OR ten_san_pham LIKE '%cuoc%'").executeUpdate();
        System.out.println("Đã chuyển sản phẩm Cước về Danh mục Cước (ID 164): " + updatedCuoc);

        // Các ID cần giữ lại:
        // Danh mục: 1 (Vợt Cầu Lông), 164 (Cước)
        // Thương hiệu: 1 (Yonex), 3 (Lining)

        String invalidProductsSubQuery = "SELECT id FROM SanPham WHERE id_danh_muc NOT IN (1, 164) OR id_thuong_hieu NOT IN (1, 3)";
        String invalidVariantsSubQuery = "SELECT id FROM SanPhamChiTiet WHERE id_san_pham IN (" + invalidProductsSubQuery + ")";

        // 2. Xóa các đơn hàng & thanh toán liên quan tới các biến thể sản phẩm bị xóa
        entityManager.createNativeQuery("DELETE FROM GiaoDichThanhToan WHERE id_hoa_don IN (SELECT id_hoa_don FROM HoaDonChiTiet WHERE id_san_pham_chi_tiet IN (" + invalidVariantsSubQuery + "))").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM TichHopVanChuyen WHERE id_hoa_don IN (SELECT id_hoa_don FROM HoaDonChiTiet WHERE id_san_pham_chi_tiet IN (" + invalidVariantsSubQuery + "))").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM HoaDonChiTiet WHERE id_san_pham_chi_tiet IN (" + invalidVariantsSubQuery + ")").executeUpdate();

        // 3. Xóa giỏ hàng & hình ảnh sản phẩm liên quan
        entityManager.createNativeQuery("DELETE FROM GioHangChiTiet WHERE id_san_pham_chi_tiet IN (" + invalidVariantsSubQuery + ")").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM HinhAnhSanPham WHERE id_san_pham_chi_tiet IN (" + invalidVariantsSubQuery + ")").executeUpdate();

        // 4. Xóa đợt giảm giá, đánh giá & yêu thích liên quan đến sản phẩm
        entityManager.createNativeQuery("DELETE FROM SanPham_DotGiamGia WHERE id_san_pham IN (" + invalidProductsSubQuery + ")").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM DanhGia WHERE id_san_pham IN (" + invalidProductsSubQuery + ")").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM SanPhamYeuThich WHERE id_san_pham IN (" + invalidProductsSubQuery + ")").executeUpdate();

        // 5. Xóa biến thể sản phẩm & sản phẩm
        int deletedVariants = entityManager.createNativeQuery("DELETE FROM SanPhamChiTiet WHERE id_san_pham IN (" + invalidProductsSubQuery + ")").executeUpdate();
        System.out.println("Đã xóa số biến thể sản phẩm không hợp lệ: " + deletedVariants);

        int deletedProducts = entityManager.createNativeQuery("DELETE FROM SanPham WHERE id_danh_muc NOT IN (1, 164) OR id_thuong_hieu NOT IN (1, 3)").executeUpdate();
        System.out.println("Đã xóa số sản phẩm không thuộc Vợt/Cước hoặc thuộc Victor: " + deletedProducts);



        // 7. Xóa các danh mục không thuộc (Vợt Cầu Lông #1, Cước #164)
        int deletedCategories = entityManager.createNativeQuery("DELETE FROM DanhMuc WHERE id NOT IN (1, 164)").executeUpdate();
        System.out.println("Đã xóa số danh mục dư thừa (chỉ giữ lại Vợt & Cước): " + deletedCategories);

        // 8. Xóa thương hiệu Victor và các thương hiệu khác (chỉ giữ lại Yonex #1 & Lining #3)
        int deletedBrands = entityManager.createNativeQuery("DELETE FROM ThuongHieu WHERE id NOT IN (1, 3)").executeUpdate();
        System.out.println("Đã xóa thương hiệu Victor và thương hiệu không dùng: " + deletedBrands);

        System.out.println("=== THÀNH CÔNG: ĐÃ HOÀN TẤT DỌN DẸP CSDL THEO YÊU CẦU ===");
    }
}
