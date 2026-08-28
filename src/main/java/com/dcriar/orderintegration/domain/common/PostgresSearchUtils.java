package com.dcriar.orderintegration.domain.common;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Utilitário para construção de expressões Criteria do PostgreSQL utilizando
 * a função nativa {@code unaccent} e normalização para caixa baixa (case-insensitive).
 */
public final class PostgresSearchUtils {

    private PostgresSearchUtils() {
    }

    public static Expression<String> unaccentLower(CriteriaBuilder cb, Expression<String> expression) {
        Expression<String> unaccented = cb.function("unaccent", String.class, expression);
        return cb.lower(unaccented);
    }

    public static Predicate containsNormalized(CriteriaBuilder cb, Expression<String> expression, String term) {
        if (term == null || term.isBlank()) {
            return cb.conjunction();
        }
        Expression<String> normalizedField = unaccentLower(cb, expression);
        String pattern = "%" + term.toLowerCase() + "%";
        return cb.like(normalizedField, pattern);
    }
}
