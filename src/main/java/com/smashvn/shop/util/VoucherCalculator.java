package com.smashvn.shop.util;

import com.smashvn.shop.entity.PhieuGiamGia;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tiện ích tính toán giảm giá từ PHIẾU GIẢM GIÁ (Voucher).
 *
 * <p>Toàn bộ logic tính tiền giảm khi áp dụng voucher PHẢI đi qua class này,
 * bao gồm {@code CheckoutController}, {@code GioHangService} và {@code AdminPosService}.
 * Điều này đảm bảo kết quả nhất quán ở mọi luồng thanh toán (online, POS, API...).</p>
 *
 * <h3>Quy tắc tính giảm giá:</h3>
 * <ul>
 *   <li>Trả về {@code 0} nếu voucher null, tổng đơn null hoặc ≤ 0.</li>
 *   <li>Trả về {@code 0} nếu tổng đơn chưa đạt mức tối thiểu ({@code giaTriDonHangToiThieu}).</li>
 *   <li><b>Voucher %</b>: giảm = tổng đơn × tỷ lệ% / 100, làm tròn HALF_UP về đơn vị đồng.</li>
 *   <li><b>Voucher %</b> có giới hạn: nếu tính ra > {@code giaTriGiamToiDa} thì lấy cap đó.</li>
 *   <li><b>Voucher VND</b>: giảm đúng bằng {@code giaTri}, bỏ qua {@code giaTriGiamToiDa}.</li>
 *   <li>Kết quả luôn ≥ 0 và không bao giờ vượt quá tổng đơn hàng.</li>
 * </ul>
 */
public final class VoucherCalculator {

    /** Lớp tiện ích thuần túy – không cho phép khởi tạo đối tượng. */
    private VoucherCalculator() {
        // Utility class – no instantiation
    }

    /**
     * Tính số tiền được giảm từ {@code orderTotal} khi áp dụng voucher.
     *
     * <p>Ví dụ minh họa:</p>
     * <pre>
     * Voucher 20% – tối đa 100.000đ:
     *   Đơn 300.000đ → 300.000 × 20% = 60.000đ  (chưa chạm cap → giảm 60.000đ)
     *   Đơn 700.000đ → 700.000 × 20% = 140.000đ (vượt cap → chỉ giảm 100.000đ)
     *
     * Voucher 50.000đ VND:
     *   Đơn bất kỳ ≥ tối thiểu → giảm đúng 50.000đ
     * </pre>
     *
     * @param orderTotal tổng tiền hàng trước giảm (phải > 0)
     * @param voucher    phiếu giảm giá đang áp dụng (không được null)
     * @return số tiền được giảm, luôn ≥ 0 và ≤ orderTotal
     */
    public static BigDecimal calculateVoucherDiscount(BigDecimal orderTotal, PhieuGiamGia voucher) {
        // Đầu vào không hợp lệ → không giảm gì
        if (voucher == null || orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra đơn hàng có đạt giá trị tối thiểu để dùng voucher không
        BigDecimal minOrder = voucher.getGiaTriDonHangToiThieu();
        if (minOrder != null && orderTotal.compareTo(minOrder) < 0) {
            return BigDecimal.ZERO; // Đơn quá nhỏ, không đủ điều kiện dùng phiếu
        }

        BigDecimal discount;

        // Xác định loại voucher: phần trăm hay tiền mặt
        boolean isPercentage = "%".equals(voucher.getDonVi())
                || "Giảm phần trăm".equalsIgnoreCase(voucher.getLoaiGiamGia());

        if (isPercentage) {
            // Tính số tiền giảm theo %: làm tròn HALF_UP, không để số lẻ (VNĐ là đơn vị nguyên)
            discount = orderTotal
                    .multiply(voucher.getGiaTri())
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);

            // Áp giới hạn tối đa (nếu có) – chỉ áp dụng cho voucher phần trăm
            BigDecimal cap = voucher.getGiaTriGiamToiDa();
            if (cap != null && cap.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(cap) > 0) {
                discount = cap; // Cắt về mức trần
            }
        } else {
            // Voucher giảm tiền cố định – lấy nguyên giá trị, bỏ qua giaTriGiamToiDa
            discount = voucher.getGiaTri();
        }

        // Bảo vệ: không cho kết quả âm
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        // Bảo vệ: không giảm nhiều hơn tổng đơn (tránh tiền âm)
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }

        return discount;
    }
}
