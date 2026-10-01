package com.smashvn.shop.service;

import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.order.OrderViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class Phase6ProviderAndDeliveryAuditTest {

    @Autowired
    private OrderViewService orderViewService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

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
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private EditLogRepository editLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private KhachHang testKh;
    private PhuongThucThanhToan testPt;
    private SanPhamChiTiet testSpct;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM EditLog WHERE ten_bang = 'HoaDon' AND id_ban_ghi IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_AUDIT_%')");
        jdbcTemplate.execute("DELETE FROM TichHopVanChuyen WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_AUDIT_%')");
        jdbcTemplate.execute("DELETE FROM HoaDonChiTiet WHERE id_hoa_don IN (SELECT id FROM HoaDon WHERE ghi_chu LIKE 'TEST_AUDIT_%')");
        jdbcTemplate.execute("DELETE FROM HoaDon WHERE ghi_chu LIKE 'TEST_AUDIT_%'");

        testKh = khachHangRepository.findAll().stream().findFirst().orElse(null);
        testPt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);

        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseThrow();
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseThrow();
        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseThrow();

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Audit " + System.currentTimeMillis());
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nv);
        sp.setMoTa("Test Audit");
        sp.setTrangThai("dang_ban");
        sp = sanPhamRepository.save(sp);

        testSpct = new SanPhamChiTiet();
        testSpct.setSanPham(sp);
        testSpct.setSoLuongTon(10);
        testSpct.setGiaBan(new BigDecimal("500000"));
        testSpct.setTrangThai("dang_ban");
        testSpct = sanPhamChiTietRepository.save(testSpct);
    }

    private HoaDon createTestOrder() {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setPhuongThucThanhToan(testPt);
        hd.setNgayTao(LocalDateTime.now().minusDays(3));
        hd.setNgayThanhToan(LocalDateTime.now().minusDays(3));
        hd.setTongTienHang(new BigDecimal("500000"));
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setSoTienGiamVoucher(BigDecimal.ZERO);
        hd.setTongTien(new BigDecimal("500000"));
        hd.setTrangThaiDonHang("cho_xac_nhan");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setTenNguoiNhan("Khách Test Audit");
        hd.setSdtNhan("0987654321");
        hd.setDiaChiNhan("123 Đường Audit, Hà Nội");
        hd.setGhiChu("TEST_AUDIT_" + System.currentTimeMillis() + "_" + (int)(Math.random()*1000));
        hd = hoaDonRepository.save(hd);

        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(testSpct);
        hdct.setSoLuong(1);
        hdct.setDonGia(testSpct.getGiaBan());
        hoaDonChiTietRepository.save(hdct);

        return hd;
    }

    @Test
    @DisplayName("Test Provider Isolation: Đơn có cả GHN, GHN_RETURN, GHN_EXCHANGE -> Phân lập 100%")
    void testProviderIsolation() {
        HoaDon hd = createTestOrder();

        // 1. Insert 3 shipments with different providers into TichHopVanChuyen
        jdbcTemplate.update(
                "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) VALUES (?, 'GHN', 'OUTBOUND-001', 'OUTBOUND-001', 'delivering', GETDATE())",
                hd.getId()
        );
        jdbcTemplate.update(
                "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) VALUES (?, 'GHN_RETURN', 'RETURN-001', 'RETURN-001', 'picked', GETDATE())",
                hd.getId()
        );
        jdbcTemplate.update(
                "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_don_hang_ngoai, ma_van_don, trang_thai, ngay_tao) VALUES (?, 'GHN_EXCHANGE', 'EXCHANGE-001', 'EXCHANGE-001', 'ready_to_pick', GETDATE())",
                hd.getId()
        );

        // Reload entity to check Hibernate @Formula fields
        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElseThrow();

        // Verify Formula & resolve helpers return provider-isolated values
        assertEquals("RETURN-001", reloaded.getGhnReturnOrderCode(), "Formula ghnReturnOrderCode phải trả về mã RETURN-001");
        assertEquals("delivering", reloaded.getGhnStatus(), "Formula ghnStatus phải trả về trạng thái của vận đơn GHN bán xuôi");

        String resolvedReturnCode = orderViewService.resolveGhnReturnOrderCode(hd.getId(), reloaded);
        assertEquals("RETURN-001", resolvedReturnCode, "resolveGhnReturnOrderCode phải trả về mã RETURN-001");

        // Verify findActiveShippingOrders only considers outbound GHN shipment
        List<HoaDon> activeOrders = hoaDonRepository.findActiveShippingOrders(PageRequest.of(0, 10));
        assertTrue(activeOrders.stream().anyMatch(o -> o.getId().equals(hd.getId())), "Đơn bán xuôi dang_giao với provider GHN phải xuất hiện trong findActiveShippingOrders");
    }

    @Test
    @DisplayName("Test 7-Day Delivery Timestamp Source Guard")
    void testDeliveryTimestampSourceGuard() {
        HoaDon hd = createTestOrder();
        hd.setTrangThaiDonHang("DA_GIAO");
        hoaDonRepository.save(hd);

        // Case A: Has 'da_giao' in EditLog -> Use 'da_giao'
        EditLog log1 = new EditLog();
        log1.setTenBang("HoaDon");
        log1.setIdBanGhi(hd.getId());
        log1.setHanhDong("UPDATE");
        log1.setGiaTriCu("trangThaiDonHang=dang_giao");
        log1.setGiaTriMoi("trangThaiDonHang=da_giao");
        log1.setThoiGian(LocalDateTime.now().minusDays(2));
        log1.setVaiTroThucHien("SYSTEM");
        editLogRepository.save(log1);

        LocalDateTime deliveredAt = orderViewService.getDeliveredTimestamp(hd);
        assertNotNull(deliveredAt, "Phải lấy được deliveredAt từ EditLog da_giao");

        // Case B: No da_giao, but has hoan_thanh WITH Customer confirmation -> Allowed
        jdbcTemplate.execute("DELETE FROM EditLog WHERE id_ban_ghi = " + hd.getId() + " AND ten_bang = 'HoaDon'");
        EditLog log2 = new EditLog();
        log2.setTenBang("HoaDon");
        log2.setIdBanGhi(hd.getId());
        log2.setHanhDong("UPDATE");
        log2.setGiaTriCu("trangThaiDonHang=da_giao");
        log2.setGiaTriMoi("trangThaiDonHang=hoan_thanh");
        log2.setThoiGian(LocalDateTime.now().minusDays(1));
        log2.setVaiTroThucHien("KHACH_HANG");
        log2.setGhiChu("Khách hàng xác nhận đã nhận được hàng.");
        editLogRepository.save(log2);

        LocalDateTime customerDeliveredAt = orderViewService.getDeliveredTimestamp(hd);
        assertNotNull(customerDeliveredAt, "Phải chấp nhận hoan_thanh do KHACH_HANG xác nhận");

        // Case C: No da_giao, and hoan_thanh is by ADMIN/SYSTEM without customer confirmation -> Reject (return null)
        jdbcTemplate.execute("DELETE FROM EditLog WHERE id_ban_ghi = " + hd.getId() + " AND ten_bang = 'HoaDon'");
        EditLog log3 = new EditLog();
        log3.setTenBang("HoaDon");
        log3.setIdBanGhi(hd.getId());
        log3.setHanhDong("UPDATE");
        log3.setGiaTriCu("trangThaiDonHang=da_giao");
        log3.setGiaTriMoi("trangThaiDonHang=hoan_thanh");
        log3.setThoiGian(LocalDateTime.now().minusDays(1));
        log3.setVaiTroThucHien("ADMIN");
        log3.setGhiChu("Admin tự chuyển trạng thái.");
        editLogRepository.save(log3);

        LocalDateTime adminDeliveredAt = orderViewService.getDeliveredTimestamp(hd);
        assertNull(adminDeliveredAt, "Không được dùng hoan_thanh do ADMIN chuyển mà không có khách xác nhận");
    }

    @Test
    @DisplayName("Test PENDING_RETURN Status Guard: PENDING_RETURN bị reject khi gọi duyetYeuCauTraHangVaTaoDonGhn")
    void testPendingReturnRejection() {
        HoaDon hd = createTestOrder();
        hd.setTrangThaiHoanHang(ReturnStatus.PENDING_RETURN);
        hoaDonRepository.save(hd);

        TaiKhoan adminAcc = taiKhoanRepository.findAll().stream().findFirst().orElseThrow();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                orderViewService.duyetYeuCauTraHangVaTaoDonGhn(hd.getId(), adminAcc.getId(), "127.0.0.1")
        );
        assertTrue(ex.getMessage().contains("Chỉ yêu cầu đang ở trạng thái Chờ duyệt (PENDING_APPROVAL) mới có thể thực hiện thao tác duyệt."));
    }
}
