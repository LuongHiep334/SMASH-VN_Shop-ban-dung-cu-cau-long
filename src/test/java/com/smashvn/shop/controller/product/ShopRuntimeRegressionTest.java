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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
public class ShopRuntimeRegressionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Runtime Test 1: Category Endpoints 1..8 Load Cleanly (HTTP 200)")
    void testCategoryEndpoints() throws Exception {
        int[] categoryIds = {1, 2, 3, 4, 5, 6, 7, 8};

        // All products
        mockMvc.perform(get("/shop"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("products"))
                .andExpect(view().name("shop"));

        // Each Category
        for (int catId : categoryIds) {
            mockMvc.perform(get("/shop").param("categoryId", String.valueOf(catId)))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("products"))
                    .andExpect(view().name("shop"));
        }
    }

    @Test
    @DisplayName("Runtime Test 2: Racket Facets (Color, Stiffness, Weight, Balance, Player, Tension)")
    void testRacketFacets() throws Exception {
        // Màu sắc
        mockMvc.perform(get("/shop").param("categoryId", "1").param("attr_1", "Light Beige"))
                .andExpect(status().isOk());

        // Độ cứng
        mockMvc.perform(get("/shop").param("categoryId", "1").param("attr_2", "Cứng"))
                .andExpect(status().isOk());

        // Trọng lượng
        mockMvc.perform(get("/shop").param("categoryId", "1").param("attr_3", "4U"))
                .andExpect(status().isOk());

        // Điểm cân bằng
        mockMvc.perform(get("/shop").param("categoryId", "1").param("attr_4", "Nặng đầu"))
                .andExpect(status().isOk());

        // Loại người chơi
        mockMvc.perform(get("/shop").param("categoryId", "1").param("attr_5", "Tấn công"))
                .andExpect(status().isOk());

        // Sức căng
        mockMvc.perform(get("/shop").param("categoryId", "1").param("attr_7", "30 lbs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Runtime Test 3: Complex Special Character Attribute Values")
    void testSpecialCharacterAttributeValues() throws Exception {
        String[] specialValues = {
                "Đen - Vàng (Lightning Yellow)",
                "Xanh / Nâu nhạt",
                "Black/Green",
                "Phản tạt, phòng thủ",
                "20 - 30 lbs",
                "30 - 31 lbs",
                "3F",
                "3U",
                "5U"
        };

        for (String val : specialValues) {
            MvcResult result = mockMvc.perform(get("/shop").param("categoryId", "1").param("attr_1", val).param("attr_7", val).param("attr_5", val))
                    .andExpect(status().isOk())
                    .andReturn();

            String html = result.getResponse().getContentAsString();
            assertNotNull(html, "Response content must not be null");
            assertFalse(html.contains("TemplateProcessingException"), "Response must not contain Thymeleaf TemplateProcessingException");
        }
    }

    @Test
    @DisplayName("Runtime Test 4: Multiple Filters Same Facet (OR) & Across Facets (AND)")
    void testMultipleFilters() throws Exception {
        // Across Facet (AND): 4U + Cứng
        mockMvc.perform(get("/shop")
                        .param("categoryId", "1")
                        .param("attr_3", "4U")
                        .param("attr_2", "Cứng"))
                .andExpect(status().isOk());

        // Across Facet (AND): 4U + Cứng + Tấn công
        mockMvc.perform(get("/shop")
                        .param("categoryId", "1")
                        .param("attr_3", "4U")
                        .param("attr_2", "Cứng")
                        .param("attr_5", "Tấn công"))
                .andExpect(status().isOk());

        // Same Facet (OR): 3U + 4U
        mockMvc.perform(get("/shop")
                        .param("categoryId", "1")
                        .param("attr_3", "3U")
                        .param("attr_3", "4U"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Runtime Test 5: Accessory Categories Render No Forbidden Group Names")
    void testAccessoryCategoriesNoForbiddenGroupNames() throws Exception {
        int[] accessoryCatIds = {5, 6, 7, 8}; // Balo, Túi, Cước, Quấn cán

        for (int catId : accessoryCatIds) {
            MvcResult result = mockMvc.perform(get("/shop").param("categoryId", String.valueOf(catId)))
                    .andExpect(status().isOk())
                    .andReturn();

            String html = result.getResponse().getContentAsString();
            assertFalse(html.contains("Số ngăn"), "Accessory view must not contain 'Số ngăn'");
            assertFalse(html.contains("Quy cách"), "Accessory view must not contain 'Quy cách'");
            assertFalse(html.contains("Grip"), "Accessory view must not contain 'Grip'");
        }
    }

    @Test
    @DisplayName("Runtime Test 6: Pagination and Sort Parameters")
    void testPaginationAndSort() throws Exception {
        String[] sortOptions = {"newest", "price_asc", "price_desc"};

        for (String sort : sortOptions) {
            mockMvc.perform(get("/shop")
                            .param("categoryId", "1")
                            .param("sort", sort)
                            .param("page", "0"))
                    .andExpect(status().isOk());
        }
    }
}
