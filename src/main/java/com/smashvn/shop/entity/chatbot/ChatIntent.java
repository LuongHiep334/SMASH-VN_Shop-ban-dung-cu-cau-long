package com.smashvn.shop.entity.chatbot;

public enum ChatIntent {
    PRODUCT_SEARCH,          // Hỏi tìm kiếm danh sách sản phẩm
    PRODUCT_INFORMATION,     // Hỏi chi tiết một sản phẩm cụ thể
    STORE_INFORMATION,       // Hỏi địa chỉ, hotline, giờ hoạt động
    BASIC_CONSULTATION,      // Tư vấn cơ bản (người mới, thiên công/thủ...)
    ADVANCED_CONSULTATION,   // Tư vấn chuyên sâu/kỹ thuật/y tế
    ORDER_LOOKUP,            // Tra cứu trạng thái đơn hàng
    VOUCHER_LOOKUP,          // Tra cứu mã khuyến mãi, voucher
    GREETING,                // Chào hỏi, tương tác mở đầu
    OUT_OF_SCOPE,            // Ngoài phạm vi dự án (tin tức, lập trình, giải trí...)
    SECURITY_SENSITIVE       // Yêu cầu nhạy cảm bảo mật (API key, system prompt, SQL...)
}
