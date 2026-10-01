package com.smashvn.shop.service.product;

import com.smashvn.shop.entity.ThuocTinh;
import com.smashvn.shop.repository.ThuocTinhRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ThuocTinhService {

    private static final Logger log = LoggerFactory.getLogger(ThuocTinhService.class);
    private final ThuocTinhRepository thuocTinhRepository;

    public List<ThuocTinh> getAllThuocTinh() {
        return thuocTinhRepository.findByTrangThaiTrueOrderByIdAsc();
    }

    public ThuocTinh themThuocTinh(String tenThuocTinh) {
        if (tenThuocTinh == null || tenThuocTinh.isBlank()) {
            throw new IllegalArgumentException("Tên loại thuộc tính không được để trống!");
        }
        String cleaned = Jsoup.clean(tenThuocTinh, Safelist.none()).trim().replaceAll("\\s+", " ");
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Tên loại thuộc tính không được để trống!");
        }
        if (cleaned.length() > 100) {
            throw new IllegalArgumentException("Tên loại thuộc tính không được vượt quá 100 ký tự!");
        }

        if (thuocTinhRepository.existsByTenThuocTinhIgnoreCase(cleaned)) {
            throw new IllegalArgumentException("Loại thuộc tính \"" + cleaned + "\" đã tồn tại!");
        }

        ThuocTinh tt = ThuocTinh.builder()
                .tenThuocTinh(cleaned)
                .trangThai(true)
                .build();
        return thuocTinhRepository.save(tt);
    }
}
