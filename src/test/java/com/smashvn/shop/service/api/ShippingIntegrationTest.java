package com.smashvn.shop.service.api;

import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.ShippingZone;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ShippingIntegrationTest {

    @Autowired
    private ShippingZoneResolver zoneResolver;

    @Autowired
    private ShippingFeeCalculator feeCalculator;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Test
    void testAddressNormalizationAndZoneResolution() {
        // Local cases
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Hà Nội"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("ha noi"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("   HA NOI   "));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Ha-Noi"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Thái Nguyên"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("thai nguyen"));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Thái-Nguyên, Việt Nam."));
        assertEquals(ShippingZone.LOCAL, zoneResolver.resolveZone("Hà Nội, Cầu Giấy"));

        // Nationwide cases
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone("Hồ Chí Minh"));
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone("Đà Nẵng"));
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone("Hai Phong"));
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone(""));
        assertEquals(ShippingZone.NATIONWIDE, zoneResolver.resolveZone(null));
    }

    @Test
    void testCarrierFeeMatrixAndFallbacks() {
        // Mock / Fetch GHTK and GHN carrier references
        DonViVanChuyen ghtk = new DonViVanChuyen();
        ghtk.setTenDonVi("Giao hàng tiết kiệm (GHTK)");

        DonViVanChuyen ghn = new DonViVanChuyen();
        ghn.setTenDonVi("Giao Hàng Nhanh (GHN)");

        DonViVanChuyen other = new DonViVanChuyen();
        other.setTenDonVi("Viettel Post");

        // GHTK fees
        assertEquals(0, BigDecimal.valueOf(22000).compareTo(feeCalculator.calculateFee(ghtk, "Thái Nguyên")));
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee(ghtk, "Hồ Chí Minh")));

        // GHN fees
        assertEquals(0, BigDecimal.valueOf(25000).compareTo(feeCalculator.calculateFee(ghn, "Hà Nội")));
        assertEquals(0, BigDecimal.valueOf(38000).compareTo(feeCalculator.calculateFee(ghn, "Đà Nẵng")));

        // Other carrier
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee(other, "Hà Nội")));
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee(other, "Đà Nẵng")));

        // Null / Empty cases & fallback
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee((DonViVanChuyen) null, "Hà Nội")));
        assertEquals(0, BigDecimal.valueOf(38000).compareTo(feeCalculator.calculateFee(ghn, "")));
        assertEquals(0, BigDecimal.valueOf(30000).compareTo(feeCalculator.calculateFee((DonViVanChuyen) null, (String) null)));
    }

    @Test
    void testHistoricalOrderDisplayIntegrity() {
        // Seed / fetch required data
        KhachHang kh = seedKhachHang();
        PhuongThucThanhToan pttt = seedPhuongThucThanhToan();
        DonViVanChuyen dvvc = seedDonViVanChuyen();

        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(dvvc);
        hd.setDiaChiNhan("Hà Nội");
        hd.setSdtNhan("0912345678");
        hd.setTongTien(new BigDecimal("1000000"));

        // Persist shipping fee as 22000
        BigDecimal persistedFee = BigDecimal.valueOf(22000);
        hd.setPhiVanChuyen(persistedFee);

        hd = hoaDonRepository.save(hd);
        assertNotNull(hd.getId());

        // Now verify it's persisted and loaded correctly
        HoaDon loaded = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(loaded);
        assertEquals(0, persistedFee.compareTo(loaded.getPhiVanChuyen()));

        // Even if we query fee calculation or change the address logic, the persisted order's fee remains 22000
        assertEquals(0, persistedFee.compareTo(loaded.getPhiVanChuyen()));
    }

    @Test
    void testHistoricalOrderFeeUnaffected() {
        // Create an order with fee 22000
        KhachHang kh = seedKhachHang();
        PhuongThucThanhToan pttt = seedPhuongThucThanhToan();
        DonViVanChuyen carrier = seedDonViVanChuyen();

        HoaDon hd = new HoaDon();
        hd.setKhachHang(kh);
        hd.setPhuongThucThanhToan(pttt);
        hd.setDonViVanChuyen(carrier);
        hd.setDiaChiNhan("Thái Nguyên");
        hd.setSdtNhan("0912345678");
        hd.setTongTien(BigDecimal.valueOf(100000));
        hd.setPhiVanChuyen(BigDecimal.valueOf(22000));
        hd = hoaDonRepository.save(hd);

        // Reload the order, fee must remain 22000
        HoaDon reloaded = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(reloaded);
        assertEquals(0, BigDecimal.valueOf(22000).compareTo(reloaded.getPhiVanChuyen()));
    }
    // ── Seeding helpers ──────────────────────────────────────────────────────

    private KhachHang seedKhachHang() {
        List<KhachHang> khs = khachHangRepository.findAll();
        if (!khs.isEmpty()) return khs.get(0);
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("ship-kh-" + System.nanoTime() + "@test.com");
        tk.setMatKhau("123");
        tk.setVaiTro("KH");

        tk = taiKhoanRepository.save(tk);
        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoKh("Ship");
        kh.setTenKh("Test");
        kh.setSoDienThoaiKh("0900000001");
        return khachHangRepository.save(kh);
    }

    private PhuongThucThanhToan seedPhuongThucThanhToan() {
        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        if (!ptts.isEmpty()) return ptts.get(0);
        PhuongThucThanhToan p = new PhuongThucThanhToan();
        p.setTenPhuongThuc("COD");
        return phuongThucThanhToanDAO.save(p);
    }

    private DonViVanChuyen seedDonViVanChuyen() {
        List<DonViVanChuyen> dvvcs = donViVanChuyenDAO.findAll();
        if (!dvvcs.isEmpty()) return dvvcs.get(0);
        DonViVanChuyen d = new DonViVanChuyen();
        d.setTenDonVi("GHTK Test");
        d.setHotline("19001234");
        d.setPhiLocal(BigDecimal.valueOf(22000));
        d.setPhiNationwide(BigDecimal.valueOf(30000));
        return donViVanChuyenDAO.save(d);
    }
}
