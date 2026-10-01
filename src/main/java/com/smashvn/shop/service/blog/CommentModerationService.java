package com.smashvn.shop.service.blog;

import com.smashvn.shop.entity.CommentModerationKeyword;
import com.smashvn.shop.repository.CommentModerationKeywordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentModerationService {

    private final CommentModerationKeywordRepository keywordRepository;

    @Cacheable(value = "moderationKeywords")
    public List<String> getActiveKeywords() {
        log.info("[CACHE_EVENT] Loading active moderation keywords from database...");
        List<CommentModerationKeyword> list = keywordRepository.findAllByActiveTrue();
        return list.stream()
                .map(CommentModerationKeyword::getKeyword)
                .map(this::normalizeKeyword)
                .filter(k -> !k.trim().isEmpty())
                .collect(Collectors.toList());
    }

    @Cacheable(value = "moderationRawKeywords")
    public List<String> getActiveRawKeywords() {
        log.info("[CACHE_EVENT] Loading active raw moderation keywords from database...");
        List<CommentModerationKeyword> list = keywordRepository.findAllByActiveTrue();
        return list.stream()
                .map(CommentModerationKeyword::getKeyword)
                .filter(k -> k != null && !k.trim().isEmpty())
                .collect(Collectors.toList());
    }

    @CacheEvict(value = {"moderationKeywords", "moderationRawKeywords"}, allEntries = true)
    public void clearKeywordCache() {
        log.info("[CACHE_EVENT] Evicting moderation keywords cache...");
    }

    private String normalizeKeyword(String input) {
        if (input == null) return "";
        String temp = input.toLowerCase();
        temp = Normalizer.normalize(temp, Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{M}+", "");
        temp = temp.replace('đ', 'd').replace('Đ', 'd');
        // Keep only alphanumeric and whitespace
        temp = temp.replaceAll("[^a-z0-9\\s]", "");
        // Collapse multiple spaces
        temp = temp.replaceAll("\\s+", " ").trim();
        return temp;
    }
}
