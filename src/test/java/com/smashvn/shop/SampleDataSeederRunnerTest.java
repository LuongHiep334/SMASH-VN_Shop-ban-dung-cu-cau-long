package com.smashvn.shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;

import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.DanhGiaDAO;
import com.smashvn.shop.dao.DonViVanChuyenDAO;
import com.smashvn.shop.dao.PhuongThucThanhToanDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest
public class SampleDataSeederRunnerTest {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private DanhGiaDAO danhGiaDAO;

    @Autowired
    private DanhGiaAnhRepository danhGiaAnhRepository;

    @Autowired
    private CommentModerationKeywordRepository keywordRepository;

    @Autowired
    private CommentViolationLogRepository violationLogRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private BlogCommentRepository blogCommentRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private com.smashvn.shop.repository.PhieuNhapRepository phieuNhapRepository;

    @Autowired
    private com.smashvn.shop.repository.PhieuNhapChiTietRepository phieuNhapChiTietRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    public void testDeleteAllOrders() throws Exception {
        System.out.println("=== THỰC THI FILE delete-orders.sql ===");
        String deleteSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/delete-orders.sql"));
        jdbcTemplate.execute(deleteSql);
        
        Integer countHd = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM HoaDon", Integer.class);
        Integer countHdct = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM HoaDonChiTiet", Integer.class);
        System.out.println("-> KẾT QUẢ: Số hóa đơn còn lại = " + countHd + ", Số chi tiết hóa đơn = " + countHdct);
    }

    @Test
    public void testExecuteSeedSqlFile() throws Exception {
        System.out.println("=== 1. THỰC THI FILE demo-statistics-rollback.sql ===");
        String rollbackSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-rollback.sql"));
        jdbcTemplate.execute(rollbackSql);
        System.out.println("-> ROLLBACK SCRIPT THỰC THI THÀNH CÔNG!");

        System.out.println("=== 2. THỰC THI FILE demo-statistics-seed.sql ===");
        String seedSql = java.nio.file.Files.readString(java.nio.file.Path.of("scratch/demo-statistics-seed.sql"));
        jdbcTemplate.execute(seedSql);
        System.out.println("-> SEED SCRIPT THỰC THI THÀNH CÔNG LẦN 1!");

        System.out.println("=== 3. CHẠY LẠI demo-statistics-seed.sql ĐỂ KIỂM TRA TÍNH TOÀN VẸN (IDEMPOTENCE) ===");
        jdbcTemplate.execute(seedSql);
        System.out.println("-> SEED SCRIPT THỰC THI THÀNH CÔNG LẦN 2 (KHÔNG XUNG ĐỘT KHÓA NGOẠI)!");
    }

    @Test
    @Commit
    public void seedAllSampleData() {
        System.out.println("==========================================================");
        System.out.println("=== BẮT ĐẦU SEED DỮ LIỆU MẪU ĐẦY ĐỦ THEO YÊU CẦU ===");
        System.out.println("==========================================================");

        // 1. Chuẩn bị khách hàng & nhân viên & phương thức
        KhachHang khachHang = getOrCreateSampleCustomer();
        NhanVien nhanVien = getOrCreateSampleStaff();
        PhuongThucThanhToan ptttCod = getOrCreatePaymentMethod("COD", "Thanh toán khi nhận hàng (COD)");
        PhuongThucThanhToan ptttVnpay = getOrCreatePaymentMethod("VNPAY", "Thanh toán qua VNPAY / Chuyển khoản");
        DonViVanChuyen dvvcGhn = getOrCreateShippingCarrier();

        List<SanPhamChiTiet> spctList = sanPhamChiTietRepository.findAll();
        if (spctList.isEmpty()) {
            System.err.println("Chưa có sản phẩm chi tiết trong CSDL để tạo đơn hàng!");
            return;
        }
        SanPhamChiTiet defaultSpct1 = spctList.get(0);
        SanPhamChiTiet defaultSpct2 = spctList.size() > 1 ? spctList.get(1) : defaultSpct1;

        // 0. Tạo dữ liệu Phiếu Nhập Hàng & Lô Hàng cho toàn bộ các biến thể
        seedImportLots(nhanVien, spctList);

        // 2. Không seed đơn hàng giả lập (Đơn hàng để trống theo yêu cầu)
        // seed10OnlineOrders(khachHang, ptttCod, ptttVnpay, dvvcGhn, defaultSpct1, defaultSpct2);

        // 3. Tạo dữ liệu Đánh Giá sản phẩm
        seedReviews(khachHang, defaultSpct1.getSanPham());

        // 4. Tạo Từ Khóa Cấm & Bình Luận Vi Phạm & Blog Comment
        seedKeywordsAndViolations(khachHang, defaultSpct1.getSanPham());

        // 5. Tạo 2 Bài Viết Blog mới
        seed2NewBlogPosts(nhanVien.getTaiKhoan());

        System.out.println("==========================================================");
        System.out.println("=== HOÀN TẤT SEED TOÀN BỘ DỮ LIỆU MẪU THÀNH CÔNG! ===");
        System.out.println("==========================================================");
    }

