package com.smashvn.shop.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.smashvn.shop.config.ShopContactProperties;
import com.smashvn.shop.dto.chatbot.ChatFeedbackRequest;
import com.smashvn.shop.dto.chatbot.ChatMessageDto;
import com.smashvn.shop.dto.chatbot.ChatProductResponse;
import com.smashvn.shop.dto.chatbot.ChatRequest;
import com.smashvn.shop.dto.chatbot.ChatbotProductSearchResponseDto;
import com.smashvn.shop.dto.chatbot.ProductSearchCriteria;
import com.smashvn.shop.dto.chatbot.ShopContactDto;
import com.smashvn.shop.entity.ChatConversation;
import com.smashvn.shop.entity.ChatFeedback;
import com.smashvn.shop.entity.ChatMessage;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.chatbot.ChatIntent;
import com.smashvn.shop.repository.ChatConversationRepository;
import com.smashvn.shop.repository.ChatFeedbackRepository;
import com.smashvn.shop.repository.ChatMessageRepository;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.service.ChatbotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatFeedbackRepository chatFeedbackRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final HoaDonRepository hoaDonRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final ChatbotDbHelper chatbotDbHelper;
    private final ShopContactProperties shopContactProperties;
    private final ChatbotProductCache chatbotProductCache;

    @Qualifier("geminiRestTemplate")
    private final RestTemplate geminiRestTemplate;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String baseUrl;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    @Value("${gemini.chat.max-history-messages:5}")
    private int maxHistoryMessages;

    @Value("${gemini.chat.max-user-message-length:2000}")
    private int maxUserMessageLength;

    @Value("${gemini.chat.max-product-suggestions:5}")
    private int maxProductSuggestions;

    private String getHotline() {
        String phone = shopContactProperties.getPhone();
        if (phone == null || phone.isBlank()) {
            return "0981472035";
        }
        return phone.trim();
    }

    public static String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    @Override
    @Transactional
    public ChatMessageDto sendMessage(ChatRequest request, Integer idTaiKhoan, String sessionId) {
        String rawMessage = request.getMessage();
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống.");
        }
        if (rawMessage.length() > maxUserMessageLength) {
            throw new IllegalArgumentException("Tin nhắn quá dài (tối đa " + maxUserMessageLength + " ký tự).");
        }

        ChatConversation conversation = getOrCreateConversation(request.getConversationId(), idTaiKhoan, sessionId);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setConversation(conversation);
        userMessage.setVaiTro("USER");
        userMessage.setNoiDung(rawMessage.trim());
        userMessage.setTrangThai("SUCCESS");
        chatbotDbHelper.saveMessage(userMessage);

        String hotline = getHotline();
        ChatIntent intent = classifyQuestion(rawMessage);
        log.info("Classified intent: {}", intent);

        // 1. STRICT SCOPE GUARD: Security sensitive & Out of scope
        if (intent == ChatIntent.SECURITY_SENSITIVE || intent == ChatIntent.OUT_OF_SCOPE) {
            ChatMessage blockedMsg = new ChatMessage();
            blockedMsg.setConversation(conversation);
            blockedMsg.setVaiTro("ASSISTANT");
            blockedMsg.setNoiDung("Xin lỗi, nội dung này nằm ngoài phạm vi hỗ trợ của chatbot. Tôi chỉ có thể hỗ trợ tìm kiếm sản phẩm và cung cấp thông tin có trên website của cửa hàng. Bạn có thể liên hệ " + hotline + " để được nhân viên hỗ trợ thêm.");
            blockedMsg.setTrangThai("BLOCKED");
            blockedMsg = chatbotDbHelper.saveMessage(blockedMsg);

            chatbotDbHelper.updateConversationTime(conversation.getId());
            return mapToDto(blockedMsg);
        }

        // 2. Pure medical diagnosis / surgery without badminton equipment context
        if (isPureMedicalQuery(rawMessage)) {
            ChatMessage medicalMsg = new ChatMessage();
            medicalMsg.setConversation(conversation);
            medicalMsg.setVaiTro("ASSISTANT");
            medicalMsg.setNoiDung("Tôi có thể hỗ trợ bạn tìm kiếm sản phẩm theo tên hoặc khoảng giá. Để được tư vấn chuyên sâu về điều trị chấn thương y tế, bạn vui lòng liên hệ số điện thoại " + hotline + " hoặc bác sĩ chuyên khoa.");
            medicalMsg.setTrangThai("SUCCESS");
            medicalMsg = chatbotDbHelper.saveMessage(medicalMsg);

            chatbotDbHelper.updateConversationTime(conversation.getId());
            ChatMessageDto dto = mapToDto(medicalMsg);
            dto.setRequiresHumanSupport(true);
            dto.setContact(buildContactDto());
            return dto;
        }

        // 3. Fast-path: Greetings
        if (intent == ChatIntent.GREETING) {
            ChatMessage greetingMsg = new ChatMessage();
            greetingMsg.setConversation(conversation);
            greetingMsg.setVaiTro("ASSISTANT");
            greetingMsg.setNoiDung("Xin chào! 👋 Mình là trợ lý ảo của SmashVN Shop.\n"
                    + "Tôi hỗ trợ tư vấn và giải đáp các thắc mắc đối với các sản phẩm cầu lông tại cửa hàng:\n"
                    + "🏸 Tư vấn chọn vợt theo lối chơi (tấn công, phòng thủ, toàn diện)\n"
                    + "🔰 Gợi ý sản phẩm phù hợp với trình độ và mức giá\n"
                    + "👟 Giải đáp thông số, giá bán và tình trạng còn hàng của sản phẩm\n"
                    + "Bạn cần tư vấn sản phẩm nào hôm nay?");
            greetingMsg.setTrangThai("SUCCESS");
            greetingMsg = chatbotDbHelper.saveMessage(greetingMsg);
            chatbotDbHelper.updateConversationTime(conversation.getId());

            ChatMessageDto dto = mapToDto(greetingMsg);
            dto.setSuggestedProducts(getFeaturedProducts(3));
            return dto;
        }

        // 4. Fast-path: Store Information & Policies (Address, Hotline, Warranty, Shipping)
        if (intent == ChatIntent.STORE_INFORMATION) {
            ChatMessage storeMsg = new ChatMessage();
            storeMsg.setConversation(conversation);
            storeMsg.setVaiTro("ASSISTANT");
            storeMsg.setNoiDung(buildStoreInfoAnswer(rawMessage, hotline));
            storeMsg.setTrangThai("SUCCESS");
            storeMsg = chatbotDbHelper.saveMessage(storeMsg);
            chatbotDbHelper.updateConversationTime(conversation.getId());

            ChatMessageDto dto = mapToDto(storeMsg);
            dto.setContact(buildContactDto());
            return dto;
        }

        // 5. Intelligent Order Tracking
        if (intent == ChatIntent.ORDER_LOOKUP) {
            String orderReply = handleOrderLookup(rawMessage, idTaiKhoan);
            ChatMessage orderMsg = new ChatMessage();
            orderMsg.setConversation(conversation);
            orderMsg.setVaiTro("ASSISTANT");
            orderMsg.setNoiDung(orderReply);
            orderMsg.setTrangThai("SUCCESS");
            orderMsg = chatbotDbHelper.saveMessage(orderMsg);
            chatbotDbHelper.updateConversationTime(conversation.getId());

            ChatMessageDto dto = mapToDto(orderMsg);
            dto.setSuggestedProducts(Collections.emptyList());
            return dto;
        }

        // 6. Intelligent Voucher / Promotion Discovery
        if (intent == ChatIntent.VOUCHER_LOOKUP) {
            String voucherReply = handleVoucherLookup();
            ChatMessage voucherMsg = new ChatMessage();
            voucherMsg.setConversation(conversation);
            voucherMsg.setVaiTro("ASSISTANT");
            voucherMsg.setNoiDung(voucherReply);
            voucherMsg.setTrangThai("SUCCESS");
            voucherMsg = chatbotDbHelper.saveMessage(voucherMsg);
            chatbotDbHelper.updateConversationTime(conversation.getId());

            ChatMessageDto dto = mapToDto(voucherMsg);
            dto.setSuggestedProducts(getFeaturedProducts(3));
            return dto;
        }

        // 7. Check vague / incomplete query
        if (isIncompleteQuery(rawMessage)) {
            ChatMessage vagueMsg = new ChatMessage();
            vagueMsg.setConversation(conversation);
            vagueMsg.setVaiTro("ASSISTANT");
            vagueMsg.setNoiDung("Bạn muốn tìm sản phẩm theo tên hay theo khoảng giá? Ví dụ: “Tìm vợt Yonex dưới 2 triệu” hoặc “Tư vấn vợt công thủ toàn diện tầm 1 triệu rưỡi”.");
            vagueMsg.setTrangThai("SUCCESS");
            vagueMsg = chatbotDbHelper.saveMessage(vagueMsg);
            chatbotDbHelper.updateConversationTime(conversation.getId());

            ChatMessageDto dto = mapToDto(vagueMsg);
            dto.setSuggestedProducts(getFeaturedProducts(3));
            return dto;
        }

        // 8. Query Cached Products
        ProductSearchCriteria criteria = extractSearchCriteria(rawMessage);
        ChatbotProductSearchResponseDto searchResult = executeProductSearch(criteria, 5);
        List<ChatProductResponse> suggestionDtos = searchResult.getProducts();

        boolean relaxedBrand = false;
        boolean outOfBudget = false;

        // If no products match exact criteria, try fallback while respecting user constraints
        if (suggestionDtos.isEmpty()) {
            // Level 1: If keyword was specified, drop keyword but KEEP brand, category, and price
            if (criteria.getKeyword() != null) {
                ProductSearchCriteria relaxKeywords = new ProductSearchCriteria();
                relaxKeywords.setCategoryName(criteria.getCategoryName());
                relaxKeywords.setBrandName(criteria.getBrandName());
                relaxKeywords.setMinPrice(criteria.getMinPrice());
                relaxKeywords.setMaxPrice(criteria.getMaxPrice());
                suggestionDtos = chatbotProductCache.search(relaxKeywords, 5);
            }

            // Level 2: If still empty and brand was specified, drop brand but KEEP price & category
            if (suggestionDtos.isEmpty() && criteria.getBrandName() != null && (criteria.getMinPrice() != null || criteria.getMaxPrice() != null)) {
                ProductSearchCriteria relaxBrand = new ProductSearchCriteria();
                relaxBrand.setCategoryName(criteria.getCategoryName());
                relaxBrand.setMinPrice(criteria.getMinPrice());
                relaxBrand.setMaxPrice(criteria.getMaxPrice());
                suggestionDtos = chatbotProductCache.search(relaxBrand, 5);
                if (!suggestionDtos.isEmpty()) {
                    relaxedBrand = true;
                }
            }

            // Level 3: If still empty, find available products in category but mark as outOfBudget
            if (suggestionDtos.isEmpty()) {
                String catName = criteria.getCategoryName() != null ? criteria.getCategoryName() : "Vợt";
                ProductSearchCriteria broader = new ProductSearchCriteria();
                broader.setCategoryName(catName);
                if (criteria.getMaxPrice() != null) {
                    broader.setMaxPrice(criteria.getMaxPrice());
                    suggestionDtos = chatbotProductCache.search(broader, 3);
                }
                if (suggestionDtos.isEmpty()) {
                    broader.setMaxPrice(null);
                    suggestionDtos = chatbotProductCache.search(broader, 3);
                    if (criteria.getMinPrice() != null || criteria.getMaxPrice() != null) {
                        outOfBudget = true;
                    }
                }
            }
        }

        // 9. Retrieve Chat History
        List<ChatMessage> dbMessages = chatMessageRepository.findAllByConversationId(conversation.getId());
        dbMessages.sort((m1, m2) -> {
            int dateComp = m2.getNgayTao().compareTo(m1.getNgayTao());
            if (dateComp != 0) return dateComp;
            return m2.getId().compareTo(m1.getId());
        });
        List<ChatMessage> history = dbMessages.stream()
                .limit(Math.min(maxHistoryMessages, 5))
                .collect(Collectors.toList());
        Collections.reverse(history);

        // 10. Call Gemini API or use Smart Domain Fallback
        String aiResponse = null;
        String errorCode = null;
        String errorMessage = null;
        long startTime = System.currentTimeMillis();

        boolean hasApiKey = apiKey != null && !apiKey.isBlank();
        if (hasApiKey) {
            try {
                aiResponse = callGeminiApi(history, suggestionDtos, intent);
            } catch (HttpStatusCodeException ex) {
                log.error("Gemini API HTTP Error status: {}", ex.getStatusCode());
                errorCode = String.valueOf(ex.getStatusCode().value());
                errorMessage = ex.getResponseBodyAsString();
            } catch (ResourceAccessException ex) {
                log.error("Gemini API Connect Timeout/Network Error: {}", ex.getMessage());
                errorCode = "TIMEOUT_OR_NETWORK";
                errorMessage = ex.getMessage();
            } catch (Exception ex) {
                log.error("Gemini API Unknown Error: {}", ex.getMessage());
                errorCode = "UNKNOWN_ERROR";
                errorMessage = ex.getMessage();
            }
        } else {
            log.info("Gemini API key not configured, using Smart Badminton Domain Fallback Engine.");
        }

        // Smart Badminton Domain Fallback if AI call didn't succeed
        if (aiResponse == null) {
            aiResponse = buildBadmintonExpertFallback(rawMessage, suggestionDtos, criteria, relaxedBrand, outOfBudget);
        }

        long duration = System.currentTimeMillis() - startTime;

        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setConversation(conversation);
        assistantMessage.setVaiTro("ASSISTANT");
        assistantMessage.setTenModel(hasApiKey ? model : "SmashVN-Badminton-Expert-Engine");
        assistantMessage.setThoiGianXuLyMs(duration);

        String validatedResponse = validateGeminiResponse(aiResponse, suggestionDtos);
        assistantMessage.setNoiDung(validatedResponse);
        assistantMessage.setTrangThai("SUCCESS");

        if (errorCode != null) {
            assistantMessage.setMaLoi(errorCode.length() > 50 ? errorCode.substring(0, 50) : errorCode);
            if (errorMessage != null) {
                assistantMessage.setNoiDungLoi(errorMessage.length() > 250 ? errorMessage.substring(0, 250) : errorMessage);
            }
        }

        assistantMessage = chatbotDbHelper.saveMessage(assistantMessage);
        chatbotDbHelper.updateConversationTime(conversation.getId());

        ChatMessageDto dto = mapToDto(assistantMessage);
        dto.setSuggestedProducts(suggestionDtos);
        return dto;
    }

    private List<ChatProductResponse> getFeaturedProducts(int limit) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        return chatbotProductCache.search(criteria, limit);
    }

    private String buildStoreInfoAnswer(String rawMessage, String hotline) {
        String msgLower = removeAccents(rawMessage.toLowerCase());
        String address = shopContactProperties.getAddress();
        if (address == null || address.isBlank()) {
            address = "Số 123 Đường Cầu Lông, Quận Cầu Giấy, Hà Nội";
        }
        String email = shopContactProperties.getEmail();
        if (email == null || email.isBlank()) {
            email = "cskh@smashvn.com";
        }

        if (msgLower.contains("doi tra") || msgLower.contains("bao hanh")) {
            return "🛡️ **Chính sách Đổi trả & Bảo hành tại SmashVN:**\n"
                    + "- Hỗ trợ 1 đổi 1 trong vòng **7 ngày** nếu sản phẩm có lỗi từ nhà sản xuất.\n"
                    + "- Sản phẩm vợt bảo hành chính hãng từ 3 - 6 tháng theo quy định từng hãng.\n"
                    + "- Để hỗ trợ bảo hành nhanh nhất, bạn vui lòng liên hệ hotline: **" + hotline + "**.";
        }

        if (msgLower.contains("phi ship") || msgLower.contains("van chuyen") || msgLower.contains("giao hang")) {
            return "🚚 **Chính sách Vận chuyển SmashVN:**\n"
                    + "- Giao hàng toàn quốc qua đối tác vận chuyển Giao Hàng Nhanh (GHN).\n"
                    + "- Thời gian giao nội thành: 1 - 2 ngày; liên tỉnh: 2 - 4 ngày.\n"
                    + "- Phí vận chuyển được tính tự động tại bước thanh toán theo địa chỉ nhận hàng.";
        }

        return "🏪 **Thông tin Cửa hàng SmashVN:**\n"
                + "- **Địa chỉ Showroom:** " + address + "\n"
                + "- **Hotline:** " + hotline + " (Hỗ trợ từ 8h00 - 21h30)\n"
                + "- **Email:** " + email + "\n"
                + "Bạn có thể ghé trực tiếp cửa hàng để trải nghiệm vợt và được tư vấn căng cước chuẩn nhé!";
    }

    private String handleOrderLookup(String rawMessage, Integer idTaiKhoan) {
        Pattern orderPattern = Pattern.compile("(?i)(?:HDSVN|DHSVN)?\\d{8}-\\d+|\\b\\d{1,7}\\b");
        Matcher matcher = orderPattern.matcher(rawMessage.replaceAll("\\s+", ""));
        String matchedCode = null;
        if (matcher.find()) {
            matchedCode = matcher.group();
        }

        if (matchedCode != null && !matchedCode.isBlank()) {
            Optional<HoaDon> hoaDonOpt = hoaDonRepository.findByMaDonHang(matchedCode);
            if (hoaDonOpt.isPresent()) {
                return formatHoaDonStatus(hoaDonOpt.get());
            }
        }

        // If no code or code not found, check logged in user's recent orders
        if (idTaiKhoan != null) {
            KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
            if (kh != null) {
                List<HoaDon> recentOrders = hoaDonRepository.findByKhachHang_IdOrderByIdDesc(kh.getId());
                if (!recentOrders.isEmpty()) {
                    HoaDon latest = recentOrders.get(0);
                    return "📦 **Đơn hàng gần nhất của bạn:**\n" + formatHoaDonStatus(latest);
                }
            }
        }

        return "🔍 Để tra cứu chính xác trạng thái đơn hàng, bạn vui lòng cung cấp **Mã đơn hàng** (Ví dụ: `DHSVN20260907-140`) hoặc liên hệ hotline **" + getHotline() + "** để được hỗ trợ kiểm tra ngay nhé!";
    }

    private String formatHoaDonStatus(HoaDon hd) {
        String statusVi = switch (hd.getTrangThaiDonHang()) {
            case "CHO_XAC_NHAN" -> "⏳ Chờ xác nhận";
            case "DA_XAC_NHAN" -> "📦 Đã xác nhận / Đang đóng gói";
            case "DANG_GIAO" -> "🚚 Đang giao hàng";
            case "DA_GIAO" -> "✅ Đã giao hàng";
            case "HOAN_THANH" -> "🎉 Hoàn thành";
            case "DA_HUY" -> "❌ Đã hủy";
            case "TRA_HANG" -> "🔄 Đổi / Trả hàng";
            default -> hd.getTrangThaiDonHang();
        };

        String paymentVi = "DA_THANH_TOAN".equalsIgnoreCase(hd.getTrangThaiThanhToan())
                ? "Đã thanh toán"
                : "Chờ thanh toán (COD / Chuyển khoản)";

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = hd.getNgayTao() != null ? hd.getNgayTao().format(dtf) : "";

        String ghnInfo = (hd.getGhnOrderCode() != null && !hd.getGhnOrderCode().isBlank())
                ? "\n- Mã vận đơn GHN: **" + hd.getGhnOrderCode() + "**"
                : "";

        return String.format(
                "Mã đơn: **%s**\n"
                        + "- Ngày đặt: %s\n"
                        + "- Người nhận: **%s** (%s)\n"
                        + "- Địa chỉ: %s\n"
                        + "- Tổng tiền: **%s đ**\n"
                        + "- Trạng thái: **%s**\n"
                        + "- Thanh toán: %s%s",
                hd.getMaDonHang(),
                dateStr,
                hd.getTenNguoiNhan() != null ? hd.getTenNguoiNhan() : "Khách hàng",
                hd.getSdtNhan(),
                hd.getDiaChiNhan(),
                hd.getTongTien() != null ? String.format("%,d", hd.getTongTien().longValue()) : "0",
                statusVi,
                paymentVi,
                ghnInfo
        );
    }

    private String handleVoucherLookup() {
        List<PhieuGiamGia> vouchers = phieuGiamGiaRepository.findActiveVouchers(LocalDateTime.now());
        if (vouchers == null || vouchers.isEmpty()) {
            return "🎟️ Hiện tại cửa hàng chưa có mã giảm giá mới. Bạn có thể theo dõi các chương trình ưu đãi trực tiếp tại trang sản phẩm giảm giá nhé!";
        }

        StringBuilder sb = new StringBuilder("🎟️ **Mã giảm giá đang áp dụng tại SmashVN:**\n\n");
        int count = 0;
        for (PhieuGiamGia v : vouchers) {
            if (count >= 5) break;
            String discountStr = "%".equals(v.getDonVi())
                    ? v.getGiaTri().toPlainString() + "%"
                    : String.format("%,d đ", v.getGiaTri().longValue());

            String minOrder = (v.getGiaTriDonHangToiThieu() != null && v.getGiaTriDonHangToiThieu().compareTo(BigDecimal.ZERO) > 0)
                    ? "cho đơn từ " + String.format("%,d đ", v.getGiaTriDonHangToiThieu().longValue())
                    : "mọi đơn hàng";

            sb.append("• Mã: `").append(v.getMaPhieu()).append("` - Giảm **").append(discountStr).append("** (").append(minOrder).append(")\n");
            count++;
        }
        sb.append("\n👉 Nhập mã tại bước **Thanh toán** để được giảm giá ngay!");
        return sb.toString();
    }

    private String buildBadmintonExpertFallback(String rawMessage, List<ChatProductResponse> products, ProductSearchCriteria criteria, boolean relaxedBrand, boolean outOfBudget) {
        String msgLower = removeAccents(rawMessage.toLowerCase());

        boolean isBeginner = msgLower.contains("moi choi") || msgLower.contains("nguoi moi")
                || msgLower.contains("moi tap") || msgLower.contains("moi bat dau")
                || msgLower.contains("nhap mon") || msgLower.contains("chua biet choi")
                || msgLower.contains("co tay yeu") || msgLower.contains("tay yeu")
                || msgLower.contains("luc tay yeu") || msgLower.contains("chua co luc")
                || msgLower.contains("tro luc") || msgLower.contains("de choi")
                || msgLower.contains("de danh") || msgLower.contains("de thuan");

        if (isBeginner) {
            String priceContext = "";
            if (criteria.getMaxPrice() != null && criteria.getMinPrice() != null) {
                priceContext = " trong tầm giá từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** đến **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMaxPrice() != null) {
                priceContext = " trong tầm giá dưới **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMinPrice() != null) {
                priceContext = " trong tầm giá từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** trở lên";
            }
            return "🏸 **Tư vấn dành cho Người mới chơi / Lực tay vừa" + (priceContext.isEmpty() ? "" : " (" + priceContext.trim() + ")") + ":**\n"
                    + "- **Trọng lượng:** Bạn nên ưu tiên chọn vợt **4U (80-84g)** hoặc **5U** để nhẹ tay, dễ điều khiển và tránh chấn thương khớp vai, cổ tay.\n"
                    + "- **Thân vợt:** Chọn thân vợt **dẻo (Flexible)** hoặc trung bình để có độ trợ lực tự nhiên tốt khi phông cầu và đập cầu.\n"
                    + "- **Mức căng cước:** Khuyến nghị căng ở mức an toàn từ **9.5 kg - 10.5 kg** (21-23 lbs) để tối ưu độ nảy trợ lực và không bị đau tay.\n\n"
                    + "SmashVN gợi ý các cây vợt dễ chơi, trợ lực tốt nhất" + priceContext + " cho bạn bên dưới:";
        }

        boolean isAttacking = msgLower.contains("tan cong") || msgLower.contains("dap cau")
                || msgLower.contains("nang dau") || msgLower.contains("smash")
                || msgLower.contains("chuyen cong") || msgLower.contains("thich dap")
                || msgLower.contains("dap manh") || msgLower.contains("uy luc")
                || msgLower.contains("head heavy");

        if (isAttacking) {
            String priceContext = "";
            if (criteria.getMaxPrice() != null && criteria.getMinPrice() != null) {
                priceContext = " trong tầm giá từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** đến **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMaxPrice() != null) {
                priceContext = " trong tầm giá dưới **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMinPrice() != null) {
                priceContext = " trong tầm giá từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** trở lên";
            }
            return "🏸 **Tư vấn lối chơi Tấn công (Smash):**\n"
                    + "- **Đặc điểm vợt:** Nên chọn vợt **nặng đầu (Head-heavy, điểm cân bằng > 295mm)** giúp tăng quán tính cho cú đập cầu cắm sân và uy lực.\n"
                    + "- **Độ cứng thân:** Thân vợt từ **trung bình đến cứng** để truyền lực chính xác và đầm tay.\n"
                    + "- **Trọng lượng:** 4U dành cho cổ tay trung bình, hoặc 3U nếu bạn có lực cổ tay khỏe và thể lực tốt.\n\n"
                    + "SmashVN gợi ý các dòng vợt tấn công nổi bật" + priceContext + " bên dưới:";
        }

        boolean isDefenseOrSpeed = msgLower.contains("phong thu") || msgLower.contains("phan tat")
                || msgLower.contains("nhe dau") || msgLower.contains("toc do")
                || msgLower.contains("chuyen thu") || msgLower.contains("danh nhanh")
                || msgLower.contains("vung nhanh") || msgLower.contains("linh hoat")
                || msgLower.contains("bat luoi") || msgLower.contains("gai cau")
                || msgLower.contains("head light");

        if (isDefenseOrSpeed) {
            String priceContext = "";
            if (criteria.getMaxPrice() != null && criteria.getMinPrice() != null) {
                priceContext = " trong tầm giá từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** đến **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMaxPrice() != null) {
                priceContext = " trong tầm giá dưới **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMinPrice() != null) {
                priceContext = " trong tầm giá từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** trở lên";
            }
            return "🏸 **Tư vấn lối chơi Phòng thủ / Phản tạt / Tốc độ:**\n"
                    + "- **Đặc điểm vợt:** Ưu tiên dòng vợt **nhẹ đầu (Head-light)** hoặc cân bằng, khung vát khí động học xé gió nhanh.\n"
                    + "- **Ưu thế:** Giúp xoay trở cực nhanh trong các pha đôi công sát lưới, đè lưới và thủ smash hiệu quả mà không bị mỏi tay.\n"
                    + "- **Trọng lượng:** 4U hoặc 5U siêu nhẹ và linh hoạt.\n\n"
                    + "Dưới đây là các cây vợt tốc độ & phản tạt tốt nhất" + priceContext + " tại SmashVN:";
        }

        boolean isAllAround = msgLower.contains("cong thu") || msgLower.contains("toan dien")
                || msgLower.contains("can bang") || msgLower.contains("danh doi")
                || msgLower.contains("doi nam nu") || msgLower.contains("all around");

        if (isAllAround) {
            String priceContext = "";
            if (criteria.getMaxPrice() != null && criteria.getMinPrice() != null) {
                priceContext = " trong tầm giá từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** đến **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMaxPrice() != null) {
                priceContext = " trong tầm giá dưới **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMinPrice() != null) {
                priceContext = " trong tầm giá từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** trở lên";
            }
            return "🏸 **Tư vấn lối chơi Công thủ toàn diện (All-around):**\n"
                    + "- **Đặc điểm:** Điểm cân bằng lý tưởng khoảng **290mm - 295mm**, vừa có độ đầm để đập cầu khi có cơ hội, vừa đủ linh hoạt để thủ cầu và phản tạt.\n"
                    + "- **Khuyến nghị:** Đây là lối chơi cân bằng và phổ biến nhất trong đánh đôi phong trào. Nên chọn vợt 4U thân trung bình để dễ kiểm soát.\n\n"
                    + "Mời bạn tham khảo các mẫu vợt công thủ toàn diện được ưa chuộng nhất" + priceContext + " bên dưới:";
        }

        boolean isGeneralConsultation = msgLower.contains("nen mua vot nao") || msgLower.contains("nen choi vot nao")
                || msgLower.contains("chon vot nao") || msgLower.contains("tu van vot")
                || msgLower.contains("nen chon vot") || msgLower.contains("cay vot nao tot")
                || msgLower.contains("vot nao hay") || msgLower.contains("vot nao de danh");

        if (isGeneralConsultation) {
            return "🏸 **Tư vấn chọn vợt cầu lông phù hợp tại SmashVN:**\n"
                    + "Để chọn được cây vợt ưng ý nhất, bạn có thể cân nhắc theo 3 tiêu chí:\n"
                    + "- **Trình độ / Lực tay:** Người mới nên chọn vợt **4U thân dẻo trợ lực**; người chơi có lực tay tốt có thể chọn **3U hoặc thân cứng**.\n"
                    + "- **Lối chơi:** Thích đập cầu chọn vợt **Nặng đầu (Head-heavy)**; thích phản tạt, thủ nhanh chọn vợt **Nhẹ đầu/Cân bằng**; chơi linh hoạt chọn vợt **Công thủ toàn diện**.\n"
                    + "- **Ngân sách:** Bạn có thể gửi cho SmashVN mức giá mong muốn (ví dụ: *dưới 1 triệu*, *tầm 1tr5 đến 2 triệu*) để shop gợi ý chính xác nhất nhé!\n\n"
                    + "Dưới đây là các mẫu vợt nổi bật, dễ chơi nhất tại SmashVN để bạn tham khảo:";
        }

        if (outOfBudget) {
            String budgetStr = "";
            if (criteria.getMaxPrice() != null) {
                budgetStr = "dưới **" + String.format("%,d đ", criteria.getMaxPrice().longValue()) + "**";
            } else if (criteria.getMinPrice() != null) {
                budgetStr = "từ **" + String.format("%,d đ", criteria.getMinPrice().longValue()) + "** trở lên";
            }
            return "Hiện tại SmashVN chưa có sản phẩm nào trong tầm giá " + budgetStr + " (hoặc các mẫu này đã tạm hết hàng). Bạn có thể tham khảo một số mẫu sản phẩm có mức giá tốt nhất hiện có của shop bên dưới hoặc liên hệ Hotline để được tư vấn thêm nhé:";
        }

        if (relaxedBrand && criteria.getBrandName() != null) {
            return "Hiện tại shop chưa có mẫu vợt thương hiệu **" + criteria.getBrandName() + "** trong tầm giá bạn yêu cầu. Tuy nhiên, SmashVN xin gợi ý một số mẫu vợt chính hãng chất lượng khác cùng phân khúc giá bên dưới để bạn tham khảo nhé:";
        }

        if (criteria.getMaxPrice() != null && criteria.getMinPrice() != null) {
            return String.format("SmashVN tìm thấy các sản phẩm trong tầm giá từ **%,d đ** đến **%,d đ** phù hợp với yêu cầu của bạn bên dưới. Mời bạn tham khảo:",
                    criteria.getMinPrice().longValue(), criteria.getMaxPrice().longValue());
        }
        if (criteria.getMaxPrice() != null) {
            return String.format("SmashVN tìm thấy các sản phẩm trong tầm giá dưới **%,d đ** phù hợp với yêu cầu của bạn bên dưới. Mời bạn tham khảo:",
                    criteria.getMaxPrice().longValue());
        }
        if (criteria.getMinPrice() != null) {
            return String.format("SmashVN tìm thấy các sản phẩm trong tầm giá từ **%,d đ** trở lên phù hợp với yêu cầu của bạn bên dưới. Mời bạn tham khảo:",
                    criteria.getMinPrice().longValue());
        }

        if (!products.isEmpty()) {
            return "SmashVN tìm thấy các sản phẩm phù hợp với yêu cầu của bạn bên dưới. Mời bạn tham khảo chi tiết:";
        }

        return "Tôi chưa tìm thấy sản phẩm phù hợp với yêu cầu của bạn. Bạn có thể thử nhập tên sản phẩm khác hoặc thay đổi khoảng giá.";
    }

    @Override
    public ChatbotProductSearchResponseDto searchProductsApi(String keyword, String category, String brand, BigDecimal minPrice, BigDecimal maxPrice, Integer limit) {
        int safeLimit = (limit == null) ? 5 : Math.min(Math.max(limit, 1), 5);

        // Swap minPrice and maxPrice if minPrice > maxPrice
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }

        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setKeyword(keyword != null ? keyword.trim() : null);
        criteria.setCategoryName(category != null ? category.trim() : null);
        criteria.setBrandName(brand != null ? brand.trim() : null);
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);

        return executeProductSearch(criteria, safeLimit);
    }

    private ChatbotProductSearchResponseDto executeProductSearch(ProductSearchCriteria criteria, int limit) {
        long total = chatbotProductCache.countMatched(criteria);
        List<ChatProductResponse> productDtos = chatbotProductCache.search(criteria, limit);

        return ChatbotProductSearchResponseDto.builder()
                .success(true)
                .total(total)
                .displayed(productDtos.size())
                .products(productDtos)
                .build();
    }

    private ProductSearchCriteria extractSearchCriteria(String userPrompt) {
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        String promptLower = userPrompt.toLowerCase();

        // 1. Dynamic Brands from Cache
        List<String> activeBrands = chatbotProductCache.getCachedBrands();
        for (String b : activeBrands) {
            if (promptLower.contains(b.toLowerCase())) {
                criteria.setBrandName(b);
                break;
            }
        }
        if (criteria.getBrandName() == null) {
            if (promptLower.contains("lining") || promptLower.contains("li-ning") || promptLower.contains("li ning")) {
                criteria.setBrandName("Lining");
            } else if (promptLower.contains("yonex")) {
                criteria.setBrandName("Yonex");
            } else if (promptLower.contains("victor")) {
                criteria.setBrandName("Victor");
            } else if (promptLower.contains("mizuno")) {
                criteria.setBrandName("Mizuno");
            } else if (promptLower.contains("kumpoo")) {
                criteria.setBrandName("Kumpoo");
            }
        }

        // 2. Dynamic Categories from Cache
        List<String> activeCats = chatbotProductCache.getCachedCategories();
        for (String c : activeCats) {
            if (promptLower.contains(c.toLowerCase())) {
                criteria.setCategoryName(c);
                break;
            }
        }
        if (criteria.getCategoryName() == null) {
            if (promptLower.contains("vợt") || promptLower.contains("vot") || promptLower.contains("cây") || promptLower.contains("cay")) {
                criteria.setCategoryName("Vợt");
            } else if (promptLower.contains("giày") || promptLower.contains("giay") || promptLower.contains("đôi") || promptLower.contains("doi")) {
                criteria.setCategoryName("Giày");
            } else if (promptLower.contains("áo") || promptLower.contains("quần") || promptLower.contains("ao") || promptLower.contains("quan") || promptLower.contains("trang phục")) {
                criteria.setCategoryName("Trang phục");
            } else if (promptLower.contains("cầu") || promptLower.contains("cau") || promptLower.contains("ống cầu") || promptLower.contains("hộp cầu")) {
                criteria.setCategoryName("Quả cầu lông");
            } else if (promptLower.contains("phụ kiện") || promptLower.contains("phu kien") || promptLower.contains("cước") || promptLower.contains("balo") || promptLower.contains("túi")) {
                criteria.setCategoryName("Phụ kiện");
            }
        }

        // 3. Parse Price & Direction with Slang and Bound Support
        VietnamesePriceParser.PriceFilter priceFilter = VietnamesePriceParser.parsePriceFilter(userPrompt);
        if (priceFilter.hasPrice()) {
            criteria.setMinPrice(priceFilter.minPrice());
            criteria.setMaxPrice(priceFilter.maxPrice());
        }

        if (criteria.getMinPrice() != null && criteria.getMaxPrice() != null
                && criteria.getMinPrice().compareTo(criteria.getMaxPrice()) > 0) {
            BigDecimal temp = criteria.getMinPrice();
            criteria.setMinPrice(criteria.getMaxPrice());
            criteria.setMaxPrice(temp);
        }

        // 4. Extract specific keywords (e.g. model name like "astrox", "bladex", "axforce", "88d", "100zz")
        List<String> keywords = extractProductKeywords(promptLower);
        if (!keywords.isEmpty()) {
            criteria.setKeyword(keywords.get(0));
            if (keywords.size() > 1) criteria.setKeyword2(keywords.get(1));
            if (keywords.size() > 2) criteria.setKeyword3(keywords.get(2));
        }

        return criteria;
    }

    private List<String> extractProductKeywords(String promptLower) {
        String cleaned = VietnamesePriceParser.stripPriceTokens(promptLower);
        String unaccented = removeAccents(cleaned);
        Set<String> ignored = new java.util.HashSet<>(java.util.Arrays.asList(
                // Pronouns & polite words
                "toi", "minh", "em", "anh", "chi", "ban", "ad", "shop", "admin", "bac", "chu", "ai", "nguoi",
                // Question words & particles
                "nhung", "ma", "thi", "co", "khong", "k", "ko", "khg", "hong", "dc", "duoc", "ha", "a", "oi", "nhe", "nha", "nao", "gi", "sao", "the", "vay", "di", "nhi", "chi", "voi", "va", "hoac", "hay", "la",
                // Modal & intention verbs
                "muon", "can", "tim", "mua", "tu", "van", "hoi", "xem", "cho", "xin", "giup", "ho", "dum", "biet", "thay", "tinh",
                // General nouns & units
                "san", "pham", "hang", "loai", "chiec", "cay", "doi", "bo", "mon", "mau", "cai", "con", "dong", "tien", "gia", "muc",
                // Categories
                "vot", "giay", "ao", "quan", "cau", "long", "phu", "kien", "balo", "tui", "cuoc", "ong", "hop",
                // Brands
                "yonex", "lining", "li-ning", "victor", "mizuno", "kumpoo", "felet",
                // Price range & direction words
                "tam", "khoang", "quanh", "chung", "duoi", "tren", "tro", "xuong", "len", "do", "lai", "ve", "quay", "dau", "hat", "thap", "cao", "hon", "it", "nhat", "nho", "be", "lon", "toi", "da", "thieu", "max", "min", "tu", "den", "toi", "trieu", "nghin", "ngan", "cu", "canh", "chai", "dong", "vnd", "ruoi",
                // Playstyle terms
                "tan", "cong", "phong", "thu", "toan", "dien", "phan", "tat", "dap", "nang", "dau", "nhe", "tro", "luc", "toc", "do", "dieu", "can", "bang", "cang", "cuoc", "moi", "choi", "tay", "yeu",
                // Adjectives
                "tot", "re", "dep", "ben", "ngon", "xin", "chinh", "auth", "real", "fake", "rep", "chat", "luong", "3u", "4u", "5u", "do", "xanh", "den", "trang", "vang", "hong", "cam"
        ));

        return java.util.Arrays.stream(unaccented.replaceAll("[^a-zA-Z0-9-]+", " ").trim().split("\\s+"))
                .filter(token -> token.length() > 1 && !ignored.contains(token))
                .filter(token -> !token.matches("(?i)\\d+(?:tr|trieu|k|cu|canh|nghin|ngan|vnd)\\d*"))
                .filter(token -> !token.matches("\\d{5,}"))
                .distinct()
                .limit(3)
                .toList();
    }

    private boolean isIncompleteQuery(String message) {
        String msgLower = removeAccents(message.toLowerCase().trim());
        Set<String> vagueExact = Set.of(
                "toi muon mua vot", "muon mua vot", "tim san pham gia re",
                "tu van cho toi mot cay vot", "tu van vot", "tu van giay", "mua vot", "muon mua giay"
        );
        return vagueExact.contains(msgLower);
    }

    private ChatIntent classifyQuestion(String message) {
        String msgLower = removeAccents(message.toLowerCase());

        // 1. Security Sensitive
        if (msgLower.contains("api key") || msgLower.contains("system prompt")
                || msgLower.contains("co so du lieu") || msgLower.contains("database schema")
                || msgLower.contains("source code") || msgLower.contains("secret key")
                || msgLower.contains("mat khau admin") || msgLower.contains("du lieu he thong")
                || msgLower.contains("hack") || msgLower.contains("drop table")) {
            return ChatIntent.SECURITY_SENSITIVE;
        }

        // 2. Out of Scope (Strict guardrail for off-topic queries)
        if (msgLower.contains("lap trinh") || msgLower.contains("java code") || msgLower.contains("code java")
                || msgLower.contains("viet code") || msgLower.contains("database") || msgLower.contains("sql server")
                || msgLower.contains("python") || msgLower.contains("html") || msgLower.contains("javascript")
                || msgLower.contains("chinh tri") || msgLower.contains("thoi tiet") || msgLower.contains("mua khong")
                || msgLower.contains("tin tuc") || msgLower.contains("giai tri") || msgLower.contains("hat")
                || msgLower.contains("singing") || msgLower.contains("am nhac") || msgLower.contains("bai hat")
                || msgLower.contains("cong thuc nau an") || msgLower.contains("du bao thoi tiet")
                || msgLower.contains("toan hoc") || msgLower.contains("giai toan") || msgLower.contains("bai tap")) {
            return ChatIntent.OUT_OF_SCOPE;
        }

        // 3. Order Tracking
        if (msgLower.contains("don hang") || msgLower.contains("tra cuu don")
                || msgLower.contains("kiem tra don") || msgLower.contains("tinh trang don")
                || msgLower.contains("ma don") || msgLower.contains("van don")
                || message.toUpperCase().matches(".*(HDSVN|DHSVN)\\d+.*")) {
            return ChatIntent.ORDER_LOOKUP;
        }

        // 4. Voucher / Promotions
        if (msgLower.contains("voucher") || msgLower.contains("ma giam gia")
                || msgLower.contains("khuyen mai") || msgLower.contains("uu dai")
                || msgLower.contains("ma giam") || msgLower.contains("coupon")
                || msgLower.contains("giam gia hom nay")) {
            return ChatIntent.VOUCHER_LOOKUP;
        }

        // 5. Greetings
        if (msgLower.matches("^(xin chao|chao|chao ban|chao shop|hello|hi|hey|alo|ad oi|shop oi)[!\\?\\s.]*$")) {
            return ChatIntent.GREETING;
        }

        // 6. Store Information & Policies
        if (msgLower.contains("hotline") || msgLower.contains("dia chi")
                || msgLower.contains("so dien thoai") || msgLower.contains("email")
                || msgLower.contains("gio mo cua") || msgLower.contains("hoat dong")
                || msgLower.contains("lien he") || msgLower.contains("phong trung bay")
                || msgLower.contains("dia chi shop") || msgLower.contains("o dau")
                || msgLower.contains("doi tra") || msgLower.contains("bao hanh")
                || msgLower.contains("phi ship") || msgLower.contains("van chuyen")
                || msgLower.contains("giao hang")) {
            return ChatIntent.STORE_INFORMATION;
        }

        // 7. Product Consultation (Badminton plays, specs)
        if (msgLower.contains("tan cong") || msgLower.contains("phong thu")
                || msgLower.contains("nang dau") || msgLower.contains("nhe dau")
                || msgLower.contains("luc co tay") || msgLower.contains("nguoi moi choi")
                || msgLower.contains("cong thu toan dien") || msgLower.contains("tro luc")
                || msgLower.contains("cang bao nhieu") || msgLower.contains("suc cang")
                || msgLower.contains("3u") || msgLower.contains("4u") || msgLower.contains("5u")) {
            return ChatIntent.BASIC_CONSULTATION;
        }

        // 8. Product Detail Information
        if (msgLower.contains("chi tiet") || msgLower.contains("thong so")
                || msgLower.contains("chat lieu") || msgLower.contains("cau tao")) {
            return ChatIntent.PRODUCT_INFORMATION;
        }

        // 9. Product Search
        if (msgLower.contains("tim") || msgLower.contains("mua")
                || msgLower.contains("gia") || msgLower.contains("re")
                || msgLower.contains("bao nhieu") || msgLower.contains("con hang")
                || msgLower.contains("san hang") || msgLower.contains("vot")
                || msgLower.contains("giay") || msgLower.contains("k")
                || msgLower.contains("cu") || msgLower.contains("canh") || msgLower.contains("trieu")) {
            return ChatIntent.PRODUCT_SEARCH;
        }

        return ChatIntent.BASIC_CONSULTATION;
    }

    private boolean isPureMedicalQuery(String message) {
        String msgLower = removeAccents(message.toLowerCase());
        boolean hasMedicalKeywords = msgLower.contains("chan thuong") || msgLower.contains("dau khop")
                || msgLower.contains("phuc hoi") || msgLower.contains("bac si") || msgLower.contains("dieu tri")
                || msgLower.contains("dau vai") || msgLower.contains("dau co tay") || msgLower.contains("thuoc");

        boolean hasProductKeywords = msgLower.contains("vot") || msgLower.contains("giay")
                || msgLower.contains("ao") || msgLower.contains("phu kien") || msgLower.contains("yonex")
                || msgLower.contains("lining") || msgLower.contains("victor") || msgLower.contains("san pham")
                || msgLower.contains("bang goi") || msgLower.contains("bang co tay");

        return hasMedicalKeywords && !hasProductKeywords;
    }

    private String callGeminiApi(List<ChatMessage> history, List<ChatProductResponse> suggestionDtos, ChatIntent intent) throws Exception {
        String apiUrl = baseUrl + "/chat/completions";

        List<Map<String, String>> messagesPayload = new ArrayList<>();

        StringBuilder systemPrompt = new StringBuilder("""
                Bạn là Chuyên gia tư vấn Cầu lông & Trợ lý khách hàng của SmashVN Shop.
                
                QUY TẮC PHẠM VI (BẮT BUỘC):
                - CHỈ trả lời các câu hỏi về sản phẩm cầu lông (vợt, giày, cước, phụ kiện), tư vấn lối chơi/kỹ thuật liên quan đến chọn vợt, kiểm tra đơn hàng, voucher khuyến mãi và thông tin chính sách của SmashVN Shop.
                - TUYỆT ĐỐI TỪ CHỐI các câu hỏi ngoài lề (lập trình, toán học, thời tiết, chính trị, tin tức, công thức nấu ăn, giải trí...). Nếu khách hỏi ngoài lề, hãy lịch sự từ chối và nhắc khách rằng bạn chỉ hỗ trợ các dịch vụ của SmashVN Shop.
                - Không tự bịa thông tin sản phẩm, không bịa giá tiền hay đường dẫn. Chỉ dùng thông tin sản phẩm được cung cấp bên dưới.
                - Giọng điệu: Thân thiện, chuyên nghiệp, súc tích (tối đa 3-4 câu).
                
                KIẾN THỨC TƯ VẤN CẦU LÔNG:
                - Lối chơi Tấn công (Smash): Phù hợp vợt nặng đầu (Head-heavy), đũa cứng/trung bình, 3U hoặc 4U đầm tay (ví dụ Astrox, Halbertec, Thruster).
                - Lối chơi Tốc độ / Phản tạt / Phòng thủ: Phù hợp vợt nhẹ đầu/cân bằng, 4U/5U linh hoạt vung vợt (ví dụ Nanoflare, Bladex, DriveX).
                - Công thủ toàn diện: Vợt cân bằng ~290-295mm, dễ thuần (ví dụ Arcsaber, Axforce).
                - Người mới chơi / Nữ / Cổ tay yếu: Chọn vợt 4U/5U thân dẻo để trợ lực tốt, mức căng dây khuyến nghị 9 - 10.5 kg.
                """);

        systemPrompt.append("\nDanh sách sản phẩm từ hệ thống SmashVN:\n");
        if (suggestionDtos.isEmpty()) {
            systemPrompt.append("(Hiện không có sản phẩm khớp trực tiếp, hãy đưa ra lời khuyên chung và hướng dẫn khách tìm theo tầm giá hoặc liên hệ hotline).\n");
        } else {
            for (ChatProductResponse prod : suggestionDtos) {
                systemPrompt.append("- ID: ").append(prod.getId())
                        .append(", Tên: ").append(prod.getName())
                        .append(", Hãng: ").append(prod.getBrand() != null ? prod.getBrand() : "")
                        .append(", Giá: ").append(prod.getPrice().toPlainString()).append(" VND")
                        .append(prod.getSalePrice() != null ? ", Giá KM: " + prod.getSalePrice().toPlainString() + " VND" : "")
                        .append("\n");
            }
        }

        messagesPayload.add(Map.of("role", "system", "content", systemPrompt.toString()));

        for (ChatMessage msg : history) {
            String apiRole = "user";
            if ("ASSISTANT".equalsIgnoreCase(msg.getVaiTro())) {
                apiRole = "assistant";
            } else if ("SYSTEM".equalsIgnoreCase(msg.getVaiTro())) {
                apiRole = "system";
            }
            messagesPayload.add(Map.of("role", apiRole, "content", msg.getNoiDung()));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messagesPayload);
        requestBody.put("max_tokens", 350);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map<String, Object>> response = geminiRestTemplate.exchange(apiUrl, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        if (response != null && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            List<?> choices = (List<?>) body.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = (Map<String, Object>) choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
        }

        throw new RuntimeException("Cấu trúc phản hồi từ Gemini không hợp lệ.");
    }

    private String validateGeminiResponse(String responseText, List<ChatProductResponse> suggestionDtos) {
        String lowerText = responseText.toLowerCase();

        if (lowerText.contains("system prompt") || lowerText.contains("api_key")
                || lowerText.contains("api-key") || lowerText.contains("database schema")
                || lowerText.contains("select *")) {
            return "Xin lỗi, tôi chỉ hỗ trợ các nội dung liên quan đến sản phẩm và dịch vụ của SmashVN Shop.";
        }

        return responseText;
    }

    private ShopContactDto buildContactDto() {
        String addr = shopContactProperties.getAddress();
        String mail = shopContactProperties.getEmail();
        String ph = getHotline();

        ShopContactDto dto = new ShopContactDto();
        dto.setAddress(addr != null && !addr.trim().isEmpty() ? addr.trim() : null);
        dto.setEmail(mail != null && !mail.trim().isEmpty() ? mail.trim() : null);
        dto.setPhone(ph);
        return dto;
    }

    private ChatMessageDto mapToDto(ChatMessage m) {
        String roleStr = m.getVaiTro();
        String senderType = "USER";
        if ("ASSISTANT".equalsIgnoreCase(roleStr)) {
            senderType = "BOT";
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        String formattedTime = m.getNgayTao() != null ? m.getNgayTao().format(dtf) : "";

        return ChatMessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .role(roleStr)
                .senderType(senderType)
                .content(m.getNoiDung())
                .createdAt(m.getNgayTao())
                .thoiGian(formattedTime)
                .status(m.getTrangThai())
                .build();
    }

    @Override
    public List<ChatMessageDto> getConversationHistory(Long conversationId, Integer idTaiKhoan, String sessionId) {
        ChatConversation conversation;
        if (conversationId == null) {
            List<ChatConversation> activeConversations = idTaiKhoan != null
                    ? chatConversationRepository.findAllByTaiKhoanIdAndTrangThai(idTaiKhoan, "ACTIVE")
                    : chatConversationRepository.findAllBySessionIdAndTrangThai(sessionId, "ACTIVE");
            if (activeConversations.isEmpty()) {
                return Collections.emptyList();
            }
            conversation = activeConversations.get(0);
        } else {
            conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại."));
            verifyConversationOwnership(conversation, idTaiKhoan, sessionId);
        }

        List<ChatMessage> messages = chatMessageRepository.findAllByConversationId(conversation.getId());
        messages.sort((m1, m2) -> {
            int dateComp = m2.getNgayTao().compareTo(m1.getNgayTao());
            if (dateComp != 0) return dateComp;
            return m2.getId().compareTo(m1.getId());
        });

        List<ChatMessage> limited = messages.stream().limit(50).collect(Collectors.toList());
        Collections.reverse(limited);

        return limited.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void submitFeedback(ChatFeedbackRequest request, Integer idTaiKhoan, String sessionId) {
        ChatMessage message = chatMessageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new IllegalArgumentException("Tin nhắn không tồn tại."));

        if (!"ASSISTANT".equalsIgnoreCase(message.getVaiTro())) {
            throw new IllegalArgumentException("Chỉ được đánh giá tin nhắn phản hồi của trợ lý ảo.");
        }
        verifyConversationOwnership(message.getConversation(), idTaiKhoan, sessionId);

        Integer rating = request.getRating();
        if (rating == null || (rating != 1 && rating != -1)) {
            throw new IllegalArgumentException("Đánh giá không hợp lệ. Chỉ chấp nhận 1 hoặc -1.");
        }

        Optional<ChatFeedback> existingOpt = idTaiKhoan != null
                ? chatFeedbackRepository.findByMessageIdAndTaiKhoanId(message.getId(), idTaiKhoan)
                : chatFeedbackRepository.findByMessageIdAndSessionId(message.getId(), sessionId);

        ChatFeedback feedback;
        if (existingOpt.isPresent()) {
            feedback = existingOpt.get();
            feedback.setDanhGia(rating.shortValue());
            feedback.setGhiChu(request.getNote());
            feedback.setNgayCapNhat(LocalDateTime.now());
        } else {
            feedback = new ChatFeedback();
            feedback.setMessage(message);
            if (idTaiKhoan != null) {
                feedback.setTaiKhoan(taiKhoanRepository.findById(idTaiKhoan).orElse(null));
            } else {
                feedback.setSessionId(sessionId);
            }
            feedback.setDanhGia(rating.shortValue());
            feedback.setGhiChu(request.getNote());
        }

        chatbotDbHelper.saveFeedback(feedback);
    }

    private ChatConversation getOrCreateConversation(Long conversationId, Integer idTaiKhoan, String sessionId) {
        if (conversationId != null) {
            ChatConversation conversation = chatConversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuộc trò chuyện không tồn tại."));
            verifyConversationOwnership(conversation, idTaiKhoan, sessionId);
            return conversation;
        }

        List<ChatConversation> activeConversations = idTaiKhoan != null
                ? chatConversationRepository.findAllByTaiKhoanIdAndTrangThai(idTaiKhoan, "ACTIVE")
                : chatConversationRepository.findAllBySessionIdAndTrangThai(sessionId, "ACTIVE");

        if (!activeConversations.isEmpty()) {
            activeConversations.sort((c1, c2) -> {
                LocalDateTime t1 = c1.getNgayCapNhat() != null ? c1.getNgayCapNhat() : c1.getNgayTao();
                LocalDateTime t2 = c2.getNgayCapNhat() != null ? c2.getNgayCapNhat() : c2.getNgayTao();
                int dateComp = t2.compareTo(t1);
                if (dateComp != 0) return dateComp;
                return c2.getId().compareTo(c1.getId());
            });
            return activeConversations.get(0);
        }

        ChatConversation newConversation = new ChatConversation();
        newConversation.setTieuDe("Hội thoại tư vấn");
        if (idTaiKhoan != null) {
            newConversation.setTaiKhoan(taiKhoanRepository.findById(idTaiKhoan).orElse(null));
        } else {
            newConversation.setSessionId(sessionId);
        }
        return chatbotDbHelper.saveConversation(newConversation);
    }

    private void verifyConversationOwnership(ChatConversation conversation, Integer idTaiKhoan, String sessionId) {
        if (idTaiKhoan != null) {
            if (conversation.getTaiKhoan() == null || !idTaiKhoan.equals(conversation.getTaiKhoan().getId())) {
                throw new IllegalArgumentException("Bạn không có quyền truy cập cuộc trò chuyện này.");
            }
        } else {
            if (conversation.getSessionId() == null || !conversation.getSessionId().equals(sessionId)) {
                throw new IllegalArgumentException("Bạn không có quyền truy cập cuộc trò chuyện này.");
            }
        }
    }
}
