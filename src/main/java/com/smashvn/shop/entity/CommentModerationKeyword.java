package com.smashvn.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CommentModerationKeyword")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentModerationKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tu_khoa", nullable = false, unique = true)
    private String keyword;

    @Column(name = "kich_hoat", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "ngay_tao", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
