package com.dcriar.orderintegration.domain.marketplace.mercadolivre.settlement.oauth;

/**
 * Resposta normalizada do endpoint OAuth2 do Mercado Livre.
 *
 * @param accessToken  novo access token
 * @param refreshToken novo refresh token, quando retornado
 * @param expiresIn    validade do access token em segundos
 */
public record MercadoLivreTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
