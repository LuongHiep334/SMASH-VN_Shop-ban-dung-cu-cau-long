package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BlogComment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_blog", nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "noi_dung", nullable = false, length = 1000)
    private String content;

    @Column(name = "ngay_tao", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Transient
    private TaiKhoan createdBy;

    @Column(name = "da_xoa", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "ngay_xoa")
    private LocalDateTime deletedAt;

    @Transient
    private TaiKhoan deletedBy;

    @Column(name = "ly_do_xoa", length = 500)
    private String deletedReason;
}
