package com.smashvn.shop.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressDto {
    private Integer id;

    @NotBlank(message = "Họ người nhận không được để trống!")
    @Size(max = 50, message = "Họ người nhận không được vượt quá 50 ký tự!")
    private String hoNguoiNhan;

    @NotBlank(message = "Tên người nhận không được để trống!")
    @Size(max = 50, message = "Tên người nhận không được vượt quá 50 ký tự!")
    private String tenNguoiNhan;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không đúng định dạng!")
    private String sdtNguoiNhan;

    @NotBlank(message = "Địa chỉ cụ thể không được để trống!")
    @Size(min = 5, max = 255, message = "Địa chỉ cụ thể phải từ 5 đến 255 ký tự!")
    private String diaChiCuThe;

    // Giữ lại để tương thích code cũ; form mới không nhận tên địa giới từ người dùng.
    @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự!")
    private String tinhThanh;

    // Backend luôn tự gán Việt Nam.
    @Size(max = 100, message = "Quốc gia không được vượt quá 100 ký tự!")
    private String quocGia;

    /**
     * Mã hành chính do GHN cấp. Tọa độ từ Leaflet chỉ dùng để hỗ trợ điền địa
     * chỉ hiển thị, không thể thay thế các mã này khi tạo vận đơn.
     */
    @NotNull(message = "Vui lòng chọn Tỉnh/Thành phố!")
    @Positive(message = "Tỉnh/Thành phố đã chọn không hợp lệ!")
    private Integer ghnProvinceId;

    @NotNull(message = "Vui lòng chọn Quận/Huyện!")
    @Positive(message = "Quận/Huyện đã chọn không hợp lệ!")
    private Integer ghnDistrictId;

    @NotBlank(message = "Vui lòng chọn Phường/Xã!")
    @Size(max = 50, message = "Mã Phường/Xã không hợp lệ!")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Phường/Xã đã chọn không hợp lệ!")
    private String ghnWardCode;

    // Các tên này được backend resolve lại từ ba mã khi lưu.
    @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự!")
    private String quanHuyen;

    @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự!")
    private String phuongXa;

    @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ!")
    @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ!")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ!")
    @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ!")
    private Double longitude;
    private boolean defaultAddress;
}
