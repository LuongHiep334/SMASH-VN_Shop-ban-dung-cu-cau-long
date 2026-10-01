package com.smashvn.shop.service;

import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.order.OrderViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GhnReturnProviderIsolationTest {

    @Autowired
    private OrderViewService orderViewService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private KhachHang testKhachHang;
    private PhuongThucThanhToan testPhuongThuc;

    @BeforeEach
    public void setUp() {
        testKhachHang = khachHangRepository.findAll().stream().findFirst().orElse(null);
        testPhuongThuc = phuongThucThanhToanDAO.findAll().stream().findFirst().orElse(null);
    }

    private HoaDon createBaseHoaDon(String name) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKhachHang);
        hd.setPhuongThucThanhToan(testPhuongThuc);
        hd.setTenNguoiNhan(name);
        hd.setSdtNhan("0900000000");
        hd.setDiaChiNhan("123 Duong ABC, Quan 1, TP HCM");
        hd.setTrangThaiDonHang("dang_giao");
        return hoaDonRepository.save(hd);
    }

    @Test
    @DisplayName("Verify provider isolation: GHN vs GHN_RETURN vs GHN_EXCHANGE")
    public void testProviderIsolation() {
        HoaDon hd = createBaseHoaDon("Test Provider Isolation");
        Integer idHoaDon = hd.getId();

        // 1. Insert 3 separate shipments into TichHopVanChuyen
        jdbcTemplate.update(
            "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_van_don, ma_don_hang_ngoai, trang_thai, ngay_tao) VALUES (?, 'GHN', 'OUTBOUND-001', 'OUTBOUND-001', 'ready_to_pick', GETDATE())",
            idHoaDon
        );
        jdbcTemplate.update(
            "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_van_don, ma_don_hang_ngoai, trang_thai, ngay_tao) VALUES (?, 'GHN_RETURN', 'RETURN-002', 'RETURN-002', 'waiting_to_return', GETDATE())",
            idHoaDon
        );
        jdbcTemplate.update(
            "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_van_don, ma_don_hang_ngoai, trang_thai, ngay_tao) VALUES (?, 'GHN_EXCHANGE', 'EXCHANGE-003', 'EXCHANGE-003', 'ready_to_pick', GETDATE())",
            idHoaDon
        );

        // Clear persistence context so @Formula is populated from DB SELECT
        entityManager.flush();
        entityManager.clear();

        // Re-load entity
        HoaDon loadedHd = hoaDonRepository.findById(idHoaDon).orElseThrow();

        // 2. Verify outbound field is OUTBOUND-001
        assertEquals("OUTBOUND-001", loadedHd.getGhnOrderCode(), "Outbound field must be OUTBOUND-001");

        // 3. Verify return field is RETURN-002
        String resolvedReturnCode = orderViewService.resolveGhnReturnOrderCode(idHoaDon, loadedHd);
        assertEquals("RETURN-002", resolvedReturnCode, "Return resolver must return RETURN-002");
        assertNotEquals(loadedHd.getGhnOrderCode(), resolvedReturnCode, "Outbound and return codes MUST NOT be equal!");

        // 4. Verify listener saving HoaDon does NOT overwrite GHN_RETURN row with GHN outbound code
        hoaDonRepository.save(loadedHd);

        String returnCodeAfterSave = orderViewService.resolveGhnReturnOrderCode(idHoaDon, loadedHd);
        assertEquals("RETURN-002", returnCodeAfterSave, "Saving HoaDon must NOT overwrite GHN_RETURN with outbound code!");
    }

    @Test
    @DisplayName("Verify fake/simulated return code is ignored by resolver")
    public void testSimulatedCodeIgnored() {
        HoaDon hd = createBaseHoaDon("Test Simulated Code");
        Integer idHoaDon = hd.getId();

        jdbcTemplate.update(
            "INSERT INTO TichHopVanChuyen (id_hoa_don, nha_cung_cap, ma_van_don, ma_don_hang_ngoai, trang_thai, ngay_tao) VALUES (?, 'GHN_RETURN', 'GHN-RETURN-SIMULATED-12345678', 'GHN-RETURN-SIMULATED-12345678', 'waiting_to_return', GETDATE())",
            idHoaDon
        );

        entityManager.flush();
        entityManager.clear();

        HoaDon loadedHd = hoaDonRepository.findById(idHoaDon).orElseThrow();
        String resolvedCode = orderViewService.resolveGhnReturnOrderCode(idHoaDon, loadedHd);
        assertNull(resolvedCode, "Simulated code GHN-RETURN-SIMULATED-* must be ignored by resolver and return null");
    }
}
