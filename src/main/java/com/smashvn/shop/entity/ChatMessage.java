package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "ChatMessage")
@Data
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuoc_tro_chuyen", nullable = false)
    private ChatConversation conversation;

    @Column(name = "vai_tro", nullable = false)
    private String vaiTro; // USER, ASSISTANT, SYSTEM, TOOL

    @Column(name = "noi_dung", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    @Column(name = "ten_model")
    private String tenModel;

    @Column(name = "trang_thai", nullable = false)
    private String trangThai; // PENDING, SUCCESS, FAILED, BLOCKED

    @Column(name = "so_token_dau_vao")
    private Integer soTokenDauVao;

    @Column(name = "so_token_dau_ra")
    private Integer soTokenDauRa;

    @Column(name = "thoi_gian_xu_ly_ms")
    private Long thoiGianXuLyMs;

    @Column(name = "ma_loi")
    private String maLoi;

    @Column(name = "noi_dung_loi")
    private String noiDungLoi;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) {
            trangThai = "SUCCESS";
        }
        if (ngayTao == null) {
            ngayTao = LocalDateTime.now();
        }
    }
}
