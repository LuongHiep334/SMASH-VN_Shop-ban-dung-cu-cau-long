package com.smashvn.shop.controller.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.smashvn.shop.dao.DanhGiaDAO;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.CommentModerationKeyword;
import com.smashvn.shop.entity.CommentViolationLog;
import com.smashvn.shop.entity.DanhGia;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThongBao;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.CommentModerationKeywordRepository;
import com.smashvn.shop.repository.CommentViolationLogRepository;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.product.DanhGiaService;

@SpringBootTest
@Transactional
public class DanhGiaIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private DanhGiaDAO danhGiaDAO;

    @Autowired
    private DanhGiaService danhGiaService;

    @Autowired
    private ThongBaoRepository thongBaoRepository;

    @Autowired
    private CommentViolationLogRepository commentViolationLogRepository;

    @Autowired
    private CommentModerationKeywordRepository commentModerationKeywordRepository;

    @Autowired
    private com.smashvn.shop.service.blog.CommentModerationService commentModerationService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    private MockMvc mockMvc;

    private TaiKhoan testUser;
    private KhachHang testKhachHang;
    private TaiKhoan testAdmin;

    private SanPham activeProduct;
    private SanPham inactiveProduct;
    private SanPhamChiTiet activeSpct;
    private SanPhamChiTiet inactiveSpct;

    private DonViVanChuyen testDvvc;
    private PhuongThucThanhToan testPttt;
    private final List<Integer> orderIdsToClean = new java.util.concurrent.CopyOnWriteArrayList<>();

    // A valid 1x1 transparent PNG byte array
    private static final byte[] TINY_PNG = new byte[]{
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
        (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01,
        0x00, 0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
        0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    @BeforeEach
    void setUp() {
        orderIdsToClean.clear();

        // 1. Delete all CommentViolationLog records linked to any TaiKhoan whose email starts with buyer_ or admin_
        try {
            List<CommentViolationLog> logs = commentViolationLogRepository.findAll();
            for (CommentViolationLog log : logs) {
                try {
                    if (log.getTaiKhoan() != null) {
                        String email = log.getTaiKhoan().getUsername();
                        if (email != null && (email.startsWith("buyer_") || email.startsWith("admin_"))) {
                            commentViolationLogRepository.delete(log);
                        }
                    }
                } catch (Exception inner) {
                }
            }
            commentViolationLogRepository.flush();
        } catch (Exception e) {
        }

        // 2. Delete reviews created by buyer_ or admin_
        try {
            List<DanhGia> reviews = danhGiaDAO.findAll();
            for (DanhGia dg : reviews) {
                try {
                    if (dg.getKhachHang() != null && dg.getKhachHang().getTaiKhoan() != null) {
                        String email = dg.getKhachHang().getTaiKhoan().getUsername();
                        if (email != null && (email.startsWith("buyer_") || email.startsWith("admin_"))) {
                            danhGiaDAO.delete(dg);
                        }
                    }
                } catch (Exception inner) {
                }
            }
            danhGiaDAO.flush();
        } catch (Exception e) {
        }

        // 3. Delete customers and users
        try {
            List<TaiKhoan> strayUsers = taiKhoanRepository.findAll().stream()
                    .filter(tk -> tk.getUsername() != null && (tk.getUsername().startsWith("buyer_") || tk.getUsername().startsWith("admin_")))
                    .toList();
            for (TaiKhoan tk : strayUsers) {
                try {
                    KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
                    if (kh != null) {
                        // Delete orders
                        List<HoaDon> orders = hoaDonRepository.findByKhachHang_Id(kh.getId());
                        for (HoaDon hd : orders) {
                            try {
                                hoaDonChiTietRepository.deleteAll(hoaDonChiTietRepository.findByHoaDon_Id(hd.getId()));
                                hoaDonRepository.delete(hd);
                            } catch (Exception inner) {
                            }
                        }
                        khachHangRepository.delete(kh);
                    }
                    taiKhoanRepository.delete(tk);
                } catch (Exception inner) {
                }
            }
            taiKhoanRepository.flush();
        } catch (Exception e) {
        }

        // 4. Delete stray products
        try {
            List<SanPham> strayProducts = sanPhamRepository.findAll().stream()
                    .filter(sp -> sp.getTenSanPham() != null
                    && (sp.getTenSanPham().equals("Yonex Astrox 88D Pro TEST") || sp.getTenSanPham().equals("Yonex Nanoflare 800 (Ngừng bán) TEST")))
                    .toList();
            for (SanPham sp : strayProducts) {
                try {
                    // Delete violation logs referencing this product
                    List<CommentViolationLog> strayLogs = commentViolationLogRepository.findAll().stream()
                            .filter(l -> l.getSanPham() != null && l.getSanPham().getId().equals(sp.getId()))
                            .toList();
                    commentViolationLogRepository.deleteAll(strayLogs);
                    commentViolationLogRepository.flush();

                    // Delete reviews referencing this product
                    List<DanhGia> strayReviews = danhGiaDAO.findAll().stream()
                            .filter(dg -> dg.getSanPham() != null && dg.getSanPham().getId().equals(sp.getId()))
                            .toList();
                    danhGiaDAO.deleteAll(strayReviews);
                    danhGiaDAO.flush();

                    List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findBySanPham_Id(sp.getId());
                    for (SanPhamChiTiet spct : variants) {
                        try {
                            // Delete order details referencing this variant
                            List<HoaDonChiTiet> orderDetails = hoaDonChiTietRepository.findAll().stream()
                                    .filter(od -> od.getSanPhamChiTiet() != null && od.getSanPhamChiTiet().getId().equals(spct.getId()))
                                    .toList();
                            hoaDonChiTietRepository.deleteAll(orderDetails);
                            sanPhamChiTietRepository.delete(spct);
                        } catch (Exception inner) {
                        }
                    }
                    sanPhamChiTietRepository.flush();
                    sanPhamRepository.delete(sp);
                } catch (Exception inner) {
                }
            }
            sanPhamRepository.flush();
        } catch (Exception e) {
        }

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(new org.springframework.web.filter.CharacterEncodingFilter("UTF-8", true))
                .build();

        // Seed customer user
        testUser = new TaiKhoan();
        testUser.setUsername("buyer_" + java.util.UUID.randomUUID().toString().substring(0, 5) + "@gmail.com");
        testUser.setMatKhau("testpass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");

        testUser = taiKhoanRepository.save(testUser);

        testKhachHang = new KhachHang();
        testKhachHang.setTaiKhoan(testUser);
        testKhachHang.setHoKh("Nguyen");
        testKhachHang.setTenKh("Van A");
        testKhachHang.setSoDienThoaiKh("0987654321");
        testKhachHang = khachHangRepository.save(testKhachHang);

        // Seed admin user
        testAdmin = new TaiKhoan();
        testAdmin.setUsername("admin_" + java.util.UUID.randomUUID().toString().substring(0, 5) + "@gmail.com");
        testAdmin.setMatKhau("adminpass");
        testAdmin.setVaiTro("QL");
        testAdmin.setTrangThai("hoat_dong");

        testAdmin = taiKhoanRepository.save(testAdmin);

        // Seed Catalog & Brand
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("Vợt Cầu Lông");
            return danhMucRepository.save(newDm);
        });

        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu newTh = new ThuongHieu();
            newTh.setTenThuongHieu("Yonex");
            return thuongHieuRepository.save(newTh);
        });

        // Seed Staff
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan nvUser = new TaiKhoan();
            nvUser.setUsername("staff_" + java.util.UUID.randomUUID().toString().substring(0, 5) + "@gmail.com");
            nvUser.setMatKhau("pass123");
            nvUser.setVaiTro("NV");
            nvUser.setTrangThai("hoat_dong");

            nvUser = taiKhoanRepository.save(nvUser);

            NhanVien newNv = new NhanVien();
            newNv.setTaiKhoan(nvUser);
            newNv.setHoTenNv("Staff");
            newNv.setChucVu("Nhân viên");
            newNv.setSoDienThoaiNv("0981112223");
            return nhanVienRepository.save(newNv);
        });

        // Seed Active Product
        activeProduct = new SanPham();
        activeProduct.setTenSanPham("Yonex Astrox 88D Pro TEST");
        activeProduct.setTrangThai("dang_ban");
        activeProduct.setDanhMuc(dm);
        activeProduct.setThuongHieu(th);
        activeProduct.setNhanVien(nv);
        activeProduct.setSoDanhGia(0);
        activeProduct.setDiemTrungBinh(0.0);
        activeProduct = sanPhamRepository.save(activeProduct);

        activeSpct = new SanPhamChiTiet();
        activeSpct.setSanPham(activeProduct);
        activeSpct.setMauSac("Đỏ");
        activeSpct.setTrongLuong("4U");
        activeSpct.setMucCang("28 lbs");
        activeSpct.setSoLuongTon(50);
        activeSpct.setGiaBan(new BigDecimal("3500000"));
        activeSpct = sanPhamChiTietRepository.save(activeSpct);

        // Seed Inactive Product
        inactiveProduct = new SanPham();
        inactiveProduct.setTenSanPham("Yonex Nanoflare 800 (Ngừng bán) TEST");
        inactiveProduct.setTrangThai("ngung_kinh_doanh");
        inactiveProduct.setDanhMuc(dm);
        inactiveProduct.setThuongHieu(th);
        inactiveProduct.setNhanVien(nv);
        inactiveProduct.setSoDanhGia(0);
        inactiveProduct.setDiemTrungBinh(0.0);
        inactiveProduct = sanPhamRepository.save(inactiveProduct);

        inactiveSpct = new SanPhamChiTiet();
        inactiveSpct.setSanPham(inactiveProduct);
        inactiveSpct.setMauSac("Xanh");
        inactiveSpct.setTrongLuong("3U");
        inactiveSpct.setMucCang("26 lbs");
        inactiveSpct.setSoLuongTon(10);
        inactiveSpct.setGiaBan(new BigDecimal("4000000"));
        inactiveSpct = sanPhamChiTietRepository.save(inactiveSpct);

        // Seed Carrier & Payment Method
        testDvvc = donViVanChuyenDAO.findAll().stream().findFirst().orElseGet(() -> {
            DonViVanChuyen dv = new DonViVanChuyen();
            dv.setTenDonVi("Giao Hàng Nhanh");
            return donViVanChuyenDAO.save(dv);
        });

        testPttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan pt = new PhuongThucThanhToan();
            pt.setTenPhuongThuc("COD");
            return phuongThucThanhToanDAO.save(pt);
        });
    }

    private HoaDon createOrder(String orderStatus) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setTongTien(new BigDecimal("3500000"));
        hd.setPaymentStatus("PAID");
        hd.setTrangThaiDonHang(orderStatus);
        hd.setDiaChiNhan("123 Ha Noi");
        hd.setSdtNhan("0987654321");
        hd.setMaDonHang("HD_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        hd.setDonViVanChuyen(testDvvc);
        hd.setPhuongThucThanhToan(testPttt);
        hd = hoaDonRepository.save(hd);
        orderIdsToClean.add(hd.getId());

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(activeSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("3500000"));
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }

    @Test
    void testReviewInactiveProduct_ShouldFail() throws Exception {
        mockMvc.perform(multipart("/san-pham/" + inactiveProduct.getId() + "/danh-gia")
                .param("rating", "5")
                .param("comment", "Sản phẩm tệ quá!")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", "Sản phẩm này hiện không còn hỗ trợ đánh giá."));
    }

    @Test
    void testReviewWithoutPurchase_ShouldFail() throws Exception {
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "5")
                .param("comment", "Vợt ngon nha!")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công."));
    }

    @Test
    void testReviewWithUncompletedOrderStatus_ShouldFail() throws Exception {
        // Order with pending status (cho_xac_nhan)
        createOrder("cho_xac_nhan");

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "5")
                .param("comment", "Vợt ngon nha!")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công."));
    }

    @Test
    void testReviewSuccessAndUniquenessPolicy() throws Exception {
        // Create completed order (da_giao)
        createOrder("da_giao");

        // First review submission
        MockMultipartFile image1 = new MockMultipartFile("fileAnh", "img1.png", "image/png", TINY_PNG);

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .file(image1)
                .param("rating", "5")
                .param("comment", "Vợt đánh rất đầm tay!")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Gửi đánh giá sản phẩm thành công!"));

        // Verify Database
        List<DanhGia> activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        assertEquals(1, activeReviews.size());
        DanhGia dg = activeReviews.get(0);
        assertEquals(5, dg.getSoSao());
        assertEquals("Vợt đánh rất đầm tay!", dg.getBinhLuan());
        assertEquals(1, dg.getDanhSachAnh().size());

        // Verify Cache Stats on SanPham
        SanPham updatedSp = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(1, updatedSp.getSoDanhGia());
        assertEquals(5.0, updatedSp.getDiemTrungBinh());

        // --- SECOND SUBMISSION (Edit / Overwrite existing review) ---
        // We simulate a delay by changing the system rate limit parameters or wait, the rate limit would reject it!
        // To bypass the 30-second rate limiter during this test, we can manually change the last review's ngayDanhGia to 1 minute ago.
        dg.setNgayDanhGia(LocalDateTime.now().minusMinutes(1));
        dg.setNgayCapNhat(LocalDateTime.now().minusMinutes(1));
        danhGiaDAO.save(dg);

        MockMultipartFile image2 = new MockMultipartFile("fileAnh", "img2.png", "image/png", TINY_PNG);

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .file(image2)
                .param("rating", "4")
                .param("comment", "Cập nhật: Dùng lâu thấy hơi mỏi vai.")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Gửi đánh giá sản phẩm thành công!"));

        // Verify database still has only 1 review
        activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        assertEquals(1, activeReviews.size());
        DanhGia updatedDg = activeReviews.get(0);
        assertEquals(4, updatedDg.getSoSao());
        assertEquals("Cập nhật: Dùng lâu thấy hơi mỏi vai.", updatedDg.getBinhLuan());
        // Verify new image replaced the old one
        assertEquals(1, updatedDg.getDanhSachAnh().size());
        assertNotNull(updatedDg.getNgayCapNhat());

        // Verify Cache Stats on SanPham is updated
        updatedSp = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(1, updatedSp.getSoDanhGia());
        assertEquals(4.0, updatedSp.getDiemTrungBinh());
    }

    @Test
    void testRateLimiting_ShouldBlock() throws Exception {
        createOrder("hoan_thanh");

        // Submit first review
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "5")
                .param("comment", "Review 1")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Gửi đánh giá sản phẩm thành công!"));

        // Submit second review immediately (should fail because seconds < 30)
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "4")
                .param("comment", "Review 2")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMsg", "Bạn gửi yêu cầu quá nhanh! Vui lòng đợi ít nhất 30 giây giữa các lần đánh giá."));
    }

    @Test
    void testSoftDeleteReviewAndStatsCacheUpdate() throws Exception {
        createOrder("da_giao");

        // Submit a review
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "5")
                .param("comment", "Đánh giá tốt")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        List<DanhGia> activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        assertEquals(1, activeReviews.size());
        DanhGia dg = activeReviews.get(0);

        // Perform Soft Delete by Admin
        mockMvc.perform(post("/admin/danh-gia/xoa/" + dg.getId())
                .sessionAttr("idNguoiDung", testAdmin.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Đã xóa mềm đánh giá thành công."));

        // Verify is soft deleted (daXoa = true)
        DanhGia softDeletedDg = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertTrue(softDeletedDg.getDaXoa());
        assertNotNull(softDeletedDg.getNgayXoa());
        assertEquals(testAdmin.getId(), softDeletedDg.getNguoiXoa().getId());

        // Verify active reviews list is empty
        activeReviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        assertEquals(0, activeReviews.size());

        // Verify cache rating on SanPham is reset to 0
        SanPham updatedSp = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(0, updatedSp.getSoDanhGia());
        assertEquals(0.0, updatedSp.getDiemTrungBinh());
    }

    @Test
    void testSoftModeration_IndependentCommentAndImageHide() throws Exception {
        createOrder("da_giao");

        // Submit review with images
        MockMultipartFile image = new MockMultipartFile("fileAnh", "img.jpg", "image/jpeg", TINY_PNG);
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .file(image)
                .param("rating", "5")
                .param("comment", "Bình luận bậy bạ...")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        DanhGia dg = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);

        // 1. Hide Comment Only
        mockMvc.perform(post("/admin/danh-gia/an-binh-luan/" + dg.getId())
                .sessionAttr("idNguoiDung", testAdmin.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Đã ẩn nội dung bình luận thành công."));

        DanhGia updatedDg = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertTrue(updatedDg.getBinhLuanAn());
        assertFalse(updatedDg.getHinhAnhAn()); // Independent image is still visible
        assertEquals(testAdmin.getId(), updatedDg.getNhanVien().getId());
        assertNotNull(updatedDg.getNgayAnBinhLuan());

        // Rating Stats cache should be updated by moderation action (excluding the hidden comment)
        SanPham updatedSp = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(0, updatedSp.getSoDanhGia());
        assertEquals(0.0, updatedSp.getDiemTrungBinh());

        // 2. Hide Image Only
        mockMvc.perform(post("/admin/danh-gia/an-hinh-anh/" + dg.getId())
                .sessionAttr("idNguoiDung", testAdmin.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", "Đã ẩn hình ảnh đánh giá thành công."));

        updatedDg = danhGiaDAO.findById(dg.getId()).orElseThrow();
        assertTrue(updatedDg.getBinhLuanAn());
        assertTrue(updatedDg.getHinhAnhAn());
        assertEquals(testAdmin.getId(), updatedDg.getNhanVien().getId());
        assertNotNull(updatedDg.getNgayAnHinhAnh());
    }

    @Test
    void testCommentViolation_SeverityAndBanningWorkflow() throws Exception {
        createOrder("da_giao");

        // 1. LOW severity violation: "vcl"
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "4")
                .param("comment", "S\u1ea3n ph\u1ea9m d\u00f9ng vcl nh\u00e9")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        // Check TaiKhoan violation count and ban time
        TaiKhoan updatedUser = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, updatedUser.getSoLanNhacNhoViPham());
        assertNotNull(updatedUser.getNgayKhoaBinhLuanDen());
        // 1st violation should ban for 3 hours
        long banHours = java.time.Duration.between(LocalDateTime.now(), updatedUser.getNgayKhoaBinhLuanDen()).toHours();
        assertTrue(banHours >= 2 && banHours <= 3);

        // Check ThongBao (Notification) created for customer
        List<ThongBao> notifications = thongBaoRepository.findByTaiKhoan_IdOrderByNgayTaoDesc(testUser.getId());
        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).getNoiDung().contains("LOW"));

        // Check CommentViolationLog created
        List<CommentViolationLog> logs = commentViolationLogRepository.findAllByOrderByNgayViPhamDesc();
        assertFalse(logs.isEmpty());
        assertEquals("LOW", logs.get(0).getMucDoViPham());
        assertEquals("S\u1ea3n ph\u1ea9m d\u00f9ng *** nh\u00e9", logs.get(0).getNoiDungGoc());
        assertEquals("S\u1ea3n ph\u1ea9m d\u00f9ng *** nh\u00e9", logs.get(0).getNoiDungDaLoc());

        // Bản ghi vẫn còn cho admin nhưng bị loại hoàn toàn khỏi dữ liệu public
        assertEquals(1, danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).size());
        assertTrue(danhGiaService.layDanhSachDanhGiaTheoSanPham(activeProduct.getId()).isEmpty());
        SanPham productWithPublicStats = sanPhamRepository.findById(activeProduct.getId()).orElseThrow();
        assertEquals(0, productWithPublicStats.getSoDanhGia());
        assertEquals(0.0, productWithPublicStats.getDiemTrungBinh());

        // Log cũ có thể chưa lưu id_danh_gia; vẫn xác định được bằng tài khoản + sản phẩm.
        CommentViolationLog legacyLog = logs.get(0);
        legacyLog.setDanhGia(null);
        commentViolationLogRepository.saveAndFlush(legacyLog);
        assertTrue(danhGiaService.layDanhSachDanhGiaTheoSanPham(activeProduct.getId()).isEmpty());

        // Check mailSender was NOT called for LOW severity
        verify(mailSender, times(0)).send(any(org.springframework.mail.SimpleMailMessage.class));

        // Backdate the first review to bypass the 30-second spam check
        DanhGia dg1 = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);
        dg1.setNgayDanhGia(LocalDateTime.now().minusSeconds(35));
        dg1.setNgayCapNhat(LocalDateTime.now().minusSeconds(35));
        danhGiaDAO.saveAndFlush(dg1);

        // 2. High severity violation: "đm" (should increment violation count and send email)
        updatedUser.setNgayKhoaBinhLuanDen(null);
        taiKhoanRepository.save(updatedUser);

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "3")
                .param("comment", "V\u1ee3t \u0111m qu\u00e1 t\u1ec7")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        updatedUser = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(2, updatedUser.getSoLanNhacNhoViPham()); // Increment to 2

        // Verify mailSender WAS called for HIGH severity
        verify(mailSender, atLeastOnce()).send(any(org.springframework.mail.SimpleMailMessage.class));

        // Backdate the second review to bypass the 30-second spam check
        DanhGia dg2 = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);
        dg2.setNgayDanhGia(LocalDateTime.now().minusSeconds(35));
        dg2.setNgayCapNhat(LocalDateTime.now().minusSeconds(35));
        danhGiaDAO.saveAndFlush(dg2);

        // 3. 180-day Reset Mechanism:
        updatedUser.setNgayViPhamGanNhat(LocalDateTime.now().minusDays(181));
        updatedUser.setNgayKhoaBinhLuanDen(null); // Clear active ban
        taiKhoanRepository.save(updatedUser);

        // Submit another violation (should reset count to 0 and act as 1st violation)
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "2")
                .param("comment", "\u0111m ch\u00e1n th\u1ebf")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        updatedUser = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, updatedUser.getSoLanNhacNhoViPham()); // Reset to 0 then incremented to 1
    }

    @Test
    void testCommentViolation_CriticalSeverityAutoHide() throws Exception {
        createOrder("da_giao");

        // Submit CRITICAL severity violation: "lừa đảo"
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "1")
                .param("comment", "B\u1ecdn l\u1eeba \u0111\u1ea3o \u0103n c\u01b0\u1edbp!")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        // Review should be automatically hidden (anBinhLuan = true)
        List<DanhGia> reviews = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId());
        DanhGia savedDg = reviews.get(0);
        assertTrue(savedDg.getAnBinhLuan());
        assertEquals("B\u1ecdn *** ***!", savedDg.getBinhLuan());
    }

    @Test
    void testCommentViolation_AdminModerationActions() throws Exception {
        createOrder("da_giao");

        // Put user in ban status with 3 violations
        TaiKhoan user = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        user.setSoLanNhacNhoViPham(3);
        user.setNgayKhoaBinhLuanDen(LocalDateTime.now().plusDays(7));
        taiKhoanRepository.save(user);

        // 1. Admin removes ban early
        mockMvc.perform(post("/admin/danh-gia/vi-pham/go-khoa/" + testUser.getId())
                .sessionAttr("idNguoiDung", testAdmin.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", org.hamcrest.Matchers.containsString("gỡ khóa bình luận thành công")));

        TaiKhoan updatedUser = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertNull(updatedUser.getNgayKhoaBinhLuanDen());

        // 2. Admin resets violation count
        mockMvc.perform(post("/admin/danh-gia/vi-pham/reset-vi-pham/" + testUser.getId())
                .sessionAttr("idNguoiDung", testAdmin.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMsg", org.hamcrest.Matchers.containsString("reset bộ đếm vi phạm thành công")));

        updatedUser = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(0, updatedUser.getSoLanNhacNhoViPham());
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void testCommentViolation_ConcurrentSubmissions() throws Exception {
        createOrder("da_giao");

        int numThreads = 3;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numThreads);
        java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            futures.add(java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    danhGiaService.themHoacCapNhatDanhGia(testUser.getId(), activeProduct.getId(), 5.0, "vcl " + index, null);
                } catch (Exception e) {
                    // Lock protection works, concurrent execution is serialized
                }
            }, executor));
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
        executor.shutdown();

        TaiKhoan updatedUser = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(updatedUser.getSoLanNhacNhoViPham() >= 1);
        assertNotNull(updatedUser.getNgayKhoaBinhLuanDen());
    }

    @Test
    public void testCommentViolation_CustomModerationKeywords() throws Exception {
        createOrder("da_giao");

        // Clean up any conflicting pre-existing database keywords (to avoid unique constraint violations)
        java.util.List<String> keywordsToClean = java.util.Arrays.asList("spamkeyword", "tệ hại", "c++", "dm", "x");
        for (com.smashvn.shop.entity.CommentModerationKeyword kw : commentModerationKeywordRepository.findAll()) {
            if (keywordsToClean.contains(kw.getKeyword())) {
                commentModerationKeywordRepository.delete(kw);
            }
        }
        commentModerationKeywordRepository.flush();

        // 1. Inactive keyword check
        CommentModerationKeyword kw1 = CommentModerationKeyword.builder()
                .keyword("spamkeyword")
                .active(false)
                .build();
        commentModerationKeywordRepository.save(kw1);
        commentModerationService.clearKeywordCache();

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "5")
                .param("comment", "Bình luận chứa spamkeyword bình thường")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        // Verify violation count did not increase (still 0)
        TaiKhoan user = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(0, user.getSoLanNhacNhoViPham());

        // Backdate the review to bypass 30s check
        DanhGia dg = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);
        dg.setNgayDanhGia(LocalDateTime.now().minusSeconds(35));
        dg.setNgayCapNhat(LocalDateTime.now().minusSeconds(35));
        danhGiaDAO.saveAndFlush(dg);

        // 2. Active custom keyword check (triggers MEDIUM severity)
        CommentModerationKeyword kw2 = CommentModerationKeyword.builder()
                .keyword("tệ hại")
                .active(true)
                .build();
        commentModerationKeywordRepository.save(kw2);
        commentModerationService.clearKeywordCache();

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "3")
                .param("comment", "Sản phẩm tệ hại lắm nha")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        // Verify violation count increased to 1, and log created with MEDIUM severity
        user = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, user.getSoLanNhacNhoViPham());

        List<CommentViolationLog> logs = commentViolationLogRepository.findAllByOrderByNgayViPhamDesc();
        assertFalse(logs.isEmpty());
        assertEquals("MEDIUM", logs.get(0).getMucDoViPham());
        assertEquals("Sản phẩm *** lắm nha", logs.get(0).getNoiDungGoc());
        assertEquals("Sản phẩm *** lắm nha", logs.get(0).getNoiDungDaLoc());

        // Reset ban/violation count and backdate for next test
        user.setNgayKhoaBinhLuanDen(null);
        taiKhoanRepository.saveAndFlush(user);
        DanhGia dg2 = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);
        dg2.setNgayDanhGia(LocalDateTime.now().minusSeconds(35));
        dg2.setNgayCapNhat(LocalDateTime.now().minusSeconds(35));
        danhGiaDAO.saveAndFlush(dg2);

        // 3. Regex special characters check (c++, a+b, test.com)
        CommentModerationKeyword kw3 = CommentModerationKeyword.builder()
                .keyword("c++")
                .active(true)
                .build();
        commentModerationKeywordRepository.save(kw3);
        commentModerationService.clearKeywordCache();

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "4")
                .param("comment", "Học lập trình c++ rất tốt")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        user = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(2, user.getSoLanNhacNhoViPham()); // Incremented to 2

        logs = commentViolationLogRepository.findAllByOrderByNgayViPhamDesc();
        assertEquals("MEDIUM", logs.get(0).getMucDoViPham());
        assertEquals("Học lập trình *** rất tốt", logs.get(0).getNoiDungGoc());
        assertEquals("Học lập trình *** rất tốt", logs.get(0).getNoiDungDaLoc());

        user.setNgayKhoaBinhLuanDen(null);
        taiKhoanRepository.saveAndFlush(user);
        DanhGia dg3 = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);
        dg3.setNgayDanhGia(LocalDateTime.now().minusSeconds(35));
        dg3.setNgayCapNhat(LocalDateTime.now().minusSeconds(35));
        danhGiaDAO.saveAndFlush(dg3);

        // 4. Obfuscation boundary check (đ.m, Đ.M, đ m)
        CommentModerationKeyword kw4 = CommentModerationKeyword.builder()
                .keyword("dm")
                .active(true)
                .build();
        commentModerationKeywordRepository.save(kw4);
        commentModerationService.clearKeywordCache();

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "2")
                .param("comment", "Cái đồ đ.m này")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        user = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(3, user.getSoLanNhacNhoViPham()); // Incremented to 3

        logs = commentViolationLogRepository.findAllByOrderByNgayViPhamDesc();
        assertEquals("HIGH", logs.get(0).getMucDoViPham()); // HIGH because 'đ.m' is a hardcoded HIGH pattern
        assertEquals("Cái đồ *** này", logs.get(0).getNoiDungGoc());

        user.setNgayKhoaBinhLuanDen(null);
        taiKhoanRepository.saveAndFlush(user);
        DanhGia dg4 = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);
        dg4.setNgayDanhGia(LocalDateTime.now().minusSeconds(35));
        dg4.setNgayCapNhat(LocalDateTime.now().minusSeconds(35));
        danhGiaDAO.saveAndFlush(dg4);

        // 4b. Non-hardcoded custom keyword obfuscation check
        // "tệ hại" matches "tệ.hại"
        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "2")
                .param("comment", "Thật là tệ.hại")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        user = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(4, user.getSoLanNhacNhoViPham()); // Incremented to 4

        logs = commentViolationLogRepository.findAllByOrderByNgayViPhamDesc();
        assertEquals("MEDIUM", logs.get(0).getMucDoViPham()); // MEDIUM because it is custom keyword only
        assertEquals("Thật là ***", logs.get(0).getNoiDungGoc());
        assertEquals("Thật là ***", logs.get(0).getNoiDungDaLoc()); // Obfuscated keyword replaced

        user.setNgayKhoaBinhLuanDen(null);
        taiKhoanRepository.saveAndFlush(user);
        DanhGia dg4b = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);
        dg4b.setNgayDanhGia(LocalDateTime.now().minusSeconds(35));
        dg4b.setNgayCapNhat(LocalDateTime.now().minusSeconds(35));
        danhGiaDAO.saveAndFlush(dg4b);

        // 5. Short keyword check (should ignore length < 2)
        CommentModerationKeyword kw5 = CommentModerationKeyword.builder()
                .keyword("x")
                .active(true)
                .build();
        commentModerationKeywordRepository.save(kw5);
        commentModerationService.clearKeywordCache();

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "5")
                .param("comment", "Bình luận chứa chữ x bình thường")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        user = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(4, user.getSoLanNhacNhoViPham()); // Still 4, ignored keyword 'x'

        // 6. Conflict check: match both hardcoded HIGH and custom MEDIUM -> HIGH wins
        user.setSoLanNhacNhoViPham(0);
        taiKhoanRepository.saveAndFlush(user);
        DanhGia dg5 = danhGiaDAO.findBySanPham_IdAndDaXoaFalseOrderByNgayDanhGiaDesc(activeProduct.getId()).get(0);
        dg5.setNgayDanhGia(LocalDateTime.now().minusSeconds(35));
        dg5.setNgayCapNhat(LocalDateTime.now().minusSeconds(35));
        danhGiaDAO.saveAndFlush(dg5);

        mockMvc.perform(multipart("/san-pham/" + activeProduct.getId() + "/danh-gia")
                .param("rating", "1")
                .param("comment", "đm tệ hại")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection());

        user = taiKhoanRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(1, user.getSoLanNhacNhoViPham());

        logs = commentViolationLogRepository.findAllByOrderByNgayViPhamDesc();
        assertEquals("HIGH", logs.get(0).getMucDoViPham()); // HIGH severity wins over MEDIUM

        // Clean up keywords we created to avoid affecting other tests
        commentModerationKeywordRepository.delete(kw1);
        commentModerationKeywordRepository.delete(kw2);
        commentModerationKeywordRepository.delete(kw3);
        commentModerationKeywordRepository.delete(kw4);
        commentModerationKeywordRepository.delete(kw5);
        commentModerationService.clearKeywordCache();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.out.println("DEBUG: activeProduct=" + (activeProduct != null ? activeProduct.getId() : "null") + ", inactiveProduct=" + (inactiveProduct != null ? inactiveProduct.getId() : "null"));

        // 1. Delete comment violation logs
        try {
            List<CommentViolationLog> logs = commentViolationLogRepository.findAll();
            for (CommentViolationLog log : logs) {
                Integer logSpId = log.getSanPham() != null ? log.getSanPham().getId() : null;
                System.out.println("DEBUG: Found CommentViolationLog id=" + log.getId() + " with sanPhamId=" + logSpId);
                if (log.getSanPham() != null
                        && ((activeProduct != null && log.getSanPham().getId().equals(activeProduct.getId()))
                        || (inactiveProduct != null && log.getSanPham().getId().equals(inactiveProduct.getId())))) {
                    System.out.println("DEBUG: Deleting CommentViolationLog id=" + log.getId());
                    commentViolationLogRepository.delete(log);
                }
            }
            commentViolationLogRepository.flush();
        } catch (Exception e) {
            System.err.println("Error deleting comment violation logs: " + e.getMessage());
            e.printStackTrace();
        }

        // 2. Delete reviews
        try {
            List<DanhGia> reviews = danhGiaDAO.findAll();
            for (DanhGia dg : reviews) {
                Integer dgSpId = dg.getSanPham() != null ? dg.getSanPham().getId() : null;
                System.out.println("DEBUG: Found DanhGia id=" + dg.getId() + " with sanPhamId=" + dgSpId);
                if (dg.getSanPham() != null
                        && ((activeProduct != null && dg.getSanPham().getId().equals(activeProduct.getId()))
                        || (inactiveProduct != null && dg.getSanPham().getId().equals(inactiveProduct.getId())))) {
                    System.out.println("DEBUG: Deleting DanhGia id=" + dg.getId());
                    danhGiaDAO.delete(dg);
                }
            }
            danhGiaDAO.flush();
        } catch (Exception e) {
            System.err.println("Error deleting reviews: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. Delete tracked orders
        for (Integer orderId : orderIdsToClean) {
            try {
                hoaDonChiTietRepository.deleteAll(hoaDonChiTietRepository.findByHoaDon_Id(orderId));
                hoaDonChiTietRepository.flush();
            } catch (Exception e) {
            }
            try {
                hoaDonRepository.deleteById(orderId);
                hoaDonRepository.flush();
            } catch (Exception e) {
            }
        }

        // 4. Delete variants (SanPhamChiTiet)
        try {
            if (activeSpct != null) {
                sanPhamChiTietRepository.delete(activeSpct);
                sanPhamChiTietRepository.flush();
            }
        } catch (Exception e) {
        }
        try {
            if (inactiveSpct != null) {
                sanPhamChiTietRepository.delete(inactiveSpct);
                sanPhamChiTietRepository.flush();
            }
        } catch (Exception e) {
        }

        // 5. Delete products (SanPham)
        try {
            if (activeProduct != null) {
                sanPhamRepository.delete(activeProduct);
                sanPhamRepository.flush();
            }
        } catch (Exception e) {
        }
        try {
            if (inactiveProduct != null) {
                sanPhamRepository.delete(inactiveProduct);
                sanPhamRepository.flush();
            }
        } catch (Exception e) {
        }

        // 6. Delete customers and users
        try {
            if (testKhachHang != null) {
                khachHangRepository.delete(testKhachHang);
                khachHangRepository.flush();
            }
        } catch (Exception e) {
        }
        try {
            if (testUser != null) {
                taiKhoanRepository.delete(testUser);
                taiKhoanRepository.flush();
            }
        } catch (Exception e) {
        }
        try {
            if (testAdmin != null) {
                taiKhoanRepository.delete(testAdmin);
                taiKhoanRepository.flush();
            }
        } catch (Exception e) {
        }

        // 7. Delete stray staff
        try {
            List<TaiKhoan> strayStaff = taiKhoanRepository.findAll().stream()
                    .filter(tk -> tk.getUsername() != null && tk.getUsername().startsWith("staff_"))
                    .toList();
            for (TaiKhoan tk : strayStaff) {
                NhanVien nv = nhanVienRepository.findByTaiKhoanId(tk.getId());
                if (nv != null) {
                    nhanVienRepository.delete(nv);
                    nhanVienRepository.flush();
                }
                taiKhoanRepository.delete(tk);
                taiKhoanRepository.flush();
            }
        } catch (Exception e) {
        }
    }
}
