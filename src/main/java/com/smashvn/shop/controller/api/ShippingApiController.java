package com.smashvn.shop.controller.api;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.ShippingZone;
import com.smashvn.shop.service.admin.AdminShippingService;
import com.smashvn.shop.service.api.ShippingFeeCalculator;
import com.smashvn.shop.service.api.ShippingZoneResolver;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@Slf4j
public class ShippingApiController {

    private final AdminShippingService adminShippingService;
    private final ShippingFeeCalculator feeCalculator;
    private final ShippingZoneResolver zoneResolver;

    @GetMapping("/fee")
    public ResponseEntity<Map<String, Object>> getShippingFee(
            @RequestParam(value = "carrierId", required = false) Integer carrierId,
            @RequestParam(value = "districtId", required = false) Integer districtId,
            @RequestParam(value = "wardCode", required = false) String wardCode,
            @RequestParam(value = "address", required = false) String address) {

        log.debug("Received request to calculate shipping fee: carrierId={}, districtId={}, wardCode={}, address={}", carrierId, districtId, wardCode, address);

        DonViVanChuyen carrier = null;
        if (carrierId != null) {
            try {
                carrier = adminShippingService.getAllCarriers().stream()
                        .filter(c -> c.getId().equals(carrierId))
                        .findFirst()
                        .orElse(null);
            } catch (Exception e) {
                log.warn("Invalid or errored carrierId: {}", carrierId, e);
            }
        }
        if (carrier == null) {
            carrier = adminShippingService.getAllCarriers().stream()
                    .filter(c -> DonViVanChuyen.isGhnCarrier(c))
                    .findFirst()
                    .orElse(null);
        }

        BigDecimal fee;
        ShippingZone zone;
        String carrierCode;

        try {
            fee = feeCalculator.calculateFee(carrier, districtId, wardCode, address);
            zone = zoneResolver.resolveZone(districtId, address);
            carrierCode = feeCalculator.getCarrierCode(carrier);
        } catch (Exception e) {
            log.error("Error calculating shipping fee, falling back to safe default", e);
            fee = ShippingFeeCalculator.DEFAULT_NATIONWIDE_FALLBACK;
            zone = ShippingZone.NATIONWIDE;
            carrierCode = "DEFAULT";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("shippingFee", fee);
        response.put("zone", zone.name());
        response.put("carrier", carrierCode);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/carriers")
    public ResponseEntity<java.util.List<DonViVanChuyen>> getCarriers() {
        return ResponseEntity.ok(adminShippingService.getAllCarriers());
    }
}
