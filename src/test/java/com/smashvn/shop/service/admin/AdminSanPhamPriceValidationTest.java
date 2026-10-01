package com.smashvn.shop.service.admin;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dto.BienTheCreateRequest;
import com.smashvn.shop.dto.SanPhamCreateRequest;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminSanPhamPriceValidationTest {

    @Autowired
    private AdminSanPhamService adminSanPhamService;

    @Autowired
    private AdminBienTheService adminBienTheService;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    private DanhMuc categoryVot;
    private DanhMuc categoryHopCau;
    private ThuongHieu brandYonex;
    private NhanVien nhanVien;
    private MockMultipartFile validImage;

    @BeforeEach
    void setUp() {
        categoryVot = danhMucRepository.findAll().stream()
                .filter(dm -> Boolean.TRUE.equals(dm.getTrangThai()) && (dm.getTenDanhMuc().toLowerCase().startsWith("vợt") || dm.getTenDanhMuc().toLowerCase().startsWith("vot")))
                .findFirst()
                .orElseGet(() -> {
                    DanhMuc dm = new DanhMuc();
                    dm.setTenDanhMuc("Vợt cầu lông Test");
                    dm.setTrangThai(true);
                    return danhMucRepository.save(dm);
                });

        categoryHopCau = danhMucRepository.findAll().stream()
                .filter(dm -> Boolean.TRUE.equals(dm.getTrangThai()) && (dm.getTenDanhMuc().toLowerCase().startsWith("hộp cầu") || dm.getTenDanhMuc().toLowerCase().startsWith("hop cau") || dm.getTenDanhMuc().toLowerCase().startsWith("quả cầu")))
                .findFirst()
                .orElseGet(() -> {
                    DanhMuc dm = new DanhMuc();
                    dm.setTenDanhMuc("Hộp cầu lông Test");
                    dm.setTrangThai(true);
                    return danhMucRepository.save(dm);
                });

        brandYonex = thuongHieuRepository.findAll().stream()
                .filter(th -> Boolean.TRUE.equals(th.getTrangThai()))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu th = new ThuongHieu();
                    th.setTenThuongHieu("Yonex Test");
                    th.setTrangThai(true);
                    return thuongHieuRepository.save(th);
                });

        List<NhanVien> listNV = nhanVienRepository.findAll();
        if (!listNV.isEmpty()) {
            nhanVien = listNV.get(0);
        } else {
            nhanVien = new NhanVien();
            nhanVien.setHoTenNv("Test NV");
            nhanVien.setChucVu("Quản lý");
            nhanVien.setSoDienThoaiNv("0987654321");
            nhanVien = nhanVienRepository.save(nhanVien);
        }

        validImage = new MockMultipartFile(
                "fileAnh",
                "test-racket.jpg",
                "image/jpeg",
                "fake-image-bytes".getBytes()
        );
    }

    @Test
    @DisplayName("Thêm sản phẩm mới với biến thể có giá nhập cao hơn giá bán -> Bị từ chối và yêu cầu nhập lại")
    void testThemSanPhamVaBienThe_WhenGiaNhapCaoHonGiaBan_ShouldThrowException() {
        SanPhamCreateRequest req = new SanPhamCreateRequest();
        req.setTenSanPham("Vợt Test Giá Nhập Cao Hơn Giá Bán");
        req.setIdDanhMuc(categoryVot.getId());
        req.setIdThuongHieu(brandYonex.getId());
        req.setMoTa("Mô tả test");
        req.setFileAnh(validImage);

        BienTheCreateRequest variant = new BienTheCreateRequest();
        variant.setMauSac("Đỏ");
        variant.setTrongLuong("4U");
        variant.setGiaNhap(new BigDecimal("3500000")); // Giá nhập 3.500.000 đ
        variant.setGiaBan(new BigDecimal("3000000"));  // Giá bán 3.000.000 đ
        variant.setSoLuongTon(10);

        req.setVariants(List.of(variant));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminSanPhamService.themSanPhamVaBienThe(req, null, "127.0.0.1");
        });

        assertTrue(ex.getMessage().contains("Giá nhập hiện tại đang cao hơn giá bán"), 
                "Thông báo lỗi phải nêu rõ giá nhập cao hơn giá bán: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Vui lòng kiểm tra và nhập lại!"), 
                "Thông báo lỗi phải yêu cầu nhập lại: " + ex.getMessage());
    }

    @Test
    @DisplayName("Thêm sản phẩm Hộp Cầu với giá nhập cao hơn giá bán -> Bị từ chối và yêu cầu nhập lại")
    void testThemSanPhamHopCau_WhenGiaNhapCaoHonGiaBan_ShouldThrowException() {
        SanPhamCreateRequest req = new SanPhamCreateRequest();
        req.setTenSanPham("Hộp Cầu Test Giá Cao");
        req.setIdDanhMuc(categoryHopCau.getId());
        req.setIdThuongHieu(brandYonex.getId());
        req.setMoTa("Mô tả test hộp cầu");
        req.setFileAnh(validImage);
        req.setGiaNhapDefault(new BigDecimal("250000")); // Giá nhập 250.000 đ
        req.setGiaBanDefault(new BigDecimal("200000"));  // Giá bán 200.000 đ
        req.setSoLuongTonDefault(20);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminSanPhamService.themSanPhamVaBienThe(req, null, "127.0.0.1");
        });

        assertTrue(ex.getMessage().contains("Giá nhập hiện tại đang cao hơn giá bán"), 
                "Thông báo lỗi phải nêu rõ giá nhập cao hơn giá bán: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Vui lòng kiểm tra và nhập lại!"), 
                "Thông báo lỗi phải yêu cầu nhập lại: " + ex.getMessage());
    }

    @Test
    @DisplayName("Thêm biến thể đơn lẻ có giá nhập cao hơn giá bán -> Bị từ chối")
    void testThemBienThe_WhenGiaNhapCaoHonGiaBan_ShouldThrowException() {
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Cho Biến Thể");
        sp.setDanhMuc(categoryVot);
        sp.setThuongHieu(brandYonex);
        sp.setNhanVien(nhanVien);
        sp.setTrangThai("dang_ban");
        sp = sanPhamRepository.save(sp);

        final Integer spId = sp.getId();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            adminBienTheService.themBienThe(
                    spId,
                    new BigDecimal("2000000"), // Giá bán 2.000.000 đ
                    new BigDecimal("2500000"), // Giá nhập 2.500.000 đ
                    10,
                    "Xanh dương",
                    "4U",
                    null,
                    null,
                    validImage,
                    null
            );
        });

        assertTrue(ex.getMessage().contains("Giá nhập hiện tại đang cao hơn giá bán"), 
                "Thông báo lỗi phải nêu rõ giá nhập cao hơn giá bán: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Vui lòng kiểm tra và nhập lại!"), 
                "Thông báo lỗi phải yêu cầu nhập lại: " + ex.getMessage());
    }

    @Test
    @DisplayName("Thêm sản phẩm với giá nhập <= giá bán -> Thành công không báo lỗi giá")
    void testThemSanPhamVaBienThe_WhenGiaNhapHopLe_ShouldSucceed() throws Exception {
        SanPhamCreateRequest req = new SanPhamCreateRequest();
        req.setTenSanPham("Vợt Hợp Lệ 88D Pro Test");
        req.setIdDanhMuc(categoryVot.getId());
        req.setIdThuongHieu(brandYonex.getId());
        req.setMoTa("Mô tả hợp lệ");
        req.setFileAnh(validImage);

        BienTheCreateRequest variant = new BienTheCreateRequest();
        variant.setMauSac("Đen");
        variant.setTrongLuong("4U");
        variant.setGiaNhap(new BigDecimal("1800000")); // Giá nhập 1.800.000 đ
        variant.setGiaBan(new BigDecimal("2500000"));  // Giá bán 2.500.000 đ
        variant.setSoLuongTon(15);

        req.setVariants(List.of(variant));

        assertDoesNotThrow(() -> {
            adminSanPhamService.themSanPhamVaBienThe(req, null, "127.0.0.1");
        });
    }
}
