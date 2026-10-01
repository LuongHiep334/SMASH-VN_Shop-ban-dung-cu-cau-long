package com.smashvn.shop.service.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dto.payment.SepayIpnRequest;
import com.smashvn.shop.dto.payment.SepayTransactionDto;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.OrderStatus;
import com.smashvn.shop.entity.PaymentMethod;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.repository.GioHangChiTietRepository;
import com.smashvn.shop.repository.GioHangRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.ThongBaoRepository;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.api.GhnService;
import com.smashvn.shop.service.payment.SepayGatewayService;

public class SepayTimeoutAndExpirationTest {

    @Mock private jakarta.persistence.EntityManager entityManager;

    @Mock
    private HoaDonRepository hoaDonRepository;

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

    @Mock
    private GuestCheckoutService guestCheckoutService;

    @Mock
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Mock
    private ThongBaoRepository thongBaoRepository;

    @Mock
    private SepayConfig sepayConfig;

    @Mock
    private com.smashvn.shop.repository.EditLogRepository editLogRepository;

    @Mock
    private com.smashvn.shop.repository.TaiKhoanRepository taiKhoanRepository;

    @Mock
    private com.smashvn.shop.repository.NhanVienRepository nhanVienRepository;

    @Mock
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @InjectMocks
    private GioHangService gioHangService;

    @InjectMocks
    private OrderViewService orderViewService;

    @Mock
    private com.smashvn.shop.service.payment.SepayOrderPaymentService sepayOrderPaymentService;

    private SepayGatewayService sepayGatewayService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        org.springframework.test.util.ReflectionTestUtils.setField(gioHangService, "entityManager", entityManager);

        sepayGatewayService = new SepayGatewayService(
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
        org.springframework.test.util.ReflectionTestUtils.setField(sepayGatewayService, "entityManager", entityManager);
    }


    @Test
    void testExpirePendingOrder_UpdatesStatusToDaHuyAndExpired() {
        HoaDon order = new HoaDon();
        order.setId(101);
        order.setMaDonHang("DHSVN20260731-112233");
        order.setTrangThaiDonHang(OrderStatus.CHO_THANH_TOAN.getValue());
        order.setPaymentStatus("pending");

        when(hoaDonRepository.findByIdWithLock(101)).thenReturn(Optional.of(order));

        gioHangService.expirePendingOrder(order);

        assertEquals("da_huy", order.getTrangThaiDonHang());
        assertEquals("expired", order.getPaymentStatus());
        assertEquals("HỦY", order.getTrangThaiThanhToan());
        verify(hoaDonRepository).save(order);
    }

    @Test
    void testLateWebhookOnExpiredOrder_DoesNotConfirmOrDeduct() throws Exception {
        HoaDon order = new HoaDon();
        order.setId(102);
        order.setMaDonHang("DHSVN20260731-998877");
        order.setTrangThaiDonHang("da_huy");
        order.setPaymentStatus("expired");
        order.setTongTien(new BigDecimal("200000"));

        when(hoaDonRepository.findByMaDonHangOrNormalized(any(), any())).thenReturn(Optional.of(order));
        when(hoaDonRepository.findByIdWithLock(102)).thenReturn(Optional.of(order));

        SepayIpnRequest ipn = new SepayIpnRequest();
        SepayTransactionDto tx = new SepayTransactionDto();
        tx.setTransactionId("TX_LATE_123");
        tx.setTransferAmount(new BigDecimal("200000"));
        tx.setContent("DHSVN20260731-998877");
        tx.setCode("DHSVN20260731-998877");
        ipn.setTransaction(tx);

        Map<String, Object> result = sepayGatewayService.handleIpn(ipn, "{}");

        assertTrue((Boolean) result.get("success"));
        assertEquals("da_huy", order.getTrangThaiDonHang());
        assertEquals(PaymentStatus.PAID_RECEIVED_AFTER_CANCEL.getValue(), order.getPaymentStatus());
        verify(sanPhamChiTietRepository, never()).findByIdWithLock(any());
        verify(guestCheckoutService, never()).sendOrderConfirmationEmail(any(), any(), any());
    }

    @Test
    void testOrderHistory_FiltersOutChoThanhToanAndUnconfirmedDaHuy() {
        HoaDon confirmedOrder = new HoaDon();
        confirmedOrder.setId(1);
        confirmedOrder.setNgayTao(LocalDateTime.now());
        confirmedOrder.setTrangThaiDonHang("cho_xac_nhan");
        confirmedOrder.setPaymentStatus("paid");

        HoaDon pendingOrder = new HoaDon();
        pendingOrder.setId(2);
        pendingOrder.setNgayTao(LocalDateTime.now());
        pendingOrder.setTrangThaiDonHang("cho_thanh_toan");
        pendingOrder.setPaymentStatus("pending");

        HoaDon expiredOrder = new HoaDon();
        expiredOrder.setId(3);
        expiredOrder.setNgayTao(LocalDateTime.now());
        expiredOrder.setTrangThaiDonHang("da_huy");
        expiredOrder.setPaymentStatus("expired");

        when(hoaDonRepository.findByKhachHang_IdOrderByIdDesc(10)).thenReturn(List.of(confirmedOrder, pendingOrder, expiredOrder));

        List<Map<String, Object>> result = orderViewService.layDanhSachOrders(10);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("id"));
    }
}