    private void seedImportLots(NhanVien nhanVien, List<SanPhamChiTiet> spctList) {
        System.out.println("-> [0/5] Đang tạo dữ liệu Phiếu Nhập & Lô Hàng cho tất cả biến thể...");

        PhieuNhap pn1 = phieuNhapRepository.findByMaPhieuNhap("PN20260601-001").orElseGet(() -> {
            PhieuNhap p = PhieuNhap.builder()
                    .maPhieuNhap("PN20260601-001")
                    .nhanVien(nhanVien)
                    .ngayNhap(LocalDateTime.now().minusMonths(2))
                    .tongTien(BigDecimal.ZERO)
                    .ghiChu("Nhập lô hàng Vợt & Phụ kiện Yonex, Lining hè 2026")
                    .ngayTao(LocalDateTime.now().minusMonths(2))
                    .ngayCapNhat(LocalDateTime.now().minusMonths(2))
                    .build();
            return phieuNhapRepository.save(p);
        });

        PhieuNhap pn2 = phieuNhapRepository.findByMaPhieuNhap("PN20260701-002").orElseGet(() -> {
            PhieuNhap p = PhieuNhap.builder()
                    .maPhieuNhap("PN20260701-002")
                    .nhanVien(nhanVien)
                    .ngayNhap(LocalDateTime.now().minusMonths(1))
                    .tongTien(BigDecimal.ZERO)
                    .ghiChu("Nhập bổ sung lô Giày, Trang phục & Dụng cụ tập luyện")
                    .ngayTao(LocalDateTime.now().minusMonths(1))
                    .ngayCapNhat(LocalDateTime.now().minusMonths(1))
                    .build();
            return phieuNhapRepository.save(p);
        });

        PhieuNhap pn3 = phieuNhapRepository.findByMaPhieuNhap("PN20260801-003").orElseGet(() -> {
            PhieuNhap p = PhieuNhap.builder()
                    .maPhieuNhap("PN20260801-003")
                    .nhanVien(nhanVien)
                    .ngayNhap(LocalDateTime.now().minusDays(10))
                    .tongTien(BigDecimal.ZERO)
                    .ghiChu("Nhập lô hàng chính hãng chuẩn bị cho chiến dịch Flash Sale")
                    .ngayTao(LocalDateTime.now().minusDays(10))
                    .ngayCapNhat(LocalDateTime.now().minusDays(10))
                    .build();
            return phieuNhapRepository.save(p);
        });

        BigDecimal total1 = BigDecimal.ZERO;
        BigDecimal total2 = BigDecimal.ZERO;
        BigDecimal total3 = BigDecimal.ZERO;

        for (SanPhamChiTiet spct : spctList) {
            BigDecimal giaBan = spct.getGiaBan() != null && spct.getGiaBan().compareTo(BigDecimal.ZERO) > 0 ? spct.getGiaBan() : new BigDecimal("2000000");
            BigDecimal giaNhap = (spct.getGiaNhap() != null && spct.getGiaNhap().compareTo(BigDecimal.ZERO) > 0)
                    ? spct.getGiaNhap()
                    : giaBan.multiply(new BigDecimal("0.65")).setScale(0, java.math.RoundingMode.HALF_UP);

            spct.setGiaNhap(giaNhap);
            sanPhamChiTietRepository.save(spct);

            int currentStock = spct.getSoLuongTon() != null ? spct.getSoLuongTon() : 0;
            int soLuongNhap = currentStock > 0 ? currentStock + 10 : 25;
            BigDecimal thanhTien = giaNhap.multiply(BigDecimal.valueOf(soLuongNhap));

            // Kiểm tra xem biến thể đã có trong phiếu nhập nào chưa
            List<PhieuNhapChiTiet> existingDetails = phieuNhapChiTietRepository.findBySpctIdWithReceiptDetails(spct.getId());
            if (existingDetails.isEmpty()) {
                PhieuNhap targetPn = (spct.getId() % 3 == 1) ? pn1 : (spct.getId() % 3 == 2 ? pn2 : pn3);

                PhieuNhapChiTiet pnct = PhieuNhapChiTiet.builder()
                        .phieuNhap(targetPn)
                        .sanPhamChiTiet(spct)
                        .soLuong(soLuongNhap)
                        .giaNhap(giaNhap)
                        .thanhTien(thanhTien)
                        .build();
                phieuNhapChiTietRepository.save(pnct);

                if (targetPn == pn1) total1 = total1.add(thanhTien);
                else if (targetPn == pn2) total2 = total2.add(thanhTien);
                else total3 = total3.add(thanhTien);
            }
        }

        if (total1.compareTo(BigDecimal.ZERO) > 0) {
            pn1.setTongTien(pn1.getTongTien().add(total1));
            phieuNhapRepository.save(pn1);
        }
        if (total2.compareTo(BigDecimal.ZERO) > 0) {
            pn2.setTongTien(pn2.getTongTien().add(total2));
            phieuNhapRepository.save(pn2);
        }
        if (total3.compareTo(BigDecimal.ZERO) > 0) {
            pn3.setTongTien(pn3.getTongTien().add(total3));
            phieuNhapRepository.save(pn3);
        }

        System.out.println("-> Đã tạo thành công dữ liệu Phiếu Nhập Hàng & Lô Hàng cho toàn bộ biến thể!");
    }

