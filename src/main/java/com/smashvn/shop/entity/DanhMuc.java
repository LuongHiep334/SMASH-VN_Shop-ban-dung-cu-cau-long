package com.smashvn.shop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "DanhMuc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "danhMucThuocTinhs")
public class DanhMuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "ten_danh_muc", nullable = false)
    private String tenDanhMuc;

    @Column(name = "trang_thai")
    @Builder.Default
    private Boolean trangThai = true;

    @OneToMany(mappedBy = "danhMuc", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    @Builder.Default
    private List<DanhMucThuocTinh> danhMucThuocTinhs = new ArrayList<>();

    public List<ThuocTinh> getThuocTinhList() {
        if (danhMucThuocTinhs == null) return List.of();
        return danhMucThuocTinhs.stream()
                .filter(dmtt -> Boolean.TRUE.equals(dmtt.getTrangThai()) && dmtt.getThuocTinh() != null && Boolean.TRUE.equals(dmtt.getThuocTinh().getTrangThai()))
                .map(DanhMucThuocTinh::getThuocTinh)
                .collect(Collectors.toList());
    }

    public String getThuocTinhDisplay() {
        List<ThuocTinh> list = getThuocTinhList();
        if (list.isEmpty()) return "Chưa cấu hình";
        return list.stream().map(ThuocTinh::getTenThuocTinh).collect(Collectors.joining(", "));
    }
}