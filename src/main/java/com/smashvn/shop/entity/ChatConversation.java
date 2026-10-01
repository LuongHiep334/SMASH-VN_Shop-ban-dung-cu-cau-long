package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatConversation")
@Data
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tai_khoan")
    private TaiKhoan taiKhoan;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "tieu_de")
    private String tieuDe;

    @Column(name = "trang_thai", nullable = false)
    private String trangThai;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) {
            trangThai = "ACTIVE";
        }
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
        if (ngayCapNhat == null) {
            ngayCapNhat = LocalDateTime.now();
        }
    }
}
