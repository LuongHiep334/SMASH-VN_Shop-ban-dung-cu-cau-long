package com.smashvn.shop.exception;

/**
 * Exception ném ra khi xảy ra timeout hoặc lỗi mạng trong lúc gọi POST /shipping-order/create.
 * Kết quả tạo đơn ở phía GHN là KHÔNG XÁC ĐỊNH (GHN có thể đã tạo đơn hoặc chưa).
 * Tuyệt đối không tự động sinh DEMO code và không tự động retry để tránh tạo 2 vận đơn.
 */
public class GhnCreateIndeterminateException extends RuntimeException {

    public GhnCreateIndeterminateException(String message) {
        super(message);
    }

    public GhnCreateIndeterminateException(String message, Throwable cause) {
        super(message, cause);
    }
}
