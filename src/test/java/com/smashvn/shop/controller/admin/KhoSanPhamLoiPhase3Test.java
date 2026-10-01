package com.smashvn.shop.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.smashvn.shop.dto.inventory.KhoSanPhamLoiDetailView;
import com.smashvn.shop.dto.inventory.KhoSanPhamLoiLichSuXuLyView;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.EditLog;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.EditLogRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.inventory.InventoryLotService;

@SpringBootTest
@ActiveProfiles("default")
@Transactional
class KhoSanPhamLoiPhase3Test {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private InventoryLotService inventoryLotService;

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
    private EditLogRepository editLogRepository;

    private SanPhamChiTiet sampleSpct;
    private TaiKhoan qlAccount;
    private TaiKhoan nvAccount;
    private TaiKhoan khAccount;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

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

        qlAccount = new TaiKhoan();
        qlAccount.setUsername("admin_test_ql_" + System.nanoTime());
        qlAccount.setMatKhau("123456");
        qlAccount.setVaiTro("QL");
        qlAccount.setTrangThai("hoat_dong");
        qlAccount = taiKhoanRepository.save(qlAccount);

        NhanVien nvQl = new NhanVien();
        nvQl.setTaiKhoan(qlAccount);
        nvQl.setHoTenNv("Quản Lý Test");
        nvQl.setChucVu("Quản lý");
        nvQl.setSoDienThoaiNv("0981111111");
        nhanVienRepository.save(nvQl);

        nvAccount = new TaiKhoan();
        nvAccount.setUsername("nv_test_" + System.nanoTime());
        nvAccount.setMatKhau("123456");
        nvAccount.setVaiTro("NV");
        nvAccount.setTrangThai("hoat_dong");
        nvAccount = taiKhoanRepository.save(nvAccount);

        NhanVien nvStaff = new NhanVien();
        nvStaff.setTaiKhoan(nvAccount);
        nvStaff.setHoTenNv("Nhân Viên Test");
        nvStaff.setChucVu("Nhân viên kho");
        nvStaff.setSoDienThoaiNv("0982222222");
        nhanVienRepository.save(nvStaff);

        khAccount = new TaiKhoan();
        khAccount.setUsername("kh_test_" + System.nanoTime());
        khAccount.setMatKhau("123456");
        khAccount.setVaiTro("KH");
        khAccount.setTrangThai("hoat_dong");
        khAccount = taiKhoanRepository.save(khAccount);

        SanPham sp = new SanPham();
        sp.setTenSanPham("Vợt Test Phase 3 " + System.nanoTime());
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(nvQl);
        sp.setTrangThai("dang_ban");
        sp = sanPhamRepository.save(sp);

