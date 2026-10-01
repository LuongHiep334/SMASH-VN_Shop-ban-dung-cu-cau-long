package com.smashvn.shop.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.api.GhnService;
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
import java.time.LocalDateTime;
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
public class ReturnExchangeRefundRegressionTest {

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
    private EditLogRepository editLogRepository;

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
            when(ghnService.createReturnShippingOrder(any(), any())).thenReturn("GHN-RETURN-TEST-999");
        } catch (Exception ignored) {
        }
    }

    private Integer createAndDeliverTestOrder(KhachHang kh, String itemIds) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", kh.getTaiKhoan().getId());

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
        String token = (String) resp.get("checkoutToken");

        String phone = kh.getSoDienThoaiKh() != null ? kh.getSoDienThoaiKh() : "09" + String.valueOf(System.currentTimeMillis()).substring(3, 11);
        String email = kh.getTaiKhoan().getUsername();

        MvcResult submitResult = mockMvc.perform(post("/checkout/submit")
                        .session(session)
                        .param("checkoutToken", token)
                        .param("hoTenNhan", kh.getHoTenKh() != null ? kh.getHoTenKh() : "Customer ReturnTest")
                        .param("sdtNhan", phone)
                        .param("email", email)
                        .param("diaChiNhan", "123 Le Loi, Quan 1, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101")
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk())
                .andReturn();

        Map submitMap = objectMapper.readValue(submitResult.getResponse().getContentAsString(), Map.class);
        Integer orderId = (Integer) submitMap.get("orderId");

        // Transition order to da_giao (Delivered)
        orderViewService.applyShippingStatus(orderId, "da_giao", "delivered");

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        hd.setThoiGianXacNhan(LocalDateTime.now());
        hoaDonRepository.save(hd);

        return orderId;
    }

    @Test
    @DisplayName("Return Test 1: Delivered Order Eligibility")
    void testDeliveredOrderEligibility() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("idNguoiDung", kh.getTaiKhoan().getId());

        // Create order in cho_xac_nhan status
        mockMvc.perform(post("/gio-hang/them").session(session).param("idSanPhamChiTiet", "25").param("soLuong", "1")).andExpect(status().isOk());
        MvcResult startResult = mockMvc.perform(post("/checkout/start").session(session).param("selectedItemIds", "25")).andExpect(status().isOk()).andReturn();
        String token = (String) objectMapper.readValue(startResult.getResponse().getContentAsString(), Map.class).get("checkoutToken");

        MvcResult submitResult = mockMvc.perform(post("/checkout/submit")
                        .session(session).param("checkoutToken", token)
                        .param("hoTenNhan", "Test User").param("sdtNhan", "0912345678").param("email", kh.getTaiKhoan().getUsername())
                        .param("diaChiNhan", "123 Street, Quan 1, TP HCM")
                        .param("ghnProvinceId", "201")
                        .param("ghnToDistrictId", "1442")
                        .param("ghnToWardCode", "20101")
                        .param("phuongThucThanhToan", "COD"))
                .andExpect(status().isOk()).andReturn();
        Integer unconfirmedOrderId = (Integer) objectMapper.readValue(submitResult.getResponse().getContentAsString(), Map.class).get("orderId");
        assertNotNull(unconfirmedOrderId, "unconfirmedOrderId must not be null");
        HoaDon unconfirmedHd = hoaDonRepository.findById(unconfirmedOrderId).orElseThrow();
        unconfirmedHd.setKhachHang(kh);
        hoaDonRepository.save(unconfirmedHd);

        // Attempt return request on non-delivered order -> Fails
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.yeuCauTraHang(unconfirmedOrderId, kh.getId(), "TRA", "San pham khong nhu mo ta", null, "127.0.0.1");
        });

        // Delivered order -> Succeeds
        Integer deliveredOrderId = createAndDeliverTestOrder(kh, "25");
        boolean success = orderViewService.yeuCauTraHang(deliveredOrderId, kh.getId(), "TRA", "San pham bi loi", null, "127.0.0.1");
        assertTrue(success, "Return request on delivered order must succeed");

        HoaDon hd = hoaDonRepository.findById(deliveredOrderId).orElseThrow();
        assertEquals(ReturnStatus.PENDING_APPROVAL, hd.getTrangThaiHoanHang());
    }

    @Test
    @DisplayName("Return Test 2: 7-Day Return Window Boundary Test")
    void testSevenDayReturnWindowBoundary() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();

        // 1. Delivered 6 days ago -> Allowed
        editLogRepository.deleteAll(editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", orderId));

        EditLog log6DaysAgo = new EditLog();
        log6DaysAgo.setTenBang("HoaDon");
        log6DaysAgo.setIdBanGhi(orderId);
        log6DaysAgo.setHanhDong("UPDATE");
        log6DaysAgo.setGiaTriMoi("status=da_giao");
        log6DaysAgo.setThoiGian(LocalDateTime.now().minusDays(6));
        log6DaysAgo.setVaiTroThucHien("SYSTEM");
        editLogRepository.save(log6DaysAgo);
        hd.setThoiGianXacNhan(LocalDateTime.now().minusDays(6));
        hoaDonRepository.save(hd);

        assertTrue(orderViewService.isWithinReturnWindow(hd), "Delivered 6 days ago must be within 7-day return window");

        // 2. Delivered 8 days ago -> Expired / Rejected
        editLogRepository.deleteAll(editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", orderId));

        EditLog log8DaysAgo = new EditLog();
        log8DaysAgo.setTenBang("HoaDon");
        log8DaysAgo.setIdBanGhi(orderId);
        log8DaysAgo.setHanhDong("UPDATE");
        log8DaysAgo.setGiaTriMoi("status=da_giao");
        log8DaysAgo.setThoiGian(LocalDateTime.now().minusDays(8));
        log8DaysAgo.setVaiTroThucHien("SYSTEM");
        editLogRepository.save(log8DaysAgo);
        hd.setThoiGianXacNhan(LocalDateTime.now().minusDays(8));
        hoaDonRepository.save(hd);

        assertFalse(orderViewService.isWithinReturnWindow(hd), "Delivered 8 days ago must be EXPIRED outside 7-day return window");

        assertThrows(IllegalStateException.class, () -> {
            orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Too late return request", null, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Return Test 3: Request Ownership & IDOR Protection")
    void testRequestOwnershipAndIDOR() throws Exception {
        List<KhachHang> customers = khachHangRepository.findAll();
        assertTrue(customers.size() >= 2, "Requires 2 customer accounts");

        KhachHang customerA = customers.get(0);
        KhachHang customerB = customers.get(1);

        Integer orderIdA = createAndDeliverTestOrder(customerA, "25");

        // Customer B attempts to submit return request for Customer A's order -> IDOR Rejected cleanly
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.yeuCauTraHang(orderIdA, customerB.getId(), "TRA", "IDOR attack attempt", null, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Return Test 4: Return Type & Reason Validation")
    void testReturnTypeAndReasonValidation() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");

        // Missing / blank reason -> Exception
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "   ", null, "127.0.0.1");
        });

        // Invalid return type -> Exception
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.yeuCauTraHang(orderId, kh.getId(), "INVALID_TYPE", "San pham loi", null, "127.0.0.1");
        });

        // Valid EXCHANGE request
        boolean successExchange = orderViewService.yeuCauTraHang(orderId, kh.getId(), "DOI", "Doi sang size khac", null, "127.0.0.1");
        assertTrue(successExchange);

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals("DOI", hd.getLoaiYeuCauDoiTra());
        assertEquals("Doi sang size khac", hd.getLyDoHoanTra());
    }

    @Test
    @DisplayName("Return Test 5: Evidence File Paths Serialization")
    void testEvidencePathsSerialization() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");

        List<String> evidencePaths = List.of("/uploads/returns/evidence1.jpg", "/uploads/returns/evidence2.mp4");
        boolean success = orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Hang bi vo vo hop", evidencePaths, "127.0.0.1");
        assertTrue(success);

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertNotNull(hd.getBangChungHoanTra());
        assertTrue(hd.getBangChungHoanTra().contains("/uploads/returns/evidence1.jpg"));
        assertTrue(hd.getBangChungHoanTra().contains("/uploads/returns/evidence2.mp4"));
    }

    @Test
    @DisplayName("Return Test 6: Duplicate Return Request Prevention")
    void testDuplicateReturnRequestPrevention() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");

        // Request #1 -> Success
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Lan 1", null, "127.0.0.1");

        // Request #2 (Replay / Duplicate) -> Exception
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Lan 2 duplicate", null, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Return Test 7: Admin Approve Return & GHN Waybill Creation")
    void testAdminApproveReturnAndGhnWaybill() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Muon tra hang", null, "127.0.0.1");

        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        // Admin approves return
        String ghnCode = orderViewService.duyetYeuCauTraHangVaTaoDonGhn(orderId, adminTk.getId(), "127.0.0.1");
        assertNotNull(ghnCode, "GHN return code must be generated");

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnStatus.WAITING_FOR_PICKUP, hd.getTrangThaiHoanHang());
        assertNotNull(hd.getGhnReturnOrderCode());
    }

    @Test
    @DisplayName("Return Test 8: Admin Reject Return & Reason Preservation")
    void testAdminRejectReturn() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Ly do khach hang", null, "127.0.0.1");

        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        // Admin rejects return request
        orderViewService.tuChoiYeuCauTraHang(orderId, "Khong du dieu kien tra hang theo quy dinh", adminTk.getId(), "127.0.0.1");

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnStatus.REJECTED, hd.getTrangThaiHoanHang());
        // Verify customer reason was NOT overwritten by admin rejection reason
        assertEquals("Ly do khach hang", hd.getLyDoHoanTra());
    }

    @Test
    @DisplayName("Return Test 9: GHN Status Mapping & Stale Return Status Protection")
    void testGhnReturnStatusMappingAndStaleProtection() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Tra hang", null, "127.0.0.1");

        // Transition to DELIVERED_TO_SHOP
        orderViewService.updateReturnStatusFromGhn(orderId, ReturnStatus.DELIVERED_TO_SHOP, "returned", "GHN_WEBHOOK");
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnStatus.DELIVERED_TO_SHOP, hd.getTrangThaiHoanHang());

        // Further transition to RETURNED (inspected and restocked)
        hd.setTrangThaiHoanHang(ReturnStatus.RETURNED);
        hoaDonRepository.save(hd);

        // Simulate stale GHN webhook sending picked_up
        orderViewService.updateReturnStatusFromGhn(orderId, ReturnStatus.PICKED_UP, "picked", "GHN_WEBHOOK");

        // Verify status remains RETURNED (stale update ignored for terminal/inspected state)
        HoaDon hdStaleCheck = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnStatus.RETURNED, hdStaleCheck.getTrangThaiHoanHang(), "Terminal return state must be protected against stale webhooks");
    }

    @Test
    @DisplayName("Return Test 10: Normal Restock Resolution (BAN_LAI)")
    void testNormalRestockResolution() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        SanPhamChiTiet spctBefore = sanPhamChiTietRepository.findById(25).orElseThrow();
        int initialNormalStock = spctBefore.getSoLuongTon();
        int initialDefectiveStock = spctBefore.getSoLuongSpLoi() != null ? spctBefore.getSoLuongSpLoi() : 0;

        Integer orderId = createAndDeliverTestOrder(kh, "25");
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Tra hang con nguyen tem", null, "127.0.0.1");

        // Set state to DELIVERED_TO_SHOP
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        hd.setTrangThaiHoanHang(ReturnStatus.DELIVERED_TO_SHOP);
        hoaDonRepository.save(hd);

        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        // Admin confirms restock as sellable BAN_LAI
        orderViewService.xacNhanKiemKhoVaNhapKho(orderId, "BAN_LAI", null, adminTk.getId(), "127.0.0.1");

        HoaDon hdRestocked = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnInventoryStatus.DA_HOAN_KHO, hdRestocked.getTrangThaiXuLyHangHoan());
        assertEquals(ReturnStatus.RETURNED, hdRestocked.getTrangThaiHoanHang());

        // Verify normal stock increased by 1, defective stock unchanged
        SanPhamChiTiet spctAfter = sanPhamChiTietRepository.findById(25).orElseThrow();
        assertEquals(initialNormalStock, spctAfter.getSoLuongTon(), "Normal stock restored to initial after order & restock");
        assertEquals(initialDefectiveStock, spctAfter.getSoLuongSpLoi() != null ? spctAfter.getSoLuongSpLoi() : 0, "Defective stock must be unchanged");
    }

    @Test
    @DisplayName("Return Test 11: Defective Stock Resolution (HANG_LOI)")
    void testDefectiveStockResolution() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        SanPhamChiTiet spctBefore = sanPhamChiTietRepository.findById(25).orElseThrow();
        int initialNormalStock = spctBefore.getSoLuongTon();
        int initialDefectiveStock = spctBefore.getSoLuongSpLoi() != null ? spctBefore.getSoLuongSpLoi() : 0;

        Integer orderId = createAndDeliverTestOrder(kh, "25");
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Tra hang bi loi san xuat", null, "127.0.0.1");

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        hd.setTrangThaiHoanHang(ReturnStatus.DELIVERED_TO_SHOP);
        hoaDonRepository.save(hd);

        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        // Admin confirms inspection as HANG_LOI
        orderViewService.xacNhanKiemKhoVaNhapKho(orderId, "HANG_LOI", null, adminTk.getId(), "127.0.0.1");

        HoaDon hdDefective = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, hdDefective.getTrangThaiXuLyHangHoan());
        assertEquals(ReturnStatus.RETURNED, hdDefective.getTrangThaiHoanHang());

        // Verify normal stock NOT restored, defective stock incremented by 1
        SanPhamChiTiet spctAfter = sanPhamChiTietRepository.findById(25).orElseThrow();
        assertEquals(initialNormalStock - 1, spctAfter.getSoLuongTon(), "Normal stock must NOT be restored for defective goods");
        assertEquals(initialDefectiveStock + 1, spctAfter.getSoLuongSpLoi(), "Defective stock so_luong_sp_loi must increase by 1");
    }

    @Test
    @DisplayName("Return Test 12: Double Restock Protection")
    void testDoubleRestockProtection() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Tra hang", null, "127.0.0.1");

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        hd.setTrangThaiHoanHang(ReturnStatus.DELIVERED_TO_SHOP);
        hoaDonRepository.save(hd);

        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        // Restock #1 -> Success
        orderViewService.xacNhanKiemKhoVaNhapKho(orderId, "BAN_LAI", null, adminTk.getId(), "127.0.0.1");

        int stockAfterFirstRestock = sanPhamChiTietRepository.findById(25).orElseThrow().getSoLuongTon();

        // Restock #2 (Replay) -> Exception
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.xacNhanKiemKhoVaNhapKho(orderId, "BAN_LAI", null, adminTk.getId(), "127.0.0.1");
        });

        int stockAfterSecondRestock = sanPhamChiTietRepository.findById(25).orElseThrow().getSoLuongTon();
        assertEquals(stockAfterFirstRestock, stockAfterSecondRestock, "Stock must NOT be restored a second time");
    }

    @Test
    @DisplayName("Return Test 13: Refund Eligibility & Amount Server Authority")
    void testRefundEligibilityAndAmountServerAuthority() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Tra hang", null, "127.0.0.1");

        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        // Refund attempt before inspection (state is PENDING_APPROVAL) -> Fails
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.xacNhanHoanTienChoKhach(orderId, "CHUYEN_KHOAN", new BigDecimal("100000"), "TX123", "Ghi chu", "/uploads/refunds/proof.png", adminTk.getId(), "127.0.0.1");
        });

        // Set to RETURNED and DA_HOAN_KHO
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        hd.setTrangThaiHoanHang(ReturnStatus.RETURNED);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.DA_HOAN_KHO);
        hoaDonRepository.save(hd);

        // Refund attempt without proof image -> IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.xacNhanHoanTienChoKhach(orderId, "CHUYEN_KHOAN", hd.getTongTien(), "TX123", "Ghi chu", null, adminTk.getId(), "127.0.0.1");
        });

        // Refund attempt with arbitrary huge amount (> total order amount) -> Exception
        BigDecimal hugeAmount = hd.getTongTien().add(new BigDecimal("999999999"));
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.xacNhanHoanTienChoKhach(orderId, "CHUYEN_KHOAN", hugeAmount, "TX123", "Ghi chu", "/uploads/refunds/proof.png", adminTk.getId(), "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Return Test 14: Confirm Refund & Double Refund Idempotency Protection")
    void testConfirmRefundAndDoubleRefundProtection() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Tra hang nhap kho", null, "127.0.0.1");

        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();
        hd.setTrangThaiHoanHang(ReturnStatus.RETURNED);
        hd.setTrangThaiXuLyHangHoan(ReturnInventoryStatus.DA_HOAN_KHO);
        hoaDonRepository.save(hd);

        TaiKhoan adminTk = taiKhoanRepository.findAll().stream()
                .filter(t -> "QL".equals(t.getVaiTro()) || "NV".equals(t.getVaiTro()))
                .findFirst().orElseThrow();

        // Confirm Refund #1
        orderViewService.xacNhanHoanTienChoKhach(orderId, "CHUYEN_KHOAN", hd.getTongTien(), "REFUND-TX-001", "Hoan tien thanh cong", "/uploads/refunds/proof.png", adminTk.getId(), "127.0.0.1");

        HoaDon hdRefunded = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnStatus.REFUNDED, hdRefunded.getTrangThaiHoanHang());
        assertEquals("DA_HOAN_TIEN", hdRefunded.getTrangThaiThanhToan());
        assertEquals(RefundStatus.COMPLETED, hdRefunded.getRefundStatus());

        // Confirm Refund #2 (Replay / Double Refund) -> Protected safely via double refund guard / reconcile
        orderViewService.xacNhanHoanTienChoKhach(orderId, "CHUYEN_KHOAN", hd.getTongTien(), "REFUND-TX-001", "Hoan tien duplicate", "/uploads/refunds/proof.png", adminTk.getId(), "127.0.0.1");

        HoaDon hdRefundedSecond = hoaDonRepository.findById(orderId).orElseThrow();
        assertEquals(ReturnStatus.REFUNDED, hdRefundedSecond.getTrangThaiHoanHang());
        assertEquals(RefundStatus.COMPLETED, hdRefundedSecond.getRefundStatus());
    }

    @Test
    @DisplayName("Return Test 15: Return Window Uses Delivery Time NOT Payment Time")
    void testReturnWindowUsesDeliveryNotPaymentTime() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();

        // Payment 10 days ago, Delivery 4 days ago
        hd.setNgayThanhToan(LocalDateTime.now().minusDays(10));
        hd.setPaidAt(LocalDateTime.now().minusDays(10));
        hd.setThoiGianXacNhan(LocalDateTime.now().minusDays(10));
        hoaDonRepository.save(hd);

        // Replace EditLog with Delivery 4 days ago
        editLogRepository.deleteAll(editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", orderId));

        EditLog log4DaysAgo = new EditLog();
        log4DaysAgo.setTenBang("HoaDon");
        log4DaysAgo.setIdBanGhi(orderId);
        log4DaysAgo.setHanhDong("UPDATE");
        log4DaysAgo.setGiaTriMoi("status=da_giao");
        log4DaysAgo.setThoiGian(LocalDateTime.now().minusDays(4));
        log4DaysAgo.setVaiTroThucHien("SYSTEM");
        editLogRepository.save(log4DaysAgo);

        assertTrue(orderViewService.isWithinReturnWindow(hd), "Must be eligible because delivery was 4 days ago (even though payment was 10 days ago)");

        boolean success = orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Fit problem", null, "127.0.0.1");
        assertTrue(success, "Return request must succeed based strictly on delivery timestamp");
    }

    @Test
    @DisplayName("Return Test 16: Missing Delivery Record Rejection")
    void testMissingDeliveryRecordRejection() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();

        hd.setNgayThanhToan(LocalDateTime.now().minusDays(2));
        hd.setThoiGianXacNhan(LocalDateTime.now().minusDays(2));
        hoaDonRepository.save(hd);

        // Clear EditLog (no delivery transition record exists)
        editLogRepository.deleteAll(editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", orderId));

        assertFalse(orderViewService.isWithinReturnWindow(hd), "Must return false when delivery transition log is missing (never silently use payment time)");

        assertThrows(IllegalStateException.class, () -> {
            orderViewService.yeuCauTraHang(orderId, kh.getId(), "TRA", "Missing delivery timestamp test", null, "127.0.0.1");
        });
    }

    @Test
    @DisplayName("Return Test 17: Repeated Polling Does Not Overwrite Delivered Timestamp")
    void testRepeatedPollingDoesNotOverwriteDeliveredAt() throws Exception {
        KhachHang kh = khachHangRepository.findAll().stream().findFirst().orElseThrow();
        Integer orderId = createAndDeliverTestOrder(kh, "25");
        HoaDon hd = hoaDonRepository.findById(orderId).orElseThrow();

        // Clear EditLog and insert initial delivery transition (5 days ago) and subsequent polling transition (now)
        editLogRepository.deleteAll(editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", orderId));

        LocalDateTime initialDeliveryTime = LocalDateTime.now().minusDays(5);
        EditLog log5DaysAgo = new EditLog();
        log5DaysAgo.setTenBang("HoaDon");
        log5DaysAgo.setIdBanGhi(orderId);
        log5DaysAgo.setHanhDong("UPDATE");
        log5DaysAgo.setGiaTriMoi("status=da_giao");
        log5DaysAgo.setThoiGian(initialDeliveryTime);
        log5DaysAgo.setVaiTroThucHien("SYSTEM");
        editLogRepository.save(log5DaysAgo);

        // Simulate repeated GHN polling 1 hour later
        EditLog pollingLog = new EditLog();
        pollingLog.setTenBang("HoaDon");
        pollingLog.setIdBanGhi(orderId);
        pollingLog.setHanhDong("UPDATE");
        pollingLog.setGiaTriMoi("status=da_giao");
        pollingLog.setThoiGian(LocalDateTime.now());
        pollingLog.setVaiTroThucHien("SYSTEM");
        editLogRepository.save(pollingLog);

        LocalDateTime computedDelivery = orderViewService.getDeliveredTimestamp(hd);
        assertNotNull(computedDelivery, "Computed delivery timestamp must not be null");
        assertEquals(initialDeliveryTime.toLocalDate(), computedDelivery.toLocalDate(), "Delivered timestamp must NOT be overwritten on status polling replay");
    }
}
