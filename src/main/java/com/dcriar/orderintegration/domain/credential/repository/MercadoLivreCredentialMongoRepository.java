package com.dcriar.orderintegration.domain.credential.repository;

import com.dcriar.orderintegration.domain.credential.document.MercadoLivreCredentialDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório MongoDB estrito para credenciais do Mercado Livre na coleção {@code credenciais_lojas}.
 */
@Repository
public interface MercadoLivreCredentialMongoRepository extends MongoRepository<MercadoLivreCredentialDocument, String> {

    /**
     * Busca as credenciais de uma aplicação/conta do Mercado Livre pelo clientId.
     *
     * @param clientId identificador da aplicação no Mercado Livre
     * @return credencial correspondente do Mercado Livre
     */
    Optional<MercadoLivreCredentialDocument> findByClientId(String clientId);

    /**
     * Busca as credenciais da conta cadastrada para o Mercado Livre pelo identificador da plataforma.
     *
     * @param plataforma identificador da plataforma (ex: "mercadolivre")
     * @return credencial correspondente
     */
    Optional<MercadoLivreCredentialDocument> findByPlataforma(String plataforma);
}
