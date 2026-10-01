package com.smashvn.shop.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RacketSpecUtilsTest {

    @Test
    void sanitizeRecommendedTension_keepsLbsRange() {
        String result = RacketSpecUtils.sanitizeRecommendedTension(" 20 - 28 lbs ");
        assertEquals("20 - 28 lbs", result);
    }

    @Test
    void sanitizeRecommendedTension_keepsKgRange() {
        String result = RacketSpecUtils.sanitizeRecommendedTension("9.0 - 12.5 kg");
        assertEquals("9.0 - 12.5 kg", result);
    }

    @Test
    void sanitizeRecommendedTension_allowsBlank() {
        assertEquals("", RacketSpecUtils.sanitizeRecommendedTension("   "));
        assertEquals("", RacketSpecUtils.sanitizeRecommendedTension(null));
    }

    @Test
    void sanitizeRecommendedTension_rejectsDangerousCharacters() {
        assertThrows(IllegalArgumentException.class,
                () -> RacketSpecUtils.sanitizeRecommendedTension("<script>"));
    }

    @Test
    void sanitizeRecommendedTension_rejectsTooLongValue() {
        assertThrows(IllegalArgumentException.class,
                () -> RacketSpecUtils.sanitizeRecommendedTension("a".repeat(51)));
    }
}
