package com.smashvn.shop.exception;

/**
 * Ném khi tài khoản tồn tại nhưng mật khẩu nhập vào không khớp mật khẩu đã mã hóa.
 */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Mật khẩu không chính xác.");
    }
}
