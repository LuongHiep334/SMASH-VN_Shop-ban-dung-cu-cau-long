package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "GiaoDichThanhToan")
@Data
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_giao_dich", nullable = false, unique = true, length = 100)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hoa_don", nullable = true)
    private HoaDon order;

    @Column(name = "so_tien", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "cong_thanh_toan", nullable = false, length = 50)
    private String gateway;

    @Column(name = "trang_thai", nullable = false, length = 50)
    private String status;

    @Column(name = "du_lieu_tho", columnDefinition = "NVARCHAR(MAX)")
    private String rawPayload;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
