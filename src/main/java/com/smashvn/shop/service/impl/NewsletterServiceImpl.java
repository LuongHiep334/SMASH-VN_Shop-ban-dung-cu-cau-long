package com.smashvn.shop.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smashvn.shop.dao.DotGiamGiaDAO;
import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.NewsletterSubscriber;
import com.smashvn.shop.entity.PhieuGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.exception.NewsletterValidationException;
import com.smashvn.shop.repository.KhachHangRepository;
import com.smashvn.shop.repository.NewsletterSubscriberRepository;
import com.smashvn.shop.repository.PhieuGiamGiaRepository;
import com.smashvn.shop.service.NewsletterService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterServiceImpl implements NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final DotGiamGiaDAO dotGiamGiaDAO;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final KhachHangRepository khachHangRepository;
    private final JavaMailSender mailSender;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final String BASE_URL = "http://localhost:8080";
    private static final int BATCH_SIZE = 50;

    @Override
    @Transactional
    public void subscribe(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new NewsletterValidationException("Email không được để trống!");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (!normalizedEmail.matches(EMAIL_REGEX)) {
            throw new NewsletterValidationException("Định dạng email không hợp lệ!");
        }

        Optional<NewsletterSubscriber> existingOpt = subscriberRepository.findByEmail(normalizedEmail);

        if (existingOpt.isPresent()) {
            NewsletterSubscriber subscriber = existingOpt.get();
            if ("hoat_dong".equalsIgnoreCase(subscriber.getTrangThai())) {
                throw new NewsletterValidationException("Email này đã đăng ký nhận ưu đãi.");
            } else {
                // Reactivate
                subscriber.setTrangThai("hoat_dong");
                subscriber.setNgayDangKy(LocalDateTime.now());
                subscriber.setNgayHuy(null);
                subscriber.setTokenHuy(UUID.randomUUID().toString());
                subscriberRepository.save(subscriber);
                sendWelcomeEmail(subscriber);
                log.info("[Newsletter] Reactivated subscriber: {}", normalizedEmail);
            }
        } else {
            // Create new
            NewsletterSubscriber subscriber = new NewsletterSubscriber();
            subscriber.setEmail(normalizedEmail);
            subscriber.setNgayDangKy(LocalDateTime.now());
            subscriber.setTrangThai("hoat_dong");
            subscriber.setTokenHuy(UUID.randomUUID().toString());
            subscriberRepository.save(subscriber);
            sendWelcomeEmail(subscriber);
            log.info("[Newsletter] Created new subscriber: {}", normalizedEmail);
        }

        // Sync with KhachHang profile if it exists
        khachHangRepository.findByTaiKhoan_Username(normalizedEmail).ifPresent(kh -> {
            kh.setNhanBanTin(true);
            khachHangRepository.save(kh);
        });
    }

    @Override
    @Transactional
    public void unsubscribe(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new NewsletterValidationException("Token không hợp lệ!");
        }

        NewsletterSubscriber subscriber = subscriberRepository.findByTokenHuy(token.trim())
                .orElseThrow(() -> new NewsletterValidationException("Không tìm thấy thông tin đăng ký nhận tin với token này!"));

        if ("da_huy".equalsIgnoreCase(subscriber.getTrangThai())) {
            log.info("[Newsletter] Subscriber was already unsubscribed: {}", subscriber.getEmail());
            return;
        }

        subscriber.setTrangThai("da_huy");
        subscriber.setNgayHuy(LocalDateTime.now());
        subscriberRepository.save(subscriber);
        log.info("[Newsletter] Unsubscribed email: {}", subscriber.getEmail());

        // Sync with KhachHang profile if it exists
        khachHangRepository.findByTaiKhoan_Username(subscriber.getEmail()).ifPresent(kh -> {
            kh.setNhanBanTin(false);
            khachHangRepository.save(kh);
        });
    }

    @Override
    @Async("newsletterExecutor")
    public void sendPromotionEmailAsync(Integer dotGiamGiaId) {
        log.info("[Newsletter] Starting async promotion email job for ID: {}", dotGiamGiaId);
        Optional<DotGiamGia> campaignOpt = dotGiamGiaDAO.findById(dotGiamGiaId);
        if (campaignOpt.isEmpty()) {
            log.error("[Newsletter] Promotion campaign not found with ID: {}", dotGiamGiaId);
            return;
        }

        DotGiamGia campaign = campaignOpt.get();
        if (campaign.getActive() == null || !campaign.getActive()) {
            log.warn("[Newsletter] Promotion campaign ID {} is not active, skipping email notifications", dotGiamGiaId);
            return;
        }

        List<NewsletterSubscriber> activeSubscribers = subscriberRepository.findByTrangThai("hoat_dong");
        if (activeSubscribers.isEmpty()) {
            log.info("[Newsletter] No active subscribers found for promotion email");
            return;
        }

        log.info("[Newsletter] Found {} active subscribers for promotion email", activeSubscribers.size());

        int total = activeSubscribers.size();
        int successCount = 0;
        int failureCount = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String startTimeStr = campaign.getNgayBatDau().format(formatter);
        String endTimeStr = campaign.getNgayKetThuc().format(formatter);

        // Build list of products in promotion
        StringBuilder productListHtml = new StringBuilder();
        if (campaign.getSanPhams() != null && !campaign.getSanPhams().isEmpty()) {
            productListHtml.append("<div style='margin-top: 15px; border-top: 1px solid #eee; padding-top: 15px;'>");
            productListHtml.append("<h4 style='color: #ff4500; margin-bottom: 10px;'>Sản phẩm áp dụng tiêu biểu:</h4>");
            productListHtml.append("<ul style='padding-left: 20px; color: #555;'>");
            int count = 0;
            for (SanPham sp : campaign.getSanPhams()) {
                if (count++ >= 5) {
                    productListHtml.append("<li>... và nhiều sản phẩm hấp dẫn khác!</li>");
                    break;
                }
                String spName = sp.getTenSanPham();
                String spBrand = sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : "Chính hãng";
                productListHtml.append("<li style='margin-bottom: 5px;'><strong>").append(spName).append("</strong> (").append(spBrand).append(")</li>");
            }
            productListHtml.append("</ul>");
            productListHtml.append("</div>");
        }

        for (int i = 0; i < total; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, total);
            List<NewsletterSubscriber> batch = activeSubscribers.subList(i, end);

            for (NewsletterSubscriber sub : batch) {
                try {
                    String htmlContent = buildPromotionHtml(campaign.getTenChienDich(), campaign.getPhanTramGiam(), startTimeStr, endTimeStr, productListHtml.toString(), sub.getTokenHuy());
                    sendHtmlMail(sub.getEmail(), "[Smash VN] Siêu ưu đãi: Chiến dịch " + campaign.getTenChienDich() + " giảm tới " + campaign.getPhanTramGiam() + "%", htmlContent);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.warn("[Newsletter] Failed to send promotion email to recipient (token hidden): {}", e.getMessage());
                }
            }

            if (end < total) {
                try {
                    Thread.sleep(1500); // Backoff between batches
                } catch (InterruptedException e) {
                    log.warn("[Newsletter] Async promotion email thread interrupted during batch sleep");
                    Thread.currentThread().interrupt();
                    return; // Stop the task immediately
                }
            }
        }

        log.info("[Newsletter] Finished promotion email job. Total: {}, Success: {}, Failure: {}", total, successCount, failureCount);
    }

    @Override
    @Async("newsletterExecutor")
    public void sendVoucherEmailAsync(Integer phieuGiamGiaId) {
        log.info("[Newsletter] Starting async voucher email job for ID: {}", phieuGiamGiaId);
        Optional<PhieuGiamGia> voucherOpt = phieuGiamGiaRepository.findById(phieuGiamGiaId);
        if (voucherOpt.isEmpty()) {
            log.error("[Newsletter] Voucher not found with ID: {}", phieuGiamGiaId);
            return;
        }

        PhieuGiamGia voucher = voucherOpt.get();
        if (voucher.getActive() == null || !voucher.getActive()) {
            log.warn("[Newsletter] Voucher ID {} is not active, skipping email notifications", phieuGiamGiaId);
            return;
        }

        List<NewsletterSubscriber> activeSubscribers = subscriberRepository.findByTrangThai("hoat_dong");
        if (activeSubscribers.isEmpty()) {
            log.info("[Newsletter] No active subscribers found for voucher email");
            return;
        }

        log.info("[Newsletter] Found {} active subscribers for voucher email", activeSubscribers.size());

        int total = activeSubscribers.size();
        int successCount = 0;
        int failureCount = 0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String endTimeStr = voucher.getNgayKetThuc().format(formatter);

        String valueStr = voucher.getGiaTri().stripTrailingZeros().toPlainString();
        String unit = "%".equals(voucher.getDonVi()) ? "%" : " VNĐ";

        for (int i = 0; i < total; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, total);
            List<NewsletterSubscriber> batch = activeSubscribers.subList(i, end);

            for (NewsletterSubscriber sub : batch) {
                try {
                    String htmlContent = buildVoucherHtml(voucher.getMaPhieu(), voucher.getTenPhieu(), valueStr + unit, voucher.getGiaTriDonHangToiThieu().stripTrailingZeros().toPlainString(), endTimeStr, sub.getTokenHuy());
                    sendHtmlMail(sub.getEmail(), "[Smash VN] Tặng bạn mã giảm giá " + voucher.getMaPhieu() + " - Giảm ngay " + valueStr + unit, htmlContent);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.warn("[Newsletter] Failed to send voucher email to recipient (token hidden): {}", e.getMessage());
                }
            }

            if (end < total) {
                try {
                    Thread.sleep(1500); // Backoff between batches
                } catch (InterruptedException e) {
                    log.warn("[Newsletter] Async voucher email thread interrupted during batch sleep");
                    Thread.currentThread().interrupt();
                    return; // Stop the task immediately
                }
            }
        }

        log.info("[Newsletter] Finished voucher email job. Total: {}, Success: {}, Failure: {}", total, successCount, failureCount);
    }

    private void sendWelcomeEmail(NewsletterSubscriber sub) {
        try {
            String htmlContent = buildWelcomeHtml(sub.getTokenHuy());
            sendHtmlMail(sub.getEmail(), "Cảm ơn bạn đã đăng ký nhận tin ưu đãi từ Smash VN!", htmlContent);
            log.info("[Newsletter] Welcome email sent successfully to: {}", sub.getEmail());
        } catch (Exception e) {
            log.warn("[Newsletter] Failed to send welcome email to: {}. Error: {}", sub.getEmail(), e.getMessage());
        }
    }

    private void sendHtmlMail(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    private String buildWelcomeHtml(String tokenHuy) {
        return "<html>"
                + "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
                + "  <div style='max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;'>"
                + "    <div style='background-color: #15171c; padding: 20px; text-align: center;'>"
                + "      <h2 style='color: #ff4500; margin: 0;'>SMASH VN</h2>"
                + "      <p style='color: #fff; margin: 5px 0 0 0;'>Cửa Hàng Vợt Cầu Lông Chính Hãng</p>"
                + "    </div>"
                + "    <div style='padding: 24px;'>"
                + "      <h3>Chào bạn,</h3>"
                + "      <p>Cảm ơn bạn đã đăng ký nhận thông tin ưu đãi từ Smash VN. Chúng tôi sẽ gửi tới bạn những thông tin mới nhất về các chương trình khuyến mãi, voucher giảm giá và các sản phẩm cầu lông hot nhất!</p>"
                + "      <div style='text-align: center; margin: 30px 0;'>"
                + "        <a href='" + BASE_URL + "/shop' style='background-color: #ff4500; color: #fff; text-decoration: none; padding: 12px 24px; font-weight: bold; border-radius: 4px; display: inline-block;'>Ghé Cửa Hàng Ngay</a>"
                + "      </div>"
                + "      <p>Chúc bạn có những trải nghiệm mua sắm tuyệt vời tại Smash VN!</p>"
                + "    </div>"
                + "    <div style='background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #777; border-top: 1px solid #eee;'>"
                + "      <p style='margin: 0;'>Hệ thống Cửa hàng Smash VN &copy; 2026</p>"
                + "      <p style='margin: 5px 0 0 0;'>Nếu bạn không muốn nhận email ưu đãi từ chúng tôi, vui lòng "
                + "         <a href='" + BASE_URL + "/api/newsletter/unsubscribe?token=" + tokenHuy + "' style='color: #ff4500; text-decoration: underline;'>Hủy nhận tin tại đây</a>."
                + "      </p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }

    private String buildPromotionHtml(String name, Integer percent, String start, String end, String productListHtml, String tokenHuy) {
        return "<html>"
                + "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
                + "  <div style='max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;'>"
                + "    <div style='background-color: #15171c; padding: 20px; text-align: center;'>"
                + "      <h2 style='color: #ff4500; margin: 0;'>SMASH VN</h2>"
                + "      <p style='color: #fff; margin: 5px 0 0 0;'>Cửa Hàng Vợt Cầu Lông Chính Hãng</p>"
                + "    </div>"
                + "    <div style='padding: 24px;'>"
                + "      <h3 style='color: #ff4500;'>SIÊU KHUYẾN MÃI: " + name + "</h3>"
                + "      <p>Smash VN xin gửi tới bạn chương trình ưu đãi đặc biệt giảm giá lên tới <strong>" + percent + "%</strong> cho các sản phẩm vợt cầu lông và phụ kiện chính hãng.</p>"
                + "      <div style='background-color: #fff5f2; border-left: 4px solid #ff4500; padding: 12px 15px; margin: 15px 0;'>"
                + "        <p style='margin: 0;'><strong>Thời gian áp dụng:</strong> Từ " + start + " đến " + end + "</p>"
                + "      </div>"
                + "      " + productListHtml
                + "      <div style='text-align: center; margin: 30px 0;'>"
                + "        <a href='" + BASE_URL + "/shop' style='background-color: #ff4500; color: #fff; text-decoration: none; padding: 12px 24px; font-weight: bold; border-radius: 4px; display: inline-block;'>Săn Sale Ngay</a>"
                + "      </div>"
                + "    </div>"
                + "    <div style='background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #777; border-top: 1px solid #eee;'>"
                + "      <p style='margin: 0;'>Hệ thống Cửa hàng Smash VN &copy; 2026</p>"
                + "      <p style='margin: 5px 0 0 0;'>Nếu bạn không muốn nhận email ưu đãi từ chúng tôi, vui lòng "
                + "         <a href='" + BASE_URL + "/api/newsletter/unsubscribe?token=" + tokenHuy + "' style='color: #ff4500; text-decoration: underline;'>Hủy nhận tin tại đây</a>."
                + "      </p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }

    private String buildVoucherHtml(String code, String name, String value, String minOrder, String expiry, String tokenHuy) {
        String nameText = name != null ? name : "Voucher mua sắm";
        return "<html>"
                + "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>"
                + "  <div style='max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden;'>"
                + "    <div style='background-color: #15171c; padding: 20px; text-align: center;'>"
                + "      <h2 style='color: #ff4500; margin: 0;'>SMASH VN</h2>"
                + "      <p style='color: #fff; margin: 5px 0 0 0;'>Cửa Hàng Vợt Cầu Lông Chính Hãng</p>"
                + "    </div>"
                + "    <div style='padding: 24px;'>"
                + "      <h3 style='color: #ff4500;'>TẶNG BẠN MÃ GIẢM GIÁ: " + nameText + "</h3>"
                + "      <p>Cơ hội mua sắm giá hời! Smash VN gửi tặng bạn mã giảm giá đặc quyền dùng để thanh toán đơn hàng:</p>"
                + "      <div style='text-align: center; margin: 25px 0;'>"
                + "        <div style='display: inline-block; border: 2px dashed #ff4500; padding: 15px 30px; background-color: #fffaf9; border-radius: 8px;'>"
                + "          <span style='font-size: 14px; color: #777; display: block;'>MÃ VOUCHER</span>"
                + "          <strong style='font-size: 26px; color: #ff4500; letter-spacing: 2px;'>" + code + "</strong>"
                + "          <span style='font-size: 16px; color: #333; display: block; margin-top: 5px;'>Giảm ngay: <strong>" + value + "</strong></span>"
                + "        </div>"
                + "      </div>"
                + "      <div style='background-color: #f9f9f9; padding: 12px 15px; border-radius: 4px; margin-bottom: 20px; font-size: 14px; color: #555;'>"
                + "        <p style='margin: 0 0 5px 0;'>&bull; Áp dụng cho đơn hàng tối thiểu: <strong>" + minOrder + " VNĐ</strong></p>"
                + "        <p style='margin: 0;'>&bull; Hạn sử dụng đến: <strong>" + expiry + "</strong></p>"
                + "      </div>"
                + "      <div style='text-align: center; margin: 30px 0;'>"
                + "        <a href='" + BASE_URL + "/shop' style='background-color: #ff4500; color: #fff; text-decoration: none; padding: 12px 24px; font-weight: bold; border-radius: 4px; display: inline-block;'>Áp Dụng Mã Ngay</a>"
                + "      </div>"
                + "    </div>"
                + "    <div style='background-color: #f9fafb; padding: 15px; text-align: center; font-size: 12px; color: #777; border-top: 1px solid #eee;'>"
                + "      <p style='margin: 0;'>Hệ thống Cửa hàng Smash VN &copy; 2026</p>"
                + "      <p style='margin: 5px 0 0 0;'>Nếu bạn không muốn nhận email ưu đãi từ chúng tôi, vui lòng "
                + "         <a href='" + BASE_URL + "/api/newsletter/unsubscribe?token=" + tokenHuy + "' style='color: #ff4500; text-decoration: underline;'>Hủy nhận tin tại đây</a>."
                + "      </p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }
}
