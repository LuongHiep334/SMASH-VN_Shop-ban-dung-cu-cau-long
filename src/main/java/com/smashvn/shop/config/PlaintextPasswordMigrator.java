package com.smashvn.shop.config;

import java.util.List;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.TaiKhoanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(
    name = "app.password-migration.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@RequiredArgsConstructor
@Slf4j
public class PlaintextPasswordMigrator implements CommandLineRunner {

    private final TaiKhoanRepository taiKhoanRepository;

    @Override
    public void run(String... args) throws Exception {
        List<TaiKhoan> accounts = taiKhoanRepository.findPlaintextAccounts();

        if (accounts.isEmpty()) {
            return; // Trả về ngay lập tức nếu không có tài khoản plaintext nào cần di trú, tránh in log thừa lúc khởi động
        }

        log.info("[MIGRATION] Phát hiện {} tài khoản có mật khẩu plaintext. Bắt đầu mã hóa bằng BCrypt...", accounts.size());
        int count = 0;

        for (TaiKhoan tk : accounts) {
            String dbPass = tk.getMatKhau();
            if (dbPass != null) {
                String hashed = BCrypt.hashpw(dbPass, BCrypt.gensalt());
                tk.setMatKhau(hashed);
                taiKhoanRepository.save(tk);
                count++;
                log.info("[MIGRATION] Đã mã hóa thành công tài khoản email: {}", tk.getUsername());
            }
        }

        log.info("[MIGRATION] Hoàn thành di trú! Tổng cộng đã mã hóa thành công {} tài khoản.", count);
    }
}
