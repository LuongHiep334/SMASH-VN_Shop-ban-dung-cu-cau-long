package com.smashvn.shop.service.order;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.service.admin.AdminPosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class OrderCreationTenNguoiNhanTest {

    @Autowired
    private GioHangService gioHangService;

    @Autowired
    private AdminPosService adminPosService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private GioHangRepository gioHangRepository;

    @Autowired
    private GioHangChiTietRepository gioHangChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    private TaiKhoan testUser;
    private KhachHang testKhachHang;
    private SanPhamChiTiet testSpct;
    private NhanVien testNhanVien;
    private DonViVanChuyen testDvvc;

    @BeforeEach
    void setUp() {
        // Clear shipping cache
        if (cacheManager != null && cacheManager.getCache("shipping-carriers") != null) {
            cacheManager.getCache("shipping-carriers").clear();
        }
        // Seed test user
        testUser = new TaiKhoan();
        testUser.setUsername("order_recipient_tester@gmail.com");
        testUser.setMatKhau("testpass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");

        testUser = taiKhoanRepository.save(testUser);

        testKhachHang = new KhachHang();
        testKhachHang.setTaiKhoan(testUser);
        testKhachHang.setHoKh("Order");
        testKhachHang.setTenKh("Recipient Tester");
        testKhachHang.setSoDienThoaiKh("0987654321");
        testKhachHang = khachHangRepository.save(testKhachHang);

        // Retrieve or seed DanhMuc
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("Mặc định");
            return danhMucRepository.save(newDm);
        });

        // Retrieve or seed ThuongHieu
        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu newTh = new ThuongHieu();
            newTh.setTenThuongHieu("Mặc định");
            return thuongHieuRepository.save(newTh);
        });

        // Seed NhanVien
        TaiKhoan nvUser = new TaiKhoan();
        nvUser.setUsername("staff_tester_pos@gmail.com");
        nvUser.setMatKhau("testpass123");
        nvUser.setVaiTro("NV");
        nvUser.setTrangThai("hoat_dong");

        nvUser = taiKhoanRepository.save(nvUser);

        testNhanVien = new NhanVien();
        testNhanVien.setTaiKhoan(nvUser);
        testNhanVien.setHoTenNv("Staff Tester POS");
        testNhanVien.setChucVu("Nhân viên bán hàng");
        testNhanVien.setSoDienThoaiNv("0912345679");
        testNhanVien = nhanVienRepository.save(testNhanVien);

        // Seed a test product
        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Recipient");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setTrangThai("dang_ban");
        sp.setNhanVien(testNhanVien);
        sp = sanPhamRepository.save(sp);

        testSpct = new SanPhamChiTiet();
        testSpct.setSanPham(sp);
        testSpct.setMauSac("Trắng");
        testSpct.setMucCang("24 lbs");
        testSpct.setTrongLuong("4U");
        testSpct.setSoLuongTon(100);
        testSpct.setGiaBan(new BigDecimal("1000000"));
        testSpct = sanPhamChiTietRepository.save(testSpct);

        // Seed DonViVanChuyen
        testDvvc = new DonViVanChuyen();
        testDvvc.setTenDonVi("Đơn vị vận chuyển Test");
        testDvvc.setHotline("19001000");
        testDvvc.setPhiLocal(new BigDecimal("30000"));
        testDvvc.setPhiNationwide(new BigDecimal("50000"));
        testDvvc = donViVanChuyenDAO.save(testDvvc);

        // Set up cart and item
        GioHang cart = new GioHang();
        cart.setKhachHang(testKhachHang);
        cart = gioHangRepository.save(cart);

        GioHangChiTiet cartItem = new GioHangChiTiet();
        cartItem.setGioHang(cart);
        cartItem.setSanPhamChiTiet(testSpct);
        cartItem.setSoLuong(2);
        gioHangChiTietRepository.save(cartItem);
    }

    @Test
    void testOnlineCheckoutSavesRecipientName() {
        // Run createOrder
        HoaDon hd = gioHangService.createOrder(
                testUser.getId(),
                "Nguyễn Văn Nhận",
                "0909090909",
                "123 Đường Láng, Hà Nội",
                testDvvc.getId(), // idDonViVanChuyen
                "cod",
                "Ghi chú test",
                null, // ghnToDistrictId
                null, // ghnToWardCode
                null, // ghnProvinceId
                null, // idDiaChiLuu
                null  // voucherCode
        );

        assertNotNull(hd);
        assertNotNull(hd.getId());

        // Reload from DB and assert tenNguoiNhan
        HoaDon savedHd = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(savedHd);
        assertEquals("Nguyễn Văn Nhận", savedHd.getTenNguoiNhan());
    }

    @Test
    void testPosCheckoutSavesRecipientName() {
        // Prepare list of PosItem
        List<AdminPosService.PosItem> items = new ArrayList<>();
        AdminPosService.PosItem item = new AdminPosService.PosItem();
        item.idSanPhamChiTiet = testSpct.getId();
        item.soLuong = 1;
        items.add(item);

        // Run thanhToanPos
        HoaDon hd = adminPosService.thanhToanPos(
                testKhachHang.getId(),
                null, // maVoucher
                items,
                "TIEN_MAT", // phuongThucPos
                "TX-POS-100", // maGiaoDich
                "Ghi chú POS", // ghiChu
                testNhanVien.getTaiKhoan().getId(), // idNhanVienTaiKhoan
                "127.0.0.1" // clientIp
        );

        assertNotNull(hd);
        assertNotNull(hd.getId());

        // Reload from DB and assert tenNguoiNhan
        HoaDon savedHd = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(savedHd);
        assertEquals("Recipient Tester", savedHd.getTenNguoiNhan());
    }

    @Test
    void testPosCheckoutSavesGuestRecipientName() {
        // Prepare list of PosItem
        List<AdminPosService.PosItem> items = new ArrayList<>();
        AdminPosService.PosItem item = new AdminPosService.PosItem();
        item.idSanPhamChiTiet = testSpct.getId();
        item.soLuong = 1;
        items.add(item);

        // Run thanhToanPos with idKhachHang = null (Guest)
        HoaDon hd = adminPosService.thanhToanPos(
                null, // Guest Customer
                null, // maVoucher
                items,
                "TIEN_MAT", // phuongThucPos
                "TX-POS-101", // maGiaoDich
                "Ghi chú POS khách lẻ", // ghiChu
                testNhanVien.getTaiKhoan().getId(), // idNhanVienTaiKhoan
                "127.0.0.1" // clientIp
        );

        assertNotNull(hd);
        assertNotNull(hd.getId());

        // Reload from DB and assert tenNguoiNhan
        HoaDon savedHd = hoaDonRepository.findById(hd.getId()).orElse(null);
        assertNotNull(savedHd);
        assertEquals("Khách lẻ", savedHd.getTenNguoiNhan());
    }
}
