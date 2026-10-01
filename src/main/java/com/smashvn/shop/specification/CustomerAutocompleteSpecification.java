package com.smashvn.shop.specification;

import com.smashvn.shop.entity.SanPham;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class CustomerAutocompleteSpecification {
    private CustomerAutocompleteSpecification() { }

    public static Specification<SanPham> matching(String keyword) {
        return (root, query, cb) -> {
            var brand = root.join("thuongHieu", JoinType.LEFT);
            var category = root.join("danhMuc", JoinType.LEFT);
            return cb.and(cb.isTrue(root.get("trangThaiValue")),
                    cb.or(cb.isNull(root.get("thuongHieu")), cb.isTrue(brand.get("trangThai"))),
                    cb.or(cb.isNull(root.get("danhMuc")), cb.isTrue(category.get("trangThai"))),
                    // Preserve the existing autocomplete fields; do not add variant SKU search.
                    CustomerSearchText.matches(cb, keyword, List.of(root.get("tenSanPham"),
                            root.get("maSanPham"), brand.get("tenThuongHieu"))));
        };
    }
}
