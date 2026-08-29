package com.dcriar.orderintegration.domain.channel.repository;

import com.dcriar.orderintegration.domain.channel.entity.MarketplaceChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para gerenciamento e consulta de canais de marketplace dinâmicos.
 */
@Repository
public interface MarketplaceChannelRepository extends JpaRepository<MarketplaceChannel, Long> {

    /**
     * Localiza um canal de marketplace através do seu código único.
     *
     * @param code o código do canal (ex: "SHOPEE")
     * @return um {@link Optional} contendo o canal caso exista
     */
    Optional<MarketplaceChannel> findByCode(String code);

    /**
     * Localiza um canal de marketplace ativo através do seu código único.
     *
     * @param code o código do canal (ex: "SHOPEE")
     * @return um {@link Optional} contendo o canal caso exista e esteja ativo
     */
    Optional<MarketplaceChannel> findByCodeAndActiveTrue(String code);

    /**
     * Verifica se já existe um canal cadastrado com o código informado, ignorando maiúsculas/minúsculas.
     *
     * @param code o código do canal a ser verificado
     * @return {@code true} se já existir, {@code false} caso contrário
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Retorna a lista de todos os canais de marketplace que estão atualmente ativos.
     *
     * @return lista de canais ativos
     */
    List<MarketplaceChannel> findAllByActiveTrue();
}
