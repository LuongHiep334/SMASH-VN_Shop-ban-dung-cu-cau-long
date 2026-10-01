package com.smashvn.shop.controller.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;

@SpringBootTest
@Transactional
public class CheckoutValidationIntegrationTest {

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
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private SoDiaChiRepository soDiaChiRepository;

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private TaiKhoan testUser;
    private KhachHang testKhachHang;
    private SanPhamChiTiet testSpct;
    private DonViVanChuyen testDvvc;
    private CsrfToken csrfToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

        // Clear all caches to avoid stale DonViVanChuyen cache
        if (cacheManager != null) {
            for (String cacheName : cacheManager.getCacheNames()) {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
        }

        // Seed test user
        testUser = new TaiKhoan();
        testUser.setUsername("checkout_tester@gmail.com");
        testUser.setMatKhau("testpass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");

        testUser = taiKhoanRepository.save(testUser);

        testKhachHang = new KhachHang();
        testKhachHang.setTaiKhoan(testUser);
        testKhachHang.setHoKh("Checkout");
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
            nvUser.setUsername("checkout_staff@gmail.com");
            nvUser.setMatKhau("testpass123");
            nvUser.setVaiTro("NV");
            nvUser.setTrangThai("hoat_dong");

            nvUser = taiKhoanRepository.save(nvUser);

            NhanVien newNv = new NhanVien();
            newNv.setTaiKhoan(nvUser);
            newNv.setHoTenNv("Staff Tester");
            newNv.setChucVu("Nhân viên bán hàng");
            newNv.setSoDienThoaiNv("0912345670");
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
        testSpct.setSoLuongTon(100);
        testSpct.setGiaBan(new BigDecimal("2000000"));
        testSpct = sanPhamChiTietRepository.save(testSpct);

        // Seed carrier
        testDvvc = new DonViVanChuyen();
        testDvvc.setTenDonVi("Giao Hàng Tiết Kiệm");
        testDvvc = donViVanChuyenDAO.save(testDvvc);

        // Seed payment method
        PhuongThucThanhToan pttt = new PhuongThucThanhToan();
        pttt.setTenPhuongThuc("COD");
        phuongThucThanhToanDAO.save(pttt);

        // Seed user cart item
        mockMvc.perform(post("/gio-hang/them")
                .sessionAttr("idNguoiDung", testUser.getId())
                .sessionAttr("vaiTro", "KH")
                .requestAttr("_csrf", csrfToken)
                .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                .param("soLuong", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void testSubmitCheckout_EmptyValues() throws Exception {
        // Missing name
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Họ và tên người nhận không được để trống."));

        // Missing phone
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "")
                .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Số điện thoại không được để trống."));

        // Missing address
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Địa chỉ nhận hàng không được để trống."));
    }

    @Test
    void testSubmitCheckout_InvalidPhoneFormat() throws Exception {
        // Invalid phone prefix (legacy format or wrong start digits)
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0123456789") // Invalid prefix
                .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09)."));

        // Non-numeric phone number
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "091abc3456") // Invalid characters
                .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Số điện thoại không đúng định dạng (phải có 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09)."));
    }