    private KhachHang getOrCreateSampleCustomer() {
        List<KhachHang> list = khachHangRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("khachhang_demo");
        tk.setMatKhau(passwordEncoder.encode("123456"));
        tk.setVaiTro("KH");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk.setSoLanMuaThanhCong(5);
        tk.setSoLanNhacNhoViPham(0);
        tk.setNgayTao(LocalDateTime.now());
        tk = taiKhoanRepository.save(tk);

        KhachHang kh = new KhachHang();
        kh.setTaiKhoan(tk);
        kh.setHoTenKh("Nguyễn Hoàng Nam");
        kh.setSoDienThoaiKh("0987654321");
        kh.setNgayTao(LocalDateTime.now());
        return khachHangRepository.save(kh);
    }

    private NhanVien getOrCreateSampleStaff() {
        List<NhanVien> list = nhanVienRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        TaiKhoan tk = new TaiKhoan();
        tk.setUsername("admin_demo");
        tk.setMatKhau(passwordEncoder.encode("123456"));
        tk.setVaiTro("QL");
        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        tk.setSoLanMuaThanhCong(0);
        tk.setSoLanNhacNhoViPham(0);
        tk.setNgayTao(LocalDateTime.now());
        tk = taiKhoanRepository.save(tk);

        NhanVien nv = new NhanVien();
        nv.setTaiKhoan(tk);
        nv.setHoTen("Ban Quản Trị SmashVN");
        nv.setSoDienThoaiNv("0348874711");
        nv.setChucVu("Quản Lý");
        nv.setNgayTao(LocalDateTime.now());
        return nhanVienRepository.save(nv);
    }

    private PhuongThucThanhToan getOrCreatePaymentMethod(String ma, String ten) {
        List<PhuongThucThanhToan> list = phuongThucThanhToanDAO.findAll();
        for (PhuongThucThanhToan p : list) {
            if (ma.equalsIgnoreCase(p.getMaPhuongThuc()) || (p.getTenPhuongThuc() != null && p.getTenPhuongThuc().toUpperCase().contains(ma))) {
                return p;
            }
        }
        PhuongThucThanhToan p = new PhuongThucThanhToan();
        p.setMaPhuongThuc(ma);
        p.setTenPhuongThuc(ten);
        return phuongThucThanhToanDAO.save(p);
    }

    private DonViVanChuyen getOrCreateShippingCarrier() {
        List<DonViVanChuyen> list = donViVanChuyenDAO.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        DonViVanChuyen dv = new DonViVanChuyen();
        dv.setMaDonVi("GHN");
        dv.setTenDonVi("Giao Hàng Nhanh (GHN Express)");
        return donViVanChuyenDAO.save(dv);
    }

