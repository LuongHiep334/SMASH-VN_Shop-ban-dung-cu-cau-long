package com.smashvn.shop.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.*;
import com.smashvn.shop.repository.*;
import com.smashvn.shop.dao.*;
import com.smashvn.shop.service.admin.AdminPosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class AdminPosIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private PhuongThucThanhToanDAO phuongThucThanhToanDAO;

    @Autowired
    private DonViVanChuyenDAO donViVanChuyenDAO;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    private MockMvc mockMvc;
    private MockHttpSession session;
    private TaiKhoan staffUser;
    private NhanVien staff;
    private SanPhamChiTiet spct;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Seed staff user
        staffUser = new TaiKhoan();
        staffUser.setUsername("pos_staff@gmail.com");
        staffUser.setMatKhau("staffpass123");
        staffUser.setVaiTro("NV");
        staffUser.setTrangThai("hoat_dong");

        staffUser = taiKhoanRepository.save(staffUser);

        staff = new NhanVien();
        staff.setTaiKhoan(staffUser);
        staff.setHoTenNv("Staff Tester");
        staff.setChucVu("Nhân viên bán hàng");
        staff.setSoDienThoaiNv("0912345670");
        staff = nhanVienRepository.save(staff);

        session = new MockHttpSession();
        session.setAttribute("idNguoiDung", staffUser.getId());
        session.setAttribute("vaiTro", "NV");
        session.setAttribute("vaiTro", "NV");
        // Seed product
        DanhMuc dm = danhMucRepository.findAll().stream().findFirst().orElseGet(() -> {
            DanhMuc newDm = new DanhMuc();
            newDm.setTenDanhMuc("DanhMucTest");
            return danhMucRepository.save(newDm);
        });

        ThuongHieu th = thuongHieuRepository.findAll().stream().findFirst().orElseGet(() -> {
            ThuongHieu newTh = new ThuongHieu();
            newTh.setTenThuongHieu("ThuongHieuTest");
            return thuongHieuRepository.save(newTh);
        });

        SanPham sp = new SanPham();
        sp.setTenSanPham("SanPhamTest");
        sp.setTrangThai("dang_ban");
        sp.setDanhMuc(dm);
        sp.setThuongHieu(th);
        sp.setNhanVien(staff);
        sp = sanPhamRepository.save(sp);

        spct = new SanPhamChiTiet();
        spct.setSanPham(sp);
        spct.setMauSac("Trắng");
        spct.setTrongLuong("3U");
        spct.setMucCang("28 lbs");
        spct.setSoLuongTon(10);
        spct.setGiaBan(new BigDecimal("150000"));
        spct = sanPhamChiTietRepository.save(spct);

        // Seed base payment methods if empty
        if (phuongThucThanhToanDAO.count() == 0) {
            PhuongThucThanhToan pttt1 = new PhuongThucThanhToan();
            pttt1.setTenPhuongThuc("COD");
            phuongThucThanhToanDAO.save(pttt1);
        }
    }

    @Test
    void testPosCheckoutCash_Success() throws Exception {
        AdminPosController.PosCheckoutRequest req = new AdminPosController.PosCheckoutRequest();
        req.idKhachHang = -1;
        req.maVoucher = "";
        req.phuongThucPos = "TIEN_MAT";
        req.maGiaoDich = "";
        req.ghiChu = "Test cash checkout";
        req.items = new ArrayList<>();
        AdminPosService.PosItem item = new AdminPosService.PosItem();
        item.idSanPhamChiTiet = spct.getId();
        item.soLuong = 2;
        req.items.add(item);

        mockMvc.perform(post("/admin/pos/checkout")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify stock is decremented
        SanPhamChiTiet updatedSpct = sanPhamChiTietRepository.findById(spct.getId()).orElse(null);
        assertNotNull(updatedSpct);
        assertEquals(8, updatedSpct.getSoLuongTon());
    }

    @Test
    void testPosCheckoutMultipleCashAndPaymentMethod_Success() throws Exception {
        // First Checkout
        AdminPosController.PosCheckoutRequest req1 = new AdminPosController.PosCheckoutRequest();
        req1.idKhachHang = -1;
        req1.maVoucher = "";
        req1.phuongThucPos = "TIEN_MAT";
        req1.maGiaoDich = "";
        req1.ghiChu = "Test cash checkout 1";
        req1.items = new ArrayList<>();
        AdminPosService.PosItem item1 = new AdminPosService.PosItem();
        item1.idSanPhamChiTiet = spct.getId();
        item1.soLuong = 1;
        req1.items.add(item1);

        String responseJson1 = mockMvc.perform(post("/admin/pos/checkout")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        // Second Checkout (different request)
        AdminPosController.PosCheckoutRequest req2 = new AdminPosController.PosCheckoutRequest();
        req2.idKhachHang = -1;
        req2.maVoucher = "";
        req2.phuongThucPos = "TIEN_MAT";
        req2.maGiaoDich = "";
        req2.ghiChu = "Test cash checkout 2";
        req2.items = new ArrayList<>();
        AdminPosService.PosItem item2 = new AdminPosService.PosItem();
        item2.idSanPhamChiTiet = spct.getId();
        item2.soLuong = 1;
        req2.items.add(item2);

        String responseJson2 = mockMvc.perform(post("/admin/pos/checkout")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        // Parse response to find HoaDon ID and verify payment method
        var node1 = objectMapper.readTree(responseJson1);
        int hdId1 = node1.get("hoaDonId").asInt();
        HoaDon hd1 = hoaDonRepository.findById(hdId1).orElse(null);
        assertNotNull(hd1);
        assertNotNull(hd1.getPhuongThucThanhToan());
        assertEquals("Tiền mặt", hd1.getPhuongThucThanhToan().getTenPhuongThuc());

        var node2 = objectMapper.readTree(responseJson2);
        int hdId2 = node2.get("hoaDonId").asInt();
        HoaDon hd2 = hoaDonRepository.findById(hdId2).orElse(null);
        assertNotNull(hd2);
        assertNotNull(hd2.getPhuongThucThanhToan());
        assertEquals("Tiền mặt", hd2.getPhuongThucThanhToan().getTenPhuongThuc());
        assertNotEquals(hd1.getMaDonHang(), hd2.getMaDonHang());
    }
}
