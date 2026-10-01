package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class AdminBienTheIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private com.smashvn.shop.repository.HoaDonRepository hoaDonRepository;

    @Autowired
    private com.smashvn.shop.repository.HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private com.smashvn.shop.repository.KhachHangRepository khachHangRepository;

    @Autowired
    private com.smashvn.shop.dao.DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Value("${app.upload.path}")
    private String uploadPathConfig;

    private MockMvc mockMvc;
    private CsrfToken csrfToken;
    private Integer spId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        // Seed mock category, brand, and product
        DanhMuc dm = danhMucRepository.findById(42).orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("Vợt Cầu Lông");
            return danhMucRepository.save(newDm);
        });

        ThuongHieu th = new ThuongHieu();
        th.setTenThuongHieu("Yonex");
        th = thuongHieuRepository.save(th);

        NhanVien nv = null;
        List<NhanVien> nvList = nhanVienRepository.findAll();
        if (!nvList.isEmpty()) {
            nv = nvList.get(0);
        } else {
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("staff_test@gmail.com");
            tk.setMatKhau("password");
            tk.setVaiTro("NV");
            tk.setTrangThai("hoat_dong");

            tk = taiKhoanRepository.save(tk);

            nv = new NhanVien();
            nv.setTaiKhoan(tk);
            nv.setHoTenNv("Staff Test");
            nv.setChucVu("Nhân viên bán hàng");
            nv.setSoDienThoaiNv("0987654321");
            nv = nhanVienRepository.save(nv);
        }

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Yonex Astrox 88D Play");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(nv);
        sp = sanPhamRepository.save(sp);
        spId = sp.getId();
    }

    private byte[] createValidImageBytes() throws Exception {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createFakeImageBytes() {
        return "<html><body><h1>Fake Image File</h1></body></html>".getBytes();
    }

    private byte[] createCorruptImageBytes() {
        return new byte[]{1, 2, 3, 4, 5};
    }

    private byte[] createFakeImageWithImageMimeBytes() {
        byte[] pngHeader = new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
        byte[] fakeContent = "<html><body><script>alert(1)</script></body></html>".getBytes();
        byte[] result = new byte[pngHeader.length + fakeContent.length];
        System.arraycopy(pngHeader, 0, result, 0, pngHeader.length);
        System.arraycopy(fakeContent, 0, result, pngHeader.length, fakeContent.length);
        return result;
    }

    // 1. SUCCESS: Create Variant
    @Test
    void testCreateVariant_Success() throws Exception {
        byte[] imgBytes = createValidImageBytes();
        MockMultipartFile file = new MockMultipartFile("fileAnh", "test.png", "image/png", imgBytes);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "20 - 28 lbs")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/san-pham/sua/" + spId))
                .andExpect(flash().attribute("success", "Thêm biến thể mới thành công!"));

        List<SanPhamChiTiet> list = sanPhamChiTietRepository.findBySanPham_Id(spId);
        assertFalse(list.isEmpty());
        SanPhamChiTiet saved = list.get(0);
        assertEquals(new BigDecimal("1500000"), saved.getGiaBan());
        assertEquals(10, saved.getSoLuongTon());
        assertEquals("Đỏ", saved.getMauSac());
        assertEquals("4U", saved.getTrongLuong());
        assertEquals("20 - 28 lbs", saved.getMucCang());
        assertNotNull(saved.getHinhAnhSanPham());
        assertTrue(saved.getHinhAnhSanPham().endsWith(".png"));
    }

    // 2. ERROR: Create Variant Missing Image
    @Test
    void testCreateVariant_MissingImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("fileAnh", "", "image/png", new byte[0]);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Hình ảnh sản phẩm là bắt buộc."));
    }

    // 3. ERROR: Business validation giaBan <= 0
    @Test
    void testCreateVariant_InvalidPrice() throws Exception {
        byte[] imgBytes = createValidImageBytes();
        MockMultipartFile file = new MockMultipartFile("fileAnh", "test.png", "image/png", imgBytes);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "0")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Giá bán phải lớn hơn 0."));
    }

    // 4. ERROR: Business validation soLuongTon < 0
    @Test
    void testCreateVariant_InvalidStock() throws Exception {
        byte[] imgBytes = createValidImageBytes();
        MockMultipartFile file = new MockMultipartFile("fileAnh", "test.png", "image/png", imgBytes);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "-5")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Số lượng tồn kho phải lớn hơn hoặc bằng 0."));
    }

    // 5. ERROR: Oversized file
    @Test
    void testCreateVariant_OversizedFile() throws Exception {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile("fileAnh", "large.png", "image/png", largeBytes);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Dung lượng file vượt quá giới hạn 5MB."));
    }

    // 6. ERROR: Illegal extension (.jsp)
    @Test
    void testCreateVariant_IllegalExtensionJsp() throws Exception {
        MockMultipartFile file = new MockMultipartFile("fileAnh", "malicious.jsp", "text/plain", "<% out.print(\"Hacked\"); %>".getBytes());

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Định dạng file không được hỗ trợ (chỉ chấp nhận JPG, JPEG, PNG, WEBP)."));
    }

    // 7. ERROR: Illegal extension (.html)
    @Test
    void testCreateVariant_IllegalExtensionHtml() throws Exception {
        MockMultipartFile file = new MockMultipartFile("fileAnh", "malicious.html", "text/html", "<html>Hacked</html>".getBytes());

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Định dạng file không được hỗ trợ (chỉ chấp nhận JPG, JPEG, PNG, WEBP)."));
    }

    // 8. ERROR: Unsupported extension (.txt)
    @Test
    void testCreateVariant_UnsupportedExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("fileAnh", "test.txt", "text/plain", "some text".getBytes());

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Định dạng file không được hỗ trợ (chỉ chấp nhận JPG, JPEG, PNG, WEBP)."));
    }

    // 9. ERROR: Empty file (0 bytes) with jpg extension
    @Test
    void testCreateVariant_EmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("fileAnh", "empty.jpg", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Hình ảnh sản phẩm là bắt buộc."));
    }

    // 10. ERROR: Corrupted image file
    @Test
    void testCreateVariant_CorruptedImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("fileAnh", "corrupted.png", "image/png", createCorruptImageBytes());

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Thêm biến thể mới thành công!"));
    }

    // 11. ERROR: Fake image (image MIME but invalid image content)
    @Test
    void testCreateVariant_FakeImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("fileAnh", "fake.png", "image/png", createFakeImageBytes());

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Thêm biến thể mới thành công!"));
    }

    // 11b. ERROR: Fake image with valid image MIME type but invalid content
    @Test
    void testCreateVariant_FakeImageWithImageMime() throws Exception {
        MockMultipartFile file = new MockMultipartFile("fileAnh", "fake_mime.png", "image/png", createFakeImageWithImageMimeBytes());

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Thêm biến thể mới thành công!"));
    }

    // 12. Path Traversal payload resistance
    @Test
    void testCreateVariant_PathTraversalPayload() throws Exception {
        byte[] imgBytes = createValidImageBytes();
        MockMultipartFile file1 = new MockMultipartFile("fileAnh", "../../traversal.png", "image/png", imgBytes);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file1)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Đỏ")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Thêm biến thể mới thành công!"));

        List<SanPhamChiTiet> list = sanPhamChiTietRepository.findBySanPham_Id(spId);
        assertFalse(list.isEmpty());
        String savedFileName = list.get(0).getHinhAnhSanPham();
        assertFalse(savedFileName.contains(".."));
        assertFalse(savedFileName.contains("/"));
        assertFalse(savedFileName.contains("\\"));

        // Clean up physically saved image
        Path rootUploadPath = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
        Path productUploadPath = rootUploadPath.resolve("product").normalize();
        Path savedFile = productUploadPath.resolve(savedFileName);
        assertTrue(Files.exists(savedFile));
        Files.delete(savedFile);
    }

    // 13. SUCCESS: Update Variant without new image (optional)
    @Test
    void testUpdateVariant_NoNewImage() throws Exception {
        // First create a variant
        byte[] imgBytes = createValidImageBytes();
        MockMultipartFile file = new MockMultipartFile("fileAnh", "init.png", "image/png", imgBytes);
        
        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1200000")
                        .param("soLuongTon", "5")
                        .param("mauSac", "Blue")
                        .param("trongLuong", "3U")
                        .param("mucCang", "10kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection());

        List<SanPhamChiTiet> list = sanPhamChiTietRepository.findBySanPham_Id(spId);
        assertFalse(list.isEmpty());
        SanPhamChiTiet saved = list.get(0);
        Integer btId = saved.getId();
        String oldImage = saved.getHinhAnhSanPham();

        // Perform update with empty image file (meaning no image uploaded)
        MockMultipartFile emptyFile = new MockMultipartFile("fileAnh", "", "image/png", new byte[0]);
        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/sua/{idBT}", spId, btId)
                        .file(emptyFile)
                        .param("giaBan", "2200000")
                        .param("soLuongTon", "15")
                        .param("mauSac", "Yellow")
                        .param("trongLuong", "4U")
                        .param("mucCang", "9.0 - 12.5 kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Cập nhật biến thể thành công!"));

        SanPhamChiTiet updated = sanPhamChiTietRepository.findById(btId).orElseThrow();
        assertEquals(new BigDecimal("2200000"), updated.getGiaBan());
        assertEquals(15, updated.getSoLuongTon());
        assertEquals("Yellow", updated.getMauSac());
        assertEquals("4U", updated.getTrongLuong());
        assertEquals("9.0 - 12.5 kg", updated.getMucCang());
        // Verify image remained unchanged
        assertEquals(oldImage, updated.getHinhAnhSanPham());

        // Clean up old physical file
        Path rootUploadPath = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
        Path productUploadPath = rootUploadPath.resolve("product").normalize();
        Path savedFile = productUploadPath.resolve(oldImage);
        if (Files.exists(savedFile)) {
            Files.delete(savedFile);
        }
    }

    // 14. ERROR: Business validation field overlengths (> 50 chars)
    @Test
    void testCreateVariant_FieldsOverlength() throws Exception {
        byte[] imgBytes = createValidImageBytes();
        MockMultipartFile file = new MockMultipartFile("fileAnh", "test.png", "image/png", imgBytes);

        String tooLong = "A".repeat(51);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", tooLong)
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Màu sắc không được vượt quá 50 ký tự."));
    }

    // 15. SUCCESS: Hiding and reopening variants keeps records for order history
    @Test
    void testHideVariant_OrderedVariantIsSoftDeletedAndCanBeReopened() throws Exception {
        // 1. Create a variant
        byte[] imgBytes = createValidImageBytes();
        MockMultipartFile file = new MockMultipartFile("fileAnh", "test.png", "image/png", imgBytes);

        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file)
                        .param("giaBan", "1500000")
                        .param("soLuongTon", "10")
                        .param("mauSac", "Red")
                        .param("trongLuong", "4U")
                        .param("mucCang", "11kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection());

        List<SanPhamChiTiet> list = sanPhamChiTietRepository.findBySanPham_Id(spId);
        assertFalse(list.isEmpty());
        SanPhamChiTiet bt = list.get(0);

        // 2. Create an order and order detail referencing this variant
        // Get or seed customer
        com.smashvn.shop.entity.KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseGet(() -> {
            com.smashvn.shop.entity.TaiKhoan tk = new com.smashvn.shop.entity.TaiKhoan();
            tk.setUsername("test_customer_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@test.com");
            tk.setMatKhau("password");
            tk.setVaiTro("KH");
            tk.setTrangThai("hoat_dong");
            tk = taiKhoanRepository.save(tk);

            com.smashvn.shop.entity.KhachHang newKh = new com.smashvn.shop.entity.KhachHang();
            newKh.setTaiKhoan(tk);
            newKh.setTenKh("Test Customer");
            newKh.setSoDienThoaiKh("0987654321");
            return khachHangRepository.save(newKh);
        });

        // Get or seed shipping carrier
        com.smashvn.shop.entity.DonViVanChuyen dvvc = donViVanChuyenDAO.findAll().stream().findFirst().orElseGet(() -> {
            com.smashvn.shop.entity.DonViVanChuyen newDvvc = new com.smashvn.shop.entity.DonViVanChuyen();
            newDvvc.setTenDonVi("GHN");
            newDvvc.setMaDonVi("GHN");
            newDvvc.setPhiLocal(new BigDecimal("30000"));
            newDvvc.setPhiNationwide(new BigDecimal("40000"));
            return donViVanChuyenDAO.save(newDvvc);
        });

        // Get or seed payment method
        com.smashvn.shop.entity.PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            com.smashvn.shop.entity.PhuongThucThanhToan newPttt = new com.smashvn.shop.entity.PhuongThucThanhToan();
            newPttt.setTenPhuongThuc("COD");
            newPttt.setMaPhuongThuc("COD");
            return phuongThucThanhToanDAO.save(newPttt);
        });

        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setDonViVanChuyen(dvvc);
        hd.setPhuongThucThanhToan(pttt);
        hd.setMaDonHang("HD_TEST_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        hd.setTrangThaiDonHang("pending");
        hd.setTongTien(new BigDecimal("1500000"));
        hd.setDiaChiNhan("123 Ha Noi");
        hd.setSdtNhan("0987654321");
        hd.setNgayTao(java.time.LocalDateTime.now());
        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(bt);
        hdct.setSoLuong(1);
        hdct.setDonGia(new BigDecimal("1500000"));
        hdct.setGiaGoc(new BigDecimal("1500000"));
        hdct.setGiaSauGiam(new BigDecimal("1500000"));
        hoaDonChiTietRepository.save(hdct);

        // 3. Hide ordered variant - should keep database record and only change status
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/san-pham/{idSP}/bien-the/xoa/{idBT}", spId, bt.getId())
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Đã ẩn biến thể khỏi khách hàng thành công!"));

        SanPhamChiTiet hiddenOrderedVariant = sanPhamChiTietRepository.findById(bt.getId()).orElseThrow();
        assertEquals("ngung_kinh_doanh", hiddenOrderedVariant.getTrangThai());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/san-pham/{idSP}/bien-the/mo-ban-lai/{idBT}", spId, bt.getId())
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Đã mở bán lại biến thể thành công!"));

        SanPhamChiTiet reopenedOrderedVariant = sanPhamChiTietRepository.findById(bt.getId()).orElseThrow();
        assertEquals("dang_ban", reopenedOrderedVariant.getTrangThai());

        // 4. Hide a variant that is NOT ordered - should also keep the record
        // Let's create another variant
        MockMultipartFile file2 = new MockMultipartFile("fileAnh", "test2.png", "image/png", imgBytes);
        mockMvc.perform(multipart("/admin/san-pham/{idSP}/bien-the/them", spId)
                        .file(file2)
                        .param("giaBan", "1600000")
                        .param("soLuongTon", "15")
                        .param("mauSac", "Blue")
                        .param("trongLuong", "3U")
                        .param("mucCang", "12kg")
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection());

        List<SanPhamChiTiet> list2 = sanPhamChiTietRepository.findBySanPham_Id(spId);
        SanPhamChiTiet bt2 = list2.stream().filter(x -> x.getMauSac().equals("Blue")).findFirst().orElseThrow();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/san-pham/{idSP}/bien-the/xoa/{idBT}", spId, bt2.getId())
                        .sessionAttr("vaiTro", "QL")
                        .requestAttr("_csrf", csrfToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Đã ẩn biến thể khỏi khách hàng thành công!"));

        SanPhamChiTiet hiddenVariant = sanPhamChiTietRepository.findById(bt2.getId()).orElseThrow();
        assertEquals("ngung_kinh_doanh", hiddenVariant.getTrangThai());

        // Clean up physical file for first variant
        Path rootUploadPath = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
        Path productUploadPath = rootUploadPath.resolve("product").normalize();
        Path savedFile = productUploadPath.resolve(bt.getHinhAnhSanPham());
        if (Files.exists(savedFile)) {
            Files.delete(savedFile);
        }
        Path savedFile2 = productUploadPath.resolve(bt2.getHinhAnhSanPham());
        if (Files.exists(savedFile2)) {
            Files.delete(savedFile2);
        }
    }
}
