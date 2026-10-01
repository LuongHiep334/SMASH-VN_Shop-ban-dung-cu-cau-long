package com.smashvn.shop.controller.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.SanPham;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class ProductDetailRuntimeRegressionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Detail Test 1: Category Product Details Load Cleanly (HTTP 200)")
    void testProductDetailLoadAllCategories() throws Exception {
        // Sample product IDs from each category in seed:
        // 1: Vợt (SP 1, SP 2), 2: Giày (SP 28), 3: Áo (SP 34), 4: Quần (SP 38),
        // 5: Balo (SP 44), 6: Túi (SP 48), 7: Cước (SP 53), 8: Quấn cán (SP 58)
        int[] sampleProductIds = {1, 2, 28, 34, 38, 44, 48, 53, 58};

        for (int spId : sampleProductIds) {
            MvcResult result = mockMvc.perform(get("/san-pham/" + spId))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("sp"))
                    .andExpect(model().attributeExists("listChiTiet"))
                    .andExpect(model().attributeExists("listBienTheJS"))
                    .andReturn();

            String html = result.getResponse().getContentAsString();
            assertNotNull(html, "HTML content must not be null");
            assertFalse(html.contains("TemplateProcessingException"), "Response must not contain Thymeleaf exceptions");
        }
    }

    @Test
    @DisplayName("Detail Test 2: Racket Multi-Variant Data Integrity & Resolution")
    void testRacketMultiVariantResolution() throws Exception {
        // Racket SP 2 (Vợt cầu lông Lining Axforce 100 Gen 2)
        MvcResult result = mockMvc.perform(get("/san-pham/2"))
                .andExpect(status().isOk())
                .andReturn();

        Object jsVal = result.getModelAndView().getModel().get("listBienTheJS");
        assertNotNull(jsVal, "listBienTheJS model attribute must exist");

        String json = objectMapper.writeValueAsString(jsVal);
        List<Map<String, Object>> variants = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});

        assertFalse(variants.isEmpty(), "Racket variants must not be empty");
        for (Map<String, Object> varMap : variants) {
            assertNotNull(varMap.get("id"), "Variant ID must not be null");
            assertNotNull(varMap.get("giaBan"), "Variant price must not be null");
            assertNotNull(varMap.get("soLuongTon"), "Variant stock must not be null");
            assertTrue(((Number) varMap.get("soLuongTon")).intValue() >= 0, "Stock must be non-negative");
        }
    }

    @Test
    @DisplayName("Detail Test 3: Shoes Multi-Color & Size Variants Resolution")
    void testShoesMultiColorSizeResolution() throws Exception {
        // Shoes SP 28 (Giày cầu lông Lining AYZW007-3)
        MvcResult result = mockMvc.perform(get("/san-pham/28"))
                .andExpect(status().isOk())
                .andReturn();

        Object jsVal = result.getModelAndView().getModel().get("listBienTheJS");
        assertNotNull(jsVal);

        String json = objectMapper.writeValueAsString(jsVal);
        List<Map<String, Object>> variants = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});

        assertFalse(variants.isEmpty(), "Shoes variants must not be empty");

        // Verify distinct size / color resolution
        Set<Object> ids = new HashSet<>();
        for (Map<String, Object> varMap : variants) {
            ids.add(varMap.get("id"));
            BigDecimal giaBan = new BigDecimal(varMap.get("giaBan").toString());
            assertTrue(giaBan.compareTo(BigDecimal.ZERO) > 0, "Shoes variant price must be > 0");
        }
        assertEquals(variants.size(), ids.size(), "Each shoes variant must have a unique SPCT ID");
    }

    @Test
    @DisplayName("Detail Test 4: Apparel Size Resolution (S, M, L, XL)")
    void testApparelSizeResolution() throws Exception {
        // Áo SP 34 (Áo cầu lông Lining P-APLUA47-1)
        MvcResult result = mockMvc.perform(get("/san-pham/34"))
                .andExpect(status().isOk())
                .andReturn();

        Object jsVal = result.getModelAndView().getModel().get("listBienTheJS");
        assertNotNull(jsVal);

        String json = objectMapper.writeValueAsString(jsVal);
        List<Map<String, Object>> variants = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});

        assertEquals(4, variants.size(), "Apparel SP 34 must have 4 size variants (S, M, L, XL)");
    }

    @Test
    @DisplayName("Detail Test 5: Accessory Without EAV Loads Safely (No NPE / Empty Options)")
    void testAccessoryWithoutEavLoadsSafely() throws Exception {
        // Dây cước SP 53 (Dây cước căng vợt GOSEN Ryzonic 62)
        MvcResult result = mockMvc.perform(get("/san-pham/53"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        assertFalse(html.contains("NullPointerException"), "Accessory page must not throw NPE");
        assertFalse(html.contains("Số ngăn"), "Accessory page must not contain 'Số ngăn'");
        assertFalse(html.contains("Quy cách"), "Accessory page must not contain 'Quy cách'");
        assertFalse(html.contains("Grip"), "Accessory page must not contain 'Grip'");

        // Quấn cán SP 58
        MvcResult result58 = mockMvc.perform(get("/san-pham/58"))
                .andExpect(status().isOk())
                .andReturn();

        String html58 = result58.getResponse().getContentAsString();
        assertNotNull(html58);
    }

    @Test
    @DisplayName("Detail Test 6: Attribute Display Contract (No forbidden placeholders / metadata)")
    void testAttributeDisplayContract() throws Exception {
        int[] allTestIds = {1, 2, 28, 34, 38, 44, 48, 53, 58};

        String[] forbiddenStrings = {
                "Số ngăn",
                "Quy cách",
                "Màu mặc định",
                " Không xác định "
        };

        for (int id : allTestIds) {
            MvcResult result = mockMvc.perform(get("/san-pham/" + id))
                    .andExpect(status().isOk())
                    .andReturn();

            String html = result.getResponse().getContentAsString();
            for (String forbidden : forbiddenStrings) {
                assertFalse(html.contains(forbidden), "Product detail ID " + id + " must not contain forbidden text: '" + forbidden + "'");
            }
        }
    }

    @Test
    @DisplayName("Detail Test 7: Invalid Product ID Handles Gracefully with Generic Error View")
    void testInvalidProductIdReturnsErrorView() throws Exception {
        mockMvc.perform(get("/san-pham/99999"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/generic"))
                .andExpect(model().attributeExists("loi"));
    }
}
