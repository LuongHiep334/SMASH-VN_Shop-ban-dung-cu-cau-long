package com.smashvn.shop.service.api;

import com.smashvn.shop.entity.DonViVanChuyen;
import com.smashvn.shop.entity.ShippingZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingFeeCalculator {

    private final ShippingZoneResolver zoneResolver;
    private final GhnService ghnService;

    @Value("${shipping.ghtk.local:22000}")
    private BigDecimal ghtkLocal;

    @Value("${shipping.ghtk.nationwide:30000}")
    private BigDecimal ghtkNationwide;

    @Value("${shipping.ghn.local:25000}")
    private BigDecimal ghnLocal;

    @Value("${shipping.ghn.nationwide:38000}")
    private BigDecimal ghnNationwide;

    @Value("${shipping.default.local:30000}")
    private BigDecimal defaultLocal;

    @Value("${shipping.default.nationwide:30000}")
    private BigDecimal defaultNationwide;

    // Legacy Fallback Constants for external reference or baseline
    public static final BigDecimal GHTK_LOCAL_FALLBACK = BigDecimal.valueOf(22000);
    public static final BigDecimal GHTK_NATIONWIDE_FALLBACK = BigDecimal.valueOf(30000);
    public static final BigDecimal GHN_LOCAL_FALLBACK = BigDecimal.valueOf(25000);
    public static final BigDecimal GHN_NATIONWIDE_FALLBACK = BigDecimal.valueOf(38000);
    public static final BigDecimal DEFAULT_LOCAL_FALLBACK = BigDecimal.valueOf(30000);
    public static final BigDecimal DEFAULT_NATIONWIDE_FALLBACK = BigDecimal.valueOf(30000);

    public BigDecimal calculateFee(DonViVanChuyen carrier, Integer districtId, String wardCode, String address) {
        String carrierCode = getCarrierCode(carrier);
        if ("GHN".equals(carrierCode) && districtId != null && wardCode != null && !wardCode.trim().isEmpty()) {
            BigDecimal fee = ghnService.calculateShipFee(districtId, wardCode.trim(), 1000000);
            if (fee != null) {
                log.debug("Calculated real-time GHN shipping fee using API: {}", fee);
                return fee;
            }
        }
        ShippingZone zone = zoneResolver.resolveZone(districtId, address);
        return calculateFee(carrier, zone);
    }

    public BigDecimal calculateFee(DonViVanChuyen carrier, Integer districtId, String address) {
        return calculateFee(carrier, districtId, null, address);
    }

    public BigDecimal calculateFee(DonViVanChuyen carrier, String address) {
        if (address == null || address.trim().isEmpty()) {
            log.debug("Address is null or empty. Resolving to NATIONWIDE zone and calculating fee.");
            return calculateFee(carrier, ShippingZone.NATIONWIDE);
        }
        ShippingZone zone = zoneResolver.resolveZone(address);
        return calculateFee(carrier, zone);
    }


    public BigDecimal calculateFee(DonViVanChuyen carrier, ShippingZone zone) {
        if (carrier == null) {
            return (zone == ShippingZone.LOCAL) ? defaultLocal : defaultNationwide;
        }

        // 1. Priority 1: Database value
        BigDecimal dbFee = (zone == ShippingZone.LOCAL) ? carrier.getPhiLocal() : carrier.getPhiNationwide();
        if (dbFee != null) {
            log.debug("Resolved carrier fee from database for carrier: {}, zone: {}, fee: {}", 
                    carrier.getTenDonVi(), zone, dbFee);
            return dbFee;
        }

        // 2. Priority 2 & 3: Application config & fallback constants
        String carrierCode = getCarrierCode(carrier);
        ShippingZone finalZone = zone != null ? zone : ShippingZone.NATIONWIDE;
        
        BigDecimal fee;
        if ("GHTK".equals(carrierCode)) {
            fee = (finalZone == ShippingZone.LOCAL) ? ghtkLocal : ghtkNationwide;
        } else if ("GHN".equals(carrierCode)) {
            fee = (finalZone == ShippingZone.LOCAL) ? ghnLocal : ghnNationwide;
        } else {
            fee = (finalZone == ShippingZone.LOCAL) ? defaultLocal : defaultNationwide;
        }

        log.debug("Resolved carrier: {}, resolved zone: {}, calculated fallback fee: {}", 
                carrierCode, finalZone, fee);
        return fee;
    }

    public String getCarrierCode(DonViVanChuyen carrier) {
        if (carrier == null) {
            return "DEFAULT";
        }

        if (DonViVanChuyen.isGhnCarrier(carrier)) {
            return "GHN";
        }

        if (carrier.getMaDonVi() != null && carrier.getMaDonVi().trim().equalsIgnoreCase("GHTK")) {
            return "GHTK";
        }

        if (carrier.getTenDonVi() != null) {
            String name = zoneResolver.normalizeAddress(carrier.getTenDonVi()).replace(" ", "");
            if (name.contains("ghtk") || name.contains("tietkiem")) {
                return "GHTK";
            }
        }

        return "DEFAULT";
    }
}
