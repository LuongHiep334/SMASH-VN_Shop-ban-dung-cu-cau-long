package com.smashvn.shop.specification;

import com.smashvn.shop.dto.product.ShopFilterRequest;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.SanPhamChiTietThuocTinh;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

public class SanPhamSpecification {

    public static Specification<SanPham> filter(ShopFilterRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. SanPham active status
            predicates.add(cb.equal(root.get("trangThaiValue"), true));

            // 2. Active Category and active Brand joins
            Join<Object, Object> danhMucJoin = root.join("danhMuc", JoinType.LEFT);
            Join<Object, Object> thuongHieuJoin = root.join("thuongHieu", JoinType.LEFT);
            predicates.add(cb.or(cb.isNull(root.get("danhMuc")), cb.equal(danhMucJoin.get("trangThai"), true)));
            predicates.add(cb.or(cb.isNull(root.get("thuongHieu")), cb.equal(thuongHieuJoin.get("trangThai"), true)));

            // 3. Category Filter
            if (req.getCategoryId() != null) {
                predicates.add(cb.equal(danhMucJoin.get("id"), req.getCategoryId()));
            }

            // 4. Brand Filter
            if (req.getBrandId() != null) {
                predicates.add(cb.equal(thuongHieuJoin.get("id"), req.getBrandId()));
            }

            // 5. Keyword Search Filter
            if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
                predicates.add(CustomerSearchText.matches(cb, req.getKeyword(), List.of(
                        root.get("tenSanPham"), thuongHieuJoin.get("tenThuongHieu"), danhMucJoin.get("tenDanhMuc"))));
            }

            // 6. Rating Filter
            if (req.getRating() != null && req.getRating() > 0.0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("diemTrungBinh"), req.getRating()));
            }

            // 7. SAME VARIANT FILTER (Price + Dynamic Attributes + Legacy trongLuong)
            // Single variantScope correlated subquery to guarantee that ALL conditions match on the SAME SanPhamChiTiet
            Subquery<Integer> variantSubquery = query.subquery(Integer.class);
            Root<SanPhamChiTiet> variantRoot = variantSubquery.from(SanPhamChiTiet.class);
            variantSubquery.select(cb.literal(1));

            List<Predicate> variantPredicates = new ArrayList<>();
            variantPredicates.add(cb.equal(variantRoot.get("sanPham"), root));
            variantPredicates.add(cb.equal(variantRoot.get("trangThaiValue"), true));

            // Price conditions on the SAME variant
            Expression<BigDecimal> customerPrice = null;
            if (req.getMinPrice() != null || req.getMaxPrice() != null) {
                variantPredicates.add(CustomerPriceExpressions.availableOrAllSoldOut(root, variantRoot, query, cb));
                customerPrice = CustomerPriceExpressions.discounted(variantRoot.get("giaBan"), root, query, cb);
            }
            if (req.getMinPrice() != null) {
                variantPredicates.add(cb.greaterThanOrEqualTo(customerPrice, req.getMinPrice()));
            }
            if (req.getMaxPrice() != null) {
                variantPredicates.add(cb.lessThanOrEqualTo(customerPrice, req.getMaxPrice()));
            }

            // Process validated attributes map
            Map<Integer, List<String>> attributesMap = req.getAttributes();

            if (attributesMap != null && !attributesMap.isEmpty()) {
                for (Map.Entry<Integer, List<String>> entry : attributesMap.entrySet()) {
                    Integer thuocTinhId = entry.getKey();
                    List<String> rawValues = entry.getValue();

                    if (thuocTinhId != null && rawValues != null && !rawValues.isEmpty()) {
                        List<String> cleanValues = rawValues.stream()
                                .filter(v -> v != null && !v.trim().isEmpty())
                                .map(String::trim)
                                .toList();

                        if (!cleanValues.isEmpty()) {
                            // Subquery correlated to variantRoot for THIS attribute group
                            Subquery<Integer> attSubquery = query.subquery(Integer.class);
                            Root<SanPhamChiTietThuocTinh> attRoot = attSubquery.from(SanPhamChiTietThuocTinh.class);
                            attSubquery.select(cb.literal(1));

                            List<Predicate> attPredicates = new ArrayList<>();
                            attPredicates.add(cb.equal(attRoot.get("sanPhamChiTiet"), variantRoot));
                            attPredicates.add(cb.equal(attRoot.get("thuocTinh").get("id"), thuocTinhId));

                            // OR logic within the same attribute group
                            attPredicates.add(attRoot.get("giaTri").in(cleanValues));

                            attSubquery.where(attPredicates.toArray(new Predicate[0]));
                            variantPredicates.add(cb.exists(attSubquery));
                        }
                    }
                }
            }

            variantSubquery.where(variantPredicates.toArray(new Predicate[0]));
            predicates.add(cb.exists(variantSubquery));

            // 8. ORDER BY (Only applied for content queries, NOT count queries)
            if (query.getResultType() == SanPham.class) {
                List<Order> orders = new ArrayList<>();

                // 8a. In-stock products first
                Subquery<Integer> stockSumSubquery = query.subquery(Integer.class);
                Root<SanPhamChiTiet> stockRoot = stockSumSubquery.from(SanPhamChiTiet.class);
                Expression<Integer> sumStock = cb.sum(stockRoot.get("soLuongTon"));
                stockSumSubquery.select(cb.coalesce(sumStock, 0));
                stockSumSubquery.where(
                        cb.equal(stockRoot.get("sanPham"), root),
                        cb.equal(stockRoot.get("trangThaiValue"), true)
                );
                Expression<Integer> hasStockExpr = cb.<Integer>selectCase()
                        .when(cb.greaterThan(stockSumSubquery, 0), 1)
                        .otherwise(0);
                orders.add(cb.desc(hasStockExpr));

                // 8b. Price sorting
                if ("price_asc".equalsIgnoreCase(req.getSort())) {
                    orders.add(cb.asc(CustomerPriceExpressions.representative(root, query, cb)));
                } else if ("price_desc".equalsIgnoreCase(req.getSort())) {
                    orders.add(cb.desc(CustomerPriceExpressions.representative(root, query, cb)));
                }

                // 8c. Default sorting: newest ID first
                orders.add(cb.desc(root.get("id")));

                query.orderBy(orders);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
