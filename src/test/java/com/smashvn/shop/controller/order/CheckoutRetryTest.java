package com.smashvn.shop.controller.order;

import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dto.order.*;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.order.*;
import com.smashvn.shop.service.product.*;
import com.smashvn.shop.service.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ConcurrentModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutRetryTest {
    @Mock GioHangService orders;
    @Mock DonViVanChuyenDAO carriers;
    @Mock UserAddressService addresses;
    @Mock SepayConfig sepay;
    @Mock KhachHangRepository customers;
    @Mock PhieuGiamGiaRepository vouchers;
    @Mock PricingService pricing;
    @Mock GuestCartService guestCart;
    @Mock GuestCheckoutService guests;
    @Mock UserDangNhapService login;
    @Mock SanPhamChiTietRepository products;
    @Mock TaiKhoanRepository accounts;
    @Mock TokenKhoiPhucRepository activationTokens;
    @Mock SoDiaChiRepository savedAddresses;
    @Mock PendingCheckoutRegistry pending;
    @Mock GioHangChiTietRepository cartItems;
    @Mock ProductAvailabilityService availability;
    @Mock TemporaryPasswordService passwords;

    CheckoutContextService contexts;
    CheckoutController controller;
    MockHttpSession session;
    MockHttpServletRequest request;
    CheckoutContext context;
    TaiKhoan account;
    HoaDon order;

    @BeforeEach
    void setUp() {
        contexts = new CheckoutContextService(guestCart, orders, products, accounts, availability);
        controller = new CheckoutController(orders, carriers, addresses, sepay, customers,
                vouchers, pricing, guestCart, guests, login, products, accounts, activationTokens,
                savedAddresses, contexts, pending, cartItems, availability, passwords);
        session = new MockHttpSession();
        session.setAttribute("idNguoiDung", 7);
        request = new MockHttpServletRequest();
        request.setSession(session);
        context = contexts.createBuyNowContext(session, 7, 99, 1);
        account = new TaiKhoan();
        account.setId(7);
        account.setUsername("buyer@example.invalid");
        account.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        account.setTrangThai("hoat_dong");
        account.setMatKhau("test-hash");
        KhachHang customer = new KhachHang();
        customer.setId(8);
        customer.setTaiKhoan(account);
        DonViVanChuyen carrier = new DonViVanChuyen();
        carrier.setId(1);
        carrier.setMaDonVi("GHN");
        carrier.setTenDonVi("Giao Hàng Nhanh");
        lenient().when(accounts.findById(7)).thenReturn(Optional.of(account));
        lenient().when(customers.findByTaiKhoan_Id(7)).thenReturn(customer);
        lenient().when(carriers.findById(1)).thenReturn(Optional.of(carrier));
        lenient().when(savedAddresses.findByKhachHang_Id(8)).thenReturn(List.of(new SoDiaChi()));

        order = new HoaDon();
        order.setId(501);
        order.setMaDonHang("HD501");
        order.setPaymentMethod("COD");
        order.setTongTien(new BigDecimal("175000"));
        lenient().when(orders.submitCodOrder(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenAnswer(i -> createdOrder());
        lenient().when(orders.createSepayPendingOrder(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenAnswer(i -> createdOrder());
    }

    private OrderCreationResult createdOrder() {
        return OrderCreationResult.builder().hoaDon(order).purchasedItems(List.of()).build();
    }

    private Map<String, Object> submit(String name, String phone, String email, Integer addressId,
                                       Integer province, String method) {
        return controller.submitCheckout(context.getToken(), name, phone, "123 Test Street", 1,
                method, null, 1442, "20101", province, addressId, null, email,
                "City", "District", "Ward", "123 Test Street", false, session, request).getBody();
    }

    private Map<String, Object> submit() {
        return submit("Test Buyer", "0912345678", null, null, 201, "COD");
    }

    private long createdOrderCalls() {
        return mockingDetails(orders).getInvocations().stream()
                .filter(i -> List.of("submitCodOrder", "createSepayPendingOrder").contains(i.getMethod().getName()))
                .count();
    }

    @Test
    void correctInvalidInputsAndSubmitAgainWithTheSameToken() {
        assertEquals("Họ và tên người nhận không được để trống.",
                submit("", "", null, null, 201, "COD").get("message"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("Số điện thoại không được để trống.",
                submit("Test Buyer", "", null, null, 201, "COD").get("message"));
        assertEquals("READY", controller.checkoutStatus(context.getToken(), session).getBody().get("checkoutStatus"));
        assertEquals("ok", submit().get("trangThai"));
        assertEquals(1, createdOrderCalls());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "12345", "0123456789"})
    void phoneValidationDoesNotConsumeTheToken(String phone) {
        assertEquals("loi", submit("Test Buyer", phone, null, null, 201, "COD").get("trangThai"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("ok", submit().get("trangThai"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOCKED", "ACTIVE"})
    void guestEmailRejectionAndLoginRequestReleaseTheToken(String emailStatus) {
        session.removeAttribute("idNguoiDung");
        context.setCustomerId(null);
        when(guests.checkEmailStatus("buyer@example.invalid")).thenReturn(emailStatus);
        Map<String, Object> response = submit("Test Buyer", "0912345678", "buyer@example.invalid", null, 201, "COD");
        assertEquals("ACTIVE".equals(emailStatus) ? "yeucaudangnhap" : "loi", response.get("trangThai"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertNotNull(contexts.promoteGuestContextToAuthenticatedUser(context.getToken(), session, session, 7));
        session.setAttribute("idNguoiDung", 7);
        assertEquals("ok", submit().get("trangThai"));
    }

    @Test
    void missingGuestEmailAndRegistrationFailureAreRetryable() {
        session.removeAttribute("idNguoiDung");
        context.setCustomerId(null);
        assertEquals("Email không được để trống.", submit().get("message"));
        when(guests.checkEmailStatus("buyer@example.invalid")).thenReturn("NEW");
        when(guests.autoRegisterGuest(any(), any(), any())).thenThrow(new IllegalArgumentException("Phone already exists"));
        assertEquals("loi", submit("Test Buyer", "0912345678", "buyer@example.invalid", null, 201, "COD").get("trangThai"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals(0, createdOrderCalls());
        session.setAttribute("idNguoiDung", 7);
        assertEquals("ok", submit().get("trangThai"));
    }

    @Test
    void invalidSavedAddressAndMissingShippingMappingCanBeCorrected() {
        when(addresses.layDiaChiTheoId(42, 8)).thenThrow(new IllegalArgumentException("Missing address"));
        assertEquals("loi", submit("Test Buyer", "0912345678", null, 42, 201, "COD").get("trangThai"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("loi", submit("Test Buyer", "0912345678", null, null, null, "COD").get("trangThai"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("ok", submit().get("trangThai"));
    }

    @Test
    void missingPaymentMethodCanBeCorrected() {
        assertEquals("loi", submit("Test Buyer", "0912345678", null, null, 201, "").get("trangThai"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("ok", submit().get("trangThai"));
    }

    @Test
    void unexpectedFailureBeforeOrderCreationReleasesTheToken() {
        when(accounts.findById(7)).thenThrow(new IllegalStateException("Temporary read failure"))
                .thenReturn(Optional.of(account));
        assertEquals("loi", submit().get("trangThai"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("ok", submit().get("trangThai"));
    }

    @Test
    void failedOrderTransactionCanBeRetried() {
        when(orders.submitCodOrder(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Transaction rolled back")).thenReturn(createdOrder());
        assertEquals("loi", submit().get("trangThai"));
        assertNull(context.getCompletedCheckout());
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("ok", submit().get("trangThai"));
    }

    @Test
    void addressSaveFailureBeforeOrderCreationCanBeRetried() {
        when(savedAddresses.findByKhachHang_Id(8)).thenReturn(List.of());
        when(savedAddresses.save(any())).thenThrow(new IllegalStateException("Address store unavailable"))
                .thenAnswer(i -> {
                    SoDiaChi address = i.getArgument(0);
                    address.setId(42);
                    return address;
                });
        assertEquals("loi", submit().get("trangThai"));
        assertEquals(0, createdOrderCalls());
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("ok", submit().get("trangThai"));
    }

    @Test
    void missingAddressAndInvalidNameLengthCanBeCorrected() {
        assertEquals("loi", submit("A", "0912345678", null, null, 201, "COD").get("trangThai"));
        assertEquals("loi", submit("A".repeat(101), "0912345678", null, null, 201, "COD").get("trangThai"));
        var missingAddress = controller.submitCheckout(context.getToken(), "Test Buyer", "0912345678", "",
                1, "COD", null, 1442, "20101", 201, null, null, null,
                "City", "District", "Ward", "", false, session, request).getBody();
        assertEquals("Địa chỉ nhận hàng không được để trống.", missingAddress.get("message"));
        assertEquals(CheckoutContextStatus.READY, context.getStatus());
        assertEquals("ok", submit().get("trangThai"));
    }

    @Test
    void failureAfterCodCommitReturnsTheSavedOrderAndNeverCreatesAnother() {
        when(passwords.recordCodOrderCreated(7, false)).thenThrow(new IllegalStateException("Post-order failure"));
        Map<String, Object> first = submit();
        assertEquals("ok", first.get("trangThai"));
        assertEquals(501, first.get("orderId"));
        assertEquals(CheckoutContextStatus.CONSUMED, context.getStatus());
        // A different selected payment method on retry must not create or change the existing order.
        Map<String, Object> retry = submit("Test Buyer", "0912345678", null, null, 201, "SePay");
        assertEquals(PaymentMethod.COD.getValue(), retry.get("paymentMethod"));
        assertEquals("/user/manage-order/501", retry.get("redirectUrl"));
        assertEquals(1, createdOrderCalls());
        verify(passwords, times(1)).recordCodOrderCreated(7, false);
    }

    @Test
    void sepayRegistryFailureAfterCommitStillReturnsSamePaymentPage() {
        order.setPaymentMethod("SEPAY");
        doThrow(new IllegalStateException("Registry unavailable")).when(pending).registerSnapshot(any());
        Map<String, Object> first = submit("Test Buyer", "0912345678", null, null, 201, "SePay");
        assertEquals("ok", first.get("trangThai"));
        assertEquals("/payment/sepay/simulate?maDonHang=" + order.getMaDonHang(), first.get("redirectUrl"));
        assertEquals(501, submit().get("orderId"));
        assertEquals(1, createdOrderCalls());
    }

    @Test
    void paymentValidationAfterCommitDoesNotReopenTheCheckout() {
        order.setPaymentMethod("SEPAY");
        order.setTongTien(BigDecimal.ZERO);
        assertEquals("/user/manage-order/501",
                submit("Test Buyer", "0912345678", null, null, 201, "SePay").get("redirectUrl"));
        assertEquals(501, submit().get("orderId"));
        assertEquals(1, createdOrderCalls());
    }

    @Test
    void lostResponseCanBeRecoveredByStatusAndPageReloadEvenAfterExpiry() {
        assertEquals("ok", submit().get("trangThai"));
        context.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertEquals(501, controller.checkoutStatus(context.getToken(), session).getBody().get("orderId"));
        assertEquals("redirect:/user/manage-order/501",
                controller.viewCheckout(context.getToken(), session, new ConcurrentModel()));
        assertEquals(501, submit().get("orderId"));
        assertEquals(1, createdOrderCalls());
    }

    @Test
    void expiredOrUnknownTokensDoNotFallBackToCreatingAnotherOrder() {
        context.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertEquals("CHECKOUT_EXPIRED", submit().get("errorCode"));
        context.setToken("unknown-token");
        assertEquals("CHECKOUT_EXPIRED", submit().get("errorCode"));
        assertEquals(0, createdOrderCalls());
    }

    @Test
    void anotherAccountOrSessionCannotRecoverACompletedCheckout() {
        submit();
        MockHttpSession otherSession = new MockHttpSession();
        assertEquals("CHECKOUT_EXPIRED", controller.checkoutStatus(context.getToken(), otherSession).getBody().get("errorCode"));
        session.setAttribute("idNguoiDung", 900);
        assertEquals("CHECKOUT_EXPIRED", submit().get("errorCode"));
        assertEquals(1, createdOrderCalls());
    }

    @Test
    void concurrentRequestsCreateOnlyOneOrderAndPollingDoesNotReleaseItsLock() throws Exception {
        CountDownLatch inTransaction = new CountDownLatch(1);
        CountDownLatch finishTransaction = new CountDownLatch(1);
        when(orders.submitCodOrder(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any())).thenAnswer(i -> {
            inTransaction.countDown();
            assertTrue(finishTransaction.await(5, TimeUnit.SECONDS));
            return createdOrder();
        });
        try (var executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(() -> { return submit(); });
            try {
                assertTrue(inTransaction.await(5, TimeUnit.SECONDS));
                context.setExpiresAt(LocalDateTime.now().minusSeconds(1));
                assertEquals("PROCESSING", submit().get("checkoutStatus"));
                assertEquals("PROCESSING", controller.checkoutStatus(context.getToken(), session).getBody().get("checkoutStatus"));
                assertEquals(CheckoutContextStatus.PROCESSING, context.getStatus());
            } finally {
                finishTransaction.countDown();
            }
            assertEquals("ok", first.get(5, TimeUnit.SECONDS).get("trangThai"));
        }
        assertEquals(501, submit().get("orderId"));
        assertEquals(1, createdOrderCalls());
    }

    @Test
    void guestSessionRotationAndLostGuestAccessCanRecoverOnlyTheSavedOrder() {
        session.removeAttribute("idNguoiDung");
        context.setCustomerId(null);
        account.setTrangThaiTaiKhoan(AccountStatus.GUEST);
        account.setVaiTro("KH");
        when(guests.checkEmailStatus("buyer@example.invalid")).thenReturn("NEW");
        when(guests.autoRegisterGuest(any(), any(), any()))
                .thenReturn(new GuestCheckoutService.GuestRegisterResult(account, "test", true));
        when(savedAddresses.save(any())).thenAnswer(i -> {
            SoDiaChi address = i.getArgument(0);
            address.setId(42);
            return address;
        });
        String oldSessionId = session.getId();
        assertEquals("ok", submit("Test Buyer", "0912345678", "buyer@example.invalid", null, 201, "COD").get("trangThai"));
        assertNotEquals(oldSessionId, session.getId());
        session.removeAttribute("allowedGuestOrderAccesses");
        assertEquals(501, controller.checkoutStatus(context.getToken(), session).getBody().get("orderId"));
        controller.checkoutStatus(context.getToken(), session);
        List<?> accesses = (List<?>) session.getAttribute("allowedGuestOrderAccesses");
        assertEquals(1, accesses.size());
        assertEquals(501, ((CheckoutController.GuestOrderAccess) accesses.get(0)).getOrderId());
        assertEquals(1, createdOrderCalls());
    }
}
