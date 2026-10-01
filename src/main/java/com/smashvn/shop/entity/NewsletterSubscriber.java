package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "NewsletterSubscriber")
@Data
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "ngay_dang_ky", nullable = false)
    private LocalDateTime ngayDangKy = LocalDateTime.now();

    @Column(name = "ngay_huy")
    private LocalDateTime ngayHuy;

    @Column(name = "trang_thai", nullable = false, length = 30)
    private String trangThai = "ACTIVE";

    @Column(name = "token_huy_dang_ky", length = 255)
    private String tokenHuy;
}
