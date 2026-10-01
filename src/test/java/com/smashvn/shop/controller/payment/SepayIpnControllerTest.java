package com.smashvn.shop.controller.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dto.payment.SepayIpnRequest;
import com.smashvn.shop.dto.payment.SepayTransactionDto;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.exception.InvalidPaymentException;
import com.smashvn.shop.exception.OrderNotFoundException;
import com.smashvn.shop.repository.GioHangChiTietRepository;
import com.smashvn.shop.repository.GioHangRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.api.GhnService;
import com.smashvn.shop.service.payment.SepayGatewayService;
import com.smashvn.shop.service.payment.SepayOrderPaymentService;

public class SepayIpnControllerTest {

    @Mock private jakarta.persistence.EntityManager entityManager;


    private MockMvc mockMvc;

    @Mock
    private SepayOrderPaymentService sepayOrderPaymentService;


    @Mock
    private SepayConfig sepayConfig;

    @Mock
    private SepayGatewayService sepayGatewayService;

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private com.smashvn.shop.service.order.GioHangService gioHangService;

    @Mock
    private com.smashvn.shop.service.order.GuestCheckoutService guestCheckoutService;

    @Mock
    private com.smashvn.shop.repository.PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Mock
    private com.smashvn.shop.repository.ThongBaoRepository thongBaoRepository;

    @InjectMocks
    private SepayIpnController sepayIpnController;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Fields for Service Testing
    @Mock
    private HoaDonChiTietRepository hoaDonChiTietRepository;
    @Mock
    private SanPhamChiTietRepository sanPhamChiTietRepository;
    @Mock
    private GioHangRepository gioHangRepository;
    @Mock
    private GioHangChiTietRepository gioHangChiTietRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private GhnService ghnService;

    private SepayGatewayService realService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(sepayIpnController).build();

        // Setup config mocks
        when(sepayConfig.getIpnSecret()).thenReturn("super-secret-ipn-token");
        when(sepayConfig.isIpVerification()).thenReturn(true);
        when(sepayConfig.getIpRanges()).thenReturn("172.236.138.20,172.233.83.68");
        when(sepayConfig.isDebug()).thenReturn(false);
        when(sepayOrderPaymentService.xuLyThanhToanSePay(any(), any(), any(), any())).thenAnswer(invocation -> {
            Integer id = invocation.getArgument(0);
            Optional<HoaDon> hdOpt = hoaDonRepository.findById(id);
            if (hdOpt.isPresent()) {
                HoaDon hd = hdOpt.get();
                hd.setPaymentStatus("paid");
                hd.setTrangThaiDonHang("cho_xac_nhan");
            }
            return true;
        });

        // Delegate findByMaDonHangOrNormalized to findByMaDonHang (check both raw code and normalized code)
        when(hoaDonRepository.findByMaDonHangOrNormalized(anyString(), anyString())).thenAnswer(invocation -> {
            String code1 = invocation.getArgument(0);
            String code2 = invocation.getArgument(1);
            Optional<HoaDon> opt1 = hoaDonRepository.findByMaDonHang(code1);
            if (opt1.isPresent()) return opt1;
            Optional<HoaDon> opt2 = hoaDonRepository.findByMaDonHang(code2);
            if (opt2.isPresent()) return opt2;
            return Optional.empty();
        });

