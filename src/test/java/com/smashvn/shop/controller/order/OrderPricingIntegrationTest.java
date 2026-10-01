package com.smashvn.shop.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.dao.DotGiamGiaDAO;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.admin.AdminPosService;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.product.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class OrderPricingIntegrationTest {

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
    private DotGiamGiaDAO dotGiamGiaDAO;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private TokenKhoiPhucRepository tokenRepository;

    @Autowired
    private EditLogRepository editLogRepository;

    @Autowired
    private GioHangChiTietRepository gioHangChiTietRepository;

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private AdminPosService adminPosService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private TaiKhoan testUser;
    private KhachHang testKhachHang;
    private SanPhamChiTiet testSpct;
    private DonViVanChuyen testDvvc;
    private DotGiamGia testDgg;
    private CsrfToken csrfToken;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<Integer> orderIdsToClean = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final List<Integer> adminUserIdsToClean = new java.util.concurrent.CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        orderIdsToClean.clear();
        adminUserIdsToClean.clear();
        // Clean up any stray temp test users/customers from previous interrupted runs
        try {
            List<TaiKhoan> strayUsers = taiKhoanRepository.findAll().stream()
                    .filter(tk -> tk.getUsername() != null && (tk.getUsername().startsWith("temp_r_") || tk.getUsername().contains("tester_pricing_")))
                    .toList();
            for (TaiKhoan tk : strayUsers) {
                KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
                if (kh != null) {
                    // Delete orders
                    List<HoaDon> orders = hoaDonRepository.findByKhachHang_Id(kh.getId());
                    for (HoaDon hd : orders) {
                        paymentTransactionRepository.deleteAll(paymentTransactionRepository.findByOrder_Id(hd.getId()));
                        hoaDonChiTietRepository.deleteAll(hoaDonChiTietRepository.findByHoaDon_Id(hd.getId()));
                        hoaDonRepository.delete(hd);
                    }
                    // Delete cart
                    GioHang gh = gioHangRepository.findByKhachHang_Id(kh.getId());
                    if (gh != null) {
                        gioHangChiTietRepository.deleteAll(gioHangChiTietRepository.findByGioHang_Id(gh.getId()));
                        gioHangRepository.delete(gh);
                    }
                    khachHangRepository.delete(kh);
                }
                // Delete account-owned records first to avoid FK constraints
                deleteEditLogsForAccount(tk.getId());
                tokenRepository.deleteAll(tokenRepository.findByTaiKhoan_Id(tk.getId()));
                taiKhoanRepository.delete(tk);
            }
        } catch (Exception e) {
            System.err.println("Failed to clean up stray temp users in setUp: " + e.getMessage());
        }

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");

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
        testUser.setUsername("tester_pricing_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com");
        testUser.setMatKhau("testpass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");

        testUser = taiKhoanRepository.save(testUser);

        testKhachHang = new KhachHang();
        testKhachHang.setTaiKhoan(testUser);
        testKhachHang.setHoKh("Pricing");
        testKhachHang.setTenKh("Tester");
        testKhachHang.setSoDienThoaiKh("0912123456");
        testKhachHang = khachHangRepository.save(testKhachHang);

        // Seed basic metadata
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc d = new DanhMuc();
            d.setTenDanhMuc("Vợt Cầu Lông");
            return danhMucRepository.save(d);
        });

        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu t = new ThuongHieu();
            t.setTenThuongHieu("Yonex");
            return thuongHieuRepository.save(t);
        });

        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan staffTk = new TaiKhoan();
            staffTk.setUsername("staff_pricing_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com");
            staffTk.setMatKhau("testpass123");
            staffTk.setVaiTro("NV");
            staffTk.setTrangThai("hoat_dong");

            staffTk = taiKhoanRepository.save(staffTk);

            NhanVien n = new NhanVien();
            n.setTaiKhoan(staffTk);
            n.setHoTenNv("Staff Pricing");
            n.setChucVu("Staff");
            n.setSoDienThoaiNv("0981112223");
            return nhanVienRepository.save(n);
        });

        // Seed product
        SanPham sp = new SanPham();
        sp.setTenSanPham("Yonex Astrox 88D Play");
        sp.setTrangThai("dang_ban");
        sp.setMoTa("Vợt cầu lông Yonex Astrox");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp = sanPhamRepository.save(sp);

        testSpct = new SanPhamChiTiet();
        testSpct.setSanPham(sp);
        testSpct.setMauSac("Camel Gold");
        testSpct.setTrongLuong("4U");
        testSpct.setMucCang("26 lbs");
        testSpct.setSoLuongTon(100);
        testSpct.setGiaBan(new BigDecimal("3500000")); // Original list price
        testSpct = sanPhamChiTietRepository.save(testSpct);

        // Seed active campaign DotGiamGia (10% off)
        testDgg = new DotGiamGia();
        testDgg.setTenChienDich("Học Sinh Sinh Viên");
        testDgg.setPhanTramGiam(10);
        testDgg.setNgayBatDau(LocalDateTime.now().minusDays(1));
        testDgg.setNgayKetThuc(LocalDateTime.now().plusDays(2));
        testDgg.setActive(true);
        testDgg.setLoaiGiamGia("Giảm phần trăm");
        testDgg.setNhanVien(nv);
        testDgg.setSanPhams(new HashSet<>(Collections.singletonList(sp)));
        testDgg = dotGiamGiaDAO.save(testDgg);

        sp.setCacDotGiamGia(new HashSet<>(Collections.singletonList(testDgg)));
        sp = sanPhamRepository.save(sp);

        // Ensure carrier
        testDvvc = donViVanChuyenDAO.findAll().stream()
                .filter(c -> {
                    if (c.getTenDonVi() == null) return false;
                    String name = c.getTenDonVi().toLowerCase();
                    return !name.contains("ghn") && 
                           !name.contains("giao hàng nhanh") && 
                           !name.contains("quầy") && 
                           !name.contains("quay") && 
                           !name.contains("chỗ") && 
                           !name.contains("cho") && 
                           !name.contains("mua") && 
                           !name.contains("tại") && 
                           !name.contains("tai");
                })
                .findFirst()
                .orElseGet(() -> {
                    DonViVanChuyen d = new DonViVanChuyen();
                    d.setTenDonVi("Giao Hàng Tiết Kiệm");
                    d.setHotline("18006092");
                    return donViVanChuyenDAO.save(d);
                });

        // Ensure payment method
        if (phuongThucThanhToanDAO.findAll().isEmpty()) {
            PhuongThucThanhToan pttt = new PhuongThucThanhToan();
            pttt.setTenPhuongThuc("COD");
            phuongThucThanhToanDAO.save(pttt);
        }
    }

    @Test
    public void testHistoricalPricingAndVariantSnapshotIntegrity() throws Exception {
        // Step 1: Add product to cart and checkout (purchase price should be 3,150,000)
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult checkoutResult = mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Người nhận test")
                        .param("sdtNhan", "0912123456")
                        .param("diaChiNhan", "123 Đường Láng, Hà Nội")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1454")
                        .param("ghnToWardCode", "21012")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("ok"))
                .andReturn();

        String responseStr = checkoutResult.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(responseStr, Map.class);
        Integer orderId = (Integer) respMap.get("orderId");
        assertNotNull(orderId);
        orderIdsToClean.add(orderId);

        // Step 2: Update product variant details and campaign details in the database (simulate admin changing catalog later)
        SanPhamChiTiet spctDb = sanPhamChiTietRepository.findById(testSpct.getId()).orElseThrow();
        spctDb.setGiaBan(new BigDecimal("4000000")); // Updated list price
        spctDb.setMauSac("Ruby Red"); // Updated variant color
        sanPhamChiTietRepository.save(spctDb);

        SanPham spDb = spctDb.getSanPham();
        spDb.setTenSanPham("Yonex Astrox 88D Play PRO"); // Updated product name
        sanPhamRepository.save(spDb);

        // Deactivate campaign
        testDgg.setActive(false);
        dotGiamGiaDAO.save(testDgg);

        // Step 3: Fetch order detail JSON and verify historical fields are intact
        TaiKhoan testAdmin = new TaiKhoan();
        testAdmin.setUsername("admin_pricing_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com");
        testAdmin.setMatKhau("testpass123");
        testAdmin.setVaiTro("QL");
        testAdmin.setTrangThai("hoat_dong");

        testAdmin = taiKhoanRepository.save(testAdmin);
        adminUserIdsToClean.add(testAdmin.getId());

        org.springframework.security.core.context.SecurityContext securityContext = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken securityAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                testAdmin.getUsername(),
                null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_QL"))
        );
        securityContext.setAuthentication(securityAuth);

        MvcResult detailResult = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/don-hang/detail-json")
                        .sessionAttr("idNguoiDung", testAdmin.getId())
                        .sessionAttr("vaiTro", "QL")
                        .sessionAttr(org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext)
                        .param("id", String.valueOf(orderId)))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> detailMap = objectMapper.readValue(detailResult.getResponse().getContentAsString(), Map.class);
        List<Map<String, Object>> items = (List<Map<String, Object>>) detailMap.get("items");
        assertEquals(1, items.size());

        Map<String, Object> itemMap = items.get(0);
        // Verify pricing snapshots
        assertEquals(0, new BigDecimal("3500000").compareTo(new BigDecimal(itemMap.get("giaNiemYet").toString())));
        assertEquals(0, new BigDecimal("3150000").compareTo(new BigDecimal(itemMap.get("giaBan").toString())));
        assertEquals(0, new BigDecimal("10.00").compareTo(new BigDecimal(itemMap.get("phanTramGiam").toString())));
        assertEquals(0, new BigDecimal("350000.00").compareTo(new BigDecimal(itemMap.get("soTienGiamSanPham").toString())));
        assertEquals("Học Sinh Sinh Viên", itemMap.get("tenDotGiamGia"));

        // Verify product & variant snapshots
        assertEquals("Yonex Astrox 88D Play", itemMap.get("tenSanPham"));
        assertTrue(itemMap.get("thuocTinh").toString().contains("Camel Gold"));
        assertTrue(itemMap.get("sku").toString().startsWith("SKU-"));
    }

    @Test
    public void testPricingConsistencyAcrossOnlineAndPosChannels() throws Exception {
        // Online order flow
        mockMvc.perform(post("/gio-hang/them")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("idSanPhamChiTiet", String.valueOf(testSpct.getId()))
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult onlineCheckoutResult = mockMvc.perform(post("/checkout/submit")
                        .sessionAttr("idNguoiDung", testUser.getId())
                        .sessionAttr("vaiTro", "KH")
                        .requestAttr("_csrf", csrfToken)
                        .param("hoTenNhan", "Người nhận Online")
                        .param("sdtNhan", "0912123456")
                        .param("diaChiNhan", "Hà Nội")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1454")
                        .param("ghnToWardCode", "21012")
                        .param("idDonViVanChuyen", String.valueOf(testDvvc.getId()))
                        .param("phuongThucThanhToan", "COD"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> onlineMap = objectMapper.readValue(onlineCheckoutResult.getResponse().getContentAsString(), Map.class);
        Integer onlineOrderId = (Integer) onlineMap.get("orderId");
        orderIdsToClean.add(onlineOrderId);

        // POS order flow
        AdminPosService.PosItem posItem = new AdminPosService.PosItem();
        posItem.idSanPhamChiTiet = testSpct.getId();
        posItem.soLuong = 1;

        // Perform POS checkout via service
        HoaDon posOrder = adminPosService.thanhToanPos(
                testKhachHang.getId(),
                null,
                Collections.singletonList(posItem),
                "TIEN_MAT",
                null,
                "Bán tại quầy",
                testUser.getId(),
                "127.0.0.1"
        );
        orderIdsToClean.add(posOrder.getId());

        // Fetch details of both orders
        HoaDonChiTiet onlineDetail = hoaDonChiTietRepository.findByHoaDon_Id(onlineOrderId).get(0);
        HoaDonChiTiet posDetail = hoaDonChiTietRepository.findByHoaDon_Id(posOrder.getId()).get(0);

        // Verify pricing snapshots are identical
        assertEquals(onlineDetail.getGiaGoc(), posDetail.getGiaGoc());
        assertEquals(onlineDetail.getGiaSauGiam(), posDetail.getGiaSauGiam());
        assertEquals(onlineDetail.getDonGia(), posDetail.getDonGia());
        assertEquals(onlineDetail.getTenSanPhamSnapshot(), posDetail.getTenSanPhamSnapshot());
        assertEquals(onlineDetail.getSkuSnapshot(), posDetail.getSkuSnapshot());
    }

    @Test
    public void testPromotionDeactivationRaceCondition() throws Exception {
        // Remove @Transactional from test to allow multi-threaded behavior to persist to DB
        // We will seed and manually clean up at the end.
        
        List<Integer> orderIds = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<Integer> tempUserIds = new java.util.concurrent.CopyOnWriteArrayList<>();
        List<Integer> tempKhachHangIds = new java.util.concurrent.CopyOnWriteArrayList<>();
        
        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Map<String, Object>>> checkoutFutures = new ArrayList<>();

        // 5 checkouts threads
        for (int i = 0; i < 5; i++) {
            final int index = i;
            checkoutFutures.add(executor.submit(() -> {
                latch.await(); // Wait for green light
                try {
                    // Seed cart for this temporary user
                    TaiKhoan tempUser = new TaiKhoan();
                    tempUser.setUsername("temp_r_" + index + "_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com");
                    tempUser.setMatKhau("pass123");
                    tempUser.setVaiTro("KH");
                    tempUser.setTrangThai("hoat_dong");

                    synchronized (this) {
                        tempUser = taiKhoanRepository.save(tempUser);
                        tempUserIds.add(tempUser.getId());
                        KhachHang tempKh = new KhachHang();
                        tempKh.setTaiKhoan(tempUser);
                        tempKh.setHoKh("Temp");
                        tempKh.setTenKh("User" + index);
                        tempKh.setSoDienThoaiKh("0981" + String.format("%06d", index));
                        tempKh = khachHangRepository.save(tempKh);
                        tempKhachHangIds.add(tempKh.getId());
                    }

                    // Add to cart directly using service
                    gioHangService.themVaoGio(tempUser.getId(), testSpct.getId(), 1);

                    // Create order
                    HoaDon hd = gioHangService.createOrder(
                            tempUser.getId(),
                            "Temp User " + index,
                            "0981" + String.format("%06d", index),
                            "Hà Nội",
                            testDvvc.getId(),
                            "COD",
                            null,
                            null, null, null, null, null
                    );
                    orderIds.add(hd.getId());

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("orderId", hd.getId());
                    return result;
                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("error", e.getMessage());
                    return result;
                }
            }));
        }

        // 6th thread deactivates the campaign
        Future<Void> deactivationFuture = executor.submit(() -> {
            latch.await();
            // sleep briefly to interleave
            Thread.sleep(10);
            synchronized (this) {
                testDgg.setActive(false);
                dotGiamGiaDAO.save(testDgg);
            }
            return null;
        });

        try {
            // Trigger start
            latch.countDown();

            // Wait for all to complete
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // Verify results
            for (Future<Map<String, Object>> future : checkoutFutures) {
                Map<String, Object> res = future.get();
                if (Boolean.TRUE.equals(res.get("success"))) {
                    Integer orderId = (Integer) res.get("orderId");
                    HoaDonChiTiet hdct = hoaDonChiTietRepository.findByHoaDon_Id(orderId).get(0);
                    
                    // Assert price is either 3,150,000 (committed before deactivation) or 3,500,000 (committed after deactivation)
                    BigDecimal purchasedPrice = hdct.getDonGia();
                    assertTrue(
                        purchasedPrice.compareTo(new BigDecimal("3150000")) == 0 ||
                        purchasedPrice.compareTo(new BigDecimal("3500000")) == 0,
                        "Purchased price must be either campaign-discounted price or original price, but was: " + purchasedPrice
                    );
                }
            }
        } finally {
            // 1. Delete transactions
            for (Integer orderId : orderIds) {
                try {
                    paymentTransactionRepository.deleteAll(paymentTransactionRepository.findByOrder_Id(orderId));
                } catch (Exception e) {}
            }
            // 2. Delete order details
            for (Integer orderId : orderIds) {
                try {
                    hoaDonChiTietRepository.deleteAll(hoaDonChiTietRepository.findByHoaDon_Id(orderId));
                } catch (Exception e) {}
            }
            // 3. Delete orders
            for (Integer orderId : orderIds) {
                try {
                    hoaDonRepository.deleteById(orderId);
                } catch (Exception e) {}
            }
            // 4. Delete cart details & carts for temp users
            for (Integer userId : tempUserIds) {
                try {
                    KhachHang kh = khachHangRepository.findByTaiKhoan_Id(userId);
                    if (kh != null) {
                        GioHang gh = gioHangRepository.findByKhachHang_Id(kh.getId());
                        if (gh != null) {
                            gioHangChiTietRepository.deleteAll(gioHangChiTietRepository.findByGioHang_Id(gh.getId()));
                            gioHangRepository.deleteById(gh.getId());
                        }
                    }
                } catch (Exception e) {}
            }
            // 5. Delete temp customers & users
            for (Integer khId : tempKhachHangIds) {
                try {
                    khachHangRepository.deleteById(khId);
                } catch (Exception e) {}
            }
            for (Integer userId : tempUserIds) {
                try {
                    deleteEditLogsForAccount(userId);
                    tokenRepository.deleteAll(tokenRepository.findByTaiKhoan_Id(userId));
                    tokenRepository.flush();
                    taiKhoanRepository.deleteById(userId);
                } catch (Exception e) {}
            }
            // 6. Dissociate and delete seeded test objects from setUp() since this test is not transactional
            try {
                if (testDgg != null) {
                    DotGiamGia freshDgg = dotGiamGiaDAO.findById(testDgg.getId()).orElse(null);
                    if (freshDgg != null) {
                        freshDgg.setSanPhams(new HashSet<>());
                        dotGiamGiaDAO.saveAndFlush(freshDgg);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error clearing DotGiamGia association in finally: " + e.getMessage());
            }

            try {
                if (testSpct != null && testSpct.getSanPham() != null) {
                    SanPham sp = testSpct.getSanPham();
                    sp.setCacDotGiamGia(new HashSet<>());
                    sanPhamRepository.saveAndFlush(sp);
                }
            } catch (Exception e) {
                System.err.println("Error clearing SanPham association in finally: " + e.getMessage());
            }

            if (testDgg != null) {
                try {
                    dotGiamGiaDAO.deleteById(testDgg.getId());
                    dotGiamGiaDAO.flush();
                    testDgg = null;
                } catch (Exception e) {
                    System.err.println("Error deleting testDgg in finally: " + e.getMessage());
                }
            }
            if (testSpct != null) {
                try {
                    sanPhamChiTietRepository.deleteById(testSpct.getId());
                    sanPhamChiTietRepository.flush();
                } catch (Exception e) {
                    System.err.println("Error deleting testSpct in finally: " + e.getMessage());
                }
                try {
                    sanPhamRepository.deleteById(testSpct.getSanPham().getId());
                    sanPhamRepository.flush();
                    testSpct = null;
                } catch (Exception e) {
                    System.err.println("Error deleting SanPham in finally: " + e.getMessage());
                }
            }
            if (testKhachHang != null) {
                try {
                    khachHangRepository.deleteById(testKhachHang.getId());
                    khachHangRepository.flush();
                    testKhachHang = null;
                } catch (Exception e) {
                    System.err.println("Error deleting testKhachHang in finally: " + e.getMessage());
                }
            }
            if (testUser != null) {
                try {
                    deleteEditLogsForAccount(testUser.getId());
                    tokenRepository.deleteAll(tokenRepository.findByTaiKhoan_Id(testUser.getId()));
                    tokenRepository.flush();
                    taiKhoanRepository.deleteById(testUser.getId());
                    taiKhoanRepository.flush();
                    testUser = null;
                } catch (Exception e) {
                    System.err.println("Error deleting testUser in finally: " + e.getMessage());
                }
            }
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Clean up any test-specific orders created in the tests
        for (Integer orderId : orderIdsToClean) {
            try {
                jdbcTemplate.update("DELETE FROM TichHopVanChuyen WHERE id_hoa_don = ?", orderId);
            } catch (Exception e) {}
            try {
                paymentTransactionRepository.deleteAll(paymentTransactionRepository.findByOrder_Id(orderId));
                paymentTransactionRepository.flush();
            } catch (Exception e) {}
            try {
                hoaDonChiTietRepository.deleteAll(hoaDonChiTietRepository.findByHoaDon_Id(orderId));
                hoaDonChiTietRepository.flush();
            } catch (Exception e) {}
            try {
                hoaDonRepository.deleteById(orderId);
                hoaDonRepository.flush();
            } catch (Exception e) {}
        }
        
        // Clean up test admin users
        for (Integer adminId : adminUserIdsToClean) {
            try {
                deleteEditLogsForAccount(adminId);
                tokenRepository.deleteAll(tokenRepository.findByTaiKhoan_Id(adminId));
                tokenRepository.flush();
                taiKhoanRepository.deleteById(adminId);
                taiKhoanRepository.flush();
            } catch (Exception e) {}
        }

        try {
            if (testDgg != null) {
                DotGiamGia freshDgg = dotGiamGiaDAO.findById(testDgg.getId()).orElse(null);
                if (freshDgg != null) {
                    freshDgg.setSanPhams(new HashSet<>());
                    dotGiamGiaDAO.saveAndFlush(freshDgg);
                }
            }
        } catch (Exception e) {
            System.err.println("Error clearing DotGiamGia association in tearDown: " + e.getMessage());
        }

        try {
            if (testSpct != null && testSpct.getSanPham() != null) {
                SanPham sp = testSpct.getSanPham();
                sp.setCacDotGiamGia(new HashSet<>());
                sanPhamRepository.saveAndFlush(sp);
            }
        } catch (Exception e) {
            System.err.println("Error clearing SanPham association in tearDown: " + e.getMessage());
        }

        try {
            if (testDgg != null) {
                dotGiamGiaDAO.deleteById(testDgg.getId());
                dotGiamGiaDAO.flush();
            }
        } catch (Exception e) {
            System.err.println("Error deleting testDgg in tearDown: " + e.getMessage());
        }
        try {
            if (testSpct != null) {
                sanPhamChiTietRepository.deleteById(testSpct.getId());
                sanPhamChiTietRepository.flush();
            }
        } catch (Exception e) {
            System.err.println("Error deleting testSpct in tearDown: " + e.getMessage());
        }
        try {
            if (testSpct != null && testSpct.getSanPham() != null) {
                sanPhamRepository.deleteById(testSpct.getSanPham().getId());
                sanPhamRepository.flush();
            }
        } catch (Exception e) {
            System.err.println("Error deleting SanPham in tearDown: " + e.getMessage());
        }
        try {
            if (testKhachHang != null) {
                // Delete cart first
                GioHang gh = gioHangRepository.findByKhachHang_Id(testKhachHang.getId());
                if (gh != null) {
                    gioHangChiTietRepository.deleteAll(gioHangChiTietRepository.findByGioHang_Id(gh.getId()));
                    gioHangRepository.delete(gh);
                    gioHangRepository.flush();
                }
                khachHangRepository.deleteById(testKhachHang.getId());
                khachHangRepository.flush();
            }
        } catch (Exception e) {
            System.err.println("Error deleting testKhachHang in tearDown: " + e.getMessage());
        }
        try {
            if (testUser != null) {
                // Delete account-owned records first to avoid FK constraints
                deleteEditLogsForAccount(testUser.getId());
                tokenRepository.deleteAll(tokenRepository.findByTaiKhoan_Id(testUser.getId()));
                tokenRepository.flush();
                taiKhoanRepository.deleteById(testUser.getId());
                taiKhoanRepository.flush();
            }
        } catch (Exception e) {
            System.err.println("Error deleting testUser in tearDown: " + e.getMessage());
        }
        try {
            List<TaiKhoan> strayStaff = taiKhoanRepository.findAll().stream()
                    .filter(tk -> tk.getUsername() != null && tk.getUsername().startsWith("staff_pricing_"))
                    .toList();
            for (TaiKhoan tk : strayStaff) {
                NhanVien nv = nhanVienRepository.findByTaiKhoanId(tk.getId());
                if (nv != null) {
                    nhanVienRepository.delete(nv);
                    nhanVienRepository.flush();
                }
                deleteEditLogsForAccount(tk.getId());
                tokenRepository.deleteAll(tokenRepository.findByTaiKhoan_Id(tk.getId()));
                tokenRepository.flush();
                taiKhoanRepository.delete(tk);
                taiKhoanRepository.flush();
            }
        } catch (Exception e) {
            System.err.println("Error deleting strayStaff in tearDown: " + e.getMessage());
        }

        try {
            List<TaiKhoan> strayPricingUsers = taiKhoanRepository.findAll().stream()
                    .filter(tk -> tk.getUsername() != null && tk.getUsername().contains("tester_pricing_"))
                    .toList();
            for (TaiKhoan tk : strayPricingUsers) {
                KhachHang kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
                if (kh != null) {
                    GioHang gh = gioHangRepository.findByKhachHang_Id(kh.getId());
                    if (gh != null) {
                        gioHangChiTietRepository.deleteAll(gioHangChiTietRepository.findByGioHang_Id(gh.getId()));
                        gioHangRepository.delete(gh);
                        gioHangRepository.flush();
                    }
                    List<HoaDon> orders = hoaDonRepository.findByKhachHang_Id(kh.getId());
                    for (HoaDon hd : orders) {
                        try {
                            jdbcTemplate.update("DELETE FROM TichHopVanChuyen WHERE id_hoa_don = ?", hd.getId());
                        } catch (Exception e) {}
                        paymentTransactionRepository.deleteAll(paymentTransactionRepository.findByOrder_Id(hd.getId()));
                        hoaDonChiTietRepository.deleteAll(hoaDonChiTietRepository.findByHoaDon_Id(hd.getId()));
                        hoaDonRepository.delete(hd);
                    }
                    khachHangRepository.delete(kh);
                    khachHangRepository.flush();
                }
                deleteEditLogsForAccount(tk.getId());
                tokenRepository.deleteAll(tokenRepository.findByTaiKhoan_Id(tk.getId()));
                tokenRepository.flush();
                taiKhoanRepository.delete(tk);
                taiKhoanRepository.flush();
            }
        } catch (Exception e) {
            System.err.println("Error deleting strayPricingUsers in tearDown: " + e.getMessage());
        }
    }

    private void deleteEditLogsForAccount(Integer accountId) {
        if (accountId == null) {
            return;
        }
        editLogRepository.deleteAll(editLogRepository.findByTaiKhoan_Id(accountId));
        editLogRepository.flush();
    }
}

