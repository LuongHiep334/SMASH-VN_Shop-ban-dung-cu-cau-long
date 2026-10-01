package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "Blog")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tieu_de", nullable = false)
    private String title;

    @Column(name = "duong_dan", nullable = false, unique = true)
    private String slug;

    @Column(name = "tom_tat", length = 1000)
    private String summary;

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String content;

    @Column(name = "hinh_anh")
    private String image;

    @Column(name = "ngay_dang")
    private LocalDate publishDate;

    @org.hibernate.annotations.Formula("COALESCE((SELECT nv.ho_ten FROM NhanVien nv WHERE nv.id_tai_khoan = id_tai_khoan), (SELECT kh.ho_ten_kh FROM KhachHang kh WHERE kh.id_tai_khoan = id_tai_khoan))")
    private String author;

    @Column(name = "danh_muc")
    private String category;

    @Column(name = "the")
    private String tags;

    @Transient
    private Integer commentsCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    @Builder.Default
    private BlogStatus status = BlogStatus.DRAFT;

    @Column(name = "da_xoa", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "ngay_xoa")
    private java.time.LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan")
    private TaiKhoan nguoiDang;

    @Transient
    private String createdBy;


    @Column(name = "ngay_tao", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "ngay_cap_nhat")
    private java.time.LocalDateTime updatedAt;
}

