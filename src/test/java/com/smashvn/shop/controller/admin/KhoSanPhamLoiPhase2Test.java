package com.smashvn.shop.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.smashvn.shop.dao.PhuongThucThanhToanDAO;
import com.smashvn.shop.dto.inventory.KhoSanPhamLoiDetailView;
import com.smashvn.shop.dto.inventory.KhoSanPhamLoiSourceView;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.EditLog;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.HoaDonChiTiet;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.PhuongThucThanhToan;
import com.smashvn.shop.entity.ReturnInventoryStatus;
import com.smashvn.shop.entity.ReturnStatus;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.EditLogRepository;
import com.smashvn.shop.repository.HoaDonChiTietRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.inventory.InventoryLotService;

@SpringBootTest
@Transactional
public class KhoSanPhamLoiPhase2Test {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private HoaDonChiTietRepository hoaDonChiTietRepository;

    @Autowired
    private EditLogRepository editLogRepository;

    @Autowired
    private InventoryLotService inventoryLotService;

    private MockMvc mockMvc;

    private SanPham sampleProduct;
    private SanPhamChiTiet sampleSpct;
    private NhanVien sampleNhanVien;
    private KhachHang sampleKhachHang;
    private PhuongThucThanhToan samplePttt;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        samplePttt = phuongThucThanhToanDAO.findAll().stream().findFirst().orElseGet(() -> {
            PhuongThucThanhToan p = new PhuongThucThanhToan();
            p.setMaPhuongThuc("COD");
            p.setTenPhuongThuc("Thanh toán khi nhận hàng");
            return phuongThucThanhToanDAO.save(p);
        });

        sampleNhanVien = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("staff_test_" + System.nanoTime() + "@smashvn.com");
            tk.setMatKhau("123456");
            tk.setVaiTro("NV");
            tk.setTrangThai("hoat_dong");
            tk = taiKhoanRepository.save(tk);

