package com.smashvn.shop.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProfanityFilterTest {
    private final ProfanityFilter filter = new ProfanityFilter();

    @Test
    void filtersAllOccurrencesIgnoringCaseAndAccents() {
        ProfanityFilter.FilterResult result = filter.filterWithResult(
                "Đồ TỆ HẠI, te hai thật tệ hại", List.of("tệ hại"));
        assertEquals("Đồ ***, *** thật ***", result.content());
        assertEquals(3, result.replacementCount());
    }

    @Test
    void filtersInsertedSpacesAndSpecialCharacters() {
        assertEquals("Sản phẩm ***", filter.filter("Sản phẩm x.a.u", List.of("xấu")));
        assertEquals("Sản phẩm ***", filter.filter("Sản phẩm x a u", List.of("xấu")));
    }

    @Test
    void doesNotFilterKeywordInsideAValidWord() {
        assertEquals("Chất lượng caocấp", filter.filter("Chất lượng caocấp", List.of("cao")));
        assertEquals("scao không khớp", filter.filter("scao không khớp", List.of("cao")));
    }

    @Test
    void handlesRegexCharactersLiterallyAndDoesNotMutateKeywords() {
        List<String> keywords = new java.util.ArrayList<>(List.of("c++", "a.b"));
        assertEquals("Học *** và ***", filter.filter("Học C++ và a.b", keywords));
        assertEquals(List.of("c++", "a.b"), keywords);
    }

    @Test
    void ignoresNullAndBlankKeywordsAndPreservesHtmlForTemplateEscaping() {
        List<String> keywords = new java.util.ArrayList<>();
        keywords.add(null);
        keywords.add("  ");
        keywords.add("xấu");
        assertEquals("<script>alert(1)</script> ***", filter.filter("<script>alert(1)</script> xấu", keywords));
    }
}
