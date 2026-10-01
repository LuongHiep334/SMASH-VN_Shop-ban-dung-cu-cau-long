package com.smashvn.shop.service.api;

import com.smashvn.shop.entity.ShippingZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingZoneResolver {

    private final GhnService ghnService;

    @Value("${shipping.local-province-ids:201,244}")
    private List<Integer> localProvinceIds;

    @Value("${shipping.local-provinces:ha noi,thai nguyen}")
    private List<String> localProvinceNames;

    private Set<Integer> localDistrictIds = null;

    private synchronized void initLocalDistricts() {
        if (localDistrictIds != null) {
            return;
        }
        localDistrictIds = new HashSet<>();
        if (localProvinceIds != null) {
            for (Integer provinceId : localProvinceIds) {
                try {
                    List<Map<String, Object>> districts = ghnService.getDistricts(provinceId);
                    if (districts != null) {
                        for (Map<String, Object> district : districts) {
                            Object idObj = district.get("DistrictID");
                            if (idObj != null) {
                                localDistrictIds.add(Integer.valueOf(idObj.toString()));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to load districts for local province ID: {}", provinceId, e);
                }
            }
        }
        log.info("Initialized local district IDs cache with {} items: {}", localDistrictIds.size(), localDistrictIds);
    }

    public ShippingZone resolveZone(Integer districtId, String address) {
        if (districtId != null) {
            initLocalDistricts();
            if (localDistrictIds.contains(districtId)) {
                return ShippingZone.LOCAL;
            }
            return ShippingZone.NATIONWIDE;
        }
        return resolveZone(address);
    }

    public ShippingZone resolveZone(String address) {
        if (address == null || address.trim().isEmpty()) {
            return ShippingZone.NATIONWIDE;
        }

        String normalized = normalizeAddress(address);
        if (localProvinceNames != null) {
            for (String province : localProvinceNames) {
                if (normalized.contains(normalizeAddress(province))) {
                    return ShippingZone.LOCAL;
                }
            }
        }
        return ShippingZone.NATIONWIDE;
    }

    public String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }

        // Convert to lowercase
        String normalized = address.toLowerCase();

        // Replace hyphens, commas, periods, colons, underscores, and question marks with spaces
        normalized = normalized.replaceAll("[\\-\\.,;:_\\?]", " ");

        // Remove Vietnamese accents / diacritics
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        Pattern diacriticsPattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        normalized = diacriticsPattern.matcher(normalized).replaceAll("");

        // Replace specific characters (like đ -> d)
        normalized = normalized.replace("đ", "d");

        // Trim multiple spaces into a single space
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }
}

