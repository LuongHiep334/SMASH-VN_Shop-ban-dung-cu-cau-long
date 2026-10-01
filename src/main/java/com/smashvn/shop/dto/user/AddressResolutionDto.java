package com.smashvn.shop.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kết quả chuyển tọa độ bản đồ sang địa giới hành chính dùng khi giao hàng.
 * Tên nhà cung cấp dữ liệu và các thông tin xác thực không được đưa ra frontend.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResolutionDto {

    public enum ResolutionLevel {
        NONE,
        PROVINCE,
        DISTRICT,
        WARD
    }

    private boolean success;
    private ResolutionLevel resolutionLevel;
    private boolean manualSelectionRequired;
    private String message;
    private Integer provinceId;
    private String provinceName;
    private Integer districtId;
    private String districtName;
    private String wardCode;
    private String wardName;
    private String addressDetail;
    private Double latitude;
    private Double longitude;
}