    private void seed10OnlineOrders(KhachHang khachHang, PhuongThucThanhToan ptttCod, PhuongThucThanhToan ptttVnpay,
                                   DonViVanChuyen dvvc, SanPhamChiTiet spct1, SanPhamChiTiet spct2) {
        System.out.println("-> [1/4] Đang tạo 10 đơn hàng online với 10 trạng thái khác nhau...");

        String[][] orderConfigs = {
            {"CHO_XAC_NHAN", "CHO_THANH_TOAN", "COD", "Khách đặt đơn giao giờ hành chính", "0981112221", "Trần Minh Quân", "Số 15 Cầu Giấy, Hà Nội"},
            {"CHO_THANH_TOAN", "CHO_THANH_TOAN", "VNPAY", "Chờ khách quét mã VietQR/VNPAY", "0981112222", "Lê Thanh Sơn", "Số 42 Đội Cấn, Ba Đình, Hà Nội"},
            {"DA_XAC_NHAN", "CHO_THANH_TOAN", "COD", "Đã liên hệ xác nhận đơn hàng thành công", "0981112223", "Phạm Thị Hương", "Số 88 Lê Duẩn, Hoàn Kiếm, Hà Nội"},
            {"DANG_CHUAN_BI_HANG", "DA_THANH_TOAN", "VNPAY", "Đang lấy hàng tại kho Thái Nguyên", "0981112224", "Nguyễn Tuấn Kiệt", "Số 12 Quang Trung, TP Thái Nguyên"},
            {"SAN_SANG_GIAO", "DA_THANH_TOAN", "VNPAY", "Đã đóng gói hộp carton niêm phong SmashVN", "0981112225", "Hoàng Văn Nam", "Số 68 Nguyễn Chí Thanh, Đống Đa, Hà Nội"},
            {"DA_BAN_GIAO_GHN", "CHO_THANH_TOAN", "COD", "Đã bàn giao bưu tá GHN - Mã vận đơn: GHN778899VN", "0981112226", "Vũ Mai Linh", "Số 29 Võ Chí Công, Tây Hồ, Hà Nội"},
            {"DANG_GIAO", "CHO_THANH_TOAN", "COD", "Shipper GHN đang trên đường giao tới người nhận", "0981112227", "Đặng Đình Bách", "Số 102 Hoàng Văn Thụ, TP Bắc Giang"},
            {"DA_GIAO", "DA_THANH_TOAN", "COD", "Giao hàng thành công - Khách đã ký nhận và thanh toán", "0981112228", "Trương Mỹ Duyên", "Số 56 Nguyễn Trãi, Thanh Xuân, Hà Nội"},
            {"GIAO_THAT_BAI", "CHO_THANH_TOAN", "COD", "Khách bận đi công tác hẹn giao lại vào thứ Hai tuần tới", "0981112229", "Bùi Quốc Cường", "Số 73 Giải Phóng, Hai Bà Trưng, Hà Nội"},
            {"DA_HUY", "DA_HUY", "COD", "Khách hàng đổi ý muốn chuyển sang mẫu vợt Astrox 88D Pro", "0981112230", "Ngô Đức Trọng", "Số 18 Phạm Hùng, Nam Từ Liêm, Hà Nội"}
        };

        for (int i = 0; i < orderConfigs.length; i++) {
            String[] cfg = orderConfigs[i];
            String trangThaiDonHang = cfg[0];
            String trangThaiThanhToan = cfg[1];
            String ptttKey = cfg[2];
            String ghiChu = cfg[3];
            String sdt = cfg[4];
            String hoTen = cfg[5];
            String diaChi = cfg[6];

            BigDecimal gia1 = spct1.getGiaBan() != null ? spct1.getGiaBan() : BigDecimal.valueOf(1500000);
            BigDecimal gia2 = spct2.getGiaBan() != null ? spct2.getGiaBan() : BigDecimal.valueOf(2200000);
            int sl1 = (i % 2 == 0) ? 1 : 2;
            int sl2 = (i % 3 == 0) ? 1 : 0;

            BigDecimal tienHang = gia1.multiply(BigDecimal.valueOf(sl1)).add(gia2.multiply(BigDecimal.valueOf(sl2)));
            BigDecimal phiVanChuyen = BigDecimal.valueOf(30000);
            BigDecimal tongTien = tienHang.add(phiVanChuyen);

            HoaDon hd = new HoaDon();
            hd.setKhachHang(khachHang);
            hd.setNhanVien(null); // Đơn Online đặt trên web
            hd.setPhuongThucThanhToan(ptttKey.equals("VNPAY") ? ptttVnpay : ptttCod);
            hd.setDonViVanChuyen(dvvc);
            hd.setNgayTao(LocalDateTime.now().minusDays(10 - i).minusHours(i * 2));
            if ("DA_THANH_TOAN".equals(trangThaiThanhToan)) {
                hd.setNgayThanhToan(hd.getNgayTao().plusMinutes(15));
            }
            hd.setTongTienHang(tienHang);
            hd.setPhiVanChuyen(phiVanChuyen);
            hd.setSoTienGiamVoucher(BigDecimal.ZERO);
            hd.setTongTien(tongTien);
            hd.setTrangThaiDonHang(trangThaiDonHang);
            hd.setTrangThaiThanhToan(trangThaiThanhToan);
            hd.setTenNguoiNhan(hoTen);
            hd.setSdtNhan(sdt);
            hd.setEmailNguoiNhan("khachhang" + (i + 1) + "@gmail.com");
            hd.setDiaChiNhan(diaChi);
            hd.setGhiChu("[DATA_SAMPLE] " + ghiChu);
            if ("DA_HUY".equals(trangThaiDonHang)) {
                hd.setLyDoHuy(ghiChu);
            }

            hd = hoaDonRepository.save(hd);

            // Chi tiết đơn hàng 1
            HoaDonChiTiet hdct1 = new HoaDonChiTiet();
            hdct1.setHoaDon(hd);
            hdct1.setSanPhamChiTiet(spct1);
            hdct1.setSoLuong(sl1);
            hdct1.setDonGia(gia1);
            hdct1.setGiaGoc(gia1);
            hdct1.setGiaSauGiam(gia1);
            hdct1.setTenSanPhamSnapshot(spct1.getSanPham() != null ? spct1.getSanPham().getTenSanPham() : "Sản phẩm cầu lông");
            hdct1.setSkuSnapshot("SPCT-" + spct1.getId());
            hdct1.setNgayTao(hd.getNgayTao());
            hoaDonChiTietRepository.save(hdct1);

            // Chi tiết đơn hàng 2 nếu có
            if (sl2 > 0) {
                HoaDonChiTiet hdct2 = new HoaDonChiTiet();
                hdct2.setHoaDon(hd);
                hdct2.setSanPhamChiTiet(spct2);
                hdct2.setSoLuong(sl2);
                hdct2.setDonGia(gia2);
                hdct2.setGiaGoc(gia2);
                hdct2.setGiaSauGiam(gia2);
                hdct2.setTenSanPhamSnapshot(spct2.getSanPham() != null ? spct2.getSanPham().getTenSanPham() : "Sản phẩm cầu lông");
                hdct2.setSkuSnapshot("SPCT-" + spct2.getId());
                hdct2.setNgayTao(hd.getNgayTao());
                hoaDonChiTietRepository.save(hdct2);
            }
        }
        System.out.println("-> Đã tạo thành công 10 đơn hàng Online với 10 trạng thái!");
    }

