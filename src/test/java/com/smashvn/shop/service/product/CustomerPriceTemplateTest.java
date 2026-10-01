package com.smashvn.shop.service.product;

import com.smashvn.shop.dto.product.CustomerPriceSummary;
import com.smashvn.shop.entity.SanPham;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CustomerPriceTemplateTest {
    private Document render(String file, String selector, CustomerPriceSummary price) throws Exception {
        String template = Files.readString(Path.of("src/main/resources/templates", file));
        String fragment = Jsoup.parse(template).select(selector).stream()
                .filter(element -> element.hasAttr("th:with")).findFirst().orElseThrow().outerHtml();
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        Context context = new Context(Locale.forLanguageTag("vi-VN"));
        context.setVariable("product", SanPham.builder().id(27).build());
        context.setVariable("customerPrices", Map.of(27, price));
        context.setVariable("customerPrice", price);
        return Jsoup.parse(engine.process(fragment, context));
    }

    @Test void shopRendersDiscountedFromPriceUsingRecordProperties() throws Exception {
        var doc = render("shop.html", ".product-m__price-wrap", new CustomerPriceSummary(
                new BigDecimal("1000000"), new BigDecimal("750000"), 25, true, true));
        assertEquals("1,000,000 đ", doc.selectFirst(".product-m__price-original").text());
        assertEquals("Từ 750,000 đ", doc.selectFirst(".product-m__price-sale").text());
        assertTrue(doc.select(".autocomplete-stock").isEmpty());
    }

    @Test void shopRendersSoldOutWithoutInventingAPrice() throws Exception {
        var doc = render("shop.html", ".product-m__price-wrap",
                new CustomerPriceSummary(null, null, 0, false, false));
        assertEquals("Tạm hết hàng", doc.body().text());
        assertTrue(doc.select(".product-m__price-sale,.product-m__price-normal").isEmpty());
    }

    @Test void detailStartsAtTheSamePriceAsSearch() throws Exception {
        var doc = render("product-detail.html", ".pd-detail__inline", new CustomerPriceSummary(
                new BigDecimal("1000000"), new BigDecimal("750000"), 25, true, true));
        assertEquals("Từ 750,000 đ", doc.selectFirst(".js-display-price").text());
        assertEquals("1,000,000 đ", doc.selectFirst(".js-display-original-price").text());
        assertTrue(doc.select(".autocomplete-stock").isEmpty());
    }

    @Test void detailDisplaysSoldOutAndKeepsARealZeroPrice() throws Exception {
        var doc = render("product-detail.html", ".pd-detail__inline",
                new CustomerPriceSummary(BigDecimal.ZERO, BigDecimal.ZERO, 0, false, false));
        assertEquals("0 đ", doc.selectFirst(".js-display-price").text());
        assertEquals("Tạm hết hàng", doc.selectFirst(".autocomplete-stock").text());
    }
}
