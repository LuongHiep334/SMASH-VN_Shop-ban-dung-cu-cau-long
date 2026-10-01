package com.smashvn.shop.service.product;

import com.smashvn.shop.dto.product.AttributeFilterDTO;
import com.smashvn.shop.dto.product.AttributeOptionProjection;
import com.smashvn.shop.dto.product.AttributeValueDTO;
import com.smashvn.shop.dto.product.ShopFilterRequest;
import com.smashvn.shop.entity.DanhMucThuocTinh;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.repository.DanhMucThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamChiTietThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
import com.smashvn.shop.specification.SanPhamSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietThuocTinhRepository sanPhamChiTietThuocTinhRepository;
    private final DanhMucThuocTinhRepository danhMucThuocTinhRepository;
    private final ThuocTinhRepository thuocTinhRepository;

    @Transactional(readOnly = true)
    public Page<SanPham> filterProducts(ShopFilterRequest request, Pageable pageable) {
        ShopFilterRequest sanitizedRequest = sanitizeFilterRequest(request);
        return sanPhamRepository.findAll(SanPhamSpecification.filter(sanitizedRequest), pageable);
    }

    @Transactional(readOnly = true)
    public List<AttributeFilterDTO> getDynamicAttributeFilters(Integer categoryId, Map<Integer, List<String>> selectedAttributes, List<String> legacyTrongLuong) {
        if (categoryId == null) {
            return Collections.emptyList();
        }

        // Fetch active category attribute mappings
        List<DanhMucThuocTinh> categoryAttributes = danhMucThuocTinhRepository.findByDanhMucIdAndTrangThaiTrue(categoryId);
        if (categoryAttributes == null || categoryAttributes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> validAttrIds = categoryAttributes.stream()
                .filter(dmtt -> dmtt.getThuocTinh() != null && Boolean.TRUE.equals(dmtt.getThuocTinh().getTrangThai()))
                .map(dmtt -> dmtt.getThuocTinh().getId())
                .collect(Collectors.toSet());

        // Fetch projections containing distinct values & product count for this category
        List<AttributeOptionProjection> projections = sanPhamChiTietThuocTinhRepository.findAttributeOptionProjectionsByCategory(categoryId);

        // Group projections by thuocTinhId
        Map<Integer, List<AttributeOptionProjection>> projectionGroupMap = new LinkedHashMap<>();
        if (projections != null) {
            for (AttributeOptionProjection p : projections) {
                if (validAttrIds.contains(p.getThuocTinhId())) {
                    projectionGroupMap.computeIfAbsent(p.getThuocTinhId(), k -> new ArrayList<>()).add(p);
                }
            }
        }

        // Sanitize selected attributes lookup
        Map<Integer, Set<String>> selectedLookup = prepareSelectedLookup(selectedAttributes, legacyTrongLuong, validAttrIds);

        List<AttributeFilterDTO> result = new ArrayList<>();

        for (DanhMucThuocTinh dmtt : categoryAttributes) {
            if (dmtt.getThuocTinh() == null || !Boolean.TRUE.equals(dmtt.getThuocTinh().getTrangThai())) {
                continue;
            }

            Integer ttId = dmtt.getThuocTinh().getId();
            String ttName = dmtt.getThuocTinh().getTenThuocTinh();

            List<AttributeOptionProjection> groupProjections = projectionGroupMap.getOrDefault(ttId, Collections.emptyList());
            if (groupProjections.isEmpty()) {
                continue;
            }

            Set<String> selectedValues = selectedLookup.getOrDefault(ttId, Collections.emptySet());

            List<AttributeValueDTO> optionDtos = new ArrayList<>();
            for (AttributeOptionProjection p : groupProjections) {
                String val = p.getGiaTri();
                boolean isSelected = selectedValues.contains(val);
                optionDtos.add(AttributeValueDTO.builder()
                        .value(val)
                        .count(p.getProductCount() != null ? p.getProductCount() : 0L)
                        .selected(isSelected)
                        .build());
            }

            result.add(AttributeFilterDTO.builder()
                    .thuocTinhId(ttId)
                    .tenThuocTinh(ttName)
                    .options(optionDtos)
                    .build());
        }

        return result;
    }

    /**
     * Dynamically resolve attribute ID for "Trọng lượng" without hardcoded IDs.
     */
    public Optional<Integer> resolveTrongLuongAttributeId() {
        return thuocTinhRepository.findByTenThuocTinhIgnoreCaseAndTrangThaiTrue("Trọng lượng")
                .or(() -> thuocTinhRepository.findByTenThuocTinhContainingIgnoreCaseAndTrangThaiTrue("trọng").stream().findFirst())
                .map(ThuocTinh::getId);
    }

    /**
     * Sanitizes filter request against active category attributes.
     * Drops attribute IDs that do NOT belong to the selected category or are invalid.
     */
    public ShopFilterRequest sanitizeFilterRequest(ShopFilterRequest req) {
        if (req == null) return new ShopFilterRequest();

        Map<Integer, List<String>> sanitizedAttrs = new HashMap<>();

        if (req.getCategoryId() != null) {
            List<DanhMucThuocTinh> categoryAttributes = danhMucThuocTinhRepository.findByDanhMucIdAndTrangThaiTrue(req.getCategoryId());
            Set<Integer> validAttrIds = categoryAttributes.stream()
                    .filter(dmtt -> dmtt.getThuocTinh() != null && Boolean.TRUE.equals(dmtt.getThuocTinh().getTrangThai()))
                    .map(dmtt -> dmtt.getThuocTinh().getId())
                    .collect(Collectors.toSet());

            if (req.getAttributes() != null && !req.getAttributes().isEmpty()) {
                for (Map.Entry<Integer, List<String>> entry : req.getAttributes().entrySet()) {
                    Integer attrId = entry.getKey();
                    if (attrId != null && validAttrIds.contains(attrId) && entry.getValue() != null) {
                        List<String> cleanVals = entry.getValue().stream()
                                .filter(v -> v != null && !v.trim().isEmpty())
                                .map(String::trim)
                                .collect(Collectors.toList());
                        if (!cleanVals.isEmpty()) {
                            sanitizedAttrs.put(attrId, cleanVals);
                        }
                    }
                }
            }

            // Map legacy trongLuong if present & valid for category
            if (req.getLegacyTrongLuong() != null && !req.getLegacyTrongLuong().isEmpty()) {
                Optional<Integer> trongLuongIdOpt = resolveTrongLuongAttributeId();
                if (trongLuongIdOpt.isPresent()) {
                    Integer tlId = trongLuongIdOpt.get();
                    if (validAttrIds.contains(tlId)) {
                        List<String> cleanLegacy = req.getLegacyTrongLuong().stream()
                                .filter(v -> v != null && !v.trim().isEmpty())
                                .map(String::trim)
                                .collect(Collectors.toList());
                        if (!cleanLegacy.isEmpty()) {
                            List<String> existing = sanitizedAttrs.computeIfAbsent(tlId, k -> new ArrayList<>());
                            for (String val : cleanLegacy) {
                                if (!existing.contains(val)) {
                                    existing.add(val);
                                }
                            }
                        }
                    }
                }
            }
        }

        return ShopFilterRequest.builder()
                .keyword(req.getKeyword())
                .categoryId(req.getCategoryId())
                .brandId(req.getBrandId())
                .minPrice(req.getMinPrice())
                .maxPrice(req.getMaxPrice())
                .rating(req.getRating())
                .attributes(sanitizedAttrs)
                .legacyTrongLuong(req.getLegacyTrongLuong())
                .sort(req.getSort() != null ? req.getSort() : "newest")
                .page(req.getPage())
                .size(req.getSize() > 0 ? req.getSize() : 12)
                .build();
    }

    private Map<Integer, Set<String>> prepareSelectedLookup(Map<Integer, List<String>> selectedAttributes, List<String> legacyTrongLuong, Set<Integer> validAttrIds) {
        Map<Integer, Set<String>> selectedLookup = new HashMap<>();
        if (selectedAttributes != null && !selectedAttributes.isEmpty()) {
            for (Map.Entry<Integer, List<String>> e : selectedAttributes.entrySet()) {
                if (e.getKey() != null && validAttrIds.contains(e.getKey()) && e.getValue() != null) {
                    selectedLookup.put(e.getKey(), new HashSet<>(e.getValue()));
                }
            }
        }

        if (legacyTrongLuong != null && !legacyTrongLuong.isEmpty()) {
            Optional<Integer> trongLuongIdOpt = resolveTrongLuongAttributeId();
            if (trongLuongIdOpt.isPresent()) {
                Integer tlId = trongLuongIdOpt.get();
                if (validAttrIds.contains(tlId)) {
                    Set<String> set = selectedLookup.computeIfAbsent(tlId, k -> new HashSet<>());
                    set.addAll(legacyTrongLuong);
                }
            }
        }

        return selectedLookup;
    }
}
