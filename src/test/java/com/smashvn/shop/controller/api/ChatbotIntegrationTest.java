package com.smashvn.shop.controller.api;

import com.smashvn.shop.config.ShopContactProperties;
import com.smashvn.shop.dto.chatbot.ChatFeedbackRequest;
import com.smashvn.shop.dto.chatbot.ChatMessageDto;
import com.smashvn.shop.dto.chatbot.ChatRequest;
import com.smashvn.shop.dto.chatbot.ChatbotProductSearchResponseDto;
import com.smashvn.shop.entity.ChatConversation;
import com.smashvn.shop.entity.ChatFeedback;
import com.smashvn.shop.entity.ChatMessage;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.ChatConversationRepository;
import com.smashvn.shop.repository.ChatFeedbackRepository;
import com.smashvn.shop.repository.ChatMessageRepository;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.ChatbotService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ChatbotIntegrationTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ChatConversationRepository chatConversationRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatFeedbackRepository chatFeedbackRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ShopContactProperties shopContactProperties;

    @MockitoBean(name = "geminiRestTemplate")
    private RestTemplate restTemplate;

    private MockMvc mockMvc;
    private MockHttpSession session;
    private TaiKhoan testUser;

    @BeforeEach
    void setUp() {
        chatFeedbackRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatConversationRepository.deleteAll();
        
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        session = new MockHttpSession();

        TaiKhoan existing = taiKhoanRepository.findByUsername("chatbot_user@gmail.com");
        if (existing != null) {
            taiKhoanRepository.delete(existing);
        }
        testUser = new TaiKhoan();
        testUser.setUsername("chatbot_user@gmail.com");
        testUser.setMatKhau("SecurePass123");
        testUser.setVaiTro("KH");
        testUser.setTrangThai("hoat_dong");
        testUser = taiKhoanRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        chatFeedbackRepository.deleteAll();
        chatMessageRepository.deleteAll();
        chatConversationRepository.deleteAll();
        TaiKhoan existing = taiKhoanRepository.findByUsername("chatbot_user@gmail.com");
        if (existing != null) {
            taiKhoanRepository.delete(existing);
        }
    }

    @Test
    void testPriceParser_FormatsAndRanges() {
        ChatbotProductSearchResponseDto r1 = chatbotService.searchProductsApi("500k", null, null, null, null, 5);
        assertNotNull(r1);

        ChatbotProductSearchResponseDto r2 = chatbotService.searchProductsApi("1tr5", null, null, null, null, 5);
        assertNotNull(r2);

        ChatbotProductSearchResponseDto r3 = chatbotService.searchProductsApi("1.5tr", null, null, null, null, 5);
        assertNotNull(r3);

        ChatbotProductSearchResponseDto r4 = chatbotService.searchProductsApi("1,5 triệu", null, null, null, null, 5);
        assertNotNull(r4);

        ChatbotProductSearchResponseDto r5 = chatbotService.searchProductsApi("2tr5", null, null, null, null, 5);
        assertNotNull(r5);
    }

    @Test
    void testPriceRangeSwapping() {
        // "từ 2 triệu đến 1 triệu" -> minPrice 1M, maxPrice 2M
        ChatbotProductSearchResponseDto r = chatbotService.searchProductsApi(null, null, null, new BigDecimal("2000000"), new BigDecimal("1000000"), 5);
        assertNotNull(r);
        assertTrue(r.isSuccess());
    }

    @Test
    void testUnaccentedSearch_VotYonex() {
        // "vot Yonex" matches "Vợt Yonex"
        ChatbotProductSearchResponseDto r = chatbotService.searchProductsApi("vot Yonex", null, null, null, null, 5);
        assertNotNull(r);
    }

    @Test
    void testNonExistentProduct_ReturnsEmpty() {
        ChatbotProductSearchResponseDto r = chatbotService.searchProductsApi("NonExistentProductXYZ12345", null, null, null, null, 5);
        assertNotNull(r);
        assertEquals(0, r.getTotal());
        assertEquals(0, r.getDisplayed());
        assertTrue(r.getProducts().isEmpty());
    }

    @Test
    void testLimitNormalization_Max5() {
        // limit=100 should still return displayed <= 5
        ChatbotProductSearchResponseDto r = chatbotService.searchProductsApi(null, null, null, null, null, 100);
        assertNotNull(r);
        assertTrue(r.getDisplayed() <= 5);
        assertEquals(r.getDisplayed(), r.getProducts().size());
    }

    @Test
    void testDetailUrlFormattedCorrectly() {
        ChatbotProductSearchResponseDto r = chatbotService.searchProductsApi(null, null, null, null, null, 5);
        assertNotNull(r);
        if (!r.getProducts().isEmpty()) {
            var first = r.getProducts().get(0);
            assertNotNull(first.getDetailUrl());
            assertTrue(first.getDetailUrl().startsWith("/san-pham/"));
            assertEquals("/san-pham/" + first.getId(), first.getDetailUrl());
        }
    }

    @Test
    void testOutOfScope_ReturnsHotline() throws Exception {
        String expectedHotline = shopContactProperties.getPhone();
        if (expectedHotline == null || expectedHotline.isBlank()) {
            expectedHotline = "0981472035";
        }

        mockMvc.perform(post("/api/chat/send")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"content\":\"Hôm nay thời tiết thế nào?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(expectedHotline)))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void testApiGetProductsEndpoint() throws Exception {
        mockMvc.perform(get("/api/chatbot/products")
                        .param("keyword", "astrox")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.displayed").value(org.hamcrest.Matchers.lessThanOrEqualTo(5)));
    }

    @Test
    void testFeedbackFlow() throws Exception {
        ChatConversation conversation = new ChatConversation();
        String guestSessionId = "test-session-123";
        conversation.setSessionId(guestSessionId);
        conversation = chatConversationRepository.save(conversation);

        ChatMessage botMsg = new ChatMessage();
        botMsg.setConversation(conversation);
        botMsg.setVaiTro("ASSISTANT");
        botMsg.setNoiDung("Response message");
        botMsg.setTrangThai("SUCCESS");
        botMsg = chatMessageRepository.save(botMsg);

        session.setAttribute("guestSessionId", guestSessionId);

        String feedbackPayload = String.format("{\"messageId\":%d,\"rating\":1,\"note\":\"Rất hữu ích\"}", botMsg.getId());
        mockMvc.perform(post("/api/chat/feedback")
                        .session(session)
                        .contentType("application/json")
                        .content(feedbackPayload))
                .andExpect(status().isOk());

        Optional<ChatFeedback> fbOpt = chatFeedbackRepository.findByMessageIdAndSessionId(botMsg.getId(), guestSessionId);
        assertTrue(fbOpt.isPresent());
        assertEquals(1, fbOpt.get().getDanhGia().intValue());
    }

    @Test
    void testSlangPriceParser_CuVaCanh() {
        BigDecimal p1 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePrice("1 củ rưỡi");
        assertNotNull(p1);
        assertEquals(new BigDecimal("1500000"), p1);

        BigDecimal p2 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePrice("500 cành");
        assertNotNull(p2);
        assertEquals(new BigDecimal("500000"), p2);

        BigDecimal p3 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePrice("2 củ");
        assertNotNull(p3);
        assertEquals(new BigDecimal("2000000"), p3);
    }

    @Test
    void testBadmintonPlaystyleConsultation_NotBlocked() throws Exception {
        // Legitimate badminton playstyle question must be answered (SUCCESS), NOT BLOCKED
        mockMvc.perform(post("/api/chat/send")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"content\":\"Tôi muốn mua vợt nặng đầu để đập cầu tấn công mạnh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsStringIgnoringCase("tấn công")));
    }

    @Test
    void testBeginnerConsultation_ProvidesExpertAdvice() throws Exception {
        mockMvc.perform(post("/api/chat/send")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"content\":\"tôi là người mới thì nên chơi vợt nào\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Người mới chơi")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("4U")));
    }

    @Test
    void testOrderLookup_ProvidesHelpfulStatus() throws Exception {
        mockMvc.perform(post("/api/chat/send")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"content\":\"Kiểm tra tình trạng đơn hàng của tôi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đơn hàng")));
    }

    @Test
    void testVoucherInquiry_ReturnsVoucherInfo() throws Exception {
        mockMvc.perform(post("/api/chat/send")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"content\":\"Shop có mã giảm giá nào không?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("giảm giá")));
    }

    @Test
    void testStrictOffTopicBlocking_CodingQuestions() throws Exception {
        String expectedHotline = shopContactProperties.getPhone();
        if (expectedHotline == null || expectedHotline.isBlank()) {
            expectedHotline = "0981472035";
        }

        mockMvc.perform(post("/api/chat/send")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"content\":\"Viết code Java kết nối SQL server giúp tôi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(expectedHotline)));
    }

    @Test
    void testPriceFilter_UpperLowerRangeApproximations() {
        // 1. User query with "trở xuống" (<= 1.500.000)
        var f1 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePriceFilter("nhưng tôi cần vợt tầm 1tr5 trở xuống thì có k");
        assertTrue(f1.hasPrice());
        assertNull(f1.minPrice());
        assertEquals(new BigDecimal("1500000"), f1.maxPrice());

        // 2. "2 củ quay đầu" (<= 2.000.000)
        var f2 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePriceFilter("vợt 2 củ quay đầu");
        assertTrue(f2.hasPrice());
        assertNull(f2.minPrice());
        assertEquals(new BigDecimal("2000000"), f2.maxPrice());

        // 3. "1tr5 đổ lại" (<= 1.500.000)
        var f3 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePriceFilter("vợt 1tr5 đổ lại");
        assertTrue(f3.hasPrice());
        assertNull(f3.minPrice());
        assertEquals(new BigDecimal("1500000"), f3.maxPrice());

        // 4. "từ 1tr đến 2tr" (range 1M - 2M)
        var f4 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePriceFilter("vợt từ 1tr đến 2tr");
        assertTrue(f4.hasPrice());
        assertEquals(new BigDecimal("1000000"), f4.minPrice());
        assertEquals(new BigDecimal("2000000"), f4.maxPrice());

        // 5. "1tr - 2tr" (range 1M - 2M)
        var f5 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePriceFilter("vợt 1tr - 2tr");
        assertTrue(f5.hasPrice());
        assertEquals(new BigDecimal("1000000"), f5.minPrice());
        assertEquals(new BigDecimal("2000000"), f5.maxPrice());

        // 6. "vợt trên 3 triệu" (>= 3.000.000)
        var f6 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePriceFilter("vợt trên 3 triệu");
        assertTrue(f6.hasPrice());
        assertEquals(new BigDecimal("3000000"), f6.minPrice());
        assertNull(f6.maxPrice());

        // 7. "vợt tầm 1tr5" (approximate ±20%)
        var f7 = com.smashvn.shop.service.impl.VietnamesePriceParser.parsePriceFilter("vợt tầm 1tr5");
        assertTrue(f7.hasPrice());
        assertEquals(new BigDecimal("1200000"), f7.minPrice());
        assertEquals(new BigDecimal("1800000"), f7.maxPrice());
    }

    @Test
    void testChatbot_Under1Tr5BudgetConstraint() throws Exception {
        var resultActions = mockMvc.perform(post("/api/chat/send")
                        .session(session)
                        .contentType("application/json")
                        .content("{\"content\":\"nhưng tôi cần vợt tầm 1tr5 trở xuống thì có k\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        String responseJson = resultActions.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseJson);
        com.fasterxml.jackson.databind.JsonNode products = root.path("data").path("suggestedProducts");

        // Verify that ANY returned product strictly respects the <= 1.500.000 limit
        if (products.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode prod : products) {
                double price = prod.hasNonNull("salePrice") ? prod.get("salePrice").asDouble() : prod.get("price").asDouble();
                assertTrue(price <= 1500000.0, "Sản phẩm " + prod.path("name").asText() + " có giá " + price + " vượt quá ngân sách 1tr5!");
            }
        }
    }
}
