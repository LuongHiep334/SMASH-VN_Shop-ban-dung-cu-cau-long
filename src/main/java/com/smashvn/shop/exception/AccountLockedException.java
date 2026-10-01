package com.smashvn.shop.exception;

/**
 * Ném khi thông tin xác thực đúng nhưng tài khoản đang bị khóa.
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException() {
        super("Tài khoản của bạn đã bị khóa.");
    }
}
