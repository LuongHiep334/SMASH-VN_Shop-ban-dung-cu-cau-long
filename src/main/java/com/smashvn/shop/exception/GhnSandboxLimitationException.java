package com.smashvn.shop.exception;

/**
 * Lỗi nghiệp vụ đã biết chỉ xuất hiện do giới hạn dữ liệu của GHN Sandbox,
 * ví dụ Sandbox không đọc được thông tin kho dù yêu cầu tạo vận đơn hợp lệ.
 * Lỗi này được phép chuyển sang vận đơn Demo nhưng chỉ trên môi trường Sandbox.
 */
public class GhnSandboxLimitationException extends RuntimeException {

    public GhnSandboxLimitationException(String message) {
        super(message);
    }

    public GhnSandboxLimitationException(String message, Throwable cause) {
        super(message, cause);
    }
}
