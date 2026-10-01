package com.smashvn.shop.security;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.order.GioHangService;

@SpringBootTest
@Transactional
public class PendingOrderDeletionBugTest {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;
    @Autowired
    private KhachHangRepository khachHangRepository;
    @Autowired
    private HoaDonRepository hoaDonRepository;
    @Autowired
    private com.smashvn.shop.dao.DonViVanChuyenDAO donViVanChuyenDAO;
    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;
    @Autowired
    private GioHangService gioHangService;

    private DonViVanChuyen testDvvc;
    private PhuongThucThanhToan testPttt;
    private TaiKhoan testTk;
    private KhachHang testKh;

    @BeforeEach
    void setUp() {
        List<DonViVanChuyen> vcs = donViVanChuyenDAO.findAll();
        if (vcs.isEmpty()) {
            DonViVanChuyen vc = new DonViVanChuyen();
            vc.setTenDonVi("Standard Shipping");
            vc.setHotline("1900");
            vc.setWebsite("https://smashvn.com");
            testDvvc = donViVanChuyenDAO.save(vc);
        } else {
            testDvvc = vcs.get(0);
        }

        List<PhuongThucThanhToan> ptts = phuongThucThanhToanDAO.findAll();
        if (ptts.isEmpty()) {
            PhuongThucThanhToan pt = new PhuongThucThanhToan();
            pt.setTenPhuongThuc("COD");
            testPttt = phuongThucThanhToanDAO.save(pt);
        } else {
            testPttt = ptts.get(0);
        }

        String email = "test-pending-bug-" + System.nanoTime() + "@example.com";
        testTk = new TaiKhoan();
        testTk.setUsername(email);
        testTk.setMatKhau("password123");
        testTk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        testTk.setVaiTro("KH");
        testTk.setTrangThai("hoat_dong");

        testTk = taiKhoanRepository.save(testTk);

        testKh = new KhachHang();
        testKh.setTaiKhoan(testTk);
        testKh.setHoKh("Test");
        testKh.setTenKh("User");
        String uniqueSdt = "09" + String.format("%08d", (int)(Math.random() * 100000000));
        testKh.setSoDienThoaiKh(uniqueSdt);
        testKh.setLaTaiKhoanNoiBo(false);
        testKh = khachHangRepository.save(testKh);
    }

    private HoaDon createOrder(String orderStatus, String paymentStatus, LocalDateTime ngayTao) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(testKh);
        hd.setTrangThaiDonHang(orderStatus);
        hd.setPaymentStatus(paymentStatus);
        hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
        hd.setTongTien(new BigDecimal("500000"));
        hd.setDiaChiNhan("123 Street");
        hd.setSdtNhan(testKh.getSoDienThoaiKh());
        hd.setMaDonHang("PENDING-BUG-ORDER-" + System.nanoTime());
        hd.setDonViVanChuyen(testDvvc);
        hd.setPhuongThucThanhToan(testPttt);
        hd.setNgayTao(ngayTao);
        return hoaDonRepository.save(hd);
    }

    @Test
    void testPendingOrdersAreNotDeletedImmediately() {
        // Create an order just now (0 minutes ago)
        HoaDon newOrder = createOrder("cho_thanh_toan", "pending", LocalDateTime.now());

        // Perform clean pending orders
        gioHangService.cleanPendingOrders(testTk.getId());

        // Verify that the order still exists
        assertTrue(hoaDonRepository.findById(newOrder.getId()).isPresent(), 
                "Pending order created just now should NOT be deleted.");
    }

    @Test
    void testPendingOrdersAreDeletedAfterExpiry() {
        // Create an order 20 minutes ago (expired)
        HoaDon expiredOrder = createOrder("cho_thanh_toan", "pending", LocalDateTime.now().minusMinutes(20));

        // Perform clean pending orders
        gioHangService.cleanPendingOrders(testTk.getId());

        // Verify that the order has been marked as expired (da_huy)
        HoaDon reloaded = hoaDonRepository.findById(expiredOrder.getId()).orElse(null);
        assertNotNull(reloaded, "Expired pending order record should exist.");
        assertEquals("da_huy", reloaded.getTrangThaiDonHang(), "Pending order created 20 minutes ago should be expired to da_huy.");
        assertEquals("expired", reloaded.getPaymentStatus(), "Pending order created 20 minutes ago should have paymentStatus expired.");
    }
}
