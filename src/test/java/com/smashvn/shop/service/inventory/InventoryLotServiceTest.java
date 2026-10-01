package com.smashvn.shop.service.inventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.smashvn.shop.dto.inventory.*;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.AuditService;
import com.smashvn.shop.service.payment.SepayOrderPaymentService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InventoryLotServiceTest {

    @Mock private jakarta.persistence.EntityManager entityManager;

    @Mock
    private SanPhamRepository sanPhamRepository;

    @Mock
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Mock
    private HoaDonRepository hoaDonRepository;

    @Mock
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private com.smashvn.shop.repository.NhanVienRepository nhanVienRepository;

    @Mock
    private com.smashvn.shop.repository.PhieuNhapRepository phieuNhapRepository;

    @Mock
    private com.smashvn.shop.repository.PhieuNhapChiTietRepository phieuNhapChiTietRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InventoryLotService inventoryLotService;

    @InjectMocks
    private SepayOrderPaymentService sepayOrderPaymentService;

    private SanPham sampleProduct1;
    private SanPham sampleProduct2;
    private SanPhamChiTiet spct1;
    private SanPhamChiTiet spct2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryLotService, "entityManager", entityManager);
        ReflectionTestUtils.setField(inventoryLotService, "enabledFromStr", "2026-08-07T08:30:00");
        ReflectionTestUtils.setField(sepayOrderPaymentService, "inventoryLotService", inventoryLotService);

        sampleProduct1 = new SanPham();

        sampleProduct1.setId(3);
        sampleProduct1.setTenSanPham("Vợt Yonex Astrox 88D Pro");
        sampleProduct1.setTrangThaiValue(true);

        sampleProduct2 = new SanPham();
        sampleProduct2.setId(8);
        sampleProduct2.setTenSanPham("Giày Yonex Power Cushion 65Z3");
        sampleProduct2.setTrangThaiValue(true);

        spct1 = new SanPhamChiTiet();
        spct1.setId(101);
        spct1.setSanPham(sampleProduct1);
        spct1.setSoLuongTon(10);
        spct1.setGiaBan(new BigDecimal("3500000"));
        spct1.setGiaNhap(new BigDecimal("2800000"));
        spct1.setTrangThaiValue(true);
        spct1.setNgayTao(LocalDateTime.of(2026, 8, 7, 9, 0));

        spct2 = new SanPhamChiTiet();
        spct2.setId(102);
        spct2.setSanPham(sampleProduct2);
        spct2.setSoLuongTon(5);
        spct2.setGiaBan(new BigDecimal("2200000"));
        spct2.setGiaNhap(new BigDecimal("1700000"));
        spct2.setTrangThaiValue(true);
        spct2.setNgayTao(LocalDateTime.of(2026, 8, 7, 9, 0));
    }

    @Test
    @DisplayName("Test 1: SePay nhận tiền nhưng thiếu tồn kho -> Chặn giao hàng & finalizeRefundWithoutRestock không cộng tồn kho")
    void testSePayPaidInsufficientStockAndRefundNoRestock() {
        Integer orderId = 99;
        HoaDon order = new HoaDon();
        order.setId(orderId);
        order.setTrangThaiDonHang("CHO_THANH_TOAN");
        order.setTongTien(new BigDecimal("3500000"));

        HoaDonChiTiet pItem = new HoaDonChiTiet();
        pItem.setId(501);
        pItem.setHoaDon(order);
        pItem.setSanPhamChiTiet(spct1);
        pItem.setSoLuong(20); // Yêu cầu 20 nhưng kho chỉ có 10

        when(hoaDonRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentTransactionRepository.findByTransactionId("TX_SEPAY_99")).thenReturn(Optional.empty());
        when(hoaDonChiTietRepository.findByHoaDon_Id(orderId)).thenReturn(List.of(pItem));
        when(sanPhamChiTietRepository.findById(101)).thenReturn(Optional.of(spct1));
        when(sanPhamRepository.findByIdWithLock(3)).thenReturn(Optional.of(sampleProduct1));
        when(sanPhamChiTietRepository.findActiveCandidatesBySanPhamId(3)).thenReturn(List.of(spct1));

        boolean processed = sepayOrderPaymentService.xuLyThanhToanSePay(orderId, "TX_SEPAY_99", new BigDecimal("3500000"), "payload");

        assertTrue(processed);
        assertEquals("YEU_CAU_HUY", order.getTrangThaiDonHang());
        assertEquals("CHO_HOAN_TIEN", order.getTrangThaiThanhToan());
        assertEquals(RefundStatus.PENDING, order.getRefundStatus());

        // Gọi finalizeRefundWithoutRestock khi hoàn tiền thủ công xong
        PaymentTransaction tx = new PaymentTransaction();
        tx.setStatus("PAID_INSUFFICIENT_STOCK");
        when(paymentTransactionRepository.findByOrder_Id(orderId)).thenReturn(List.of(tx));

        sepayOrderPaymentService.finalizeRefundWithoutRestock(orderId, 1);

        assertEquals("DA_HUY", order.getTrangThaiDonHang());
        assertEquals("REFUNDED", order.getTrangThaiThanhToan());
        assertEquals(RefundStatus.COMPLETED, order.getRefundStatus());
        verify(sanPhamChiTietRepository, never()).save(argThat(s -> s.getSoLuongTon() > 10));
    }

    @Test
    @DisplayName("Test 2: Hoàn kho hàng loạt khóa ID sản phẩm tăng dần")
    void testMultiProductRestockAscendingLock() {
        RestockItemRequest req1 = RestockItemRequest.builder().idSanPhamChiTiet(102).quantityToRestock(2).conBanDuoc(true).build();
        RestockItemRequest req2 = RestockItemRequest.builder().idSanPhamChiTiet(101).quantityToRestock(3).conBanDuoc(true).build();

        when(sanPhamChiTietRepository.findById(101)).thenReturn(Optional.of(spct1));
        when(sanPhamChiTietRepository.findById(102)).thenReturn(Optional.of(spct2));
        when(sanPhamRepository.findByIdWithLock(3)).thenReturn(Optional.of(sampleProduct1));
        when(sanPhamRepository.findByIdWithLock(8)).thenReturn(Optional.of(sampleProduct2));

        inventoryLotService.hoanKhoHangLoat(List.of(req1, req2));

        assertEquals(13, spct1.getSoLuongTon()); // 10 + 3
        assertEquals(7, spct2.getSoLuongTon());  // 5 + 2
    }

    @Test
    @DisplayName("Test 3: Trả hàng một phần LIFO trên các dòng HDCT gốc")
    void testPartialReturnLifo() {
        SanPhamChiTiet oldLotSpct = new SanPhamChiTiet();
        oldLotSpct.setId(90);
        oldLotSpct.setSanPham(sampleProduct1);
        oldLotSpct.setSoLuongTon(2);

        SanPhamChiTiet newLotSpct = new SanPhamChiTiet();
        newLotSpct.setId(95);
        newLotSpct.setSanPham(sampleProduct1);
        newLotSpct.setSoLuongTon(1);

        when(sanPhamChiTietRepository.findById(95)).thenReturn(Optional.of(newLotSpct));
        when(sanPhamRepository.findByIdWithLock(3)).thenReturn(Optional.of(sampleProduct1));

        RestockItemRequest restockReq = RestockItemRequest.builder()
                .idSanPhamChiTiet(95)
                .quantityToRestock(1)
                .conBanDuoc(true)
                .build();

        inventoryLotService.hoanKhoHangLoat(List.of(restockReq));

        assertEquals(2, newLotSpct.getSoLuongTon()); // 1 + 1
    }

    @Test
    @DisplayName("Test 4: Phân loại Legacy Lot (ngayTao < enabledFrom) vs Lô Mới")
    void testLegacyLotEnabledFromClassification() {
        SanPhamChiTiet legacySpct = new SanPhamChiTiet();
        legacySpct.setId(1);
        legacySpct.setSanPham(sampleProduct1);
        legacySpct.setSoLuongTon(10);
        legacySpct.setGiaNhap(new BigDecimal("1000000"));
        legacySpct.setNgayTao(LocalDateTime.of(2026, 8, 1, 10, 0));

        SanPhamChiTiet newSpct = new SanPhamChiTiet();
        newSpct.setId(2);
        newSpct.setSanPham(sampleProduct1);
        newSpct.setSoLuongTon(5);
        newSpct.setGiaNhap(new BigDecimal("1200000"));
        newSpct.setNgayTao(LocalDateTime.of(2026, 8, 7, 9, 0));

        when(sanPhamChiTietRepository.findBySanPham_Id(3)).thenReturn(List.of(legacySpct, newSpct));

        List<LoHangDTO> summaries = inventoryLotService.calculateLotSummaries(3);

        assertEquals(2, summaries.size());
        assertTrue(summaries.get(0).isLegacyLot());
        assertEquals("LÔ KHỞI TẠO (KHO BAN ĐẦU)", summaries.get(0).getMaLo());
        assertFalse(summaries.get(1).isLegacyLot());
        assertTrue(summaries.get(1).getMaLo().startsWith("LO-3-20260807090000-2"));
    }

    @Test
    @DisplayName("Test 5: Tính nguyên tử @Transactional khi nhập lô mới")
    void testLotImportAtomicity() {
        when(sanPhamChiTietRepository.findById(101)).thenReturn(Optional.of(spct1));
        when(sanPhamRepository.findByIdWithLock(3)).thenReturn(Optional.of(sampleProduct1));
        when(sanPhamChiTietRepository.save(any(SanPhamChiTiet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(nhanVienRepository.findByTaiKhoanId(any())).thenReturn(new com.smashvn.shop.entity.NhanVien());

        SanPhamChiTiet result = inventoryLotService.nhapLoMoi(101, 15, new BigDecimal("2900000"), 1);

        assertNotNull(result);
        assertEquals(25, result.getSoLuongTon());
        assertEquals(new BigDecimal("2860000.00"), result.getGiaNhap());
    }

    @Test
    @DisplayName("Test 6: Rollback 100% hai giai đoạn allocateFifo nếu 1 mặt hàng thiếu kho")
    void testTwoPhaseAllocateFifoRollbackOnSingleItemShortage() {
        OrderItemRequest req1 = OrderItemRequest.builder().representativeSpctId(101).quantity(5).build();  // Đủ (có 10)
        OrderItemRequest req2 = OrderItemRequest.builder().representativeSpctId(102).quantity(20).build(); // Thiếu (chỉ có 5)

        when(sanPhamChiTietRepository.findById(101)).thenReturn(Optional.of(spct1));
        when(sanPhamChiTietRepository.findById(102)).thenReturn(Optional.of(spct2));
        when(sanPhamRepository.findByIdWithLock(3)).thenReturn(Optional.of(sampleProduct1));
        when(sanPhamRepository.findByIdWithLock(8)).thenReturn(Optional.of(sampleProduct2));
        when(sanPhamChiTietRepository.findActiveCandidatesBySanPhamId(3)).thenReturn(List.of(spct1));
        when(sanPhamChiTietRepository.findActiveCandidatesBySanPhamId(8)).thenReturn(List.of(spct2));

        AllocationResult result = inventoryLotService.allocateFifo(List.of(req1, req2));

        assertEquals(AllocationStatus.INSUFFICIENT_STOCK, result.status());
        assertEquals(10, spct1.getSoLuongTon()); // Vẫn giữ nguyên 10, không bị trừ
        assertEquals(5, spct2.getSoLuongTon());  // Vẫn giữ nguyên 5
    }

    @Test
    @DisplayName("Test 7: Gom nhóm biến thể tổng hợp theo AttributeKey")
    void testCalculateAggregatedVariants() {
        when(sanPhamChiTietRepository.findBySanPham_Id(3)).thenReturn(List.of(spct1));

        List<VariantGroupDTO> groups = inventoryLotService.calculateAggregatedVariants(3);

        assertEquals(1, groups.size());
        assertEquals(101, groups.get(0).getRepresentativeSpctId());
        assertEquals(10, groups.get(0).getTongSoLuongTon());
    }
}
