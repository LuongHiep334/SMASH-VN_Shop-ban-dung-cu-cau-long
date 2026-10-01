package com.smashvn.shop.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogCommentDTO {
    private Integer id;
    private Integer idBlog;
    private Integer idTaiKhoan;
    private String emailTaiKhoan;
    private String tenHienThi;
    private String content;
    private String createdAt;
    private Boolean deleted;
    private String deletedAt;
    private String deletedReason;
    private String deletedByEmail;
}