    @Test
    void testSubmitCheckout_ValidPhoneFormats() throws Exception {
        String[] validPhones = {"0912345678", "+84912345678", "0391234567"};
        for (String phone : validPhones) {
            MvcResult result = mockMvc.perform(post("/checkout/submit")
                    .sessionAttr("idNguoiDung", testUser.getId())
                    .requestAttr("_csrf", csrfToken)
                    .param("hoTenNhan", "Nguyễn Văn A")
                    .param("sdtNhan", phone)
                    .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                    .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                    .param("phuongThucThanhToan", "COD")
                    .param("ghnProvinceId", "201")
                    .param("ghnToDistrictId", "1454")
                    .param("ghnToWardCode", "1A0807"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trangThai").value("ok"))
                    .andReturn();

            String responseString = result.getResponse().getContentAsString();
            Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
            Integer orderId = (Integer) respMap.get("orderId");
            assertNotNull(orderId);

            // Re-seed cart for next loop iteration
            mockMvc.perform(post("/gio-hang/them")
                    .sessionAttr("idNguoiDung", testUser.getId())
                    .requestAttr("_csrf", csrfToken)
                    .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                    .param("soLuong", "1"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void testSubmitCheckout_VietnameseUnicodeSupport() throws Exception {
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn Ánh")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "Số 12 Phố Huế, Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);
        assertEquals("Số 12 Phố Huế, Hà Nội", savedOrder.getDiaChiNhan());
        assertEquals("0912345678", savedOrder.getSdtNhan());
    }

    @Test
    void testSubmitCheckout_NoteLengthBoundaries() throws Exception {
        // 499 chars -> Valid
        String note499 = "a".repeat(499);
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807")
                .param("ghiChu", note499))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"));

        // Re-seed cart
        mockMvc.perform(post("/gio-hang/them")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                .param("soLuong", "1"))
                .andExpect(status().isOk());

        // 500 chars -> Valid
        String note500 = "a".repeat(500);
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807")
                .param("ghiChu", note500))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"));

        // 501 chars -> Invalid
        String note501 = "a".repeat(501);
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "Số 1 Đường ABC, Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807")
                .param("ghiChu", note501))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Ghi chú đơn hàng tối đa 500 ký tự."));
    }

    @Test
    void testSubmitCheckout_XssSanitization() throws Exception {
        String xssName = "<b>Nguyễn Văn A</b>";
        String xssAddress = "<script>alert('XSS')</script>Hà Nội";
        String xssNote = "<img src=x onerror=alert(1)>Ghi chú";

        MvcResult result = mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", xssName)
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", xssAddress)
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807")
                .param("ghiChu", xssNote))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        // Verify clean text is saved to DB
        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);

