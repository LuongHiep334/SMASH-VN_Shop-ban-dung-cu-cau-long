package com.smashvn.shop.service;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.smashvn.shop.controller.admin.AdminSanPhamController;
import com.smashvn.shop.controller.product.SanPhamController;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.admin.AdminSanPhamService;
import com.smashvn.shop.service.product.ProductAvailabilityService;

@SpringBootTest
@Transactional
public class ProductManagementNewFlowIntegrationTest {

    @Autowired
    private AdminSanPhamService adminSanPhamService;

    @Autowired
    private AdminSanPhamController adminSanPhamController;

    @Autowired
    private SanPhamController sanPhamController;

    @Autowired
    private ProductAvailabilityService productAvailabilityService;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    private DanhMuc activeCategory;
    private DanhMuc inactiveCategory;
    private ThuongHieu activeBrand;
    private ThuongHieu inactiveBrand;
    private TaiKhoan adminUser;
    private NhanVien adminNhanVien;

    @BeforeEach
    void setUp() {
        activeCategory = new DanhMuc();
        activeCategory.setTenDanhMuc("Vợt Cầu Lông Test");
        activeCategory.setTrangThai(true);
        activeCategory = danhMucRepository.save(activeCategory);

        inactiveCategory = new DanhMuc();
        inactiveCategory.setTenDanhMuc("Danh Mục Ẩn");
        inactiveCategory.setTrangThai(false);
        inactiveCategory = danhMucRepository.save(inactiveCategory);

        activeBrand = new ThuongHieu();
        activeBrand.setTenThuongHieu("Yonex Test");
        activeBrand.setTrangThai(true);
        activeBrand = thuongHieuRepository.save(activeBrand);

        inactiveBrand = new ThuongHieu();
        inactiveBrand.setTenThuongHieu("Thương Hiệu Ẩn");
        inactiveBrand.setTrangThai(false);
        inactiveBrand = thuongHieuRepository.save(inactiveBrand);

        adminUser = new TaiKhoan();
        adminUser.setUsername("admin_product_test_" + System.currentTimeMillis());
        adminUser.setMatKhau("123456");
        adminUser.setVaiTro("QL");
        adminUser.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        adminUser = taiKhoanRepository.save(adminUser);

        adminNhanVien = new NhanVien();
        adminNhanVien.setTaiKhoan(adminUser);
        adminNhanVien.setHoTenNv("Admin Product Manager");
        adminNhanVien.setChucVu("Quản lý");
        adminNhanVien.setSoDienThoaiNv("0912345678");
        adminNhanVien = nhanVienRepository.save(adminNhanVien);
    }

    private SanPham createBaseSanPham(String ten, DanhMuc dm, ThuongHieu th, boolean trangThai) {
        SanPham sp = new SanPham();
        sp.setTenSanPham(ten);
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(adminNhanVien);
        sp.setTrangThaiValue(trangThai);
        sp.setSanPhamChiTiets(new ArrayList<>());
        return sp;
    }

