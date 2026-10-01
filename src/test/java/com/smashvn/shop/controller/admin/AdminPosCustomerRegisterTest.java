package com.smashvn.shop.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.dto.user.PosRegisterCustomerRequest;
import com.smashvn.shop.dto.user.PosCustomerResponse;
import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
public class AdminPosCustomerRegisterTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private KhachHangRepository khachHangRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private MockHttpSession session;
    private TaiKhoan staffUser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Seed a staff user session
        staffUser = new TaiKhoan();
        staffUser.setUsername("pos_staff_test@gmail.com");
        staffUser.setMatKhau("staffpass123");
        staffUser.setVaiTro("NV");
        staffUser.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        staffUser = taiKhoanRepository.save(staffUser);

        session = new MockHttpSession();
        session.setAttribute("idNguoiDung", staffUser.getId());
        session.setAttribute("vaiTro", "NV");
    }

    @Test
    void testRegisterNewCustomer_Success() throws Exception {
        PosRegisterCustomerRequest req = PosRegisterCustomerRequest.builder()
                .hoTen("Nguyễn Văn A")
                .soDienThoai("0987654321")
                .build();

        mockMvc.perform(post("/admin/pos/customers/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.created").value(true))
                .andExpect(jsonPath("$.requiresConfirmation").value(false))
                .andExpect(jsonPath("$.customer.hoTen").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.customer.sdt").value("0987654321"));

        // Verify entities in database
        TaiKhoan tk = taiKhoanRepository.findByUsername("0987654321");
        assertNotNull(tk);
        assertEquals("KH", tk.getVaiTro());
        assertEquals(AccountStatus.ACTIVE, tk.getTrangThaiTaiKhoan());
        assertTrue(passwordEncoder.matches("12345678", tk.getMatKhau()));

        KhachHang kh = khachHangRepository.findBySoDienThoaiKh("0987654321");
        assertNotNull(kh);
        assertEquals("Nguyễn Văn A", kh.getHoTenKh());
        assertEquals(tk.getId(), kh.getTaiKhoan().getId());
    }

    @Test
    void testRegisterExistingCustomer_RequiresConfirmation() throws Exception {
        // Seed existing customer
        TaiKhoan customerTk = new TaiKhoan();
        customerTk.setUsername("0987654321");
        customerTk.setMatKhau(passwordEncoder.encode("oldpass123"));
        customerTk.setVaiTro("KH");
        customerTk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        customerTk = taiKhoanRepository.save(customerTk);

        KhachHang customerKh = new KhachHang();
        customerKh.setTaiKhoan(customerTk);
        customerKh.setHoTenKh("Khách Hàng Cũ");
        customerKh.setSoDienThoaiKh("0987654321");
        khachHangRepository.save(customerKh);

        PosRegisterCustomerRequest req = PosRegisterCustomerRequest.builder()
                .hoTen("Khách Hàng Mới")
                .soDienThoai("0987654321")
                .build();

        mockMvc.perform(post("/admin/pos/customers/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.created").value(false))
                .andExpect(jsonPath("$.requiresConfirmation").value(true))
                .andExpect(jsonPath("$.customer.hoTen").value("Khách Hàng Cũ"))
                .andExpect(jsonPath("$.customer.sdt").value("0987654321"));

        // Verify no changes to existing customer profile name
        KhachHang checkKh = khachHangRepository.findBySoDienThoaiKh("0987654321");
        assertEquals("Khách Hàng Cũ", checkKh.getHoTenKh());
    }

    @Test
    void testRegisterDuplicateUsername_Exception() throws Exception {
        // Seed existing account with username = phone, but no KhachHang linked
        TaiKhoan customerTk = new TaiKhoan();
        customerTk.setUsername("0987654321");
        customerTk.setMatKhau(passwordEncoder.encode("oldpass123"));
        customerTk.setVaiTro("NV");
        customerTk.setTrangThaiTaiKhoan(AccountStatus.ACTIVE);
        taiKhoanRepository.save(customerTk);

        PosRegisterCustomerRequest req = PosRegisterCustomerRequest.builder()
                .hoTen("Khách Hàng Trùng Username")
                .soDienThoai("0987654321")
                .build();

        mockMvc.perform(post("/admin/pos/customers/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Số điện thoại này đã được sử dụng!"));
    }

    @Test
    void testRegisterInvalidInput_ValidationErrors() throws Exception {
        PosRegisterCustomerRequest req = PosRegisterCustomerRequest.builder()
                .hoTen("")
                .soDienThoai("123")
                .build();

        mockMvc.perform(post("/admin/pos/customers/register")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterSecurity_Unauthorized() throws Exception {
        PosRegisterCustomerRequest req = PosRegisterCustomerRequest.builder()
                .hoTen("Nguyễn Văn A")
                .soDienThoai("0987654321")
                .build();

        mockMvc.perform(post("/admin/pos/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
