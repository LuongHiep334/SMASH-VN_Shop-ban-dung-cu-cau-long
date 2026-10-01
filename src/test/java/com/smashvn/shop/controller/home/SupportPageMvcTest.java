package com.smashvn.shop.controller.home;

import com.smashvn.shop.config.SecurityConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Real MVC, SecurityConfig and Thymeleaf without Spring Boot, DB or external services. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SupportPageMvcTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private static final List<String> ROUTES = List.of(
            "/huong-dan-mua-hang", "/huong-dan-thanh-toan", "/chinh-sach");

    @BeforeAll void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(PageConfig.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class)).build();
    }

    @AfterAll void close() { context.close(); }

    private Document render(String route) throws Exception {
        var response = mvc.perform(get(route))
                .andExpect(status().isOk()).andExpect(view().name("support-page"))
                .andReturn().getResponse();
        assertNull(response.getRedirectedUrl());
        assertTrue(response.getContentType().contains("text/html"));
        String html = response.getContentAsString(StandardCharsets.UTF_8);
        // Optional browser fixture export; never connects to a customer database.
        if (Boolean.getBoolean("support.preview")) {
            Path output = Path.of("target/support-preview", route.substring(1) + ".html");
            Files.createDirectories(output.getParent());
            Files.writeString(output, html);
        }
        return Jsoup.parse(html);
    }

    @Test void anonymousCanReadEveryPageAndItsImagesAndNavigation() throws Exception {
        for (String route : ROUTES) {
            Document doc = render(route);
            assertEquals("vi", doc.selectFirst("html").attr("lang"));
            assertEquals(1, doc.select("main h1").size());
            assertEquals(2, doc.select(".support-nav-menu a").size());
            assertEquals("/huong-dan-mua-hang", doc.select(".support-nav-menu a").get(0).attr("href"));
            assertEquals("/huong-dan-thanh-toan", doc.select(".support-nav-menu a").get(1).attr("href"));
            assertEquals("Tin tức / Blogs", doc.selectFirst(".support-nav").previousElementSibling().text());
            assertEquals("false", doc.selectFirst(".support-nav-trigger").attr("aria-expanded"));
            assertEquals("support-nav-links", doc.selectFirst(".support-nav-trigger").attr("aria-controls"));
            assertTrue(doc.selectFirst(".support-nav-menu").hasAttr("hidden"));
            assertEquals(1, doc.select(".support-tabs [aria-current=page]").size());
            assertFalse(doc.select("header a[href='tel:0835420088']").isEmpty());
            assertFalse(doc.select("footer a[href='mailto:luonghiep334@gmail.com']").isEmpty());
            assertFalse(doc.select("footer a[href='/chinh-sach']").isEmpty());
            assertFalse(doc.text().contains("0348874711"));
            assertFalse(doc.text().contains("smash.support@gmail.com"));
            for (var link : doc.select(".support-toc a[href^='#']")) {
                assertNotNull(doc.getElementById(link.attr("href").substring(1)), link.outerHtml());
            }
            for (var asset : doc.select("img[src^='/'],link[rel=stylesheet][href^='/'],script[src^='/']")) {
                String url = asset.hasAttr("src") ? asset.attr("src") : asset.attr("href");
                assertTrue(Files.isRegularFile(Path.of("src/main/resources/static", url.split("\\?")[0].substring(1))), url);
            }
        }
    }

    @Test void relatedLinksResolveToRealSectionsAndProductReturnMessageMatchesPolicy() throws Exception {
        Document buying = render(ROUTES.get(0));
        Document payment = render(ROUTES.get(1));
        Document policies = render(ROUTES.get(2));
        Map<String, Document> pages = Map.of(ROUTES.get(0), buying, ROUTES.get(1), payment, ROUTES.get(2), policies);
        for (Document doc : pages.values()) {
            for (var link : doc.select("a[href]")) {
                String href = link.attr("href");
                String[] parts = href.split("#", 2);
                if (pages.containsKey(parts[0]) && parts.length == 2) {
                    assertNotNull(pages.get(parts[0]).getElementById(parts[1]), href);
                }
            }
        }
        String returnMessage = "Hỗ trợ gửi yêu cầu đổi/trả trong 7 ngày kể từ khi giao hàng thành công nếu sản phẩm lỗi do nhà sản xuất; shop kiểm tra và xét duyệt.";
        Document product = Jsoup.parse(Files.readString(Path.of("src/main/resources/templates/product-detail.html")));
        assertTrue(product.text().contains(returnMessage));
        assertTrue(policies.text().contains(returnMessage));
        assertFalse(product.text().contains("Hoàn tiền 100%"));
        assertFalse(product.text().contains("Bảo hành theo tiêu chuẩn"));
        assertNotNull(payment.getElementById("su-co-thanh-toan"));
        for (String file : List.of("cart.html", "checkout.html")) {
            Document source = Jsoup.parse(Files.readString(Path.of("src/main/resources/templates", file)));
            assertEquals(3, source.select(".support-inline-links a").size());
        }
    }

    @Configuration
    @EnableWebMvc
    @Import({SupportPageController.class, SecurityConfig.class})
    static class PageConfig {
        @Bean OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenClient() {
            return request -> { throw new AssertionError("Public help pages must not initiate OAuth requests"); };
        }

        @Bean ClientRegistrationRepository clients() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("preview")
                    .clientId("preview-only").clientSecret("unused")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .authorizationUri("https://example.invalid/authorize")
                    .tokenUri("https://example.invalid/token").build());
        }

        @Bean SpringResourceTemplateResolver templateResolver() {
            var resolver = new SpringResourceTemplateResolver();
            resolver.setPrefix("file:src/main/resources/templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode("HTML");
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(false);
            return resolver;
        }

        @Bean SpringTemplateEngine templateEngine(SpringResourceTemplateResolver resolver) {
            var engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            return engine;
        }

        @Bean ThymeleafViewResolver viewResolver(SpringTemplateEngine engine) {
            var resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
}