        sampleSpct = new SanPhamChiTiet();
        sampleSpct.setSanPham(sp);
        sampleSpct.setGiaBan(BigDecimal.valueOf(1500000));
        sampleSpct.setGiaNhap(BigDecimal.valueOf(1000000));
        sampleSpct.setSoLuongTon(10);
        sampleSpct.setSoLuongSpLoi(5);
        sampleSpct = sanPhamChiTietRepository.save(sampleSpct);
    }

    @Test
    @DisplayName("Phase 3 - Case 1: Tiêu hủy sản phẩm lỗi (QL) -> giảm kho lỗi, không đổi kho bán, ghi EditLog")
    void testTieuHuySanPhamLoi_Success() {
        inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "TIEU_HUY",
                2,
                "Khung gãy hoàn toàn",
                qlAccount.getId(),
                "QL",
                "127.0.0.1"
        );

        SanPhamChiTiet updated = sanPhamChiTietRepository.findById(sampleSpct.getId()).orElseThrow();
        assertThat(updated.getSoLuongSpLoi()).isEqualTo(3);
        assertThat(updated.getSoLuongTon()).isEqualTo(10);

        List<EditLog> logs = editLogRepository.findLichSuXuLyKhoLoi(sampleSpct.getId());
        assertThat(logs).isNotEmpty();
        EditLog latest = logs.get(0);
        assertThat(latest.getGhiChu()).contains("[KHO_LOI_TIEU_HUY]");
        assertThat(latest.getGhiChu()).contains("soLuong=2");
        assertThat(latest.getGhiChu()).contains("lyDo=Khung gãy hoàn toàn");
        assertThat(latest.getGiaTriCu()).contains("soLuongSpLoi=5");
        assertThat(latest.getGiaTriMoi()).contains("soLuongSpLoi=3");
    }

    @Test
    @DisplayName("Phase 3 - Case 2: Trả nhà cung cấp (NV) -> giảm kho lỗi, không đổi kho bán, ghi EditLog")
    void testTraNhaCungCap_Success() {
        inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "TRA_NHA_CUNG_CAP",
                1,
                "Lỗi kỹ thuật từ nhà sản xuất",
                nvAccount.getId(),
                "NV",
                "127.0.0.1"
        );

        SanPhamChiTiet updated = sanPhamChiTietRepository.findById(sampleSpct.getId()).orElseThrow();
        assertThat(updated.getSoLuongSpLoi()).isEqualTo(4);
        assertThat(updated.getSoLuongTon()).isEqualTo(10);

        List<EditLog> logs = editLogRepository.findLichSuXuLyKhoLoi(sampleSpct.getId());
        assertThat(logs).isNotEmpty();
        EditLog latest = logs.get(0);
        assertThat(latest.getGhiChu()).contains("[KHO_LOI_TRA_NCC]");
        assertThat(latest.getGhiChu()).contains("soLuong=1");
        assertThat(latest.getGhiChu()).contains("lyDo=Lỗi kỹ thuật từ nhà sản xuất");
    }

    @Test
    @DisplayName("Phase 3 - Case 3: Sửa xong nhập lại kho bán -> giảm kho lỗi, tăng tồn kho bán, ghi EditLog")
    void testSuaXongNhapLaiKho_Success() {
        inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "SUA_XONG_NHAP_LAI_KHO",
                2,
                "Đã thay gen và căn chỉnh đạt chuẩn",
                nvAccount.getId(),
                "NV",
                "127.0.0.1"
        );

        SanPhamChiTiet updated = sanPhamChiTietRepository.findById(sampleSpct.getId()).orElseThrow();
        assertThat(updated.getSoLuongSpLoi()).isEqualTo(3);
        assertThat(updated.getSoLuongTon()).isEqualTo(12);

        List<EditLog> logs = editLogRepository.findLichSuXuLyKhoLoi(sampleSpct.getId());
        assertThat(logs).isNotEmpty();
        EditLog latest = logs.get(0);
        assertThat(latest.getGhiChu()).contains("[KHO_LOI_SUA_XONG_NHAP_LAI_KHO]");
        assertThat(latest.getGhiChu()).contains("soLuong=2");
        assertThat(latest.getGiaTriCu()).contains("soLuongTon=10");
        assertThat(latest.getGiaTriMoi()).contains("soLuongTon=12");
    }

    @Test
    @DisplayName("Phase 3 - Case 4: Xử lý số lượng vượt quá tồn kho lỗi -> Ném ngoại lệ, không sửa DB")
    void testXuLyVuotQuaTonKhoLoi_ThrowsException() {
        assertThatThrownBy(() -> inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "TIEU_HUY",
                6, // hiện có 5
                "Thử tiêu hủy quá số lượng",
                qlAccount.getId(),
                "QL",
                "127.0.0.1"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("vượt quá");

        SanPhamChiTiet unchanged = sanPhamChiTietRepository.findById(sampleSpct.getId()).orElseThrow();
        assertThat(unchanged.getSoLuongSpLoi()).isEqualTo(5);
    }

    @Test
    @DisplayName("Phase 3 - Case 5: Số lượng 0 hoặc âm -> Ném ngoại lệ, không sửa DB")
    void testXuLySoLuongKhongHopLe_ThrowsException() {
        assertThatThrownBy(() -> inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "TIEU_HUY",
                0,
                "Lý do",
                qlAccount.getId(),
                "QL",
                "127.0.0.1"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "TIEU_HUY",
                -1,
                "Lý do",
                qlAccount.getId(),
                "QL",
                "127.0.0.1"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Phase 3 - Case 6: Ghi chú trống -> Ném ngoại lệ, không sửa DB")
    void testGhiChuTrong_ThrowsException() {
        assertThatThrownBy(() -> inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "TIEU_HUY",
                1,
                "   ",
                qlAccount.getId(),
                "QL",
                "127.0.0.1"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Ghi chú");
    }

    @Test
    @DisplayName("Phase 3 - Case 7: Hành động không hợp lệ -> Ném ngoại lệ, không sửa DB")
    void testHanhDongKhongHopLe_ThrowsException() {
        assertThatThrownBy(() -> inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "THANH_LY_BAN_RE",
                1,
                "Lý do test",
                qlAccount.getId(),
                "QL",
                "127.0.0.1"
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Hành động");
    }

    @Test
    @DisplayName("Phase 3 - Case 8: Phân quyền NV không được quyền Tiêu hủy (AccessDeniedException)")
    void testPhanQuyen_NV_KhongDuocTieuHuy() {
        assertThatThrownBy(() -> inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "TIEU_HUY",
                1,
                "Nhân viên thử tiêu hủy",
                nvAccount.getId(),
                "NV",
                "127.0.0.1"
        )).isInstanceOf(AccessDeniedException.class)
          .hasMessageContaining("Chỉ Quản lý (QL)");
    }

    @Test
    @DisplayName("Phase 3 - Case 9: MockMvc Controller POST endpoint thành công và redirect flash message")
    void testController_PostXuLyEndpoint_Success() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("vaiTro", "QL");
        session.setAttribute("idNguoiDung", qlAccount.getId());

        mockMvc.perform(post("/admin/kho-san-pham-loi/" + sampleSpct.getId() + "/xu-ly")
                        .session(session)
                        .param("hanhDong", "SUA_XONG_NHAP_LAI_KHO")
                        .param("soLuong", "1")
                        .param("ghiChu", "Sửa xong qua Web Controller"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/kho-san-pham-loi/" + sampleSpct.getId()))
                .andExpect(flash().attributeExists("successMsg"));

        SanPhamChiTiet updated = sanPhamChiTietRepository.findById(sampleSpct.getId()).orElseThrow();
        assertThat(updated.getSoLuongSpLoi()).isEqualTo(4);
        assertThat(updated.getSoLuongTon()).isEqualTo(11);
    }

    @Test
    @DisplayName("Phase 3 - Case 10: MockMvc Controller POST Tiêu hủy bởi NV -> Bị chặn và có errorMsg flash")
    void testController_PostTieuHuyByNV_BlockedWithErrorFlash() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("vaiTro", "NV");
        session.setAttribute("idNguoiDung", nvAccount.getId());

        mockMvc.perform(post("/admin/kho-san-pham-loi/" + sampleSpct.getId() + "/xu-ly")
                        .session(session)
                        .param("hanhDong", "TIEU_HUY")
                        .param("soLuong", "1")
                        .param("ghiChu", "NV gửi form tiêu hủy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/kho-san-pham-loi/" + sampleSpct.getId()))
                .andExpect(flash().attributeExists("errorMsg"));

        SanPhamChiTiet unchanged = sanPhamChiTietRepository.findById(sampleSpct.getId()).orElseThrow();
        assertThat(unchanged.getSoLuongSpLoi()).isEqualTo(5);
    }

    @Test
    @DisplayName("Phase 3 - Case 11: Khách hàng (KH) không được phép truy cập POST endpoint")
    void testController_PostByKH_Redirects() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("vaiTro", "KH");
        session.setAttribute("idNguoiDung", khAccount.getId());

        // SecurityConfig or AdminController redirects non-admin
        mockMvc.perform(post("/admin/kho-san-pham-loi/" + sampleSpct.getId() + "/xu-ly")
                        .session(session)
                        .param("hanhDong", "SUA_XONG_NHAP_LAI_KHO")
                        .param("soLuong", "1")
                        .param("ghiChu", "Khách hàng thử gọi"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Phase 3 - Case 12: Đọc chi tiết bao gồm Lịch sử xử lý kho lỗi và sanitize XSS")
    void testLayChiTiet_IncludesLichSuXuLyAndSanitizedXss() {
        inventoryLotService.xuLySanPhamLoi(
                sampleSpct.getId(),
                "TIEU_HUY",
                1,
                "<script>alert('XSS')</script> Vợt gãy",
                qlAccount.getId(),
                "QL",
                "127.0.0.1"
        );

        KhoSanPhamLoiDetailView detail = inventoryLotService.layChiTietKhoSanPhamLoi(sampleSpct.getId());
        assertThat(detail).isNotNull();
        assertThat(detail.getLichSuXuLyKhoLoi()).isNotEmpty();

        KhoSanPhamLoiLichSuXuLyView item = detail.getLichSuXuLyKhoLoi().get(0);
        assertThat(item.getHanhDong()).isEqualTo("Tiêu hủy");
        assertThat(item.getSoLuong()).isEqualTo(1);
        assertThat(item.getGhiChu()).doesNotContain("<script>");
        assertThat(item.getGhiChu()).contains("&lt;script&gt;");
        assertThat(item.getNguoiThucHien()).contains("Quản Lý Test");
        assertThat(item.getVaiTroThucHien()).isEqualTo("Quản lý");
    }
}