            NhanVien n = new NhanVien();
            n.setTaiKhoan(tk);
            n.setHoTenNv("Nhan Vien Kiem Kho");
            n.setSoDienThoaiNv("0987654321");
            return nhanVienRepository.save(n);
        });

        sampleKhachHang = khachHangRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("cust_test_" + System.nanoTime() + "@smashvn.com");
            tk.setMatKhau("123456");
            tk.setVaiTro("KH");
            tk.setTrangThai("hoat_dong");
            tk = taiKhoanRepository.save(tk);

            KhachHang kh = new KhachHang();
            kh.setTaiKhoan(tk);
            kh.setHoTenKh("Khach Hang Test");
            kh.setSoDienThoaiKh("0912345678");
            return khachHangRepository.save(kh);
        });

        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc d = new DanhMuc();
            d.setTenDanhMuc("Vot cau long");
            d.setTrangThai(true);
            return danhMucRepository.save(d);
        });

        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu t = new ThuongHieu();
            t.setTenThuongHieu("Yonex");
            t.setTrangThai(true);
            return thuongHieuRepository.save(t);
        });

        sampleProduct = new SanPham();
        sampleProduct.setTenSanPham("Vot Test Phase 2 " + System.nanoTime());
        sampleProduct.setDanhMuc(dm);
        sampleProduct.setThuongHieu(th);
        sampleProduct.setNhanVien(sampleNhanVien);
        sampleProduct.setTrangThai("dang_ban");
        sampleProduct = sanPhamRepository.save(sampleProduct);

        sampleSpct = new SanPhamChiTiet();
        sampleSpct.setSanPham(sampleProduct);
        sampleSpct.setGiaBan(BigDecimal.valueOf(1500000));
        sampleSpct.setSoLuongTon(10);
        sampleSpct.setSoLuongSpLoi(2);
        sampleSpct = sanPhamChiTietRepository.save(sampleSpct);
    }

    private HoaDon createOrder(ReturnInventoryStatus returnInvStatus, String returnReason, String evidenceJson, String requestType) {
        HoaDon hd = new HoaDon();
        hd.setKhachHang(sampleKhachHang);
        hd.setPhuongThucThanhToan(samplePttt);
        hd.setTenNguoiNhan("Nguoi Nhan Test");
        hd.setSdtNhan("0912345678");
        hd.setDiaChiNhan("123 Duong Test, Ha Noi");
        hd.setTongTien(BigDecimal.valueOf(1500000));
        hd.setTrangThaiDonHang("hoan_thanh");
        hd.setTrangThaiThanhToan("DA_THANH_TOAN");
        hd.setTrangThaiHoanHang(ReturnStatus.RETURNED);
        hd.setTrangThaiXuLyHangHoan(returnInvStatus);
        hd.setLyDoHoanTra(returnReason);
        hd.setBangChungHoanTra(evidenceJson);
        hd.setLoaiYeuCauDoiTra(requestType);
        return hoaDonRepository.save(hd);
    }

    private void addOrderItem(HoaDon hd, SanPhamChiTiet spct, int qty) {
        HoaDonChiTiet hdct = new HoaDonChiTiet();
        hdct.setHoaDon(hd);
        hdct.setSanPhamChiTiet(spct);
        hdct.setSoLuong(qty);
        hdct.setDonGia(spct.getGiaBan());
        hdct.setTenSanPhamSnapshot(sampleProduct.getTenSanPham());
        hoaDonChiTietRepository.save(hdct);
    }

    private void addEditLog(Integer hdId, TaiKhoan tk, String role, String note) {
        EditLog log = new EditLog();
        log.setTenBang("HoaDon");
        log.setIdBanGhi(hdId);
        log.setHanhDong("UPDATE");
        log.setTaiKhoan(tk);
        log.setVaiTroThucHien(role);
        log.setGhiChu(note);
        log.setThoiGian(LocalDateTime.now());
        editLogRepository.save(log);
    }

    /**
     * Test Case 1: SPCT co soLuongSpLoi = 2, co 2 don HD001 va HD002 deu DA_CHUYEN_KHO_LOI -> Hien thi 2 don
     */
    @Test
    void testCase1_spctHasTwoSourceOrders_detailShowsTwoOrders() {
        HoaDon hd1 = createOrder(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, "Khung bi nut", "[\"/uploads/returns/1/vid1.mp4\"]", "TRA");
        addOrderItem(hd1, sampleSpct, 1);

        HoaDon hd2 = createOrder(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, "Can vot bi mop", "[\"/uploads/returns/2/vid2.mp4\"]", "DOI");
        addOrderItem(hd2, sampleSpct, 1);

        KhoSanPhamLoiDetailView detail = inventoryLotService.layChiTietKhoSanPhamLoi(sampleSpct.getId());
        assertNotNull(detail);
        assertEquals(2, detail.getLichSuChuyenKhoLoi().size(), "Phai hien thi dung 2 don hang nguon");
        assertEquals(sampleProduct.getTenSanPham(), detail.getTenSanPham());
        assertEquals(2, detail.getSoLuongSpLoi());
        assertEquals(10, detail.getSoLuongTon());
    }

    /**
     * Test Case 2: HD001 co nhieu dong cung SPCT (x1 va x1 hoac 1 dong x2) -> Tong soLuongDaChuyen = 2
     */
    @Test
    void testCase2_multipleLinesSameSpct_aggregatedQty() {
        HoaDon hd1 = createOrder(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, "Loi khung", null, "TRA");
        addOrderItem(hd1, sampleSpct, 1);
        addOrderItem(hd1, sampleSpct, 1);

        KhoSanPhamLoiDetailView detail = inventoryLotService.layChiTietKhoSanPhamLoi(sampleSpct.getId());
        assertNotNull(detail);
        assertEquals(1, detail.getLichSuChuyenKhoLoi().size(), "Chi hien thi 1 record cho cung 1 hoa don");
        assertEquals(2, detail.getLichSuChuyenKhoLoi().get(0).getSoLuongDaChuyen(), "So luong da chuyen phai la tong = 2");
    }

    /**
     * Test Case 3: Don co lyDoHoanTra va bangChungHoanTra -> Hien thi dung ly do va video/anh
     */
    @Test
    void testCase3_hasReasonAndEvidence_parsedCorrectly() {
        String evidenceJson = "[\"/uploads/returns/10/video_loi.mp4\", \"/uploads/returns/10/anh_loi.png\"]";
        HoaDon hd = createOrder(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, "Loi son va nut gen", evidenceJson, "TRA");
        addOrderItem(hd, sampleSpct, 1);

        KhoSanPhamLoiDetailView detail = inventoryLotService.layChiTietKhoSanPhamLoi(sampleSpct.getId());
        assertNotNull(detail);
        assertFalse(detail.getLichSuChuyenKhoLoi().isEmpty());

        KhoSanPhamLoiSourceView source = detail.getLichSuChuyenKhoLoi().get(0);
        assertEquals("Loi son va nut gen", source.getLyDoHoanTra());
        assertEquals(2, source.getBangChungList().size());
        assertTrue(source.isVideo(source.getBangChungList().get(0)));
        assertTrue(source.isImage(source.getBangChungList().get(1)));
    }

    /**
     * Test Case 4: Khong co bang chung -> bangChungList rong, ly do fallback hop le, khong loi JSON
     */
    @Test
    void testCase4_noEvidenceOrMalformedJson_safeFallback() {
        HoaDon hd = createOrder(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, null, "invalid-json-content", "TRA");
        addOrderItem(hd, sampleSpct, 1);

        KhoSanPhamLoiDetailView detail = inventoryLotService.layChiTietKhoSanPhamLoi(sampleSpct.getId());
        assertNotNull(detail);
        assertFalse(detail.getLichSuChuyenKhoLoi().isEmpty());

        KhoSanPhamLoiSourceView source = detail.getLichSuChuyenKhoLoi().get(0);
        assertEquals("Không có thông tin lý do hoàn trả.", source.getLyDoHoanTra());
        assertTrue(source.getBangChungList().isEmpty());
    }

    /**
     * Test Case 5: Co EditLog [KIEM_HANG_HANG_LOI] -> Hien thi nguoi xu ly, thoi gian, vai tro
     */
    @Test
    void testCase5_hasEditLog_displaysProcessorInfo() {
        HoaDon hd = createOrder(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, "Vot cong", null, "TRA");
        addOrderItem(hd, sampleSpct, 1);

        addEditLog(hd.getId(), sampleNhanVien.getTaiKhoan(), "NV", "Xác nhận kiểm kho hoàn hàng: [KIEM_HANG_HANG_LOI] ghi nhận 1 sản phẩm lỗi.");

        KhoSanPhamLoiDetailView detail = inventoryLotService.layChiTietKhoSanPhamLoi(sampleSpct.getId());
        assertNotNull(detail);
        KhoSanPhamLoiSourceView source = detail.getLichSuChuyenKhoLoi().get(0);

        assertEquals(sampleNhanVien.getHoTenNv(), source.getNguoiXuLy());
        assertEquals("Nhân viên", source.getVaiTroNguoiXuLy());
        assertNotNull(source.getThoiGianXuLy());
        assertNotNull(source.getThoiGianXuLyFormatted());
        assertFalse(source.getThoiGianXuLyFormatted().equals("Không xác định"));
    }

    /**
     * Test Case 6: Khong tim thay EditLog -> Nguoi xu ly: Khong xac dinh, khong crash
     */
    @Test
    void testCase6_noEditLog_safeDefaults() {
        HoaDon hd = createOrder(ReturnInventoryStatus.DA_CHUYEN_KHO_LOI, "Vot cong", null, "TRA");
        addOrderItem(hd, sampleSpct, 1);

        KhoSanPhamLoiDetailView detail = inventoryLotService.layChiTietKhoSanPhamLoi(sampleSpct.getId());
        assertNotNull(detail);
        KhoSanPhamLoiSourceView source = detail.getLichSuChuyenKhoLoi().get(0);

        assertEquals("Không xác định", source.getNguoiXuLy());
        assertEquals("Không xác định", source.getVaiTroNguoiXuLy());
        assertEquals("Không xác định", source.getThoiGianXuLyFormatted());
    }

    /**
     * Test Case 7: SPCT khong ton tai -> Controller redirect, khong 500
     */
    @Test
    void testCase7_spctNotFound_redirectNo500() throws Exception {
        mockMvc.perform(get("/admin/kho-san-pham-loi/999999")
                .sessionAttr("vaiTro", "QL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/kho-san-pham-loi"))
                .andExpect(flash().attributeExists("errorMsg"));
    }

    /**
     * Test Case 8: QL & NV truy cap duoc (200 OK), KH / Unauthenticated bi chan (redirect)
     */
    @Test
    void testCase8_roleAccessCheck() throws Exception {
        // QL access -> 200 OK
        mockMvc.perform(get("/admin/kho-san-pham-loi/" + sampleSpct.getId())
                .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/kho-san-pham-loi-detail"))
                .andExpect(model().attributeExists("detail"));

        // NV access -> 200 OK
        mockMvc.perform(get("/admin/kho-san-pham-loi/" + sampleSpct.getId())
                .sessionAttr("vaiTro", "NV"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/kho-san-pham-loi-detail"))
                .andExpect(model().attributeExists("detail"));

        // Unauthenticated -> redirect dang nhap
        mockMvc.perform(get("/admin/kho-san-pham-loi/" + sampleSpct.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dang-nhap"));
    }

    /**
     * Test DB state unmodified: Read-only call does not change soLuongSpLoi or create new rows
     */
    @Test
    void testDbUnmodifiedOnRead() {
        int initialSpLoi = sampleSpct.getSoLuongSpLoi();
        long initialHdctCount = hoaDonChiTietRepository.count();

        inventoryLotService.layChiTietKhoSanPhamLoi(sampleSpct.getId());

        SanPhamChiTiet reloaded = sanPhamChiTietRepository.findById(sampleSpct.getId()).orElseThrow();
        assertEquals(initialSpLoi, reloaded.getSoLuongSpLoi(), "soLuongSpLoi khong duoc thay doi khi chi doc detail");
        assertEquals(initialHdctCount, hoaDonChiTietRepository.count(), "HoaDonChiTiet khong bi them/xoa");
    }
}