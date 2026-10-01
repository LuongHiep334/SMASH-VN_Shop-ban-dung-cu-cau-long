package com.smashvn.shop.util;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Pattern;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailValidatorUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,10}$");

    private static final Set<String> DISALLOWED_TYPO_DOMAINS = Set.of(
        "gm2.com", "gmai.com", "gamil.com", "gm.com", "gmai.com.vn", "gamil.com.vn", "gmail2.com",
        "yaho.com", "yahooo.com", "yaho.com.vn",
        "hotmai.com", "hotmial.com",
        "outlok.com", "outlookk.com",
        "iclud.com",
        "test.com", "example.com", "tempmail.com", "mailinator.com", "dispostable.com"
    );

    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống!");
        }
        String trimmedEmail = email.trim().toLowerCase();
        if (trimmedEmail.length() > 100) {
            throw new IllegalArgumentException("Email không được vượt quá 100 ký tự!");
        }
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ!");
        }

        String[] parts = trimmedEmail.split("@");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Định dạng email không hợp lệ!");
        }
        String domain = parts[1].trim().toLowerCase();

        // 1. Check blocked typo / fake email domains
        if (DISALLOWED_TYPO_DOMAINS.contains(domain)) {
            throw new IllegalArgumentException("Tên miền email không hợp lệ hoặc có dấu hiệu viết sai (Ví dụ: @gm2.com). Vui lòng sử dụng địa chỉ email thực!");
        }

        // 2. DNS check: Check if domain has MX records or valid IP address
        if (!isDomainValidAndReachable(domain)) {
            throw new IllegalArgumentException("Tên miền email '@" + domain + "' không tồn tại hoặc không thể nhận thư. Vui lòng kiểm tra lại địa chỉ email!");
        }
    }

    public static boolean isValid(String email) {
        try {
            validateEmail(email);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isDomainValidAndReachable(String domain) {
        // Quick DNS MX record check
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            InitialDirContext ictx = new InitialDirContext(env);
            Attributes attrs = ictx.getAttributes(domain, new String[]{"MX"});
            if (attrs != null && attrs.get("MX") != null && attrs.get("MX").size() > 0) {
                return true;
            }
        } catch (Exception e) {
            // MX lookup failed, fallback to A record host resolution
        }

        try {
            InetAddress addr = InetAddress.getByName(domain);
            return addr != null;
        } catch (Exception e) {
            log.warn("[EMAIL_VALIDATOR] Domain resolution failed for {}: {}", domain, e.getMessage());
            return false;
        }
    }
}
