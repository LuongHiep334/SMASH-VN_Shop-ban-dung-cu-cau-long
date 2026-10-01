package com.smashvn.shop.controller.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.dto.cart.CartItemView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class CartRuntimeRegressionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Cart Test 1: Add Exact SPCT (Astrox Lite 43i Aqua Blue SPCT ID 25)")
    void testAddExactSpctGuest() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Add SPCT 25 (Astrox Lite 43i - Aqua Blue)
        MvcResult result = mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "2"))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResp = result.getResponse().getContentAsString();
        Map<String, Object> respMap = objectMapper.readValue(jsonResp, Map.class);
        assertEquals("ok", respMap.get("trangThai"));
        assertNotNull(respMap.get("giaBan"));

        // Verify cart page loads with exact SPCT 25
        MvcResult cartResult = mockMvc.perform(get("/gio-hang").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("danhSachCart"))
                .andExpect(model().attributeExists("tongTien"))
                .andReturn();

        List<CartItemView> cartList = (List<CartItemView>) cartResult.getModelAndView().getModel().get("danhSachCart");
        assertEquals(1, cartList.size());
        assertEquals(25, cartList.get(0).getIdSanPhamChiTiet());
        assertEquals(2, cartList.get(0).getSoLuong());
        assertEquals(new BigDecimal("709000.00"), cartList.get(0).getDonGia());
        assertEquals(new BigDecimal("1418000.00"), cartList.get(0).getThanhTien());
    }

    @Test
    @DisplayName("Cart Test 2: Multi-color/size Shoes (Yonex Eclipsion X3 SPCT ID 41)")
    void testAddShoesMultiColorSizeSpct() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // SPCT 41: Eclipsion X3 Trắng size 39
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "41")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult cartResult = mockMvc.perform(get("/gio-hang").session(session))
                .andExpect(status().isOk())
                .andReturn();

        List<CartItemView> cartList = (List<CartItemView>) cartResult.getModelAndView().getModel().get("danhSachCart");
        assertEquals(1, cartList.size());
        assertEquals(41, cartList.get(0).getIdSanPhamChiTiet());
        assertEquals(new BigDecimal("2100000.00"), cartList.get(0).getDonGia());
    }

    @Test
    @DisplayName("Cart Test 3: Single-SPCT Accessory (GOSEN Ryzonic 62 SPCT ID 112)")
    void testAddSingleSpctAccessory() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "112")
                        .param("soLuong", "3"))
                .andExpect(status().isOk());

        MvcResult cartResult = mockMvc.perform(get("/gio-hang").session(session))
                .andExpect(status().isOk())
                .andReturn();

        List<CartItemView> cartList = (List<CartItemView>) cartResult.getModelAndView().getModel().get("danhSachCart");
        assertEquals(1, cartList.size());
        assertEquals(112, cartList.get(0).getIdSanPhamChiTiet());
        assertEquals(3, cartList.get(0).getSoLuong());
        assertEquals(new BigDecimal("150000.00"), cartList.get(0).getDonGia());
    }

    @Test
    @DisplayName("Cart Test 4: Duplicate Add Merges Quantity into Single Cart Row")
    void testDuplicateAddMergesQuantity() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Add 2 of SPCT 25
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "2"))
                .andExpect(status().isOk());

        // Add 3 more of SPCT 25
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "3"))
                .andExpect(status().isOk());

        MvcResult cartResult = mockMvc.perform(get("/gio-hang").session(session))
                .andExpect(status().isOk())
                .andReturn();

        List<CartItemView> cartList = (List<CartItemView>) cartResult.getModelAndView().getModel().get("danhSachCart");
        assertEquals(1, cartList.size(), "Duplicate add must merge into a single row");
        assertEquals(5, cartList.get(0).getSoLuong(), "Merged quantity must equal sum (2 + 3 = 5)");
    }

    @Test
    @DisplayName("Cart Test 5: Different Variants of Same Product Form Separate Rows")
    void testDifferentVariantsFormSeparateRows() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Add SPCT 25 (Astrox Lite 43i - Aqua Blue)
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        // Add SPCT 26 (Astrox Lite 43i - Green)
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "26")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        MvcResult cartResult = mockMvc.perform(get("/gio-hang").session(session))
                .andExpect(status().isOk())
                .andReturn();

        List<CartItemView> cartList = (List<CartItemView>) cartResult.getModelAndView().getModel().get("danhSachCart");
        assertEquals(2, cartList.size(), "Different variants must create 2 distinct cart rows");
    }

    @Test
    @DisplayName("Cart Test 6: Quantity Updates and Stock Limit Enforcement")
    void testQuantityUpdatesAndStockLimits() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Add 1 of SPCT 25 (Stock = 20)
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        // Update quantity to 10 -> Should succeed
        mockMvc.perform(post("/gio-hang/cap-nhat")
                        .session(session)
                        .param("idChiTiet", "25")
                        .param("soLuong", "10"))
                .andExpect(status().isOk());

        // Update quantity to 25 (> Stock 20) -> Should fail with 400 Bad Request
        mockMvc.perform(post("/gio-hang/cap-nhat")
                        .session(session)
                        .param("idChiTiet", "25")
                        .param("soLuong", "25"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cart Test 7: Direct Request Tampering & Invalid Inputs (0, negative, invalid SPCT)")
    void testRequestTamperingAndInvalidInputs() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Quantity <= 0
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "-5"))
                .andExpect(status().isBadRequest());

        // Invalid SPCT ID (99999)
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "99999")
                        .param("soLuong", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cart Test 8: Item Removal and Cart Total Recalculation")
    void testItemRemovalAndTotalCalculation() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // Add SPCT 25 (709,000 đ) x 2
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "25")
                        .param("soLuong", "2"))
                .andExpect(status().isOk());

        // Add SPCT 112 (150,000 đ) x 1
        mockMvc.perform(post("/gio-hang/them")
                        .session(session)
                        .param("idSanPhamChiTiet", "112")
                        .param("soLuong", "1"))
                .andExpect(status().isOk());

        // Remove SPCT 25
        mockMvc.perform(post("/gio-hang/api/xoa/25").session(session))
                .andExpect(status().isOk());

        MvcResult cartResult = mockMvc.perform(get("/gio-hang").session(session))
                .andExpect(status().isOk())
                .andReturn();

        List<CartItemView> cartList = (List<CartItemView>) cartResult.getModelAndView().getModel().get("danhSachCart");
        assertEquals(1, cartList.size());
        assertEquals(112, cartList.get(0).getIdSanPhamChiTiet());

        BigDecimal tongTien = (BigDecimal) cartResult.getModelAndView().getModel().get("tongTien");
        assertEquals(new BigDecimal("150000.00"), tongTien);
    }
}
