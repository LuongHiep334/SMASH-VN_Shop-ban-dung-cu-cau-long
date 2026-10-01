package com.smashvn.shop.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RegisterRateLimiterTest {

    private RegisterRateLimiter rateLimiter;
    private final String testIp = "192.168.1.1";

    @BeforeEach
    void setUp() {
        rateLimiter = new RegisterRateLimiter();
    }

    @Test
    void testNotBlockedInitially() {
        assertFalse(rateLimiter.isBlocked(testIp));
    }

    @Test
    void testBlockedAfterFiveFailures() {
        // First 4 failures do not block
        for (int i = 0; i < 4; i++) {
            rateLimiter.registerFailed(testIp);
            assertFalse(rateLimiter.isBlocked(testIp));
        }

        // 5th failure blocks
        rateLimiter.registerFailed(testIp);
        assertTrue(rateLimiter.isBlocked(testIp));
    }

    @Test
    void testSucceedResetsAttempts() {
        rateLimiter.registerFailed(testIp);
        rateLimiter.registerFailed(testIp);
        rateLimiter.registerSucceeded(testIp);

        // After success, it should take 5 more failures to block
        for (int i = 0; i < 4; i++) {
            rateLimiter.registerFailed(testIp);
            assertFalse(rateLimiter.isBlocked(testIp));
        }
        rateLimiter.registerFailed(testIp);
        assertTrue(rateLimiter.isBlocked(testIp));
    }
}
