package com.smashvn.shop.controller.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smashvn.shop.service.api.LocationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationRestController {

    private final LocationService locationService;

    @Value("${location.test.ip:1.53.0.0}")
    private String testIpFallback;

    // Rate Limiting Storage: IP -> Timestamps of requests in the last minute
    private final Map<String, List<Long>> ipRequestTimestamps = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    @GetMapping("/ip")
    public ResponseEntity<?> getIpLocation(HttpServletRequest request) {
        String ip = getClientIp(request);
        
        // 1. Rate Limiting Check
        if (isRateLimited(ip)) {
            System.out.println("[Location API] Rate limit exceeded for IP: " + ip);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Rate limit exceeded. Please try again later.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        System.out.println("[Location API] Received request from IP: " + ip);

        // 2. Local IP Fallback (for development environments)
        if (isLocalIp(ip)) {
            ip = testIpFallback;
            System.out.println("[Location API] Local IP detected. Falling back to configured test IP: " + ip);
        }

        // 3. Delegate to LocationService
        try {
            Map<String, Object> locationData = locationService.getIpLocation(ip);
            return ResponseEntity.ok(locationData);
        } catch (Exception e) {
            System.err.println("[Location API] Error resolving location for IP " + ip + ": " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể lấy dữ liệu vị trí vào lúc này.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = ipRequestTimestamps.computeIfAbsent(ip, k -> new CopyOnWriteArrayList<>());
        
        // Clean up timestamps older than 1 minute
        timestamps.removeIf(time -> now - time > 60000);
        
        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            return true;
        }
        
        timestamps.add(now);
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // Parse out first proxy in list if applicable
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private boolean isLocalIp(String ip) {
        if (ip == null) return true;
        String cleanIp = ip.trim();
        return "127.0.0.1".equals(cleanIp) || 
               "0:0:0:0:0:0:0:1".equals(cleanIp) || 
               "localhost".equalsIgnoreCase(cleanIp) ||
               cleanIp.startsWith("192.168.") || 
               cleanIp.startsWith("10.") || 
               cleanIp.startsWith("172.16.") || 
               cleanIp.startsWith("172.17.") || 
               cleanIp.startsWith("172.18.") || 
               cleanIp.startsWith("172.19.") || 
               cleanIp.startsWith("172.20.") || 
               cleanIp.startsWith("172.21.") || 
               cleanIp.startsWith("172.22.") || 
               cleanIp.startsWith("172.23.") || 
               cleanIp.startsWith("172.24.") || 
               cleanIp.startsWith("172.25.") || 
               cleanIp.startsWith("172.26.") || 
               cleanIp.startsWith("172.27.") || 
               cleanIp.startsWith("172.28.") || 
               cleanIp.startsWith("172.29.") || 
               cleanIp.startsWith("172.30.") || 
               cleanIp.startsWith("172.31.");
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchAddress(@RequestParam("q") String query, HttpServletRequest request) {
        String ip = getClientIp(request);
        
        // 1. Rate Limiting Check
        if (isRateLimited(ip)) {
            System.out.println("[Location API] Rate limit exceeded for search by IP: " + ip);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Rate limit exceeded. Please try again later.");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
        }

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Query string is empty");
        }

        try {
            Map<String, Object> searchResult = locationService.searchAddress(query.trim());
            return ResponseEntity.ok(searchResult);
        } catch (Exception e) {
            System.err.println("[Location API] Error geocoding address '" + query + "': " + e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể tìm thấy tọa độ địa chỉ này.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
