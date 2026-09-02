package com.dcriar.orderintegration.domain.marketplace.shopee.credential.repository;

import com.dcriar.orderintegration.domain.marketplace.shopee.credential.document.ShopeeCredentialDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório MongoDB exclusivo para credenciais da Shopee.
 */
@Repository
public interface ShopeeCredentialMongoRepository extends MongoRepository<ShopeeCredentialDocument, String> {

    /**
     * Busca a credencial de uma loja Shopee.
     *
     * @param shopId identificador da loja Shopee
     * @return credencial correspondente
     */
    Optional<ShopeeCredentialDocument> findByShopId(String shopId);
}
