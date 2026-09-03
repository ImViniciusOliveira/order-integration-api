package com.dcriar.orderintegration.domain.marketplace.shopee.credential.service;

import com.dcriar.orderintegration.domain.marketplace.shopee.credential.document.ShopeeCredentialDocument;

/**
 * Contrato de acesso às credenciais de uma loja Shopee.
 */
public interface ShopeeCredentialService {

    /**
     * Obtém as credenciais da loja Shopee informada.
     *
     * @param shopId identificador da loja
     * @return credencial ativa da loja
     * @throws com.dcriar.orderintegration.exception.custom.ResourceNotFoundException quando a credencial não existir
     */
    ShopeeCredentialDocument getCredential(String shopId);

    /**
     * Persiste os tokens atualizados de uma loja Shopee.
     *
     * @param credential credencial que sera atualizada
     * @param accessToken novo access token
     * @param refreshToken novo refresh token
     * @param expirationEpoch vencimento do access token em epoch seconds
     * @return credencial persistida
     */
    ShopeeCredentialDocument updateTokens(
            ShopeeCredentialDocument credential,
            String accessToken,
            String refreshToken,
            long expirationEpoch
    );
}
