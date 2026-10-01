package com.smashvn.shop.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

import com.smashvn.shop.entity.AccountStatus;
import com.smashvn.shop.entity.TaiKhoan;
import com.smashvn.shop.entity.TokenKhoiPhuc;
import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.repository.TaiKhoanRepository;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.TokenKhoiPhucRepository;
import com.smashvn.shop.service.order.GuestCheckoutService;
import com.smashvn.shop.util.PhoneUtils;

@Service
@RequiredArgsConstructor
public class UserQuenMatKhauService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final TokenKhoiPhucRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final GuestCheckoutService guestCheckoutService;

    // 1. Tạo và gửi Link khôi phục
    @Transactional
    public void guiYeuCauKhoiPhuc(String inputIdentifier, String appUrl) {
        if (inputIdentifier == null || inputIdentifier.trim().isEmpty()) {
            throw new RuntimeException("Tài khoản không được để trống!");
        }
        String trimmedIdentifier = inputIdentifier.trim();

        // Find account directly by username
        TaiKhoan tk = taiKhoanRepository.findByUsername(trimmedIdentifier);
        
        // Fallback to check KhachHang.soDienThoaiKh to support legacy data
        if (tk == null) {
            String normalizedPhone = PhoneUtils.normalize(trimmedIdentifier);
            if (PhoneUtils.isValid(normalizedPhone)) {
                KhachHang kh = khachHangRepository.findBySoDienThoaiKh(normalizedPhone);
                if (kh != null) {
                    tk = kh.getTaiKhoan();
                }
            }
        }

        if (tk == null) {
            throw new RuntimeException("Tài khoản không tồn tại trên hệ thống!");
        }

        // Chặn tài khoản bị khóa hoặc chờ khóa
        if (tk.getTrangThaiTaiKhoan() == AccountStatus.LOCKED || tk.getTrangThaiTaiKhoan() == AccountStatus.PENDING_LOCK
                || "bi_khoa".equals(tk.getTrangThai()) || "cho_khoa".equals(tk.getTrangThai())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa hoặc đang chờ khóa. Vui lòng liên hệ quản trị viên để được hỗ trợ!");
        }

        // Không cho phép tài khoản Guest chưa kích hoạt mật khẩu dùng tính năng Quên mật khẩu
        if (tk.getTrangThaiTaiKhoan() == AccountStatus.GUEST && (tk.getMatKhau() == null || tk.getMatKhau().trim().isEmpty())) {
            throw new RuntimeException("Tài khoản này là tài khoản khách vãng lai chưa thiết lập mật khẩu. Vui lòng kiểm tra email kích hoạt tài khoản đã nhận khi đặt hàng!");
        }

        // Check if the username is a valid email
        String destinationEmail = tk.getUsername();
        if (!destinationEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new RuntimeException("Tài khoản của bạn đăng ký bằng số điện thoại và không có email khôi phục. Vui lòng liên hệ quản trị viên để được hỗ trợ!");
        }

        // Vô hiệu hóa tất cả token FORGOT_PASSWORD cũ chưa dùng của tài khoản này
        java.util.List<TokenKhoiPhuc> oldTokens = tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(tk.getId(), "FORGOT_PASSWORD");
        for (TokenKhoiPhuc old : oldTokens) {
            old.setDaSuDung(true);
        }
        if (!oldTokens.isEmpty()) {
            tokenRepository.saveAll(oldTokens);
        }

        // Tạo chuỗi Token ngẫu nhiên không trùng lặp
        String token = UUID.randomUUID().toString();

        // Lưu vào Database với loại FORGOT_PASSWORD
        TokenKhoiPhuc tkp = new TokenKhoiPhuc();
        tkp.setTaiKhoan(tk);
        tkp.setMaXacNhan(token);
        tkp.setLoaiXacNhan("FORGOT_PASSWORD");
        tkp.setThoiGianHetHan(LocalDateTime.now().plusMinutes(15)); // Hết hạn sau 15 phút
        tkp.setDaSuDung(false);
        tokenRepository.save(tkp);

        // Tạo Link trỏ về trang đổi mật khẩu
        String resetUrl = appUrl + "/user/dat-lai-mat-khau?token=" + token;

        // Gửi Mail
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destinationEmail);
        message.setSubject("Yêu cầu khôi phục mật khẩu - Smash VN");
        message.setText("Chào bạn,\n\n" +
                "Bạn vừa yêu cầu khôi phục mật khẩu. Vui lòng click vào đường link bên dưới để đặt lại mật khẩu mới:\n" +
                resetUrl + "\n\n" +
                "Đường link này sẽ hết hạn sau 15 phút.\n" +
                "Nếu bạn không yêu cầu, vui lòng bỏ qua email này.");
        
        mailSender.send(message);
    }

    // 2. Xác thực Token có hợp lệ không (Chưa hết hạn, chưa dùng, đúng loại FORGOT_PASSWORD)
    public TokenKhoiPhuc kiemTraToken(String token) {
        TokenKhoiPhuc tkp = tokenRepository.findByMaXacNhan(token);
        if (tkp == null) {
            throw new RuntimeException("Đường link khôi phục không hợp lệ!");
        }
        if (!"FORGOT_PASSWORD".equals(tkp.getLoaiXacNhan())) {
            throw new RuntimeException("Đường link khôi phục không hợp lệ hoặc sai loại xác nhận!");
        }
        if (tkp.isDaSuDung()) {
            throw new RuntimeException("Đường link này đã được sử dụng!");
        }
        if (tkp.getThoiGianHetHan().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Đường link này đã hết hạn!");
        }

        TaiKhoan tk = tkp.getTaiKhoan();
        if (tk == null || tk.getTrangThaiTaiKhoan() == AccountStatus.LOCKED || tk.getTrangThaiTaiKhoan() == AccountStatus.PENDING_LOCK) {
            throw new RuntimeException("Tài khoản liên kết với link này đã bị khóa hoặc không hợp lệ!");
        }

        return tkp;
    }

    // 3. Tiến hành đổi mật khẩu
    @Transactional
    public void datLaiMatKhau(String token, String matKhauMoi) {
        TokenKhoiPhuc tkp = kiemTraToken(token); // Kiểm tra lại lần cuối cho chắc

        if (matKhauMoi == null || matKhauMoi.isEmpty()) {
            throw new RuntimeException("Mật khẩu mới không được để trống!");
        }
        if (matKhauMoi.length() < 8 || matKhauMoi.length() > 30) {
            throw new RuntimeException("Mật khẩu phải dài từ 8 đến 30 ký tự!");
        }
        if (matKhauMoi.contains(" ") || matKhauMoi.contains("\t") || matKhauMoi.contains("\n") || matKhauMoi.contains("\r")) {
            throw new RuntimeException("Mật khẩu không được chứa khoảng trắng!");
        }
        if (!matKhauMoi.matches("^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$")) {
            throw new RuntimeException("Mật khẩu phải chứa cả chữ và số!");
        }

        TaiKhoan tk = tkp.getTaiKhoan();
        if (tk.getTrangThaiTaiKhoan() == AccountStatus.GUEST) {
            // Mọi đường đặt mật khẩu chính thức của Guest đều đi qua cùng một điểm chuyển ACTIVE.
            guestCheckoutService.setPasswordForGuest(tk.getId(), matKhauMoi);
        } else {
            tk.setMatKhau(passwordEncoder.encode(matKhauMoi));
            taiKhoanRepository.saveAndFlush(tk);
        }

        tkp.setDaSuDung(true);
        tokenRepository.saveAndFlush(tkp);

        // Vô hiệu hóa mọi token FORGOT_PASSWORD còn lại của tài khoản này
        java.util.List<TokenKhoiPhuc> otherTokens = tokenRepository.findByTaiKhoan_IdAndLoaiXacNhanAndDaSuDungFalse(tk.getId(), "FORGOT_PASSWORD");
        for (TokenKhoiPhuc ot : otherTokens) {
            ot.setDaSuDung(true);
        }
        if (!otherTokens.isEmpty()) {
            tokenRepository.saveAll(otherTokens);
        }
    }
}
