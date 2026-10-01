package com.smashvn.shop.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smashvn.shop.dto.user.AddressResolutionDto;
import com.smashvn.shop.dto.user.AddressResolutionDto.ResolutionLevel;
import com.smashvn.shop.service.api.AddressResolutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@Slf4j
public class AddressRestController {

    private final AddressResolutionService addressResolutionService;

    @GetMapping("/resolve")
    public ResponseEntity<AddressResolutionDto> resolve(
            @RequestParam double lat,
            @RequestParam double lng) {
        if (!isValidCoordinate(lat, lng)) {
            return ResponseEntity.badRequest().body(AddressResolutionDto.builder()
                    .success(false)
                    .resolutionLevel(ResolutionLevel.NONE)
                    .manualSelectionRequired(true)
                    .message("Tọa độ không hợp lệ. Vui lòng chọn địa chỉ bên dưới.")
                    .latitude(lat)
                    .longitude(lng)
                    .build());
        }

        try {
            return ResponseEntity.ok(addressResolutionService.resolve(lat, lng));
        } catch (Exception exception) {
            log.warn("Unable to resolve delivery area for coordinates lat={}, lng={}: {}",
                    lat, lng, exception.getMessage());
            return ResponseEntity.ok(AddressResolutionDto.builder()
                    .success(false)
                    .resolutionLevel(ResolutionLevel.NONE)
                    .manualSelectionRequired(true)
                    .message("Không thể xác định chính xác khu vực. Vui lòng chọn địa chỉ bên dưới.")
                    .latitude(lat)
                    .longitude(lng)
                    .build());
        }
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }
}
