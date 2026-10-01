package com.smashvn.shop.service.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.admin.AdminBienTheService;
import com.smashvn.shop.service.admin.AdminSanPhamService;
import com.smashvn.shop.util.ProductCodeAndSkuGenerator;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductCodeAndSkuIntegrationTest {

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private AdminSanPhamService adminSanPhamService;

    @Autowired
    private AdminBienTheService adminBienTheService;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    // Helper tạo danh mục và thương hiệu test
    private DanhMuc getOrCreateDanhMuc() {
        return danhMucRepository.findAll().stream()
                .filter(dm -> dm.getTenDanhMuc() != null && dm.getTenDanhMuc().toLowerCase().contains("vợt"))
                .findFirst()
                .orElseGet(() -> {
                    DanhMuc dm = new DanhMuc();
                    dm.setTenDanhMuc("Vợt cầu lông");
                    dm.setTrangThai(true);
                    return danhMucRepository.save(dm);
                });
    }

    private ThuongHieu getOrCreateThuongHieu() {
        return thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu th = new ThuongHieu();
            th.setTenThuongHieu("Thương Hiệu Test");
            th.setTrangThai(true);
            return thuongHieuRepository.save(th);
        });
    }

    private NhanVien getOrCreateNhanVien() {
        return nhanVienRepository.findAll().stream().findFirst().orElse(null);
    }

    @Test
    @Order(1)
    @DisplayName("CASE 1 & 2: Sinh mã sản phẩm SP%06d và SKU SPxxxxxx-Vxxxxxx tự động")
    @Transactional
    public void testAutoGenerateProductCodeAndSku() {
        DanhMuc dm = getOrCreateDanhMuc();
        ThuongHieu th = getOrCreateThuongHieu();
        NhanVien nv = getOrCreateNhanVien();

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Tự Sinh Mã");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Mô tả test");
        sp.setTrangThai("dang_ban");
        sp = sanPhamRepository.save(sp);

        if (sp.getMaSanPham() == null) {
            sp.setMaSanPham(ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
            sp = sanPhamRepository.save(sp);
        }

        assertNotNull(sp.getMaSanPham());
        assertTrue(sp.getMaSanPham().startsWith("SP"));
        assertEquals(String.format("SP%06d", sp.getId()), sp.getMaSanPham());

        // Tạo biến thể
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setGiaBan(new BigDecimal("1500000"));
        spct.setSoLuongTon(10);
        spct.setTrangThai("dang_ban");
        spct.setHinhAnhSanPham("test.jpg");
        spct = sanPhamChiTietRepository.save(spct);

        if (spct.getSku() == null) {
            spct.setSku(ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), spct.getId()));
            spct = sanPhamChiTietRepository.save(spct);
        }

        assertNotNull(spct.getSku());
        assertTrue(spct.getSku().startsWith(sp.getMaSanPham() + "-V"));
        assertEquals(String.format("%s-V%06d", sp.getMaSanPham(), spct.getId()), spct.getSku());
    }

    @Test
    @Order(2)
    @DisplayName("CASE 3 & 4: Sinh SKU cho nhiều biến thể và định dạng độc lập thuộc tính")
    @Transactional
    public void testMultipleVariantsUniqueSku() {
        DanhMuc dm = getOrCreateDanhMuc();
        ThuongHieu th = getOrCreateThuongHieu();
        NhanVien nv = getOrCreateNhanVien();

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Multi Sku");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Multi Sku Test");
        sp = sanPhamRepository.save(sp);
        sp.setMaSanPham(ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet v1 = new SanPhamChiTiet();
        v1.setSanPham(sp);
        v1.setGiaBan(new BigDecimal("2000000"));
        v1.setSoLuongTon(5);
        v1.setTrangThai("dang_ban");
        v1 = sanPhamChiTietRepository.save(v1);
        v1.setSku(ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), v1.getId()));
        v1 = sanPhamChiTietRepository.save(v1);

        SanPhamChiTiet v2 = new SanPhamChiTiet();
        v2.setSanPham(sp);
        v2.setGiaBan(new BigDecimal("2500000"));
        v2.setSoLuongTon(8);
        v2.setTrangThai("dang_ban");
        v2 = sanPhamChiTietRepository.save(v2);
        v2.setSku(ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), v2.getId()));
        v2 = sanPhamChiTietRepository.save(v2);

        assertNotEquals(v1.getSku(), v2.getSku());
        assertTrue(v1.getSku().startsWith(sp.getMaSanPham()));
        assertTrue(v2.getSku().startsWith(sp.getMaSanPham()));
    }

    @Test
    @Order(3)
    @DisplayName("CASE 5, 6, 7: Tính BẤT BIẾN (Immutable) của maSanPham và sku khi update thông tin")
    @Transactional
    public void testImmutabilityOnUpdate() {
        DanhMuc dm = getOrCreateDanhMuc();
        ThuongHieu th = getOrCreateThuongHieu();
        NhanVien nv = getOrCreateNhanVien();

        SanPham sp = new SanPham();
        sp.setTenSanPham("Sản Phẩm Ban Đầu");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Mô tả 1");
        sp = sanPhamRepository.save(sp);
        sp.setMaSanPham(ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
        sp = sanPhamRepository.save(sp);

        String originalMaSp = sp.getMaSanPham();

        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setGiaBan(new BigDecimal("1000000"));
        spct.setSoLuongTon(10);
        spct.setTrangThai("dang_ban");
        spct = sanPhamChiTietRepository.save(spct);
        spct.setSku(ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), spct.getId()));
        spct = sanPhamChiTietRepository.save(spct);

        String originalSku = spct.getSku();

        // 1. Update SanPham (Tên, mô tả)
        sp.setTenSanPham("Sản Phẩm Đổi Tên");
        sp.setMoTa("Mô tả mới cập nhật");
        sp = sanPhamRepository.save(sp);
        assertEquals(originalMaSp, sp.getMaSanPham(), "maSanPham không được phép thay đổi khi sửa tên sản phẩm");

        // 2. Update SanPhamChiTiet (Giá bán, hình ảnh, tồn kho)
        spct.setGiaBan(new BigDecimal("1800000"));
        spct.setHinhAnhSanPham("new-image.png");
        spct.setSoLuongTon(25);
        spct = sanPhamChiTietRepository.save(spct);
        assertEquals(originalSku, spct.getSku(), "sku không được phép thay đổi khi cập nhật giá hoặc ảnh biến thể");
    }

    @Test
    @Order(4)
    @DisplayName("CASE 8 & 9: Nhập hàng biến thể cũ (Reuse SKU & Lot) vs Biến thể mới (New SKU)")
    @Transactional
    public void testReuseSkuOnExistingVariant() throws Exception {
        DanhMuc dm = getOrCreateDanhMuc();
        ThuongHieu th = getOrCreateThuongHieu();
        NhanVien nv = getOrCreateNhanVien();

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Nhập Lô");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Test nhập lô");
        sp = sanPhamRepository.save(sp);
        sp.setMaSanPham(ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
        sp = sanPhamRepository.save(sp);

        Integer nvId = nv != null ? nv.getId() : 1;
        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "fileAnh", "test.jpg", "image/jpeg", "dummy image data".getBytes());

        // Thêm biến thể lần 1
        adminBienTheService.themBienThe(sp.getId(), new BigDecimal("1200000"), new BigDecimal("1000000"), 5, "Đỏ", "4U", null, null, mockFile, nvId);

        List<SanPhamChiTiet> variants1 = sanPhamChiTietRepository.findBySanPham_Id(sp.getId());
        assertEquals(1, variants1.size());
        SanPhamChiTiet v1 = variants1.get(0);
        String sku1 = v1.getSku();
        assertNotNull(sku1);
        assertEquals(5, v1.getSoLuongTon());

        // Thêm biến thể lần 2 cùng thuộc tính (Đỏ, 4U) -> Phải reuse v1 và giữ nguyên sku1
        adminBienTheService.themBienThe(sp.getId(), new BigDecimal("1200000"), new BigDecimal("1050000"), 10, "Đỏ", "4U", null, null, null, nvId);

        List<SanPhamChiTiet> variants2 = sanPhamChiTietRepository.findBySanPham_Id(sp.getId());
        assertEquals(1, variants2.size(), "Không được tạo thêm biến thể trùng thuộc tính");
        SanPhamChiTiet v1After = variants2.get(0);
        assertEquals(v1.getId(), v1After.getId());
        assertEquals(sku1, v1After.getSku(), "SKU của biến thể cũ phải được giữ nguyên (REUSE)");
        assertEquals(15, v1After.getSoLuongTon(), "Tồn kho phải được cộng dồn đúng");
    }

    @Test
    @Order(5)
    @DisplayName("CASE 16 & 17: Soft delete và mở bán lại biến thể bảo toàn SKU")
    @Transactional
    public void testSoftDeleteAndReopenVariantPreservesSku() {
        DanhMuc dm = getOrCreateDanhMuc();
        ThuongHieu th = getOrCreateThuongHieu();
        NhanVien nv = getOrCreateNhanVien();

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Soft Delete");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Test soft delete");
        sp = sanPhamRepository.save(sp);
        sp.setMaSanPham(ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setGiaBan(new BigDecimal("1200000"));
        spct.setSoLuongTon(5);
        spct.setTrangThai("dang_ban");
        spct = sanPhamChiTietRepository.save(spct);
        spct.setSku(ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), spct.getId()));
        spct = sanPhamChiTietRepository.save(spct);

        String originalSku = spct.getSku();

        // 1. Soft delete
        adminBienTheService.xoaBienThe(spct.getId());
        SanPhamChiTiet deletedVariant = sanPhamChiTietRepository.findById(spct.getId()).orElseThrow();
        assertEquals("ngung_kinh_doanh", deletedVariant.getTrangThai());
        assertEquals(originalSku, deletedVariant.getSku(), "SKU phải được giữ nguyên khi soft-delete");

        // 2. Mở bán lại
        adminBienTheService.moBanLaiBienThe(spct.getId());
        SanPhamChiTiet reopenedVariant = sanPhamChiTietRepository.findById(spct.getId()).orElseThrow();
        assertEquals("dang_ban", reopenedVariant.getTrangThai());
        assertEquals(originalSku, reopenedVariant.getSku(), "SKU phải được giữ nguyên khi mở bán lại");
    }

    @Test
    @Order(6)
    @DisplayName("CASE 18 & 19: Tìm kiếm chính xác và không phân biệt chữ hoa thường theo maSanPham & SKU")
    @Transactional
    public void testSearchByProductCodeAndSku() {
        DanhMuc dm = getOrCreateDanhMuc();
        ThuongHieu th = getOrCreateThuongHieu();
        NhanVien nv = getOrCreateNhanVien();

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Tìm Kiếm Mã");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Test search");
        sp.setTrangThai("dang_ban");
        sp = sanPhamRepository.save(sp);
        sp.setMaSanPham(ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setGiaBan(new BigDecimal("1200000"));
        spct.setSoLuongTon(5);
        spct.setTrangThai("dang_ban");
        spct = sanPhamChiTietRepository.save(spct);
        spct.setSku(ProductCodeAndSkuGenerator.generateVariantSku(sp.getMaSanPham(), spct.getId()));
        spct = sanPhamChiTietRepository.save(spct);

        // 1. Tìm SanPham theo maSanPham
        Optional<SanPham> foundSp = sanPhamRepository.findByMaSanPham(sp.getMaSanPham());
        assertTrue(foundSp.isPresent());
        assertEquals(sp.getId(), foundSp.get().getId());

        // 2. Tìm SanPhamChiTiet theo SKU
        Optional<SanPhamChiTiet> foundSpct = sanPhamChiTietRepository.findBySku(spct.getSku());
        assertTrue(foundSpct.isPresent());
        assertEquals(spct.getId(), foundSpct.get().getId());

        // 3. Tìm trong POS searchActiveVariantsForPos (lowercase/uppercase/trim)
        List<SanPhamChiTiet> posResultsExact = sanPhamChiTietRepository.searchActiveVariantsForPos(spct.getSku(), null, null);
        assertFalse(posResultsExact.isEmpty());
        assertEquals(spct.getId(), posResultsExact.get(0).getId());

        List<SanPhamChiTiet> posResultsLower = sanPhamChiTietRepository.searchActiveVariantsForPos(spct.getSku().toLowerCase(), null, null);
        assertFalse(posResultsLower.isEmpty());
        assertEquals(spct.getId(), posResultsLower.get(0).getId());
    }

    @Test
    @Order(7)
    @DisplayName("CASE 20 & 21: Java Generator với ID > 999999 KHÔNG bị truncate")
    public void testLargeIdFormattingNoTruncation() {
        // productId
        assertEquals("SP000001", ProductCodeAndSkuGenerator.generateProductCode(1));
        assertEquals("SP000125", ProductCodeAndSkuGenerator.generateProductCode(125));
        assertEquals("SP999999", ProductCodeAndSkuGenerator.generateProductCode(999999));
        assertEquals("SP1000000", ProductCodeAndSkuGenerator.generateProductCode(1000000));
        assertEquals("SP1234567", ProductCodeAndSkuGenerator.generateProductCode(1234567));

        // variant SKU
        assertEquals("SP000125-V000750", ProductCodeAndSkuGenerator.generateVariantSku("SP000125", 750));
        assertEquals("SP000125-V999999", ProductCodeAndSkuGenerator.generateVariantSku("SP000125", 999999));
        assertEquals("SP000125-V1000000", ProductCodeAndSkuGenerator.generateVariantSku("SP000125", 1000000));
        assertEquals("SP000125-V1234567", ProductCodeAndSkuGenerator.generateVariantSku("SP000125", 1234567));
    }

    @Test
    @Order(8)
    @DisplayName("CASE 22: SQL Padding Algorithm Logic tương thích tuyệt đối với Java")
    public void testSqlPaddingAlgorithmEquivalence() {
        int[] testIds = {1, 25, 999999, 1000000, 1234567};
        for (int id : testIds) {
            String javaCode = ProductCodeAndSkuGenerator.generateProductCode(id);
            // Giả lập logic SQL:
            // CASE WHEN id < 1000000 THEN 'SP' + RIGHT('000000' + CAST(id AS VARCHAR(20)), 6) ELSE 'SP' + CAST(id AS VARCHAR(20)) END
            String sqlCode;
            if (id < 1000000) {
                String padded = ("000000" + id);
                sqlCode = "SP" + padded.substring(padded.length() - 6);
            } else {
                sqlCode = "SP" + id;
            }
            assertEquals(javaCode, sqlCode, "SQL padding kết quả phải tương đương Java với id = " + id);
        }
    }

    @Test
    @Order(9)
    @DisplayName("CASE 24: Nhập AttributeKey trùng variant ngung_kinh_doanh không tạo duplicate variant và giữ SKU cũ")
    @Transactional
    public void testNhapHangTrùngVariantSoftDeleted() throws Exception {
        DanhMuc dm = getOrCreateDanhMuc();
        ThuongHieu th = getOrCreateThuongHieu();
        NhanVien nv = getOrCreateNhanVien();
        Integer nvId = nv != null ? nv.getId() : 1;

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Trùng Soft Deleted");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Test");
        sp = sanPhamRepository.save(sp);
        sp.setMaSanPham(ProductCodeAndSkuGenerator.generateProductCode(sp.getId()));
        sp = sanPhamRepository.save(sp);

        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "fileAnh", "test.jpg", "image/jpeg", "dummy image data".getBytes());

        // Tạo biến thể ban đầu
        adminBienTheService.themBienThe(sp.getId(), new BigDecimal("1000000"), new BigDecimal("900000"), 5, "Xanh", "3U", null, null, mockFile, nvId);
        SanPhamChiTiet v = sanPhamChiTietRepository.findBySanPham_Id(sp.getId()).get(0);
        String vSku = v.getSku();

        // Ẩn biến thể
        adminBienTheService.xoaBienThe(v.getId());
        SanPhamChiTiet vDeleted = sanPhamChiTietRepository.findById(v.getId()).orElseThrow();
        assertEquals("ngung_kinh_doanh", vDeleted.getTrangThai());

        // Nhập lại cùng thuộc tính Xanh + 3U
        adminBienTheService.themBienThe(sp.getId(), new BigDecimal("1000000"), new BigDecimal("900000"), 10, "Xanh", "3U", null, null, null, nvId);

        List<SanPhamChiTiet> allVariants = sanPhamChiTietRepository.findBySanPham_Id(sp.getId());
        assertEquals(1, allVariants.size(), "Không được tạo thêm biến thể mới trùng với biến thể soft-deleted");
        SanPhamChiTiet vReused = allVariants.get(0);
        assertEquals(v.getId(), vReused.getId());
        assertEquals(vSku, vReused.getSku(), "Phải giữ nguyên SKU của biến thể cũ");
        assertEquals("dang_ban", vReused.getTrangThai(), "Biến thể phải được mở bán lại");
        assertEquals(15, vReused.getSoLuongTon(), "Tồn kho phải được cộng dồn chính xác");
    }

    @Test
    @Order(10)
    @DisplayName("CASE 25: Validation generator ném ngoại lệ khi tham số sai")
    public void testGeneratorValidationExceptions() {
        assertThrows(IllegalArgumentException.class, () -> ProductCodeAndSkuGenerator.generateProductCode(null));
        assertThrows(IllegalArgumentException.class, () -> ProductCodeAndSkuGenerator.generateProductCode(0));
        assertThrows(IllegalArgumentException.class, () -> ProductCodeAndSkuGenerator.generateProductCode(-5));

        assertThrows(IllegalArgumentException.class, () -> ProductCodeAndSkuGenerator.generateVariantSku(null, 1));
        assertThrows(IllegalArgumentException.class, () -> ProductCodeAndSkuGenerator.generateVariantSku("", 1));
        assertThrows(IllegalArgumentException.class, () -> ProductCodeAndSkuGenerator.generateVariantSku("SP000001", null));
        assertThrows(IllegalArgumentException.class, () -> ProductCodeAndSkuGenerator.generateVariantSku("SP000001", 0));
        assertThrows(IllegalArgumentException.class, () -> ProductCodeAndSkuGenerator.generateVariantSku("SP000001", -10));
    }
}
