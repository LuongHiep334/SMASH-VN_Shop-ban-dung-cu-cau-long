package com.smashvn.shop.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "HinhAnhDanhGia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HinhAnhDanhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_danh_gia", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DanhGia danhGia;

    @Column(name = "url_hinh_anh", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String urlHinhAnh;

    @Column(name = "thu_tu")
    private Integer thuTu;

    @Builder.Default
    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao = LocalDateTime.now();

    public String getDuongDan() {
        return urlHinhAnh;
    }

    public void setDuongDan(String duongDan) {
        this.urlHinhAnh = duongDan;
    }
}
