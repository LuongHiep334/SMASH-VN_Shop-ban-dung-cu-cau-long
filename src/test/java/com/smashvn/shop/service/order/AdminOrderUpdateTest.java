package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AdminOrderUpdateTest {

    @Autowired
    private OrderViewService orderViewService;



    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private EditLogRepository editLogRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private TaiKhoan adminUser;
    private TaiKhoan staffUser;
    private TaiKhoan customerUser;
    private KhachHang testKhachHang;
    private PhuongThucThanhToan ptttCOD;
    private PhuongThucThanhToan ptttOnline;
    private DonViVanChuyen testDvvc;
    private SanPhamChiTiet testSpct;

    @BeforeEach
    void setUp() {
        // Create accounts
        adminUser = new TaiKhoan();
        adminUser.setUsername("admin-test-" + System.nanoTime() + "@smashvn.com");
        adminUser.setMatKhau("123");
        adminUser.setVaiTro("QL");

        adminUser = taiKhoanRepository.save(adminUser);

        staffUser = new TaiKhoan();
        staffUser.setUsername("staff-test-" + System.nanoTime() + "@smashvn.com");
        staffUser.setMatKhau("123");
        staffUser.setVaiTro("NV");

        staffUser = taiKhoanRepository.save(staffUser);

        customerUser = new TaiKhoan();
        customerUser.setUsername("customer-test-" + System.nanoTime() + "@smashvn.com");
        customerUser.setMatKhau("123");
        customerUser.setVaiTro("KH");

        customerUser = taiKhoanRepository.save(customerUser);

        // Fetch / Create customer
        List<KhachHang> khs = khachHangRepository.findAll();
        if (khs.isEmpty()) {
            KhachHang newKh = new KhachHang();
            newKh.setHoKh("Test");
            newKh.setTenKh("Customer");
            newKh.setSoDienThoaiKh("0912345678");
            newKh.setLaTaiKhoanNoiBo(false);
            newKh.setTaiKhoan(customerUser);
            testKhachHang = khachHangRepository.save(newKh);
        } else {
            testKhachHang = khs.get(0);
        }

        // Fetch / Create Payment Methods
        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        ptttCOD = ptts.stream()
                .filter(p -> "COD".equalsIgnoreCase(p.getTenPhuongThuc()))
                .findFirst()
                .orElseGet(() -> {
                    PhuongThucThanhToan p = new PhuongThucThanhToan();
                    p.setTenPhuongThuc("COD");
                    return phuongThucThanhToanDAO.save(p);
                });

        ptttOnline = ptts.stream()
                .filter(p -> !"COD".equalsIgnoreCase(p.getTenPhuongThuc()))
                .findFirst()
                .orElseGet(() -> {
                    PhuongThucThanhToan p = new PhuongThucThanhToan();
                    p.setTenPhuongThuc("SEPAY");
                    return phuongThucThanhToanDAO.save(p);
                });

        // Fetch / Create shipping carrier
        List<DonViVanChuyen> dvvcs = donViVanChuyenDAO.findAll();
        if (dvvcs.isEmpty()) {
            DonViVanChuyen d = new DonViVanChuyen();
            d.setTenDonVi("GHTK");
            d.setHotline("19001000");
            d.setWebsite("ghtk.vn");
            testDvvc = donViVanChuyenDAO.save(d);
        } else {
            testDvvc = dvvcs.get(0);
        }

        // Seed or fetch NhanVien
        TaiKhoan nvUser = new TaiKhoan();
        nvUser.setUsername("staff-nv-" + System.nanoTime() + "@smashvn.com");
        nvUser.setMatKhau("123");
        nvUser.setVaiTro("NV");

        nvUser = taiKhoanRepository.save(nvUser);
        NhanVien nv = new NhanVien();
        nv.setTaiKhoan(nvUser);
        nv.setHoTenNv("Test Staff");
        nv.setChucVu("Nhân viên");
        nv.setSoDienThoaiNv("0999888777");
        nv = nhanVienRepository.save(nv);

        // Seed or fetch DanhMuc / ThuongHieu / SanPham / SanPhamChiTiet
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc d = new DanhMuc();
            d.setTenDanhMuc("Mặc định");
            return danhMucRepository.save(d);
        });
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu t = new ThuongHieu();
            t.setTenThuongHieu("Mặc định");
            return thuongHieuRepository.save(t);
        });
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Admin " + System.nanoTime());
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(nv);
        sp = sanPhamRepository.save(sp);
        testSpct = new SanPhamChiTiet();
        testSpct.setSanPham(sp);
        testSpct.setMauSac("Đen");
        testSpct.setSoLuongTon(100);
        testSpct.setGiaBan(BigDecimal.valueOf(500000));
        testSpct = sanPhamChiTietRepository.save(testSpct);
    }

    private HoaDon createTestOrder(String status, String paymentMethod, String paymentStatus, PhuongThucThanhToan pttt, int qty) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(testDvvc);
        hd.setNgayTao(LocalDateTime.now());
        hd.setTongTien(BigDecimal.valueOf(100000));
        hd.setMaDonHang("TEST-DHSVN-" + System.nanoTime());
        hd.setTrangThaiDonHang(status);
        hd.setPaymentMethod(paymentMethod);
        hd.setPaymentStatus(paymentStatus);
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        } else if ("cancelled".equalsIgnoreCase(paymentStatus)) {
            hd.setTrangThaiThanhToan("CANCELLED");
        } else if ("refunded".equalsIgnoreCase(paymentStatus)) {
            hd.setTrangThaiThanhToan("REFUNDED");
        } else {
            hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
        }
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(qty);
        hdct.setDonGia(testSpct.getGiaBan() != null ? testSpct.getGiaBan() : BigDecimal.valueOf(100000));
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }

    @Test
    void testInvalidTransitionsBlocked() {
        HoaDon hd = createTestOrder("da_giao", "COD", "paid", ptttCOD, 1);

        // Transition from da_giao is forbidden
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hd.getId(), "cho_thanh_toan", "da_giao", adminUser.getId(), "127.0.0.1");
        });

        // Invalid step transition: cho_thanh_toan -> dang_giao is forbidden
        HoaDon hd2 = createTestOrder("cho_thanh_toan", "SEPAY", "pending", ptttOnline, 1);
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hd2.getId(), "dang_giao", "cho_thanh_toan", adminUser.getId(), "127.0.0.1");
        });
    }

    @Test
    void testCODOrderDeliveredPaymentUpdate() {
        HoaDon hd = createTestOrder("dang_giao", "COD", "pending", ptttCOD, 1);

        orderViewService.applyShippingStatus(hd.getId(), "da_giao", "delivered");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("da_giao", updated.getTrangThaiDonHang());
        assertEquals("paid", updated.getPaymentStatus());
        assertEquals("DA_THANH_TOAN", updated.getTrangThaiThanhToan());
        assertNotNull(updated.getPaidAt());
    }

    @Test
    void testPaidSePayOrderCancelledRefundRequired() {
        HoaDon hd = createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, 1);

        orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_huy", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("da_huy", updated.getTrangThaiDonHang());
        // For paid online orders, payment status is NOT changed to CANCELLED, and payment details are preserved
        assertEquals("paid", updated.getPaymentStatus());
        assertEquals("CHO_HOAN_TIEN", updated.getTrangThaiThanhToan());
    }

    @Test
    void testDoubleDeductionProtection() {
        // Status changes: da_xac_nhan (deducted) -> dang_chuan_bi_hang (deducted) -> san_sang_giao (deducted)
        int initialStock = testSpct.getSoLuongTon();
        HoaDon hd = createTestOrder("da_xac_nhan", "COD", "pending", ptttCOD, 2);

        // Simulate that when order moved to da_xac_nhan, stock was deducted by 2
        testSpct.setSoLuongTon(initialStock - 2);
        sanPhamChiTietRepository.saveAndFlush(testSpct);

        // Move to dang_chuan_bi_hang (deducted -> deducted, no double deduction)
        orderViewService.updateOrderStatusByAdmin(hd.getId(), "dang_chuan_bi_hang", "da_xac_nhan", adminUser.getId(), "127.0.0.1");

        SanPhamChiTiet updatedSpct = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertNotNull(updatedSpct);
        assertEquals(initialStock - 2, updatedSpct.getSoLuongTon());

        // Move to san_sang_giao (deducted -> deducted, no double deduction)
        orderViewService.updateOrderStatusByAdmin(hd.getId(), "san_sang_giao", "dang_chuan_bi_hang", adminUser.getId(), "127.0.0.1");
        updatedSpct = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertNotNull(updatedSpct);
        assertEquals(initialStock - 2, updatedSpct.getSoLuongTon());
    }

    @Test
    void testDoubleRestockProtection() {
        // cho_thanh_toan -> da_huy (Both belong to Not Deducted group)
        int initialStock = testSpct.getSoLuongTon();
        HoaDon hd = createTestOrder("cho_thanh_toan", "SEPAY", "pending", ptttOnline, 2);

        // Move to da_huy
        orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_huy", "cho_thanh_toan", adminUser.getId(), "127.0.0.1");

        SanPhamChiTiet updatedSpct = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertNotNull(updatedSpct);
        // Stock should not be incremented because no stock was deducted initially
        assertEquals(initialStock, updatedSpct.getSoLuongTon());
    }

    @Test
    void testDeliveredOrderImmutable() {
        HoaDon hd = createTestOrder("da_giao", "COD", "paid", ptttCOD, 1);

        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_huy", "da_giao", adminUser.getId(), "127.0.0.1");
        });
    }

    @Test
    void testInsufficientStockReactivationRejected() {
        // Change stock to 1
        testSpct.setSoLuongTon(1);
        sanPhamChiTietRepository.saveAndFlush(testSpct);

        // Order needs 2 items, initially in cho_xac_nhan status (stock not yet deducted)
        HoaDon hd = createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, 2);

        // Moving to da_xac_nhan (STOCK_NOT_DEDUCTED -> STOCK_DEDUCTED) should fail because stock is 1 < 2
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_xac_nhan", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");
        });
    }

    @Test
    void testLostUpdateRejected() {
        HoaDon hd = createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, 1);

        // Admin B updates status to da_xac_nhan
        orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_xac_nhan", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");

        // Admin A submits with stale status expectedStatus = cho_xac_nhan
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hd.getId(), "dang_giao", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");
        });
    }

    @Test
    void testUnauthorizedServiceCallRejected() {
        HoaDon hd = createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, 1);

        // Invoke service with customer user ID (vaiTro = KH, not QL/NV)
        assertThrows(AccessDeniedException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_xac_nhan", "cho_xac_nhan", customerUser.getId(), "127.0.0.1");
        });

        // Invoke with null user ID
        assertThrows(AccessDeniedException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_xac_nhan", "cho_xac_nhan", null, "127.0.0.1");
        });
    }

    @Test
    void testStockConflictRecovery() {
        // Set stock to 0
        testSpct.setSoLuongTon(0);
        sanPhamChiTietRepository.saveAndFlush(testSpct);

        // Order is in cho_xac_nhan status
        HoaDon hd = createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, 2);

        // Transition from cho_xac_nhan to da_xac_nhan should fail when stock is 0
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_xac_nhan", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");
        });

        // Add stock back to 5
        testSpct.setSoLuongTon(5);
        sanPhamChiTietRepository.saveAndFlush(testSpct);

        // Transition should now succeed and deduct stock
        orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_xac_nhan", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("da_xac_nhan", updated.getTrangThaiDonHang());

        SanPhamChiTiet updatedSpct = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertNotNull(updatedSpct);
        assertEquals(3, updatedSpct.getSoLuongTon()); // 5 - 2 = 3
    }

    @Test
    void testOnlinePaymentDeliveredDoesNotOverwritePayment() {
        // Online paid order
        HoaDon hd = createTestOrder("dang_giao", "SEPAY", "paid", ptttOnline, 1);
        hd.setTransactionId("TX-12345");
        hd.setMaGiaoDich("TX-12345");
        hd.setPaidAt(LocalDateTime.now().minusHours(1));
        hd = hoaDonRepository.save(hd);

        orderViewService.applyShippingStatus(hd.getId(), "da_giao", "delivered");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("da_giao", updated.getTrangThaiDonHang());
        assertEquals("paid", updated.getPaymentStatus());
        assertEquals("DA_THANH_TOAN", updated.getTrangThaiThanhToan());
        assertEquals("TX-12345", updated.getTransactionId());
        assertEquals("TX-12345", updated.getMaGiaoDich());
    }

    @Test
    void testDangLayHangTransition() {
        HoaDon hd = createTestOrder("da_xac_nhan", "COD", "pending", ptttCOD, 1);

        // Transition from da_xac_nhan to dang_lay_hang
        orderViewService.updateOrderStatusByAdmin(hd.getId(), "dang_lay_hang", "da_xac_nhan", adminUser.getId(), "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("dang_lay_hang", updated.getTrangThaiDonHang());

        // Transition from dang_lay_hang to dang_giao
        orderViewService.updateOrderStatusByAdmin(updated.getId(), "dang_giao", "dang_lay_hang", adminUser.getId(), "127.0.0.1");
        updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("dang_giao", updated.getTrangThaiDonHang());
    }

    @Test
    void testMoveOrderToNextStatusProgression() {
        // Set ample stock to prevent stock conflict error when activating order
        testSpct.setSoLuongTon(100);
        sanPhamChiTietRepository.save(testSpct);

        HoaDon hd = createTestOrder("cho_thanh_toan", "COD", "pending", ptttCOD, 1);

        // 1. cho_thanh_toan -> cho_xac_nhan
        orderViewService.moveOrderToNextStatus(hd.getId(), adminUser.getId(), "127.0.0.1");
        hd = hoaDonRepository.findById(hd.getId()).get();
        assertEquals("cho_xac_nhan", hd.getTrangThaiDonHang());

        // 2. cho_xac_nhan -> da_xac_nhan
        orderViewService.moveOrderToNextStatus(hd.getId(), adminUser.getId(), "127.0.0.1");
        hd = hoaDonRepository.findById(hd.getId()).get();
        assertEquals("da_xac_nhan", hd.getTrangThaiDonHang());

        // 3. da_xac_nhan -> dang_chuan_bi_hang
        orderViewService.moveOrderToNextStatus(hd.getId(), adminUser.getId(), "127.0.0.1");
        hd = hoaDonRepository.findById(hd.getId()).get();
        assertEquals("dang_chuan_bi_hang", hd.getTrangThaiDonHang());

        // 4. dang_chuan_bi_hang -> san_sang_giao
        orderViewService.moveOrderToNextStatus(hd.getId(), adminUser.getId(), "127.0.0.1");
        hd = hoaDonRepository.findById(hd.getId()).get();
        assertEquals("san_sang_giao", hd.getTrangThaiDonHang());

        // 5. san_sang_giao without ghnOrderCode -> moveOrderToNextStatus MUST be blocked
        final Integer hdId = hd.getId();
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.moveOrderToNextStatus(hdId, adminUser.getId(), "127.0.0.1");
        });

        // 6. After GHN creates shipment code, order moves to da_tao_van_don_ghn
        hd.setGhnOrderCode("DEMO-GHN-TEST-123");
        hd.setGhnStatus("ready_to_pick");
        hd.setTrangThaiDonHang("da_tao_van_don_ghn");
        hd = hoaDonRepository.save(hd);
        assertEquals("da_tao_van_don_ghn", hd.getTrangThaiDonHang());
        assertNotNull(hd.getGhnOrderCode());

        // 7. Once at da_tao_van_don_ghn, order has GHN order code, manual transition is locked
        final Integer orderId = hd.getId();
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.moveOrderToNextStatus(orderId, adminUser.getId(), "127.0.0.1");
        });
    }

    @Test
    void testDaBanGiaoGhnIsLockedForManualUpdate() {
        HoaDon hd = createTestOrder("da_ban_giao_ghn", "COD", "pending", ptttCOD, 1);
        final Integer orderId = hd.getId();

        // 1. Trying to move to next status via moveOrderToNextStatus must fail
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.moveOrderToNextStatus(orderId, adminUser.getId(), "127.0.0.1");
        });

        // 2. Trying to update status via updateOrderStatusByAdmin must fail
        assertThrows(IllegalArgumentException.class, () -> {
            orderViewService.updateOrderStatusByAdmin(orderId, "da_giao", "da_ban_giao_ghn", adminUser.getId(), "127.0.0.1");
        });
    }

    @Test
    void testCustomerPaidOrderCancelledRefundRequired() {
        HoaDon hd = createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, 1);

        boolean success = orderViewService.huyDonHang(hd.getId(), testKhachHang.getId(), "127.0.0.1");
        assertTrue(success);

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("da_huy", updated.getTrangThaiDonHang());
        assertEquals("paid", updated.getPaymentStatus());
        assertEquals("CHO_HOAN_TIEN", updated.getTrangThaiThanhToan());
    }

    @Test
    void testApproveAndRejectRefund() {
        HoaDon hd = createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, 1);

        // Cancel order using admin update, which sets CHO_HOAN_TIEN and generates token
        orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_huy", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");

        HoaDon cancelled = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(cancelled);
        assertEquals("da_huy", cancelled.getTrangThaiDonHang());
        assertEquals("CHO_HOAN_TIEN", cancelled.getTrangThaiThanhToan());

        // Extract token
        String response = cancelled.getGatewayResponse();
        assertNotNull(response);
        assertTrue(response.contains("REFUND_TOKEN:"));
        int start = response.indexOf("REFUND_TOKEN:") + 13;
        int end = response.indexOf(";", start);
        if (end == -1) end = response.length();
        String token = response.substring(start, end);
        assertFalse(token.isEmpty());

        // Test approve refund
        orderViewService.approveRefund(hd.getId(), token, adminUser.getId(), "127.0.0.1");
        
        HoaDon approved = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(approved);
        assertEquals("REFUNDED", approved.getPaymentStatus());
        assertEquals("REFUNDED", approved.getTrangThaiThanhToan());
        assertFalse(approved.getGatewayResponse().contains("REFUND_TOKEN:"));

        // Setup for reject refund
        HoaDon hd2 = createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, 1);
        orderViewService.updateOrderStatusByAdmin(hd2.getId(), "da_huy", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");

        HoaDon cancelled2 = hoaDonRepository.findById(hd2.getId()).orElse(null);
        assertNotNull(cancelled2);
        String response2 = cancelled2.getGatewayResponse();
        int start2 = response2.indexOf("REFUND_TOKEN:") + 13;
        int end2 = response2.indexOf(";", start2);
        if (end2 == -1) end2 = response2.length();
        String token2 = response2.substring(start2, end2);

        // Test reject refund
        orderViewService.rejectRefund(hd2.getId(), token2, adminUser.getId(), "127.0.0.1");

        HoaDon rejected = hoaDonRepository.findById(hd2.getId()).orElse(null);
        assertNotNull(rejected);
        assertEquals("paid", rejected.getPaymentStatus());
        assertEquals("DA_THANH_TOAN", rejected.getTrangThaiThanhToan());
        assertFalse(rejected.getGatewayResponse().contains("REFUND_TOKEN:"));
    }

    @Test
    void testAdminConfirmationSetsThoiGianXacNhan() {
        HoaDon hd = createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, 1);
        assertNull(hd.getThoiGianXacNhan());

        orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_xac_nhan", "cho_xac_nhan", adminUser.getId(), "127.0.0.1");

        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertNotNull(updated.getThoiGianXacNhan());
    }

    @Test
    void testTransactionQueryIncludesCancelledPaidOrders() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        long initialCount = hoaDonRepository.countTransactionsInPeriod(start, end);

        // 1. Create a paid order
        HoaDon hd1 = createTestOrder("cho_xac_nhan", "SEPAY", "paid", ptttOnline, 1);
        hd1.setPaidAt(LocalDateTime.now());
        hoaDonRepository.save(hd1);

        // 2. Create a cancelled order that was paid (marked CHO_HOAN_TIEN)
        HoaDon hd2 = createTestOrder("da_huy", "SEPAY", "paid", ptttOnline, 1);
        hd2.setTrangThaiThanhToan("CHO_HOAN_TIEN");
        hd2.setPaidAt(LocalDateTime.now());
        hoaDonRepository.save(hd2);

        // 3. Create a cancelled order that was paid (paymentStatus is CANCELLED but paidAt is set)
        HoaDon hd3 = createTestOrder("da_huy", "SEPAY", "cancelled", ptttOnline, 1);
        hd3.setPaidAt(LocalDateTime.now());
        hoaDonRepository.save(hd3);

        // 4. Create an unpaid cancelled order (should NOT be included)
        HoaDon hd4 = createTestOrder("da_huy", "COD", "cancelled", ptttCOD, 1);
        hoaDonRepository.save(hd4);

        long newCount = hoaDonRepository.countTransactionsInPeriod(start, end);
        assertEquals(initialCount + 3, newCount);

        List<Object[]> rawTransactions = hoaDonRepository.findRawTransactionsInPeriod(start, end, org.springframework.data.domain.Pageable.unpaged());
        boolean hasHd1 = false, hasHd2 = false, hasHd3 = false, hasHd4 = false;
        for (Object[] row : rawTransactions) {
            Integer id = (Integer) row[0];
            if (id.equals(hd1.getId())) hasHd1 = true;
            if (id.equals(hd2.getId())) hasHd2 = true;
            if (id.equals(hd3.getId())) hasHd3 = true;
            if (id.equals(hd4.getId())) hasHd4 = true;
        }

        assertTrue(hasHd1);
        assertTrue(hasHd2);
        assertTrue(hasHd3);
        assertFalse(hasHd4);
    }

    @Test
    void testCancellationReasonSavingAndAppending() {
        // 1. Customer cancellation reason
        HoaDon hd1 = createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, 1);
        hd1.setGhiChu("Ghi chú ban đầu");
        hoaDonRepository.save(hd1);

        boolean success = orderViewService.huyDonHang(hd1.getId(), testKhachHang.getId(), "127.0.0.1", "Muốn đổi sản phẩm khác");
        assertTrue(success);

        HoaDon updated1 = hoaDonRepository.findById(hd1.getId()).orElse(null);
        assertNotNull(updated1);
        assertEquals("Ghi chú ban đầu\nLý do hủy: Muốn đổi sản phẩm khác", updated1.getGhiChu());

        // 2. Admin cancellation reason
        HoaDon hd2 = createTestOrder("cho_xac_nhan", "COD", "pending", ptttCOD, 1);
        hd2.setGhiChu("Ghi chú cũ");
        hoaDonRepository.save(hd2);

        orderViewService.updateOrderStatusByAdmin(hd2.getId(), "da_huy", "cho_xac_nhan", adminUser.getId(), "127.0.0.1", "Sản phẩm hết hàng");

        HoaDon updated2 = hoaDonRepository.findById(hd2.getId()).orElse(null);
        assertNotNull(updated2);
        assertEquals("Ghi chú cũ\nLý do hủy: Sản phẩm hết hàng", updated2.getGhiChu());
    }

    @org.junit.jupiter.api.Test
    void testGhnWebhookExceptionAndRefundStatus() {
        HoaDon hd = createTestOrder("dang_giao", "SEPAY", "paid", ptttOnline, 2);
        
        // Call webhook with exception
        orderViewService.applyShippingStatus(hd.getId(), "da_huy", "exception");
        
        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("da_huy", updated.getTrangThaiDonHang());
        assertEquals(ReturnStatus.PENDING_RETURN, updated.getTrangThaiHoanHang());
        assertEquals(RefundStatus.PENDING, updated.getRefundStatus());
    }

    @org.junit.jupiter.api.Test
    void testDuplicateWebhookIdempotency() {
        HoaDon hd = createTestOrder("dang_giao", "SEPAY", "paid", ptttOnline, 2);
        
        // Call webhook with lost twice
        orderViewService.applyShippingStatus(hd.getId(), "da_huy", "lost");
        int logCount1 = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", hd.getId()).size();
        
        // Second call
        orderViewService.applyShippingStatus(hd.getId(), "da_huy", "lost");
        int logCount2 = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", hd.getId()).size();
        
        // Log count should not increase
        assertEquals(logCount1, logCount2);
    }

    @org.junit.jupiter.api.Test
    void testInvalidReturnStatusTransitions() {
        HoaDon hd = createTestOrder("dang_giao", "SEPAY", "paid", ptttOnline, 2);
        orderViewService.applyShippingStatus(hd.getId(), "da_huy", "lost"); // Set to LOST
        
        HoaDon updated = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertEquals(ReturnStatus.LOST, updated.getTrangThaiHoanHang());
        
        // Attempting to move from LOST to RETURNED should throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.updateReturnStatusByAdmin(hd.getId(), "RETURNED", adminUser.getId(), "127.0.0.1");
        });
        
        // Check that a WARNING log was generated
        List<EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", hd.getId());
        boolean hasWarning = logs.stream().anyMatch(log -> log.getGhiChu() != null && log.getGhiChu().contains("[WARNING]"));
        assertTrue(hasWarning);
    }

    @org.junit.jupiter.api.Test
    void testDetailedInventoryAdjustmentLogAndRestoration() {
        // Fetch fresh copy of testSpct to get its initial stock in transactional boundary
        SanPhamChiTiet freshSpct = sanPhamChiTietRepository.findById(testSpct.getId()).get();
        int initialStock = freshSpct.getSoLuongTon();
        HoaDon hd = createTestOrder("dang_giao", "SEPAY", "paid", ptttOnline, 2);
        
        // Cancel order -> PENDING_RETURN (stock not restored yet)
        orderViewService.applyShippingStatus(hd.getId(), "da_huy", "return");
        
        SanPhamChiTiet spctPending = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertEquals(initialStock, spctPending.getSoLuongTon()); // Not restored
        
        // Confirm returned
        orderViewService.updateReturnStatusByAdmin(hd.getId(), "RETURNED", adminUser.getId(), "127.0.0.1");
        
        SanPhamChiTiet spctReturned = sanPhamChiTietRepository.findById(testSpct.getId()).orElse(null);
        assertEquals(initialStock + 2, spctReturned.getSoLuongTon()); // Restored
        
        // Verify detailed audit log contents
        List<EditLog> logs = editLogRepository.findByTenBangAndIdBanGhiOrderByThoiGianAsc("HoaDon", hd.getId());
        EditLog returnLog = logs.stream()
                .filter(l -> l.getGhiChu() != null && l.getGhiChu().contains("[WAREHOUSE_RETURN_RETURNED]"))
                .findFirst().orElse(null);
        assertNotNull(returnLog);
        assertTrue(returnLog.getGhiChu().contains("SPCT-" + testSpct.getId() + " : +2"));
        
        // Second call should throw Exception (Idempotency / transition lock)
        assertThrows(IllegalStateException.class, () -> {
            orderViewService.updateReturnStatusByAdmin(hd.getId(), "RETURNED", adminUser.getId(), "127.0.0.1");
        });
    }

    @org.junit.jupiter.api.Test
    void testRefundAndInventoryIsolation() {
        SanPhamChiTiet freshSpct = sanPhamChiTietRepository.findById(testSpct.getId()).get();
        int initialStock = freshSpct.getSoLuongTon();
        HoaDon hd = createTestOrder("da_xac_nhan", "SEPAY", "paid", ptttOnline, 2);

        // Simulate that when order moved to da_xac_nhan, stock was deducted by 2
        testSpct.setSoLuongTon(initialStock - 2);
        sanPhamChiTietRepository.saveAndFlush(testSpct);

        // Cancel order -> CHO_HOAN_TIEN (since it was da_xac_nhan and not shipped, it restores stock immediately)
        orderViewService.updateOrderStatusByAdmin(hd.getId(), "da_huy", "da_xac_nhan", adminUser.getId(), "127.0.0.1");

        // Since it wasn't shipped but was in a deducted state, cancellation restores the stock to initialStock
        SanPhamChiTiet spctCancelled = sanPhamChiTietRepository.findById(testSpct.getId()).get();
        assertEquals(initialStock, spctCancelled.getSoLuongTon());
        
        HoaDon cancelled = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(cancelled);
        assertEquals(RefundStatus.PENDING, cancelled.getRefundStatus());
        assertNull(cancelled.getTrangThaiHoanHang()); // null because not shipped
        
        String response = cancelled.getGatewayResponse();
        int start = response.indexOf("REFUND_TOKEN:") + 13;
        int end = response.indexOf(";", start);
        if (end == -1) end = response.length();
        String token = response.substring(start, end);
        
        // Approve Refund
        orderViewService.approveRefund(hd.getId(), token, adminUser.getId(), "127.0.0.1");
        
        HoaDon refunded = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertEquals(RefundStatus.COMPLETED, refunded.getRefundStatus());
        assertNotNull(refunded.getRefundTime());
        
        // Stock should remain unchanged when refund is confirmed (still initialStock)
        SanPhamChiTiet spctRefunded = sanPhamChiTietRepository.findById(testSpct.getId()).get();
        assertEquals(initialStock, spctRefunded.getSoLuongTon());
    }
}
