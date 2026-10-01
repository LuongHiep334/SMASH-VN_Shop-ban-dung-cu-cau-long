package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.DanhMucThuocTinh;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.constant.CategoryType;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.DanhMucThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamChiTietThuocTinhRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuocTinhRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DanhMucService {

    private static final Logger log = LoggerFactory.getLogger(DanhMucService.class);

    private final DanhMucRepository danhMucRepository;
    private final DanhMucThuocTinhRepository danhMucThuocTinhRepository;
    private final ThuocTinhRepository thuocTinhRepository;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietThuocTinhRepository sanPhamChiTietThuocTinhRepository;

    public DanhMuc themDanhMuc(String tenDanhMuc) {
        return themDanhMuc(tenDanhMuc, List.of());
    }

    @Transactional
    public DanhMuc themDanhMuc(String tenDanhMuc, List<Integer> thuocTinhIds) {
        String normalized = normalize(tenDanhMuc);
        validateLength(normalized);

        if (danhMucRepository.existsByTenDanhMucIgnoreCase(normalized)) {
            log.warn("[CATEGORY] Duplicate category add attempt detected.");
            throw new IllegalArgumentException(
                    "Danh mục \"" + normalized + "\" đã tồn tại. Vui lòng nhập tên khác!");
        }

        DanhMuc dm = new DanhMuc();
        dm.setTenDanhMuc(normalized);
        dm = danhMucRepository.save(dm);

        if (thuocTinhIds != null && !thuocTinhIds.isEmpty()) {
            List<DanhMucThuocTinh> listMapping = new ArrayList<>();
            for (Integer ttId : thuocTinhIds) {
                if (ttId != null) {
                    ThuocTinh tt = thuocTinhRepository.findById(ttId).orElse(null);
                    if (tt != null) {
                        DanhMucThuocTinh mapping = DanhMucThuocTinh.builder()
                                .danhMuc(dm)
                                .thuocTinh(tt)
                                .trangThai(true)
                                .build();
                        listMapping.add(mapping);
                    }
                }
            }
            if (!listMapping.isEmpty()) {
                danhMucThuocTinhRepository.saveAll(listMapping);
                dm.setDanhMucThuocTinhs(listMapping);
            }
        }

        return dm;
    }

    @Transactional
    public DanhMuc suaDanhMuc(Integer id, String tenDanhMuc) {
        return suaDanhMuc(id, tenDanhMuc, null, false);
    }

    @Transactional
    public DanhMuc suaDanhMuc(Integer id, String tenDanhMuc, List<Integer> thuocTinhIds) {
        return suaDanhMuc(id, tenDanhMuc, thuocTinhIds, true);
    }

    @Transactional
    public DanhMuc suaDanhMuc(
            Integer id,
            String tenDanhMuc,
            List<Integer> thuocTinhIds,
            boolean capNhatThuocTinh) {
        String normalized = normalize(tenDanhMuc);
        validateLength(normalized);

        if (danhMucRepository.existsByTenDanhMucIgnoreCaseAndIdNot(normalized, id)) {
            log.warn("[CATEGORY] Duplicate category edit attempt detected.");
            throw new IllegalArgumentException(
                    "Danh mục \"" + normalized + "\" đã tồn tại. Vui lòng nhập tên khác!");
        }

        DanhMuc dm = danhMucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));

        CategoryType oldType = CategoryType.fromDanhMuc(dm);
        CategoryType newType = CategoryType.fromName(normalized);
        if (sanPhamRepository.existsByDanhMucId(id) && oldType != newType) {
            throw new IllegalArgumentException(
                    "Không thể đổi tên làm thay đổi loại nghiệp vụ từ " + oldType
                            + " sang " + newType + " vì danh mục đang được gán cho sản phẩm.");
        }

        dm.setTenDanhMuc(normalized);

        if (capNhatThuocTinh) {
            updateAttributeMappings(dm, thuocTinhIds);
        }

        return danhMucRepository.save(dm);
    }

    private void updateAttributeMappings(DanhMuc dm, List<Integer> rawAttributeIds) {
        Set<Integer> requestedIds = rawAttributeIds == null
                ? Set.of()
                : rawAttributeIds.stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<DanhMucThuocTinh> mappings = dm.getDanhMucThuocTinhs();
        Map<Integer, DanhMucThuocTinh> existingByAttributeId = mappings.stream()
                .filter(mapping -> mapping.getThuocTinh() != null && mapping.getThuocTinh().getId() != null)
                .collect(Collectors.toMap(
                        mapping -> mapping.getThuocTinh().getId(),
                        Function.identity(),
                        (first, ignored) -> first));

        Set<Integer> removedIds = mappings.stream()
                .filter(mapping -> Boolean.TRUE.equals(mapping.getTrangThai()))
                .map(DanhMucThuocTinh::getThuocTinh)
                .filter(java.util.Objects::nonNull)
                .map(ThuocTinh::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        removedIds.removeAll(requestedIds);
        for (Integer removedId : removedIds) {
            if (sanPhamChiTietThuocTinhRepository
                    .existsByThuocTinh_IdAndSanPhamChiTiet_SanPham_DanhMuc_Id(removedId, dm.getId())) {
                ThuocTinh usedAttribute = existingByAttributeId.get(removedId).getThuocTinh();
                throw new IllegalArgumentException(
                        "Không thể bỏ thuộc tính \"" + usedAttribute.getTenThuocTinh()
                                + "\" vì biến thể sản phẩm trong danh mục đang sử dụng thuộc tính này.");
            }
        }

        for (DanhMucThuocTinh mapping : mappings) {
            Integer attributeId = mapping.getThuocTinh() == null ? null : mapping.getThuocTinh().getId();
            mapping.setTrangThai(attributeId != null && requestedIds.contains(attributeId));
        }

        for (Integer requestedId : requestedIds) {
            DanhMucThuocTinh existing = existingByAttributeId.get(requestedId);
            if (existing != null) {
                existing.setTrangThai(true);
                continue;
            }
            ThuocTinh attribute = thuocTinhRepository.findById(requestedId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thuộc tính với ID: " + requestedId));
            mappings.add(DanhMucThuocTinh.builder()
                    .danhMuc(dm)
                    .thuocTinh(attribute)
                    .trangThai(true)
                    .build());
        }
    }

    @Transactional
    public DanhMuc anHoacHienDanhMuc(Integer id) {
        DanhMuc dm = danhMucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục với ID: " + id));
        boolean currentStatus = Boolean.TRUE.equals(dm.getTrangThai());
        dm.setTrangThai(!currentStatus);
        log.info("[CATEGORY] Toggled status for category ID {}: {} -> {}", id, currentStatus, !currentStatus);
        return danhMucRepository.save(dm);
    }

    String normalize(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Tên danh mục không được để trống!");
        }
        String cleaned = Jsoup.clean(input, Safelist.none()).trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống!");
        }
        return cleaned;
    }

    private void validateLength(String normalized) {
        if (normalized.length() < 2) {
            throw new IllegalArgumentException("Tên danh mục phải có ít nhất 2 ký tự!");
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Tên danh mục không được vượt quá 100 ký tự!");
        }
    }
}
