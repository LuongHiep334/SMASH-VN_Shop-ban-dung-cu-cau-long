package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.ThuongHieuRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThuongHieuService {

    private static final Logger log = LoggerFactory.getLogger(ThuongHieuService.class);

    private final ThuongHieuRepository thuongHieuRepository;

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Thêm mới thương hiệu.
     * Service là nguồn xác thực duy nhất: sanitize → validate → kiểm tra trùng → lưu.
     */
    public ThuongHieu themThuongHieu(String tenThuongHieu) {
        String normalized = normalize(tenThuongHieu);
        validateLength(normalized);

        if (thuongHieuRepository.existsByTenThuongHieuIgnoreCase(normalized)) {
            log.warn("[BRAND] Duplicate brand add attempt detected.");
            throw new IllegalArgumentException(
                    "Thương hiệu \"" + normalized + "\" đã tồn tại. Vui lòng nhập tên khác!");
        }

        ThuongHieu th = new ThuongHieu();
        th.setTenThuongHieu(normalized);
        return thuongHieuRepository.save(th);
    }

    /**
     * Cập nhật thương hiệu theo ID.
     * Cho phép giữ nguyên tên hiện tại (self-exclude khi kiểm tra trùng).
     */
    public ThuongHieu suaThuongHieu(Integer id, String tenThuongHieu) {
        String normalized = normalize(tenThuongHieu);
        validateLength(normalized);

        if (thuongHieuRepository.existsByTenThuongHieuIgnoreCaseAndIdNot(normalized, id)) {
            log.warn("[BRAND] Duplicate brand edit attempt detected.");
            throw new IllegalArgumentException(
                    "Thương hiệu \"" + normalized + "\" đã tồn tại. Vui lòng nhập tên khác!");
        }

        ThuongHieu th = thuongHieuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu với ID: " + id));
        th.setTenThuongHieu(normalized);
        return thuongHieuRepository.save(th);
    }

    @org.springframework.transaction.annotation.Transactional
    public ThuongHieu anHoacHienThuongHieu(Integer id) {
        ThuongHieu th = thuongHieuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu với ID: " + id));
        boolean currentStatus = Boolean.TRUE.equals(th.getTrangThai());
        th.setTrangThai(!currentStatus);
        log.info("[BRAND] Toggled status for brand ID {}: {} -> {}", id, currentStatus, !currentStatus);
        return thuongHieuRepository.save(th);
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /**
     * Normalization pipeline (applied before ALL validation and duplicate checks):
     * 1. Jsoup.clean(Safelist.none()) — strip all HTML / XSS tags
     * 2. .trim()                       — remove leading/trailing whitespace
     * 3. .replaceAll("\\s+", " ")      — collapse multiple inner spaces to one
     *
     * Examples:
     *   "  NIKE  "  →  "NIKE"
     *   "<b>Nike</b>" →  "Nike"
     *   "Nike  Pro"  →  "Nike Pro"
     */
    String normalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống!");
        }
        String cleaned = Jsoup.clean(input, Safelist.none()).trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Tên thương hiệu không được để trống!");
        }
        return cleaned;
    }

    /** Validate 2–100 characters after normalization */
    private void validateLength(String normalized) {
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("Tên thương hiệu phải có ít nhất 2 ký tự!");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Tên thương hiệu không được vượt quá 100 ký tự!");
        }
    }
}
