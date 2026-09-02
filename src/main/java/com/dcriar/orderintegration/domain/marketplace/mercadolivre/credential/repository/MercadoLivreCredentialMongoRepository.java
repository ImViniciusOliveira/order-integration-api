package com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.repository;

import com.dcriar.orderintegration.domain.marketplace.mercadolivre.credential.document.MercadoLivreCredentialDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório MongoDB exclusivo para credenciais do Mercado Livre.
 */
@Repository
public interface MercadoLivreCredentialMongoRepository extends MongoRepository<MercadoLivreCredentialDocument, String> {

    /**
     * Busca credencial pelo identificador da aplicação Mercado Livre.
     *
     * @param clientId identificador da aplicação
     * @return credencial correspondente
     */
    Optional<MercadoLivreCredentialDocument> findByClientId(String clientId);

    /**
     * Busca a credencial padrão cadastrada para o Mercado Livre.
     *
     * @param plataforma identificador da plataforma
     * @return credencial correspondente
     */
    Optional<MercadoLivreCredentialDocument> findByPlataforma(String plataforma);
}
