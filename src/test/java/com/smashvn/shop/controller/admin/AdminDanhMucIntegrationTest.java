package com.smashvn.shop.controller.admin;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Category (DanhMuc) and Brand (ThuongHieu) validation hardening.
 *
 * Tests cover: add/edit success, duplicate detection (same/different case/extra spaces),
 * boundary lengths, Vietnamese Unicode support, and XSS payload sanitization.
 */
@SpringBootTest
@Transactional
public class AdminDanhMucIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    private MockMvc mockMvc;
    /** Session with admin role flags required by admin/layout/header SpEL expressions */
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        // Plain webAppContextSetup — no Spring Security filter chain
        // (matches the pattern used in all other passing integration tests in this project)
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        adminSession = new MockHttpSession();
        adminSession.setAttribute("vaiTro", "NV");
    }


    // ================================================================
    // CATEGORY — ADD
    // ================================================================

    @Test
    void addCategory_ValidName_Success() throws Exception {
        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "Vo Cau Long")
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/danh-muc"));

        assertTrue(danhMucRepository.existsByTenDanhMucIgnoreCase("Vo Cau Long"));
    }

    @Test
    void addCategory_EmptyName_ShowsError() throws Exception {
        long countBefore = danhMucRepository.count();

        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "   ")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(view().name("admin/danhmuc-list"))
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countBefore, danhMucRepository.count());
    }

    @Test
    void addCategory_NullName_ShowsError() throws Exception {
        long countBefore = danhMucRepository.count();

        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // no tenDanhMuc param
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(view().name("admin/danhmuc-list"))
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countBefore, danhMucRepository.count());
    }

    @Test
    void addCategory_TooShort_OneChar_Rejected() throws Exception {
        long countBefore = danhMucRepository.count();

        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "A")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countBefore, danhMucRepository.count());
    }

    @Test
    void addCategory_BoundaryMin_TwoChars_Accepted() throws Exception {
        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "AB")
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection());

        assertTrue(danhMucRepository.existsByTenDanhMucIgnoreCase("AB"));
    }

    @Test
    void addCategory_BoundaryMax_100Chars_Accepted() throws Exception {
        String name100 = "A".repeat(100);
        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", name100)
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection());

        assertTrue(danhMucRepository.existsByTenDanhMucIgnoreCase(name100));
    }

    @Test
    void addCategory_TooLong_101Chars_Rejected() throws Exception {
        long countBefore = danhMucRepository.count();
        String name101 = "A".repeat(101);

        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", name101)
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countBefore, danhMucRepository.count());
    }

    @Test
    void addCategory_DuplicateSameCase_Rejected() throws Exception {
        // Seed "Nike"
        DanhMuc existing = new DanhMuc();
        existing.setTenDanhMuc("Nike");
        danhMucRepository.save(existing);
        long countAfterSeed = danhMucRepository.count();

        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "Nike")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countAfterSeed, danhMucRepository.count());
    }

    @Test
    void addCategory_DuplicateDifferentCase_Rejected() throws Exception {
        DanhMuc existing = new DanhMuc();
        existing.setTenDanhMuc("Nike");
        danhMucRepository.save(existing);
        long countAfterSeed = danhMucRepository.count();

        // "NIKE" must be treated as duplicate of "Nike"
        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "NIKE")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countAfterSeed, danhMucRepository.count());
    }

    @Test
    void addCategory_DuplicateWithLeadingTrailingSpaces_Rejected() throws Exception {
        DanhMuc existing = new DanhMuc();
        existing.setTenDanhMuc("Nike");
        danhMucRepository.save(existing);
        long countAfterSeed = danhMucRepository.count();

        // "  nike  " must be treated as duplicate after normalization
        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "  nike  ")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countAfterSeed, danhMucRepository.count());
    }

    @Test
    void addCategory_VietnameseUnicode_Accepted() throws Exception {
        String[] names = {"Ao Thun", "Dien Tu", "Thoi Trang Nam"};
        for (String name : names) {
            mockMvc.perform(post("/admin/danh-muc/them")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("tenDanhMuc", name)
                    .session(adminSession)
)
                    .andExpect(status().is3xxRedirection());

            assertTrue(danhMucRepository.existsByTenDanhMucIgnoreCase(name),
                    "Vietnamese name should be accepted: " + name);
        }
    }

    @Test
    void addCategory_XssPayload_SanitizedAndSaved() throws Exception {
        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "<b>Nike</b>")
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection());

        // Tags stripped: "Nike" should be in DB, "<b>Nike</b>" must NOT
        assertTrue(danhMucRepository.existsByTenDanhMucIgnoreCase("Nike"),
                "Sanitized name 'Nike' should be stored");
        assertFalse(danhMucRepository.existsByTenDanhMucIgnoreCase("<b>Nike</b>"),
                "Raw XSS name must NOT be stored");
    }

    @Test
    void addCategory_XssScriptTag_Rejected() throws Exception {
        long countBefore = danhMucRepository.count();

        // After sanitize: content is empty → rejected
        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "<script>alert(1)</script>")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countBefore, danhMucRepository.count());
    }

    @Test
    void addCategory_XssImgTag_SanitizedOrRejected() throws Exception {
        // <img src=x onerror=alert(1)> → sanitized to empty → rejected
        long countBefore = danhMucRepository.count();

        mockMvc.perform(post("/admin/danh-muc/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "<img src=x onerror=alert(1)>")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiDanhMuc"));

        assertEquals(countBefore, danhMucRepository.count());
    }

    // ================================================================
    // CATEGORY — EDIT
    // ================================================================

    @Test
    void editCategory_KeepSameName_Success() throws Exception {
        DanhMuc dm = new DanhMuc();
        dm.setTenDanhMuc("Yonex");
        dm = danhMucRepository.save(dm);

        // Edit back to same name — must succeed (self-exclude)
        mockMvc.perform(post("/admin/danh-muc/sua/" + dm.getId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "Yonex")
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/danh-muc"));
    }

    @Test
    void editCategory_ChangeToExistingName_Rejected() throws Exception {
        DanhMuc dmA = new DanhMuc(); dmA.setTenDanhMuc("Nike"); dmA = danhMucRepository.save(dmA);
        DanhMuc dmB = new DanhMuc(); dmB.setTenDanhMuc("Adidas"); dmB = danhMucRepository.save(dmB);

        // Try to rename "Adidas" → "nike" (case-insensitive duplicate of dmA)
        mockMvc.perform(post("/admin/danh-muc/sua/" + dmB.getId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenDanhMuc", "nike")
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/danh-muc"))
                .andExpect(flash().attributeExists("errorMessage"));

        // dmB name must remain "Adidas"
        DanhMuc updated = danhMucRepository.findById(dmB.getId()).orElseThrow();
        assertEquals("Adidas", updated.getTenDanhMuc());
    }

    // ================================================================
    // BRAND (ThuongHieu) — ADD
    // ================================================================

    @Test
    void addBrand_ValidName_Success() throws Exception {
        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "TestBrandUnique9834")
                .session(adminSession))
                .andExpect(status().is3xxRedirection());

        assertTrue(thuongHieuRepository.existsByTenThuongHieuIgnoreCase("TestBrandUnique9834"));
    }

    @Test
    void addBrand_EmptyName_ShowsError() throws Exception {
        long countBefore = thuongHieuRepository.count();

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "   ")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiThuongHieu"));

        assertEquals(countBefore, thuongHieuRepository.count());
    }

    @Test
    void addBrand_NullName_ShowsError() throws Exception {
        long countBefore = thuongHieuRepository.count();

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiThuongHieu"));

        assertEquals(countBefore, thuongHieuRepository.count());
    }

    @Test
    void addBrand_TooShort_OneChar_Rejected() throws Exception {
        long countBefore = thuongHieuRepository.count();

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "X")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiThuongHieu"));

        assertEquals(countBefore, thuongHieuRepository.count());
    }

    @Test
    void addBrand_BoundaryMin_TwoChars_Accepted() throws Exception {
        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "LN")
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection());

        assertTrue(thuongHieuRepository.existsByTenThuongHieuIgnoreCase("LN"));
    }

    @Test
    void addBrand_BoundaryMax_100Chars_Accepted() throws Exception {
        String name100 = "B".repeat(100);
        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", name100)
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection());

        assertTrue(thuongHieuRepository.existsByTenThuongHieuIgnoreCase(name100));
    }

    @Test
    void addBrand_TooLong_101Chars_Rejected() throws Exception {
        long countBefore = thuongHieuRepository.count();
        String name101 = "B".repeat(101);

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", name101)
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiThuongHieu"));

        assertEquals(countBefore, thuongHieuRepository.count());
    }

    @Test
    void addBrand_DuplicateSameCase_Rejected() throws Exception {
        ThuongHieu existing = new ThuongHieu();
        existing.setTenThuongHieu("Adidas");
        thuongHieuRepository.save(existing);
        long countAfterSeed = thuongHieuRepository.count();

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "Adidas")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiThuongHieu"));

        assertEquals(countAfterSeed, thuongHieuRepository.count());
    }

    @Test
    void addBrand_DuplicateDifferentCase_Rejected() throws Exception {
        ThuongHieu existing = new ThuongHieu();
        existing.setTenThuongHieu("Adidas");
        thuongHieuRepository.save(existing);
        long countAfterSeed = thuongHieuRepository.count();

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "ADIDAS")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiThuongHieu"));

        assertEquals(countAfterSeed, thuongHieuRepository.count());
    }

    @Test
    void addBrand_DuplicateWithSpaces_Rejected() throws Exception {
        ThuongHieu existing = new ThuongHieu();
        existing.setTenThuongHieu("Adidas");
        thuongHieuRepository.save(existing);
        long countAfterSeed = thuongHieuRepository.count();

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "  adidas  ")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiThuongHieu"));

        assertEquals(countAfterSeed, thuongHieuRepository.count());
    }

    @Test
    void addBrand_VietnameseUnicode_Accepted() throws Exception {
        String[] names = {"Vo Cau Long Viet", "Phu Kien The Thao", "Hang Noi Dia"};
        for (String name : names) {
            mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("tenThuongHieu", name)
                    .session(adminSession)
)
                    .andExpect(status().is3xxRedirection());

            assertTrue(thuongHieuRepository.existsByTenThuongHieuIgnoreCase(name),
                    "Vietnamese brand name should be accepted: " + name);
        }
    }

    @Test
    void addBrand_XssPayload_SanitizedAndSaved() throws Exception {
        // <b>TestBrand9835</b> sanitizes to "TestBrand9835" — a unique name not in seeded DB
        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "<b>TestBrand9835</b>")
                .session(adminSession))
                .andExpect(status().is3xxRedirection());

        assertTrue(thuongHieuRepository.existsByTenThuongHieuIgnoreCase("TestBrand9835"),
                "Sanitized name 'TestBrand9835' should be stored");
        assertFalse(thuongHieuRepository.existsByTenThuongHieuIgnoreCase("<b>TestBrand9835</b>"),
                "Raw XSS name must NOT be stored");
    }

    @Test
    void addBrand_XssSvgTag_Rejected() throws Exception {
        long countBefore = thuongHieuRepository.count();

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/them")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "<svg onload=alert(1)>")
                .session(adminSession)
)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("loiThuongHieu"));

        assertEquals(countBefore, thuongHieuRepository.count());
    }

    // ================================================================
    // BRAND — EDIT
    // ================================================================

    @Test
    void editBrand_KeepSameName_Success() throws Exception {
        ThuongHieu th = new ThuongHieu();
        th.setTenThuongHieu("Victor");
        th = thuongHieuRepository.save(th);

        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/sua/" + th.getId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "Victor")
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/danh-muc"));
    }

    @Test
    void editBrand_ChangeToExistingName_Rejected() throws Exception {
        ThuongHieu thA = new ThuongHieu(); thA.setTenThuongHieu("Yonex"); thA = thuongHieuRepository.save(thA);
        ThuongHieu thB = new ThuongHieu(); thB.setTenThuongHieu("Victor"); thB = thuongHieuRepository.save(thB);

        // Rename "Victor" → "YONEX" (case-insensitive duplicate)
        mockMvc.perform(post("/admin/danh-muc/thuong-hieu/sua/" + thB.getId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("tenThuongHieu", "YONEX")
                .session(adminSession)
)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/danh-muc"))
                .andExpect(flash().attributeExists("errorMessage"));

        ThuongHieu updated = thuongHieuRepository.findById(thB.getId()).orElseThrow();
        assertEquals("Victor", updated.getTenThuongHieu());
    }
}
