package com.smashvn.shop.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity đại diện cho một ĐỢT GIẢM GIÁ (Campaign) trong hệ thống.
 *
 * <p>Mỗi đợt giảm giá được áp dụng trực tiếp lên SẢN PHẨM (không phải đơn hàng),
 * làm giảm giá niêm yết của sản phẩm theo một tỷ lệ phần trăm cố định.
 * Khác với PhieuGiamGia (voucher) mà khách hàng nhập mã, đợt giảm giá
 * được hiển thị tự động trên trang sản phẩm khi đang trong thời gian hiệu lực.</p>
 *
 * <p>Quan hệ Many-to-Many với SanPham thông qua bảng trung gian {@code SanPham_DotGiamGia}.</p>
 *
 * <p>Trường {@code active} kết hợp với thời gian hiện tại xác định trạng thái thực tế
 * thông qua method {@link #getDynamicStatus()}.</p>
 */
@Entity
@Table(name = "DotGiamGia")
@Data
public class DotGiamGia {

    /** Khóa chính, tự tăng. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Tên chiến dịch giảm giá, ví dụ: "Flash Sale Hè 2025". Độ dài 2–100 ký tự. */
    @Column(name = "ten_chien_dich", nullable = false)
    private String tenChienDich;

    /** Thời điểm bắt đầu áp dụng đợt giảm giá. Bắt buộc nhập, phải trước ngày kết thúc. */
    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    /** Thời điểm kết thúc đợt giảm giá. Bắt buộc nhập. */
    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    /**
     * Tỷ lệ phần trăm giảm giá áp dụng lên giá niêm yết của sản phẩm.
     * Hợp lệ: 1% đến {@code PromotionValidationConstants.MAX_CAMPAIGN_DISCOUNT_PERCENT}%.
     * Ví dụ: phanTramGiam = 20 → giá sản phẩm 500.000đ còn 400.000đ.
     */
    @Column(name = "phan_tram_giam")
    private Integer phanTramGiam;

    @Column(name = "gia_tri_giam")
    private java.math.BigDecimal giaTriGiam;

    @Column(name = "gia_tu")
    private java.math.BigDecimal giaTu;

    @Column(name = "gia_den")
    private java.math.BigDecimal giaDen;

    /**
     * Loại hình giảm giá. Hiện tại hệ thống chỉ hỗ trợ hai giá trị:
     * <ul>
     *   <li>{@code "Theo Phần Trăm"}: giảm thẳng theo % trên giá niêm yết.</li>
     *   <li>{@code "Theo Khoảng"}: về mặt giao diện có thể dùng để phân loại,
     *       nhưng logic tính giá vẫn dùng trường {@code phanTramGiam}.</li>
     * </ul>
     */
    @Column(name = "loai_giam_gia", nullable = false, length = 100)
    private String loaiGiamGia;

    /**
     * Nhân viên đã tạo / cập nhật đợt giảm giá này.
     * Dùng để tra cứu lịch sử chỉnh sửa.
     */
    @ManyToOne
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;

    /**
     * Cờ bật/tắt thủ công (soft-delete).
     * {@code true}  → đợt giảm giá còn hiệu lực (nếu cũng trong thời gian ngày bắt đầu–kết thúc).
     * {@code false} → đã bị vô hiệu hóa hoặc xóa logic, không xuất hiện trên trang sản phẩm.
     */
    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    /**
     * Danh sách sản phẩm được áp dụng đợt giảm giá này.
     * Quan hệ Many-to-Many, dữ liệu lưu tại bảng trung gian {@code SanPham_DotGiamGia}.
     * Tải LAZY để tránh query thừa khi chỉ cần thông tin cơ bản của đợt giảm giá.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "SanPham_DotGiamGia",
        joinColumns = @JoinColumn(name = "id_dot_giam_gia"),
        inverseJoinColumns = @JoinColumn(name = "id_san_pham")
    )
    @ToString.Exclude        // Tránh vòng lặp vô hạn khi gọi toString()
    @EqualsAndHashCode.Exclude // Tránh vòng lặp khi so sánh equals/hashCode
    private java.util.Set<SanPham> sanPhams = new java.util.HashSet<>();

    /**
     * Trả về giá trị {@code trangThai}, mặc định là {@code true} nếu cột chưa có dữ liệu (NULL).
     */
    public Boolean getActive() {
        return trangThai == null ? true : trangThai;
    }

    public void setActive(Boolean active) {
        this.trangThai = active;
    }

    /**
     * Tính trạng thái thực tế của đợt giảm giá tại thời điểm hiện tại.
     * Được dùng trên giao diện admin và logic lọc trong {@code PricingServiceImpl}.
     *
     * <ul>
     *   <li>{@code "INACTIVE"} – đã bị vô hiệu hóa thủ công ({@code trangThai = false}).</li>
     *   <li>{@code "UPCOMING"} – chưa đến ngày bắt đầu.</li>
     *   <li>{@code "EXPIRED"}  – đã qua ngày kết thúc.</li>
     *   <li>{@code "ACTIVE"}   – đang trong thời gian hiệu lực và chưa bị tắt.</li>
     * </ul>
     *
     * @return chuỗi trạng thái động, không lưu vào cơ sở dữ liệu.
     */
    public String getDynamicStatus() {
        if (trangThai != null && !trangThai) {
            return "INACTIVE";
        }
        LocalDateTime now = LocalDateTime.now();
        if (ngayBatDau != null && now.isBefore(ngayBatDau)) {
            return "UPCOMING";
        } else if (ngayKetThuc != null && now.isAfter(ngayKetThuc)) {
            return "EXPIRED";
        } else {
            return "ACTIVE";
        }
    }
}
