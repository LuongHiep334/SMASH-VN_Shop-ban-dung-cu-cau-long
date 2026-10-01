package com.smashvn.shop.scheduler;

import com.smashvn.shop.service.product.DanhGiaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentModerationScheduler {

    private final DanhGiaService danhGiaService;

    @Scheduled(fixedDelayString = "${app.comment-moderation.scan-delay-ms:300000}")
    public void runCommentModerationScan() {
        log.info("[MODERATION_SCHEDULER] Starting scheduled comment moderation scan...");
        try {
            danhGiaService.scanAndModerateReviews();
        } catch (Exception e) {
            log.error("[MODERATION_SCHEDULER_ERROR] Error during scheduled scan", e);
        }
    }
}