    private void seedReviews(KhachHang khachHang, SanPham defaultSanPham) {
        System.out.println("-> [2/4] Đang tạo dữ liệu Đánh Giá sản phẩm mẫu...");

        List<SanPham> sanPhams = sanPhamRepository.findAll();
        if (sanPhams.isEmpty()) {
            return;
        }

        Object[][] reviews = {
            {5.0, "Vợt đánh cực kỳ thoát tay, phông cầu đầm và đập cầu rất cắm. Căng cước 11kg đánh tiếng nổ đanh vang, shop đóng gói hàng rất kỹ có ống chống sốc. 10/10!", false, false, "khachhang_review_01", "Nguyễn Văn Hoàng"},
            {5.0, "Sản phẩm chính hãng chuẩn 100%, quét mã check code ra thông tin rõ ràng. Giao hàng từ Thái Nguyên về Hà Nội mất chưa tới 1 ngày.", false, false, "khachhang_review_02", "Trần Thu Hà"},
            {4.0, "Vợt nhẹ, độ cứng vừa phải thích hợp cho người lực cổ tay trung bình khá. Cước căng chuẩn, tuy nhiên quấn cán tặng kèm màu hơi tối.", false, false, "khachhang_review_03", "Lê Bảo Nam"},
            {3.0, "Hàng nhận được đầy đủ phụ kiện. Chất lượng tạm ổn nhưng hộp carton bên ngoài bị móp nhẹ trong quá trình vận chuyển.", false, false, "khachhang_review_04", "Phạm Quốc Tuấn"},
            {1.0, "Hàng kém chất lượng, shop lừa đảo không đúng như cam kết ban đầu!", true, false, "khachhang_review_05", "Đỗ Minh Khang"},
            {5.0, "Rất hài lòng về thái độ phục vụ của nhân viên tư vấn. Vợt trợ lực tốt, đánh đôi phản tạt cầu cực nhanh.", false, false, "khachhang_review_06", "Vũ Mỹ Tiên"}
        };

        for (int i = 0; i < reviews.length; i++) {
            final int reviewIndex = i;
            Double soSao = (Double) reviews[i][0];
            String noiDung = (String) reviews[i][1];
            Boolean binhLuanAn = (Boolean) reviews[i][2];
            Boolean hinhAnhAn = (Boolean) reviews[i][3];
            String username = (String) reviews[i][4];
            String hoTen = (String) reviews[i][5];

            SanPham targetSp = sanPhams.get(i % sanPhams.size());

            // Tìm hoặc tạo khách hàng riêng cho từng đánh giá
            KhachHang reviewKh = khachHangRepository.findAll().stream()
                    .filter(k -> k.getTaiKhoan() != null && username.equalsIgnoreCase(k.getTaiKhoan().getUsername()))
                    .findFirst()
                    .orElseGet(() -> {
                        TaiKhoan tk = new TaiKhoan();
                        tk.setUsername(username);
                        tk.setMatKhau(passwordEncoder.encode("123456"));
                        tk.setVaiTro("KH");
                        tk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
                        tk.setSoLanMuaThanhCong(1);
                        tk.setSoLanNhacNhoViPham(0);
                        tk.setNgayTao(LocalDateTime.now());
                        tk = taiKhoanRepository.save(tk);

                        KhachHang k = new KhachHang();
                        k.setTaiKhoan(tk);
                        k.setHoTenKh(hoTen);
                        k.setSoDienThoaiKh("097700000" + (char)('1' + (char)Math.min(9, reviewIndex)));
                        k.setNgayTao(LocalDateTime.now());
                        return khachHangRepository.save(k);
                    });

            // Kiểm tra xem đã có đánh giá cho cặp (khachHang, sanPham) chưa
            Optional<DanhGia> existingDg = danhGiaDAO.findByKhachHang_IdAndSanPham_IdAndDaXoaFalse(reviewKh.getId(), targetSp.getId());
            if (existingDg.isEmpty()) {
                DanhGia dg = DanhGia.builder()
                        .khachHang(reviewKh)
                        .sanPham(targetSp)
                        .soSao(soSao)
                        .binhLuan(noiDung)
                        .binhLuanAn(binhLuanAn)
                        .hinhAnhAn(hinhAnhAn)
                        .daXoa(false)
                        .ngayDanhGia(LocalDateTime.now().minusDays(i * 3 + 1))
                        .build();

                dg = danhGiaDAO.save(dg);

                if (i < 3) {
                    HinhAnhDanhGia anh = new HinhAnhDanhGia();
                    anh.setDanhGia(dg);
                    anh.setUrlHinhAnh("/images/products/product-" + (i + 1) + ".jpg");
                    anh.setNgayTao(LocalDateTime.now());
                    danhGiaAnhRepository.save(anh);
                }
            }
        }
        System.out.println("-> Đã tạo thành công dữ liệu Đánh Giá sản phẩm!");
    }

