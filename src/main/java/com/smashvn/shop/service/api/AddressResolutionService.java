package com.smashvn.shop.service.api;

import org.springframework.stereotype.Service;

import com.smashvn.shop.dto.user.AddressResolutionDto;
import com.smashvn.shop.dto.user.AddressResolutionDto.ResolutionLevel;
import com.smashvn.shop.service.api.GhnService.GhnAddressDetails;
import com.smashvn.shop.service.api.LocationService.ReverseGeocodedAddress;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressResolutionService {

    private final LocationService locationService;
    private final GhnService ghnService;

    public AddressResolutionDto resolve(double latitude, double longitude) {
        ReverseGeocodedAddress geocoded = locationService.reverseGeocode(latitude, longitude);

        if (!"vn".equalsIgnoreCase(geocoded.countryCode())) {
            return AddressResolutionDto.builder()
                    .success(false)
                    .resolutionLevel(ResolutionLevel.NONE)
                    .manualSelectionRequired(true)
                    .message("Vị trí hiện tại nằm ngoài khu vực giao hàng tại Việt Nam. Vui lòng chọn địa chỉ bên dưới.")
                    .addressDetail(geocoded.addressDetail())
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
        }

        GhnAddressDetails matched;
        try {
            matched = ghnService.resolveAdministrativeAddress(
                    geocoded.provinceCandidates(),
                    geocoded.districtCandidates(),
                    geocoded.wardCandidates());
        } catch (Exception exception) {
            log.warn("Unable to match reverse-geocoded address with delivery areas: {}",
                    exception.getMessage());
            return AddressResolutionDto.builder()
                    .success(false)
                    .resolutionLevel(ResolutionLevel.NONE)
                    .manualSelectionRequired(true)
                    .message("Không thể xác định chính xác khu vực. Vui lòng chọn địa chỉ bên dưới.")
                    .addressDetail(geocoded.addressDetail())
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
        }

        ResolutionLevel resolutionLevel = resolutionLevelOf(matched);
        boolean fullyMatched = resolutionLevel == ResolutionLevel.WARD;

        return AddressResolutionDto.builder()
                .success(fullyMatched)
                .resolutionLevel(resolutionLevel)
                .manualSelectionRequired(!fullyMatched)
                .message(messageFor(resolutionLevel))
                .provinceId(matched.getProvinceId())
                .provinceName(matched.getProvinceName())
                .districtId(matched.getDistrictId())
                .districtName(matched.getDistrictName())
                .wardCode(matched.getWardCode())
                .wardName(matched.getWardName())
                .addressDetail(geocoded.addressDetail())
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    private ResolutionLevel resolutionLevelOf(GhnAddressDetails matched) {
        if (matched == null || matched.getProvinceId() == null) {
            return ResolutionLevel.NONE;
        }
        if (matched.getDistrictId() == null) {
            return ResolutionLevel.PROVINCE;
        }
        if (matched.getWardCode() == null || matched.getWardCode().isBlank()) {
            return ResolutionLevel.DISTRICT;
        }
        return ResolutionLevel.WARD;
    }

    private String messageFor(ResolutionLevel resolutionLevel) {
        return switch (resolutionLevel) {
            case WARD -> "Đã xác định khu vực giao hàng. Bạn có thể kiểm tra và điều chỉnh nếu cần.";
            case DISTRICT -> "Đã xác định Tỉnh/Thành phố và Quận/Huyện. Vui lòng chọn Phường/Xã.";
            case PROVINCE -> "Đã xác định Tỉnh/Thành phố. Vui lòng chọn Quận/Huyện và Phường/Xã.";
            case NONE -> "Không thể xác định chính xác khu vực. Vui lòng chọn địa chỉ bên dưới.";
        };
    }
}
