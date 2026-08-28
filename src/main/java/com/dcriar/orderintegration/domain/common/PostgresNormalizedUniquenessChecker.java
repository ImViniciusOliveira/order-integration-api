package com.dcriar.orderintegration.domain.common;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilitário para verificação de duplicidade e normalização de campos textuais
 * ignorando acentuação e diferenças de maiúsculas/minúsculas.
 */
@Component
@RequiredArgsConstructor
public class PostgresNormalizedUniquenessChecker {

    private final EntityManager entityManager;

    public static String normalizeText(String input) {
        if (input == null) {
            return null;
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).strip();
    }
}
