package com.smashvn.shop.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Vietnamese text matching without altering stored product names or the schema. */
public final class CustomerSearchText {
    private static final String ACCENTS =
            "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
    private static final String BASE_LETTERS = normalize(ACCENTS);

    private CustomerSearchText() { }

    public static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace('đ', 'd').strip().replaceAll("\\s+", " ");
    }

    public static List<String> tokens(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? List.of() : Arrays.stream(normalized.split(" ")).distinct().toList();
    }

    public static Predicate matches(CriteriaBuilder cb, String value, List<Expression<String>> fields) {
        // Bind Unicode mappings instead of SQL varchar literals, which can lose Vietnamese characters on SQL Server.
        var hibernate = (org.hibernate.query.criteria.HibernateCriteriaBuilder) cb;
        List<Expression<String>> normalizedFields = fields.stream()
                .map(field -> cb.function("translate", String.class,
                        cb.lower(cb.coalesce(field, "")), hibernate.value(ACCENTS), hibernate.value(BASE_LETTERS)))
                .toList();
        Predicate[] words = tokens(value).stream().map(word -> cb.or(normalizedFields.stream()
                .map(field -> cb.like(field, "%" + word + "%")).toArray(Predicate[]::new)))
                .toArray(Predicate[]::new);
        return cb.and(words);
    }
}
