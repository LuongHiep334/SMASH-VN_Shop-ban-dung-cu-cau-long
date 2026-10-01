package com.smashvn.shop.controller.admin;

import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class AdminControllerRenderTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Test
    public void testDashboardRenderAndMetricsContract() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        mockMvc.perform(get("/admin/all")
                        .requestAttr("_csrf", csrfToken)
                        .sessionAttr("activeRole", "QL")
                        .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view()
                        .name("admin/admin-dashboard"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                        .attributeExists(
                                "soLuongNhanVien",
                                "soLuongTaiKhoanNhanVienOnly",
                                "soLuongTaiKhoanQuanLy",
                                "soLuongTaiKhoanNhanVien",
                                "soLuongTaiKhoanKhachHang",
                                "soLuongSanPham",
                                "soLuongSanPhamConHang",
                                "danhSachTaiKhoanNhanVien",
                                "danhSachTaiKhoanKhachHang",
                                "danhSachSanPham",
                                "danhSachChoKhoa"));
    }

    @Test
    public void testDonHangRender() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");
        
        mockMvc.perform(get("/admin/don-hang")
                .requestAttr("_csrf", csrfToken)
                .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    public void testSanPhamAddRender() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        mockMvc.perform(get("/admin/san-pham/them")
                .requestAttr("_csrf", csrfToken)
                .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    public void testGetOrderDetailJson_Unauthorized() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockMvc.perform(get("/admin/don-hang/detail-json").param("id", "999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetOrderDetailJson_ForbiddenForCustomer() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TaiKhoan customer = new TaiKhoan();
        customer.setUsername("customer_test@smashvn.com");
        customer.setMatKhau("pass123");
        customer.setVaiTro("KH");

        customer = taiKhoanRepository.save(customer);

        mockMvc.perform(get("/admin/don-hang/detail-json")
                .param("id", "999")
                .sessionAttr("idNguoiDung", customer.getId())
                .sessionAttr("vaiTro", "KH"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetOrderDetailJson_NotFound() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        TaiKhoan staff = new TaiKhoan();
        staff.setUsername("staff_test@smashvn.com");
        staff.setMatKhau("pass123");
        staff.setVaiTro("NV");

        staff = taiKhoanRepository.save(staff);

        mockMvc.perform(get("/admin/don-hang/detail-json")
                .param("id", "999999")
                .sessionAttr("idNguoiDung", staff.getId())
                .sessionAttr("vaiTro", "NV"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetOrderDetailJson_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Seed staff
        TaiKhoan staff = new TaiKhoan();
        staff.setUsername("staff_test_ok@smashvn.com");
        staff.setMatKhau("pass123");
        staff.setVaiTro("NV");

        staff = taiKhoanRepository.save(staff);

        // Seed associations
        DonViVanChuyen dvvc = donViVanChuyenDAO.findAll().stream().findFirst().orElseGet(() -> {
            DonViVanChuyen d = new DonViVanChuyen();
            d.setTenDonVi("Mua tại quầy");
            d.setHotline("000000");
            return donViVanChuyenDAO.save(d);
        });

        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan p = new PhuongThucThanhToan();
            p.setTenPhuongThuc("Tiền mặt");
            return phuongThucThanhToanDAO.save(p);
        });

        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan guestTk = taiKhoanRepository.findByUsername("guest@smashvn.com");
            if (guestTk == null) {
                TaiKhoan tk = new TaiKhoan();
                tk.setUsername("guest@smashvn.com");
                tk.setMatKhau("pass123");
                tk.setVaiTro("KH");
                tk.setTrangThai("hoat_dong");
                guestTk = taiKhoanRepository.save(tk);
            }
            KhachHang customer = new KhachHang();
            customer.setTaiKhoan(guestTk);
            customer.setHoKh("Khách");
            customer.setTenKh("Lẻ");
            customer.setSoDienThoaiKh("0000000000");
            customer.setNhanBanTin(false);
            customer.setLaTaiKhoanNoiBo(false);
            return khachHangRepository.save(customer);
        });

        // Seed POS order
        HoaDon hd = new HoaDon();
        hd.setMaDonHang("HDSVN20260619-T100");
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(new BigDecimal("300000"));
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setNguoiXacNhanThanhToan("Staff Tester");
        hd.setThoiGianXacNhan(LocalDateTime.now());
        hd.setDiaChiNhan("Bán tại quầy");
        hd.setSdtNhan("0000000000");
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        hd.setTrangThaiDonHang("da_giao");
        hd.setDonViVanChuyen(dvvc);
        hd.setPhuongThucThanhToan(pttt);
        NhanVien nv = new NhanVien();
        nv.setTaiKhoan(staff);
        nv.setHoTenNv("Staff Tester");
        nv.setChucVu("Nhân viên");
        nv.setSoDienThoaiNv("0111222333");
        nv = nhanVienRepository.save(nv);

        hd.setNhanVien(nv);
        hd.setKhachHang(kh);
        hd = hoaDonRepository.save(hd);

        mockMvc.perform(get("/admin/don-hang/detail-json")
                .param("id", String.valueOf(hd.getId()))
                .sessionAttr("idNguoiDung", staff.getId())
                .sessionAttr("vaiTro", "NV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(hd.getId()))
                .andExpect(jsonPath("$.maDonHang").value(hd.getMaDonHang()))
                .andExpect(jsonPath("$.nguoiXacNhan").value("Staff Tester"))
                .andExpect(jsonPath("$.thoiGianXacNhan").isNotEmpty());
    }

    @Test
    public void testGetChiTietKhachHangApi() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("khach_test_api@smashvn.com");
        tk.setMatKhau("123456");
        tk.setVaiTro("KH");
        tk.setTrangThai("hoat_dong");
        tk = taiKhoanRepository.save(tk);

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoTenKh("Nguyễn Văn Test");
        kh.setSoDienThoaiKh("0988777666");
        kh = khachHangRepository.save(kh);

        mockMvc.perform(get("/admin/khach-hang/api/" + kh.getId())
                .sessionAttr("idNguoiDung", tk.getId())
                .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(kh.getId()))
                .andExpect(jsonPath("$.maKhachHang").value("KH" + kh.getId()))
                .andExpect(jsonPath("$.hoTen").value("Nguyễn Văn Test"))
                .andExpect(jsonPath("$.soDienThoai").value("0988777666"))
                .andExpect(jsonPath("$.trangThaiTaiKhoan").value("Hoạt động"))
                .andExpect(jsonPath("$.tongDonHoanThanh").value(0))
                .andExpect(jsonPath("$.tongChiTieuFormatted").value("0 ₫"));
    }

    @Test
    public void testGetOrderDetailJson_WithReturnEvidence_AdminAndStaff() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 1. Seed Manager (QL) and Staff (NV)
        TaiKhoan manager = new TaiKhoan();
        manager.setUsername("manager_return_test@smashvn.com");
        manager.setMatKhau("pass123");
        manager.setVaiTro("QL");
        manager = taiKhoanRepository.save(manager);

        TaiKhoan staff = new TaiKhoan();
        staff.setUsername("staff_return_test@smashvn.com");
        staff.setMatKhau("pass123");
        staff.setVaiTro("NV");
        staff = taiKhoanRepository.save(staff);

        // 2. Seed Customer and Order with return evidence
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan p = new PhuongThucThanhToan();
            p.setTenPhuongThuc("Tiền mặt");
            return phuongThucThanhToanDAO.save(p);
        });
        DonViVanChuyen dvvc = donViVanChuyenDAO.findAll().stream().findFirst().orElseGet(() -> {
            DonViVanChuyen d = new DonViVanChuyen();
            d.setTenDonVi("Mua tại quầy");
            d.setHotline("000000");
            return donViVanChuyenDAO.save(d);
        });

        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setMaDonHang("HD-RETURN-TEST-001");
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(new BigDecimal("1500000"));
        hd.setDiaChiNhan("123 Le Loi, Quan 1, TP HCM");
        hd.setSdtNhan("0988776655");
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(dvvc);
        hd.setTrangThaiDonHang("da_giao");
        hd.setLoaiYeuCauDoiTra("TRA");
        hd.setLyDoHoanTra("Sản phẩm bị lỗi / hỏng hóc - Vợt bị nứt ở khung");
        hd.setBangChungHoanTra("[\"/uploads/returns/999/f5c7378d-a1e2-4d9f.mp4\"]");
        hd.setTrangThaiHoanHang(com.smashvn.shop.entity.ReturnStatus.PENDING_APPROVAL);
        hd = hoaDonRepository.save(hd);

        // 3. Test Admin (QL) access
        mockMvc.perform(get("/admin/don-hang/detail-json")
                        .param("id", String.valueOf(hd.getId()))
                        .sessionAttr("idNguoiDung", manager.getId())
                        .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(hd.getId()))
                .andExpect(jsonPath("$.loaiYeuCauDoiTra").value("TRA"))
                .andExpect(jsonPath("$.lyDoHoanTra").value("Sản phẩm bị lỗi / hỏng hóc - Vợt bị nứt ở khung"))
                .andExpect(jsonPath("$.bangChungHoanTra").value("[\"/uploads/returns/999/f5c7378d-a1e2-4d9f.mp4\"]"))
                .andExpect(jsonPath("$.trangThaiHoanHang").value("PENDING_APPROVAL"));

        // 4. Test Staff (NV) access
        mockMvc.perform(get("/admin/don-hang/detail-json")
                        .param("id", String.valueOf(hd.getId()))
                        .sessionAttr("idNguoiDung", staff.getId())
                        .sessionAttr("vaiTro", "NV"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(hd.getId()))
                .andExpect(jsonPath("$.loaiYeuCauDoiTra").value("TRA"))
                .andExpect(jsonPath("$.lyDoHoanTra").value("Sản phẩm bị lỗi / hỏng hóc - Vợt bị nứt ở khung"))
                .andExpect(jsonPath("$.bangChungHoanTra").value("[\"/uploads/returns/999/f5c7378d-a1e2-4d9f.mp4\"]"))
                .andExpect(jsonPath("$.trangThaiHoanHang").value("PENDING_APPROVAL"));
    }

    @Test
    public void testGetOrderDetailJson_WithoutReturnEvidence() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        TaiKhoan manager = new TaiKhoan();
        manager.setUsername("manager_no_return_test@smashvn.com");
        manager.setMatKhau("pass123");
        manager.setVaiTro("QL");
        manager = taiKhoanRepository.save(manager);

        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan p = new PhuongThucThanhToan();
            p.setTenPhuongThuc("Tiền mặt");
            return phuongThucThanhToanDAO.save(p);
        });
        DonViVanChuyen dvvc = donViVanChuyenDAO.findAll().stream().findFirst().orElseGet(() -> {
            DonViVanChuyen d = new DonViVanChuyen();
            d.setTenDonVi("Mua tại quầy");
            d.setHotline("000000");
            return donViVanChuyenDAO.save(d);
        });

        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setMaDonHang("HD-NO-RETURN-002");
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(new BigDecimal("500000"));
        hd.setDiaChiNhan("123 Le Loi, Quan 1, TP HCM");
        hd.setSdtNhan("0988776655");
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(dvvc);
        hd.setTrangThaiDonHang("cho_xac_nhan");
        hd = hoaDonRepository.save(hd);

        mockMvc.perform(get("/admin/don-hang/detail-json")
                        .param("id", String.valueOf(hd.getId()))
                        .sessionAttr("idNguoiDung", manager.getId())
                        .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(hd.getId()))
                .andExpect(jsonPath("$.bangChungHoanTra").value(""))
                .andExpect(jsonPath("$.trangThaiHoanHang").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    public void testConfirmRefund_WithoutImage_Fails() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        TaiKhoan manager = new TaiKhoan();
        manager.setUsername("mgr_refund_noimg@smashvn.com");
        manager.setMatKhau("pass123");
        manager.setVaiTro("QL");
        manager = taiKhoanRepository.save(manager);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/don-hang/confirm-refund")
                        .requestAttr("_csrf", csrfToken)
                        .sessionAttr("idNguoiDung", manager.getId())
                        .sessionAttr("vaiTro", "QL")
                        .param("idHoaDon", "100")
                        .param("phuongThucHoanTien", "CHUYEN_KHOAN")
                        .param("soTienHoan", "100000")
                        .param("maGiaoDichHoanTien", "FT123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("errorMsg", "Vui lòng tải lên ảnh / chứng từ xác nhận đã hoàn tiền cho khách hàng."));
    }

    @Test
    public void testConfirmRefund_WithValidImage_Success() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        TaiKhoan manager = new TaiKhoan();
        manager.setUsername("mgr_refund_ok@smashvn.com");
        manager.setMatKhau("pass123");
        manager.setVaiTro("QL");
        manager = taiKhoanRepository.save(manager);

        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseThrow();
        DonViVanChuyen dvvc = donViVanChuyenDAO.findAll().stream().findFirst().orElseThrow();

        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setMaDonHang("HD-REFUND-IMG-001");
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(new BigDecimal("500000"));
        hd.setDiaChiNhan("123 Le Loi, Quan 1, TP HCM");
        hd.setSdtNhan("0988776655");
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(dvvc);
        hd.setTrangThaiDonHang("da_giao");
        hd.setLoaiYeuCauDoiTra("TRA");
        hd.setLyDoHoanTra("Vợt gãy");
        hd.setTrangThaiHoanHang(com.smashvn.shop.entity.ReturnStatus.RETURNED);
        hd.setTrangThaiXuLyHangHoan(com.smashvn.shop.entity.ReturnInventoryStatus.DA_HOAN_KHO);
        hd = hoaDonRepository.save(hd);

        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", baos);
        org.springframework.mock.web.MockMultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                "fileChungTu", "refund_proof.png", "image/png", baos.toByteArray()
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/admin/don-hang/confirm-refund")
                        .file(mockFile)
                        .requestAttr("_csrf", csrfToken)
                        .sessionAttr("idNguoiDung", manager.getId())
                        .sessionAttr("vaiTro", "QL")
                        .param("idHoaDon", String.valueOf(hd.getId()))
                        .param("phuongThucHoanTien", "CHUYEN_KHOAN")
                        .param("soTienHoan", "500000")
                        .param("maGiaoDichHoanTien", "FT_PROOF_" + System.currentTimeMillis()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attribute("successMsg", "Đã xác nhận hoàn tiền thành công cho khách hàng!"));
    }

    @Test
    public void testHeaderQuickSearch_HiddenForStaff() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        mockMvc.perform(get("/admin/don-hang")
                        .requestAttr("_csrf", csrfToken)
                        .sessionAttr("activeRole", "NV")
                        .sessionAttr("vaiTro", "NV"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("adminGlobalSearch"))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Tìm sản phẩm..."))));
    }

    @Test
    public void testHeaderQuickSearch_VisibleForManager() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        mockMvc.perform(get("/admin/don-hang")
                        .requestAttr("_csrf", csrfToken)
                        .sessionAttr("activeRole", "QL")
                        .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("adminGlobalSearch")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Tìm sản phẩm...")));
    }
}

