package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class GioHangValidationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private GioHangChiTietRepository gioHangChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    private MockMvc mockMvc;
    private TaiKhoan testUser;
    private KhachHang testKhachHang;
    private SanPhamChiTiet testSpct;
    private CsrfToken csrfToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        // Seed test user
        testUser = new TaiKhoan();
        testUser.setUsername("cart_tester@gmail.com");
        testUser.setMatKhau("testpass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");

        testUser = taiKhoanRepository.save(testUser);

        testKhachHang = new KhachHang();
        testKhachHang.setTaiKhoan(testUser);
        testKhachHang.setHoKh("Cart");
        testKhachHang.setTenKh("Tester");
        testKhachHang.setSoDienThoaiKh("0987654321");
        testKhachHang = khachHangRepository.save(testKhachHang);

        // Retrieve or seed DanhMuc
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("Mặc định");
            return danhMucRepository.save(newDm);
        });

        // Retrieve or seed ThuongHieu
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu newTh = new ThuongHieu();
            newTh.setTenThuongHieu("Mặc định");
            return thuongHieuRepository.save(newTh);
        });

        // Retrieve or seed NhanVien
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan nvUser = new TaiKhoan();
            nvUser.setUsername("staff_tester@gmail.com");
            nvUser.setMatKhau("testpass123");
            nvUser.setVaiTro("NV");
            nvUser.setTrangThai("hoat_dong");

            nvUser = taiKhoanRepository.save(nvUser);

            NhanVien newNv = new NhanVien();
            newNv.setTaiKhoan(nvUser);
            newNv.setHoTenNv("Staff Tester");
            newNv.setChucVu("Nhân viên bán hàng");
            newNv.setSoDienThoaiNv("0912345679");
            return nhanVienRepository.save(newNv);
        });

        // Seed a test product
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Cầu Lông Yonex Astrox");
        sp.setTrangThai("dang_ban");
        sp.setMoTa("Mô tả sản phẩm");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp = sanPhamRepository.save(sp);

        testSpct = new SanPhamChiTiet();
        testSpct.setSanPham(sp);
        testSpct.setMauSac("Đỏ");
        testSpct.setTrongLuong("4U");
        testSpct.setMucCang("26 lbs");
        testSpct.setSoLuongTon(100); // Stock = 100
        testSpct.setGiaBan(new BigDecimal("2000000"));
        testSpct = sanPhamChiTietRepository.save(testSpct);
    }

    @Test
    void testThemVaoGio_NullQuantity() throws Exception {
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng sản phẩm không được để trống."));
    }

    @Test
    void testThemVaoGio_NegativeQuantity() throws Exception {
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng sản phẩm thêm vào giỏ hàng phải lớn hơn 0."));
    }

    @Test
    void testThemVaoGio_ZeroQuantity() throws Exception {
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng sản phẩm thêm vào giỏ hàng phải lớn hơn 0."));
    }

    @Test
    void testThemVaoGio_Success() throws Exception {
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andExpect(jsonPath("$.tenSanPham").value("Vợt Cầu Lông Yonex Astrox"))
                .andExpect(jsonPath("$.soLuongThem").value(5));
    }

    @Test
    void testThemVaoGio_ExceedStock() throws Exception {
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "105")) // Stock = 100
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng tồn kho không đủ! Chỉ còn 100 sản phẩm."));
    }

    @Test
    void testThemVaoGio_IntegerOverflow() throws Exception {
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", String.valueOf(Integer.MAX_VALUE)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng sản phẩm thêm vào giỏ hàng không được vượt quá 999."));
    }

    @Test
    void testThemVaoGio_CumulativeExceedLimit() throws Exception {
        // Change stock to 2000 to avoid stock limit interfering with MAX_CART_QUANTITY limit (999)
        testSpct.setSoLuongTon(2000);
        sanPhamChiTietRepository.save(testSpct);

        // Add 990 items
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "990"))
                .andExpect(status().isOk());

        // Try adding 10 more (total 1000)
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Tổng số lượng sản phẩm này trong giỏ hàng không được vượt quá 999."));
    }

    @Test
    void testCapNhatSoLuong_NullQuantity() throws Exception {
        // First add an item to get an idChiTiet
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "5"))
                .andExpect(status().isOk());

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(testKhachHang.getId());
        List<GioHangChiTiet> details = gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
        assertFalse(details.isEmpty());
        Integer idChiTiet = details.get(0).getId();

        mockMvc.perform(post("/gio-hang/cap-nhat")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idChiTiet", String.valueOf(idChiTiet)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng sản phẩm không được để trống."));
    }

    @Test
    void testCapNhatSoLuong_NegativeQuantity() throws Exception {
        // First add an item
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "5"))
                .andExpect(status().isOk());

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(testKhachHang.getId());
        List<GioHangChiTiet> details = gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
        Integer idChiTiet = details.get(0).getId();

        mockMvc.perform(post("/gio-hang/cap-nhat")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idChiTiet", String.valueOf(idChiTiet))
                        .param("soLuong", "-2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng sản phẩm trong giỏ hàng phải lớn hơn 0."));
    }

    @Test
    void testCapNhatSoLuong_ExceedStock() throws Exception {
        // First add an item
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "5"))
                .andExpect(status().isOk());

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(testKhachHang.getId());
        List<GioHangChiTiet> details = gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
        Integer idChiTiet = details.get(0).getId();

        mockMvc.perform(post("/gio-hang/cap-nhat")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idChiTiet", String.valueOf(idChiTiet))
                        .param("soLuong", "120")) // Stock is 100
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng tồn kho không đủ! Chỉ còn 100 sản phẩm."));
    }

    @Test
    void testCapNhatSoLuong_ExceedLimit() throws Exception {
        // Change stock to 2000 to avoid stock interference
        testSpct.setSoLuongTon(2000);
        sanPhamChiTietRepository.save(testSpct);

        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "5"))
                .andExpect(status().isOk());

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(testKhachHang.getId());
        List<GioHangChiTiet> details = gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
        Integer idChiTiet = details.get(0).getId();

        mockMvc.perform(post("/gio-hang/cap-nhat")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idChiTiet", String.valueOf(idChiTiet))
                        .param("soLuong", "1000")) // Over MAX_CART_QUANTITY
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Số lượng sản phẩm trong giỏ hàng không được vượt quá 999."));
    }

    @Test
    void testThemVaoGio_NoKhachHangProfile() throws Exception {
        // Create a new user account without a KhachHang profile
        TaiKhoan noProfileUser = new TaiKhoan();
        noProfileUser.setUsername("noprofile_tester@gmail.com");
        noProfileUser.setMatKhau("testpass123");
        noProfileUser.setVaiTro("QL"); // Role manager, typically doesn't have a profile by default
        noProfileUser.setTrangThai("hoat_dong");

        noProfileUser = taiKhoanRepository.save(noProfileUser);

        // Verify that the profile doesn't exist
        assertNull(khachHangRepository.findByTaiKhoan_Id(noProfileUser.getId()));

        // Perform add to cart
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", noProfileUser.getId())
                        .sessionAttr("vaiTro", "QL")
                .sessionAttr("vaiTro", "KH")
                .sessionAttr("vaiTro", "NV")
                .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soLuongThem").value(3));

        // Verify that the KhachHang profile was automatically created and links to this user
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(noProfileUser.getId());
        assertNotNull(kh);
        assertEquals("noprofile_tester", kh.getHoTenKh());
        assertEquals("", kh.getSoDienThoaiKh());
        assertTrue("QL".equals(kh.getTaiKhoan().getVaiTro())); // QL is an internal account
    }

    @Test
    void testBulkDelete_AllOrNothing_ForeignItem() throws Exception {
        // User A adds an item
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "2"))
                .andExpect(status().isOk());

        GioHang gioHangA = gioHangRepository.findByKhachHang_Id(testKhachHang.getId());
        List<GioHangChiTiet> detailsA = gioHangChiTietRepository.findByGioHang_Id(gioHangA.getId());
        Integer itemA = detailsA.get(0).getId();

        // Create User B with an item
        TaiKhoan userB = new TaiKhoan();
        userB.setUsername("userB_tester@gmail.com");
        userB.setMatKhau("testpass123");
        userB.setVaiTro("KH");
        userB.setTrangThai("hoat_dong");
        userB = taiKhoanRepository.save(userB);

        KhachHang khB = new KhachHang();
        khB.setTaiKhoan(userB);
        khB.setHoKh("User");
        khB.setTenKh("B");
        khB.setSoDienThoaiKh("0987654322");
        khB = khachHangRepository.save(khB);

        GioHang gioHangB = new GioHang();
        gioHangB.setKhachHang(khB);
        gioHangB = gioHangRepository.save(gioHangB);

        GioHangChiTiet itemBObj = new GioHangChiTiet();
        itemBObj.setGioHang(gioHangB);
        itemBObj.setSanPhamChiTiet(testSpct);
        itemBObj.setSoLuong(1);
        itemBObj = gioHangChiTietRepository.save(itemBObj);
        Integer itemB = itemBObj.getId();

        // User A attempts bulk delete containing User B's itemB (IDOR attempt)
        mockMvc.perform(post("/gio-hang/api/xoa-nhieu")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("selectedItemIds", String.valueOf(itemA))
                        .param("selectedItemIds", String.valueOf(itemB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("error"));

        // All-or-Nothing check: User A's itemA must NOT be deleted!
        assertTrue(gioHangChiTietRepository.existsById(itemA), "Owned item A should remain untouched when request includes a foreign ID.");
        assertTrue(gioHangChiTietRepository.existsById(itemB), "Foreign item B should remain untouched.");
    }

    @Test
    void testBulkDelete_DuplicateIds() throws Exception {
        // User adds an item
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "2"))
                .andExpect(status().isOk());

        GioHang gioHang = gioHangRepository.findByKhachHang_Id(testKhachHang.getId());
        List<GioHangChiTiet> details = gioHangChiTietRepository.findByGioHang_Id(gioHang.getId());
        Integer item1 = details.get(0).getId();

        // Send duplicate ID [item1, item1]
        mockMvc.perform(post("/gio-hang/api/xoa-nhieu")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("selectedItemIds", String.valueOf(item1))
                        .param("selectedItemIds", String.valueOf(item1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andExpect(jsonPath("$.deletedCount").value(1));

        assertFalse(gioHangChiTietRepository.existsById(item1));
    }
}