    @Test
    void testDangBanValidation_NoVariants_ShouldFail() {
        SanPham sp = createBaseSanPham("Sản Phẩm Không Có Biến Thể", activeCategory, activeBrand, false);
        sp = sanPhamRepository.save(sp);

        final Integer spId = sp.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminSanPhamService.dangBan(spId, adminUser.getId(), "127.0.0.1");
        });
        assertTrue(ex.getMessage().contains("ít nhất một biến thể"));
    }

    @Test
    void testDangBanValidation_ZeroPriceVariant_ShouldFail() {
        SanPham sp = createBaseSanPham("Sản Phẩm Giá 0 Đồng", activeCategory, activeBrand, false);
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet variant = new SanPhamChiTiet();
        variant.setSanPham(sp);
        variant.setGiaBan(BigDecimal.ZERO);
        variant.setSoLuongTon(10);
        variant.setTrangThaiValue(true);
        sanPhamChiTietRepository.save(variant);

        sp.getSanPhamChiTiets().add(variant);
        sanPhamRepository.save(sp);

        final Integer spId = sp.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminSanPhamService.dangBan(spId, adminUser.getId(), "127.0.0.1");
        });
        assertTrue(ex.getMessage().contains("giá bán hợp lệ"));
    }

    @Test
    void testDangBanValidation_InactiveCategory_ShouldFail() {
        SanPham sp = createBaseSanPham("Sản Phẩm Danh Mục Ẩn", inactiveCategory, activeBrand, false);
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet variant = new SanPhamChiTiet();
        variant.setSanPham(sp);
        variant.setGiaBan(new BigDecimal("1500000"));
        variant.setSoLuongTon(5);
        variant.setTrangThaiValue(true);
        sanPhamChiTietRepository.save(variant);

        sp.getSanPhamChiTiets().add(variant);
        sanPhamRepository.save(sp);

        final Integer spId = sp.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminSanPhamService.dangBan(spId, adminUser.getId(), "127.0.0.1");
        });
        assertTrue(ex.getMessage().contains("Danh mục"));
    }

    @Test
    void testDangBanValidation_InactiveBrand_ShouldFail() {
        SanPham sp = createBaseSanPham("Sản Phẩm Thương Hiệu Ẩn", activeCategory, inactiveBrand, false);
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet variant = new SanPhamChiTiet();
        variant.setSanPham(sp);
        variant.setGiaBan(new BigDecimal("1500000"));
        variant.setSoLuongTon(5);
        variant.setTrangThaiValue(true);
        sanPhamChiTietRepository.save(variant);

        sp.getSanPhamChiTiets().add(variant);
        sanPhamRepository.save(sp);

        final Integer spId = sp.getId();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminSanPhamService.dangBan(spId, adminUser.getId(), "127.0.0.1");
        });
        assertTrue(ex.getMessage().contains("Thương hiệu"));
    }

    @Test
    void testDangBanAndNgungHienThiSuccessFlow() {
        // 1. Tạo sản phẩm đang ngưng hiển thị
        SanPham sp = createBaseSanPham("Vợt Yonex Astrox 88D Pro Gen 3", activeCategory, activeBrand, false);
        sp.setMoTa("Vợt cầu lông cao cấp chuyên công");
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet variant = new SanPhamChiTiet();
        variant.setSanPham(sp);
        variant.setTrongLuong("4U");
        variant.setKichThuoc("G5");
        variant.setGiaBan(new BigDecimal("4200000"));
        variant.setSoLuongTon(20);
        variant.setTrangThaiValue(true);
        sanPhamChiTietRepository.save(variant);

        sp.getSanPhamChiTiets().add(variant);
        sanPhamRepository.save(sp);

        // Ban đầu đang ngưng hiển thị
        assertFalse(productAvailabilityService.isProductPublished(sp));
        assertFalse(productAvailabilityService.isVariantPublished(variant));

        // 2. Thực hiện ĐĂNG BÁN
        adminSanPhamService.dangBan(sp.getId(), adminUser.getId(), "127.0.0.1");

        SanPham updatedSp = sanPhamRepository.findById(sp.getId()).orElseThrow();
        assertTrue(Boolean.TRUE.equals(updatedSp.getTrangThaiValue()));
        assertEquals("dang_ban", updatedSp.getTrangThai());
        assertTrue(productAvailabilityService.isProductPublished(updatedSp));
        assertTrue(productAvailabilityService.isVariantPublished(variant));

        // 3. Thực hiện NGƯNG HIỂN THỊ
        adminSanPhamService.ngungHienThi(sp.getId(), adminUser.getId(), "127.0.0.1");

        SanPham unpubSp = sanPhamRepository.findById(sp.getId()).orElseThrow();
        assertTrue(Boolean.FALSE.equals(unpubSp.getTrangThaiValue()));
        assertEquals("ngung_kinh_doanh", unpubSp.getTrangThai());
        assertFalse(productAvailabilityService.isProductPublished(unpubSp));
        assertFalse(productAvailabilityService.isVariantPublished(variant));

        // Sản phẩm và biến thể vẫn nguyên vẹn trong DB
        assertEquals(1, unpubSp.getSanPhamChiTiets().size());
        assertEquals("4U", unpubSp.getSanPhamChiTiets().get(0).getTrongLuong());
    }

    @Test
    void testXemTruocSanPhamEndpoint() {
        // Tạo sản phẩm đang ngưng hiển thị
        SanPham sp = createBaseSanPham("Vợt Xem Trước Test", activeCategory, activeBrand, false);
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet variant = new SanPhamChiTiet();
        variant.setSanPham(sp);
        variant.setGiaBan(new BigDecimal("2000000"));
        variant.setSoLuongTon(10);
        variant.setTrangThaiValue(true);
        sanPhamChiTietRepository.save(variant);

        sp.getSanPhamChiTiets().add(variant);
        sanPhamRepository.save(sp);

        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", adminUser.getId());

        String viewName = adminSanPhamController.xemTruocSanPham(sp.getId(), model, session);

        assertEquals("product-detail", viewName);
        assertTrue(Boolean.TRUE.equals(model.getAttribute("previewMode")));
        assertTrue(Boolean.TRUE.equals(model.getAttribute("sanPhamKhaDung")));
        assertNotNull(model.getAttribute("sp"));
        assertEquals("Vợt Xem Trước Test", ((SanPham) model.getAttribute("sp")).getTenSanPham());
    }

    @Test
    void testCustomerAccessUnpublishedProduct_ShouldRedirect() {
        // Tạo sản phẩm đang ngưng hiển thị
        SanPham sp = createBaseSanPham("Sản Phẩm Đã Ẩn", activeCategory, activeBrand, false);
        sp = sanPhamRepository.save(sp);

        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        String view = sanPhamController.hienThiChiTietSanPham(sp.getId(), model, session);
        assertTrue(view.startsWith("redirect:/shop?loi="));
    }

    @Test
    void testAdminControllerDangBanAndNgungHienThiActions() {
        SanPham sp = createBaseSanPham("Vợt Admin Action Test", activeCategory, activeBrand, false);
        sp = sanPhamRepository.save(sp);

        SanPhamChiTiet variant = new SanPhamChiTiet();
        variant.setSanPham(sp);
        variant.setGiaBan(new BigDecimal("3000000"));
        variant.setSoLuongTon(15);
        variant.setTrangThaiValue(true);
        sanPhamChiTietRepository.save(variant);

        sp.getSanPhamChiTiets().add(variant);
        sanPhamRepository.save(sp);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", adminUser.getId());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        // 1. Action Đăng bán (mặc định quay về danh sách)
        String viewDangBan = adminSanPhamController.xuLyDangBanSanPham(sp.getId(), null, session, request, redirectAttributes);
        assertEquals("redirect:/admin/san-pham", viewDangBan);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("success"));

        SanPham dbSp = sanPhamRepository.findById(sp.getId()).orElseThrow();
        assertTrue(Boolean.TRUE.equals(dbSp.getTrangThaiValue()));

        // 2. Action Ngưng hiển thị (có redirectUrl giữ nguyên trang sửa)
        redirectAttributes = new RedirectAttributesModelMap();
        String customRedirect = "/admin/san-pham/sua/" + sp.getId();
        String viewNgungHienThi = adminSanPhamController.xuLyNgungHienThiSanPham(sp.getId(), customRedirect, session, request, redirectAttributes);
        assertEquals("redirect:/admin/san-pham/sua/" + sp.getId(), viewNgungHienThi);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("success"));

        dbSp = sanPhamRepository.findById(sp.getId()).orElseThrow();
        assertTrue(Boolean.FALSE.equals(dbSp.getTrangThaiValue()));
    }
}
