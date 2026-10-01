package com.smashvn.shop.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogDTO {
    private Integer id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String image;
    private String publishDate;
    private String author;
    private String category;
    private List<String> tags;
    private Integer commentsCount;
    private String status;
    private Boolean deleted;
    private String createdBy;
    private String createdAt;
    private String updatedBy;
    private String updatedAt;
    private Integer idNguoiDang;
    private String emailNguoiDang;
}

