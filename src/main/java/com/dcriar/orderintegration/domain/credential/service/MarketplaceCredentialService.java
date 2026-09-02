package com.dcriar.orderintegration.domain.credential.service;

import com.dcriar.orderintegration.domain.credential.document.MercadoLivreCredentialDocument;
import com.dcriar.orderintegration.domain.credential.document.ShopeeCredentialDocument;

/**
 * Contrato de serviço para obtenção estrita de credenciais das lojas no MongoDB.
 * Lança exceções explícitas caso as credenciais não existam, sem recorrer a fallbacks genéricos.
 */
public interface MarketplaceCredentialService {

    /**
     * Obtém as credenciais estritas de uma loja da Shopee.
     *
     * @param shopId identificador da loja
     * @return credencial ativa da Shopee
     * @throws com.dcriar.orderintegration.exception.custom.ResourceNotFoundException caso a loja não seja encontrada
     */
    ShopeeCredentialDocument getShopeeCredential(String shopId);

    /**
     * Obtém as credenciais estritas da conta do Mercado Livre.
     *
     * @param accountId identificador do clientId ou nulo para conta única
     * @return credencial ativa do Mercado Livre
     * @throws com.dcriar.orderintegration.exception.custom.ResourceNotFoundException caso as credenciais não sejam encontradas
     */
    MercadoLivreCredentialDocument getMercadoLivreCredential(String accountId);
}
