package com.smashvn.shop.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VietnamesePriceParser {

    private static final Pattern PRICE_PATTERN = Pattern.compile(
            "(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*(chục\\s*)?(triệu|tr|nghìn|ngàn|k|đ|vnd)?(?:\\s*(\\d+)(?:\\s*(k|nghìn|ngàn|tr|triệu))?)?\\b"
    );

    // Specific patterns for composite numbers like 1tr5, 2tr5, 1 triệu 500
    private static final Pattern TR_COMPOSITE_PATTERN = Pattern.compile(
            "(?i)\\b(\\d+)\\s*(?:tr|triệu)\\s*(\\d+)\\s*(k|nghìn|ngàn)?\\b"
    );

    public record PriceFilter(BigDecimal minPrice, BigDecimal maxPrice, boolean hasPrice, String matchedText) {
    }

    public static PriceFilter parsePriceFilter(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new PriceFilter(null, null, false, null);
        }

        String textLower = rawText.toLowerCase().trim();
        String unaccented = ChatbotServiceImpl.removeAccents(textLower);

        // 1. Check Range pattern: "từ X đến Y", "X - Y", "X đến Y", "X tới Y"
        Pattern rangePattern = Pattern.compile(
                "(?i)(?:từ\\s+|tu\\s+|tầm\\s+|tam\\s+|khoảng\\s+|khoang\\s+)?([0-9.,]+(?:\\s*(?:tr|trieu|triệu|củ|cu|k|nghìn|nghin|ngàn|ngan|cành|canh))?)\\s*(?:-|đến|den|tới|toi)\\s*([0-9.,]+(?:\\s*(?:tr|trieu|triệu|củ|cu|k|nghìn|nghin|ngàn|ngan|cành|canh))?)"
        );
        Matcher rangeMatcher = rangePattern.matcher(textLower);
        if (rangeMatcher.find()) {
            BigDecimal p1 = parsePrice(rangeMatcher.group(1));
            BigDecimal p2 = parsePrice(rangeMatcher.group(2));
            if (p1 != null && p2 != null) {
                BigDecimal minP = p1.min(p2);
                BigDecimal maxP = p1.max(p2);
                return new PriceFilter(minP, maxP, true, rangeMatcher.group(0));
            }
        }

        // 2. Parse Single Price
        BigDecimal singlePrice = parsePrice(rawText);
        if (singlePrice == null) {
            return new PriceFilter(null, null, false, null);
        }

        // 3. Determine direction from surrounding keywords in unaccented and accented text
        // Upper bound: maxPrice <= singlePrice
        boolean isUpperBound = Pattern.compile("(?i)(?U)\\b(tro xuong|tro ve|do lai|do ve|quay dau|hat lai|xuong|duoi|thap hon|toi da|khong qua|khong vuot qua|max|be hon|nho hon|trở xuống|trở về|đổ lại|đổ về|quay đầu|hất lại|xuống|dưới|thấp hơn|tối đa|không quá|không vượt quá|bé hơn|nhỏ hơn)\\b|<=|trở xuống|đổ lại|quay đầu").matcher(textLower).find()
                || Pattern.compile("(?i)\\b(tro xuong|tro ve|do lai|do ve|quay dau|hat lai|xuong|duoi|thap hon|toi da|khong qua|khong vuot qua|max|be hon|nho hon)\\b").matcher(unaccented).find();

        // Lower bound: minPrice >= singlePrice
        boolean isLowerBound = !isUpperBound && (
                Pattern.compile("(?i)(?U)\\b(tro len|do len|hat len|len|tren|cao hon|lon hon|toi thieu|it nhat|min|trở lên|đổ lên|hất lên|lên|trên|cao hơn|lớn hơn|tối thiểu|ít nhất)\\b|>=").matcher(textLower).find()
                || Pattern.compile("(?i)\\b(tro len|do len|hat len|len|tren|cao hon|lon hon|toi thieu|it nhat|min)\\b").matcher(unaccented).find()
                || unaccented.contains("hon ")
        );

        if (isUpperBound) {
            return new PriceFilter(null, singlePrice, true, null);
        } else if (isLowerBound) {
            return new PriceFilter(singlePrice, null, true, null);
        } else {
            // Target approximate price (e.g. "tam 1tr5", "khoang 2 cu", "1tr5") -> ±20%
            BigDecimal minP = singlePrice.multiply(new BigDecimal("0.8")).setScale(0, RoundingMode.HALF_UP);
            BigDecimal maxP = singlePrice.multiply(new BigDecimal("1.2")).setScale(0, RoundingMode.HALF_UP);
            return new PriceFilter(minP, maxP, true, null);
        }
    }

    public static String stripPriceTokens(String text) {
        if (text == null) return "";
        return text
                .replaceAll("(?i)(?U)\\b\\d+\\s*(?:củ|cu|chai)\\s*rưỡi\\b", " ")
                .replaceAll("(?i)(?U)\\b(?:củ|cu|chai)\\s*rưỡi\\b", " ")
                .replaceAll("(?i)(?U)\\b\\d+\\s*(?:củ|cu|chai|cành|canh)\\b", " ")
                .replaceAll("(?i)\\b\\d+\\s*(?:tr|triệu|trieu)\\s*\\d+\\s*(?:k|nghìn|nghin|ngàn|ngan)?\\b", " ")
                .replaceAll("(?i)\\b\\d+(?:[.,]\\d+)?\\s*(?:tr|triệu|trieu|k|nghìn|nghin|ngàn|ngan|đ|vnd|vnđ)\\b", " ")
                .replaceAll("\\b\\d{1,3}(?:[.,]\\d{3})+\\b", " ")
                .replaceAll("\\b\\d{5,9}\\b", " ");
    }

    public static BigDecimal parsePrice(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String clean = text.toLowerCase().trim()
                .replaceAll("đ|vnd|vnđ", "")
                .trim();

        // 0. Normalize slang: "củ", "chai" -> "triệu", "cành" -> "k"
        clean = clean.replaceAll("(?i)(?U)\\b(\\d+)\\s*(củ|chai)\\s*rưỡi\\b", "$1.5 triệu")
                .replaceAll("(?i)(?U)\\b(củ|chai)\\s*rưỡi\\b", "1.5 triệu")
                .replaceAll("(?i)(?U)\\brưỡi\\b", ".5 triệu")
                .replaceAll("(?i)(?U)\\b(củ|chai|cu)\\b", "triệu")
                .replaceAll("(?i)(?U)\\b(cành|canh)\\b", "k");

        // 1. Try composite pattern like "1tr5", "2 triệu 500", "1tr500"
        Matcher compMatcher = TR_COMPOSITE_PATTERN.matcher(clean);
        if (compMatcher.find()) {
            try {
                long mainPart = Long.parseLong(compMatcher.group(1));
                String subStr = compMatcher.group(2);
                long subPart = Long.parseLong(subStr);
                String unit = compMatcher.group(3);

                BigDecimal base = BigDecimal.valueOf(mainPart).multiply(new BigDecimal("1000000"));
                if (subStr.length() == 1) { // e.g. "5" in 1tr5 -> 500,000
                    base = base.add(BigDecimal.valueOf(subPart).multiply(new BigDecimal("100000")));
                } else if (unit != null && (unit.equals("k") || unit.contains("nghìn") || unit.contains("ngàn"))) {
                    base = base.add(BigDecimal.valueOf(subPart).multiply(new BigDecimal("1000")));
                } else if (subPart < 1000) {
                    base = base.add(BigDecimal.valueOf(subPart).multiply(new BigDecimal("1000")));
                } else {
                    base = base.add(BigDecimal.valueOf(subPart));
                }
                return base;
            } catch (Exception ignored) {
            }
        }

        // 2. Try decimal million pattern like "1.5tr", "1,5 triệu", "1.5 triệu"
        Matcher millionDecimalMatcher = Pattern.compile("(?i)\\b(\\d+[.,]\\d+)\\s*(?:tr|triệu)\\b").matcher(clean);
        if (millionDecimalMatcher.find()) {
            try {
                String valStr = millionDecimalMatcher.group(1).replace(',', '.');
                BigDecimal val = new BigDecimal(valStr);
                return val.multiply(new BigDecimal("1000000")).setScale(0, RoundingMode.HALF_UP);
            } catch (Exception ignored) {
            }
        }

        // 3. Try thousand pattern like "500k", "500 nghìn", "500 ngàn"
        Matcher thousandMatcher = Pattern.compile("(?i)\\b(\\d+(?:[.,]\\d+)?)\\s*(?:k|nghìn|ngàn)\\b").matcher(clean);
        if (thousandMatcher.find()) {
            try {
                String valStr = thousandMatcher.group(1).replace(',', '.');
                BigDecimal val = new BigDecimal(valStr);
                return val.multiply(new BigDecimal("1000")).setScale(0, RoundingMode.HALF_UP);
            } catch (Exception ignored) {
            }
        }

        // 4. Try integer million pattern like "1tr", "1 triệu", "2 triệu"
        Matcher millionIntMatcher = Pattern.compile("(?i)\\b(\\d+)\\s*(?:tr|triệu)\\b").matcher(clean);
        if (millionIntMatcher.find()) {
            try {
                BigDecimal val = new BigDecimal(millionIntMatcher.group(1));
                return val.multiply(new BigDecimal("1000000")).setScale(0, RoundingMode.HALF_UP);
            } catch (Exception ignored) {
            }
        }

        // 5. Raw number formatted like "500.000", "1.500.000", "2.200.000", "500000"
        Matcher formattedNumMatcher = Pattern.compile("\\b(\\d{1,3}(?:[.,]\\d{3})+|\\d{5,9})\\b").matcher(clean);
        if (formattedNumMatcher.find()) {
            try {
                String numStr = formattedNumMatcher.group(1).replaceAll("[.,]", "");
                return new BigDecimal(numStr);
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
