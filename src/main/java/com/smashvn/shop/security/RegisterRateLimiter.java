package com.smashvn.shop.security;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RegisterRateLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = TimeUnit.MINUTES.toMillis(15);
    
    // Lưu số lần đăng ký sai
    private final ConcurrentHashMap<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    // Lưu mốc thời gian bắt đầu bị khóa
    private final ConcurrentHashMap<String, Long> lockTimeCache = new ConcurrentHashMap<>();
    
    public boolean isBlocked(String key) {
        if (lockTimeCache.containsKey(key)) {
            long lockTime = lockTimeCache.get(key);
            if (System.currentTimeMillis() - lockTime < BLOCK_DURATION_MS) {
                return true;
            } else {
                // Đã hết thời gian khóa, tự động mở khóa
                lockTimeCache.remove(key);
                attemptsCache.remove(key);
                log.info("Mở khóa rate-limit đăng ký thành công cho IP: {}", key);
            }
        }
        return false;
    }
    
    public void registerFailed(String key) {
        int attempts = attemptsCache.getOrDefault(key, 0) + 1;
        attemptsCache.put(key, attempts);
        log.warn("Đăng ký thất bại lần {} cho IP: {}", attempts, key);
        
        if (attempts >= MAX_ATTEMPTS) {
            lockTimeCache.put(key, System.currentTimeMillis());
            // Log an ninh dạng WARN
            log.warn("[SECURITY_EVENT] REGISTER_RATE_LIMIT_TRIGGER: IP: {} bị chặn đăng ký do vượt quá {} lần thử.", key, MAX_ATTEMPTS);
        }
    }
    
    public void registerSucceeded(String key) {
        attemptsCache.remove(key);
        lockTimeCache.remove(key);
    }
}
