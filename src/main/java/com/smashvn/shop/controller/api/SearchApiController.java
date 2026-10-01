package com.smashvn.shop.controller.api;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.dto.product.CustomerPriceSummary;
import com.smashvn.shop.specification.CustomerAutocompleteSpecification;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamChiTietRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchApiController {
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final DanhMucRepository danhMucRepository;
    private final ThuongHieuRepository thuongHieuRepository;

    /**
     * Autocomplete API - trả về top 8 sản phẩm khớp từ khóa (gõ >= 2 ký tự).
     */
    @GetMapping("/search/products")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> searchProducts(
            @RequestParam("q") String q) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        String keyword = Jsoup.clean(q.trim(), Safelist.none());
        List<SanPham> results = sanPhamRepository.findAll(CustomerAutocompleteSpecification.matching(keyword),
                PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"))).getContent();

        List<Map<String, Object>> response = results.stream().map(sp -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sp.getId());
            map.put("tenSanPham", sp.getTenSanPham());
            map.put("thuongHieu", sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : "");
            map.put("danhMuc", sp.getDanhMuc() != null ? sp.getDanhMuc().getTenDanhMuc() : "");
            addCustomerPrice(map, sp);
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Từ khóa phổ biến API - trả về danh mục, thương hiệu, SP bán chạy khi focus ô tìm kiếm.
     */
    @GetMapping("/search/popular")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getPopularKeywords() {
        Map<String, Object> result = new HashMap<>();

        // 1. Danh mục (Vợt, Giày, Phụ kiện)
        List<Map<String, Object>> categories = danhMucRepository.findByTrangThaiTrue().stream()
            .map(dm -> Map.<String, Object>of(
                "id", dm.getId(),
                "ten", dm.getTenDanhMuc()
            )).toList();
        result.put("danhMuc", categories);

        // 2. Thương hiệu (Yonex, Victor, Lining, ...)
        List<Map<String, Object>> brands = thuongHieuRepository.findByTrangThaiTrue().stream()
            .map(th -> Map.<String, Object>of(
                "id", th.getId(),
                "ten", th.getTenThuongHieu()
            )).toList();
        result.put("thuongHieu", brands);

        // 3. Sản phẩm bán chạy nhất (top 4)
        List<Map<String, Object>> trending = sanPhamRepository
            .findBestSellers(PageRequest.of(0, 4)).stream()
            .map(sp -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", sp.getId());
                map.put("tenSanPham", sp.getTenSanPham());
                map.put("thuongHieu", sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : "");
                map.put("danhMuc", sp.getDanhMuc() != null ? sp.getDanhMuc().getTenDanhMuc() : "");
                addCustomerPrice(map, sp);
                return map;
            }).toList();
        result.put("sanPhamNoiBat", trending);

        return ResponseEntity.ok(result);
    }

    private void addCustomerPrice(Map<String, Object> map, SanPham product) {
        CustomerPriceSummary price = CustomerPriceSummary.from(product,
                sanPhamChiTietRepository.findActiveBySanPham_Id(product.getId()));
        map.put("giaBan", price.giaBan());
        map.put("giaSauGiam", price.giaSauGiam());
        map.put("giamGia", price.giamGia());
        map.put("conHang", price.conHang());
        map.put("nhieuMucGia", price.nhieuMucGia());
    }
}
