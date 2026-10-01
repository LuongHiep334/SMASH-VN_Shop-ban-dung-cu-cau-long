package com.smashvn.shop.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.NewsletterSubscriberRepository;
import com.smashvn.shop.util.LoginIdentifierClassifier;
import com.smashvn.shop.util.LoginIdentifierClassifier.NormalizedLoginIdentifier;
import com.smashvn.shop.util.LoginIdentifierClassifier.LoginIdentifierType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDangKyService {
    
    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TaiKhoan dangKy(String username, String matKhau) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống!");
        }
        if (username.trim().length() > 100) {
            throw new RuntimeException("Email không được vượt quá 100 ký tự!");
        }

        String finalUsername;
        String finalPhone = null;

        // Determine if input is a phone number or email
        boolean looksLikePhone = username.trim().matches("^\\+?\\d{9,15}$") || com.smashvn.shop.util.PhoneUtils.isValid(com.smashvn.shop.util.PhoneUtils.normalize(username));

        if (!looksLikePhone) {
            // Validate as email
            String emailTrimmed = username.trim().toLowerCase();
            try {
                com.smashvn.shop.util.EmailValidatorUtils.validateEmail(emailTrimmed);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException(e.getMessage());
            }
            finalUsername = emailTrimmed;

            // Check duplicate in TaiKhoan
            if (taiKhoanRepository.existsByUsername(finalUsername)) {
                throw new RuntimeException("Email này đã được sử dụng!");
            }
        } else {
            // Validate and normalize as phone number
            String normalizedPhone = com.smashvn.shop.util.PhoneUtils.normalize(username.trim());
            if (!com.smashvn.shop.util.PhoneUtils.isValid(normalizedPhone)) {
                throw new RuntimeException("Vui lòng nhập email hoặc số điện thoại hợp lệ.");
            }
            finalUsername = normalizedPhone;
            finalPhone = normalizedPhone;

            // Check duplicate in TaiKhoan.username
            if (taiKhoanRepository.existsByUsername(finalUsername)) {
                throw new RuntimeException("Số điện thoại này đã được sử dụng!");
            }
            // Check duplicate in KhachHang.soDienThoaiKh
            KhachHang existingKh = khachHangRepository.findBySoDienThoaiKh(finalPhone);
            if (existingKh != null) {
                throw new RuntimeException("Số điện thoại này đã được sử dụng!");
            }
        }

        // Password policy: 8-25 chars, at least one uppercase letter and one number, no whitespace
        if (matKhau == null || matKhau.isEmpty()) {
            throw new RuntimeException("Mật khẩu không được để trống!");
        }
        if (matKhau.length() < 8 || matKhau.length() > 25) {
            throw new RuntimeException("Mật khẩu phải dài từ 8 đến 25 ký tự!");
        }
        if (matKhau.contains(" ") || matKhau.contains("\t") || matKhau.contains("\n") || matKhau.contains("\r")) {
            throw new RuntimeException("Mật khẩu không được chứa khoảng trắng!");
        }
        if (!matKhau.matches(".*[A-Z].*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ in hoa!");
        }
        if (!matKhau.matches(".*\\d.*")) {
            throw new RuntimeException("Mật khẩu phải có ít nhất 1 chữ số!");
        }

        // 2. Tạo Tài khoản
        TaiKhoan taiKhoanMoi = new TaiKhoan();
        taiKhoanMoi.setUsername(finalUsername);
        
        // Mã hóa mật khẩu
        String hashed = passwordEncoder.encode(matKhau);
        taiKhoanMoi.setMatKhau(hashed); 
        
        taiKhoanMoi.setVaiTro("KH");
        taiKhoanMoi.setTrangThai("hoat_dong");
        
        // Lưu Tài khoản để lấy ID
        TaiKhoan tkDaLuu = taiKhoanRepository.save(taiKhoanMoi);

        // 3. TỰ ĐỘNG TẠO HỒ SƠ KHÁCH HÀNG
        KhachHang khMoi = new KhachHang();
        khMoi.setTaiKhoan(tkDaLuu); // Móc nối với tài khoản vừa tạo
        khMoi.setHoKh(""); 
        khMoi.setTenKh("Người dùng mới"); // Đặt tên mặc định
        khMoi.setSoDienThoaiKh(finalPhone); // NULL if email, normalized phone string if phone
        boolean alreadySubscribed = false;
        if (finalPhone == null) {
            alreadySubscribed = newsletterSubscriberRepository.findByEmail(finalUsername)
                    .map(sub -> "hoat_dong".equalsIgnoreCase(sub.getTrangThai()))
                    .orElse(false);
        }
        khMoi.setNhanBanTin(alreadySubscribed);
        
        // Lưu Khách hàng
        khachHangRepository.save(khMoi);

        return tkDaLuu;
    }
}
