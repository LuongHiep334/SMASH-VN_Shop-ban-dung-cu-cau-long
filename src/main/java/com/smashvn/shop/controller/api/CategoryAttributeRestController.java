package com.smashvn.shop.controller.api;

import com.smashvn.shop.entity.DanhMuc;
import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.service.product.ThuocTinhService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CategoryAttributeRestController {

    private final DanhMucRepository danhMucRepository;
    private final ThuocTinhService thuocTinhService;

    @GetMapping("/categories/{categoryId}/attributes")
    public ResponseEntity<?> getAttributesByCategory(@PathVariable("categoryId") Integer categoryId) {
        DanhMuc dm = danhMucRepository.findById(categoryId).orElse(null);
        if (dm == null) {
            return ResponseEntity.notFound().build();
        }
        List<ThuocTinh> attributes = dm.getThuocTinhList();
        return ResponseEntity.ok(attributes);
    }

    @GetMapping("/attributes")
    public ResponseEntity<List<ThuocTinh>> getAllAttributes() {
        return ResponseEntity.ok(thuocTinhService.getAllThuocTinh());
    }

    @PostMapping("/attributes")
    public ResponseEntity<?> createAttribute(@RequestBody Map<String, String> body) {
        try {
            String name = body.get("tenThuocTinh");
            ThuocTinh created = thuocTinhService.themThuocTinh(name);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            Map<String, String> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }
}
