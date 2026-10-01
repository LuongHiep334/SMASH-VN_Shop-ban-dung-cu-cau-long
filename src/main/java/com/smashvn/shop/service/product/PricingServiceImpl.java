package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DotGiamGia;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Triển khai {@link PricingService} – tính giá bán thực tế cho từng biến thể sản phẩm.
 *
 * <p>Class này chịu trách nhiệm duy nhất: xác định giá sản phẩm có đang được
 * áp dụng ĐỢT GIẢM GIÁ ({@link DotGiamGia}) không, và nếu có thì trả về
 * giá sau giảm cùng đầy đủ thông tin để hiển thị trên trang sản phẩm.</p>
 *
 * <p><b>Lưu ý quan trọng:</b> Class này KHÔNG xử lý PHIẾU GIẢM GIÁ (Voucher).
 * Voucher chỉ áp dụng tại bước thanh toán và được tính bởi {@code VoucherCalculator}.</p>
 */
@Service("pricingService")
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {


    /**
     * Tính giá bán hiện tại của một biến thể sản phẩm ({@link SanPhamChiTiet}).
     * Trả về giá sau giảm nếu sản phẩm đang có đợt giảm giá ACTIVE,
     * hoặc giá niêm yết nếu không có khuyến mãi.
     *
     * @param spct biến thể sản phẩm cần tính giá.
     * @return giá bán hiện tại (≥ 0), hoặc {@code 0} nếu {@code spct} là null.
     */
    @Override
    public BigDecimal calculateCurrentSellingPrice(SanPhamChiTiet spct) {
        if (spct == null) {
            return BigDecimal.ZERO;
        }
        // Ủy quyền toàn bộ logic cho buildPriceSnapshot và lấy giá sau giảm
        return buildPriceSnapshot(spct).giaBanSauGiam();
    }

    /**
     * Xây dựng đầy đủ snapshot giá của một biến thể sản phẩm tại thời điểm hiện tại.
     *
     * <p>Snapshot bao gồm: giá niêm yết, giá sau giảm, % giảm, số tiền giảm,
     * tên chiến dịch và ID chiến dịch. Được dùng để hiển thị thẻ giá trên trang
     * danh sách sản phẩm, trang chi tiết, và để lưu vào {@code HoaDonChiTiet}.</p>
     *
     * <p>Luồng xử lý:</p>
     * <ol>
     *   <li>Lấy giá niêm yết từ {@code SanPhamChiTiet.giaBan}.</li>
     *   <li>Kiểm tra sản phẩm cha ({@link SanPham}) có đợt giảm giá nào không.</li>
     *   <li>Lọc lấy đợt giảm giá đang ACTIVE (cờ active=true VÀ trong thời gian hiệu lực).</li>
     *   <li>Nếu không có đợt nào active → trả snapshot với giá bằng niêm yết, không giảm.</li>
     *   <li>Nếu có → tính số tiền giảm = giaNiemYet × phanTramGiam / 100, làm tròn HALF_UP.</li>
     * </ol>
     *
     * @param spct biến thể sản phẩm cần tạo snapshot giá.
     * @return {@link PriceSnapshot} chứa đầy đủ thông tin giá.
     */
    @Override
    public PriceSnapshot buildPriceSnapshot(SanPhamChiTiet spct) {
        BigDecimal giaNiemYet = spct.getGiaBan() != null ? spct.getGiaBan() : BigDecimal.ZERO;

        SanPham sp = spct.getSanPham();
        // Sản phẩm không có đợt giảm giá nào → trả về giá nguyên, không giảm
        if (sp == null || sp.getCacDotGiamGia() == null || sp.getCacDotGiamGia().isEmpty()) {
            return new PriceSnapshot(giaNiemYet, giaNiemYet, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        }

        // Tìm đợt giảm giá duy nhất đang ACTIVE (cờ active=true và trong thời gian hiệu lực)
        // Dùng findFirst() vì một sản phẩm không được phép có 2 đợt giảm giá chồng nhau
        Optional<DotGiamGia> activeDggOpt = sp.getCacDotGiamGia().stream()
            .filter(dgg -> Boolean.TRUE.equals(dgg.getActive()) && "ACTIVE".equals(dgg.getDynamicStatus()))
            .findFirst();

        // Không có đợt nào đang active → trả về giá niêm yết
        if (activeDggOpt.isEmpty()) {
            return new PriceSnapshot(giaNiemYet, giaNiemYet, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
        }

        DotGiamGia dgg = activeDggOpt.get();
        BigDecimal phanTramGiam = BigDecimal.valueOf(dgg.getPhanTramGiam());

        // Số tiền giảm cụ thể = gia_niem_yet × phan_tram_giam / 100, làm tròn HALF_UP 2 chữ số
        BigDecimal soTienGiam = giaNiemYet.multiply(phanTramGiam)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // Giá bán sau giảm, không được nhỏ hơn 0
        BigDecimal giaBanSauGiam = giaNiemYet.subtract(soTienGiam);
        if (giaBanSauGiam.compareTo(BigDecimal.ZERO) < 0) {
            giaBanSauGiam = BigDecimal.ZERO;
        }

        return new PriceSnapshot(
            giaNiemYet,
            giaBanSauGiam.setScale(2, RoundingMode.HALF_UP),
            phanTramGiam.setScale(2, RoundingMode.HALF_UP),
            soTienGiam.setScale(2, RoundingMode.HALF_UP),
            dgg.getTenChienDich(), // Tên chiến dịch để hiển thị nhãn "Flash Sale..."
            dgg.getId()            // ID chiến dịch để lưu vào HoaDonChiTiet
        );
    }

    /**
     * Tính tổng tiền của một dòng sản phẩm trong giỏ hàng hoặc đơn hàng.
     * Công thức: giá bán hiện tại × số lượng.
     *
     * @param spct     biến thể sản phẩm.
     * @param quantity số lượng (phải > 0).
     * @return tổng tiền dòng sản phẩm, hoặc {@code 0} nếu đầu vào không hợp lệ.
     */
    @Override
    public BigDecimal calculateLineTotal(SanPhamChiTiet spct, Integer quantity) {
        if (spct == null || quantity == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal price = calculateCurrentSellingPrice(spct);
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