    private void seedKeywordsAndViolations(KhachHang khachHang, SanPham sanPham) {
        System.out.println("-> [3/4] Đang tạo Từ Khóa Cấm và Nhật Ký Vi Phạm...");

        String[] keywords = {
            "lừa đảo", "lua dao", "hàng giả", "hang gia", "fake", 
            "chửi thề", "dm", "vkl", "cờ bạc", "vay tiền", "quảng cáo"
        };

        List<CommentModerationKeyword> existingKeywords = keywordRepository.findAll();
        for (String kw : keywords) {
            boolean exists = existingKeywords.stream().anyMatch(k -> k.getKeyword() != null && k.getKeyword().equalsIgnoreCase(kw));
            if (!exists) {
                CommentModerationKeyword cmk = CommentModerationKeyword.builder()
                        .keyword(kw)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                keywordRepository.save(cmk);
            }
        }

        // Tạo 4 log vi phạm mẫu với các mức độ khác nhau
        String[][] violationData = {
            {"LOW", "Shop này bán đồ như cái lừa đảo vậy trời ơi", "Shop này bán đồ như cái *** vậy trời ơi", "1", "Cảnh cáo lần 1"},
            {"MEDIUM", "Hàng fake hàng giả đừng ai mua tốn tiền", "Hàng *** hàng *** đừng ai mua tốn tiền", "2", "Tạm khóa bình luận 24 giờ"},
            {"HIGH", "Truy cập ngay web cá độ cờ bạc nhận tiền miễn phí", "Truy cập ngay web *** *** nhận tiền miễn phí", "3", "Khóa bình luận 7 ngày"},
            {"CRITICAL", "dm thằng shop lừa đảo cờ bạc vay tiền vkl", "*** thằng shop *** *** *** ***", "4", "Khóa tài khoản vĩnh viễn"}
        };

        for (String[] v : violationData) {
            String mucDo = v[0];
            String goc = v[1];
            String daLoc = v[2];
            int soLan = Integer.parseInt(v[3]);
            String thoiHan = v[4];

            CommentViolationLog log = CommentViolationLog.builder()
                    .taiKhoan(khachHang.getTaiKhoan())
                    .sanPham(sanPham)
                    .noiDungGoc(goc)
                    .noiDungDaLoc(daLoc)
                    .mucDoViPham(mucDo)
                    .soLanViPham(soLan)
                    .thoiHanKhoa(thoiHan)
                    .ngayViPham(LocalDateTime.now().minusDays(soLan * 2))
                    .createdAt(LocalDateTime.now().minusDays(soLan * 2))
                    .build();
            violationLogRepository.save(log);
        }

        System.out.println("-> Đã tạo thành công Từ Khóa Cấm và Nhật Ký Vi Phạm!");
    }

