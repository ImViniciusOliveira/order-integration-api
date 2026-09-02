package com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service;

import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;

/**
 * Contrato de acesso às credenciais de uma conta do Mercado Livre.
 */
public interface MercadoLivreCredentialService {

    /**
     * Obtém as credenciais da conta Mercado Livre.
     *
     * @param accountId seller_id, client_id ou identificador opcional da conta
     * @return credencial ativa da conta
     * @throws com.dcriar.orderintegration.exception.custom.ResourceNotFoundException quando a credencial não existir
     */
    MercadoLivreCredentialDocument getCredential(String accountId);

    /**
     * Persiste os tokens atualizados de uma credencial Mercado Livre.
     *
     * @param credential       credencial que será atualizada
     * @param accessToken      novo access token
     * @param refreshToken     novo refresh token ou o anterior
     * @param expirationEpoch  vencimento do access token em epoch seconds
     * @return credencial persistida
     */
    MercadoLivreCredentialDocument updateTokens(
            MercadoLivreCredentialDocument credential,
            String accessToken,
            String refreshToken,
            long expirationEpoch
    );
}
