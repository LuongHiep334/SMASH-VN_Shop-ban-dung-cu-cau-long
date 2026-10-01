package com.smashvn.shop.service;

import com.smashvn.shop.config.GhnConfig;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.exception.GhnCreateIndeterminateException;
import com.smashvn.shop.exception.GhnSandboxLimitationException;
import com.smashvn.shop.exception.GhnUnsupportedRouteException;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.api.GhnService;
import com.smashvn.shop.service.api.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class GhnDecouplingAndConcurrencyTest {

    @Autowired
    private GhnService ghnService;

    @Autowired
    private GhnConfig ghnConfig;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private com.smashvn.shop.service.order.OrderViewService orderViewService;

    @Autowired
    private com.smashvn.shop.service.api.GhnShipmentPersistenceService ghnShipmentPersistenceService;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager oauth2AuthorizedClientManager;

    @MockitoBean(name = "filterChain")
    private org.springframework.security.web.SecurityFilterChain securityFilterChain;

    private final List<Integer> createdHoaDonIds = new CopyOnWriteArrayList<>();

    private KhachHang testKh;
    private PhuongThucThanhToan testPt;
    private SanPhamChiTiet testSpct;

    @BeforeEach
    void setUp() {
        testKh = khachHangRepository.findAll().stream().findFirst().orElse(null);
        testPt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
        testSpct = sanPhamChiTietRepository.findAll().stream().findFirst().orElse(null);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        if (!createdHoaDonIds.isEmpty()) {
            for (Integer id : createdHoaDonIds) {
                try {
                    jdbcTemplate.update("DELETE FROM GiaoDichThanhToan WHERE id_hoa_don = ?", id);
                    jdbcTemplate.update("DELETE FROM TichHopVanChuyen WHERE id_hoa_don = ?", id);
                    jdbcTemplate.update("DELETE FROM HoaDonChiTiet WHERE id_hoa_don = ?", id);
                    jdbcTemplate.update("DELETE FROM EditLog WHERE ten_bang = 'HoaDon' AND id_ban_ghi = ?", id);
                    jdbcTemplate.update("DELETE FROM HoaDon WHERE id = ?", id);
                } catch (Exception ignored) {
                }
            }
            createdHoaDonIds.clear();
        }
    }

    @Test
    @DisplayName("Test 1: Sandbox classification logic for supported demo fallbacks and internal errors")
    void testSandboxClassification() {
        assertTrue(ghnService.isSandboxEnvironment(), "Default config should be sandbox environment");

        // GhnUnsupportedRouteException is eligible for sandbox fallback
        assertTrue(ghnService.isEligibleForSandboxFallback(new GhnUnsupportedRouteException("GHN chưa hỗ trợ tuyến")));
        assertTrue(ghnService.isEligibleForSandboxFallback(
                new GhnSandboxLimitationException("Lỗi hệ thống - không lấy được thông tin kho")));

        // GhnCreateIndeterminateException is NOT eligible for sandbox fallback
        assertFalse(ghnService.isEligibleForSandboxFallback(new GhnCreateIndeterminateException("Timeout POST create")));

        // Internal exceptions are NOT eligible for fallback
        assertFalse(ghnService.isEligibleForSandboxFallback(new IllegalArgumentException("Null address")));
        assertFalse(ghnService.isEligibleForSandboxFallback(new NullPointerException("Null pointer")));
        assertFalse(ghnService.isEligibleForSandboxFallback(new IllegalStateException("Bad state")));

        // HTTP 401/403 credentials error is NOT eligible for fallback
        assertFalse(ghnService.isEligibleForSandboxFallback(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Unauthorized")));
        assertFalse(ghnService.isEligibleForSandboxFallback(new HttpClientErrorException(HttpStatus.FORBIDDEN, "Forbidden")));

        // HTTP 500 is an outage, not proof that the recipient route is unsupported
        assertFalse(ghnService.isEligibleForSandboxFallback(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error")));
    }

    @Test
    @DisplayName("Test 2: Fixed striped locks prevent duplicate GHN order creation across 3 concurrent threads")
    void testConcurrentGhnOrderCreationAcrossThreeThreads() throws Exception {
        assertNotNull(testKh, "KhachHang test fixture must exist in DB");
        assertNotNull(testPt, "PhuongThucThanhToan test fixture must exist in DB");

        // Verify striped lock count is 256
        assertEquals(256, ghnService.getLockStripeCount(), "Fixed lock stripe count must be 256");

        // Create a test HoaDon
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setPhuongThucThanhToan(testPt);
        hd.setMaDonHang("TEST-CONCURRENCY-3T-" + System.currentTimeMillis());
        hd.setTongTien(new BigDecimal("500000"));
        hd.setTongTienHang(new BigDecimal("500000"));
        hd.setTrangThaiDonHang(OrderStatus.SAN_SANG_GIAO.getValue());
        hd.setPaymentMethod("COD");
        hd.setTenNguoiNhan("Nguyen Concurrency 3T Test");
        hd.setSdtNhan("0987654321");
        hd.setDiaChiNhan("123 Le Loi, Ben Nghe, Quan 1, TP HCM");
        hd.setGhnToDistrictId(1442);
        hd.setGhnToWardCode("20101");
        hd = hoaDonRepository.save(hd);
        createdHoaDonIds.add(hd.getId());

        // Prepare dummy items
        List<HoaDonChiTiet> items = new ArrayList<>();
        HoaDonChiTiet item = new HoaDonChiTiet();
        item.setHoaDon(hd);
        item.setSanPhamChiTiet(testSpct);
        item.setDonGia(new BigDecimal("500000"));
        item.setSoLuong(1);
        items.add(item);

        // Mock restTemplate for available-services and create order
        String availableServicesJson = "{\"code\":200,\"data\":[{\"service_id\":53320,\"service_type_id\":2}]}";
        String createOrderJson = "{\"code\":200,\"data\":{\"order_code\":\"GHN-CONCURRENT-OK-999\"}}";

        AtomicInteger createCallCount = new AtomicInteger(0);

        when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(availableServicesJson, HttpStatus.OK));

        when(restTemplate.postForEntity(contains("shipping-order/create"), any(), eq(String.class)))
                .thenAnswer(invocation -> {
                    createCallCount.incrementAndGet();
                    Thread.sleep(150); // Simulate network latency
                    return new org.springframework.http.ResponseEntity<>(createOrderJson, HttpStatus.OK);
                });

        // 3 threads: Thread 1 runs create, Thread 2 waits on lock, Thread 3 arrives as Thread 1 finishes
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch t1FinishedLatch = new CountDownLatch(1);
        CountDownLatch allFinishLatch = new CountDownLatch(3);
        List<String> results = new CopyOnWriteArrayList<>();

        final HoaDon targetHd = hd;

        // Thread 1
        executor.submit(() -> {
            try {
                startLatch.await();
                String code = ghnService.createShippingOrderOrThrow(targetHd, items, 1442, "20101");
                if (code != null) results.add(code);
            } catch (Exception e) {
                results.add("ERROR-T1: " + e.getMessage());
            } finally {
                t1FinishedLatch.countDown();
                allFinishLatch.countDown();
            }
        });

        // Thread 2 (queued simultaneously, waits for stripe lock)
        executor.submit(() -> {
            try {
                startLatch.await();
                String code = ghnService.createShippingOrderOrThrow(targetHd, items, 1442, "20101");
                if (code != null) results.add(code);
            } catch (Exception e) {
                results.add("ERROR-T2: " + e.getMessage());
            } finally {
                allFinishLatch.countDown();
            }
        });

        // Thread 3 (starts right after Thread 1 completes to verify no race after lock release)
        executor.submit(() -> {
            try {
                startLatch.await();
                t1FinishedLatch.await(); // waits for Thread 1 to finish
                String code = ghnService.createShippingOrderOrThrow(targetHd, items, 1442, "20101");
                if (code != null) results.add(code);
            } catch (Exception e) {
                results.add("ERROR-T3: " + e.getMessage());
            } finally {
                allFinishLatch.countDown();
            }
        });

        startLatch.countDown();
        allFinishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(3, results.size(), "All 3 concurrent calls should complete");
        assertEquals("GHN-CONCURRENT-OK-999", results.get(0));
        assertEquals("GHN-CONCURRENT-OK-999", results.get(1));
        assertEquals("GHN-CONCURRENT-OK-999", results.get(2));
        assertEquals(1, createCallCount.get(), "GHN POST create API should only be called once across all 3 threads due to fixed striped locks");
    }

    @Test
    @DisplayName("Test 3: Indeterminate POST create timeout marks GHN_CREATE_UNKNOWN and blocks blind retry")
    void testIndeterminatePostCreateTimeoutAndBlindRetryBlock() {
        assertNotNull(testKh, "KhachHang test fixture must exist in DB");
        assertNotNull(testPt, "PhuongThucThanhToan test fixture must exist in DB");

        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setPhuongThucThanhToan(testPt);
        hd.setMaDonHang("TEST-TIMEOUT-" + System.currentTimeMillis());
        hd.setTongTien(new BigDecimal("300000"));
        hd.setTongTienHang(new BigDecimal("300000"));
        hd.setTrangThaiDonHang(OrderStatus.SAN_SANG_GIAO.getValue());
        hd.setPaymentMethod("COD");
        hd.setTenNguoiNhan("Nguyen Timeout Test");
        hd.setSdtNhan("0987654322");
        hd.setDiaChiNhan("456 Nguyen Trai, Quan 5, TP HCM");
        hd.setGhnToDistrictId(1443);
        hd.setGhnToWardCode("20102");
        hd = hoaDonRepository.save(hd);
        createdHoaDonIds.add(hd.getId());

        List<HoaDonChiTiet> items = new ArrayList<>();
        HoaDonChiTiet item = new HoaDonChiTiet();
        item.setHoaDon(hd);
        item.setSanPhamChiTiet(testSpct);
        item.setDonGia(new BigDecimal("300000"));
        item.setSoLuong(1);
        items.add(item);

        String availableServicesJson = "{\"code\":200,\"data\":[{\"service_id\":53320,\"service_type_id\":2}]}";
        when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(availableServicesJson, HttpStatus.OK));

        AtomicInteger createCallCount = new AtomicInteger(0);

        // Simulate ResourceAccessException (timeout) during POST create on attempt 1
        when(restTemplate.postForEntity(contains("shipping-order/create"), any(), eq(String.class)))
                .thenAnswer(invocation -> {
                    createCallCount.incrementAndGet();
                    throw new ResourceAccessException("Read timed out");
                });

        // Detail by client code returns empty array or not found
        when(restTemplate.postForEntity(contains("detail-by-client-code"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>("{\"code\":200,\"data\":[]}", HttpStatus.OK));

        final HoaDon targetHd = hd;

        // Attempt 1: Throws GhnCreateIndeterminateException
        assertThrows(GhnCreateIndeterminateException.class, () -> {
            ghnService.createShippingOrderOrThrow(targetHd, items, 1443, "20102");
        });

        assertEquals(1, createCallCount.get(), "First attempt should call POST create once");

        // Verify GHN_CREATE_UNKNOWN was recorded
        assertTrue(ghnService.hasUnknownGhnCreateStatus(hd.getId()), "Order must be marked as GHN_CREATE_UNKNOWN in DB");

        // Attempt 2: Blind Push / Retry without forceRetry -> MUST BE BLOCKED, NOT calling POST create again
        assertThrows(GhnCreateIndeterminateException.class, () -> {
            ghnService.createShippingOrderOrThrow(targetHd, items, 1443, "20102", false);
        });

        assertEquals(1, createCallCount.get(), "Second blind retry MUST NOT call POST /shipping-order/create again!");

        // Verify lock map is clean
        assertEquals(256, ghnService.getLockStripeCount());
    }

    @Test
    @DisplayName("Test 4: Reconcile finds existing order in GHN Array response without calling POST create again")
    void testReconcileResolvesOrderAfterTimeout() throws Exception {
        assertNotNull(testKh, "KhachHang test fixture must exist in DB");
        assertNotNull(testPt, "PhuongThucThanhToan test fixture must exist in DB");

        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setPhuongThucThanhToan(testPt);
        hd.setMaDonHang("TEST-RECONCILE-" + System.currentTimeMillis());
        hd.setTongTien(new BigDecimal("400000"));
        hd.setTongTienHang(new BigDecimal("400000"));
        hd.setTrangThaiDonHang(OrderStatus.SAN_SANG_GIAO.getValue());
        hd.setPaymentMethod("COD");
        hd.setTenNguoiNhan("Nguyen Reconcile Test");
        hd.setSdtNhan("0987654323");
        hd.setDiaChiNhan("789 Vo Van Tan, Quan 3, TP HCM");
        hd.setGhnToDistrictId(1444);
        hd.setGhnToWardCode("20103");
        hd = hoaDonRepository.save(hd);
        createdHoaDonIds.add(hd.getId());

        List<HoaDonChiTiet> items = new ArrayList<>();
        HoaDonChiTiet item = new HoaDonChiTiet();
        item.setHoaDon(hd);
        item.setSanPhamChiTiet(testSpct);
        item.setDonGia(new BigDecimal("400000"));
        item.setSoLuong(1);
        items.add(item);

        String availableServicesJson = "{\"code\":200,\"data\":[{\"service_id\":53320,\"service_type_id\":2}]}";
        when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(availableServicesJson, HttpStatus.OK));

        AtomicInteger createCallCount = new AtomicInteger(0);

        // Attempt 1: timeout during POST create
        when(restTemplate.postForEntity(contains("shipping-order/create"), any(), eq(String.class)))
                .thenAnswer(invocation -> {
                    createCallCount.incrementAndGet();
                    throw new ResourceAccessException("Connection timed out");
                });

        final HoaDon targetHd = hd;
        assertThrows(GhnCreateIndeterminateException.class, () -> {
            ghnService.createShippingOrderOrThrow(targetHd, items, 1444, "20103");
        });

        assertEquals(1, createCallCount.get(), "First call attempted create");
        assertTrue(ghnService.hasUnknownGhnCreateStatus(hd.getId()));

        // Now simulate that GHN returned response format with data as JSON ARRAY according to GHN docs
        String detailJson = "{\"code\":200,\"message\":\"Success\",\"data\":[{\"client_order_code\":\"" + hd.getMaDonHang() + "\",\"order_code\":\"GHN-RECONCILED-12345\"}]}";
        when(restTemplate.postForEntity(contains("detail-by-client-code"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(detailJson, HttpStatus.OK));

        // Attempt 2: Reconcile automatically finds the order in the Array without calling POST create
        String resolvedCode = ghnService.createShippingOrderOrThrow(targetHd, items, 1444, "20103", false);

        assertEquals("GHN-RECONCILED-12345", resolvedCode);
        assertEquals(1, createCallCount.get(), "POST /shipping-order/create must NOT be called again when reconciled!");
        assertEquals("GHN-RECONCILED-12345", ghnService.findExistingGhnCode(hd.getId()));
        assertEquals(256, ghnService.getLockStripeCount());
    }

    @Test
    @DisplayName("Test 5: Explicit force retry allows Admin to retry after confirming on GHN portal")
    void testExplicitForceRetryAllowsPostCreate() throws Exception {
        assertNotNull(testKh, "KhachHang test fixture must exist in DB");
        assertNotNull(testPt, "PhuongThucThanhToan test fixture must exist in DB");

        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setPhuongThucThanhToan(testPt);
        hd.setMaDonHang("TEST-FORCE-" + System.currentTimeMillis());
        hd.setTongTien(new BigDecimal("600000"));
        hd.setTongTienHang(new BigDecimal("600000"));
        hd.setTrangThaiDonHang(OrderStatus.SAN_SANG_GIAO.getValue());
        hd.setPaymentMethod("COD");
        hd.setTenNguoiNhan("Nguyen Force Test");
        hd.setSdtNhan("0987654324");
        hd.setDiaChiNhan("101 Tran Hung Dao, Quan 1, TP HCM");
        hd.setGhnToDistrictId(1442);
        hd.setGhnToWardCode("20101");
        hd = hoaDonRepository.save(hd);
        createdHoaDonIds.add(hd.getId());

        List<HoaDonChiTiet> items = new ArrayList<>();
        HoaDonChiTiet item = new HoaDonChiTiet();
        item.setHoaDon(hd);
        item.setSanPhamChiTiet(testSpct);
        item.setDonGia(new BigDecimal("600000"));
        item.setSoLuong(1);
        items.add(item);

        String availableServicesJson = "{\"code\":200,\"data\":[{\"service_id\":53320,\"service_type_id\":2}]}";
        when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(availableServicesJson, HttpStatus.OK));

        AtomicInteger createCallCount = new AtomicInteger(0);

        // Attempt 1: timeout
        when(restTemplate.postForEntity(contains("shipping-order/create"), any(), eq(String.class)))
                .thenAnswer(invocation -> {
                    createCallCount.incrementAndGet();
                    throw new ResourceAccessException("Socket timeout");
                });

        final HoaDon targetHd = hd;
        assertThrows(GhnCreateIndeterminateException.class, () -> {
            ghnService.createShippingOrderOrThrow(targetHd, items, 1442, "20101");
        });

        assertEquals(1, createCallCount.get());

        // Attempt 2 with forceRetry = true: GHN now succeeds and returns code
        String successCreateJson = "{\"code\":200,\"data\":{\"order_code\":\"GHN-FORCE-RETRY-888\"}}";
        when(restTemplate.postForEntity(contains("shipping-order/create"), any(), eq(String.class)))
                .thenAnswer(invocation -> {
                    createCallCount.incrementAndGet();
                    return new org.springframework.http.ResponseEntity<>(successCreateJson, HttpStatus.OK);
                });

        String finalCode = ghnService.createShippingOrderOrThrow(targetHd, items, 1442, "20101", true);

        assertEquals("GHN-FORCE-RETRY-888", finalCode);
        assertEquals(2, createCallCount.get(), "Force retry MUST allow second POST create call");
        assertEquals(256, ghnService.getLockStripeCount());
    }

    @Test
    @DisplayName("Test 6: Parser safety for detail-by-client-code responses (null, empty array, blank order_code)")
    void testParserSafetyForDetailByClientCode() {
        // Case 1: data is null
        when(restTemplate.postForEntity(contains("detail-by-client-code"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>("{\"code\":200,\"data\":null}", HttpStatus.OK));
        assertNull(ghnService.getOrderDetailByClientOrderCode("TEST-NULL-DATA"));

        // Case 2: data is empty array []
        when(restTemplate.postForEntity(contains("detail-by-client-code"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>("{\"code\":200,\"data\":[]}", HttpStatus.OK));
        assertNull(ghnService.getOrderDetailByClientOrderCode("TEST-EMPTY-ARRAY"));

        // Case 3: data is array with blank order_code
        when(restTemplate.postForEntity(contains("detail-by-client-code"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>("{\"code\":200,\"data\":[{\"order_code\":\"   \"}]}", HttpStatus.OK));
        assertNull(ghnService.getOrderDetailByClientOrderCode("TEST-BLANK-CODE"));

        // Case 4: data is array with valid order_code
        when(restTemplate.postForEntity(contains("detail-by-client-code"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>("{\"code\":200,\"data\":[{\"order_code\":\"GHN-VALID-ARRAY\"}]}", HttpStatus.OK));
        Map<String, Object> result = ghnService.getOrderDetailByClientOrderCode("TEST-VALID-ARRAY");
        assertNotNull(result);
        assertEquals("GHN-VALID-ARRAY", result.get("order_code"));
    }

    @Test
    @DisplayName("Test A: Sandbox - RESOLVE_SERVICE throws GhnUnsupportedRouteException -> DEMO-GHN fallback is created")
    void testA_SandboxResolveServiceThrowsUnsupportedRouteFallback() throws Exception {
        HoaDon hd = createDummyHoaDon("TEST-FALLBACK-A-");
        List<HoaDonChiTiet> items = createDummyItems(hd);

        // Mock available-services returning an explicit unsupported-route response body
        when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        org.springframework.http.HttpHeaders.EMPTY,
                        "{\"message\":\"Tuyến đường không hỗ trợ\"}".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8));

        String code = ghnService.createShippingOrderOrThrow(hd, items, 1442, "20101");
        assertNotNull(code);
        assertTrue(code.startsWith("DEMO-GHN-"), "Should generate DEMO-GHN code on Sandbox for unsupported route");
        assertEquals(code, hd.getGhnOrderCode());
        assertEquals("ready_to_pick", hd.getGhnStatus());
        assertEquals(code, ghnService.findExistingGhnCode(hd.getId()));
    }

    @Test
    @DisplayName("Test B: Sandbox - lỗi mạng ở RESOLVE_SERVICE không được che bằng DEMO-GHN")
    void testB_SandboxResolveServiceNetworkErrorDoesNotFallback() {
        HoaDon hd = createDummyHoaDon("TEST-FALLBACK-B-");
        List<HoaDonChiTiet> items = createDummyItems(hd);

        // Mock available-services throwing ResourceAccessException
        when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timeout during available-services"));

        assertThrows(ResourceAccessException.class,
                () -> ghnService.createShippingOrderOrThrow(hd, items, 1442, "20101"));
        assertNull(hd.getGhnOrderCode());
        assertNull(ghnService.findExistingGhnCode(hd.getId()));
    }

    @Test
    @DisplayName("Test C: Sandbox - lỗi lookup shop không được che bằng DEMO-GHN")
    void testC_SandboxResolveShopErrorDoesNotFallback() {
        HoaDon hd = createDummyHoaDon("TEST-FALLBACK-C-");
        List<HoaDonChiTiet> items = createDummyItems(hd);

        // Mock shop/all throwing ResourceAccessException
        when(restTemplate.postForEntity(contains("shop/all"), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timeout during shop lookup"));

        assertThrows(Exception.class,
                () -> ghnService.createShippingOrderOrThrow(hd, items, 1442, "20101"));
        assertNull(hd.getGhnOrderCode());
        assertNull(ghnService.findExistingGhnCode(hd.getId()));
    }

    @Test
    @DisplayName("Test D: Sandbox - POST create throws ResourceAccessException -> GhnCreateIndeterminateException, NO DEMO-GHN")
    void testD_SandboxPostCreateThrowsResourceAccessExceptionThrowsIndeterminate() {
        HoaDon hd = createDummyHoaDon("TEST-FALLBACK-D-");
        List<HoaDonChiTiet> items = createDummyItems(hd);

        // Mock available-services success
        String availableServicesJson = "{\"code\":200,\"data\":[{\"service_id\":53320,\"service_type_id\":2}]}";
        when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(availableServicesJson, HttpStatus.OK));

        // Mock shipping-order/create throwing ResourceAccessException
        when(restTemplate.postForEntity(contains("shipping-order/create"), any(), eq(String.class)))
                .thenThrow(new ResourceAccessException("Read timed out during create order"));

        assertThrows(GhnCreateIndeterminateException.class, () -> {
            ghnService.createShippingOrderOrThrow(hd, items, 1442, "20101");
        });

        // Verify NO DEMO-GHN was created
        assertNull(hd.getGhnOrderCode());
        assertTrue(ghnService.hasUnknownGhnCreateStatus(hd.getId()));
    }

    @Test
    @DisplayName("Test E: Production - unsupported route or network before create throws Exception, NO DEMO-GHN")
    void testE_ProductionUnsupportedRouteThrowsExceptionNoDemoFallback() {
        HoaDon hd = createDummyHoaDon("TEST-FALLBACK-E-");
        List<HoaDonChiTiet> items = createDummyItems(hd);

        // Override baseUrl temporarily to simulate Production
        String origUrl = ghnConfig.getBaseUrl();
        try {
            ghnConfig.setBaseUrl("https://online-gateway.ghn.vn");
            assertFalse(ghnService.isSandboxEnvironment(), "Environment must be production");

            when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Tuyến đường không hỗ trợ"));

            assertThrows(Exception.class, () -> {
                ghnService.createShippingOrderOrThrow(hd, items, 1442, "20101");
            });

            assertNull(hd.getGhnOrderCode(), "Production must NEVER generate DEMO-GHN fallback");
        } finally {
            ghnConfig.setBaseUrl(origUrl);
        }
    }

    @Test
    @DisplayName("Test F: ghnOrderCode is null -> Admin transition to DA_TAO_VAN_DON_GHN is blocked")
    void testF_AdminTransitionToDaTaoVanDonGhnBlockedWhenNoGhnCode() {
        HoaDon hd = createDummyHoaDon("TEST-STATUS-F-");
        hd.setTrangThaiDonHang(OrderStatus.SAN_SANG_GIAO.getValue());
        hd.setGhnOrderCode(null);
        hd = hoaDonRepository.save(hd);

        // getNextStatus must be null
        assertNull(orderViewService.getNextStatus(hd), "Next status must be null when san_sang_giao has no ghnOrderCode");

        final Integer hdId = hd.getId();
        TaiKhoan adminUser = taiKhoanRepository.findAll().stream()
                .filter(tk -> "QL".equals(tk.getVaiTro()) || "NV".equals(tk.getVaiTro()))
                .findFirst().orElse(null);
        if (adminUser != null) {
            final Integer adminId = adminUser.getId();
            assertThrows(IllegalStateException.class, () -> {
                orderViewService.updateOrderStatusByAdmin(hdId, OrderStatus.DA_TAO_VAN_DON_GHN.getValue(), OrderStatus.SAN_SANG_GIAO.getValue(), adminId, "127.0.0.1");
            });
        }
    }

    @Test
    @DisplayName("Test G: mã DEMO-GHN chỉ được chuyển qua Demo Simulator, không qua luồng admin chung")
    void testG_AdminTransitionIsBlockedForDemoCode() {
        HoaDon hd = createDummyHoaDon("TEST-STATUS-G-");
        hd.setTrangThaiDonHang(OrderStatus.SAN_SANG_GIAO.getValue());
        hd = hoaDonRepository.save(hd);

        // Persist demo shipment in TichHopVanChuyen (which backs @Formula ghnOrderCode)
        ghnShipmentPersistenceService.saveShipment(hd.getId(), "DEMO-GHN-20260818-123-9999", "GHN_FALLBACK", "ready_to_pick");

        final Integer hdId = hd.getId();
        TaiKhoan adminUser = taiKhoanRepository.findAll().stream()
                .filter(tk -> "QL".equals(tk.getVaiTro()) || "NV".equals(tk.getVaiTro()))
                .findFirst().orElse(null);
        HoaDon reloaded = hoaDonRepository.findById(hdId).orElseThrow();
        assertNull(orderViewService.getNextStatus(reloaded));
        if (adminUser != null) {
            final Integer adminId = adminUser.getId();
            assertThrows(IllegalArgumentException.class, () ->
                    orderViewService.updateOrderStatusByAdmin(
                            hdId,
                            OrderStatus.DA_TAO_VAN_DON_GHN.getValue(),
                            OrderStatus.SAN_SANG_GIAO.getValue(),
                            adminId,
                            "127.0.0.1"));
        }
    }

    @Test
    @DisplayName("Test H: Sandbox báo không lấy được thông tin kho -> tạo DEMO-GHN sau nhánh thử kho dự phòng")
    void testH_SandboxWarehouseInformationLimitationCreatesDemoFallback() throws Exception {
        HoaDon hd = createDummyHoaDon("TEST-FALLBACK-H-");
        List<HoaDonChiTiet> items = createDummyItems(hd);

        String availableServicesJson = "{\"code\":200,\"data\":[{\"service_id\":53320,\"service_type_id\":2}]}";
        String warehouseLimitationJson = "{\"code\":400,\"message\":\"Lỗi hệ thống - không lấy được thông tin kho\",\"data\":null}";
        when(restTemplate.postForEntity(contains("available-services"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(availableServicesJson, HttpStatus.OK));
        when(restTemplate.postForEntity(contains("shipping-order/create"), any(), eq(String.class)))
                .thenReturn(new org.springframework.http.ResponseEntity<>(warehouseLimitationJson, HttpStatus.OK));

        String code = ghnService.createShippingOrderOrThrow(hd, items, 1442, "20101");

        assertNotNull(code);
        assertTrue(code.startsWith("DEMO-GHN-"));
        assertEquals(code, hd.getGhnOrderCode());
        assertEquals(code, ghnService.findExistingGhnCode(hd.getId()));
    }

    private HoaDon createDummyHoaDon(String prefix) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setPhuongThucThanhToan(testPt);
        hd.setMaDonHang(prefix + System.currentTimeMillis());
        hd.setTongTien(new BigDecimal("500000"));
        hd.setTongTienHang(new BigDecimal("500000"));
        hd.setTrangThaiDonHang(OrderStatus.SAN_SANG_GIAO.getValue());
        hd.setPaymentMethod("COD");
        hd.setTenNguoiNhan("Nguyen Demo Test");
        hd.setSdtNhan("0987654321");
        hd.setDiaChiNhan("123 Le Loi, Ben Nghe, Quan 1, TP HCM");
        hd.setGhnToDistrictId(1442);
        hd.setGhnToWardCode("20101");
        HoaDon saved = hoaDonRepository.save(hd);
        createdHoaDonIds.add(saved.getId());
        return saved;
    }

    private List<HoaDonChiTiet> createDummyItems(HoaDon hd) {
        List<HoaDonChiTiet> items = new ArrayList<>();
        HoaDonChiTiet item = new HoaDonChiTiet();
        item.setHoaDon(hd);
        item.setSanPhamChiTiet(testSpct);
        item.setDonGia(new BigDecimal("500000"));
        item.setSoLuong(1);
        items.add(item);
        return items;
    }
}