        // Instantiate a real service with mocked repositories for unit-testing the service logic
        realService = new SepayGatewayService(
                sepayConfig,
                hoaDonRepository,
                hoaDonChiTietRepository,
                sanPhamChiTietRepository,
                gioHangRepository,
                gioHangChiTietRepository,
                paymentTransactionRepository,
                auditService,
                ghnService,
                guestCheckoutService,
                phieuGiamGiaRepository,
                thongBaoRepository,
                sepayOrderPaymentService
        );
        org.springframework.test.util.ReflectionTestUtils.setField(realService, "entityManager", entityManager);

    }

    // ==========================================
    // 1. HTTP LAYER & SECURITY TEST CASES
    // ==========================================
    @Test
    void testUnconfiguredIpnSecretRejectsWebhook() throws Exception {
        when(sepayConfig.isIpVerification()).thenReturn(false);
        for (String secret : new String[] {null, "", "   "}) {
            when(sepayConfig.getIpnSecret()).thenReturn(secret);
            mockMvc.perform(post("/api/payment/sepay/ipn")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
                    .header("Authorization", "Apikey supplied-token"))
                    .andExpect(status().isUnauthorized());
        }
        org.mockito.Mockito.verifyNoInteractions(sepayGatewayService);
    }

    @Test
    void testInvalidSignature() throws Exception {
        SepayIpnRequest requestPayload = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH20260608-A1B2C3");
        String payloadStr = objectMapper.writeValueAsString(requestPayload);

        // Request with missing/invalid header should return 401
        mockMvc.perform(post("/api/payment/sepay/ipn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadStr)
                .header("Authorization", "Apikey invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid API credentials."));
    }

    @Test
    void testIpVerificationRejection() throws Exception {
        SepayIpnRequest requestPayload = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH20260608-A1B2C3");
        String payloadStr = objectMapper.writeValueAsString(requestPayload);

        // Send request from an unwhitelisted IP
        mockMvc.perform(post("/api/payment/sepay/ipn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadStr)
                .header("Authorization", "Apikey super-secret-ipn-token")
                .with(request -> {
                    request.setRemoteAddr("198.51.100.42"); // Non-whitelisted IP
                    return request;
                }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testQueryTransaction_Unauthorized() throws Exception {
        HoaDon order = new HoaDon();
        order.setMaDonHang("DH20260608-A1B2C3");
        order.setTrangThaiThanhToan("CHO_THANH_TOAN");
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(999);
        kh.setTaiKhoan(tk);
        order.setKhachHang(kh);

        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/payment/sepay/query/DH20260608-A1B2C3"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testQueryTransaction_Forbidden_OwnershipMismatch() throws Exception {
        // Order belonging to customer id 999
        HoaDon order = new HoaDon();
        order.setMaDonHang("DH20260608-A1B2C3");
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(999);
        kh.setTaiKhoan(tk);
        order.setKhachHang(kh);

        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        // Query with session customer id 123 (mismatch)
        mockMvc.perform(get("/api/payment/sepay/query/DH20260608-A1B2C3")
                .sessionAttr("idNguoiDung", 123))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testQueryTransaction_GuestOrderAllowedWithoutSession() throws Exception {
        HoaDon order = new HoaDon();
        order.setMaDonHang("DH20260608-A1B2C3");
        order.setPaymentStatus("pending");
        order.setTrangThaiDonHang("cho_thanh_toan");
        order.setTrangThaiThanhToan("CHO_THANH_TOAN");

        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/payment/sepay/query/DH20260608-A1B2C3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderCode").value("DH20260608-A1B2C3"))
                .andExpect(jsonPath("$.paymentStatus").value("pending"))
                .andExpect(jsonPath("$.orderStatus").value("cho_thanh_toan"));
    }

    @Test
    void testQueryTransaction_Success() throws Exception {
        HoaDon order = new HoaDon();
        order.setMaDonHang("DH20260608-A1B2C3");
        order.setPaymentStatus("paid");
        order.setTrangThaiDonHang("cho_xac_nhan");
        KhachHang kh = new KhachHang();
        TaiKhoan tk = new TaiKhoan();
        tk.setId(123);
        kh.setTaiKhoan(tk);
        order.setKhachHang(kh);

        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        mockMvc.perform(get("/api/payment/sepay/query/DH20260608-A1B2C3")
                .sessionAttr("idNguoiDung", 123))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.orderCode").value("DH20260608-A1B2C3"))
                .andExpect(jsonPath("$.paymentStatus").value("paid"))
                .andExpect(jsonPath("$.orderStatus").value("cho_xac_nhan"))
                .andExpect(jsonPath("$.id").doesNotExist()); // Security constraint: no internal IDs returned
    }

    // ==========================================
    // 2. SERVICE LOGIC & BUSINESS PROCESS TESTS
    // ==========================================
    @Test
    void testSuccessfulPaymentCallback() throws Exception {
        HoaDon order = createMockOrder("DH20260608-A1B2C3", new BigDecimal("100000"), "pending", "cho_thanh_toan");
        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));
        List<HoaDonChiTiet> items = createMockOrderItems(order, 5); // 5 stock available, 2 ordered
        when(sepayOrderPaymentService.xuLyThanhToanSePay(eq(order.getId()), any(), any(), any())).thenAnswer(inv -> {
            order.setPaymentStatus("paid");
            order.setTrangThaiDonHang("cho_xac_nhan");
            items.get(0).getSanPhamChiTiet().setSoLuongTon(3);
            return true;
        });

        when(hoaDonChiTietRepository.findByHoaDon_Id(order.getId())).thenReturn(items);
        when(sanPhamChiTietRepository.findByIdWithLock(anyInt())).thenReturn(Optional.of(items.get(0).getSanPhamChiTiet()));

        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH20260608-A1B2C3");

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("paid", order.getPaymentStatus());
        assertEquals("cho_xac_nhan", order.getTrangThaiDonHang());
        assertEquals(3, items.get(0).getSanPhamChiTiet().getSoLuongTon()); // Stock IS deducted
    }

    @Test
    void testInvalidAmount() {
        HoaDon order = createMockOrder("DH20260608-A1B2C3", new BigDecimal("100000"), "pending", "cho_thanh_toan");
        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        // Transferred amount (90000) does not match order total (100000)
        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("90000"), "DH20260608-A1B2C3");

        assertThrows(InvalidPaymentException.class, () -> {
            realService.handleIpn(ipn, "{}");
        });
    }

    @Test
    void testDuplicateCallback() throws Exception {
        // Mock existing transaction in repository
        PaymentTransaction existingTx = new PaymentTransaction();
        existingTx.setTransactionId("TX100");
        existingTx.setStatus("success");
        when(paymentTransactionRepository.findByTransactionId("TX100")).thenReturn(Optional.of(existingTx));

        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH20260608-A1B2C3");

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("Already processed", result.get("message"));
        verify(hoaDonRepository, never()).findByMaDonHang(anyString()); // No order loading, skipped fast
    }

    @Test
    void testNonexistentOrder() {
        // Order not found
        when(hoaDonRepository.findByMaDonHang("DH999999-XXXXXX")).thenReturn(Optional.empty());

        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH999999-XXXXXX");

        assertThrows(OrderNotFoundException.class, () -> {
            realService.handleIpn(ipn, "{}");
        });
    }

    @Test
    void testAlreadyPaidOrder() throws Exception {
        HoaDon order = createMockOrder("DH20260608-A1B2C3", new BigDecimal("100000"), "paid", "cho_xac_nhan");
        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH20260608-A1B2C3");

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("Already processed", result.get("message"));
        verify(hoaDonChiTietRepository, never()).findByHoaDon_Id(any()); // No stock updates
    }

    @Test
    void testCallbackAfterOrderCancelled() throws Exception {
        HoaDon order = createMockOrder("DH20260608-A1B2C3", new BigDecimal("100000"), "pending", "da_huy");
        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        List<HoaDonChiTiet> items = createMockOrderItems(order, 5);
        when(hoaDonChiTietRepository.findByHoaDon_Id(order.getId())).thenReturn(items);

        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH20260608-A1B2C3");

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("paid_received_after_cancel", order.getPaymentStatus());
        assertEquals("da_huy", order.getTrangThaiDonHang()); // Stays cancelled
        verify(auditService, times(1)).log(
                eq(null), eq("HoaDon"), eq(Long.valueOf(order.getId())),
                eq("UPDATE"), any(), any(), any(), any(), any()
        );
    }

    @Test
    void testCallbackWhenOrderIsInStockConflict() throws Exception {
        HoaDon order = createMockOrder("DH20260608-A1B2C3", new BigDecimal("100000"), "pending", "stock_conflict");
        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        List<HoaDonChiTiet> items = createMockOrderItems(order, 5);
        when(hoaDonChiTietRepository.findByHoaDon_Id(order.getId())).thenReturn(items);

        SepayIpnRequest ipn = createMockIpnRequest("TX_STOCK_CONFLICT", new BigDecimal("100000"), "DH20260608-A1B2C3");

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("stock_conflict", order.getTrangThaiDonHang()); // Stays stock_conflict
        assertEquals(5, items.get(0).getSanPhamChiTiet().getSoLuongTon()); // Stock NOT deducted/allocated

        // Verify xuLyThanhToanSePay is NEVER called
        verify(sepayOrderPaymentService, never()).xuLyThanhToanSePay(any(), any(), any(), any());

        // Verify transaction saved with stock_conflict_blocked
        verify(paymentTransactionRepository, times(1)).saveAndFlush(argThat(tx -> 
                "stock_conflict_blocked".equals(tx.getStatus()) && "TX_STOCK_CONFLICT".equals(tx.getTransactionId())
        ));

        // Verify audit log created for STOCK_CONFLICT
        verify(auditService, times(1)).log(
                eq(null), eq("HoaDon"), eq(Long.valueOf(order.getId())),
                eq("UPDATE"), eq("stock_conflict"), eq("stock_conflict"), any(),
                argThat(msg -> msg != null && msg.contains("[PAYMENT_BLOCKED_STOCK_CONFLICT]")), eq("SYSTEM")
        );
    }

    @Test
    void testCallbackRetrySameTransactionIdForStockConflict() throws Exception {
        HoaDon order = createMockOrder("DH20260608-A1B2C3", new BigDecimal("100000"), "pending", "stock_conflict");
        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));

        List<HoaDonChiTiet> items = createMockOrderItems(order, 5);
        when(hoaDonChiTietRepository.findByHoaDon_Id(order.getId())).thenReturn(items);

        // Lần 1: IPN nhận transactionId TX123 cho đơn stock_conflict -> stock_conflict_blocked
        SepayIpnRequest ipn1 = createMockIpnRequest("TX123", new BigDecimal("100000"), "DH20260608-A1B2C3");
        Map<String, Object> result1 = realService.handleIpn(ipn1, "{}");

        assertTrue((Boolean) result1.get("success"));
        assertEquals("stock_conflict", order.getTrangThaiDonHang());

        // Giả lập PaymentTransaction với status = stock_conflict_blocked đã được lưu ở Lần 1
        PaymentTransaction savedTx = new PaymentTransaction();
        savedTx.setTransactionId("TX123");
        savedTx.setStatus("stock_conflict_blocked");
        savedTx.setOrder(order);
        when(paymentTransactionRepository.findByTransactionId("TX123")).thenReturn(Optional.of(savedTx));

        // Lần 2: SePay retry cùng transactionId = TX123 cho đơn stock_conflict
        SepayIpnRequest ipn2 = createMockIpnRequest("TX123", new BigDecimal("100000"), "DH20260608-A1B2C3");
        Map<String, Object> result2 = realService.handleIpn(ipn2, "{}");

        // Kỳ vọng Lần 2:
        // -> Trả về success true ("Already processed")
        // -> Không insert transaction lần 2
        // -> Không exception unique constraint
        // -> Không gọi xuLyThanhToanSePay
        // -> Tồn kho không thay đổi
        assertTrue((Boolean) result2.get("success"));
        assertEquals("Already processed", result2.get("message"));
        assertEquals("stock_conflict", order.getTrangThaiDonHang());
        assertEquals(5, items.get(0).getSanPhamChiTiet().getSoLuongTon());

        verify(sepayOrderPaymentService, never()).xuLyThanhToanSePay(any(), any(), any(), any());
        verify(paymentTransactionRepository, times(1)).saveAndFlush(any(PaymentTransaction.class)); // Chỉ 1 lần từ Lần 1
        verify(paymentTransactionRepository, never()).delete(any()); // Không xoá record
    }

    @Test
    void testCallbackWithMalformedOrderCode() throws Exception {
        HoaDon order = createMockOrder("DH20260608-A1B2C3", new BigDecimal("100000"), "pending", "cho_thanh_toan");
        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));
        when(sepayOrderPaymentService.xuLyThanhToanSePay(eq(order.getId()), any(), any(), any())).thenAnswer(inv -> {
            order.setPaymentStatus("paid");
            order.setTrangThaiDonHang("cho_xac_nhan");
            return true;
        });

        List<HoaDonChiTiet> items = createMockOrderItems(order, 5);
        when(hoaDonChiTietRepository.findByHoaDon_Id(order.getId())).thenReturn(items);
        when(sanPhamChiTietRepository.findByIdWithLock(anyInt())).thenReturn(Optional.of(items.get(0).getSanPhamChiTiet()));

        // Content contains extra spaces, symbols and lower/upper case variations
        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "  thanh toan don hang DH20260608-A1B2C3 ");

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("paid", order.getPaymentStatus());
        assertEquals("cho_xac_nhan", order.getTrangThaiDonHang());
    }

    @Test
    void testCallbackWithPartialCodeAndFullContent() throws Exception {
        HoaDon order = createMockOrder("DHSVN20260608104230-CECCFE", new BigDecimal("100000"), "pending", "cho_thanh_toan");
        when(hoaDonRepository.findByMaDonHang("DHSVN20260608104230-CECCFE")).thenReturn(Optional.of(order));
        when(hoaDonRepository.findByMaDonHang("DHSVN20260608104230")).thenReturn(Optional.of(order));
        when(hoaDonRepository.findByMaDonHangOrNormalized(anyString(), anyString())).thenReturn(Optional.of(order));
        when(sepayOrderPaymentService.xuLyThanhToanSePay(eq(order.getId()), any(), any(), any())).thenAnswer(inv -> {
            order.setPaymentStatus("paid");
            order.setTrangThaiDonHang("cho_xac_nhan");
            return true;
        });

        List<HoaDonChiTiet> items = createMockOrderItems(order, 5);
        when(hoaDonChiTietRepository.findByHoaDon_Id(order.getId())).thenReturn(items);
        when(sanPhamChiTietRepository.findByIdWithLock(anyInt())).thenReturn(Optional.of(items.get(0).getSanPhamChiTiet()));

        // Create IPN request with partial code in code field and full code in content field
        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "SEVQR TKPHPB DHSVN20260608104230-CECCFE");
        ipn.getTransactionData().setCode("DHSVN20260608"); // partial code

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("paid", order.getPaymentStatus());
        assertEquals("cho_xac_nhan", order.getTrangThaiDonHang());
    }

    @Test
    void testCallbackWithHyphenStrippedOrderCode() throws Exception {
        HoaDon order = createMockOrder("DHSVN20260608104230-CECCFE", new BigDecimal("100000"), "pending", "cho_thanh_toan");
        when(hoaDonRepository.findByMaDonHang("DHSVN20260608104230-CECCFE")).thenReturn(Optional.of(order));
        when(hoaDonRepository.findByMaDonHang("DHSVN20260608104230")).thenReturn(Optional.of(order));
        when(hoaDonRepository.findByMaDonHangOrNormalized(anyString(), anyString())).thenReturn(Optional.of(order));
        when(sepayOrderPaymentService.xuLyThanhToanSePay(eq(order.getId()), any(), any(), any())).thenAnswer(inv -> {
            order.setPaymentStatus("paid");
            order.setTrangThaiDonHang("cho_xac_nhan");
            return true;
        });

        List<HoaDonChiTiet> items = createMockOrderItems(order, 5);
        when(hoaDonChiTietRepository.findByHoaDon_Id(order.getId())).thenReturn(items);
        when(sanPhamChiTietRepository.findByIdWithLock(anyInt())).thenReturn(Optional.of(items.get(0).getSanPhamChiTiet()));

        // Create IPN request with hyphen-stripped code in content and code field
        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "SEVQR TKPHPB DHSVN20260608104230CECCFE");
        ipn.getTransactionData().setCode("DHSVN20260608104230CECCFE");

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("paid", order.getPaymentStatus());
        assertEquals("cho_xac_nhan", order.getTrangThaiDonHang());
    }

    @Test
    void testCallbackWithInsufficientStockSucceedsWithoutDeduction() throws Exception {
        HoaDon order = createMockOrder("DH20260608-A1B2C3", new BigDecimal("100000"), "pending", "cho_thanh_toan");
        when(hoaDonRepository.findByMaDonHang("DH20260608-A1B2C3")).thenReturn(Optional.of(order));
        when(sepayOrderPaymentService.xuLyThanhToanSePay(eq(order.getId()), any(), any(), any())).thenAnswer(inv -> {
            order.setPaymentStatus("paid");
            order.setTrangThaiDonHang("stock_conflict");
            return true;
        });

        // Stock is insufficient (only 1 available, but 2 ordered)
        List<HoaDonChiTiet> items = createMockOrderItems(order, 1);
        when(hoaDonChiTietRepository.findByHoaDon_Id(order.getId())).thenReturn(items);
        when(sanPhamChiTietRepository.findByIdWithLock(anyInt())).thenReturn(Optional.of(items.get(0).getSanPhamChiTiet()));

        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH20260608-A1B2C3");

        Map<String, Object> result = realService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("paid", order.getPaymentStatus());
        assertEquals("stock_conflict", order.getTrangThaiDonHang()); // Transitions to stock_conflict when stock is insufficient
        assertEquals(1, items.get(0).getSanPhamChiTiet().getSoLuongTon()); // Stock NOT deducted
    }

    @Test
    void testConcurrentCallbacksAndConstraintViolation() throws Exception {
        // Mock controller handling of DataIntegrityViolationException
        when(sepayGatewayService.handleIpn(any(SepayIpnRequest.class), anyString()))
                .thenThrow(new DataIntegrityViolationException("Duplicate key violation on transaction_id"));

        SepayIpnRequest ipn = createMockIpnRequest("TX100", new BigDecimal("100000"), "DH20260608-A1B2C3");
        String payloadStr = objectMapper.writeValueAsString(ipn);

        // Controller catches DataIntegrityViolationException and returns "Already processed" HTTP 200
        mockMvc.perform(post("/api/payment/sepay/ipn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadStr)
                .header("Authorization", "Apikey super-secret-ipn-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Already processed"))
                .andExpect(jsonPath("$.transactionId").value("TX100"));
    }

    // ==========================================
    // HELPER METHODS FOR TEST GENERATION
    // ==========================================
    private SepayIpnRequest createMockIpnRequest(String txId, BigDecimal amount, String content) {
        SepayIpnRequest req = new SepayIpnRequest();
        SepayTransactionDto data = new SepayTransactionDto();
        data.setTransactionId(txId);
        data.setTransferAmount(amount);
        data.setContent(content);
        data.setCode(content);
        data.setGateway("Vietcombank");
        data.setAccountNumber("1234567890");
        data.setTransactionDate("2026-06-08 09:00:00");
        req.setTransaction(data);
        return req;
    }

    private HoaDon createMockOrder(String orderCode, BigDecimal totalAmount, String paymentStatus, String orderStatus) {
        HoaDon hd = new HoaDon();
        hd.setId(1001);
        when(hoaDonRepository.findByIdWithLock(hd.getId())).thenReturn(Optional.of(hd));
        hd.setMaDonHang(orderCode);
        hd.setTongTien(totalAmount);
        hd.setPaymentStatus(paymentStatus);
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        } else {
            hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
        }
        hd.setTrangThaiDonHang(orderStatus);

        KhachHang kh = new KhachHang();
        kh.setId(123);
        TaiKhoan tk = new TaiKhoan();
        tk.setId(123);
        kh.setTaiKhoan(tk);
        hd.setKhachHang(kh);
        return hd;
    }

    private List<HoaDonChiTiet> createMockOrderItems(HoaDon order, int currentStock) {
        List<HoaDonChiTiet> items = new ArrayList<>();
        HoaDonChiTiet item = new HoaDonChiTiet();
        item.setId(2001);
        item.setHoaDon(order);
        item.setSoLuong(2); // Orders 2 items

        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(3001);
        spct.setSoLuongTon(currentStock);

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Yonex");
        sp.setTrangThai("dang_ban");
        spct.setSanPham(sp);

        item.setSanPhamChiTiet(spct);
        items.add(item);
        return items;
    }
}
