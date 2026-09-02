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
}
