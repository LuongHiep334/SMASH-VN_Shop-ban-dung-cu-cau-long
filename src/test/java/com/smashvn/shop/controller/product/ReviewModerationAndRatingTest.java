package com.smashvn.shop.controller.product;

import com.smashvn.shop.dao.DanhGiaDAO;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.product.DanhGiaService;
import com.smashvn.shop.service.blog.CommentModerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ReviewModerationAndRatingTest {

    @Autowired
    private DanhGiaService danhGiaService;

    @Autowired
    private DanhGiaDAO danhGiaDAO;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private CommentViolationLogRepository commentViolationLogRepository;

    @Autowired
    private CommentModerationKeywordRepository keywordRepository;

    @Autowired
    private CommentModerationService commentModerationService;

    private SanPham testProduct;
    private TaiKhoan userAccount;
    private TaiKhoan adminAccount;
    private KhachHang testCustomer;

    @BeforeEach
    public void setUp() {
        // Clear all previous keywords
        keywordRepository.deleteAll();
        commentModerationService.clearKeywordCache();

        // Create testing setup
        DanhMuc dm = new DanhMuc();
        dm.setTenDanhMuc("Test Category");
        dm.setTrangThai(true);
        dm = danhMucRepository.save(dm);

        ThuongHieu th = new ThuongHieu();
        th.setTenThuongHieu("Test Brand");
        th.setTrangThai(true);
        th = thuongHieuRepository.save(th);

        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan nvUser = new TaiKhoan();
            nvUser.setUsername("staff_" + System.currentTimeMillis());
            nvUser.setMatKhau("pass123");
            nvUser.setVaiTro("NV");
            nvUser.setTrangThai("hoat_dong");
            nvUser.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
            nvUser = taiKhoanRepository.save(nvUser);

            NhanVien newNv = new NhanVien();
            newNv.setTaiKhoan(nvUser);
            newNv.setHoTenNv("Staff");
            newNv.setChucVu("Nhân viên");
            newNv.setSoDienThoaiNv("0981112223");
            return nhanVienRepository.save(newNv);
        });

        testProduct = new SanPham();
        testProduct.setTenSanPham("Test Racquet");
        testProduct.setDanhMuc(dm);
        testProduct.setThuongHieu(th);
        testProduct.setNhanVien(nv);
        testProduct.setTrangThai("dang_ban");
        testProduct.setSoDanhGia(0);
        testProduct.setDiemTrungBinh(0.0);
        testProduct = sanPhamRepository.save(testProduct);

        userAccount = new TaiKhoan();
        userAccount.setUsername("testuser_" + System.currentTimeMillis());
        userAccount.setMatKhau("password");
        userAccount.setVaiTro("KH");
        userAccount.setTrangThai("hoat_dong");
        userAccount.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        userAccount = taiKhoanRepository.save(userAccount);

        adminAccount = new TaiKhoan();
        adminAccount.setUsername("admin_" + System.currentTimeMillis());
        adminAccount.setMatKhau("password");
        adminAccount.setVaiTro("QL");
        adminAccount.setTrangThai("hoat_dong");
        adminAccount.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        adminAccount = taiKhoanRepository.save(adminAccount);

        testCustomer = new KhachHang();
        testCustomer.setHoKh("Nguyen");
        testCustomer.setTenKh("Customer");
        testCustomer.setTaiKhoan(userAccount);
        testCustomer = khachHangRepository.save(testCustomer);
    }

    @Test
    public void testManualHideShowImage() {
        // 1. Create a review with an image (implicitly not hidden)
        DanhGia dg = DanhGia.builder()
                .khachHang(testCustomer)
                .sanPham(testProduct)
                .soSao(4.0)
                .binhLuan("Great racquet!")
                .binhLuanAn(false)
                .hinhAnhAn(false)
                .daXoa(false)
                .ngayDanhGia(LocalDateTime.now())
                .build();
        dg = danhGiaDAO.saveAndFlush(dg);

        // Calculate initial rating
        danhGiaService.updateProductRatingStats(testProduct.getId());
        
        SanPham sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(1, sp.getSoDanhGia());
        assertEquals(4.0, sp.getDiemTrungBinh());

        // 2. Hide image manually
        danhGiaService.anHinhAnh(dg.getId(), adminAccount.getId());

        // Verify image is hidden but rating is NOT recalculated (remains 4.0 stars)
        DanhGia updatedDg = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertTrue(updatedDg.getAnHinhAnh());
        
        sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(1, sp.getSoDanhGia());
        assertEquals(4.0, sp.getDiemTrungBinh());

        // 3. Show image manually
        danhGiaService.hienHinhAnh(dg.getId(), adminAccount.getId());

        // Verify image is visible but rating is NOT recalculated
        updatedDg = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertFalse(updatedDg.getAnHinhAnh());

        sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(1, sp.getSoDanhGia());
        assertEquals(4.0, sp.getDiemTrungBinh());
    }

    @Test
    public void testManualHideShowText() {
        // 1. Create review
        DanhGia dg = DanhGia.builder()
                .khachHang(testCustomer)
                .sanPham(testProduct)
                .soSao(5.0)
                .binhLuan("Perfect racquet!")
                .binhLuanAn(false)
                .daXoa(false)
                .ngayDanhGia(LocalDateTime.now())
                .build();
        final DanhGia savedDg = danhGiaDAO.saveAndFlush(dg);

        danhGiaService.updateProductRatingStats(testProduct.getId());
        SanPham sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(1, sp.getSoDanhGia());
        assertEquals(5.0, sp.getDiemTrungBinh());

        // 2. Hide text manually
        danhGiaService.anBinhLuan(savedDg.getId(), adminAccount.getId());

        // Verify rating is updated and comment is excluded (0 reviews, 0.0 stars)
        sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(0, sp.getSoDanhGia());
        assertEquals(0.0, sp.getDiemTrungBinh());

        // 3. Try to show text when it does NOT contain banned words
        danhGiaService.hienBinhLuan(savedDg.getId(), adminAccount.getId());

        // Verify rating is recalculated back to 5.0 stars
        sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(1, sp.getSoDanhGia());
        assertEquals(5.0, sp.getDiemTrungBinh());

        // 4. Hide it again
        danhGiaService.anBinhLuan(savedDg.getId(), adminAccount.getId());

        // 5. Add "racquet" to banned keywords
        CommentModerationKeyword keyword = CommentModerationKeyword.builder()
                .keyword("racquet")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        keywordRepository.saveAndFlush(keyword);
        commentModerationService.clearKeywordCache();
        danhGiaService.scanAndModerateReviews(); // Clears cache internally or we clear it

        // 6. Try to show text now. It should fail because "Perfect racquet!" contains "racquet" (banned)
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            danhGiaService.hienBinhLuan(savedDg.getId(), adminAccount.getId());
        });
        assertTrue(ex.getMessage().contains("vi phạm từ khóa cấm"));
    }

    @Test
    public void testSoftDeleteReview() {
        // 1. Create review
        DanhGia dg = DanhGia.builder()
                .khachHang(testCustomer)
                .sanPham(testProduct)
                .soSao(3.0)
                .binhLuan("Decent.")
                .binhLuanAn(false)
                .daXoa(false)
                .ngayDanhGia(LocalDateTime.now())
                .build();
        dg = danhGiaDAO.saveAndFlush(dg);

        danhGiaService.updateProductRatingStats(testProduct.getId());
        SanPham sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(1, sp.getSoDanhGia());
        assertEquals(3.0, sp.getDiemTrungBinh());

        // 2. Soft delete review
        danhGiaService.xoaMemDanhGia(dg.getId(), adminAccount.getId());

        // Verify rating is recalculated and deleted review is excluded
        sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(0, sp.getSoDanhGia());
        assertEquals(0.0, sp.getDiemTrungBinh());
    }

    @Test
    public void testSchedulerAutoScan() {
        // 1. Add review with a normal word "apple"
        DanhGia dg = DanhGia.builder()
                .khachHang(testCustomer)
                .sanPham(testProduct)
                .soSao(5.0)
                .binhLuan("I like apple")
                .binhLuanAn(false)
                .daXoa(false)
                .ngayDanhGia(LocalDateTime.now())
                .build();
        dg = danhGiaDAO.saveAndFlush(dg);

        danhGiaService.updateProductRatingStats(testProduct.getId());
        SanPham sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(1, sp.getSoDanhGia());
        assertEquals(5.0, sp.getDiemTrungBinh());

        // 2. Add "apple" as banned keyword
        CommentModerationKeyword keyword = CommentModerationKeyword.builder()
                .keyword("apple")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        keywordRepository.saveAndFlush(keyword);
        commentModerationService.clearKeywordCache();

        // 3. Trigger scheduler scanning
        danhGiaService.scanAndModerateReviews();

        // 4. Verify review is automatically hidden
        DanhGia updated = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertTrue(updated.getAnBinhLuan());
        assertFalse(updated.getAnHinhAnh());
        assertFalse(updated.getDaXoa());

        // 5. Verify rating is updated
        sp = sanPhamRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(0, sp.getSoDanhGia());
        assertEquals(0.0, sp.getDiemTrungBinh());

        // 6. Verify CommentViolationLog is generated with source AUTO_SCAN
        List<CommentViolationLog> logs = commentViolationLogRepository.findAll();
        assertFalse(logs.isEmpty());
        CommentViolationLog latestLog = logs.get(logs.size() - 1);
        assertEquals("AUTO_SCAN", latestLog.getNguon());
        assertNull(latestLog.getThoiHanKhoa());
    }
}
