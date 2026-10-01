package com.smashvn.shop.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.api.GhnService;
import com.smashvn.shop.service.order.GioHangService;
import com.smashvn.shop.service.order.OrderViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class OrderLifecycleRegressionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private OrderViewService orderViewService;

    @Autowired
    private GioHangService gioHangService;

    @MockitoBean
    private GhnService ghnService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DefaultCsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "test-token-123");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        when(ghnService.resolveGhnAddress(any())).thenAnswer(invocation -> {
            SoDiaChi dc = invocation.getArgument(0);
            if (dc == null) return null;
            return new GhnService.GhnAddressMapping(
                    dc.getProvinceId() != null ? dc.getProvinceId() : 201,
                    dc.getDistrictId() != null ? dc.getDistrictId() : 1442,
                    dc.getWardCode() != null ? dc.getWardCode() : "20101"
            );
        });
        try {
            when(ghnService.createShippingOrder(any(), any(), any(), any())).thenReturn("GHN-TEST-ORDER-123");
        } catch (Exception ignored) {
        }
    }

    private String createTestGuestOrderToken(MockHttpSession session, String itemIds) throws Exception {
        // Add items to guest cart
        for (String idStr : itemIds.split(",")) {
            mockMvc.perform(post("/gio-hang/them")
                            .session(session)
                            .param("idSanPhamChiTiet", idStr.trim())
                            .param("soLuong", "1"))
                    .andExpect(status().isOk());
        }

        MvcResult startResult = mockMvc.perform(post("/checkout/start")
                        .session(session)
                        .param("selectedItemIds", itemIds))
                .andExpect(status().isOk())
                .andReturn();

        Map resp = objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class);
        return (String) resp.get("checkoutToken");
    }

    private Integer submitTestOrder(MockHttpSession session, String token, String phone, String email) throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/checkout/submit")
                        .session(session)
                        .param("checkoutToken", token)
                        .param("hoTenNhan", "Nguyen Van OrderTest")
                        .param("sdtNhan", phone)
                        .param("email", email)
                        .param("diaChiNhan", "123 Le Loi, Quan 1, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101")
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andReturn();

        Map respMap = objectMapper.readValue(submitResult.getResponse().getContentAsString(), Map.class);
        assertEquals("ok", respMap.get("trangThai"), "Order submission failed: " + respMap.get("message"));
        return (Integer) respMap.get("orderId");
    }

    @Test
    @DisplayName("Order Lifecycle Test 1: Order Creation Snapshot & Amount Integrity")
    void testOrderCreationSnapshotAndTotals() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = createTestGuestOrderToken(session, "25");
        String uniquePhone = "09" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        String uniqueEmail = "ordersnap" + System.currentTimeMillis() + "@smashvn.com";

        Integer orderId = submitTestOrder(session, token, uniquePhone, uniqueEmail);
        assertNotNull(orderId, "Order ID must not be null");

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals("Nguyen Van OrderTest", hd.getTenNguoiNhan());
        assertEquals(uniquePhone, hd.getSdtNhan());
        assertEquals(uniqueEmail, hd.getEmailNguoiNhan());
        assertEquals("cho_xac_nhan", hd.getTrangThaiDonHang());
        assertNotNull(hd.getTrangThaiThanhToan(), "Payment status must be populated");

        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(orderId);
        assertFalse(items.isEmpty(), "Order items must exist");

        HoaDonChiTiet item = items.get(0);
        assertNotNull(item.getTenSanPhamSnapshot(), "Product name snapshot must be recorded");
        assertNotNull(item.getDonGia(), "Unit price snapshot must be recorded");
        assertTrue(item.getDonGia().compareTo(BigDecimal.ZERO) > 0, "Unit price must be positive");
    }

    @Test
    @DisplayName("Order Lifecycle Test 2: Multi-Item Order Separation (No Variant Merging)")
    void testMultiItemOrderNoVariantMerging() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = createTestGuestOrderToken(session, "25,26");
        String uniquePhone = "09" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        String uniqueEmail = "multiitem" + System.currentTimeMillis() + "@smashvn.com";

        Integer orderId = submitTestOrder(session, token, uniquePhone, uniqueEmail);
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();

        List<HoaDonChiTiet> items = hoaDonChiTietRepository.findByHoaDon_Id(orderId);
        assertEquals(2, items.size(), "Order must contain exactly 2 distinct variant line items");
    }

    @Test
    @DisplayName("Order Lifecycle Test 3: Customer Order List Access Control")
    void testCustomerOrderListAccessControl() throws Exception {
        // Unauthenticated access
        mockMvc.perform(get("/user/my-order"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/dang-nhap"));

        // Authenticated customer access
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("idNguoiDung", kh.getTaiKhoan().getId());

        mockMvc.perform(get("/user/my-order").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    @DisplayName("Order Lifecycle Test 4: Customer Order Detail Ownership & IDOR Protection")
    void testCustomerOrderDetailOwnershipAndIDOR() throws Exception {
        List<KhachHang> customers = khachHangRepository.findAll();
        assertTrue(customers.size() >= 2, "Requires at least 2 customer accounts for IDOR test");

        KhachHang customerA = customers.get(0);
        KhachHang customerB = customers.get(1);

        // Create order for customer A
        MockHttpSession sessionA = new MockHttpSession();
        sessionA.setAttribute("idNguoiDung", customerA.getTaiKhoan().getId());
        String tokenA = createTestGuestOrderToken(sessionA, "25");
        String phoneA = customerA.getSoDienThoaiKh() != null ? customerA.getSoDienThoaiKh() : "0912345678";
        String emailA = customerA.getTaiKhoan().getUsername();

        Integer orderIdA = submitTestOrder(sessionA, tokenA, phoneA, emailA);

        // Customer A views own order -> 200 OK
        mockMvc.perform(get("/user/manage-order/" + orderIdA).requestAttr("_csrf", csrfToken).session(sessionA))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("order"));

        // Customer B attempts to view Customer A's order -> Controlled Redirect (NO HTTP 500!)
        MockHttpSession sessionB = new MockHttpSession();
        sessionB.setAttribute("idNguoiDung", customerB.getTaiKhoan().getId());

        mockMvc.perform(get("/user/manage-order/" + orderIdA).session(sessionB))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/my-order?loi=donhangkhongton"));
    }

    @Test
    @DisplayName("Order Lifecycle Test 5: Admin Authorization & Role Security")
    void testAdminAuthorizationAndSecurity() throws Exception {
        // Anonymous -> Redirect to login
        mockMvc.perform(get("/admin/don-hang"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dang-nhap"));

        // Customer session -> Redirect to login
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("idNguoiDung", kh.getTaiKhoan().getId());

        mockMvc.perform(get("/admin/don-hang").session(customerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dang-nhap"));

        // Admin session -> 200 OK
        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("idNguoiDung", adminTk.getId());
        adminSession.setAttribute("vaiTro", adminTk.getVaiTro());

        mockMvc.perform(get("/admin/don-hang").requestAttr("_csrf", csrfToken).session(adminSession))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Order Lifecycle Test 6: Valid and Invalid Status Transitions")
    void testValidAndInvalidStatusTransitions() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = createTestGuestOrderToken(session, "25");
        String uniquePhone = "09" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        String uniqueEmail = "trans" + System.currentTimeMillis() + "@smashvn.com";

        Integer orderId = submitTestOrder(session, token, uniquePhone, uniqueEmail);

        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        // Valid transition: cho_xac_nhan -> da_xac_nhan
        orderViewService.updateOrderStatusByAdmin(orderId, "da_xac_nhan", "cho_xac_nhan", adminTk.getId(), "127.0.0.1");
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals("da_xac_nhan", hd.getTrangThaiDonHang());

        // Invalid transition: da_xac_nhan -> da_giao (skipping intermediate shipping steps)
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(orderId, "da_giao", "da_xac_nhan", adminTk.getId(), "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Order Lifecycle Test 7: Customer Cancel & Stock Restoration")
    void testCustomerCancelAndStockRestoration() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("idNguoiDung", kh.getTaiKhoan().getId());

        SanPhamChiTiet spctBefore = sanPhamChiTietRepository.findById(25).orElseThrow();
        int initialStock = spctBefore.getSoLuongTon();

        String token = createTestGuestOrderToken(customerSession, "25");
        String phone = kh.getSoDienThoaiKh() != null ? kh.getSoDienThoaiKh() : "0912345678";
        String email = kh.getTaiKhoan().getUsername();

        Integer orderId = submitTestOrder(customerSession, token, phone, email);

        // Verify stock deducted by 1
        SanPhamChiTiet spctAfterOrder = sanPhamChiTietRepository.findById(25).orElseThrow();
        assertEquals(initialStock - 1, spctAfterOrder.getSoLuongTon(), "Stock must be deducted by order quantity (1)");

        // Customer cancels order
        MvcResult cancelResult = mockMvc.perform(post("/user/manage-order/cancel/" + orderId)
                        .session(customerSession)
                        .param("lyDoHuy", "Doi y khong mua nua"))
                .andExpect(status().isOk())
                .andReturn();

        Map resp = objectMapper.readValue(cancelResult.getResponse().getContentAsString(), Map.class);
        assertEquals(true, resp.get("success"), "Cancel request must succeed");

        // Verify order status updated to da_huy
        HoaDon hdCancelled = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals("da_huy", hdCancelled.getTrangThaiDonHang());

        // Verify stock fully restored to initial amount
        SanPhamChiTiet spctAfterCancel = sanPhamChiTietRepository.findById(25).orElseThrow();
        assertEquals(initialStock, spctAfterCancel.getSoLuongTon(), "Stock must be restored to initial value after cancel");
    }

    @Test
    @DisplayName("Order Lifecycle Test 8: Double Cancel Idempotency Protection")
    void testDoubleCancelProtection() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        MockHttpSession customerSession = new MockHttpSession();
        customerSession.setAttribute("idNguoiDung", kh.getTaiKhoan().getId());

        SanPhamChiTiet spctBefore = sanPhamChiTietRepository.findById(25).orElseThrow();
        int initialStock = spctBefore.getSoLuongTon();

        String token = createTestGuestOrderToken(customerSession, "25");
        String phone = kh.getSoDienThoaiKh() != null ? kh.getSoDienThoaiKh() : "0912345678";
        String email = kh.getTaiKhoan().getUsername();

        Integer orderId = submitTestOrder(customerSession, token, phone, email);

        // Cancel #1
        mockMvc.perform(post("/user/manage-order/cancel/" + orderId)
                        .session(customerSession)
                        .param("lyDoHuy", "Lan 1"))
                .andExpect(status().isOk());

        int stockAfterFirstCancel = sanPhamChiTietRepository.findById(25).orElseThrow().getSoLuongTon();
        assertEquals(initialStock, stockAfterFirstCancel);

        // Cancel #2 (Replay / Double submit)
        MvcResult secondCancelResult = mockMvc.perform(post("/user/manage-order/cancel/" + orderId)
                        .session(customerSession)
                        .param("lyDoHuy", "Lan 2 duplicate"))
                .andExpect(status().isOk())
                .andReturn();

        Map resp2 = objectMapper.readValue(secondCancelResult.getResponse().getContentAsString(), Map.class);
        assertEquals(false, resp2.get("success"), "Replay cancel on already cancelled order must be rejected");

        // Verify stock was NOT restored a second time
        int stockAfterSecondCancel = sanPhamChiTietRepository.findById(25).orElseThrow().getSoLuongTon();
        assertEquals(initialStock, stockAfterSecondCancel, "Stock must NOT be restored twice");
    }

    @Test
    @DisplayName("Order Lifecycle Test 9: Price Snapshot Immutability")
    void testPriceSnapshotImmutability() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = createTestGuestOrderToken(session, "25");
        String uniquePhone = "09" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        String uniqueEmail = "priceimmut" + System.currentTimeMillis() + "@smashvn.com";

        Integer orderId = submitTestOrder(session, token, uniquePhone, uniqueEmail);
        HoaDonChiTiet item = hoaDonChiTietRepository.findByHoaDon_Id(orderId).get(0);
        BigDecimal originalSnapshotPrice = item.getDonGia();

        // Mutate catalog SPCT price in DB
        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(25).orElseThrow();
        BigDecimal originalSpctPrice = spct.getGiaBan();
        spct.setGiaBan(new BigDecimal("9999999"));
        sanPhamChiTietRepository.save(spct);

        // Re-read order detail from DB -> snapshot price must remain unchanged
        HoaDonChiTiet itemAfterCatalogPriceChange = hoaDonChiTietRepository.findByHoaDon_Id(orderId).get(0);
        assertEquals(0, originalSnapshotPrice.compareTo(itemAfterCatalogPriceChange.getDonGia()),
                "Order item snapshot price must remain immutable despite catalog price changes");

        // Rollback catalog price mutation
        spct.setGiaBan(originalSpctPrice);
        sanPhamChiTietRepository.save(spct);
    }

    @Test
    @DisplayName("Order Lifecycle Test 10: GHN Status Mapping & Stale Update Protection")
    void testGhnStatusMappingAndStaleUpdateProtection() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String token = createTestGuestOrderToken(session, "25");
        String uniquePhone = "09" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        String uniqueEmail = "ghnstale" + System.currentTimeMillis() + "@smashvn.com";

        Integer orderId = submitTestOrder(session, token, uniquePhone, uniqueEmail);

        // Transition order to da_giao (Delivered)
        orderViewService.applyShippingStatus(orderId, "da_giao", "delivered");
        HoaDon hdDelivered = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals("da_giao", hdDelivered.getTrangThaiDonHang());
        assertNotNull(hdDelivered.getPaidAt(), "PaidAt timestamp must be set on delivered");

        // Simulate stale GHN status update (e.g. delivering) after delivered
        orderViewService.applyShippingStatus(orderId, "dang_giao", "delivering");

        // Verify status remains da_giao and is NOT rolled back by stale webhook
        HoaDon hdStaleCheck = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals("da_giao", hdStaleCheck.getTrangThaiDonHang(),
                "Stale/out-of-order GHN updates must NOT roll back delivered status");
    }
}
