package com.smashvn.shop.exception;

/**
 * Ném khi người dùng nhập email chưa được đăng ký trong hệ thống.
 * Chỉ dùng làm tín hiệu nội bộ; giao diện đăng nhập vẫn trả về lỗi xác thực chung.
 */
public class AccountNotFoundException extends RuntimeException {
    private final String email;

    public AccountNotFoundException(String email) {
        super("Email " + email + " chưa được đăng ký trong hệ thống.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
