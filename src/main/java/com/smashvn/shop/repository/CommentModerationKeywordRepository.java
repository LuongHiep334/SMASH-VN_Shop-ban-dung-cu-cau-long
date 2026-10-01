package com.smashvn.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smashvn.shop.entity.CommentModerationKeyword;

public interface CommentModerationKeywordRepository extends JpaRepository<CommentModerationKeyword, Integer> {

    List<CommentModerationKeyword> findAllByActiveTrue();
}
