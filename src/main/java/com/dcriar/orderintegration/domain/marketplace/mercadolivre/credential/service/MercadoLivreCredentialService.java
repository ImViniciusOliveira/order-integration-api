package com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.service;

import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;

/**
 * Contrato de acesso às credenciais de uma conta do Mercado Livre.
 */
public interface MercadoLivreCredentialService {

    /**
     * Obtém as credenciais da conta Mercado Livre.
     *
     * @param accountId client_id ou identificador opcional da conta
     * @return credencial ativa da conta
     * @throws com.dcriar.orderintegration.exception.custom.ResourceNotFoundException quando a credencial não existir
     */
    MercadoLivreCredentialDocument getCredential(String accountId);
}
