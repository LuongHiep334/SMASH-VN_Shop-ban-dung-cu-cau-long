package com.smashvn.shop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ProfanityFilter {

    public record FilterResult(String content, int replacementCount) {
        public boolean moderated() {
            return replacementCount > 0;
        }
    }
    
    private static final Pattern[] LOW_PATTERNS = {
        Pattern.compile("(?U)\\bv[ck]l+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bvl+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bwtf\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] MEDIUM_PATTERNS = {
        Pattern.compile("(?U)\\b[d\u0111][e\u00e9]o+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bc[u\u01b0]t+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bc[a\u0103\u00e2]c+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bb[u\u01b0]\u00f4?[i\u00ec]+\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] HIGH_PATTERNS = {
        Pattern.compile("(?U)\\b[d\u0111][\\s\\.\\-_]*m+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\b[d\u0111][\\s\\.\\-_]*c[\\s\\.\\-_]*m+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bl[o\u00f2\u00f4]n+\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\b\u0111\u1ed3\\s+ch\u00f3\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bch\u00f3\\s+\u0111\u1ebb\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] CRITICAL_PATTERNS = {
        Pattern.compile("(?U)\\bgi\u1ebft\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bhi\u1ebfp\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\b\u0111e\\s+d\u1ecd\u0061\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\bl\u1eeba\\s+\u0111\u1ea3o\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?U)\\b\u0103n\\s+c\u01b0\u1edbp\\b", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] BAD_WORDS_PATTERNS;
    static {
        List<Pattern> all = new ArrayList<>();
        all.addAll(Arrays.asList(CRITICAL_PATTERNS));
        all.addAll(Arrays.asList(HIGH_PATTERNS));
        all.addAll(Arrays.asList(MEDIUM_PATTERNS));
        all.addAll(Arrays.asList(LOW_PATTERNS));
        BAD_WORDS_PATTERNS = all.toArray(new Pattern[0]);
    }

    public String filter(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }
        
        String result = input;
        for (Pattern pattern : BAD_WORDS_PATTERNS) {
            result = pattern.matcher(result).replaceAll("***");
        }
        return result;
    }

    public String filter(String input, List<String> customKeywords) {
        return filterWithResult(input, customKeywords).content();
    }

    /**
     * Filters database keywords against an accent-insensitive representation while
     * retaining an index map to the original text. This lets us replace the exact
     * original span (including inserted punctuation/whitespace) without storing a
     * normalized or otherwise damaged comment.
     */
    public FilterResult filterWithResult(String input, List<String> customKeywords) {
        if (input == null || input.trim().isEmpty()) {
            return new FilterResult(input, 0);
        }

        String result = filter(input);
        int replacements = countMarkers(result) - countMarkers(input);

        if (customKeywords != null && !customKeywords.isEmpty()) {
            List<String> keywords = customKeywords.stream()
                    .filter(k -> k != null && !k.trim().isEmpty())
                    .map(String::trim)
                    .filter(k -> k.length() >= 2)
                    .distinct()
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();
            for (String keyword : keywords) {
                FilterResult partial = replaceNormalized(result, keyword);
                result = partial.content();
                replacements += partial.replacementCount();
            }
        }
        return new FilterResult(result, replacements);
    }

    private FilterResult replaceNormalized(String input, String keyword) {
        // Pattern.quote is deliberately used for the literal fast path, so regex
        // metacharacters supplied by an administrator can never alter the regex.
        Pattern literal = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(keyword) + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);
        java.util.regex.Matcher literalMatcher = literal.matcher(input);
        String literalReplaced = literalMatcher.replaceAll("***");
        int literalCount = countMatches(literal.matcher(input));

        String normalizedKeyword = compactNormalize(keyword);
        if (normalizedKeyword.length() < 2) {
            return new FilterResult(literalReplaced, literalCount);
        }

        NormalizedText normalized = normalizeWithMap(literalReplaced);
        List<int[]> spans = new ArrayList<>();
        int from = 0;
        while (from <= normalized.text.length() - normalizedKeyword.length()) {
            int position = normalized.text.indexOf(normalizedKeyword, from);
            if (position < 0) break;
            int end = position + normalizedKeyword.length();
            int originalStart = normalized.originalIndexes.get(position);
            int originalEnd = normalized.originalIndexes.get(end - 1) + 1;
            boolean leftBoundary = originalStart == 0 || !Character.isLetterOrDigit(literalReplaced.charAt(originalStart - 1));
            boolean rightBoundary = originalEnd == literalReplaced.length()
                    || !Character.isLetterOrDigit(literalReplaced.charAt(originalEnd));
            if (leftBoundary && rightBoundary) {
                spans.add(new int[]{originalStart, originalEnd});
            }
            from = Math.max(position + 1, end);
        }
        StringBuilder output = new StringBuilder(literalReplaced);
        for (int i = spans.size() - 1; i >= 0; i--) {
            int[] span = spans.get(i);
            output.replace(span[0], span[1], "***");
        }
        return new FilterResult(output.toString(), literalCount + spans.size());
    }

    private NormalizedText normalizeWithMap(String input) {
        StringBuilder text = new StringBuilder();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < input.length(); i++) {
            String normalizedChar = Normalizer.normalize(String.valueOf(input.charAt(i)), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT)
                    .replace('đ', 'd');
            for (int j = 0; j < normalizedChar.length(); j++) {
                char c = normalizedChar.charAt(j);
                if (Character.isLetterOrDigit(c)) {
                    text.append(c);
                    indexes.add(i);
                }
            }
        }
        return new NormalizedText(text.toString(), indexes);
    }

    private String compactNormalize(String input) {
        return normalizeWithMap(input).text;
    }

    private int countMatches(java.util.regex.Matcher matcher) {
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private int countMarkers(String input) {
        if (input == null) return 0;
        int count = 0;
        for (int i = 0; (i = input.indexOf("***", i)) >= 0; i += 3) count++;
        return count;
    }

    private record NormalizedText(String text, List<Integer> originalIndexes) {}

    public SeverityLevel getSeverity(String input) {
        if (input == null || input.trim().isEmpty()) {
            return SeverityLevel.NONE;
        }
        if (matchesAny(input, CRITICAL_PATTERNS)) {
            return SeverityLevel.CRITICAL;
        }
        if (matchesAny(input, HIGH_PATTERNS)) {
            return SeverityLevel.HIGH;
        }
        if (matchesAny(input, MEDIUM_PATTERNS)) {
            return SeverityLevel.MEDIUM;
        }
        if (matchesAny(input, LOW_PATTERNS)) {
            return SeverityLevel.LOW;
        }
        return SeverityLevel.NONE;
    }

    public SeverityLevel getSeverity(String input, List<String> customKeywords) {
        if (input == null || input.trim().isEmpty()) {
            return SeverityLevel.NONE;
        }

        // 1. Get severity from hardcoded patterns
        SeverityLevel hardcodedSeverity = getSeverity(input);

        // 2. Check custom keywords
        SeverityLevel customSeverity = SeverityLevel.NONE;
        if (customKeywords != null && !customKeywords.isEmpty()) {
            String normalizedInput = normalizeAndCollapse(input);
            String paddedInput = " " + normalizedInput + " ";
            String rawLowerInput = input.toLowerCase();

            for (String kw : customKeywords) {
                if (kw == null) {
                    continue;
                }
                String trimmedKw = kw.trim();
                if (trimmedKw.isEmpty()) {
                    continue;
                }

                boolean matched = false;

                // 2a. Raw lowercase check (handles c++, a+b, test.com directly if raw length >= 2)
                if (trimmedKw.length() >= 2) {
                    if (rawLowerInput.contains(trimmedKw.toLowerCase())) {
                        matched = true;
                    }
                }

                // 2b. Normalized word boundary check (handles d.m, obfuscation, accents if normalized length >= 2)
                if (!matched) {
                    String normalizedKw = normalizeAndCollapse(trimmedKw);
                    if (normalizedKw.length() >= 2) {
                        String paddedKw = " " + normalizedKw + " ";
                        if (paddedInput.contains(paddedKw)) {
                            matched = true;
                        }
                    }
                }

                if (matched) {
                    customSeverity = SeverityLevel.MEDIUM;
                    break;
                }
            }
        }

        // Return higher severity
        if (hardcodedSeverity.ordinal() > customSeverity.ordinal()) {
            return hardcodedSeverity;
        }
        return customSeverity;
    }

    private boolean matchesAny(String input, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        return false;
    }

    private String normalizeAndCollapse(String input) {
        if (input == null) {
            return "";
        }
        // 1. Replace đ/Đ first
        String temp = input.toLowerCase();
        temp = temp.replace('đ', 'd').replace('Đ', 'd');

        // 2. Accent normalization
        temp = java.text.Normalizer.normalize(temp, java.text.Normalizer.Form.NFD);
        temp = temp.replaceAll("\\p{M}+", "");

        // 3. Remove non-alphanumeric except spaces
        temp = temp.replaceAll("[^a-z0-9\\s]", " ");

        // 4. Collapse single characters
        String[] words = temp.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        int prevWordLen = 0;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (word.length() == 1) {
                if (prevWordLen == 1) {
                    sb.append(word);
                } else {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(word);
                }
                prevWordLen = 1;
            } else {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(word);
                prevWordLen = word.length();
            }
        }
        return sb.toString();
    }
}
