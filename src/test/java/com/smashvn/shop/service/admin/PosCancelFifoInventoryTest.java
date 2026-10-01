package com.smashvn.shop.service.admin;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.service.admin.AdminPosService.PosItem;

@SpringBootTest
@Transactional
public class PosCancelFifoInventoryTest {

    @Autowired
    private AdminPosService adminPosService;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private com.smashvn.shop.dao.PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    private TaiKhoan staffAccount;
    private KhachHang testCustomer;
    private DanhMuc testCategory;
    private ThuongHieu testBrand;
    private NhanVien testStaff;

    @BeforeEach
    void setUp() {
        staffAccount = taiKhoanRepository.findAll().stream()
                .filter(t -> "NV".equalsIgnoreCase(t.getVaiTro()) || "QL".equalsIgnoreCase(t.getVaiTro()))
                .findFirst()
                .orElseGet(() -> {
                    TaiKhoan tk = new TaiKhoan();
                    tk.setUsername("staff_test_" + System.currentTimeMillis());
                    tk.setMatKhau("123456");
                    tk.setVaiTro("NV");
                    tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
                    return taiKhoanRepository.save(tk);
                });

        testStaff = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            NhanVien nv = new NhanVien();
            nv.setTaiKhoan(staffAccount);
            nv.setHoTenNv("Staff Test");
            nv.setChucVu("QUAN_LY");
            nv.setSoDienThoaiNv("0912345678");
            return nhanVienRepository.save(nv);
        });

        testCustomer = khachHangRepository.findAll().stream().findFirst().orElseGet(() -> {
            KhachHang kh = new KhachHang();
            kh.setTaiKhoan(staffAccount);
            kh.setHoTenKh("Test POS Customer");
            kh.setSoDienThoaiKh("0988776655");
            return khachHangRepository.save(kh);
        });

        testCategory = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc dm = new DanhMuc();
            dm.setTenDanhMuc("Vợt Cầu Lông Test");
            return danhMucRepository.save(dm);
        });

        testBrand = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu th = new ThuongHieu();
            th.setTenThuongHieu("Yonex Test");
            return thuongHieuRepository.save(th);
        });
    }

    private SanPham createTestProduct(String name) {
        SanPham sp = new SanPham();
        sp.setTenSanPham(name);
        sp.setDanhMuc(testCategory);
        sp.setThuongHieu(testBrand);
        sp.setNhanVien(testStaff);
        sp.setTrangThai("hoat_dong");
        return sanPhamRepository.save(sp);
    }

    private SanPhamChiTiet createTestSpctLot(SanPham sp, String color, int stock) {
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setMauSac(color);
        spct.setSoLuongTon(stock);
        spct.setGiaBan(new BigDecimal("500000"));
        spct.setGiaNhap(new BigDecimal("300000"));
        spct.setTrangThai("hoat_dong");
        return sanPhamChiTietRepository.save(spct);
    }

    @Test
    @DisplayName("TEST 1: Hủy đơn POS bán từ 2 lô (Lot A=10, Lot B=20, Bán 15) -> Hoàn kho A=10, B=20")
    void test1_CancelMultiLotPosOrder() {
        SanPham sp = createTestProduct("Vợt Yonex Test 1");
        SanPhamChiTiet lotA = createTestSpctLot(sp, "Đỏ", 10);
        SanPhamChiTiet lotB = createTestSpctLot(sp, "Đỏ", 20);

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = lotA.getId(); // Đại diện lotA
        item.soLuong = 15;

        // Bán 15 sản phẩm qua POS (thanh toán chuyển khoản chờ xác nhận)
        HoaDon hd = adminPosService.thanhToanPos(
                testCustomer.getId(),
                null,
                List.of(item),
                "CHUYEN_KHOAN",
                null,
                "Ban test 1",
                staffAccount.getId(),
                "127.0.0.1"
        );

        // Kiểm tra tồn kho sau khi bán 15 (FIFO lấy hết Lot A = 10, lấy tiếp 5 từ Lot B)
        lotA = sanPhamChiTietRepository.findById(lotA.getId()).orElseThrow();
        lotB = sanPhamChiTietRepository.findById(lotB.getId()).orElseThrow();
        Assertions.assertEquals(0, lotA.getSoLuongTon(), "Lot A phải hết tồn kho (0)");
        Assertions.assertEquals(15, lotB.getSoLuongTon(), "Lot B phải còn 15");

        // Thực hiện HỦY ĐƠN POS
        adminPosService.cancelOrderPos(hd.getId(), staffAccount.getId());

        // Kiểm tra lại tồn kho sau khi hủy
        lotA = sanPhamChiTietRepository.findById(lotA.getId()).orElseThrow();
        lotB = sanPhamChiTietRepository.findById(lotB.getId()).orElseThrow();
        Assertions.assertEquals(10, lotA.getSoLuongTon(), "Lot A phải được hoàn về đúng 10");
        Assertions.assertEquals(20, lotB.getSoLuongTon(), "Lot B phải được hoàn về đúng 20");

        HoaDon cancelledHd = hoaDonRepository.findById(hd.getId()).orElseThrow();
        Assertions.assertEquals(OrderStatus.DA_HUY.getValue(), cancelledHd.getTrangThaiDonHang());
    }

    @Test
    @DisplayName("TEST 2: Hủy đơn POS bán từ 3 lô (Lot A=5, Lot B=10, Lot C=20, Bán 18) -> Hoàn kho A=5, B=10, C=20")
    void test2_CancelThreeLotsPosOrder() {
        SanPham sp = createTestProduct("Vợt Victor Test 2");
        SanPhamChiTiet lotA = createTestSpctLot(sp, "Xanh", 5);
        SanPhamChiTiet lotB = createTestSpctLot(sp, "Xanh", 10);
        SanPhamChiTiet lotC = createTestSpctLot(sp, "Xanh", 20);

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = lotA.getId();
        item.soLuong = 18;

        HoaDon hd = adminPosService.thanhToanPos(
                testCustomer.getId(),
                null,
                List.of(item),
                "CHUYEN_KHOAN",
                null,
                "Ban test 2",
                staffAccount.getId(),
                "127.0.0.1"
        );

        // Đã allocate: Lot A = 5, Lot B = 10, Lot C = 3
        lotA = sanPhamChiTietRepository.findById(lotA.getId()).orElseThrow();
        lotB = sanPhamChiTietRepository.findById(lotB.getId()).orElseThrow();
        lotC = sanPhamChiTietRepository.findById(lotC.getId()).orElseThrow();
        Assertions.assertEquals(0, lotA.getSoLuongTon());
        Assertions.assertEquals(0, lotB.getSoLuongTon());
        Assertions.assertEquals(17, lotC.getSoLuongTon());

        // Hủy đơn
        adminPosService.cancelOrderPos(hd.getId(), staffAccount.getId());

        lotA = sanPhamChiTietRepository.findById(lotA.getId()).orElseThrow();
        lotB = sanPhamChiTietRepository.findById(lotB.getId()).orElseThrow();
        lotC = sanPhamChiTietRepository.findById(lotC.getId()).orElseThrow();
        Assertions.assertEquals(5, lotA.getSoLuongTon());
        Assertions.assertEquals(10, lotB.getSoLuongTon());
        Assertions.assertEquals(20, lotC.getSoLuongTon());
    }

    @Test
    @DisplayName("TEST 3: Hủy cùng một đơn 2 lần (Idempotency) -> Lần 2 bị từ chối, kho chỉ hoàn 1 lần")
    void test3_IdempotencyDoubleCancel() {
        SanPham sp = createTestProduct("Vợt Lining Test 3");
        SanPhamChiTiet lotA = createTestSpctLot(sp, "Đen", 10);

        PosItem item = new PosItem();
        item.idSanPhamChiTiet = lotA.getId();
        item.soLuong = 4;

        HoaDon hd = adminPosService.thanhToanPos(
                testCustomer.getId(),
                null,
                List.of(item),
                "CHUYEN_KHOAN",
                null,
                "Ban test 3",
                staffAccount.getId(),
                "127.0.0.1"
        );

        // Hủy lần 1 -> Thành công
        adminPosService.cancelOrderPos(hd.getId(), staffAccount.getId());
        lotA = sanPhamChiTietRepository.findById(lotA.getId()).orElseThrow();
        Assertions.assertEquals(10, lotA.getSoLuongTon());

        // Hủy lần 2 -> Ném Exception "Chỉ có thể hủy hóa đơn ở trạng thái chờ thanh toán!"
        Assertions.assertThrows(RuntimeException.class, () -> {
            adminPosService.cancelOrderPos(hd.getId(), staffAccount.getId());
        });

        // Tồn kho không bị cộng thêm lần 2
        lotA = sanPhamChiTietRepository.findById(lotA.getId()).orElseThrow();
        Assertions.assertEquals(10, lotA.getSoLuongTon(), "Tồn kho giữ nguyên 10, không bị cộng 2 lần");
    }

    @Test
    @DisplayName("TEST 4: Hủy đơn không có allocation/items -> Bị từ chối và rollback, không cộng tồn kho mù quáng")
    void test4_CancelOrderWithoutAllocationFails() {
        PhuongThucThanhToan pttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan p = new PhuongThucThanhToan();
            p.setTenPhuongThuc("CHUYEN_KHOAN");
            return phuongThucThanhToanDAO.save(p);
        });

        HoaDon hd = new HoaDon();
        hd.setKhachHang(testCustomer);
        hd.setPhuongThucThanhToan(pttt);
        hd.setMaDonHang("HDSVN-EMPTY-" + System.currentTimeMillis());
        hd.setTrangThaiDonHang(OrderStatus.CHO_THANH_TOAN.getValue());
        hd.setTrangThaiThanhToan("CHO_THANH_TOAN");
        hd.setDiaChiNhan("Tại quầy POS");
        hd.setTenNguoiNhan("Khách POS");
        hd.setSdtNhan("0912345678");
        hd.setTongTien(new BigDecimal("100000"));
        hd = hoaDonRepository.save(hd);

        final Integer emptyHdId = hd.getId();
        Assertions.assertThrows(RuntimeException.class, () -> {
            adminPosService.cancelOrderPos(emptyHdId, staffAccount.getId());
        });

        HoaDon emptyHdAfter = hoaDonRepository.findById(emptyHdId).orElseThrow();
        Assertions.assertEquals(OrderStatus.CHO_THANH_TOAN.getValue(), emptyHdAfter.getTrangThaiDonHang(), "Đơn chưa hủy do rollback");
    }

    @Test
    @DisplayName("TEST 5: Hai đơn A và B cùng dùng 1 lot. Hủy đơn A -> Chỉ hoàn allocation của A, B giữ nguyên")
    void test5_CancelOnlySpecificOrderAllocation() {
        SanPham sp = createTestProduct("Vợt Mizuno Test 5");
        SanPhamChiTiet lotA = createTestSpctLot(sp, "Vàng", 30);

        PosItem itemA = new PosItem();
        itemA.idSanPhamChiTiet = lotA.getId();
        itemA.soLuong = 10;

        PosItem itemB = new PosItem();
        itemB.idSanPhamChiTiet = lotA.getId();
        itemB.soLuong = 8;

        HoaDon hdA = adminPosService.thanhToanPos(testCustomer.getId(), null, List.of(itemA), "CHUYEN_KHOAN", null, "Đơn A", staffAccount.getId(), "127.0.0.1");
        HoaDon hdB = adminPosService.thanhToanPos(testCustomer.getId(), null, List.of(itemB), "CHUYEN_KHOAN", null, "Đơn B", staffAccount.getId(), "127.0.0.1");

        // Sau khi bán: Lot A = 30 - 10 - 8 = 12
        lotA = sanPhamChiTietRepository.findById(lotA.getId()).orElseThrow();
        Assertions.assertEquals(12, lotA.getSoLuongTon());

        // Hủy đơn A (10 cái)
        adminPosService.cancelOrderPos(hdA.getId(), staffAccount.getId());

        // Tồn kho Lot A = 12 + 10 = 22
        lotA = sanPhamChiTietRepository.findById(lotA.getId()).orElseThrow();
        Assertions.assertEquals(22, lotA.getSoLuongTon(), "Chỉ hoàn 10 cái của Đơn A, 8 cái của Đơn B vẫn giữ nguyên");
    }

    @Test
    @DisplayName("TEST 6: Quy tắc làm tròn tiền mặt POS (Dưới 500đ làm tròn xuống, từ 500đ trở lên làm tròn lên)")
    void test6_RoundCashAmountRules() {
        // Dưới 500đ -> làm tròn xuống
        Assertions.assertEquals(BigDecimal.valueOf(9000), AdminPosService.roundCashAmount(BigDecimal.valueOf(9400)));
        Assertions.assertEquals(BigDecimal.valueOf(9000), AdminPosService.roundCashAmount(BigDecimal.valueOf(9200)));
        Assertions.assertEquals(BigDecimal.valueOf(9000), AdminPosService.roundCashAmount(BigDecimal.valueOf(9499)));
        Assertions.assertEquals(BigDecimal.valueOf(0), AdminPosService.roundCashAmount(BigDecimal.valueOf(400)));
        Assertions.assertEquals(BigDecimal.valueOf(0), AdminPosService.roundCashAmount(BigDecimal.valueOf(499)));

        // Từ 500đ trở lên -> làm tròn lên
        Assertions.assertEquals(BigDecimal.valueOf(10000), AdminPosService.roundCashAmount(BigDecimal.valueOf(9500)));
        Assertions.assertEquals(BigDecimal.valueOf(10000), AdminPosService.roundCashAmount(BigDecimal.valueOf(9600)));
        Assertions.assertEquals(BigDecimal.valueOf(10000), AdminPosService.roundCashAmount(BigDecimal.valueOf(9800)));
        Assertions.assertEquals(BigDecimal.valueOf(1000), AdminPosService.roundCashAmount(BigDecimal.valueOf(500)));
        Assertions.assertEquals(BigDecimal.valueOf(1000), AdminPosService.roundCashAmount(BigDecimal.valueOf(750)));

        // Số tiền tròn chục nghìn / trăm nghìn
        Assertions.assertEquals(BigDecimal.valueOf(10000), AdminPosService.roundCashAmount(BigDecimal.valueOf(10000)));
        Assertions.assertEquals(BigDecimal.valueOf(50000), AdminPosService.roundCashAmount(BigDecimal.valueOf(50000)));
        Assertions.assertEquals(BigDecimal.ZERO, AdminPosService.roundCashAmount(BigDecimal.ZERO));
        Assertions.assertEquals(BigDecimal.ZERO, AdminPosService.roundCashAmount(null));
    }
}
