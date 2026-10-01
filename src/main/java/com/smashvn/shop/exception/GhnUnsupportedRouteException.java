package com.smashvn.shop.exception;

/**
 * Exception ném ra khi GHN chưa hỗ trợ tuyến giao hàng giữa điểm gửi và điểm nhận.
 * Dùng để phân biệt rõ ràng giữa lỗi tuyến đường (có thể fallback trên Sandbox)
 * và lỗi dữ liệu nội bộ (không bao giờ fallback).
 */
public class GhnUnsupportedRouteException extends RuntimeException {

    public GhnUnsupportedRouteException(String message) {
        super(message);
    }

    public GhnUnsupportedRouteException(String message, Throwable cause) {
        super(message, cause);
    }
}