    private void seed2NewBlogPosts(TaiKhoan adminTk) {
        System.out.println("-> [4/4] Đang tạo 2 bài viết blog mới...");

        // Bài 1: Top 5 Vợt Cầu Lông Công Thủ Toàn Diện
        String slug1 = "top-5-vot-cau-long-cong-thu-toan-dien-dang-mua-nhat-2026";
        if (blogRepository.findBySlug(slug1).isEmpty()) {
            Blog b1 = Blog.builder()
                    .title("Top 5 Cây Vợt Cầu Lông Công Thủ Toàn Diện Đáng Mua Nhất 2026")
                    .slug(slug1)
                    .summary("Tổng hợp và đánh giá chi tiết 5 mẫu vợt cầu lông công thủ toàn diện (all-around) cân bằng hoàn hảo giữa sức mạnh smash uy lực và khả năng phản tạt thủ cầu linh hoạt hàng đầu năm 2026.")
                    .content("""
                        <p>Trong bộ môn cầu lông, việc sở hữu một cây vợt <strong>công thủ toàn diện</strong> phù hợp với lối đánh kiểm soát và phản tạt linh hoạt sẽ giúp lông thủ làm chủ mọi nhịp độ trận đấu. Dưới đây là 5 cây vợt đỉnh cao được giới chuyên môn và người chơi phong trào đánh giá cao nhất năm 2026 tại Smash VN.</p>
                        
                        <h3>1. Yonex Arcsaber 11 Pro - Ông vua điều cầu chuẩn xác</h3>
                        <p>Arcsaber 11 Pro vẫn luôn là tượng đài bất hủ trong phân khúc cân bằng. Với công nghệ <em>Control-Assist Bumper</em> và vật liệu <em>Pocketing Booster</em>, cây vợt mang lại cảm giác giữ cầu lâu hơn trên mặt vợt, giúp bạn tung ra những đường cầu sát lưới và điều hướng điểm rơi chính xác đến từng centimet.</p>
                        
                        <blockquote>
                            "Arcsaber 11 Pro là sự kết hợp hoàn mỹ giữa kiểm soát điều cầu tinh tế và những pha phản tạt tốc độ cao không thể cản phá."
                        </blockquote>

                        <h3>2. Lining Axforce 80 - Vũ khí công thủ sắc bén</h3>
                        <p>Được thiết kế với thân vợt siêu mỏng và sợi carbon cao cấp TB Nano, Axforce 80 mang lại khả năng trợ lực tuyệt vời. Điểm cân bằng hơi nặng đầu một chút giúp những cú smash cầu cuối sân có độ cắm và uy lực ấn tượng nhưng vẫn cực kỳ nhanh nhạy khi thủ cầu.</p>

                        <h3>3. Victor DriveX 9X - Tốc độ phản tạt đỉnh cao</h3>
                        <p>Dòng DriveX của Victor nổi tiếng với công nghệ khung <em>Dynamic-Hex</em> kết hợp trục <em>Free Core</em> bằng vật liệu tổng hợp nhân tạo, giảm thiểu tối đa độ rung chấn và giúp cổ tay xoay chuyển linh hoạt trong các pha đôi công sát lưới.</p>

                        <h3>4. Yonex Astrox 77 Pro - Cú đập uy lực, thu vợt chớp nhoáng</h3>
                        <p>Với công nghệ <em>Rotational Generator System</em> phân bổ trọng lượng thông minh ở 3 điểm (đầu vợt, khớp chữ T và cán vợt), Astrox 77 Pro cho phép bạn chuyển đổi trạng thái từ tấn công dồn dập sang phòng thủ thủ cầu chỉ trong tích tắc.</p>

                        <h3>5. Felet Woven TJ 1000 - Sức mạnh bền bỉ từ Malaysia</h3>
                        <p>Khung vợt Woven Carbon chịu lực căng kỷ lục lên đến 35lbs (hơn 15kg). Cây vợt này cực kỳ đầm tay, bền bỉ và tạo ra những cú phông cầu cuối sân nhẹ nhàng mà không tốn nhiều sức.</p>

                        <p><strong>Lời kết:</strong> Hãy đến ngay showroom của <strong>Smash VN</strong> hoặc đặt hàng trực tuyến để được đo lực cổ tay và căng cước chính hãng chuẩn từng pound (lbs) nhé!</p>
                    """)
                    .image("/images/blog/blog-post-1.jpg")
                    .publishDate(LocalDate.now())
                    .category("Tư Vấn Chọn Vợt")
                    .tags("vot-cau-long, yonex, lining, victor, tu-van, smashvn")
                    .status(BlogStatus.PUBLISHED)
                    .deleted(false)
                    .nguoiDang(adminTk)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build();
            b1 = blogRepository.save(b1);

            // Thêm bình luận cho bài 1
            BlogComment c1 = BlogComment.builder()
                    .blog(b1)
                    .taiKhoan(adminTk)
                    .content("Bài viết rất chi tiết và hữu ích! Mình đang dùng cây Arcsaber 11 Pro căng cước 66 Ultimax đánh cực kỳ sướng tay.")
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .deleted(false)
                    .build();
            blogCommentRepository.save(c1);
        }

        // Bài 2: Hướng dẫn kỹ thuật Smash
        String slug2 = "bi-quyet-thuc-hien-cu-smash-dap-cau-uy-luc-chuan-van-dong-vien";
        if (blogRepository.findBySlug(slug2).isEmpty()) {
            Blog b2 = Blog.builder()
                    .title("Bí Quyết Thực Hiện Cú Smash (Đập Cầu) Uy Lực Chuẩn Vận Động Viên")
                    .slug(slug2)
                    .summary("Hướng dẫn chi tiết từng bước từ bộ chân di chuyển, tư thế mở vai, điểm tiếp xúc cầu đến kỹ thuật gập cổ tay phát lực tối đa giúp bạn sở hữu cú đập cầu cắm sàn uy lực.")
                    .content("""
                        <p>Đập cầu (Smash) là một trong những vũ khí ghi điểm mãn nhãn và uy lực nhất trong bộ môn cầu lông. Một cú smash tốt không chỉ phụ thuộc vào sức mạnh bắp tay mà bắt nguồn từ chuỗi động lực toàn thân (Kinetic Chain). Dưới đây là hướng dẫn chi tiết từng bước chuẩn kỹ thuật.</p>
                        
                        <h3>Bước 1: Di chuyển bộ chân và tư thế chuẩn bị</h3>
                        <p>Ngay khi đối thủ hất cầu cao sâu về cuối sân, bạn cần lập tức lùi chân về sau bằng bước bật đuổi hoặc bước chéo. Trọng tâm dồn về chân sau, người xoay nghiêng một góc 45 độ so với lưới.</p>
                        
                        <blockquote>
                            "Điểm tiếp xúc cầu lý tưởng nhất là ở phía trước mặt và chếch về bên tay cầm vợt khoảng 20-30cm khi cầu ở điểm rơi cao nhất."
                        </blockquote>

                        <h3>Bước 2: Mở vai và dẫn vợt ra sau</h3>
                        <p>Nâng tay không cầm vợt lên để giữ thăng bằng và căn cước hướng cầu rơi. Tay cầm vợt co lại, khuỷu tay nâng ngang vai, đầu vợt rủ ra sau lưng như động tác gãi lưng để tạo đà tích lũy lực đàn hồi.</p>

                        <h3>Bước 3: Phát lực toàn thân và gập cổ tay</h3>
                        <p>Xoay hông, mở ngực và vung cánh tay từ sau ra trước theo hình vòng cung. Tại khoảnh khắc tiếp xúc mặt vợt vào quả cầu (Sweet Spot), siết chặt các ngón tay và gập mạnh cổ tay hướng xuống để cầu bay theo quỹ đạo cắm thẳng xuống mặt sân đối phương.</p>

                        <h3>Những sai lầm thường gặp khi đập cầu:</h3>
                        <ul>
                            <li><strong>Gồng cứng cơ bắp tay quá sớm:</strong> Làm giảm tốc độ đầu vợt và nhanh mỏi cơ.</li>
                            <li><strong>Đập khi cầu ở sau đầu:</strong> Khiến cầu bay bổng hoặc rúc lưới do góc đánh bị với.</li>
                            <li><strong>Không bật xoay hông:</strong> Chỉ dùng lực tay khiến cú đánh thiếu độ nặng và dễ gây chấn thương vai.</li>
                        </ul>

                        <p>Chúc các bạn luyện tập chăm chỉ và có những pha đập cầu ghi điểm ấn tượng trên sân!</p>
                    """)
                    .image("/images/blog/blog-post-2.jpg")
                    .publishDate(LocalDate.now())
                    .category("Kỹ Thuật Cầu Lông")
                    .tags("ky-thuat, smash, dap-cau, luyen-tap, cau-long")
                    .status(BlogStatus.PUBLISHED)
                    .deleted(false)
                    .nguoiDang(adminTk)
                    .createdAt(LocalDateTime.now().minusHours(12))
                    .build();
            b2 = blogRepository.save(b2);

            // Thêm bình luận cho bài 2
            BlogComment c2 = BlogComment.builder()
                    .blog(b2)
                    .taiKhoan(adminTk)
                    .content("Cảm ơn bài chia sẻ rất tâm huyết của Smash VN. Đọc xong hiểu ngay lý do tại sao trước giờ mình đập cầu hay bị đau khớp vai.")
                    .createdAt(LocalDateTime.now().minusHours(5))
                    .deleted(false)
                    .build();
            blogCommentRepository.save(c2);
        }

        System.out.println("-> Đã tạo thành công 2 bài viết blog mới!");
    }
}
