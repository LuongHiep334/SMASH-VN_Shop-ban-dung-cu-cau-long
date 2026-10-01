package com.smashvn.shop.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.dto.user.UserProfileEditDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDashboardService {

    private final KhachHangRepository khachHangRepository;

    // Lấy thông tin khách hàng từ ID tài khoản (lấy từ Session)
    public KhachHang layThongTinKhachHang(Integer idTaiKhoan) {
        return khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
    }

    // Cập nhật hồ sơ cá nhân
    @Transactional
    public void capNhatHoSo(Integer idTaiKhoan, UserProfileEditDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật không được để trống.");
        }

        // 1. Trim inputs
        String ho = dto.getHo() != null ? dto.getHo().trim() : null;
        String ten = dto.getTen() != null ? dto.getTen().trim() : null;
        String sdt = dto.getSdt() != null ? dto.getSdt().trim() : null;

        // 2. Sanitize inputs against XSS using Jsoup
        String sanitizedHo = sanitizeInput(ho);
        String sanitizedTen = sanitizeInput(ten);
        String sanitizedSdt = sanitizeInput(sdt);

        if ((ho != null && !ho.equals(sanitizedHo)) || 
            (ten != null && !ten.equals(sanitizedTen)) || 
            (sdt != null && !sdt.equals(sanitizedSdt))) {
            log.warn("[SECURITY_ALERT] XSS payload detected and sanitized for user id: {}", idTaiKhoan);
        }

        // 3. Validate empty/blank/null and length limits on sanitized values
        if (sanitizedHo == null || sanitizedHo.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'ho' submission for user id: {}", idTaiKhoan);
            throw new IllegalArgumentException("Họ không được để trống.");
        }
        if (sanitizedHo.length() > 50) {
            log.warn("[SECURITY_ALERT] Invalid length of 'ho' field ({}) for user id: {}", sanitizedHo.length(), idTaiKhoan);
            throw new IllegalArgumentException("Họ không được vượt quá 50 ký tự.");
        }

        if (sanitizedTen == null || sanitizedTen.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'ten' submission for user id: {}", idTaiKhoan);
            throw new IllegalArgumentException("Tên không được để trống.");
        }
        if (sanitizedTen.length() > 50) {
            log.warn("[SECURITY_ALERT] Invalid length of 'ten' field ({}) for user id: {}", sanitizedTen.length(), idTaiKhoan);
            throw new IllegalArgumentException("Tên không được vượt quá 50 ký tự.");
        }

        if (sanitizedSdt == null || sanitizedSdt.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'sdt' submission for user id: {}", idTaiKhoan);
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (!sanitizedSdt.matches("^(\\+84|0)(3|5|7|8|9)[0-9]{8}$")) {
            log.warn("[SECURITY_ALERT] Invalid phone number format submitted for user id: {}", idTaiKhoan);
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng Việt Nam.");
        }

        // 4. Check duplicate phone number for other accounts
        KhachHang existingWithPhone = khachHangRepository.findBySoDienThoaiKh(sanitizedSdt);
        if (existingWithPhone != null && !existingWithPhone.getTaiKhoan().getId().equals(idTaiKhoan)) {
            log.warn("[SECURITY_ALERT] Attempted to update duplicate phone number for user id: {}", idTaiKhoan);
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký bởi tài khoản khác!");
        }

        // 5. Persistence
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(idTaiKhoan);
        if (kh != null) {
            kh.setHoKh(sanitizedHo);
            kh.setTenKh(sanitizedTen);
            kh.setSoDienThoaiKh(sanitizedSdt);
            khachHangRepository.save(kh);
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return org.jsoup.Jsoup.clean(input, org.jsoup.safety.Safelist.none());
    }
}