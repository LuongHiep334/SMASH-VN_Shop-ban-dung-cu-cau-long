package com.smashvn.shop.service.payment;

import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.PaymentTransaction;
import com.smashvn.shop.entity.RefundStatus;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SepayOrderPaymentServiceTest {

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private com.smashvn.shop.repository.HoaDonChiTietRepository hoaDonChiTietRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private com.smashvn.shop.service.inventory.InventoryLotService inventoryLotService;

    @Mock
    private com.smashvn.shop.repository.PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Mock
    private com.smashvn.shop.repository.GioHangRepository gioHangRepository;

    @Mock
    private com.smashvn.shop.repository.GioHangChiTietRepository gioHangChiTietRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private com.smashvn.shop.service.order.GuestCheckoutService guestCheckoutService;

    @Mock
    private com.smashvn.shop.service.user.TemporaryPasswordService temporaryPasswordService;

    @InjectMocks
    private SepayOrderPaymentService sepayOrderPaymentService;

    private HoaDon sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new HoaDon();
        sampleOrder.setId(100);
        sampleOrder.setMaDonHang("HD100");
        sampleOrder.setTrangThaiDonHang("YEU_CAU_HUY");
        sampleOrder.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        sampleOrder.setRefundStatus(RefundStatus.PENDING);
    }

    @Test
    @DisplayName("TC-B15.1 – Transaction SUCCESS bình thường phải bị chặn (throw IllegalStateException)")
    void testTC_B15_1_NormalSuccessTransactionBlocked() {
        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));

        PaymentTransaction successTx = new PaymentTransaction();
        successTx.setStatus("SUCCESS");
        when(paymentTransactionRepository.findByOrder_Id(100)).thenReturn(List.of(successTx));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);
        });

        assertEquals("Đơn hàng không thuộc trường hợp đã thanh toán nhưng thiếu tồn kho.", ex.getMessage());
        assertEquals("YEU_CAU_HUY", sampleOrder.getTrangThaiDonHang());
        assertNotEquals("REFUNDED", sampleOrder.getTrangThaiThanhToan());
        verify(hoaDonRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-B15.2 – PAID_INSUFFICIENT_STOCK hợp lệ -> Finalize refund thành công")
    void testTC_B15_2_PaidInsufficientStockSuccess() {
        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));

        PaymentTransaction insufficientTx = new PaymentTransaction();
        insufficientTx.setStatus("PAID_INSUFFICIENT_STOCK");
        when(paymentTransactionRepository.findByOrder_Id(100)).thenReturn(List.of(insufficientTx));

        sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);

        assertEquals("DA_HUY", sampleOrder.getTrangThaiDonHang());
        assertEquals("REFUNDED", sampleOrder.getTrangThaiThanhToan());
        assertEquals(RefundStatus.COMPLETED, sampleOrder.getRefundStatus());
        verify(hoaDonRepository, times(1)).save(sampleOrder);
        verify(auditService, times(1)).log(eq(1), eq("HoaDon"), eq(100L), eq("FINALIZE_REFUND_NO_RESTOCK"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-B15.3 – Có cả SUCCESS và PAID_INSUFFICIENT_STOCK -> Cho phép refund dựa trên PAID_INSUFFICIENT_STOCK")
    void testTC_B15_3_BothSuccessAndPaidInsufficientStock() {
        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));

        PaymentTransaction successTx = new PaymentTransaction();
        successTx.setStatus("SUCCESS");

        PaymentTransaction insufficientTx = new PaymentTransaction();
        insufficientTx.setStatus("PAID_INSUFFICIENT_STOCK");

        when(paymentTransactionRepository.findByOrder_Id(100)).thenReturn(List.of(successTx, insufficientTx));

        sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);

        assertEquals("DA_HUY", sampleOrder.getTrangThaiDonHang());
        assertEquals("REFUNDED", sampleOrder.getTrangThaiThanhToan());
        assertEquals(RefundStatus.COMPLETED, sampleOrder.getRefundStatus());
        verify(hoaDonRepository, times(1)).save(sampleOrder);
    }

    @Test
    @DisplayName("TC-B15.4 – Không có transaction (txs = empty) -> Reject an toàn, không NullPointerException")
    void testTC_B15_4_EmptyTransactionsRejectSafely() {
        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));
        when(paymentTransactionRepository.findByOrder_Id(100)).thenReturn(Collections.emptyList());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);
        });

        assertEquals("Đơn hàng không thuộc trường hợp đã thanh toán nhưng thiếu tồn kho.", ex.getMessage());
        assertEquals("YEU_CAU_HUY", sampleOrder.getTrangThaiDonHang());
        verify(hoaDonRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-B15.5 – Retry sau khi đã refund -> Idempotent, không refund lần 2")
    void testTC_B15_5_RetryAfterRefundedIdempotent() {
        sampleOrder.setTrangThaiDonHang("DA_HUY");
        sampleOrder.setTrangThaiThanhToan("REFUNDED");
        sampleOrder.setRefundStatus(RefundStatus.COMPLETED);

        when(hoaDonRepository.findById(100)).thenReturn(Optional.of(sampleOrder));

        sepayOrderPaymentService.finalizeRefundWithoutRestock(100, 1);

        assertEquals("DA_HUY", sampleOrder.getTrangThaiDonHang());
        assertEquals("REFUNDED", sampleOrder.getTrangThaiThanhToan());
        assertEquals(RefundStatus.COMPLETED, sampleOrder.getRefundStatus());
        verify(paymentTransactionRepository, never()).findByOrder_Id(any());
        verify(hoaDonRepository, never()).save(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("TC-VOUCHER-01 – Thanh toán SePay thành công có voucher -> Phải trừ soLuongConLai của voucher")
    void testSepayPaymentSuccess_DecrementsVoucherQuantity() {
        // Given
        com.smashvn.shop.entity.PhieuGiamGia voucher = new com.smashvn.shop.entity.PhieuGiamGia();
        voucher.setId(10);
        voucher.setMaPhieu("VOUCHER50K");
        voucher.setSoLuongConLai(5);

        HoaDon order = new HoaDon();
        order.setId(200);
        order.setMaDonHang("HD200");
        order.setTrangThaiDonHang("CHO_THANH_TOAN");
        order.setPhieuGiamGia(voucher);

        com.smashvn.shop.entity.TaiKhoan guest = new com.smashvn.shop.entity.TaiKhoan();
        guest.setId(25);
        guest.setTrangThaiTaiKhoan(com.smashvn.shop.entity.AccountStatus.GUEST);
        com.smashvn.shop.entity.KhachHang customer = new com.smashvn.shop.entity.KhachHang();
        customer.setId(26);
        customer.setTaiKhoan(guest);
        order.setKhachHang(customer);

        when(hoaDonRepository.findById(200)).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByTransactionId("TX_SEP_12345")).thenReturn(Optional.empty());

        com.smashvn.shop.entity.SanPhamChiTiet spct = new com.smashvn.shop.entity.SanPhamChiTiet();
        spct.setId(1);

        com.smashvn.shop.entity.HoaDonChiTiet hdct = new com.smashvn.shop.entity.HoaDonChiTiet();
        hdct.setId(1);
        hdct.setHoaDon(order);
        hdct.setSanPhamChiTiet(spct);
        hdct.setSoLuong(1);
        hdct.setDonGia(java.math.BigDecimal.valueOf(500000));
        hdct.setGiaGoc(java.math.BigDecimal.valueOf(500000));
        hdct.setGiaSauGiam(java.math.BigDecimal.valueOf(450000));

        when(hoaDonChiTietRepository.findByHoaDon_Id(200)).thenReturn(List.of(hdct));

        com.smashvn.shop.dto.inventory.LotAllocation alloc = new com.smashvn.shop.dto.inventory.LotAllocation(
                1, 1, spct, 1
        );

        com.smashvn.shop.dto.inventory.AllocationResult allocResult = new com.smashvn.shop.dto.inventory.AllocationResult(
                com.smashvn.shop.dto.inventory.AllocationStatus.SUCCESS,
                List.of(alloc),
                "Phân bổ thành công"
        );

        when(inventoryLotService.allocateFifo(any())).thenReturn(allocResult);
        when(phieuGiamGiaRepository.findByMaPhieuWithLock("VOUCHER50K")).thenReturn(Optional.of(voucher));

        // When
        boolean result = sepayOrderPaymentService.xuLyThanhToanSePay(200, "TX_SEP_12345", java.math.BigDecimal.valueOf(450000), "{}");

        // Then
        assertTrue(result);
        assertEquals(4, voucher.getSoLuongConLai());
        verify(phieuGiamGiaRepository, times(1)).save(voucher);
        verify(hoaDonRepository, times(1)).save(order);
        verify(temporaryPasswordService, times(1)).recordSepayPaymentSuccess(25);
    }

    @Test
    void duplicateSepayTransactionDoesNotRecordPurchaseOrIssuePasswordAgain() {
        HoaDon order = new HoaDon();
        order.setId(300);
        when(hoaDonRepository.findById(300)).thenReturn(Optional.of(order));

        PaymentTransaction existing = new PaymentTransaction();
        existing.setTransactionId("DUPLICATE_TX");
        when(paymentTransactionRepository.findByTransactionId("DUPLICATE_TX"))
                .thenReturn(Optional.of(existing));

        assertTrue(sepayOrderPaymentService.xuLyThanhToanSePay(
                300, "DUPLICATE_TX", java.math.BigDecimal.TEN, "{}"));

        verifyNoInteractions(temporaryPasswordService);
        verify(inventoryLotService, never()).allocateFifo(any());
    }

    @Test
    void smtpFailureAfterSepayCommitDoesNotUndoPaidOrderOrIssueAgain() {
        HoaDon order = new HoaDon();
        order.setId(400);
        order.setMaDonHang("HD400");
        order.setTrangThaiDonHang("CHO_THANH_TOAN");
        order.setEmailNguoiNhan("buyer@realmail.vn");

        com.smashvn.shop.entity.TaiKhoan guest = new com.smashvn.shop.entity.TaiKhoan();
        guest.setId(35);
        guest.setTrangThaiTaiKhoan(com.smashvn.shop.entity.AccountStatus.GUEST);
        com.smashvn.shop.entity.KhachHang customer = new com.smashvn.shop.entity.KhachHang();
        customer.setId(36);
        customer.setTaiKhoan(guest);
        order.setKhachHang(customer);

        com.smashvn.shop.entity.SanPhamChiTiet spct = new com.smashvn.shop.entity.SanPhamChiTiet();
        spct.setId(2);
        com.smashvn.shop.entity.HoaDonChiTiet provisional = new com.smashvn.shop.entity.HoaDonChiTiet();
        provisional.setId(2);
        provisional.setHoaDon(order);
        provisional.setSanPhamChiTiet(spct);
        provisional.setSoLuong(1);
        provisional.setDonGia(java.math.BigDecimal.valueOf(200000));
        provisional.setGiaGoc(java.math.BigDecimal.valueOf(200000));
        provisional.setGiaSauGiam(java.math.BigDecimal.valueOf(200000));

        when(hoaDonRepository.findById(400)).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByTransactionId("TX_SMTP_FAIL")).thenReturn(Optional.empty());
        when(hoaDonChiTietRepository.findByHoaDon_Id(400)).thenReturn(List.of(provisional));
        when(inventoryLotService.allocateFifo(any())).thenReturn(
                new com.smashvn.shop.dto.inventory.AllocationResult(
                        com.smashvn.shop.dto.inventory.AllocationStatus.SUCCESS,
                        List.of(new com.smashvn.shop.dto.inventory.LotAllocation(2, 2, spct, 1)),
                        "OK"));

        var issueResult = new com.smashvn.shop.service.user.TemporaryPasswordService.TemporaryPasswordIssueResult(
                com.smashvn.shop.service.user.TemporaryPasswordService.IssueStatus.ISSUED,
                35,
                "buyer@realmail.vn",
                "A7kp2Qm9Xs4L",
                "activation-token");
        when(temporaryPasswordService.recordSepayPaymentSuccess(35)).thenReturn(issueResult);
        doThrow(new RuntimeException("SMTP unavailable"))
                .when(temporaryPasswordService).sendTemporaryPasswordEmail(issueResult, "http://localhost:8080");

        TransactionSynchronizationManager.initSynchronization();
        try {
            boolean result = sepayOrderPaymentService.xuLyThanhToanSePay(
                    400, "TX_SMTP_FAIL", java.math.BigDecimal.valueOf(200000), "{}");
            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();

            assertTrue(result);
            assertEquals("paid", order.getTrangThaiThanhToan());
            assertEquals("paid", order.getPaymentStatus());
            assertEquals(1, synchronizations.size());
            assertDoesNotThrow(() -> synchronizations.forEach(TransactionSynchronization::afterCommit));

            verify(temporaryPasswordService).recordSepayPaymentSuccess(35);
            verify(temporaryPasswordService).sendTemporaryPasswordEmail(issueResult, "http://localhost:8080");
            assertEquals("paid", order.getTrangThaiThanhToan());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