        // check that HTML tag was stripped, retaining pure text
        assertEquals("Hà Nội", savedOrder.getDiaChiNhan());
        assertEquals("Ghi chú", savedOrder.getGhiChu());
    }

    private static String computeHmacSha256(String data, String key) throws Exception {
        byte[] keyBytes = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        javax.crypto.spec.SecretKeySpec signingKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Autowired
    private com.smashvn.shop.config.GhnConfig ghnConfig;

    @Test
    void testGhnWebhook_Authentication() throws Exception {
        Map<String, Object> payload = Map.of(
                "OrderCode", "GHN123456",
                "Status", "ready_to_pick"
        );

        // 1. No token -> 401
        mockMvc.perform(post("/api/ghn/webhook")
                .requestAttr("_csrf", csrfToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        // 2. Wrong token -> 401
        mockMvc.perform(post("/api/ghn/webhook")
                .requestAttr("_csrf", csrfToken)
                .param("token", "wrong_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        // 3. Valid token -> 200/Ok (order not found returns 200 with not_found status)
        mockMvc.perform(post("/api/ghn/webhook")
                .requestAttr("_csrf", csrfToken)
                .param("token", ghnConfig.getWebhookToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("not_found"));
    }

    @Test
    void testCheckoutPageValidation_OutofStock() throws Exception {
        testSpct.setSoLuongTon(0);
        sanPhamChiTietRepository.save(testSpct);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/checkout")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gio-hang?loi=*"));
    }

    @Test
    void testCheckoutPageValidation_NotSelling() throws Exception {
        SanPham sp = testSpct.getSanPham();
        sp.setTrangThai("ngung_ban");
        sanPhamRepository.save(sp);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/checkout")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gio-hang?loi=*"));
    }

    @Test
    void testCheckoutPageValidation_QuantityExceeded() throws Exception {
        // Add more items to cart to make quantity 2 (original cart had 1)
        mockMvc.perform(post("/gio-hang/them")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                .param("soLuong", "1"))
                .andExpect(status().isOk());

        // Now set stock to 1 (cart quantity is 2, stock is 1)
        testSpct.setSoLuongTon(1);
        sanPhamChiTietRepository.save(testSpct);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/checkout")
                .sessionAttr("idNguoiDung", testUser.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/gio-hang?loi=*"));
    }

    @Test
    void testShippingFeeTampering() throws Exception {
        // Submit ghnProvinceId=201 (Hanoi), but with district = 999999 (non-local)
        // Verify the server ignores the frontend's province ID and calculates the correct nationwide fee.
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "Đà Nẵng")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId())) // GHTK
                .param("phuongThucThanhToan", "COD")
                .param("ghnToDistrictId", "999999") // Non-local district
                .param("ghnProvinceId", "201")
                .param("ghnToWardCode", "1A0807")) // Pretending to be Hanoi
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);
        // The resolved fee should be GHTK Nationwide (30000), not Local (22000)
        assertEquals(0, new BigDecimal("30000").compareTo(savedOrder.getPhiVanChuyen()));
    }

    @Test
    void testSavedAddressOwnership() throws Exception {
        // Create user B
        TaiKhoan userB = new TaiKhoan();
        userB.setUsername("userb_address@gmail.com");
        userB.setMatKhau("testpass123");
        userB.setVaiTro("KH");
        userB.setTrangThai("hoat_dong");

        userB = taiKhoanRepository.save(userB);

        KhachHang khachHangB = new KhachHang();
        khachHangB.setTaiKhoan(userB);
        khachHangB.setHoKh("Khach");
        khachHangB.setTenKh("B");
        khachHangB.setSoDienThoaiKh("0987654322");
        khachHangB = khachHangRepository.save(khachHangB);

        // Create saved address for user B
        SoDiaChi soDiaChiB = new SoDiaChi();
        soDiaChiB.setKhachHang(khachHangB);
        soDiaChiB.setHoNguoiNhan("Khach");
        soDiaChiB.setTenNguoiNhan("B");
        soDiaChiB.setSdtNguoiNhan("0987654322");
        soDiaChiB.setDiaChiCuThe("123 Street B");
        soDiaChiB.setTinhThanh("Hà Nội");
        soDiaChiB.setThanhPho("Quận Ba Đình");
        soDiaChiB.setQuocGia("Việt Nam");
        soDiaChiB.setMaBuuDien("10000");
        soDiaChiB.setDefaultShipping(false);
        soDiaChiB.setDefaultBilling(false);
        soDiaChiB = soDiaChiRepository.save(soDiaChiB);

        // Attempt checkout as testUser (Customer A) but using user B's saved address ID
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("idDiaChiLuu", String.valueOf(soDiaChiB.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Địa chỉ đã lưu không tồn tại hoặc không thuộc về tài khoản của bạn. Vui lòng chọn địa chỉ khác hoặc nhập địa chỉ mới."));
    }

    @Test
    void testSavedAddressDeleted() throws Exception {
        // Create saved address for testUser
        SoDiaChi soDiaChi = new SoDiaChi();
        soDiaChi.setKhachHang(testKhachHang);
        soDiaChi.setHoNguoiNhan("Tester");
        soDiaChi.setTenNguoiNhan("Address");
        soDiaChi.setSdtNguoiNhan("0987654321");
        soDiaChi.setDiaChiCuThe("456 Street A");
        soDiaChi.setTinhThanh("Hà Nội");
        soDiaChi.setThanhPho("Quận Cầu Giấy");
        soDiaChi.setQuocGia("Việt Nam");
        soDiaChi.setMaBuuDien("10000");
        soDiaChi.setDefaultShipping(false);
        soDiaChi.setDefaultBilling(false);
        soDiaChi = soDiaChiRepository.save(soDiaChi);

        Integer savedAddressId = soDiaChi.getId();

        // Delete the address to simulate concurrent deletion after loading page
        soDiaChiRepository.delete(soDiaChi);

        // Attempt checkout and verify it fails transaction-safely
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("idDiaChiLuu", String.valueOf(savedAddressId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Địa chỉ đã lưu không tồn tại hoặc không thuộc về tài khoản của bạn. Vui lòng chọn địa chỉ khác hoặc nhập địa chỉ mới."));
    }

    @Test
    void testShippingFeeRecalculation() throws Exception {
        // Submit a valid checkout request
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnToDistrictId", "1454")
                .param("ghnProvinceId", "201")
                .param("ghnToWardCode", "1A0807"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);
        // The server computes the fee independently and stores it.
        assertNotNull(savedOrder.getPhiVanChuyen());
        assertTrue(savedOrder.getPhiVanChuyen().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testGhnMappingMissing() throws Exception {
        // Create saved address with unmappable values
        SoDiaChi unmappableAddress = new SoDiaChi();
        unmappableAddress.setKhachHang(testKhachHang);
        unmappableAddress.setHoNguoiNhan("Tester");
        unmappableAddress.setTenNguoiNhan("Unmappable");
        unmappableAddress.setSdtNguoiNhan("0987654321");
        unmappableAddress.setDiaChiCuThe("Something Weird");
        unmappableAddress.setTinhThanh("Fake Province");
        unmappableAddress.setThanhPho("Fake City");
        unmappableAddress.setQuocGia("Việt Nam");
        unmappableAddress.setMaBuuDien("10000");
        unmappableAddress.setDefaultShipping(false);
        unmappableAddress.setDefaultBilling(false);
        unmappableAddress = soDiaChiRepository.save(unmappableAddress);

        // Attempt checkout using this saved address ID
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("idDiaChiLuu", String.valueOf(unmappableAddress.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Địa chỉ đã lưu của bạn chưa được chuẩn hóa địa chỉ GHN. Vui lòng cập nhật sổ địa chỉ hoặc chọn \"Nhập địa chỉ mới\"."));
    }

    @Test
    void testApplyVoucherValidAndInvalid() throws Exception {
        NhanVien nv = nhanVienRepository.findAll().get(0);

        // 1. Create a valid voucher: Giảm phần trăm, 10%
        PhieuGiamGia validVoucher = new PhieuGiamGia();
        validVoucher.setMaPhieu("TEST10");
        validVoucher.setGiaTri(new BigDecimal("10"));
        validVoucher.setDonVi("%");
        validVoucher.setNgayBatDau(LocalDateTime.now().minusDays(1));
        validVoucher.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        validVoucher.setSoLuongConLai(5);
        validVoucher.setLoaiGiamGia("Giảm phần trăm");
        validVoucher.setNhanVien(nv);
        validVoucher.setActive(true);
        validVoucher.setGiaTriDonHangToiThieu(new BigDecimal("50000"));
        phieuGiamGiaRepository.save(validVoucher);

        // 2. Create an expired voucher
        PhieuGiamGia expiredVoucher = new PhieuGiamGia();
        expiredVoucher.setMaPhieu("TESTEXPIRED");
        expiredVoucher.setGiaTri(new BigDecimal("50000"));
        expiredVoucher.setDonVi("VND");
        expiredVoucher.setNgayBatDau(LocalDateTime.now().minusDays(5));
        expiredVoucher.setNgayKetThuc(LocalDateTime.now().minusDays(1));
        expiredVoucher.setSoLuongConLai(5);
        expiredVoucher.setLoaiGiamGia("Giảm trực tiếp");
        expiredVoucher.setNhanVien(nv);
        expiredVoucher.setActive(true);
        expiredVoucher.setGiaTriDonHangToiThieu(new BigDecimal("50000"));
        phieuGiamGiaRepository.save(expiredVoucher);

        // 3. Create an inactive voucher
        PhieuGiamGia inactiveVoucher = new PhieuGiamGia();
        inactiveVoucher.setMaPhieu("TESTINACTIVE");
        inactiveVoucher.setGiaTri(new BigDecimal("15"));
        inactiveVoucher.setDonVi("%");
        inactiveVoucher.setNgayBatDau(LocalDateTime.now().minusDays(1));
        inactiveVoucher.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        inactiveVoucher.setSoLuongConLai(5);
        inactiveVoucher.setLoaiGiamGia("Giảm phần trăm");
        inactiveVoucher.setNhanVien(nv);
        inactiveVoucher.setActive(false);
        inactiveVoucher.setGiaTriDonHangToiThieu(new BigDecimal("50000"));
        phieuGiamGiaRepository.save(inactiveVoucher);

        // 4. Create a voucher with minimum limit higher than cart value (cart value is 2M)
        PhieuGiamGia highMinVoucher = new PhieuGiamGia();
        highMinVoucher.setMaPhieu("TESTHIGH");
        highMinVoucher.setGiaTri(new BigDecimal("100000"));
        highMinVoucher.setDonVi("VND");
        highMinVoucher.setNgayBatDau(LocalDateTime.now().minusDays(1));
        highMinVoucher.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        highMinVoucher.setSoLuongConLai(5);
        highMinVoucher.setLoaiGiamGia("Giảm trực tiếp");
        highMinVoucher.setNhanVien(nv);
        highMinVoucher.setActive(true);
        highMinVoucher.setGiaTriDonHangToiThieu(new BigDecimal("3000000")); // 3M
        phieuGiamGiaRepository.save(highMinVoucher);

        // Test valid voucher apply
        mockMvc.perform(post("/api/voucher/apply")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("voucherCode", "TEST10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andExpect(jsonPath("$.maPhieu").value("TEST10"))
                .andExpect(jsonPath("$.giamGia").value(200000.0));

        // Test expired voucher apply
        mockMvc.perform(post("/api/voucher/apply")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("voucherCode", "TESTEXPIRED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Mã giảm giá đã hết hạn sử dụng."));

        // Test inactive voucher apply
        mockMvc.perform(post("/api/voucher/apply")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("voucherCode", "TESTINACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Mã giảm giá này đã ngưng hoạt động."));

        // Test min order limit violation
        mockMvc.perform(post("/api/voucher/apply")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("voucherCode", "TESTHIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Hạn mức tối thiểu để sử dụng mã này là 3,000,000 đ (Đơn của bạn: 2,000,000 đ)."));
    }

    @Test
    void testSubmitCheckoutWithVoucher() throws Exception {
        NhanVien nv = nhanVienRepository.findAll().get(0);

        PhieuGiamGia validVoucher = new PhieuGiamGia();
        validVoucher.setMaPhieu("TESTPROMO");
        validVoucher.setGiaTri(new BigDecimal("50000"));
        validVoucher.setDonVi("VND");
        validVoucher.setNgayBatDau(LocalDateTime.now().minusDays(1));
        validVoucher.setNgayKetThuc(LocalDateTime.now().plusDays(1));
        validVoucher.setSoLuongConLai(10);
        validVoucher.setLoaiGiamGia("Giảm trực tiếp");
        validVoucher.setNhanVien(nv);
        validVoucher.setActive(true);
        validVoucher.setGiaTriDonHangToiThieu(new BigDecimal("100000"));
        validVoucher = phieuGiamGiaRepository.save(validVoucher);

        // Submit checkout with voucher code
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnToDistrictId", "1454")
                .param("ghnProvinceId", "201")
                .param("ghnToWardCode", "1A0807")
                .param("voucherCode", "TESTPROMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        // Verify invoice was saved with correct voucher and amounts
        HoaDon savedOrder = hoaDonRepository.findById(orderId).orElse(null);
        assertNotNull(savedOrder);
        assertNotNull(savedOrder.getPhieuGiamGia());
        assertEquals("TESTPROMO", savedOrder.getPhieuGiamGia().getMaPhieu());

        // Verify remaining quantity has decreased
        PhieuGiamGia updatedVoucher = phieuGiamGiaRepository.findById(validVoucher.getId()).orElse(null);
        assertNotNull(updatedVoucher);
        assertEquals(9, updatedVoucher.getSoLuongConLai());
    }

    @Test
    void testGhnAddressResolution_ThaiNguyen() throws Exception {
        // Create a saved address in Kha Son, Phu Binh, Thai Nguyen
        SoDiaChi thaiNguyenAddress = new SoDiaChi();
        thaiNguyenAddress.setKhachHang(testKhachHang);
        thaiNguyenAddress.setHoNguoiNhan("Tester");
        thaiNguyenAddress.setTenNguoiNhan("ThaiNguyen");
        thaiNguyenAddress.setSdtNguoiNhan("0987654321");
        thaiNguyenAddress.setDiaChiCuThe("Xã Kha Sơn, Huyện Phú Bình, Tỉnh Thái Nguyên");
        thaiNguyenAddress.setTinhThanh("Thái Nguyên");
        thaiNguyenAddress.setThanhPho("Thái Nguyên"); // assigned as province name by UserAddressService
        thaiNguyenAddress.setQuocGia("Việt Nam");
        thaiNguyenAddress.setMaBuuDien("700000");
        thaiNguyenAddress.setDefaultShipping(false);
        thaiNguyenAddress.setDefaultBilling(false);
        thaiNguyenAddress = soDiaChiRepository.save(thaiNguyenAddress);

        com.smashvn.shop.service.api.GhnService ghnService = webApplicationContext.getBean(com.smashvn.shop.service.api.GhnService.class);
        com.smashvn.shop.service.api.GhnService.GhnAddressMapping mapping = ghnService.resolveGhnAddress(thaiNguyenAddress);

        assertNotNull(mapping);
        assertEquals(244, mapping.getProvinceId()); // Thai Nguyen province ID
        assertNotNull(mapping.getDistrictId());
        assertNotNull(mapping.getWardCode());

        // Verify that the resolved district name is indeed "Huyện Phú Bình" and not "Thành phố Thái Nguyên"
        List<Map<String, Object>> districts = ghnService.getDistricts(244);
        Map<String, Object> resolvedDistrict = districts.stream()
                .filter(d -> mapping.getDistrictId().equals(d.get("DistrictID")))
                .findFirst().orElse(null);

        assertNotNull(resolvedDistrict);
        String resolvedDistrictName = (String) resolvedDistrict.get("DistrictName");
        assertTrue(resolvedDistrictName.contains("Phú Bình"));
        assertFalse(resolvedDistrictName.contains("Thành phố Thái Nguyên"));

        // Verify ShippingZoneResolver classifies it as LOCAL
        com.smashvn.shop.service.api.ShippingZoneResolver zoneResolver = webApplicationContext.getBean(com.smashvn.shop.service.api.ShippingZoneResolver.class);
        com.smashvn.shop.entity.ShippingZone zone = zoneResolver.resolveZone(mapping.getDistrictId(), thaiNguyenAddress.getDiaChiCuThe());
        assertEquals(com.smashvn.shop.entity.ShippingZone.LOCAL, zone);
    }

    @Test
    void testGhnAddressResolution_ConfusedFields() throws Exception {
        // Create a saved address with confused fields (like ID 62/63 in DB)
        SoDiaChi confusedAddress = new SoDiaChi();
        confusedAddress.setKhachHang(testKhachHang);
        confusedAddress.setHoNguoiNhan("Tester");
        confusedAddress.setTenNguoiNhan("Confused");
        confusedAddress.setSdtNguoiNhan("0987654321");
        confusedAddress.setDiaChiCuThe("Trường Đại học Nông Lâm Thái Nguyên, Đường Mỹ Bạch, Phường Quyết Thắng");
        confusedAddress.setTinhThanh("Phường Phan Đình Phùng");
        confusedAddress.setThanhPho("Phường Phan Đình Phùng");
        confusedAddress.setQuocGia("Việt Nam");
        confusedAddress.setMaBuuDien("700000");
        confusedAddress.setDefaultShipping(false);
        confusedAddress.setDefaultBilling(false);
        confusedAddress = soDiaChiRepository.save(confusedAddress);

        com.smashvn.shop.service.api.GhnService ghnService = webApplicationContext.getBean(com.smashvn.shop.service.api.GhnService.class);
        com.smashvn.shop.service.api.GhnService.GhnAddressMapping mapping = ghnService.resolveGhnAddress(confusedAddress);

        assertNotNull(mapping);
        assertEquals(244, mapping.getProvinceId()); // resolved to Thai Nguyen (244) from diaChiCuThe
        assertNotNull(mapping.getDistrictId());
        assertNotNull(mapping.getWardCode());

        // Verify that it matched "Thành phố Thái Nguyên" as district and "Phường Quyết Thắng" as ward
        List<Map<String, Object>> districts = ghnService.getDistricts(244);
        Map<String, Object> resolvedDistrict = districts.stream()
                .filter(d -> mapping.getDistrictId().equals(d.get("DistrictID")))
                .findFirst().orElse(null);
        assertNotNull(resolvedDistrict);
        String resolvedDistrictName = (String) resolvedDistrict.get("DistrictName");
        assertTrue(resolvedDistrictName.contains("Thành phố Thái Nguyên"));

        // Verify ShippingZoneResolver classifies it as LOCAL
        com.smashvn.shop.service.api.ShippingZoneResolver zoneResolver = webApplicationContext.getBean(com.smashvn.shop.service.api.ShippingZoneResolver.class);
        com.smashvn.shop.entity.ShippingZone zone = zoneResolver.resolveZone(mapping.getDistrictId(), confusedAddress.getDiaChiCuThe());
        assertEquals(com.smashvn.shop.entity.ShippingZone.LOCAL, zone);
    }

    @Test
    void testSubmitCheckout_NewAddress_WithAllGhnIds() throws Exception {
        // Validate checkout with new address containing all GHN IDs successfully saves provinceId, districtId, and wardCode to database
        MvcResult result = mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "123 Đường Láng")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807")
                .param("tinhThanhText", "Hà Nội")
                .param("thanhPhoText", "Quận Đống Đa")
                .param("phuongXaText", "Phường Láng Thượng")
                .param("diaChiCuThe", "123 Đường Láng")
                .param("saveAddress", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseString = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseString, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");

        // Verify SoDiaChi record was saved with proper GHN fields
        List<SoDiaChi> savedAddresses = soDiaChiRepository.findByKhachHang_Id(testKhachHang.getId());
        boolean foundSavedWithGhn = false;
        for (SoDiaChi dc : savedAddresses) {
            if ("123 Đường Láng".equals(dc.getDiaChiCuThe()) &&
                Integer.valueOf(201).equals(dc.getProvinceId()) &&
                Integer.valueOf(1454).equals(dc.getDistrictId()) &&
                "1A0807".equals(dc.getWardCode())) {
                foundSavedWithGhn = true;
                break;
            }
        }
        assertTrue(foundSavedWithGhn, "Should save the new address with all GHN IDs populated");
    }

    @Test
    void testSubmitCheckout_NewAddress_MissingGhnIds() throws Exception {
        // Missing ghnProvinceId, districtId or wardCode should fail validation
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Nguyễn Văn A")
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "123 Đường Láng")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "") // empty
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã để sử dụng địa chỉ nhận hàng."));
    }

    @Test
    void testResolveGhnAddress_BypassesApiWhenIdsPresent() {
        SoDiaChi addressWithIds = new SoDiaChi();
        addressWithIds.setKhachHang(testKhachHang);
        addressWithIds.setHoNguoiNhan("Tester");
        addressWithIds.setTenNguoiNhan("Bypass");
        addressWithIds.setSdtNguoiNhan("0912345678");
        addressWithIds.setDiaChiCuThe("Some Street");
        addressWithIds.setTinhThanh("InvalidProvinceToTriggerFailureIfFuzzyMatches");
        addressWithIds.setThanhPho("InvalidDistrict");
        addressWithIds.setQuocGia("Việt Nam");
        addressWithIds.setProvinceId(201);
        addressWithIds.setDistrictId(1454);
        addressWithIds.setWardCode("1A0807");

        com.smashvn.shop.service.api.GhnService ghnService = webApplicationContext.getBean(com.smashvn.shop.service.api.GhnService.class);
        com.smashvn.shop.service.api.GhnService.GhnAddressMapping mapping = ghnService.resolveGhnAddress(addressWithIds);

        // Should return the IDs directly without failing due to invalid names
        assertNotNull(mapping);
        assertEquals(201, mapping.getProvinceId());
        assertEquals(1454, mapping.getDistrictId());
        assertEquals("1A0807", mapping.getWardCode());
    }

    @Test
    void testGuestCheckout_DuplicateExistingCustomerPhone() throws Exception {
        // Create an existing active customer with phone 0911222333
        TaiKhoan otherUser = new TaiKhoan();
        otherUser.setUsername("active_user@gmail.com");
        otherUser.setMatKhau("testpass123");
        otherUser.setVaiTro("KH");
        otherUser.setTrangThai("hoat_dong");

        otherUser = taiKhoanRepository.save(otherUser);

        KhachHang otherKh = new KhachHang();
        otherKh.setTaiKhoan(otherUser);
        otherKh.setHoKh("Active");
        otherKh.setTenKh("User");
        otherKh.setSoDienThoaiKh("0911222333");
        khachHangRepository.save(otherKh);

        // Guest checkout with different email but duplicate phone (formatted with symbols)
        mockMvc.perform(post("/checkout/submit")
                .requestAttr("_csrf", csrfToken)
                .param("email", "new_guest@gmail.com")
                .param("hoTenNhan", "Guest User")
                .param("sdtNhan", "+84 911-222.333") // matches normalized 0911222333
                .param("diaChiNhan", "Hà Nội")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Số điện thoại này đã được đăng ký. Vui lòng đăng nhập hoặc sử dụng số điện thoại khác."));
    }

    @Test
    void testPhoneNormalization() {
        assertEquals("0911222333", com.smashvn.shop.util.PhoneUtils.normalize("+84911222333"));
        assertEquals("0911222333", com.smashvn.shop.util.PhoneUtils.normalize("0911 222 333"));
        assertEquals("0911222333", com.smashvn.shop.util.PhoneUtils.normalize("0911-222-333"));
        assertEquals("0911222333", com.smashvn.shop.util.PhoneUtils.normalize("0911.222.333"));
        
        assertTrue(com.smashvn.shop.util.PhoneUtils.isValid("0911222333"));
        assertTrue(com.smashvn.shop.util.PhoneUtils.isValid("0355444555"));
        assertFalse(com.smashvn.shop.util.PhoneUtils.isValid("123456789"));
        assertFalse(com.smashvn.shop.util.PhoneUtils.isValid("0123456789"));
    }

    @Test
    void testSubmitCheckout_RecipientNameTooLongValidation() throws Exception {
        // The controller validates hoTenNhan length (2-100 chars) before reaching the DB layer.
        // A 110-char name exceeds the allowed 100-char max and returns a specific validation message.
        // This also implicitly tests that the ho_va_ten_nguoi_nhan DB column (nvarchar(100))
        // is consistent with the controller validation constraint.
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA") // 110 chars, exceeding the 2-100 char validation constraint
                .param("sdtNhan", "0912345678")
                .param("diaChiNhan", "123 Đường Láng")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807")
                .param("tinhThanhText", "Hà Nội")
                .param("thanhPhoText", "Quận Đống Đa")
                .param("phuongXaText", "Phường Láng Thượng")
                .param("diaChiCuThe", "123 Đường Láng")
                .param("saveAddress", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("loi"))
                .andExpect(jsonPath("$.message").value("Họ và tên người nhận phải từ 2 đến 100 ký tự."));
    }

    @Test
    void testSubmitCheckout_SaveAndReuseAddress() throws Exception {
        // 1. First checkout: Enter new address and check saveAddress=true
        MvcResult result1 = mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("hoTenNhan", "Trần Văn B")
                .param("sdtNhan", "0987654321")
                .param("diaChiNhan", "456 Đường CMT8")
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807")
                .param("tinhThanhText", "Hà Nội")
                .param("thanhPhoText", "Quận Đống Đa")
                .param("phuongXaText", "Phường Láng Thượng")
                .param("diaChiCuThe", "456 Đường CMT8")
                .param("saveAddress", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        // 2. Query the saved address from database and assert it has GHN details saved correctly
        List<SoDiaChi> savedAddresses = soDiaChiRepository.findByKhachHang_Id(testKhachHang.getId());
        SoDiaChi reusedAddress = null;
        for (SoDiaChi dc : savedAddresses) {
            if ("456 Đường CMT8".equals(dc.getDiaChiCuThe())) {
                reusedAddress = dc;
                break;
            }
        }
        assertNotNull(reusedAddress, "Address should be saved to database");
        assertEquals(201, reusedAddress.getProvinceId());
        assertEquals(1454, reusedAddress.getDistrictId());
        assertEquals("1A0807", reusedAddress.getWardCode());

        // 3. Clear GHN IDs from the address in the database to simulate a legacy/unstandardized address
        reusedAddress.setProvinceId(null);
        reusedAddress.setDistrictId(null);
        reusedAddress.setWardCode(null);
        reusedAddress = soDiaChiRepository.save(reusedAddress);

        // 4. Re-add product to cart (since the first checkout cleared the cart)
        mockMvc.perform(post("/gio-hang/them")
                .sessionAttr("idNguoiDung", testUser.getId())
                .sessionAttr("vaiTro", "KH")
                .requestAttr("_csrf", csrfToken)
                .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                .param("soLuong", "1"))
                .andExpect(status().isOk());

        // 5. Second checkout: Reuse this saved address ID and supply the dropdown GHN IDs to auto-heal
        mockMvc.perform(post("/checkout/submit")
                .sessionAttr("idNguoiDung", testUser.getId())
                .requestAttr("_csrf", csrfToken)
                .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                .param("phuongThucThanhToan", "COD")
                .param("idDiaChiLuu", String.valueOf(reusedAddress.getId()))
                .param("ghnProvinceId", "201")
                .param("ghnToDistrictId", "1454")
                .param("ghnToWardCode", "1A0807"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"));

        // 6. Verify that the address in the database was auto-healed (GHN IDs are restored/populated)
        SoDiaChi healedAddress = soDiaChiRepository.findById(reusedAddress.getId()).orElse(null);
        assertNotNull(healedAddress);
        assertEquals(201, healedAddress.getProvinceId(), "Province ID should be auto-healed");
        assertEquals(1454, healedAddress.getDistrictId(), "District ID should be auto-healed");
        assertEquals("1A0807", healedAddress.getWardCode(), "Ward Code should be auto-healed");
    }
}
