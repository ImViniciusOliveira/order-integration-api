package com.dcriar.orderintegration.domain.credential.repository;

import com.dcriar.orderintegration.domain.credential.document.ShopeeCredentialDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório MongoDB estrito para credenciais da Shopee na coleção {@code credenciais_lojas}.
 */
@Repository
public interface ShopeeCredentialMongoRepository extends MongoRepository<ShopeeCredentialDocument, String> {

    /**
     * Busca as credenciais de uma loja específica da Shopee pelo shopId.
     *
     * @param shopId identificador numérico da loja
     * @return credencial correspondente da Shopee
     */
    Optional<ShopeeCredentialDocument> findByShopId(String shopId);
}
