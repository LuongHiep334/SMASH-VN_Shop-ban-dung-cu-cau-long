package com.smashvn.shop.service.impl;

import com.smashvn.shop.dto.chatbot.ChatProductResponse;
import com.smashvn.shop.dto.chatbot.ProductSearchCriteria;
import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.entity.SanPhamChiTiet;
import com.smashvn.shop.entity.ThuongHieu;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatbotProductCache {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final DanhMucRepository danhMucRepository;

    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes
    private Instant lastRefreshed = Instant.EPOCH;

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private List<CachedProduct> cachedProducts = new ArrayList<>();
    private List<String> cachedBrands = new ArrayList<>();
    private List<String> cachedCategories = new ArrayList<>();

    @Data
    @Builder
    public static class CachedProduct {
        private Integer id;
        private String name;
        private String brand;
        private String category;
        private BigDecimal price;
        private BigDecimal salePrice;
        private String shortDescription;
        private String imageUrl;
        private String detailUrl;
        private int stock;

        private String unaccentedName;
        private String unaccentedBrand;
        private String unaccentedCategory;
        private String unaccentedDesc;
    }

    @PostConstruct
    public void init() {
        try {
            refreshCache();
        } catch (Exception e) {
            log.warn("Initial ChatbotProductCache refresh deferred: {}", e.getMessage());
        }
    }

    public void ensureFreshCache() {
        if (Instant.now().isAfter(lastRefreshed.plusSeconds(CACHE_TTL_SECONDS))) {
            try {
                refreshCache();
            } catch (Exception e) {
                log.warn("Background ChatbotProductCache refresh failed: {}", e.getMessage());
            }
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void refreshCache() {
        rwLock.writeLock().lock();
        try {
            log.info("Refreshing ChatbotProductCache...");
            List<SanPhamChiTiet> variants = sanPhamChiTietRepository.findAllActiveInStock();
            Map<Integer, CachedProduct> productMap = new LinkedHashMap<>();

            for (SanPhamChiTiet v : variants) {
                SanPham sp = v.getSanPham();
                if (sp == null || !Boolean.TRUE.equals(sp.getTrangThaiValue()) || !Boolean.TRUE.equals(v.getTrangThaiValue()) || v.getSoLuongTon() <= 0) {
                    continue;
                }

                if (productMap.containsKey(sp.getId())) {
                    CachedProduct cp = productMap.get(sp.getId());
                    cp.setStock(cp.getStock() + v.getSoLuongTon());
                    continue;
                }

                String brandName = (sp.getThuongHieu() != null) ? sp.getThuongHieu().getTenThuongHieu() : "";
                String catName = (sp.getDanhMuc() != null) ? sp.getDanhMuc().getTenDanhMuc() : "";

                BigDecimal basePrice = v.getGiaBan();
                BigDecimal salePrice = null;
                try {
                    salePrice = sp.getGiaSauGiam(basePrice);
                } catch (Exception ex) {
                    salePrice = basePrice;
                }
                boolean hasSale = salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 && salePrice.compareTo(basePrice) < 0;

                String imgUrl = "/images/placeholder.png";
                if (v.getHinhAnhSanPhams() != null && !v.getHinhAnhSanPhams().isEmpty()) {
                    var mainImg = v.getHinhAnhSanPhams().stream()
                            .filter(i -> Boolean.TRUE.equals(i.getLaAnhChinh()))
                            .findFirst()
                            .orElse(v.getHinhAnhSanPhams().get(0));
                    imgUrl = normalizeImageUrl(mainImg.getUrlHinhAnh());
                }

                String desc = sp.getMoTa();
                if (desc != null) {
                    desc = desc.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                    if (desc.length() > 160) {
                        desc = desc.substring(0, 157).trim() + "...";
                    }
                }

                CachedProduct cp = CachedProduct.builder()
                        .id(sp.getId())
                        .name(sp.getTenSanPham())
                        .brand(brandName)
                        .category(catName)
                        .price(basePrice)
                        .salePrice(hasSale ? salePrice : null)
                        .shortDescription(desc)
                        .imageUrl(imgUrl)
                        .detailUrl("/san-pham/" + sp.getId())
                        .stock(v.getSoLuongTon())
                        .unaccentedName(ChatbotServiceImpl.removeAccents(sp.getTenSanPham().toLowerCase()))
                        .unaccentedBrand(ChatbotServiceImpl.removeAccents(brandName.toLowerCase()))
                        .unaccentedCategory(ChatbotServiceImpl.removeAccents(catName.toLowerCase()))
                        .unaccentedDesc(desc != null ? ChatbotServiceImpl.removeAccents(desc.toLowerCase()) : "")
                        .build();

                productMap.put(sp.getId(), cp);
            }

            this.cachedProducts = new ArrayList<>(productMap.values());

            // Brands
            List<ThuongHieu> brands = thuongHieuRepository.findByTrangThaiTrue();
            this.cachedBrands = brands.stream()
                    .map(ThuongHieu::getTenThuongHieu)
                    .filter(b -> b != null && !b.isBlank())
                    .toList();

            // Categories
            List<DanhMuc> cats = danhMucRepository.findByTrangThaiTrue();
            this.cachedCategories = cats.stream()
                    .map(DanhMuc::getTenDanhMuc)
                    .filter(c -> c != null && !c.isBlank())
                    .toList();

            lastRefreshed = Instant.now();
            log.info("ChatbotProductCache refreshed: {} products, {} brands, {} categories.",
                    cachedProducts.size(), cachedBrands.size(), cachedCategories.size());
        } catch (Exception e) {
            log.error("Failed to refresh ChatbotProductCache: {}", e.getMessage(), e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public List<String> getCachedBrands() {
        ensureFreshCache();
        rwLock.readLock().lock();
        try {
            return cachedBrands;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<String> getCachedCategories() {
        ensureFreshCache();
        rwLock.readLock().lock();
        try {
            return cachedCategories;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<ChatProductResponse> search(ProductSearchCriteria criteria, int limit) {
        ensureFreshCache();
        rwLock.readLock().lock();
        try {
            String kw = criteria.getKeyword() != null ? ChatbotServiceImpl.removeAccents(criteria.getKeyword().toLowerCase()) : null;
            String kw2 = criteria.getKeyword2() != null ? ChatbotServiceImpl.removeAccents(criteria.getKeyword2().toLowerCase()) : null;
            String kw3 = criteria.getKeyword3() != null ? ChatbotServiceImpl.removeAccents(criteria.getKeyword3().toLowerCase()) : null;

            String brand = criteria.getBrandName() != null ? ChatbotServiceImpl.removeAccents(criteria.getBrandName().toLowerCase()).replace("-", "") : null;
            String cat = criteria.getCategoryName() != null ? ChatbotServiceImpl.removeAccents(criteria.getCategoryName().toLowerCase()) : null;

            BigDecimal minPrice = criteria.getMinPrice();
            BigDecimal maxPrice = criteria.getMaxPrice();

            if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
                BigDecimal temp = minPrice;
                minPrice = maxPrice;
                maxPrice = temp;
            }

            List<CachedProduct> matched = new ArrayList<>();

            for (CachedProduct p : cachedProducts) {
                BigDecimal effectivePrice = (p.getSalePrice() != null) ? p.getSalePrice() : p.getPrice();

                // Price filters
                if (minPrice != null && effectivePrice.compareTo(minPrice) < 0) {
                    continue;
                }
                if (maxPrice != null && effectivePrice.compareTo(maxPrice) > 0) {
                    continue;
                }

                // Brand filter
                if (brand != null && !brand.isEmpty()) {
                    String cleanPBrand = p.getUnaccentedBrand().replace("-", "");
                    if (!cleanPBrand.contains(brand)) {
                        continue;
                    }
                }

                // Category filter
                if (cat != null && !cat.isEmpty()) {
                    if (!p.getUnaccentedCategory().contains(cat)) {
                        continue;
                    }
                }

                // Keyword filters
                if (kw != null && !kw.isEmpty()) {
                    boolean match = p.getUnaccentedName().contains(kw)
                            || p.getUnaccentedDesc().contains(kw)
                            || p.getUnaccentedBrand().contains(kw)
                            || p.getUnaccentedCategory().contains(kw);

                    if (!match && kw2 != null && !kw2.isEmpty()) {
                        match = p.getUnaccentedName().contains(kw2) || p.getUnaccentedDesc().contains(kw2);
                    }
                    if (!match && kw3 != null && !kw3.isEmpty()) {
                        match = p.getUnaccentedName().contains(kw3) || p.getUnaccentedDesc().contains(kw3);
                    }
                    if (!match) {
                        continue;
                    }
                }

                matched.add(p);
            }

            // Prioritize items with sale price or high stock
            if (criteria.getMaxPrice() != null) {
                matched.sort(Comparator.comparing((CachedProduct p) -> p.getSalePrice() != null ? 0 : 1)
                        .thenComparing(p -> p.getSalePrice() != null ? p.getSalePrice() : p.getPrice()));
            } else {
                matched.sort(Comparator.comparing((CachedProduct p) -> p.getSalePrice() != null ? 0 : 1)
                        .thenComparing(CachedProduct::getStock, Comparator.reverseOrder()));
            }

            return matched.stream()
                    .limit(limit)
                    .map(this::mapToChatProductResponse)
                    .toList();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public long countMatched(ProductSearchCriteria criteria) {
        ensureFreshCache();
        rwLock.readLock().lock();
        try {
            return search(criteria, 1000).size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private ChatProductResponse mapToChatProductResponse(CachedProduct cp) {
        return ChatProductResponse.builder()
                .id(cp.getId())
                .name(cp.getName())
                .brand(cp.getBrand())
                .price(cp.getPrice())
                .salePrice(cp.getSalePrice())
                .shortDescription(cp.getShortDescription())
                .imageUrl(cp.getImageUrl())
                .productUrl(cp.getDetailUrl())
                .detailUrl(cp.getDetailUrl())
                .build();
    }

    private String normalizeImageUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return "/images/placeholder.png";
        }
        String path = storedPath.trim().replace('\\', '/');
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("/uploads/")) {
            return path;
        }
        if (path.startsWith("uploads/")) {
            return "/" + path;
        }
        if (path.startsWith("product/")) {
            return "/uploads/" + path;
        }
        return "/uploads/product/" + path;
    }
}
