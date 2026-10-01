package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ThuocTinh")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThuocTinh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ten_thuoc_tinh", nullable = false, unique = true, length = 100)
    private String tenThuocTinh;

    @Column(name = "trang_thai")
    @Builder.Default
    private Boolean trangThai = true;
}
