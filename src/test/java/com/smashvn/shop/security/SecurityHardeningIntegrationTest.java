package com.smashvn.shop.security;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.admin.AdminKhuyenMaiService;
import com.smashvn.shop.service.admin.AdminPosService;
import com.smashvn.shop.service.admin.AdminSanPhamService;
import com.smashvn.shop.service.order.OrderViewService;

@SpringBootTest
@Transactional
public class SecurityHardeningIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private NhanVienRepository nhanVienRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private DanhMucRepository danhMucRepository;
    @Autowired
    private ThuongHieuRepository thuongHieuRepository;
    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository;
    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;
    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    @Autowired
    private AdminKhuyenMaiService adminKhuyenMaiService;
    @Autowired
    private AdminSanPhamService adminSanPhamService;
    @Autowired
    private AdminPosService adminPosService;
    @Autowired
    private OrderViewService orderViewService;
    private MockMvc mockMvc;
    private TaiKhoan managerTk;
    private TaiKhoan staffTk;
    private TaiKhoan customerTk;
    private KhachHang customerKh;
    private NhanVien managerNv;
    private NhanVien staffNv;
    private DanhMuc testDanhMuc;
    private ThuongHieu testThuongHieu;
    private PhuongThucThanhToan testPttt;
    private DonViVanChuyen testDvvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Create unique emails for each test setup
        String managerEmail = "manager-test-" + System.nanoTime() + "@smashvn.com";
        String staffEmail = "staff-test-" + System.nanoTime() + "@smashvn.com";
        String customerEmail = "customer-test-" + System.nanoTime() + "@smashvn.com";

        managerTk = new TaiKhoan();
        managerTk.setUsername(managerEmail);
        managerTk.setMatKhau("password123");
        managerTk.setVaiTro("QL");

        managerTk.setTrangThai("hoat_dong");
        managerTk = taiKhoanRepository.save(managerTk);

        managerNv = new NhanVien();
        managerNv.setTaiKhoan(managerTk);
        managerNv.setHoTenNv("Manager Test");
        managerNv.setChucVu("Manager");
        managerNv.setSoDienThoaiNv("0987654321");
        managerNv = nhanVienRepository.save(managerNv);

        staffTk = new TaiKhoan();
        staffTk.setUsername(staffEmail);
        staffTk.setMatKhau("password123");
        staffTk.setVaiTro("NV");

        staffTk.setTrangThai("hoat_dong");
        staffTk = taiKhoanRepository.save(staffTk);

        staffNv = new NhanVien();
        staffNv.setTaiKhoan(staffTk);
        staffNv.setHoTenNv("Staff Test");
        staffNv.setChucVu("Staff");
        staffNv.setSoDienThoaiNv("0987654322");
        staffNv = nhanVienRepository.save(staffNv);

        customerTk = new TaiKhoan();
        customerTk.setUsername(customerEmail);
        customerTk.setMatKhau("password123");
        customerTk.setVaiTro("KH");

        customerTk.setTrangThai("hoat_dong");
        customerTk = taiKhoanRepository.save(customerTk);

        customerKh = new KhachHang();
        customerKh.setTaiKhoan(customerTk);
        customerKh.setHoKh("Customer");
        customerKh.setTenKh("Test");
        customerKh.setSoDienThoaiKh("0987654323");
        customerKh.setLaTaiKhoanNoiBo(false);
        customerKh = khachHangRepository.save(customerKh);

        testDanhMuc = new DanhMuc();
        testDanhMuc.setTenDanhMuc("Test Category " + System.nanoTime());
        testDanhMuc = danhMucRepository.save(testDanhMuc);

        testThuongHieu = new ThuongHieu();
        testThuongHieu.setTenThuongHieu("Test Brand " + System.nanoTime());
        testThuongHieu = thuongHieuRepository.save(testThuongHieu);

        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        if (ptts.isEmpty()) {
            PhuongThucThanhToan pt = new PhuongThucThanhToan();
            pt.setTenPhuongThuc("Tiền mặt");
            testPttt = phuongThucThanhToanDAO.save(pt);
        } else {
            testPttt = ptts.get(0);
        }

        List<DonViVanChuyen> vcs = donViVanChuyenDAO.findAll();
        if (vcs.isEmpty()) {
            DonViVanChuyen vc = new DonViVanChuyen();
            vc.setTenDonVi("Mua tại quầy");
            vc.setHotline("0000");
            vc.setWebsite("https://smashvn.com");
            testDvvc = donViVanChuyenDAO.save(vc);
        } else {
            testDvvc = vcs.get(0);
        }
    }

    private String repeat(char c, int count) {
        char[] arr = new char[count];
        Arrays.fill(arr, c);
        return new String(arr);
    }

    private HoaDon createTestHoaDon(String status) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(customerKh);
        hd.setTrangThaiDonHang(status);
        hd.setTongTien(BigDecimal.TEN);
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0987654323");
        hd.setMaDonHang("TEST-HARDEN-" + System.nanoTime());
        hd.setDonViVanChuyen(testDvvc);
        hd.setPhuongThucThanhToan(testPttt);
        return hoaDonRepository.save(hd);
    }

    // ================================================================
    // BOUNDARY TESTS
    // ================================================================
    @Test
    void testCampaignNameBoundary_100Chars() {
        String name100 = repeat('a', 100);
        SanPham sp = new SanPham();
        sp.setTenSanPham("Test Product");
        sp.setDanhMuc(testDanhMuc);
        sp.setThuongHieu(testThuongHieu);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(managerNv);
        sp = sanPhamRepository.save(sp);

        List<Integer> productIds = Collections.singletonList(sp.getId());
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        try {
            adminKhuyenMaiService.createDotGiamGia(name100, start, end, 10, "Theo Phần Trăm", productIds, managerTk.getId(), "127.0.0.1");
        } catch (Exception e) {
            assertFalse(e.getMessage().contains("độ dài từ 2 đến 100"), "Should not fail on name length: " + e.getMessage());
        }
    }

    @Test
    void testCampaignNameBoundary_101Chars() {
        String name101 = repeat('a', 101);
        SanPham sp = new SanPham();
        sp.setTenSanPham("Test Product");
        sp.setDanhMuc(testDanhMuc);
        sp.setThuongHieu(testThuongHieu);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(managerNv);
        sp = sanPhamRepository.save(sp);

        List<Integer> productIds = Collections.singletonList(sp.getId());
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        Exception e = assertThrows(RuntimeException.class, () -> {
            adminKhuyenMaiService.createDotGiamGia(name101, start, end, 10, "Theo Phần Trăm", productIds, managerTk.getId(), "127.0.0.1");
        });
        assertTrue(e.getMessage().contains("độ dài từ 2 đến 100"), "Expected length error but got: " + e.getMessage());
    }

    @Test
    void testProductNameBoundary_100Chars() {
        String name100 = repeat('a', 100);
        SanPham sp = new SanPham();
        sp.setTenSanPham("Test Product");
        sp.setDanhMuc(testDanhMuc);
        sp.setThuongHieu(testThuongHieu);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(managerNv);
        sp = sanPhamRepository.save(sp);

        final Integer spId = sp.getId();
        try {
            adminSanPhamService.capNhatSanPham(spId, name100, testDanhMuc.getId(), testThuongHieu.getId(), "Valid Desc", managerTk.getId(), "127.0.0.1");
        } catch (Exception e) {
            assertFalse(e.getMessage().contains("độ dài từ 2 đến 100"), "Should not fail on product name length: " + e.getMessage());
        }
    }

    @Test
    void testProductNameBoundary_101Chars() {
        String name101 = repeat('a', 101);
        SanPham sp = new SanPham();
        sp.setTenSanPham("Test Product");
        sp.setDanhMuc(testDanhMuc);
        sp.setThuongHieu(testThuongHieu);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(managerNv);
        sp = sanPhamRepository.save(sp);

        final Integer spId = sp.getId();
        Exception e = assertThrows(RuntimeException.class, () -> {
            adminSanPhamService.capNhatSanPham(spId, name101, testDanhMuc.getId(), testThuongHieu.getId(), "Valid Desc", managerTk.getId(), "127.0.0.1");
        });
        assertTrue(e.getMessage().contains("độ dài từ 2 đến 100"), "Expected product name length error but got: " + e.getMessage());
    }

    @Test
    void testProductDescriptionBoundary_2000Chars() {
        String desc2000 = repeat('a', 2000);
        SanPham sp = new SanPham();
        sp.setTenSanPham("Test Product");
        sp.setDanhMuc(testDanhMuc);
        sp.setThuongHieu(testThuongHieu);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(managerNv);
        sp = sanPhamRepository.save(sp);

        final Integer spId = sp.getId();
        try {
            adminSanPhamService.capNhatSanPham(spId, "Valid Name", testDanhMuc.getId(), testThuongHieu.getId(), desc2000, managerTk.getId(), "127.0.0.1");
        } catch (Exception e) {
            assertFalse(e.getMessage().contains("Mô tả sản phẩm không được vượt quá 2000"), "Should not fail on product desc length: " + e.getMessage());
        }
    }

    @Test
    void testProductDescriptionBoundary_2001Chars() {
        String desc2001 = repeat('a', 2001);
        SanPham sp = new SanPham();
        sp.setTenSanPham("Test Product");
        sp.setDanhMuc(testDanhMuc);
        sp.setThuongHieu(testThuongHieu);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(managerNv);
        sp = sanPhamRepository.save(sp);

        final Integer spId = sp.getId();
        Exception e = assertThrows(RuntimeException.class, () -> {
            adminSanPhamService.capNhatSanPham(spId, "Valid Name", testDanhMuc.getId(), testThuongHieu.getId(), desc2001, managerTk.getId(), "127.0.0.1");
        });
        assertTrue(e.getMessage().contains("Mô tả sản phẩm không được vượt quá 2000"), "Expected product desc length error but got: " + e.getMessage());
    }

    @Test
    void testPosNoteBoundary_500Chars() {
        String note500 = repeat('a', 500);
        Exception e = null;
        try {
            adminPosService.thanhToanPos(customerKh.getId(), null, Collections.emptyList(), "TIEN_MAT", "TX123", note500, managerTk.getId(), "127.0.0.1");
        } catch (Exception ex) {
            e = ex;
        }
        assertNotNull(e);
        assertFalse(e.getMessage().contains("Ghi chú không được vượt quá 500"), "Should not fail on pos note length: " + e.getMessage());
    }

    @Test
    void testPosNoteBoundary_501Chars() {
        String note501 = repeat('a', 501);
        Exception e = assertThrows(RuntimeException.class, () -> {
            adminPosService.thanhToanPos(customerKh.getId(), null, Collections.emptyList(), "TIEN_MAT", "TX123", note501, managerTk.getId(), "127.0.0.1");
        });
        assertTrue(e.getMessage().contains("Ghi chú không được vượt quá 500"), "Expected pos note length error but got: " + e.getMessage());
    }

    @Test
    void testCancellationReasonBoundary_500Chars() {
        String reason500 = repeat('a', 500);
        HoaDon hd = createTestHoaDon("cho_xac_nhan");

        final Integer hdId = hd.getId();
        try {
            orderViewService.updateOrderStatusByAdmin(hdId, "da_huy", "cho_xac_nhan", managerTk.getId(), "127.0.0.1", reason500);
        } catch (Exception e) {
            assertFalse(e.getMessage().contains("vượt quá 500"), "Should not fail on cancel reason length: " + e.getMessage());
        }
    }

    @Test
    void testCancellationReasonBoundary_501Chars() {
        String reason501 = repeat('a', 501);
        HoaDon hd = createTestHoaDon("cho_xac_nhan");

        final Integer hdId = hd.getId();
        Exception e = assertThrows(RuntimeException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hdId, "da_huy", "cho_xac_nhan", managerTk.getId(), "127.0.0.1", reason501);
        });
        assertTrue(e.getMessage().contains("vượt quá 500"), "Expected cancel reason length error but got: " + e.getMessage());
    }



    // ================================================================
    // AUTHORIZATION TESTS
    // ================================================================
    @Test
    void testCapNhatTrangThaiDonHang_Authorization() throws Exception {
        HoaDon hd = createTestHoaDon("cho_xac_nhan");

        // Manager allowed
        MockHttpSession managerSession = new MockHttpSession();
        managerSession.setAttribute("idNguoiDung", managerTk.getId());
        mockMvc.perform(post("/admin/don-hang/update-status")
                .param("idHoaDon", hd.getId().toString())
                .param("trangThai", "da_xac_nhan")
                .param("expectedStatus", "cho_xac_nhan")
                .session(managerSession))
                .andExpect(redirectedUrl("/admin/don-hang"));

        // Staff allowed
        hd.setTrangThaiDonHang("da_xac_nhan");
        hd = hoaDonRepository.save(hd);
        MockHttpSession staffSession = new MockHttpSession();
        staffSession.setAttribute("idNguoiDung", staffTk.getId());
        mockMvc.perform(post("/admin/don-hang/update-status")
                .param("idHoaDon", hd.getId().toString())
                .param("trangThai", "dang_giao")
                .param("expectedStatus", "da_xac_nhan")
                .session(staffSession))
                .andExpect(redirectedUrl("/admin/don-hang"));

        // Customer denied
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("idNguoiDung", customerTk.getId());
        mockMvc.perform(post("/admin/don-hang/update-status")
                .param("idHoaDon", hd.getId().toString())
                .param("trangThai", "da_giao")
                .param("expectedStatus", "dang_giao")
                .session(customerSession))
                .andExpect(flash().attribute("errorMsg", org.hamcrest.Matchers.containsString("không có quyền thực hiện")));
    }

    @Test
    void testApproveRefund_Authorization() throws Exception {
        HoaDon hd = createTestHoaDon("cho_xac_nhan");
        hd.setGatewayResponse("REFUND_TOKEN:test-token;");
        hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hd = hoaDonRepository.save(hd);

        // Manager allowed
        MockHttpSession managerSession = new MockHttpSession();
        managerSession.setAttribute("idNguoiDung", managerTk.getId());
        mockMvc.perform(post("/admin/don-hang/approve-refund-ui")
                .param("idHoaDon", hd.getId().toString())
                .session(managerSession))
                .andExpect(redirectedUrl("/admin/don-hang"));

        // Staff denied
        hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hd.setGatewayResponse("REFUND_TOKEN:test-token;");
        hd = hoaDonRepository.save(hd);
        MockHttpSession staffSession = new MockHttpSession();
        staffSession.setAttribute("idNguoiDung", staffTk.getId());
        mockMvc.perform(post("/admin/don-hang/approve-refund-ui")
                .param("idHoaDon", hd.getId().toString())
                .session(staffSession))
                .andExpect(flash().attribute("errorMsg", org.hamcrest.Matchers.containsString("Chỉ Quản lý mới có thể phê duyệt")));

        // Customer denied
        hd.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hd.setGatewayResponse("REFUND_TOKEN:test-token;");
        hd = hoaDonRepository.save(hd);
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("idNguoiDung", customerTk.getId());
        mockMvc.perform(post("/admin/don-hang/approve-refund-ui")
                .param("idHoaDon", hd.getId().toString())
                .session(customerSession))
                .andExpect(flash().attribute("errorMsg", org.hamcrest.Matchers.containsString("Chỉ Quản lý mới có thể phê duyệt")));
    }

    // ================================================================
    // VOUCHER TESTS
    // ================================================================
    @Test
    void testVoucherCodeValidationAndDuplicates() {
        PhieuGiamGia pgg = new PhieuGiamGia();
        pgg.setMaPhieu("HAPPY_NEW_YEAR");
        pgg.setGiaTri(BigDecimal.TEN);
        pgg.setDonVi("%");
        pgg.setNgayBatDau(LocalDateTime.now().plusDays(1));
        pgg.setNgayKetThuc(LocalDateTime.now().plusDays(5));
        pgg.setSoLuongConLai(10);
        pgg.setGiaTriDonHangToiThieu(BigDecimal.ZERO);
        pgg.setLoaiGiamGia("Giảm phần trăm");
        pgg.setNhanVien(managerNv);
        pgg.setActive(true);
        pgg = phieuGiamGiaRepository.save(pgg);

        assertTrue(phieuGiamGiaRepository.existsByMaPhieuIgnoreCase("happy_new_year"));
        assertTrue(phieuGiamGiaRepository.existsByMaPhieuIgnoreCase("Happy_New_Year"));

        assertTrue(phieuGiamGiaRepository.existsByMaPhieuIgnoreCaseAndIdNot("HAPPY_NEW_YEAR", -1));
        assertFalse(phieuGiamGiaRepository.existsByMaPhieuIgnoreCaseAndIdNot("HAPPY_NEW_YEAR", pgg.getId()));

        final String duplicateCode = "happy_new_year";
        Exception e1 = assertThrows(RuntimeException.class, () -> {
            adminKhuyenMaiService.createPhieuGiamGia(duplicateCode, BigDecimal.TEN, "%", LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(5), 10, BigDecimal.ZERO, "Giảm phần trăm", new BigDecimal("10000"), managerTk.getId(), "127.0.0.1");
        });
        assertTrue(e1.getMessage().contains("đã tồn tại"), "Should fail on duplicate: " + e1.getMessage());
    }
}
