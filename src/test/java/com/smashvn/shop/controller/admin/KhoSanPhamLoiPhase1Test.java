package com.smashvn.shop.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.smashvn.shop.dto.inventory.KhoSanPhamLoiView;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.NhanVien;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.NhanVienRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.inventory.InventoryLotService;

@SpringBootTest
@Transactional
public class KhoSanPhamLoiPhase1Test {

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
    private InventoryLotService inventoryLotService;

    private MockMvc mockMvc;

    private SanPham sampleProduct;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        NhanVien nv = nhanVienRepository.findAll().stream().findFirst().orElseGet(() -> {
            TaiKhoan tk = new TaiKhoan();
            tk.setUsername("staff_test_" + System.nanoTime() + "@smashvn.com");
            tk.setMatKhau("123456");
            tk.setVaiTro("NV");
            tk.setTrangThai("hoat_dong");
            tk = taiKhoanRepository.save(tk);

            NhanVien n = new NhanVien();
            n.setTaiKhoan(tk);
            n.setHoTenNv("Nhan Vien Test");
            n.setSoDienThoaiNv("0987654321");
            return nhanVienRepository.save(n);
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
        sampleProduct.setTenSanPham("Vot Test Kho Loi " + System.nanoTime());
        sampleProduct.setDanhMuc(dm);
        sampleProduct.setThuongHieu(th);
        sampleProduct.setNhanVien(nv);
        sampleProduct.setTrangThai("dang_ban");
        sampleProduct = sanPhamRepository.save(sampleProduct);
    }

    /**
     * Case 1: SPCT co soLuongSpLoi = 0 KHONG xuat hien trong danh sach
     */
    @Test
    void testCase1_spctSoLuongSpLoiZero_khongXuatHien() {
        SanPhamChiTiet spctA = new SanPhamChiTiet();
        spctA.setSanPham(sampleProduct);
        spctA.setGiaBan(BigDecimal.valueOf(1000000));
        spctA.setSoLuongTon(10);
        spctA.setSoLuongSpLoi(0);
        spctA = sanPhamChiTietRepository.save(spctA);

        List<KhoSanPhamLoiView> list = inventoryLotService.layDanhSachKhoSanPhamLoi();
        final Integer targetId = spctA.getId();
        boolean found = list.stream().anyMatch(item -> item.getIdSanPhamChiTiet().equals(targetId));
        assertFalse(found, "SPCT co soLuongSpLoi = 0 khong duoc xuat hien trong danh sach");
    }

    /**
     * Case 2: SPCT B co soLuongSpLoi = 2 xuat hien dung so luong
     */
    @Test
    void testCase2_spctSoLuongSpLoiGreaterThanZero_xuatHienDung() {
        SanPhamChiTiet spctB = new SanPhamChiTiet();
        spctB.setSanPham(sampleProduct);
        spctB.setGiaBan(BigDecimal.valueOf(1200000));
        spctB.setSoLuongTon(5);
        spctB.setSoLuongSpLoi(2);
        spctB = sanPhamChiTietRepository.save(spctB);

        List<KhoSanPhamLoiView> list = inventoryLotService.layDanhSachKhoSanPhamLoi();
        final Integer targetId = spctB.getId();
        KhoSanPhamLoiView found = list.stream()
                .filter(item -> item.getIdSanPhamChiTiet().equals(targetId))
                .findFirst()
                .orElse(null);

        assertTrue(found != null, "SPCT B phai xuat hien trong danh sach");
        assertEquals(2, found.getSoLuongSpLoi());
        assertEquals(5, found.getSoLuongTon());
        assertEquals(sampleProduct.getTenSanPham(), found.getTenSanPham());
    }

    /**
     * Case 3: SPCT C (loi = 5) xuat hien truoc SPCT B (loi = 2) theo thu tu DESC
     */
    @Test
    void testCase3_sapXepGiamDanTheoSoLuongSpLoi() {
        SanPhamChiTiet spctB = new SanPhamChiTiet();
        spctB.setSanPham(sampleProduct);
        spctB.setGiaBan(BigDecimal.valueOf(1000000));
        spctB.setSoLuongTon(5);
        spctB.setSoLuongSpLoi(2);
        spctB = sanPhamChiTietRepository.save(spctB);

        SanPhamChiTiet spctC = new SanPhamChiTiet();
        spctC.setSanPham(sampleProduct);
        spctC.setGiaBan(BigDecimal.valueOf(1500000));
        spctC.setSoLuongTon(8);
        spctC.setSoLuongSpLoi(5);
        spctC = sanPhamChiTietRepository.save(spctC);

        List<KhoSanPhamLoiView> list = inventoryLotService.layDanhSachKhoSanPhamLoi();
        final Integer idB = spctB.getId();
        final Integer idC = spctC.getId();

        int indexB = -1;
        int indexC = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getIdSanPhamChiTiet().equals(idB)) indexB = i;
            if (list.get(i).getIdSanPhamChiTiet().equals(idC)) indexC = i;
        }

        assertTrue(indexC != -1, "SPCT C phai ton tai trong list");
        assertTrue(indexB != -1, "SPCT B phai ton tai trong list");
        assertTrue(indexC < indexB, "SPCT C (soLuongSpLoi=5) phai dung truoc SPCT B (soLuongSpLoi=2)");
    }

    /**
     * Case 4: Khi khong co SP loi -> danh sach rong, khong gay loi
     */
    @Test
    void testCase4_emptyStateWhenNoDefective() {
        List<SanPhamChiTiet> all = sanPhamChiTietRepository.findAll();
        for (SanPhamChiTiet s : all) {
            s.setSoLuongSpLoi(0);
        }
        sanPhamChiTietRepository.saveAll(all);

        List<KhoSanPhamLoiView> list = inventoryLotService.layDanhSachKhoSanPhamLoi();
        assertTrue(list.isEmpty(), "Danh sach phai rong khi khong co san pham loi");
    }

    /**
     * Case 5: QL truy cap GET /admin/kho-san-pham-loi -> 200 OK, Model day du
     */
    @Test
    void testCase5_qlTruyCap_200Ok() throws Exception {
        mockMvc.perform(get("/admin/kho-san-pham-loi")
                .sessionAttr("vaiTro", "QL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/kho-san-pham-loi"))
                .andExpect(model().attributeExists("danhSachKhoLoi"))
                .andExpect(model().attributeExists("tongSoLuongSanPhamLoi"))
                .andExpect(model().attributeExists("soBienTheCoLoi"));
    }

    /**
     * Case 6: NV truy cap GET /admin/kho-san-pham-loi -> 200 OK, Model day du
     */
    @Test
    void testCase6_nvTruyCap_200Ok() throws Exception {
        mockMvc.perform(get("/admin/kho-san-pham-loi")
                .sessionAttr("vaiTro", "NV"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/kho-san-pham-loi"))
                .andExpect(model().attributeExists("danhSachKhoLoi"))
                .andExpect(model().attributeExists("tongSoLuongSanPhamLoi"))
                .andExpect(model().attributeExists("soBienTheCoLoi"));
    }

    /**
     * Case 7: Unauthenticated / KH khong co session hop le -> redirect dang nhap
     */
    @Test
    void testCase7_unauthenticatedOrKh_redirectDangNhap() throws Exception {
        mockMvc.perform(get("/admin/kho-san-pham-loi"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dang-nhap"));
    }
}